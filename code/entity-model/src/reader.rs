#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ModelError {
    Bounds,
    Null,
    Invalid,
}

impl std::fmt::Display for ModelError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Bounds => f.write_str("index out of bounds"),
            Self::Null => f.write_str("missing array"),
            Self::Invalid => f.write_str("invalid data"),
        }
    }
}

impl std::error::Error for ModelError {}

pub type R<T> = Result<T, ModelError>;

pub fn at<T: Copy>(values: &Option<Vec<T>>, index: i64) -> R<T> {
    let list = values.as_ref().ok_or(ModelError::Null)?;
    if index < 0 {
        return Err(ModelError::Bounds);
    }
    list.get(index as usize).copied().ok_or(ModelError::Bounds)
}

pub fn put<T>(values: &mut Option<Vec<T>>, index: i64, value: T) -> R<()> {
    let list = values.as_mut().ok_or(ModelError::Null)?;
    if index < 0 {
        return Err(ModelError::Bounds);
    }
    let slot = list.get_mut(index as usize).ok_or(ModelError::Bounds)?;
    *slot = value;
    Ok(())
}

pub fn modify<T: Copy>(values: &mut Option<Vec<T>>, index: i64, f: impl FnOnce(T) -> T) -> R<()> {
    let current = at(values, index)?;
    put(values, index, f(current))
}

pub fn at_slice<T: Copy>(values: &[T], index: i64) -> R<T> {
    if index < 0 {
        return Err(ModelError::Bounds);
    }
    values
        .get(index as usize)
        .copied()
        .ok_or(ModelError::Bounds)
}

pub fn put_slice<T>(values: &mut [T], index: i64, value: T) -> R<()> {
    if index < 0 {
        return Err(ModelError::Bounds);
    }
    let slot = values.get_mut(index as usize).ok_or(ModelError::Bounds)?;
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

    pub fn i8(&mut self) -> R<i32> {
        let value = at_slice(self.data, self.pos)?;
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
        let high = self.u16()?;
        let low = self.u16()?;
        Ok((high << 16) | low)
    }

    pub fn string(&mut self) -> R<Vec<u8>> {
        let mut out = Vec::new();
        loop {
            let byte = at_slice(self.data, self.pos)?;
            self.pos += 1;
            if byte == 0 {
                return Ok(out);
            }
            out.push(byte);
        }
    }

    pub fn smart(&mut self) -> R<i32> {
        let first = at_slice(self.data, self.pos)? as i32;
        if first < 128 {
            Ok(self.u8()? - 64)
        } else {
            Ok(self.u16()? - 49152)
        }
    }

    pub fn big_smart(&mut self) -> R<i32> {
        let first = at_slice(self.data, self.pos)? as i8;
        if first < 0 {
            Ok(self.i32()? & 0x7fff_ffff)
        } else {
            let value = self.u16()?;
            Ok(if value == 32767 { -1 } else { value })
        }
    }

    pub fn usmart(&mut self) -> R<i32> {
        let first = at_slice(self.data, self.pos)? as i32;
        if first < 128 {
            self.u8()
        } else {
            Ok(self.u16()? - 32768)
        }
    }
}
