use crate::{Error, Feed};

pub const REVISION: u32 = 727;
pub const SUBREVISION: u32 = 1;
pub const REVISION727_PREFETCH_COUNT: usize = 27;
pub const VERSION_TABLE_INDEX: u8 = 255;
pub const VERSION_TABLE_ARCHIVE: u32 = 255;
pub const DEFAULT_MAX_CONTAINER_BYTES: usize = 16 * 1024 * 1024;
const MAX_PREFETCH_COUNT: usize = 256;
const REPLY_HEADER_SIZE: usize = 10;
const CONTAINER_HEADER_SIZE: usize = 5;
const BLOCK_SIZE: usize = 512;
const VERSION_ENTRY_SIZE: usize = 72;
const MAX_SIGNATURE_SIZE: usize = 256;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Priority {
    High,
    Prefetch,
}

pub fn encode_handshake(token: &[u8]) -> Result<Vec<u8>, Error> {
    if token.contains(&0) {
        return Err(Error::InvalidToken);
    }
    let length = 9usize
        .checked_add(token.len())
        .ok_or(Error::HandshakeTooLong)?;
    if length > u8::MAX as usize {
        return Err(Error::HandshakeTooLong);
    }
    let mut bytes = Vec::with_capacity(length + 2);
    bytes.extend_from_slice(&[15, length as u8]);
    bytes.extend_from_slice(&REVISION.to_be_bytes());
    bytes.extend_from_slice(&SUBREVISION.to_be_bytes());
    bytes.extend_from_slice(token);
    bytes.push(0);
    Ok(bytes)
}

pub const fn acknowledge() -> [u8; 6] {
    [6, 0, 0, 3, 0, 0]
}

pub const fn encode_status(logged_in: bool) -> [u8; 6] {
    [if logged_in { 2 } else { 3 }, 0, 0, 0, 0, 0]
}

