use super::Error;
use openrs_protocol::payload::{PayloadError, PayloadReader};
use openrs_protocol::registry::ServerPacket;

const RECEIVE_BUFFER: usize = 65536;
const UNKNOWN_CHARACTER: char = '?';
const FALLBACK_MESSAGE: &str = "Cabbage";
const WINDOWS_1252: [char; 32] = [
    '\u{20ac}', '\0', '\u{201a}', '\u{192}', '\u{201e}', '\u{2026}', '\u{2020}', '\u{2021}',
    '\u{2c6}', '\u{2030}', '\u{160}', '\u{2039}', '\u{152}', '\0', '\u{17d}', '\0', '\0',
    '\u{2018}', '\u{2019}', '\u{201c}', '\u{201d}', '\u{2022}', '\u{2013}', '\u{2014}',
    '\u{2dc}', '\u{2122}', '\u{161}', '\u{203a}', '\u{153}', '\0', '\u{17e}', '\u{178}',
];
const BASE37: [char; 37] = [
    '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
    's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
];

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ChatHuffman {
    table: Vec<i32>,
}

impl ChatHuffman {
    pub fn new(code_lengths: &[i8]) -> Self {
        let mut counts = [0i32; 33];
        let mut table = vec![0i32; 8];
        let mut current_code = 0i32;
        for (symbol, &length) in code_lengths.iter().enumerate() {
            let cl = i32::from(length);
            if cl == 0 {
                continue;
            }
            let code = 1i32.wrapping_shl((32 - cl) as u32);
            let count = counts[cl as usize];
            let new_code;
            if count & code != 0 {
                new_code = counts[(cl - 1) as usize];
            } else {
                new_code = count | code;
                let mut j = cl - 1;
                while j >= 1 {
                    let bit_length = counts[j as usize];
                    if bit_length != count {
                        break;
                    }
                    let bit = 1i32.wrapping_shl((32 - j) as u32);
                    if bit_length & bit != 0 {
                        counts[j as usize] = counts[(j - 1) as usize];
                        break;
                    }
                    counts[j as usize] = bit_length | bit;
                    j -= 1;
                }
            }
            counts[cl as usize] = new_code;
            for j in (cl + 1)..=32 {
                if count == counts[j as usize] {
                    counts[j as usize] = new_code;
                }
            }
            let mut index = 0usize;
            for j in 0..cl {
                let bit = 0x8000_0000u32 >> j;
                if (count as u32) & bit != 0 {
                    if table[index] == 0 {
                        table[index] = current_code;
                    }
                    index = table[index] as usize;
                } else {
                    index += 1;
                }
                if index >= table.len() {
                    let mut grown = vec![0i32; table.len() * 2];
                    grown[..table.len()].copy_from_slice(&table);
                    table = grown;
                }
            }
            table[index] = !(symbol as i32);
            if index as i32 >= current_code {
                current_code = index as i32 + 1;
            }
        }
        Self { table }
    }

    fn decompress(&self, input: &[u8], offset: usize, length: usize) -> Option<(Vec<u8>, usize)> {
        if length == 0 {
            return Some((Vec::new(), 0));
        }
        let mut output = Vec::with_capacity(length);
        let mut table_index: i64 = 0;
        let mut position = offset;
        loop {
            if position >= RECEIVE_BUFFER {
                return None;
            }
            let flags = input.get(position).copied().unwrap_or(0);
            let mut mask = 0x80u8;
            while mask != 0 {
                table_index = if flags & mask != 0 {
                    i64::from(*self.table.get(usize::try_from(table_index).ok()?)?)
                } else {
                    table_index + 1
                };
                let value = *self.table.get(usize::try_from(table_index).ok()?)?;
                if value < 0 {
                    output.push((!value) as u8);
                    if output.len() >= length {
                        return Some((output, position + 1 - offset));
                    }
                    table_index = 0;
                }
                mask >>= 1;
            }
            position += 1;
        }
    }
}

