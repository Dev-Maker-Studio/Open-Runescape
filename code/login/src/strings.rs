use crate::{Error, MAX_PAYLOAD_BYTES, SensitiveBytes, SensitiveString, buffer};
use zeroize::Zeroizing;

const EXTENDED: [char; 32] = [
    '€', '\0', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '\0', 'Ž', '\0', '\0', '‘',
    '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\0', 'ž', 'Ÿ',
];

pub struct DecodedString {
    pub value: SensitiveString,
    pub consumed: usize,
}

pub fn encode(value: &str, max_content_bytes: usize) -> Result<SensitiveBytes, Error> {
    encode_with_prefix(value, max_content_bytes, false)
}

pub fn encode_jag(value: &str, max_content_bytes: usize) -> Result<SensitiveBytes, Error> {
    encode_with_prefix(value, max_content_bytes, true)
}

pub fn decode(bytes: &[u8], max_content_bytes: usize) -> Result<DecodedString, Error> {
    decode_with_prefix(bytes, max_content_bytes, false)
}

pub fn decode_jag(bytes: &[u8], max_content_bytes: usize) -> Result<DecodedString, Error> {
    decode_with_prefix(bytes, max_content_bytes, true)
}

pub(crate) fn validate(value: &str, max_content_bytes: usize) -> Result<usize, Error> {
    validate_limit(max_content_bytes)?;
    let mut length = 0;
    for character in value.chars() {
        if length == max_content_bytes {
            return Err(Error::LengthLimitExceeded);
        }
        encode_char(character)?;
        length += 1;
    }
    Ok(length)
}

pub(crate) fn append(bytes: &mut Vec<u8>, value: &str) -> Result<(), Error> {
    for character in value.chars() {
        bytes.push(encode_char(character)?);
    }
    bytes.push(0);
    Ok(())
}

fn encode_with_prefix(value: &str, limit: usize, jag: bool) -> Result<SensitiveBytes, Error> {
    let length = validate(value, limit)?;
    let mut bytes = buffer(length + 1 + usize::from(jag))?;
    if jag {
        bytes.push(0);
    }
    append(&mut bytes, value)?;
    Ok(SensitiveBytes(bytes))
}

fn decode_with_prefix(bytes: &[u8], limit: usize, jag: bool) -> Result<DecodedString, Error> {
    validate_limit(limit)?;
    let prefix = usize::from(jag);
    let bytes = if jag {
        match bytes.split_first() {
            Some((0, remaining)) => remaining,
            _ => return Err(Error::InvalidJagPrefix),
        }
    } else {
        bytes
    };
    let length = bytes
        .iter()
        .take(limit + 1)
        .position(|&byte| byte == 0)
        .ok_or(if bytes.len() > limit {
            Error::LengthLimitExceeded
        } else {
            Error::UnterminatedString
        })?;
    let mut value = String::new();
    value
        .try_reserve_exact(length * 3)
        .map_err(|_| Error::AllocationFailed)?;
    let mut value = Zeroizing::new(value);
    for &byte in &bytes[..length] {
        value.push(decode_byte(byte)?);
    }
    Ok(DecodedString {
        value: SensitiveString(value),
        consumed: prefix + length + 1,
    })
}

fn validate_limit(limit: usize) -> Result<(), Error> {
    if limit > MAX_PAYLOAD_BYTES {
        Err(Error::InvalidLimit)
    } else {
        Ok(())
    }
}

fn encode_char(character: char) -> Result<u8, Error> {
    match character as u32 {
        0 => Err(Error::EmbeddedNul),
        code @ (1..=127 | 160..=255) => Ok(code as u8),
        _ => EXTENDED
            .iter()
            .position(|&entry| entry == character)
            .map(|index| index as u8 + 128)
            .ok_or(Error::UnsupportedCharacter),
    }
}

fn decode_byte(byte: u8) -> Result<char, Error> {
    if (128..160).contains(&byte) {
        match EXTENDED[(byte - 128) as usize] {
            '\0' => Err(Error::InvalidCp1252Byte),
            character => Ok(character),
        }
    } else {
        Ok(char::from(byte))
    }
}
