use std::fmt;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PacketError {
    Truncated { position: usize, needed: usize, len: usize },
}

impl fmt::Display for PacketError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            PacketError::Truncated { position, needed, len } => {
                write!(f, "packet truncated at {position}: need {needed} bytes of {len}")
            }
        }
    }
}

impl std::error::Error for PacketError {}

pub struct Packet<'a> {
    data: &'a [u8],
    position: usize,
}

impl<'a> Packet<'a> {
    pub fn new(data: &'a [u8]) -> Self {
        Packet { data, position: 0 }
    }

    pub fn position(&self) -> usize {
        self.position
    }

    pub fn remaining(&self) -> usize {
        self.data.len() - self.position
    }

    fn take(&mut self, count: usize) -> Result<&'a [u8], PacketError> {
        if self.position + count > self.data.len() {
            return Err(PacketError::Truncated { position: self.position, needed: count, len: self.data.len() });
        }
        let slice = &self.data[self.position..self.position + count];
        self.position += count;
        Ok(slice)
    }

    pub fn read_u8(&mut self) -> Result<i32, PacketError> {
        Ok(self.take(1)?[0] as i32)
    }

    pub fn read_i8(&mut self) -> Result<i32, PacketError> {
        Ok(self.take(1)?[0] as i8 as i32)
    }

    pub fn read_u16(&mut self) -> Result<i32, PacketError> {
        let b = self.take(2)?;
        Ok(((b[0] as i32) << 8) | b[1] as i32)
    }

    pub fn read_i16(&mut self) -> Result<i32, PacketError> {
        Ok(self.read_u16()? as i16 as i32)
    }

    pub fn read_u24(&mut self) -> Result<i32, PacketError> {
        let b = self.take(3)?;
        Ok(((b[0] as i32) << 16) | ((b[1] as i32) << 8) | b[2] as i32)
    }

    pub fn read_i32(&mut self) -> Result<i32, PacketError> {
        let b = self.take(4)?;
        Ok(i32::from_be_bytes([b[0], b[1], b[2], b[3]]))
    }
}