pub const fn encode_request(index: u8, archive: u32, priority: Priority) -> [u8; 6] {
    let archive = archive.to_be_bytes();
    [
        match priority {
            Priority::High => 1,
            Priority::Prefetch => 0,
        },
        index,
        archive[0],
        archive[1],
        archive[2],
        archive[3],
    ]
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum HandshakeReply {
    Accepted { prefetch_keys: Vec<i32> },
    Rejected { status: u8 },
}

pub struct HandshakeDecoder {
    prefetch_count: usize,
    status: Option<u8>,
    bytes: [u8; MAX_PREFETCH_COUNT * 4],
    used: usize,
    finished: bool,
    failed: bool,
}

impl HandshakeDecoder {
    pub fn new(prefetch_count: usize) -> Result<Self, Error> {
        if prefetch_count > MAX_PREFETCH_COUNT {
            return Err(Error::InvalidLimit);
        }
        Ok(Self {
            prefetch_count,
            status: None,
            bytes: [0; MAX_PREFETCH_COUNT * 4],
            used: 0,
            finished: false,
            failed: false,
        })
    }

    pub fn feed(&mut self, input: &[u8]) -> Result<Feed<HandshakeReply>, Error> {
        if self.failed {
            return Err(Error::DecoderFailed);
        }
        if self.finished {
            return Err(Error::DecoderFinished);
        }
        let mut consumed = 0;
        while self.status.is_none() {
            let Some(&status) = input.get(consumed) else {
                return Ok(Feed {
                    consumed,
                    item: None,
                });
            };
            consumed += 1;
            if status == 25 {
                continue;
            }
            self.status = Some(status);
            if status != 0 {
                self.finished = true;
                return Ok(Feed {
                    consumed,
                    item: Some(HandshakeReply::Rejected { status }),
                });
            }
        }
        let expected = self.prefetch_count * 4;
        let count = (expected - self.used).min(input.len() - consumed);
        self.bytes[self.used..self.used + count]
            .copy_from_slice(&input[consumed..consumed + count]);
        self.used += count;
        consumed += count;
        let item = if self.used == expected {
            self.finished = true;
            let prefetch_keys = self.bytes[..expected]
                .chunks_exact(4)
                .map(read_i32)
                .collect();
            Some(HandshakeReply::Accepted { prefetch_keys })
        } else {
            None
        };
        Ok(Feed { consumed, item })
    }

    pub fn finish(&mut self) -> Result<(), Error> {
        if self.failed {
            return Err(Error::DecoderFailed);
        }
        if !self.finished {
            self.failed = true;
            return Err(Error::UnexpectedEof);
        }
        Ok(())
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BoundaryMode {
    Strict,
    NativeRestart,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Reply {
    pub index: u8,
    pub archive: u32,
    pub priority: Priority,
    pub container: Vec<u8>,
}

struct PartialReply {
    index: u8,
    archive: u32,
    priority: Priority,
    expected: usize,
    block_position: usize,
    container: Vec<u8>,
}

pub struct ReplyDecoder {
    max_container_bytes: usize,
    boundary_mode: BoundaryMode,
    header: [u8; REPLY_HEADER_SIZE],
    header_used: usize,
    partial: Option<PartialReply>,
    interrupted_replies: u64,
    failed: bool,
}

impl ReplyDecoder {
    pub fn new(max_container_bytes: usize) -> Result<Self, Error> {
        Self::with_boundary_mode(max_container_bytes, BoundaryMode::Strict)
    }

    pub fn with_boundary_mode(
        max_container_bytes: usize,
        boundary_mode: BoundaryMode,
    ) -> Result<Self, Error> {
        if !(CONTAINER_HEADER_SIZE..=i32::MAX as usize).contains(&max_container_bytes) {
            return Err(Error::InvalidLimit);
        }
        Ok(Self {
            max_container_bytes,
            boundary_mode,
            header: [0; REPLY_HEADER_SIZE],
            header_used: 0,
            partial: None,
            interrupted_replies: 0,
            failed: false,
        })
    }

    pub fn interrupted_replies(&self) -> u64 {
        self.interrupted_replies
    }

    pub fn feed(&mut self, input: &[u8]) -> Result<Feed<Reply>, Error> {
        if self.failed {
            return Err(Error::DecoderFailed);
        }
        let result = self.feed_inner(input);
        if result.is_err() {
            self.failed = true;
            self.partial = None;
        }
        result
    }

    fn feed_inner(&mut self, input: &[u8]) -> Result<Feed<Reply>, Error> {
        let mut consumed = 0;
        loop {
            if let Some(partial) = self.partial.as_ref() {
                if partial.container.len() == partial.expected {
                    let partial = self.partial.take().ok_or(Error::DecoderFailed)?;
                    return Ok(Feed {
                        consumed,
                        item: Some(Reply {
                            index: partial.index,
                            archive: partial.archive,
                            priority: partial.priority,
                            container: partial.container,
                        }),
                    });
                }
            }
            if consumed == input.len() {
                return Ok(Feed {
                    consumed,
                    item: None,
                });
            }
            if self.partial.is_none() {
                let count = (REPLY_HEADER_SIZE - self.header_used).min(input.len() - consumed);
                self.header[self.header_used..self.header_used + count]
                    .copy_from_slice(&input[consumed..consumed + count]);
                self.header_used += count;
                consumed += count;
                if self.header_used == REPLY_HEADER_SIZE {
                    self.start_reply()?;
                }
                continue;
            }
            let partial = self.partial.as_mut().ok_or(Error::DecoderFailed)?;
            if partial.block_position == BLOCK_SIZE {
                let marker = input[consumed];
                consumed += 1;
                if marker == 255 {
                    partial.block_position = 1;
                } else if self.boundary_mode == BoundaryMode::NativeRestart {
                    self.partial = None;
                    self.header[0] = marker;
                    self.header_used = 1;
                    self.interrupted_replies = self.interrupted_replies.saturating_add(1);
                } else {
                    return Err(Error::InvalidContinuation(marker));
                }
                continue;
            }
            let count = (partial.expected - partial.container.len())
                .min(BLOCK_SIZE - partial.block_position)
                .min(input.len() - consumed);
            partial
                .container
                .extend_from_slice(&input[consumed..consumed + count]);
            partial.block_position += count;
            consumed += count;
        }
    }

    fn start_reply(&mut self) -> Result<(), Error> {
        let length = read_i32(&self.header[6..10]);
        if length < 0 {
            return Err(Error::InvalidLength);
        }
        let compression = self.header[5] & 0x7f;
        let expected = (length as usize)
            .checked_add(if compression == 0 { 5 } else { 9 })
            .ok_or(Error::InvalidLength)?;
        if expected > self.max_container_bytes {
            return Err(Error::LengthLimitExceeded {
                length: expected,
                limit: self.max_container_bytes,
            });
        }
        let mut container = Vec::new();
        container
            .try_reserve_exact(expected)
            .map_err(|_| Error::AllocationFailed)?;
        container.push(compression);
        container.extend_from_slice(&self.header[6..10]);
        self.partial = Some(PartialReply {
            index: self.header[0],
            archive: u32::from_be_bytes(
                self.header[1..5]
                    .try_into()
                    .map_err(|_| Error::InvalidLength)?,
            ),
            priority: if self.header[5] & 0x80 == 0 {
                Priority::High
            } else {
                Priority::Prefetch
            },
            expected,
            block_position: REPLY_HEADER_SIZE,
            container,
        });
        self.header_used = 0;
        Ok(())
    }

    pub fn finish(&mut self) -> Result<(), Error> {
        if self.failed {
            return Err(Error::DecoderFailed);
        }
        if self.header_used != 0 || self.partial.is_some() {
            self.failed = true;
            self.partial = None;
            return Err(Error::UnexpectedEof);
        }
        Ok(())
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VersionEntry {
    pub crc: i32,
    pub revision: i32,
    pub whirlpool: [u8; 64],
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VersionTable {
    pub entries: Vec<VersionEntry>,
    pub signature: Vec<u8>,
}

pub fn decode_version_table(container: &[u8]) -> Result<VersionTable, Error> {
    if container.len() < 6 {
        return Err(Error::InvalidVersionTable);
    }
    if container[0] != 0 {
        return Err(Error::InvalidCompression(container[0]));
    }
    let length = read_i32(&container[1..5]);
    if length < 0 || length as usize != container.len() - CONTAINER_HEADER_SIZE {
        return Err(Error::InvalidVersionTable);
    }
    let count = container[5] as usize;
    let entries_end = 6 + count * VERSION_ENTRY_SIZE;
    if entries_end > container.len() || container.len() - entries_end > MAX_SIGNATURE_SIZE {
        return Err(Error::InvalidVersionTable);
    }
    let entries = container[6..entries_end]
        .chunks_exact(VERSION_ENTRY_SIZE)
        .map(|entry| VersionEntry {
            crc: read_i32(&entry[..4]),
            revision: read_i32(&entry[4..8]),
            whirlpool: entry[8..].try_into().expect("fixed 72-byte version entry"),
        })
        .collect();
    Ok(VersionTable {
        entries,
        signature: container[entries_end..].to_vec(),
    })
}

fn read_i32(bytes: &[u8]) -> i32 {
    i32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]])
}
