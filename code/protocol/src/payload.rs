use std::fmt;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PayloadError {
    UnexpectedEof,
    OutputFull { needed: usize, remaining: usize },
    OutOfRange { value: i64, min: i64, max: i64 },
    InvalidBitCount(u8),
    InBitMode,
    NotInBitMode,
    InvalidStringPrefix(u8),
    EmbeddedNul,
    AllocationFailed,
}

impl fmt::Display for PayloadError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::UnexpectedEof => f.write_str("truncated packet payload"),
            Self::OutputFull { needed, remaining } => {
                write!(
                    f,
                    "payload write needs {needed} bytes, only {remaining} remain"
                )
            }
            Self::OutOfRange { value, min, max } => {
                write!(f, "payload value {value} is outside {min}..={max}")
            }
            Self::InvalidBitCount(count) => write!(f, "bit count {count} is outside 1..=32"),
            Self::InBitMode => f.write_str("byte access is unavailable during bit access"),
            Self::NotInBitMode => f.write_str("bit access has not started"),
            Self::InvalidStringPrefix(byte) => write!(f, "JagString prefix {byte} is not zero"),
            Self::EmbeddedNul => f.write_str("packet string contains a null character"),
            Self::AllocationFailed => f.write_str("packet string allocation failed"),
        }
    }
}

impl std::error::Error for PayloadError {}

#[derive(Debug, Clone)]
pub struct PayloadReader<'a> {
    input: &'a [u8],
    position: usize,
    bit_cursor: Option<(usize, u8)>,
}

impl<'a> PayloadReader<'a> {
    pub fn new(input: &'a [u8]) -> Self {
        Self {
            input,
            position: 0,
            bit_cursor: None,
        }
    }

    pub fn position(&self) -> usize {
        self.position
    }

    pub fn remaining(&self) -> usize {
        self.input.len() - self.position
    }

    pub fn is_bit_access(&self) -> bool {
        self.bit_cursor.is_some()
    }

    pub fn remaining_bits(&self) -> usize {
        match self.bit_cursor {
            Some((byte, offset)) => (self.input.len() - byte) * 8 - usize::from(offset),
            None => self.remaining() * 8,
        }
    }

    fn byte_access(&self) -> Result<(), PayloadError> {
        if self.is_bit_access() {
            Err(PayloadError::InBitMode)
        } else {
            Ok(())
        }
    }

