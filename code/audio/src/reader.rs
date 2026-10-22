#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AudioError {
    Bounds,
    Invalid,
}

impl std::fmt::Display for AudioError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Bounds => f.write_str("index out of bounds"),
            Self::Invalid => f.write_str("invalid data"),
        }
    }
}

impl std::error::Error for AudioError {}

pub type R<T> = Result<T, AudioError>;

pub fn at<T: Copy>(values: &[T], index: i64) -> R<T> {
    if index < 0 {
        return Err(AudioError::Bounds);
    }
    values
        .get(index as usize)
        .copied()
        .ok_or(AudioError::Bounds)
}

pub fn put<T>(values: &mut [T], index: i64, value: T) -> R<()> {
    if index < 0 {
        return Err(AudioError::Bounds);
    }
    let slot = values.get_mut(index as usize).ok_or(AudioError::Bounds)?;
    *slot = value;
    Ok(())
}

#[derive(Clone)]
pub struct Buf<'a> {
    data: &'a [u8],
    pub pos: i64,
}

impl<'a> Buf<'a> {
    pub fn new(data: &'a [u8]) -> Self {
        Self { data, pos: 0 }
    }

    pub fn len(&self) -> usize {
        self.data.len()
    }

    pub fn is_empty(&self) -> bool {
        self.data.is_empty()
    }

    pub fn data(&self) -> &'a [u8] {
        self.data
    }

    pub fn peek(&self) -> R<i32> {
        Ok(i32::from(at(self.data, self.pos)? as i8))
    }

    pub fn i8(&mut self) -> R<i32> {
        let value = at(self.data, self.pos)?;
        self.pos += 1;
        Ok(i32::from(value as i8))
    }

    pub fn u8(&mut self) -> R<i32> {
        Ok(self.i8()? & 0xff)
    }

    pub fn u16(&mut self) -> R<i32> {
        let high = self.u8()?;
        let low = self.u8()?;
        Ok((high << 8) | low)
    }

    pub fn i16(&mut self) -> R<i32> {
        let value = self.u16()?;
        Ok(if value > 32767 { value - 65536 } else { value })
    }

    pub fn u24(&mut self) -> R<i32> {
        let high = self.u8()?;
        let middle = self.u8()?;
        let low = self.u8()?;
        Ok((high << 16) | (middle << 8) | low)
    }

    pub fn i32(&mut self) -> R<i32> {
        let a = self.u8()?;
        let b = self.u8()?;
        let c = self.u8()?;
        let d = self.u8()?;
        Ok((a << 24) | (b << 16) | (c << 8) | d)
    }

    pub fn smart(&mut self) -> R<i32> {
        let first = i32::from(at(self.data, self.pos)?);
        if first < 128 {
            Ok(self.u8()? - 64)
        } else {
            Ok(self.u16()? - 49152)
        }
    }

    pub fn var_int(&mut self) -> R<i32> {
        let mut value = 0i32;
        let mut byte;
        loop {
            byte = self.i8()?;
            value = (value | (byte & 0x7f)).wrapping_shl(7);
            if byte >= 0 {
                break;
            }
        }
        value >>= 7;
        Ok(value | byte)
    }

    pub fn usmart(&mut self) -> R<i32> {
        let first = i32::from(at(self.data, self.pos)?);
        if first < 128 {
            self.u8()
        } else {
            Ok(self.u16()? - 32768)
        }
    }
}

pub struct Writer {
    data: Vec<u8>,
    pub pos: i64,
}

impl Writer {
    pub fn new(size: i32) -> R<Self> {
        if size < 0 {
            return Err(AudioError::Invalid);
        }
        Ok(Self {
            data: vec![0; size as usize],
            pos: 0,
        })
    }

    pub fn into_data(self) -> Vec<u8> {
        self.data
    }

    pub fn byte(&mut self, value: i32) -> R<()> {
        put(&mut self.data, self.pos, value as u8)?;
        self.pos += 1;
        Ok(())
    }

    pub fn short(&mut self, value: i32) -> R<()> {
        self.byte(value >> 8)?;
        self.byte(value)
    }

    pub fn int(&mut self, value: i32) -> R<()> {
        self.byte(value >> 24)?;
        self.byte(value >> 16)?;
        self.byte(value >> 8)?;
        self.byte(value)
    }

    pub fn flags(&mut self, value: i32) -> R<()> {
        let bits = value as u32;
        if value & -128 != 0 {
            if value & -16384 != 0 {
                if value & -2_097_152 != 0 {
                    if value & -268_435_456 != 0 {
                        self.byte((bits >> 28) as i32 | 0x80)?;
                    }
                    self.byte((bits >> 21) as i32 | 0x80)?;
                }
                self.byte((bits >> 14) as i32 | 0x80)?;
            }
            self.byte((bits >> 7) as i32 | 0x80)?;
        }
        self.byte(value & 0x7f)
    }

    pub fn int_length(&mut self, length: i32) -> R<()> {
        let base = self.pos - i64::from(length);
        put(&mut self.data, base - 4, (length >> 24) as u8)?;
        put(&mut self.data, base - 3, (length >> 16) as u8)?;
        put(&mut self.data, base - 2, (length >> 8) as u8)?;
        put(&mut self.data, base - 1, length as u8)
    }
}
