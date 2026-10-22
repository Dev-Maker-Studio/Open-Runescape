use openrs_protocol::payload::{PayloadError, PayloadReader};
use openrs_protocol::registry::ServerPacket;
use std::cmp::Ordering;
use std::fmt;

pub const FRIEND_CAPACITY: usize = 200;
pub const IGNORE_CAPACITY: usize = 100;
pub const FRIENDS_CHAT_CAPACITY: usize = 100;
pub const FRIENDS_CHAT_KEEP_ROSTER: u8 = 255;
pub const FRIENDS_CHAT_REMOVE_RANK: i8 = -128;
pub const MAX_NAME_LENGTH: usize = 12;
pub const MAX_BASE37_NAME: i64 = 6_582_952_005_840_035_281;

const BASE37_ALPHABET: [char; 37] = [
    '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
    's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
];

const ALLOWED_SPECIAL: [u16; 56] = [
    0x20, 0xa0, 0x5f, 0x2d, 0xe0, 0xe1, 0xe2, 0xe4, 0xe3, 0xc0, 0xc1, 0xc2, 0xc4, 0xc3, 0xe8, 0xe9,
    0xea, 0xeb, 0xc8, 0xc9, 0xca, 0xcb, 0xed, 0xee, 0xef, 0xcd, 0xce, 0xcf, 0xf2, 0xf3, 0xf4, 0xf6,
    0xf5, 0xd2, 0xd3, 0xd4, 0xd6, 0xd5, 0xf9, 0xfa, 0xfb, 0xfc, 0xd9, 0xda, 0xdb, 0xdc, 0xe7, 0xc7,
    0xff, 0x178, 0xf1, 0xd1, 0xdf, 0x5b, 0x5d, 0x23,
];

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    UnsupportedOpcode(ServerPacket),
    Payload(PayloadError),
    NullPointer(&'static str),
    IndexOutOfBounds(&'static str),
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::UnsupportedOpcode(opcode) => write!(f, "unsupported social opcode {opcode:?}"),
            Self::Payload(error) => error.fmt(f),
            Self::NullPointer(site) => write!(f, "native null dereference at {site}"),
            Self::IndexOutOfBounds(site) => write!(f, "native array overflow at {site}"),
        }
    }
}

impl std::error::Error for Error {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Payload(error) => Some(error),
            _ => None,
        }
    }
}

impl From<PayloadError> for Error {
    fn from(error: PayloadError) -> Self {
        Self::Payload(error)
    }
}

fn is_space_unit(unit: u16) -> bool {
    matches!(unit, 0xa0 | 0x20 | 0x5f | 0x2d)
}

fn is_iso_control(unit: u16) -> bool {
    unit <= 0x1f || (0x7f..=0x9f).contains(&unit)
}