    pub fn read_bytes(&mut self, count: usize) -> Result<&'a [u8], PayloadError> {
        self.byte_access()?;
        if count > self.remaining() {
            return Err(PayloadError::UnexpectedEof);
        }
        let start = self.position;
        self.position += count;
        Ok(&self.input[start..self.position])
    }

    fn array<const N: usize>(&mut self) -> Result<[u8; N], PayloadError> {
        let mut bytes = [0; N];
        bytes.copy_from_slice(self.read_bytes(N)?);
        Ok(bytes)
    }

    pub fn read_u8(&mut self) -> Result<u8, PayloadError> {
        Ok(self.array::<1>()?[0])
    }
    pub fn read_i8(&mut self) -> Result<i8, PayloadError> {
        Ok(self.read_u8()? as i8)
    }
    pub fn read_u8_a(&mut self) -> Result<u8, PayloadError> {
        Ok(self.read_u8()?.wrapping_sub(128))
    }
    pub fn read_u8_c(&mut self) -> Result<u8, PayloadError> {
        Ok(self.read_u8()?.wrapping_neg())
    }
    pub fn read_u8_s(&mut self) -> Result<u8, PayloadError> {
        Ok(128u8.wrapping_sub(self.read_u8()?))
    }
    pub fn read_i8_a(&mut self) -> Result<i8, PayloadError> {
        Ok(self.read_u8_a()? as i8)
    }
    pub fn read_i8_c(&mut self) -> Result<i8, PayloadError> {
        Ok(self.read_u8_c()? as i8)
    }
    pub fn read_i8_s(&mut self) -> Result<i8, PayloadError> {
        Ok(self.read_u8_s()? as i8)
    }
    pub fn read_u16_be(&mut self) -> Result<u16, PayloadError> {
        Ok(u16::from_be_bytes(self.array()?))
    }
    pub fn read_u16_le(&mut self) -> Result<u16, PayloadError> {
        Ok(u16::from_le_bytes(self.array()?))
    }
    pub fn read_u16_be_a(&mut self) -> Result<u16, PayloadError> {
        let [high, low] = self.array()?;
        Ok(u16::from_be_bytes([high, low.wrapping_sub(128)]))
    }
    pub fn read_u16_le_a(&mut self) -> Result<u16, PayloadError> {
        let [low, high] = self.array()?;
        Ok(u16::from_le_bytes([low.wrapping_sub(128), high]))
    }
    pub fn read_i16_be(&mut self) -> Result<i16, PayloadError> {
        Ok(self.read_u16_be()? as i16)
    }
    pub fn read_i16_le(&mut self) -> Result<i16, PayloadError> {
        Ok(self.read_u16_le()? as i16)
    }
    pub fn read_i16_be_a(&mut self) -> Result<i16, PayloadError> {
        Ok(self.read_u16_be_a()? as i16)
    }
    pub fn read_i16_le_a(&mut self) -> Result<i16, PayloadError> {
        Ok(self.read_u16_le_a()? as i16)
    }
    pub fn read_u24_be(&mut self) -> Result<u32, PayloadError> {
        let [a, b, c] = self.array()?;
        Ok(u32::from_be_bytes([0, a, b, c]))
    }
    pub fn read_i24_be(&mut self) -> Result<i32, PayloadError> {
        Ok(((self.read_u24_be()? << 8) as i32) >> 8)
    }
    pub fn read_u32_be(&mut self) -> Result<u32, PayloadError> {
        Ok(u32::from_be_bytes(self.array()?))
    }
    pub fn read_u32_le(&mut self) -> Result<u32, PayloadError> {
        Ok(u32::from_le_bytes(self.array()?))
    }
    pub fn read_u32_middle(&mut self) -> Result<u32, PayloadError> {
        let [b, a, d, c] = self.array()?;
        Ok(u32::from_be_bytes([d, c, b, a]))
    }
    pub fn read_u32_inverse_middle(&mut self) -> Result<u32, PayloadError> {
        let [c, d, a, b] = self.array()?;
        Ok(u32::from_be_bytes([d, c, b, a]))
    }
    pub fn read_i32_be(&mut self) -> Result<i32, PayloadError> {
        Ok(self.read_u32_be()? as i32)
    }
    pub fn read_i32_le(&mut self) -> Result<i32, PayloadError> {
        Ok(self.read_u32_le()? as i32)
    }
    pub fn read_i32_middle(&mut self) -> Result<i32, PayloadError> {
        Ok(self.read_u32_middle()? as i32)
    }
    pub fn read_i32_inverse_middle(&mut self) -> Result<i32, PayloadError> {
        Ok(self.read_u32_inverse_middle()? as i32)
    }
    pub fn read_u64_be(&mut self) -> Result<u64, PayloadError> {
        Ok(u64::from_be_bytes(self.array()?))
    }
    pub fn read_i64_be(&mut self) -> Result<i64, PayloadError> {
        Ok(self.read_u64_be()? as i64)
    }

    fn peek(&self) -> Result<u8, PayloadError> {
        self.byte_access()?;
        self.input
            .get(self.position)
            .copied()
            .ok_or(PayloadError::UnexpectedEof)
    }

    pub fn read_smart(&mut self) -> Result<i16, PayloadError> {
        if self.peek()? < 128 {
            Ok(i16::from(self.read_u8()?) - 64)
        } else {
            Ok((i32::from(self.read_u16_be()?) - 49152) as i16)
        }
    }

    pub fn read_u_smart(&mut self) -> Result<u16, PayloadError> {
        if self.peek()? < 128 {
            Ok(u16::from(self.read_u8()?))
        } else {
            Ok(self.read_u16_be()? - 32768)
        }
    }

    pub fn read_big_smart(&mut self) -> Result<i32, PayloadError> {
        if self.peek()? >= 128 {
            Ok((self.read_u32_be()? & 0x7fff_ffff) as i32)
        } else {
            let value = self.read_u16_be()?;
            Ok(if value == 32767 { -1 } else { i32::from(value) })
        }
    }

    pub fn read_u_big_smart(&mut self) -> Result<u32, PayloadError> {
        if self.peek()? >= 128 {
            Ok(self.read_u32_be()? & 0x7fff_ffff)
        } else {
            Ok(u32::from(self.read_u16_be()?))
        }
    }

    pub fn read_cp1252(&mut self) -> Result<String, PayloadError> {
        self.string(false)
    }

    pub fn read_nullable_cp1252(&mut self) -> Result<Option<String>, PayloadError> {
        if self.peek()? == 0 {
            self.position += 1;
            Ok(None)
        } else {
            self.read_cp1252().map(Some)
        }
    }

    pub fn read_jag_cp1252(&mut self) -> Result<String, PayloadError> {
        self.string(true)
    }

    fn string(&mut self, jag: bool) -> Result<String, PayloadError> {
        let prefix = self.peek()?;
        if jag && prefix != 0 {
            return Err(PayloadError::InvalidStringPrefix(prefix));
        }
        let start = self.position + usize::from(jag);
        let length = self.input[start..]
            .iter()
            .position(|&b| b == 0)
            .ok_or(PayloadError::UnexpectedEof)?;
        let mut text = String::new();
        text.try_reserve(
            length
                .checked_mul(3)
                .ok_or(PayloadError::AllocationFailed)?,
        )
        .map_err(|_| PayloadError::AllocationFailed)?;
        for &byte in &self.input[start..start + length] {
            text.push(decode_cp1252(byte));
        }
        self.position = start + length + 1;
        Ok(text)
    }

    pub fn start_bit_access(&mut self) -> Result<(), PayloadError> {
        self.byte_access()?;
        self.bit_cursor = Some((self.position, 0));
        Ok(())
    }

    pub fn read_bits(&mut self, count: u8) -> Result<u32, PayloadError> {
        let (mut byte, mut offset) = self.bit_cursor.ok_or(PayloadError::NotInBitMode)?;
        if !(1..=32).contains(&count) {
            return Err(PayloadError::InvalidBitCount(count));
        }
        let needed = (usize::from(offset) + usize::from(count)).div_ceil(8);
        if needed > self.input.len() - byte {
            return Err(PayloadError::UnexpectedEof);
        }
        let mut value = 0u32;
        let mut remaining = count;
        while remaining > 0 {
            let take = remaining.min(8 - offset);
            let shift = 8 - offset - take;
            let mask = (1u16 << take) - 1;
            value = (value << take) | u32::from((u16::from(self.input[byte]) >> shift) & mask);
            remaining -= take;
            offset += take;
            if offset == 8 {
                byte += 1;
                offset = 0;
            }
        }
        self.bit_cursor = Some((byte, offset));
        Ok(value)
    }

    pub fn finish_bit_access(&mut self) -> Result<(), PayloadError> {
        let (byte, offset) = self.bit_cursor.take().ok_or(PayloadError::NotInBitMode)?;
        self.position = byte + usize::from(offset != 0);
        Ok(())
    }
}

