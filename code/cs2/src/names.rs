const MAX_NAME_LENGTH: usize = 12;
const MAX_BASE37_NAME: i64 = 6_582_952_005_840_035_281;
const NAME_OVERFLOW: i64 = 177_917_621_779_460_413;

const ALPHABET: [u8; 37] = *b"_abcdefghijklmnopqrstuvwxyz0123456789";

const ALLOWED_SPECIAL: [u16; 56] = [
    0x20, 0xa0, 0x5f, 0x2d, 0xe0, 0xe1, 0xe2, 0xe4, 0xe3, 0xc0, 0xc1, 0xc2, 0xc4, 0xc3, 0xe8, 0xe9,
    0xea, 0xeb, 0xc8, 0xc9, 0xca, 0xcb, 0xed, 0xee, 0xef, 0xcd, 0xce, 0xcf, 0xf2, 0xf3, 0xf4, 0xf6,
    0xf5, 0xd2, 0xd3, 0xd4, 0xd6, 0xd5, 0xf9, 0xfa, 0xfb, 0xfc, 0xd9, 0xda, 0xdb, 0xdc, 0xe7, 0xc7,
    0xff, 0x178, 0xf1, 0xd1, 0xdf, 0x5b, 0x5d, 0x23,
];

fn is_space_unit(unit: u16) -> bool {
    matches!(unit, 0xa0 | 0x20 | 0x5f | 0x2d)
}

fn is_allowed(unit: u16) -> bool {
    if unit <= 0x1f || (0x7f..=0x9f).contains(&unit) {
        return false;
    }
    matches!(unit, 0x30..=0x39 | 0x41..=0x5a | 0x61..=0x7a) || ALLOWED_SPECIAL.contains(&unit)
}

fn normalize(unit: u16) -> u16 {
    match unit {
        0xd2..=0xd6 | 0xf2..=0xf6 => 0x6f,
        0xc7 | 0xe7 => 0x63,
        0xdf => 0x62,
        0xff | 0x178 => 0x79,
        0x23 | 0x5b | 0x5d => unit,
        0xc8..=0xcb | 0xe8..=0xeb => 0x65,
        0xd1 | 0xf1 => 0x6e,
        0xc0..=0xc4 | 0xe0..=0xe4 => 0x61,
        0x20 | 0x2d | 0x5f | 0xa0 => 0x5f,
        0xd9..=0xdc | 0xf9..=0xfc => 0x75,
        0xcd..=0xcf | 0xed..=0xef => 0x69,
        0x41..=0x5a => unit + 0x20,
        _ => unit,
    }
}

pub fn format_displayname(text: &str) -> Option<String> {
    let units: Vec<u16> = text.encode_utf16().collect();
    let mut start = 0;
    let mut end = units.len();
    while start < end && is_space_unit(units[start]) {
        start += 1;
    }
    while end > start && is_space_unit(units[end - 1]) {
        end -= 1;
    }
    let length = end - start;
    if !(1..=MAX_NAME_LENGTH).contains(&length) {
        return None;
    }
    let mut result = String::with_capacity(length);
    for &unit in &units[start..end] {
        if is_allowed(unit) {
            let formatted = normalize(unit);
            if formatted != 0 {
                result.push(char::from_u32(u32::from(formatted)).unwrap_or('\u{fffd}'));
            }
        }
    }
    if result.is_empty() {
        None
    } else {
        Some(result)
    }
}

pub fn names_match(
    find_raw: &str,
    find_formatted: Option<&str>,
    current_raw: Option<&str>,
    current_formatted: Option<&str>,
) -> bool {
    match current_raw {
        None => false,
        Some(current_raw) => {
            if !find_raw.starts_with('#') && !current_raw.starts_with('#') {
                find_formatted == current_formatted
            } else {
                find_raw == current_raw
            }
        }
    }
}

pub fn name_to_long(text: &str) -> i64 {
    let mut uid: i64 = 0;
    for unit in text.encode_utf16() {
        uid = uid.wrapping_mul(37);
        match unit {
            0x41..=0x5a => uid += i64::from(unit) + 1 - 65,
            0x61..=0x7a => uid += i64::from(unit) + 1 - 97,
            0x30..=0x39 => uid += i64::from(unit) + 27 - 48,
            _ => {}
        }
        if uid >= NAME_OVERFLOW {
            break;
        }
    }
    while uid % 37 == 0 && uid != 0 {
        uid /= 37;
    }
    uid
}

pub fn decode_uid_name(uid: i64) -> Option<String> {
    if uid <= 0 || uid >= MAX_BASE37_NAME || uid % 37 == 0 {
        return None;
    }
    let mut remaining = uid;
    let mut built: Vec<char> = Vec::new();
    while remaining != 0 {
        let previous = remaining;
        remaining /= 37;
        let mut ch = char::from(ALPHABET[(previous - 37 * remaining) as usize]);
        if ch == '_' {
            let last = built.len() - 1;
            built[last] = built[last].to_ascii_uppercase();
            ch = '\u{a0}';
        }
        built.push(ch);
    }
    built.reverse();
    built[0] = built[0].to_ascii_uppercase();
    Some(built.into_iter().collect())
}

pub fn decode_friends_chat_name(name: &str) -> String {
    decode_uid_name(name_to_long(name)).unwrap_or_default()
}

pub fn utf16_length(text: &str) -> usize {
    text.encode_utf16().count()
}

pub fn encode_cp1252(unit: u16) -> u8 {
    match unit {
        1..=127 | 160..=255 => unit as u8,
        8364 => 0x80,
        8218 => 0x82,
        402 => 0x83,
        8222 => 0x84,
        8230 => 0x85,
        8224 => 0x86,
        8225 => 0x87,
        710 => 0x88,
        8240 => 0x89,
        352 => 0x8a,
        8249 => 0x8b,
        338 => 0x8c,
        381 => 0x8e,
        8216 => 0x91,
        8217 => 0x92,
        8220 => 0x93,
        8221 => 0x94,
        8226 => 0x95,
        8211 => 0x96,
        8212 => 0x97,
        732 => 0x98,
        8482 => 0x99,
        353 => 0x9a,
        8250 => 0x9b,
        339 => 0x9c,
        382 => 0x9e,
        376 => 0x9f,
        _ => 63,
    }
}
