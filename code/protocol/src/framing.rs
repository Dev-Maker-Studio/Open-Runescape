use crate::isaac::Isaac;
use crate::{Error, Feed};

const LARGE_OPCODE_FLAG: u8 = 128;
const MAX_OPCODE: u16 = 32767;

#[derive(Clone, Copy, PartialEq, Eq)]
pub(crate) enum OpcodeEncoding {
    Byte,
    Smart,
}

impl OpcodeEncoding {
    fn max_opcode(self) -> u16 {
        match self {
            Self::Byte => u8::MAX.into(),
            Self::Smart => MAX_OPCODE,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PacketSize {
    Fixed(usize),
    VarByte,
    VarShort,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Packet {
    pub opcode: u16,
    pub payload: Vec<u8>,
}

pub struct PacketEncoder {
    cipher: Option<Isaac>,
    max_payload_bytes: usize,
    opcode_encoding: OpcodeEncoding,
}

impl PacketEncoder {
    pub fn new(cipher: Option<Isaac>, max_payload_bytes: usize) -> Result<Self, Error> {
        validate_limit(max_payload_bytes)?;
        Ok(Self {
            cipher,
            max_payload_bytes,
            opcode_encoding: OpcodeEncoding::Smart,
        })
    }

    pub(crate) fn with_opcode_encoding(mut self, encoding: OpcodeEncoding) -> Self {
        self.opcode_encoding = encoding;
        self
    }

    pub fn encode(
        &mut self,
        opcode: u16,
        size: PacketSize,
        payload: &[u8],
    ) -> Result<Vec<u8>, Error> {
        if opcode > self.opcode_encoding.max_opcode() {
            return Err(Error::InvalidOpcode(opcode));
        }
        check_length(payload.len(), self.max_payload_bytes)?;
        let prefix_size = match size {
            PacketSize::Fixed(expected) => {
                if payload.len() != expected {
                    return Err(Error::PayloadLengthMismatch {
                        expected,
                        actual: payload.len(),
                    });
                }
                0
            }
            PacketSize::VarByte => {
                check_length(payload.len(), u8::MAX as usize)?;
                1
            }
            PacketSize::VarShort => {
                check_length(payload.len(), u16::MAX as usize)?;
                2
            }
        };
        let mut bytes = Vec::new();
        bytes
            .try_reserve_exact(payload.len() + prefix_size + 2)
            .map_err(|_| Error::AllocationFailed)?;
        if self.opcode_encoding == OpcodeEncoding::Smart && opcode >= LARGE_OPCODE_FLAG as u16 {
            bytes.push(self.encrypt(((opcode >> 8) as u8) + LARGE_OPCODE_FLAG));
        }
        bytes.push(self.encrypt(opcode as u8));
        match size {
            PacketSize::Fixed(_) => {}
            PacketSize::VarByte => bytes.push(payload.len() as u8),
            PacketSize::VarShort => bytes.extend_from_slice(&(payload.len() as u16).to_be_bytes()),
        }
        bytes.extend_from_slice(payload);
        Ok(bytes)
    }

    fn encrypt(&mut self, byte: u8) -> u8 {
        byte.wrapping_add(
            self.cipher
                .as_mut()
                .map_or(0, |cipher| cipher.next_u32() as u8),
        )
    }
}

pub struct PacketDecoder<'a> {
    cipher: Option<Isaac>,
    sizes: &'a [Option<PacketSize>],
    max_payload_bytes: usize,
    opcode_encoding: OpcodeEncoding,
    first_opcode_byte: Option<u8>,
    opcode: Option<u16>,
    prefix: [u8; 2],
    prefix_used: usize,
    expected: Option<usize>,
    payload: Vec<u8>,
    failed: bool,
}

impl<'a> PacketDecoder<'a> {
    pub fn new(
        cipher: Option<Isaac>,
        sizes: &'a [Option<PacketSize>],
        max_payload_bytes: usize,
    ) -> Result<Self, Error> {
        validate_limit(max_payload_bytes)?;
        if sizes.len() > MAX_OPCODE as usize + 1 {
            return Err(Error::InvalidLimit);
        }
        Ok(Self {
            cipher,
            sizes,
            max_payload_bytes,
            opcode_encoding: OpcodeEncoding::Smart,
            first_opcode_byte: None,
            opcode: None,
            prefix: [0; 2],
            prefix_used: 0,
            expected: None,
            payload: Vec::new(),
            failed: false,
        })
    }

    pub(crate) fn with_opcode_encoding(mut self, encoding: OpcodeEncoding) -> Self {
        self.opcode_encoding = encoding;
        self
    }

    pub fn feed(&mut self, input: &[u8]) -> Result<Feed<Packet>, Error> {
        if self.failed {
            return Err(Error::DecoderFailed);
        }
        let result = self.feed_inner(input);
        if result.is_err() {
            self.failed = true;
            self.payload = Vec::new();
        }
        result
    }

    fn feed_inner(&mut self, input: &[u8]) -> Result<Feed<Packet>, Error> {
        let mut consumed = 0;
        loop {
            if let Some(expected) = self.expected {
                if self.payload.len() == expected {
                    let opcode = self.opcode.take().ok_or(Error::DecoderFailed)?;
                    self.expected = None;
                    self.prefix_used = 0;
                    return Ok(Feed {
                        consumed,
                        item: Some(Packet {
                            opcode,
                            payload: std::mem::take(&mut self.payload),
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
            if self.opcode.is_none() {
                let byte = self.decrypt(input[consumed]);
                consumed += 1;
                let opcode = if let Some(first) = self.first_opcode_byte.take() {
                    ((first as u16 - LARGE_OPCODE_FLAG as u16) << 8) | byte as u16
                } else if self.opcode_encoding == OpcodeEncoding::Byte || byte < LARGE_OPCODE_FLAG {
                    byte as u16
                } else {
                    self.first_opcode_byte = Some(byte);
                    continue;
                };
                let size = self
                    .sizes
                    .get(opcode as usize)
                    .copied()
                    .flatten()
                    .ok_or(Error::UnknownOpcode(opcode))?;
                self.opcode = Some(opcode);
                if let PacketSize::Fixed(length) = size {
                    self.set_expected(length)?;
                }
                continue;
            }
            if self.expected.is_none() {
                let opcode = self.opcode.ok_or(Error::DecoderFailed)?;
                let prefix_size = match self.sizes[opcode as usize] {
                    Some(PacketSize::VarByte) => 1,
                    Some(PacketSize::VarShort) => 2,
                    _ => return Err(Error::DecoderFailed),
                };
                let count = (prefix_size - self.prefix_used).min(input.len() - consumed);
                self.prefix[self.prefix_used..self.prefix_used + count]
                    .copy_from_slice(&input[consumed..consumed + count]);
                self.prefix_used += count;
                consumed += count;
                if self.prefix_used == prefix_size {
                    let length = if prefix_size == 1 {
                        self.prefix[0] as usize
                    } else {
                        u16::from_be_bytes(self.prefix) as usize
                    };
                    self.set_expected(length)?;
                }
                continue;
            }
            let expected = self.expected.ok_or(Error::DecoderFailed)?;
            let count = (expected - self.payload.len()).min(input.len() - consumed);
            self.payload
                .extend_from_slice(&input[consumed..consumed + count]);
            consumed += count;
        }
    }

    fn set_expected(&mut self, length: usize) -> Result<(), Error> {
        check_length(length, self.max_payload_bytes)?;
        self.payload
            .try_reserve_exact(length)
            .map_err(|_| Error::AllocationFailed)?;
        self.expected = Some(length);
        Ok(())
    }

    fn decrypt(&mut self, byte: u8) -> u8 {
        byte.wrapping_sub(
            self.cipher
                .as_mut()
                .map_or(0, |cipher| cipher.next_u32() as u8),
        )
    }

    pub fn finish(&mut self) -> Result<(), Error> {
        if self.failed {
            return Err(Error::DecoderFailed);
        }
        if self.first_opcode_byte.is_some() || self.opcode.is_some() {
            self.failed = true;
            self.payload = Vec::new();
            return Err(Error::UnexpectedEof);
        }
        Ok(())
    }
}

fn validate_limit(limit: usize) -> Result<(), Error> {
    if limit > i32::MAX as usize {
        return Err(Error::InvalidLimit);
    }
    Ok(())
}

fn check_length(length: usize, limit: usize) -> Result<(), Error> {
    if length > limit {
        return Err(Error::LengthLimitExceeded { length, limit });
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
    #[cfg_attr(not(target_arch = "wasm32"), test)]
    fn byte_opcode_mode_preserves_all_eight_bits_and_rejects_overflow_before_isaac() {
        let mut plain = PacketEncoder::new(None, 0)
            .unwrap()
            .with_opcode_encoding(OpcodeEncoding::Byte);
        for opcode in [0, 127, 128, 255] {
            assert_eq!(
                plain.encode(opcode, PacketSize::Fixed(0), &[]).unwrap(),
                [opcode as u8]
            );
        }
        let seed = [1, 0xffff_fffe, 0x1234_5678, 0x8000_0000];
        let mut encoder = PacketEncoder::new(Some(Isaac::new(&seed).unwrap()), 0)
            .unwrap()
            .with_opcode_encoding(OpcodeEncoding::Byte);
        assert_eq!(
            encoder.encode(256, PacketSize::Fixed(0), &[]),
            Err(Error::InvalidOpcode(256))
        );
        let mut wire = encoder.encode(128, PacketSize::Fixed(0), &[]).unwrap();
        wire.extend(encoder.encode(0, PacketSize::Fixed(0), &[]).unwrap());
        assert_eq!(
            wire,
            include_bytes!("../tests/fixtures/game-client.high-unknown.wire")
        );
        let mut sizes = [None; 256];
        sizes[128] = Some(PacketSize::Fixed(0));
        sizes[0] = Some(PacketSize::Fixed(0));
        let mut decoder = PacketDecoder::new(Some(Isaac::new(&seed).unwrap()), &sizes, 0)
            .unwrap()
            .with_opcode_encoding(OpcodeEncoding::Byte);
        for (index, opcode) in [128, 0].into_iter().enumerate() {
            let result = decoder.feed(&wire[index..index + 1]).unwrap();
            assert_eq!(result.consumed, 1);
            assert_eq!(
                result.item,
                Some(Packet {
                    opcode,
                    payload: vec![]
                })
            );
        }
        decoder.finish().unwrap();
    }
}
