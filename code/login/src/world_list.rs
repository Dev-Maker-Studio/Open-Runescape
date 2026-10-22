use openrs_protocol::payload::{PayloadError, PayloadReader};
use std::fmt;

pub const MAX_WORLD_LIST_BYTES: usize = 20_000;
pub const MAX_DESCRIPTOR_SLOTS: usize = 32_768;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    Payload(PayloadError),
    LengthLimitExceeded { length: usize, limit: usize },
    InvalidMode(u8),
    InvalidDescriptorRange { start: u16, size_plus_one: u16 },
    InvalidWorldCount { count: usize, slots: usize },
    InvalidWorldId { raw_id: u16, slots: usize },
    InvalidLocation { index: u8, count: usize },
    DuplicateDescriptor(u16),
    DuplicateCount(u16),
    TrailingData,
    AllocationFailed,
    DecoderFailed,
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Payload(error) => error.fmt(f),
            Self::LengthLimitExceeded { length, limit } => {
                write!(
                    f,
                    "world-list chunks contain {length} bytes, limit is {limit}"
                )
            }
            Self::InvalidMode(mode) => write!(f, "unsupported world-list mode {mode}"),
            Self::InvalidDescriptorRange {
                start,
                size_plus_one,
            } => write!(
                f,
                "invalid world-list descriptor range {start}..={size_plus_one}"
            ),
            Self::InvalidWorldCount { count, slots } => {
                write!(
                    f,
                    "world-list has {count} worlds for {slots} descriptor slots"
                )
            }
            Self::InvalidWorldId { raw_id, slots } => {
                write!(
                    f,
                    "world-list raw ID {raw_id} is outside {slots} descriptor slots"
                )
            }
            Self::InvalidLocation { index, count } => {
                write!(
                    f,
                    "world-list location index {index} is outside {count} locations"
                )
            }
            Self::DuplicateDescriptor(raw_id) => {
                write!(f, "duplicate world-list descriptor {raw_id}")
            }
            Self::DuplicateCount(raw_id) => write!(f, "duplicate world-list player count {raw_id}"),
            Self::TrailingData => f.write_str("unexpected trailing world-list data"),
            Self::AllocationFailed => f.write_str("world-list allocation failed"),
            Self::DecoderFailed => f.write_str("world-list decoder cannot resume after an error"),
        }
    }
}

impl std::error::Error for Error {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Payload(error) => Some(error),
            _ => None,
        }
    }
}