#[derive(Debug)]
pub struct PayloadWriter<'a> {
    output: &'a mut [u8],
    position: usize,
}

impl<'a> PayloadWriter<'a> {
    pub fn new(output: &'a mut [u8]) -> Self {
        Self {
            output,
            position: 0,
        }
    }
    pub fn position(&self) -> usize {
        self.position
    }
    pub fn remaining(&self) -> usize {
        self.output.len() - self.position
    }
    pub fn written(&self) -> &[u8] {
        &self.output[..self.position]
    }

    fn ensure(&self, needed: usize) -> Result<(), PayloadError> {
        if needed > self.remaining() {
            Err(PayloadError::OutputFull {
                needed,
                remaining: self.remaining(),
            })
        } else {
            Ok(())
        }
    }

    pub fn write_bytes(&mut self, bytes: &[u8]) -> Result<(), PayloadError> {
        self.ensure(bytes.len())?;
        let end = self.position + bytes.len();
        self.output[self.position..end].copy_from_slice(bytes);
        self.position = end;
        Ok(())
    }

    pub fn write_u8(&mut self, value: u8) -> Result<(), PayloadError> {
        self.write_bytes(&[value])
    }
    pub fn write_i8(&mut self, value: i8) -> Result<(), PayloadError> {
        self.write_u8(value as u8)
    }
    pub fn write_u8_a(&mut self, value: u8) -> Result<(), PayloadError> {
        self.write_u8(value.wrapping_add(128))
    }
    pub fn write_u8_c(&mut self, value: u8) -> Result<(), PayloadError> {
        self.write_u8(value.wrapping_neg())
    }
    pub fn write_u8_s(&mut self, value: u8) -> Result<(), PayloadError> {
        self.write_u8(128u8.wrapping_sub(value))
    }
    pub fn write_i8_a(&mut self, value: i8) -> Result<(), PayloadError> {
        self.write_u8_a(value as u8)
    }
    pub fn write_i8_c(&mut self, value: i8) -> Result<(), PayloadError> {
        self.write_u8_c(value as u8)
    }
    pub fn write_i8_s(&mut self, value: i8) -> Result<(), PayloadError> {
        self.write_u8_s(value as u8)
    }
    pub fn write_u16_be(&mut self, value: u16) -> Result<(), PayloadError> {
        self.write_bytes(&value.to_be_bytes())
    }
    pub fn write_u16_le(&mut self, value: u16) -> Result<(), PayloadError> {
        self.write_bytes(&value.to_le_bytes())
    }
    pub fn write_u16_be_a(&mut self, value: u16) -> Result<(), PayloadError> {
        let [high, low] = value.to_be_bytes();
        self.write_bytes(&[high, low.wrapping_add(128)])
    }
    pub fn write_u16_le_a(&mut self, value: u16) -> Result<(), PayloadError> {
        let [low, high] = value.to_le_bytes();
        self.write_bytes(&[low.wrapping_add(128), high])
    }
    pub fn write_i16_be(&mut self, value: i16) -> Result<(), PayloadError> {
        self.write_u16_be(value as u16)
    }
    pub fn write_i16_le(&mut self, value: i16) -> Result<(), PayloadError> {
        self.write_u16_le(value as u16)
    }
    pub fn write_i16_be_a(&mut self, value: i16) -> Result<(), PayloadError> {
        self.write_u16_be_a(value as u16)
    }
    pub fn write_i16_le_a(&mut self, value: i16) -> Result<(), PayloadError> {
        self.write_u16_le_a(value as u16)
    }
    pub fn write_u24_be(&mut self, value: u32) -> Result<(), PayloadError> {
        range(i64::from(value), 0, 0xff_ffff)?;
        self.write_bytes(&value.to_be_bytes()[1..])
    }
    pub fn write_i24_be(&mut self, value: i32) -> Result<(), PayloadError> {
        range(i64::from(value), -0x80_0000, 0x7f_ffff)?;
        self.write_bytes(&value.to_be_bytes()[1..])
    }
    pub fn write_u32_be(&mut self, value: u32) -> Result<(), PayloadError> {
        self.write_bytes(&value.to_be_bytes())
    }
    pub fn write_u32_le(&mut self, value: u32) -> Result<(), PayloadError> {
        self.write_bytes(&value.to_le_bytes())
    }
    pub fn write_u32_middle(&mut self, value: u32) -> Result<(), PayloadError> {
        let [d, c, b, a] = value.to_be_bytes();
        self.write_bytes(&[b, a, d, c])
    }
    pub fn write_u32_inverse_middle(&mut self, value: u32) -> Result<(), PayloadError> {
        let [d, c, b, a] = value.to_be_bytes();
        self.write_bytes(&[c, d, a, b])
    }
    pub fn write_i32_be(&mut self, value: i32) -> Result<(), PayloadError> {
        self.write_u32_be(value as u32)
    }
    pub fn write_i32_le(&mut self, value: i32) -> Result<(), PayloadError> {
        self.write_u32_le(value as u32)
    }
    pub fn write_i32_middle(&mut self, value: i32) -> Result<(), PayloadError> {
        self.write_u32_middle(value as u32)
    }
    pub fn write_i32_inverse_middle(&mut self, value: i32) -> Result<(), PayloadError> {
        self.write_u32_inverse_middle(value as u32)
    }
    pub fn write_u64_be(&mut self, value: u64) -> Result<(), PayloadError> {
        self.write_bytes(&value.to_be_bytes())
    }
    pub fn write_i64_be(&mut self, value: i64) -> Result<(), PayloadError> {
        self.write_u64_be(value as u64)
    }

