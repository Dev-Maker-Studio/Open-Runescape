use crate::{MAX_PAYLOAD_BYTES, session::SessionCiphers};
use openrs_protocol::registry::{GamePacket, ServerPacket};
use openrs_protocol::{Error as ProtocolError, Feed};
use std::fmt;
use zeroize::Zeroize;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum WorldEntryError {
    Protocol(ProtocolError),
    CiphersUnavailable,
}

impl fmt::Display for WorldEntryError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Protocol(error) => error.fmt(f),
            Self::CiphersUnavailable => f.write_str(
                "world entry ciphers are unavailable before completion or after transfer",
            ),
        }
    }
}

impl std::error::Error for WorldEntryError {}

impl From<ProtocolError> for WorldEntryError {
    fn from(error: ProtocolError) -> Self {
        Self::Protocol(error)
    }
}

enum State {
    Header,
    Payload { opcode: ServerPacket, length: usize },
    Complete,
    Failed,
}

pub struct WorldEntryDecoder {
    ciphers: Option<SessionCiphers>,
    limit: usize,
    header: [u8; 4],
    header_used: usize,
    payload: Vec<u8>,
    state: State,
}

impl WorldEntryDecoder {
    pub fn new(ciphers: SessionCiphers, max_payload_bytes: usize) -> Result<Self, WorldEntryError> {
        if !(1..=MAX_PAYLOAD_BYTES).contains(&max_payload_bytes) {
            return Err(ProtocolError::InvalidLimit.into());
        }
        Ok(Self {
            ciphers: Some(ciphers),
            limit: max_payload_bytes,
            header: [0; 4],
            header_used: 0,
            payload: Vec::new(),
            state: State::Header,
        })
    }

    pub fn feed(
        &mut self,
        bytes: &[u8],
    ) -> Result<Feed<GamePacket<ServerPacket>>, WorldEntryError> {
        match self.state {
            State::Complete => return Err(ProtocolError::DecoderFinished.into()),
            State::Failed => return Err(ProtocolError::DecoderFailed.into()),
            _ => {}
        }
        let result = self.feed_inner(bytes);
        if result.is_err() {
            self.fail();
        }
        result
    }

    fn feed_inner(
        &mut self,
        bytes: &[u8],
    ) -> Result<Feed<GamePacket<ServerPacket>>, WorldEntryError> {
        let mut consumed = 0;
        loop {
            match self.state {
                State::Header => {
                    let count = (3 - self.header_used.min(3)).min(bytes.len() - consumed);
                    self.header[self.header_used..self.header_used + count]
                        .copy_from_slice(&bytes[consumed..consumed + count]);
                    self.header_used += count;
                    consumed += count;
                    if self.header_used < 3 {
                        break;
                    }
                    let ciphers = self.ciphers.as_mut().ok_or(ProtocolError::DecoderFailed)?;
                    let first = self.header[0].wrapping_sub(ciphers.incoming.peek_u32() as u8);
                    let opcode_bytes = if first < 128 { 1 } else { 2 };
                    if self.header_used < opcode_bytes + 2 {
                        if consumed == bytes.len() {
                            break;
                        }
                        self.header[3] = bytes[consumed];
                        self.header_used += 1;
                        consumed += 1;
                    }
                    let first = self.header[0].wrapping_sub(ciphers.incoming.next_u32() as u8);
                    let wire = if opcode_bytes == 1 {
                        u16::from(first)
                    } else {
                        let second = self.header[1].wrapping_sub(ciphers.incoming.next_u32() as u8);
                        (u16::from(first - 128) << 8) | u16::from(second)
                    };
                    let opcode =
                        ServerPacket::from_wire(wire).ok_or(ProtocolError::UnknownOpcode(wire))?;
                    let length = usize::from(u16::from_be_bytes([
                        self.header[opcode_bytes],
                        self.header[opcode_bytes + 1],
                    ]));
                    if length > self.limit {
                        return Err(ProtocolError::LengthLimitExceeded {
                            length,
                            limit: self.limit,
                        }
                        .into());
                    }
                    self.payload
                        .try_reserve_exact(length)
                        .map_err(|_| ProtocolError::AllocationFailed)?;
                    self.header.zeroize();
                    self.header_used = 0;
                    self.state = State::Payload { opcode, length };
                }
                State::Payload { opcode, length } => {
                    let count = (length - self.payload.len()).min(bytes.len() - consumed);
                    self.payload
                        .extend_from_slice(&bytes[consumed..consumed + count]);
                    consumed += count;
                    if self.payload.len() == length {
                        self.state = State::Complete;
                        return Ok(Feed {
                            consumed,
                            item: Some(GamePacket {
                                opcode,
                                payload: std::mem::take(&mut self.payload),
                            }),
                        });
                    }
                    break;
                }
                State::Complete => return Err(ProtocolError::DecoderFinished.into()),
                State::Failed => return Err(ProtocolError::DecoderFailed.into()),
            }
        }
        Ok(Feed {
            consumed,
            item: None,
        })
    }

