use std::fmt;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ClientPacketError {
    NullCharacter,
    SmartOutOfRange(i32),
    VariableLongSize(i32),
    TooLong(usize),
    ClickType(i32),
    Option(i32),
    UnsupportedSymbol(u8),
    UnknownValueType(i32),
}

impl fmt::Display for ClientPacketError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::NullCharacter => f.write_str("string contains a null character"),
            Self::SmartOutOfRange(v) => write!(f, "smart value {v} out of range"),
            Self::VariableLongSize(v) => write!(f, "variable length long size {v} out of range"),
            Self::TooLong(v) => write!(f, "body of {v} bytes does not fit the length prefix"),
            Self::ClickType(v) => write!(f, "click type {v} has no client packet"),
            Self::Option(v) => write!(f, "option {v} out of range"),
            Self::UnsupportedSymbol(v) => write!(f, "symbol {v} has no huffman code"),
            Self::UnknownValueType(v) => write!(f, "quick chat value type {v} is unknown"),
        }
    }
}

impl std::error::Error for ClientPacketError {}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct Text(pub Vec<u16>);

impl From<&str> for Text {
    fn from(value: &str) -> Self {
        Self(value.encode_utf16().collect())
    }
}

impl From<String> for Text {
    fn from(value: String) -> Self {
        Self(value.encode_utf16().collect())
    }
}

impl From<Vec<u16>> for Text {
    fn from(value: Vec<u16>) -> Self {
        Self(value)
    }
}

impl From<&[u16]> for Text {
    fn from(value: &[u16]) -> Self {
        Self(value.to_vec())
    }
}

impl From<&Text> for Text {
    fn from(value: &Text) -> Self {
        value.clone()
    }
}

impl Text {
    pub fn len(&self) -> usize {
        self.0.len()
    }

    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }
}

const CP1252_HIGH: [(u16, u8); 27] = [
    (8364, 0x80),
    (8218, 0x82),
    (402, 0x83),
    (8222, 0x84),
    (8230, 0x85),
    (8224, 0x86),
    (8225, 0x87),
    (710, 0x88),
    (8240, 0x89),
    (352, 0x8a),
    (8249, 0x8b),
    (338, 0x8c),
    (381, 0x8e),
    (8216, 0x91),
    (8217, 0x92),
    (8220, 0x93),
    (8221, 0x94),
    (8226, 0x95),
    (8211, 0x96),
    (8212, 0x97),
    (732, 0x98),
    (8482, 0x99),
    (353, 0x9a),
    (8250, 0x9b),
    (339, 0x9c),
    (382, 0x9e),
    (376, 0x9f),
];

pub fn char_to_byte(unit: u16) -> u8 {
    if (1..128).contains(&unit) || (160..256).contains(&unit) {
        return unit as u8;
    }
    CP1252_HIGH
        .iter()
        .find(|(u, _)| *u == unit)
        .map_or(63, |(_, b)| *b)
}

pub fn string_body_length(text: &Text) -> usize {
    text.len() + 1
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct Writer(pub Vec<u8>);

impl Writer {
    pub fn new() -> Self {
        Self(Vec::new())
    }

    pub fn into_bytes(self) -> Vec<u8> {
        self.0
    }

    pub fn len(&self) -> usize {
        self.0.len()
    }

    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }

    pub fn byte(&mut self, v: i32) -> &mut Self {
        self.0.push(v as u8);
        self
    }

    pub fn byte_inverse(&mut self, v: i32) -> &mut Self {
        self.byte(v.wrapping_neg())
    }

    pub fn byte_add(&mut self, v: i32) -> &mut Self {
        self.byte(v.wrapping_add(128))
    }

    pub fn byte_subtract(&mut self, v: i32) -> &mut Self {
        self.byte(128i32.wrapping_sub(v))
    }

    pub fn short(&mut self, v: i32) -> &mut Self {
        self.byte(v >> 8).byte(v)
    }

    pub fn short_add(&mut self, v: i32) -> &mut Self {
        self.byte(v >> 8).byte_add(v)
    }

    pub fn short_little(&mut self, v: i32) -> &mut Self {
        self.byte(v).byte(v >> 8)
    }

    pub fn short_add_little(&mut self, v: i32) -> &mut Self {
        self.byte_add(v).byte(v >> 8)
    }

    pub fn medium(&mut self, v: i32) -> &mut Self {
        self.byte(v >> 16).byte(v >> 8).byte(v)
    }

    pub fn int(&mut self, v: i32) -> &mut Self {
        self.byte(v >> 24).byte(v >> 16).byte(v >> 8).byte(v)
    }

    pub fn int_little(&mut self, v: i32) -> &mut Self {
        self.byte(v).byte(v >> 8).byte(v >> 16).byte(v >> 24)
    }

    pub fn int_middle(&mut self, v: i32) -> &mut Self {
        self.byte(v >> 8).byte(v).byte(v >> 24).byte(v >> 16)
    }

    pub fn int_inverse_middle(&mut self, v: i32) -> &mut Self {
        self.byte(v >> 16).byte(v >> 24).byte(v).byte(v >> 8)
    }

    pub fn smart(&mut self, v: i32) -> Result<&mut Self, ClientPacketError> {
        if (0..128).contains(&v) {
            Ok(self.byte(v))
        } else if (128..32768).contains(&v) {
            Ok(self.short(v + 32768))
        } else {
            Err(ClientPacketError::SmartOutOfRange(v))
        }
    }

    pub fn variable_length_long(
        &mut self,
        value: i64,
        size: i32,
    ) -> Result<&mut Self, ClientPacketError> {
        if !(1..=8).contains(&size) {
            return Err(ClientPacketError::VariableLongSize(size));
        }
        let mut shift = (size - 1) * 8;
        while shift >= 0 {
            self.byte((value >> shift) as i32);
            shift -= 8;
        }
        Ok(self)
    }

    pub fn string(&mut self, text: impl Into<Text>) -> Result<&mut Self, ClientPacketError> {
        let text = text.into();
        if text.0.contains(&0) {
            return Err(ClientPacketError::NullCharacter);
        }
        for unit in &text.0 {
            self.0.push(char_to_byte(*unit));
        }
        self.0.push(0);
        Ok(self)
    }

    pub fn jag_string(&mut self, text: impl Into<Text>) -> Result<&mut Self, ClientPacketError> {
        let text = text.into();
        if text.0.contains(&0) {
            return Err(ClientPacketError::NullCharacter);
        }
        self.0.push(0);
        for unit in &text.0 {
            self.0.push(char_to_byte(*unit));
        }
        self.0.push(0);
        Ok(self)
    }

    pub fn raw(&mut self, bytes: &[u8]) -> &mut Self {
        self.0.extend_from_slice(bytes);
        self
    }
}