fn read_text(bytes: &[u8]) -> String {
    let mut text = String::new();
    for &byte in bytes {
        if byte == 0 {
            continue;
        }
        if (128..160).contains(&byte) {
            let mapped = WINDOWS_1252[usize::from(byte - 128)];
            text.push(if mapped == '\0' {
                UNKNOWN_CHARACTER
            } else {
                mapped
            });
        } else {
            text.push(char::from(byte));
        }
    }
    text
}

pub(crate) fn escape_angle_brackets(text: &str) -> String {
    let mut escaped = String::with_capacity(text.len());
    for character in text.chars() {
        match character {
            '<' => escaped.push_str("<lt>"),
            '>' => escaped.push_str("<gt>"),
            other => escaped.push(other),
        }
    }
    escaped
}

pub(crate) fn read_huffman_message(
    huffman: Option<&ChatHuffman>,
    reader: &mut PayloadReader<'_>,
    payload: &[u8],
) -> Result<String, PayloadError> {
    let size = usize::from(reader.read_u_smart()?).min(32767);
    let Some(huffman) = huffman else {
        return Ok(FALLBACK_MESSAGE.to_string());
    };
    match huffman.decompress(payload, reader.position(), size) {
        Some((data, consumed)) => {
            reader.read_bytes(consumed)?;
            Ok(read_text(&data))
        }
        None => Ok(FALLBACK_MESSAGE.to_string()),
    }
}

fn decode_uid_name(uid: i64) -> Option<String> {
    if uid <= 0 || uid >= 6_582_952_005_840_035_281 {
        return None;
    }
    if uid % 37 == 0 {
        return None;
    }
    let mut remaining = uid;
    let mut name: Vec<char> = Vec::new();
    while remaining != 0 {
        let previous = remaining;
        remaining /= 37;
        let mut character = BASE37[(previous - 37 * remaining) as usize];
        if character == '_' {
            let last = name.last_mut()?;
            *last = simple_upper(*last);
            character = '\u{a0}';
        }
        name.push(character);
    }
    name.reverse();
    let first = name.first_mut()?;
    *first = simple_upper(*first);
    Some(name.into_iter().collect())
}

fn simple_upper(character: char) -> char {
    let mut upper = character.to_uppercase();
    match (upper.next(), upper.next()) {
        (Some(single), None) => single,
        _ => character,
    }
}

fn simple_lower(character: char) -> char {
    let mut lower = character.to_lowercase();
    match (lower.next(), lower.next()) {
        (Some(single), None) => single,
        _ => character,
    }
}

fn equals_ignore_case(left: &str, right: &str) -> bool {
    let mut a = left.chars();
    let mut b = right.chars();
    loop {
        match (a.next(), b.next()) {
            (None, None) => return true,
            (Some(x), Some(y)) => {
                if x != y
                    && simple_upper(x) != simple_upper(y)
                    && simple_lower(simple_upper(x)) != simple_lower(simple_upper(y))
                {
                    return false;
                }
            }
            _ => return false,
        }
    }
}