    pub fn finish(&mut self) -> Result<(), WorldEntryError> {
        match self.state {
            State::Complete => Ok(()),
            State::Failed => Err(ProtocolError::DecoderFailed.into()),
            _ => {
                self.fail();
                Err(ProtocolError::UnexpectedEof.into())
            }
        }
    }

    pub fn take_ciphers(&mut self) -> Result<SessionCiphers, WorldEntryError> {
        if !matches!(self.state, State::Complete) {
            return Err(WorldEntryError::CiphersUnavailable);
        }
        self.ciphers
            .take()
            .ok_or(WorldEntryError::CiphersUnavailable)
    }

    fn fail(&mut self) {
        self.header.zeroize();
        self.header_used = 0;
        self.payload.zeroize();
        self.payload = Vec::new();
        self.ciphers = None;
        self.state = State::Failed;
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::isaac_ciphers;

    fn decoder() -> WorldEntryDecoder {
        let (outgoing, incoming) = isaac_ciphers(&[1, 2, 3, 4]).unwrap();
        WorldEntryDecoder::new(SessionCiphers { outgoing, incoming }, 8).unwrap()
    }

    #[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
    #[cfg_attr(not(target_arch = "wasm32"), test)]
    fn failure_drops_ciphers_and_releases_cleared_payload_and_header() {
        let mut decoder = decoder();
        let (_, mut incoming) = isaac_ciphers(&[1, 2, 3, 4]).unwrap();
        let opcode = 29u8.wrapping_add(incoming.next_u32() as u8);
        let result = decoder.feed(&[opcode, 0, 8, 0x12, 0x34]).unwrap();
        assert_eq!(result.consumed, 5);
        assert!(result.item.is_none());
        assert_eq!(decoder.payload, [0x12, 0x34]);
        assert_eq!(decoder.finish(), Err(ProtocolError::UnexpectedEof.into()));
        assert!(decoder.ciphers.is_none());
        assert_eq!(decoder.header, [0; 4]);
        assert_eq!(decoder.header_used, 0);
        assert!(decoder.payload.is_empty());
        assert_eq!(decoder.payload.capacity(), 0);
        assert!(matches!(decoder.state, State::Failed));
    }

    #[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
    #[cfg_attr(not(target_arch = "wasm32"), test)]
    fn invalid_headers_release_ciphers_without_allocating_payload() {
        for wire in [29u16, 161] {
            let mut decoder = decoder();
            let (_, mut incoming) = isaac_ciphers(&[1, 2, 3, 4]).unwrap();
            let mut header = Vec::new();
            if wire >= 128 {
                header.push(128u8.wrapping_add(incoming.next_u32() as u8));
            }
            header.push((wire as u8).wrapping_add(incoming.next_u32() as u8));
            header.extend_from_slice(&65535u16.to_be_bytes());
            let expected = if wire == 29 {
                ProtocolError::LengthLimitExceeded {
                    length: 65535,
                    limit: 8,
                }
            } else {
                ProtocolError::UnknownOpcode(wire)
            };
            assert_eq!(decoder.feed(&header), Err(expected.into()));
            assert!(decoder.ciphers.is_none());
            assert_eq!(decoder.header, [0; 4]);
            assert_eq!(decoder.header_used, 0);
            assert_eq!(decoder.payload.capacity(), 0);
            assert!(matches!(decoder.state, State::Failed));
        }
    }

    #[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
    #[cfg_attr(not(target_arch = "wasm32"), test)]
    fn waiting_for_smart_header_repeatedly_peeks_without_consuming_a_key() {
        let mut decoder = decoder();
        let (_, mut incoming) = isaac_ciphers(&[1, 2, 3, 4]).unwrap();
        let first_key = incoming.next_u32();
        let second_key = incoming.next_u32();
        let third_key = incoming.next_u32();
        let prefix = [
            128u8.wrapping_add(first_key as u8),
            128u8.wrapping_add(second_key as u8),
            0,
        ];
        let result = decoder.feed(&prefix).unwrap();
        assert_eq!(result.consumed, 3);
        assert!(result.item.is_none());
        for _ in 0..8 {
            let result = decoder.feed(&[]).unwrap();
            assert_eq!(result.consumed, 0);
            assert!(result.item.is_none());
            assert_eq!(
                decoder.ciphers.as_mut().unwrap().incoming.peek_u32(),
                first_key
            );
        }
        let result = decoder.feed(&[0]).unwrap();
        assert_eq!(result.consumed, 1);
        assert_eq!(result.item.unwrap().opcode.wire(), 128);
        assert_eq!(
            decoder.take_ciphers().unwrap().incoming.next_u32(),
            third_key
        );
    }
}
