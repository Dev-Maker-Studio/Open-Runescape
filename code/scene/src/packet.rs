#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PacketError {
    Truncated,
    ParameterCapacity,
    ZeroOddsSum,
}

impl std::fmt::Display for PacketError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Truncated => f.write_str("truncated packet"),
            Self::ParameterCapacity => f.write_str("parameter table has no capacity"),
            Self::ZeroOddsSum => f.write_str("animation odds sum to zero"),
        }
    }
}

impl std::error::Error for PacketError {}

#[derive(Debug, Clone)]
pub struct Packet<'a> {
    data: &'a [u8],
    position: usize,
}

const CP1252_HIGH: [char; 32] = [
    '\u{20ac}', '?', '\u{201a}', '\u{0192}', '\u{201e}', '\u{2026}', '\u{2020}', '\u{2021}',
    '\u{02c6}', '\u{2030}', '\u{0160}', '\u{2039}', '\u{0152}', '?', '\u{017d}', '?', '?',
    '\u{2018}', '\u{2019}', '\u{201c}', '\u{201d}', '\u{2022}', '\u{2013}', '\u{2014}', '\u{02dc}',
    '\u{2122}', '\u{0161}', '\u{203a}', '\u{0153}', '?', '\u{017e}', '\u{0178}',
];

impl<'a> Packet<'a> {
    pub fn new(data: &'a [u8]) -> Self {
        Self { data, position: 0 }
    }

    pub fn position(&self) -> usize {
        self.position
    }

    pub fn remaining(&self) -> usize {
        self.data.len() - self.position
    }

    fn peek(&self) -> Result<u8, PacketError> {
        self.data
            .get(self.position)
            .copied()
            .ok_or(PacketError::Truncated)
    }

    pub fn u8(&mut self) -> Result<i32, PacketError> {
        let value = self.peek()?;
        self.position += 1;
        Ok(i32::from(value))
    }

    pub fn i8(&mut self) -> Result<i32, PacketError> {
        Ok(i32::from(self.u8()? as u8 as i8))
    }

    pub fn u16(&mut self) -> Result<i32, PacketError> {
        let high = self.u8()?;
        let low = self.u8()?;
        Ok((high << 8) | low)
    }

    pub fn i16(&mut self) -> Result<i32, PacketError> {
        Ok(i32::from(self.u16()? as u16 as i16))
    }

    pub fn u24(&mut self) -> Result<i32, PacketError> {
        let high = self.u8()?;
        let middle = self.u8()?;
        let low = self.u8()?;
        Ok((high << 16) | (middle << 8) | low)
    }

    pub fn i32(&mut self) -> Result<i32, PacketError> {
        let a = self.u8()?;
        let b = self.u8()?;
        let c = self.u8()?;
        let d = self.u8()?;
        Ok((a << 24) | (b << 16) | (c << 8) | d)
    }

    pub fn string(&mut self) -> Result<String, PacketError> {
        let start = self.position;
        let length = self.data[start..]
            .iter()
            .position(|byte| *byte == 0)
            .ok_or(PacketError::Truncated)?;
        self.position = start + length + 1;
        Ok(self.data[start..start + length]
            .iter()
            .map(|byte| match *byte {
                128..=159 => CP1252_HIGH[usize::from(*byte - 128)],
                other => char::from(other),
            })
            .collect())
    }

    pub fn usmart(&mut self) -> Result<i32, PacketError> {
        if self.peek()? < 128 {
            self.u8()
        } else {
            Ok(self.u16()? - 32768)
        }
    }

    pub fn big_smart(&mut self) -> Result<i32, PacketError> {
        if self.peek()? >= 128 {
            return Ok(self.i32()? & 0x7fff_ffff);
        }
        let value = self.u16()?;
        Ok(if value == 32767 { -1 } else { value })
    }

    pub fn sum(&mut self) -> Result<i32, PacketError> {
        let mut total = 0i32;
        loop {
            let value = self.usmart()?;
            total = total.wrapping_add(value);
            if value != 32767 {
                return Ok(total);
            }
        }
    }
}