pub(crate) fn crown(image: u8) -> String {
    format!("<img={image}>")
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ChatKind {
    PrivateMessage,
    PrivateMessageEcho,
    PrivateStaff,
    FriendsChat,
    ClanChat,
    GuestClanChat,
    PublicChat,
    StaffChat,
    PublicQuickchat,
}

impl ChatKind {
    pub fn native_name(self) -> &'static str {
        match self {
            Self::PrivateMessage => "PRIVATE_MESSAGE",
            Self::PrivateMessageEcho => "PRIVATE_MESSAGE_ECHO",
            Self::PrivateStaff => "PRIVATE_STAFF",
            Self::FriendsChat => "FC_CHAT",
            Self::ClanChat => "CLAN_CHAT",
            Self::GuestClanChat => "GUEST_CLAN_CHAT",
            Self::PublicChat => "PUBLIC_CHAT",
            Self::StaffChat => "STAFF_CHAT",
            Self::PublicQuickchat => "PUBLIC_QUICKCHAT",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ChatLine {
    pub kind: ChatKind,
    pub crowned_name: String,
    pub name: String,
    pub simple_name: String,
    pub message: String,
    pub clan: Option<String>,
    pub quickchat_id: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct IgnoredName {
    pub username: Option<String>,
    pub display_name: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ChatSession {
    pub huffman: Option<ChatHuffman>,
    pub quickchat_account: bool,
    pub verified_email: bool,
    pub quickchat_only: bool,
    pub ignored: Vec<IgnoredName>,
    pub member_channel: Option<String>,
    pub guest_channel: Option<String>,
    hashes: [i64; 100],
    hash_index: usize,
}

impl ChatSession {
    pub fn new(huffman: Option<ChatHuffman>) -> Self {
        Self {
            huffman,
            quickchat_account: false,
            verified_email: false,
            quickchat_only: false,
            ignored: Vec::new(),
            member_channel: None,
            guest_channel: None,
            hashes: [0; 100],
            hash_index: 0,
        }
    }

    pub fn seed_hashes(&mut self, hashes: &[i64], next_index: usize) {
        self.hashes = [0; 100];
        self.hashes[..hashes.len()].copy_from_slice(hashes);
        self.hash_index = next_index % 100;
    }

    pub fn hashes(&self) -> (&[i64; 100], usize) {
        (&self.hashes, self.hash_index)
    }

    fn restricted(&self) -> bool {
        !((!self.quickchat_account || self.verified_email) && !self.quickchat_only)
    }

    pub fn is_ignored(&self, name: &str) -> bool {
        self.ignored.iter().any(|entry| {
            entry
                .username
                .as_deref()
                .is_some_and(|value| equals_ignore_case(name, value))
                || entry
                    .display_name
                    .as_deref()
                    .is_some_and(|value| equals_ignore_case(name, value))
        })
    }

    pub(crate) fn public_ignored(&self, icon: u8, quickchat: bool, name: &str) -> bool {
        icon <= 1 && ((!quickchat && self.restricted()) || self.is_ignored(name))
    }

    fn rights_ignore(&self, rights: u8, name: &str) -> bool {
        rights <= 1 && (self.restricted() || self.is_ignored(name))
    }

    fn record(&mut self, hash: i64) {
        self.hashes[self.hash_index] = hash;
        self.hash_index = (self.hash_index + 1) % 100;
    }

    pub fn decode(
        &mut self,
        packet: ServerPacket,
        payload: &[u8],
    ) -> Result<Option<ChatLine>, Error> {
        let mut r = PayloadReader::new(payload);
        let line = self.decode_body(packet, &mut r, payload)?;
        if r.position() != payload.len() {
            return Err(Error::TrailingBytes {
                consumed: r.position(),
                length: payload.len(),
            });
        }
        Ok(line)
    }

    fn decode_body(
        &mut self,
        packet: ServerPacket,
        r: &mut PayloadReader<'_>,
        payload: &[u8],
    ) -> Result<Option<ChatLine>, Error> {
        match packet {
            ServerPacket::MESSAGE_PRIVATE_ECHO => {
                let recipient = r.read_cp1252()?;
                let message = escape_angle_brackets(&read_huffman_message(
                    self.huffman.as_ref(),
                    r,
                    payload,
                )?);
                Ok(Some(ChatLine {
                    kind: ChatKind::PrivateMessageEcho,
                    crowned_name: recipient.clone(),
                    name: recipient.clone(),
                    simple_name: recipient,
                    message,
                    clan: None,
                    quickchat_id: -1,
                }))
            }
            ServerPacket::MESSAGE_PRIVATE => self.decode_named(r, payload, false),
            ServerPacket::MESSAGE_FRIENDS_CHAT => self.decode_named(r, payload, true),
            ServerPacket::MESSAGE_CLANCHANNEL => self.decode_clan(r, payload),
            other => Err(Error::UnsupportedOpcode(other)),
        }
    }

    fn decode_named(
        &mut self,
        r: &mut PayloadReader<'_>,
        payload: &[u8],
        friends_chat: bool,
    ) -> Result<Option<ChatLine>, Error> {
        let has_display_name = r.read_u8()? == 1;
        let username = r.read_cp1252()?;
        let display_name = if has_display_name {
            r.read_cp1252()?
        } else {
            username.clone()
        };
        let owner = if friends_chat {
            Some(r.read_i64_be()?)
        } else {
            None
        };
        let high = i64::from(r.read_u16_be()?);
        let low = i64::from(r.read_u24_be()?);
        let rights = r.read_u8()?;
        let hash = (high << 32) + low;
        if self.hashes.contains(&hash) {
            return Ok(None);
        }
        if self.rights_ignore(rights, &display_name) {
            return Ok(None);
        }
        self.record(hash);
        let message = escape_angle_brackets(&read_huffman_message(self.huffman.as_ref(), r, payload)?);
        let line = match owner {
            None => match rights {
                2 => ChatLine {
                    kind: ChatKind::PrivateStaff,
                    crowned_name: crown(1) + &username,
                    name: crown(1) + &display_name,
                    simple_name: username,
                    message,
                    clan: None,
                    quickchat_id: -1,
                },
                1 => ChatLine {
                    kind: ChatKind::PrivateStaff,
                    crowned_name: crown(0) + &username,
                    name: crown(0) + &display_name,
                    simple_name: username,
                    message,
                    clan: None,
                    quickchat_id: -1,
                },
                _ => ChatLine {
                    kind: ChatKind::PrivateMessage,
                    crowned_name: username.clone(),
                    name: display_name,
                    simple_name: username,
                    message,
                    clan: None,
                    quickchat_id: -1,
                },
            },
            Some(uid) => {
                let clan = decode_uid_name(uid);
                let (crowned_name, name) = match rights {
                    2 | 3 => (crown(1) + &username, crown(1) + &display_name),
                    1 => (crown(0) + &username, crown(0) + &display_name),
                    _ => (username.clone(), display_name),
                };
                ChatLine {
                    kind: ChatKind::FriendsChat,
                    crowned_name,
                    name,
                    simple_name: username,
                    message,
                    clan,
                    quickchat_id: -1,
                }
            }
        };
        Ok(Some(line))
    }

    fn decode_clan(
        &mut self,
        r: &mut PayloadReader<'_>,
        payload: &[u8],
    ) -> Result<Option<ChatLine>, Error> {
        let non_guest = r.read_u8()? == 1;
        let sender = r.read_cp1252()?;
        let high = i64::from(r.read_u16_be()?);
        let low = i64::from(r.read_u24_be()?);
        let rights = r.read_u8()?;
        let hash = low + (high << 32);
        let channel = if non_guest {
            self.member_channel.clone()
        } else {
            self.guest_channel.clone()
        };
        let Some(channel) = channel else {
            return Ok(None);
        };
        if self.hashes.contains(&hash) || self.rights_ignore(rights, &sender) {
            return Ok(None);
        }
        self.record(hash);
        let message = escape_angle_brackets(&read_huffman_message(self.huffman.as_ref(), r, payload)?);
        let kind = if non_guest {
            ChatKind::ClanChat
        } else {
            ChatKind::GuestClanChat
        };
        let (crowned_name, name) = match rights {
            2 | 3 => (crown(1) + &sender, crown(1) + &sender),
            1 => (crown(0) + &sender, crown(0) + &sender),
            _ => (sender.clone(), sender.clone()),
        };
        Ok(Some(ChatLine {
            kind,
            crowned_name,
            name,
            simple_name: sender,
            message,
            clan: Some(channel),
            quickchat_id: -1,
        }))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn uid_names_follow_the_native_rules() {
        assert_eq!(decode_uid_name(0), None);
        assert_eq!(decode_uid_name(37), None);
        assert_eq!(decode_uid_name(6_582_952_005_840_035_281), None);
        assert_eq!(decode_uid_name(1), Some("A".to_string()));
    }

    #[test]
    fn missing_table_decodes_to_the_fallback_message() {
        let mut session = ChatSession::new(None);
        let line = session
            .decode(ServerPacket::MESSAGE_PRIVATE_ECHO, &[b'a', 0, 5])
            .unwrap()
            .unwrap();
        assert_eq!(line.message, FALLBACK_MESSAGE);
        assert_eq!(line.kind, ChatKind::PrivateMessageEcho);
        assert_eq!(
            session.decode(ServerPacket::MESSAGE_PRIVATE_ECHO, &[b'a', 0, 5, 1]),
            Err(Error::TrailingBytes {
                consumed: 3,
                length: 4
            })
        );
    }
}