impl From<PayloadError> for Error {
    fn from(error: PayloadError) -> Self {
        Self::Payload(error)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Location {
    pub country_id: u16,
    pub activity: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct World {
    pub raw_id: u16,
    pub world_number: u16,
    pub location_index: u8,
    pub flags: i32,
    pub activity: String,
    pub host: String,
    pub port: Option<i32>,
    pub player_count: i32,
}

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct WorldList {
    pub version: Option<i32>,
    pub locations: Vec<Location>,
    pub start: u16,
    pub size_plus_one: u16,
    pub descriptor_slots: usize,
    pub worlds: Vec<World>,
}

impl WorldList {
    pub fn world_by_raw_id(&self, raw_id: u16) -> Option<&World> {
        self.worlds
            .binary_search_by_key(&raw_id, |world| world.raw_id)
            .ok()
            .map(|index| &self.worlds[index])
    }

    pub fn world_by_number(&self, world_number: u16) -> Option<&World> {
        self.world_by_raw_id(world_number.checked_sub(self.start)?)
    }
}

#[derive(Debug, Default)]
pub struct WorldListDecoder {
    pending: Vec<u8>,
    collecting: bool,
    snapshot: Option<WorldList>,
    failed: bool,
}

impl WorldListDecoder {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn snapshot(&self) -> Option<&WorldList> {
        self.snapshot.as_ref()
    }

    pub fn pending_bytes(&self) -> usize {
        self.pending.len()
    }

    pub fn is_failed(&self) -> bool {
        self.failed
    }

    pub fn reset(&mut self) {
        *self = Self::default();
    }

    fn poison(&mut self) {
        self.pending = Vec::new();
        self.collecting = false;
        self.snapshot = None;
        self.failed = true;
    }

    pub fn feed(&mut self, packet_payload: &[u8]) -> Result<Option<&WorldList>, Error> {
        if self.failed {
            return Err(Error::DecoderFailed);
        }
        let result = self.append(packet_payload);
        match result {
            Ok(true) => Ok(self.snapshot.as_ref()),
            Ok(false) => Ok(None),
            Err(error) => {
                self.poison();
                Err(error)
            }
        }
    }

    fn append(&mut self, packet_payload: &[u8]) -> Result<bool, Error> {
        let (&refresh, chunk) = packet_payload
            .split_first()
            .ok_or(PayloadError::UnexpectedEof)?;
        if chunk.len() > MAX_WORLD_LIST_BYTES - self.pending.len() {
            return Err(Error::LengthLimitExceeded {
                length: self.pending.len().saturating_add(chunk.len()),
                limit: MAX_WORLD_LIST_BYTES,
            });
        }
        self.pending
            .try_reserve_exact(chunk.len())
            .map_err(|_| Error::AllocationFailed)?;
        self.pending.extend_from_slice(chunk);
        self.collecting = true;
        if refresh != 1 {
            return Ok(false);
        }
        let snapshot = decode(&self.pending, self.snapshot.take())?;
        self.pending = Vec::new();
        self.collecting = false;
        self.snapshot = Some(snapshot);
        Ok(true)
    }

    pub fn finish(&mut self) -> Result<(), Error> {
        if self.failed {
            return Err(Error::DecoderFailed);
        }
        if self.collecting {
            self.poison();
            return Err(PayloadError::UnexpectedEof.into());
        }
        Ok(())
    }
}

fn reserved<T>(length: usize) -> Result<Vec<T>, Error> {
    let mut values = Vec::new();
    values
        .try_reserve_exact(length)
        .map_err(|_| Error::AllocationFailed)?;
    Ok(values)
}

fn require_bytes(reader: &PayloadReader<'_>, minimum: usize) -> Result<(), Error> {
    if minimum > reader.remaining() {
        return Err(PayloadError::UnexpectedEof.into());
    }
    Ok(())
}

fn mark(seen: &mut [u64; MAX_DESCRIPTOR_SLOTS / 64], raw_id: u16) -> bool {
    let id = usize::from(raw_id);
    let mask = 1u64 << (id % 64);
    let word = &mut seen[id / 64];
    let duplicate = *word & mask != 0;
    *word |= mask;
    duplicate
}

fn metadata(reader: &mut PayloadReader<'_>) -> Result<WorldList, Error> {
    let location_count = usize::from(reader.read_u_smart()?);
    require_bytes(reader, location_count * 3 + 7)?;
    let mut locations = reserved(location_count)?;
    for _ in 0..location_count {
        locations.push(Location {
            country_id: reader.read_u_smart()?,
            activity: reader.read_jag_cp1252()?,
        });
    }
    let start = reader.read_u_smart()?;
    let size_plus_one = reader.read_u_smart()?;
    let slots = (usize::from(size_plus_one) + 1)
        .checked_sub(usize::from(start))
        .ok_or(Error::InvalidDescriptorRange {
            start,
            size_plus_one,
        })?;
    let count = usize::from(reader.read_u_smart()?);
    if count > slots {
        return Err(Error::InvalidWorldCount { count, slots });
    }
    require_bytes(reader, count * 13 + 4)?;
    let mut worlds = reserved(count)?;
    let mut seen = [0; MAX_DESCRIPTOR_SLOTS / 64];
    for _ in 0..count {
        let raw_id = reader.read_u_smart()?;
        if usize::from(raw_id) >= slots {
            return Err(Error::InvalidWorldId { raw_id, slots });
        }
        if mark(&mut seen, raw_id) {
            return Err(Error::DuplicateDescriptor(raw_id));
        }
        let location_index = reader.read_u8()?;
        if usize::from(location_index) >= location_count {
            return Err(Error::InvalidLocation {
                index: location_index,
                count: location_count,
            });
        }
        let flags = reader.read_i32_be()?;
        let activity = reader.read_jag_cp1252()?;
        let host = reader.read_jag_cp1252()?;
        let port = if flags & 0x4000_0000 != 0 {
            Some(reader.read_i32_be()?)
        } else {
            None
        };
        worlds.push(World {
            raw_id,
            world_number: raw_id + start,
            location_index,
            flags,
            activity,
            host,
            port,
            player_count: 0,
        });
    }
    worlds.sort_unstable_by_key(|world| world.raw_id);
    let version = Some(reader.read_i32_be()?);
    Ok(WorldList {
        version,
        locations,
        start,
        size_plus_one,
        descriptor_slots: slots,
        worlds,
    })
}

fn decode(data: &[u8], previous: Option<WorldList>) -> Result<WorldList, Error> {
    let mut reader = PayloadReader::new(data);
    let mode = reader.read_u8()?;
    if mode != 2 {
        return Err(Error::InvalidMode(mode));
    }
    let mut snapshot = if reader.read_u8()? == 1 {
        metadata(&mut reader)?
    } else {
        previous.unwrap_or_default()
    };
    require_bytes(&reader, snapshot.worlds.len() * 3)?;
    let mut seen = [0; MAX_DESCRIPTOR_SLOTS / 64];
    for _ in 0..snapshot.worlds.len() {
        let raw_id = reader.read_u_smart()?;
        if usize::from(raw_id) >= snapshot.descriptor_slots {
            return Err(Error::InvalidWorldId {
                raw_id,
                slots: snapshot.descriptor_slots,
            });
        }
        if mark(&mut seen, raw_id) {
            return Err(Error::DuplicateCount(raw_id));
        }
        let count = reader.read_u16_be()?;
        if let Ok(index) = snapshot
            .worlds
            .binary_search_by_key(&raw_id, |world| world.raw_id)
        {
            snapshot.worlds[index].player_count = if count == u16::MAX {
                -1
            } else {
                i32::from(count)
            };
        }
    }
    if reader.remaining() != 0 {
        return Err(Error::TrailingData);
    }
    Ok(snapshot)
}