    pub fn write_u_smart(&mut self, value: u16) -> Result<(), PayloadError> {
        range(i64::from(value), 0, 32767)?;
        if value < 128 {
            self.write_u8(value as u8)
        } else {
            self.write_u16_be(value + 32768)
        }
    }

    pub fn write_cp1252(&mut self, text: &str) -> Result<(), PayloadError> {
        self.string(text, false)
    }
    pub fn write_jag_cp1252(&mut self, text: &str) -> Result<(), PayloadError> {
        self.string(text, true)
    }

    fn string(&mut self, text: &str, jag: bool) -> Result<(), PayloadError> {
        if text.contains('\0') {
            return Err(PayloadError::EmbeddedNul);
        }
        let count = text.encode_utf16().count();
        let needed = count
            .checked_add(1 + usize::from(jag))
            .ok_or(PayloadError::AllocationFailed)?;
        self.ensure(needed)?;
        if jag {
            self.output[self.position] = 0;
            self.position += 1;
        }
        for unit in text.encode_utf16() {
            self.output[self.position] = encode_cp1252(unit);
            self.position += 1;
        }
        self.output[self.position] = 0;
        self.position += 1;
        Ok(())
    }
}

fn range(value: i64, min: i64, max: i64) -> Result<(), PayloadError> {
    if value < min || value > max {
        Err(PayloadError::OutOfRange { value, min, max })
    } else {
        Ok(())
    }
}

const CP1252: [char; 32] = [
    '€', '?', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '?', 'Ž', '?', '?', '‘', '’',
    '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '?', 'ž', 'Ÿ',
];

fn decode_cp1252(byte: u8) -> char {
    if (128..160).contains(&byte) {
        CP1252[usize::from(byte - 128)]
    } else {
        char::from(byte)
    }
}

fn encode_cp1252(unit: u16) -> u8 {
    if (1..128).contains(&unit) || (160..=255).contains(&unit) {
        unit as u8
    } else {
        CP1252
            .iter()
            .position(|&ch| u32::from(ch) == u32::from(unit))
            .map_or(b'?', |index| index as u8 + 128)
    }
}