fn is_allowed(unit: u16) -> bool {
    if is_iso_control(unit) {
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

pub fn name_for_base37(encoded: i64) -> Option<String> {
    if encoded <= 0 || encoded >= MAX_BASE37_NAME || encoded % 37 == 0 {
        return None;
    }
    let mut remaining = encoded;
    let mut characters = Vec::new();
    while remaining != 0 {
        let previous = remaining;
        remaining /= 37;
        characters.push(BASE37_ALPHABET[(previous - 37 * remaining) as usize]);
    }
    characters.reverse();
    Some(characters.into_iter().collect())
}

pub fn compare_utf16(left: &str, right: &str) -> Ordering {
    left.encode_utf16().cmp(right.encode_utf16())
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Friend {
    pub display_name: String,
    pub referrer: bool,
    pub world_id: i32,
    pub world_name: String,
    pub friends_chat_rank: i32,
    pub platform: i32,
    pub previous_display_name: String,
    pub world_flags: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Ignore {
    pub unfiltered_username: String,
    pub display_name: String,
    pub temporary: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FriendsChatPlayer {
    pub rank: i8,
    pub username: String,
    pub display_name: String,
    pub world_name: String,
    pub world_id: i32,
    pub text_name: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LoginStateUpdate {
    pub username: String,
    pub action_type: i16,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Context<'a> {
    pub server_tick: i32,
    pub world_id: Option<i32>,
    pub my_player: Option<Option<&'a str>>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SocialModel {
    pub friends_loaded: i32,
    pub friends: Vec<Friend>,
    pub ignore_slots: Vec<Option<Ignore>>,
    pub ignore_count: i32,
    pub fc_name: Option<String>,
    pub fc_owner_name: Option<String>,
    pub fc_player_count: i32,
    pub fc_players: Option<Vec<Option<FriendsChatPlayer>>>,
    pub fc_min_rank_can_kick: i8,
    pub my_fc_rank: i8,
    pub last_friend_ignore_update_tick: i32,
    pub last_friends_chat_update_tick: i32,
    pub login_state_updates: Vec<LoginStateUpdate>,
}

impl Default for SocialModel {
    fn default() -> Self {
        Self {
            friends_loaded: 0,
            friends: Vec::new(),
            ignore_slots: vec![None; IGNORE_CAPACITY],
            ignore_count: 0,
            fc_name: None,
            fc_owner_name: None,
            fc_player_count: 0,
            fc_players: None,
            fc_min_rank_can_kick: 0,
            my_fc_rank: 0,
            last_friend_ignore_update_tick: 0,
            last_friends_chat_update_tick: 0,
            login_state_updates: Vec::new(),
        }
    }
}

fn read_friend_tail(
    reader: &mut PayloadReader<'_>,
    world: u16,
) -> Result<(String, i32, i32), Error> {
    if world > 0 {
        let name = reader.read_cp1252()?;
        let platform = i32::from(reader.read_u8()?);
        let flags = reader.read_i32_be()?;
        Ok((name, platform, flags))
    } else {
        Ok((String::new(), -1, 0))
    }
}

impl SocialModel {
    pub fn is_social_packet(packet: ServerPacket) -> bool {
        matches!(
            packet,
            ServerPacket::FRIEND_STATUS
                | ServerPacket::FRIENDLIST_LOADED
                | ServerPacket::UPDATE_IGNORE_LIST
                | ServerPacket::ADD_IGNORE
                | ServerPacket::FRIENDS_CHAT_CHANNEL
                | ServerPacket::UPDATE_FRIENDCHAT_CHANNEL_SINGLEUSER
        )
    }

    pub fn apply(
        &mut self,
        packet: ServerPacket,
        payload: &[u8],
        context: &Context<'_>,
    ) -> Result<(), Error> {
        match packet {
            ServerPacket::FRIEND_STATUS => self.friend_status(payload, context),
            ServerPacket::FRIENDLIST_LOADED => {
                self.friends_loaded = 1;
                self.last_friend_ignore_update_tick = context.server_tick;
                Ok(())
            }
            ServerPacket::UPDATE_IGNORE_LIST => self.update_ignore_list(payload, context),
            ServerPacket::ADD_IGNORE => self.add_ignore(payload, context),
            ServerPacket::FRIENDS_CHAT_CHANNEL => self.friends_chat_channel(payload, context),
            ServerPacket::UPDATE_FRIENDCHAT_CHANNEL_SINGLEUSER => {
                self.friends_chat_single_user(payload, context)
            }
            other => Err(Error::UnsupportedOpcode(other)),
        }
    }

    fn friend_status(&mut self, payload: &[u8], context: &Context<'_>) -> Result<(), Error> {
        let mut reader = PayloadReader::new(payload);
        while reader.position() < payload.len() {
            let warn_message = reader.read_u8()? == 1;
            let mut display_name = Some(reader.read_cp1252()?);
            let previous_display_name = reader.read_cp1252()?;
            let world = reader.read_u16_be()?;
            let friends_chat_rank = i32::from(reader.read_u8()?);
            let referrer = reader.read_u8()? == 1;
            let (world_name, platform, world_flags) = read_friend_tail(&mut reader, world)?;
            let world = i32::from(world);
            for index in 0..self.friends.len() {
                if !warn_message {
                    if display_name.as_deref() == Some(self.friends[index].display_name.as_str()) {
                        if world != self.friends[index].world_id {
                            let name = display_name.clone().unwrap_or_default();
                            let mut announce = true;
                            self.login_state_updates.retain(|update| {
                                if update.username == name {
                                    if world != 0 && update.action_type == 0 {
                                        announce = false;
                                        return false;
                                    }
                                    if world == 0 && update.action_type != 0 {
                                        announce = false;
                                        return false;
                                    }
                                }
                                true
                            });
                            if announce {
                                self.login_state_updates.push(LoginStateUpdate {
                                    username: name,
                                    action_type: world as i16,
                                });
                            }
                            self.friends[index].world_id = world;
                        }
                        let friend = &mut self.friends[index];
                        friend.previous_display_name = previous_display_name.clone();
                        friend.world_name = world_name.clone();
                        friend.friends_chat_rank = friends_chat_rank;
                        friend.platform = platform;
                        friend.referrer = referrer;
                        friend.world_flags = world_flags;
                        display_name = None;
                        break;
                    }
                } else if previous_display_name == self.friends[index].display_name {
                    let friend = &mut self.friends[index];
                    friend.display_name = display_name.take().unwrap_or_default();
                    friend.previous_display_name = previous_display_name.clone();
                    break;
                }
            }
            if let Some(name) = display_name
                && self.friends.len() < FRIEND_CAPACITY
            {
                self.friends.push(Friend {
                    display_name: name,
                    referrer,
                    world_id: world,
                    world_name,
                    friends_chat_rank,
                    platform,
                    previous_display_name,
                    world_flags,
                });
            }
        }
        self.friends_loaded = 2;
        self.last_friend_ignore_update_tick = context.server_tick;
        let mut flags = self.friends.len();
        while flags > 0 {
            let mut sorted = true;
            flags -= 1;
            for index in 0..flags {
                let mut swap = false;
                let current = &self.friends[index];
                let next = &self.friends[index + 1];
                let world = context
                    .world_id
                    .ok_or(Error::NullPointer("GAME_CONNECTION_INFO"))?;
                if current.world_id != world && next.world_id == world {
                    swap = true;
                }
                if !swap && current.world_id == 0 && next.world_id != 0 {
                    swap = true;
                }
                if !swap && !current.referrer && next.referrer {
                    swap = true;
                }
                if swap {
                    self.friends.swap(index, index + 1);
                    sorted = false;
                }
            }
            if sorted {
                break;
            }
        }
        Ok(())
    }

    fn update_ignore_list(&mut self, payload: &[u8], context: &Context<'_>) -> Result<(), Error> {
        let mut reader = PayloadReader::new(payload);
        self.ignore_count = i32::from(reader.read_u8()?);
        for key in 0..self.ignore_count as usize {
            let ignore = Ignore {
                unfiltered_username: reader.read_cp1252()?,
                display_name: reader.read_cp1252()?,
                temporary: false,
            };
            *self
                .ignore_slots
                .get_mut(key)
                .ok_or(Error::IndexOutOfBounds("IgnoreList.IGNORED"))? = Some(ignore);
        }
        self.last_friend_ignore_update_tick = context.server_tick;
        Ok(())
    }

    fn add_ignore(&mut self, payload: &[u8], context: &Context<'_>) -> Result<(), Error> {
        let mut reader = PayloadReader::new(payload);
        let flags = reader.read_u8()?;
        let edit = flags & 1 == 1;
        let username = reader.read_cp1252()?;
        let display_name = reader.read_cp1252()?;
        if !edit {
            let slot = self.ignore_count;
            self.ignore_count = self.ignore_count.wrapping_add(1);
            let entry = usize::try_from(slot)
                .ok()
                .and_then(|slot| self.ignore_slots.get_mut(slot))
                .ok_or(Error::IndexOutOfBounds("IgnoreList.IGNORED"))?;
            *entry = Some(Ignore {
                unfiltered_username: username,
                display_name,
                temporary: flags & 2 == 2,
            });
        } else {
            for index in 0..self.ignore_count.max(0) as usize {
                let ignore = self
                    .ignore_slots
                    .get_mut(index)
                    .ok_or(Error::IndexOutOfBounds("IgnoreList.IGNORED"))?
                    .as_mut()
                    .ok_or(Error::NullPointer("IgnoreList.IGNORED"))?;
                if display_name == ignore.unfiltered_username {
                    ignore.unfiltered_username = username;
                    ignore.display_name = display_name;
                    break;
                }
            }
        }
        self.last_friend_ignore_update_tick = context.server_tick;
        Ok(())
    }

    fn friends_chat_channel(&mut self, payload: &[u8], context: &Context<'_>) -> Result<(), Error> {
        self.last_friends_chat_update_tick = context.server_tick;
        if payload.is_empty() {
            self.fc_name = None;
            self.fc_owner_name = None;
            self.fc_player_count = 0;
            self.fc_players = None;
            return Ok(());
        }
        let mut reader = PayloadReader::new(payload);
        self.fc_owner_name = Some(reader.read_cp1252()?);
        if reader.read_u8()? == 1 {
            reader.read_cp1252()?;
        }
        self.fc_name = name_for_base37(reader.read_i64_be()?);
        self.fc_min_rank_can_kick = reader.read_i8()?;
        let player_count = reader.read_u8()?;
        if player_count == FRIENDS_CHAT_KEEP_ROSTER {
            return Ok(());
        }
        self.fc_player_count = i32::from(player_count);
        let mut players: Vec<Option<FriendsChatPlayer>> = vec![None; FRIENDS_CHAT_CAPACITY];
        for index in 0..usize::from(player_count) {
            let slot = players
                .get_mut(index)
                .ok_or(Error::IndexOutOfBounds("FriendsChat.FC_PLAYERS"))?;
            let display_name = reader.read_cp1252()?;
            let username = if reader.read_u8()? == 1 {
                reader.read_cp1252()?
            } else {
                display_name.clone()
            };
            let text_name = format_displayname(&username);
            let world_id = i32::from(reader.read_u16_be()?);
            let rank = reader.read_i8()?;
            let world_name = reader.read_cp1252()?;
            let my_player = context
                .my_player
                .ok_or(Error::NullPointer("PlayerUpdate.MY_PLAYER"))?;
            if my_player == Some(username.as_str()) {
                self.my_fc_rank = rank;
            }
            *slot = Some(FriendsChatPlayer {
                rank,
                username,
                display_name,
                world_name,
                world_id,
                text_name,
            });
        }
        let mut total = usize::from(player_count);
        while total > 0 {
            let mut sorted = true;
            total -= 1;
            for index in 0..total {
                let left = players[index]
                    .as_ref()
                    .and_then(|player| player.text_name.as_deref())
                    .ok_or(Error::NullPointer("FriendsChatPlayer.textName"))?;
                let right = players[index + 1]
                    .as_ref()
                    .and_then(|player| player.text_name.as_deref())
                    .ok_or(Error::NullPointer("FriendsChatPlayer.textName"))?;
                if compare_utf16(left, right) == Ordering::Greater {
                    players.swap(index, index + 1);
                    sorted = false;
                }
            }
            if sorted {
                break;
            }
        }
        self.fc_players = Some(players);
        Ok(())
    }

    fn friends_chat_player(&self, index: i32) -> Result<&FriendsChatPlayer, Error> {
        let players = self
            .fc_players
            .as_ref()
            .ok_or(Error::NullPointer("FriendsChat.FC_PLAYERS"))?;
        usize::try_from(index)
            .ok()
            .and_then(|index| players.get(index))
            .ok_or(Error::IndexOutOfBounds("FriendsChat.FC_PLAYERS"))?
            .as_ref()
            .ok_or(Error::NullPointer("FriendsChat.FC_PLAYERS[]"))
    }

    fn friends_chat_single_user(
        &mut self,
        payload: &[u8],
        context: &Context<'_>,
    ) -> Result<(), Error> {
        let mut reader = PayloadReader::new(payload);
        let display_name = reader.read_cp1252()?;
        let username = if reader.read_u8()? == 1 {
            reader.read_cp1252()?
        } else {
            display_name.clone()
        };
        let world_id = i32::from(reader.read_u16_be()?);
        let rank = reader.read_i8()?;
        if rank == FRIENDS_CHAT_REMOVE_RANK {
            if self.fc_player_count == 0 {
                return Ok(());
            }
            let mut search = 0;
            while search < self.fc_player_count {
                let player = self.friends_chat_player(search)?;
                if player.username == username && world_id == player.world_id {
                    break;
                }
                search += 1;
            }
            if search < self.fc_player_count {
                while search < self.fc_player_count - 1 {
                    let next = self
                        .fc_players
                        .as_ref()
                        .and_then(|players| players.get(search as usize + 1))
                        .ok_or(Error::IndexOutOfBounds("FriendsChat.FC_PLAYERS"))?
                        .clone();
                    *self
                        .fc_players
                        .as_mut()
                        .and_then(|players| players.get_mut(search as usize))
                        .ok_or(Error::IndexOutOfBounds("FriendsChat.FC_PLAYERS"))? = next;
                    search += 1;
                }
                self.fc_player_count -= 1;
                let last = self.fc_player_count as usize;
                *self
                    .fc_players
                    .as_mut()
                    .and_then(|players| players.get_mut(last))
                    .ok_or(Error::IndexOutOfBounds("FriendsChat.FC_PLAYERS"))? = None;
            }
        } else {
            let world_name = reader.read_cp1252()?;
            let text_name = format_displayname(&username);
            let player = FriendsChatPlayer {
                rank,
                username: username.clone(),
                display_name,
                world_name: world_name.clone(),
                world_id,
                text_name,
            };
            let mut insert = self.fc_player_count - 1;
            while insert >= 0 {
                let existing = self
                    .friends_chat_player(insert)?
                    .text_name
                    .as_deref()
                    .ok_or(Error::NullPointer("FriendsChatPlayer.textName"))?;
                let incoming = player
                    .text_name
                    .as_deref()
                    .ok_or(Error::NullPointer("FriendsChatPlayer.textName"))?;
                match compare_utf16(existing, incoming) {
                    Ordering::Equal => {
                        let target = self
                            .fc_players
                            .as_mut()
                            .and_then(|players| players.get_mut(insert as usize))
                            .and_then(Option::as_mut)
                            .ok_or(Error::NullPointer("FriendsChat.FC_PLAYERS[]"))?;
                        target.world_id = world_id;
                        target.rank = rank;
                        target.world_name = world_name;
                        let my_player = context
                            .my_player
                            .ok_or(Error::NullPointer("PlayerUpdate.MY_PLAYER"))?;
                        if my_player == Some(username.as_str()) {
                            self.my_fc_rank = rank;
                        }
                        self.last_friends_chat_update_tick = context.server_tick;
                        return Ok(());
                    }
                    Ordering::Less => break,
                    Ordering::Greater => {}
                }
                insert -= 1;
            }
            let size = self
                .fc_players
                .as_ref()
                .ok_or(Error::NullPointer("FriendsChat.FC_PLAYERS"))?
                .len() as i32;
            if self.fc_player_count >= size {
                return Ok(());
            }
            let length = self.fc_player_count - 1 - insert;
            if length >= 0 {
                let from = (insert + 1) as usize;
                let players = self
                    .fc_players
                    .as_mut()
                    .ok_or(Error::NullPointer("FriendsChat.FC_PLAYERS"))?;
                if from + length as usize + 1 > players.len() {
                    return Err(Error::IndexOutOfBounds("FriendsChat.FC_PLAYERS"));
                }
                for offset in (0..length as usize).rev() {
                    players[from + offset + 1] = players[from + offset].clone();
                }
            }
            if self.fc_player_count == 0 {
                self.fc_players = Some(vec![None; FRIENDS_CHAT_CAPACITY]);
            }
            let slot = self
                .fc_players
                .as_mut()
                .and_then(|players| players.get_mut((insert + 1) as usize))
                .ok_or(Error::IndexOutOfBounds("FriendsChat.FC_PLAYERS"))?;
            *slot = Some(player);
            self.fc_player_count += 1;
            let my_player = context
                .my_player
                .ok_or(Error::NullPointer("PlayerUpdate.MY_PLAYER"))?;
            if my_player == Some(username.as_str()) {
                self.my_fc_rank = rank;
            }
        }
        self.last_friends_chat_update_tick = context.server_tick;
        Ok(())
    }
}
