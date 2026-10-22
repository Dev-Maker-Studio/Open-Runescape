use crate::names::{decode_friends_chat_name, encode_cp1252, format_displayname, names_match, utf16_length};
use crate::{Friend, HostError, Ignore, SocialState, FRIEND_CAPACITY, IGNORE_CAPACITY};

pub const PACKET_BUFFER_CAPACITY: usize = 260;

pub const REMOVE_IGNORE: u8 = 12;
pub const ADD_FRIEND: u8 = 26;
pub const REMOVE_FRIEND: u8 = 29;
pub const ADD_IGNORE: u8 = 34;
pub const FC_JOIN: u8 = 71;
pub const FC_SET_RANK: u8 = 7;
pub const FC_KICK: u8 = 91;
pub const CLANCHANNEL_KICKUSER: u8 = 90;
pub const UNUSED_CLAN_OP: u8 = 39;

pub const FRIEND_LIST_FULL: &str = "Your friends list is full (200 names maximum)";
pub const ALREADY_ON_FRIENDS_LIST: &str = " is already on your friends list.";
pub const IGNORE_LIST_FULL: &str = "Your ignore list is full. Max of 100 users.";
pub const ALREADY_IGNORED: &str = " is already on your ignore list.";
pub const CANT_ADD_SELF_TO_FRIEND: &str = "You can't add yourself to your own friends list.";
pub const CANT_ADD_SELF_TO_IGNORE: &str = "You can't add yourself to your own ignore list.";
pub const PLEASE_REMOVE: &str = "Please remove ";
pub const FROM_IGNORE_FIRST: &str = " from your ignore list first.";
pub const FROM_FRIENDS_FIRST: &str = " from your friends list first.";

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ClientPacket {
    pub opcode: u8,
    pub payload: Vec<u8>,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum ChatSend {
    Public {
        color: u8,
        effect: u8,
        message: String,
    },
    Private {
        recipient: String,
        message: String,
    },
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct TypedMessage {
    pub message_type: i32,
    pub effect_flags: i32,
    pub text: String,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct Outbox {
    pub typed_messages: Vec<TypedMessage>,
    pub console_clears: Vec<String>,
    pub route_requests: Vec<RouteRequest>,
    pub packets: Vec<ClientPacket>,
    pub chat_sends: Vec<ChatSend>,
    pub game_messages: Vec<String>,
    pub console_lines: Vec<String>,
    pub friend_ignore_updates: u32,
    pub interface_closes: u32,
}

impl Outbox {
    pub fn push_packet(&mut self, packet: ClientPacket) {
        self.packets.push(packet);
    }

    pub fn drain_packets(&mut self) -> Vec<ClientPacket> {
        std::mem::take(&mut self.packets)
    }

    pub fn drain_chat_sends(&mut self) -> Vec<ChatSend> {
        std::mem::take(&mut self.chat_sends)
    }

    pub fn drain_game_messages(&mut self) -> Vec<String> {
        std::mem::take(&mut self.game_messages)
    }
}

#[derive(Debug, Default)]
pub(crate) struct Effects {
    pub route_requests: Vec<RouteRequest>,
    pub packets: Vec<ClientPacket>,
    pub chat_sends: Vec<ChatSend>,
    pub game_messages: Vec<String>,
    pub friend_ignore_updates: u32,
    pub interface_closes: u32,
}

impl Effects {
    pub(crate) fn apply(self, outbox: &mut Outbox) {
        outbox.packets.extend(self.packets);
        outbox.route_requests.extend(self.route_requests);
        outbox.chat_sends.extend(self.chat_sends);
        outbox.game_messages.extend(self.game_messages);
        outbox.friend_ignore_updates += self.friend_ignore_updates;
        outbox.interface_closes += self.interface_closes;
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct RouteRequest {
    pub x: i32,
    pub y: i32,
    pub size: i32,
    pub find_alternative: bool,
}

struct Writer {
    bytes: Vec<u8>,
}

impl Writer {
    fn new(opcode: u8) -> Self {
        Self { bytes: vec![opcode] }
    }

    fn check(&self) -> Result<(), HostError> {
        if self.bytes.len() > PACKET_BUFFER_CAPACITY {
            Err(HostError::State("packet buffer overflow".to_string()))
        } else {
            Ok(())
        }
    }

    fn byte(&mut self, value: i32) -> Result<(), HostError> {
        self.bytes.push(value as u8);
        self.check()
    }

    fn short(&mut self, value: i32) -> Result<(), HostError> {
        self.bytes.push((value >> 8) as u8);
        self.bytes.push(value as u8);
        self.check()
    }

    fn short_little(&mut self, value: i32) -> Result<(), HostError> {
        self.bytes.push(value as u8);
        self.bytes.push((value >> 8) as u8);
        self.check()
    }

    fn int_inverse_middle(&mut self, value: i32) -> Result<(), HostError> {
        self.bytes.push((value >> 16) as u8);
        self.bytes.push((value >> 24) as u8);
        self.bytes.push(value as u8);
        self.bytes.push((value >> 8) as u8);
        self.check()
    }

    fn int(&mut self, value: i32) -> Result<(), HostError> {
        self.bytes.extend_from_slice(&value.to_be_bytes());
        self.check()
    }

    fn string(&mut self, text: &str) -> Result<(), HostError> {
        if text.contains('\0') {
            return Err(HostError::State("string contains null character".to_string()));
        }
        for unit in text.encode_utf16() {
            self.bytes.push(encode_cp1252(unit));
            self.check()?;
        }
        self.bytes.push(0);
        self.check()
    }

    fn finish(self) -> ClientPacket {
        let mut bytes = self.bytes;
        let opcode = bytes.remove(0);
        ClientPacket {
            opcode,
            payload: bytes,
        }
    }
}

fn name_packet(opcode: u8, name: &str, extra: Option<i32>) -> Result<ClientPacket, HostError> {
    let length = utf16_length(name) as i32 + 1;
    let mut writer = Writer::new(opcode);
    writer.byte(length + i32::from(extra.is_some()))?;
    writer.string(name)?;
    if let Some(extra) = extra {
        writer.byte(extra)?;
    }
    Ok(writer.finish())
}

fn no_entity() -> HostError {
    HostError::NoEntity
}

fn my_formatted(social: &SocialState) -> Result<Option<String>, HostError> {
    let name = social
        .my_display_name
        .as_ref()
        .ok_or_else(no_entity)?
        .as_ref()
        .ok_or_else(no_entity)?;
    Ok(format_displayname(name))
}

fn formatted_equals(candidate: &str, target: &str) -> bool {
    format_displayname(candidate).as_deref() == Some(target)
}

fn friend_known(friend: &Friend, target: &str) -> bool {
    formatted_equals(&friend.display_name, target)
        || formatted_equals(&friend.previous_display_name, target)
}

pub(crate) fn friend_add(
    social: &SocialState,
    name: &str,
    effects: &mut Effects,
) -> Result<(), HostError> {
    if social.friends.len() >= FRIEND_CAPACITY {
        effects.game_messages.push(FRIEND_LIST_FULL.to_string());
        return Ok(());
    }
    let Some(target) = format_displayname(name) else {
        return Ok(());
    };
    if social.friends.iter().any(|friend| friend_known(friend, &target)) {
        effects
            .game_messages
            .push(format!("{name}{ALREADY_ON_FRIENDS_LIST}"));
        return Ok(());
    }
    for ignore in &social.ignores {
        let unfiltered = ignore.unfiltered_username.as_deref().ok_or_else(no_entity)?;
        if formatted_equals(unfiltered, &target)
            || ignore
                .display_name
                .as_deref()
                .is_some_and(|display| formatted_equals(display, &target))
        {
            effects
                .game_messages
                .push(format!("{PLEASE_REMOVE}{name}{FROM_IGNORE_FIRST}"));
            return Ok(());
        }
    }
    if my_formatted(social)?.as_deref() == Some(target.as_str()) {
        effects.game_messages.push(CANT_ADD_SELF_TO_FRIEND.to_string());
    } else {
        effects.packets.push(name_packet(ADD_FRIEND, name, None)?);
    }
    Ok(())
}

pub(crate) fn friend_del(
    social: &mut SocialState,
    name: &str,
    effects: &mut Effects,
) -> Result<(), HostError> {
    let Some(target) = format_displayname(name) else {
        return Ok(());
    };
    let found = social.friends.iter().position(|friend| {
        names_match(
            name,
            Some(&target),
            Some(&friend.display_name),
            format_displayname(&friend.display_name).as_deref(),
        )
    });
    if let Some(index) = found {
        social.friends.remove(index);
        effects.friend_ignore_updates += 1;
        effects.packets.push(name_packet(REMOVE_FRIEND, name, None)?);
    }
    Ok(())
}

fn ignore_known(ignore: &Ignore, target: &str) -> Result<bool, HostError> {
    let unfiltered = ignore.unfiltered_username.as_deref().ok_or_else(no_entity)?;
    Ok(formatted_equals(unfiltered, target)
        || ignore
            .display_name
            .as_deref()
            .is_some_and(|display| formatted_equals(display, target)))
}

pub(crate) fn ignore_add(
    social: &SocialState,
    name: &str,
    temporary: bool,
    effects: &mut Effects,
) -> Result<(), HostError> {
    if social.ignores.len() >= IGNORE_CAPACITY {
        effects.game_messages.push(IGNORE_LIST_FULL.to_string());
        return Ok(());
    }
    let Some(target) = format_displayname(name) else {
        return Ok(());
    };
    for ignore in &social.ignores {
        if ignore_known(ignore, &target)? {
            effects
                .game_messages
                .push(format!("{name}{ALREADY_IGNORED}"));
            return Ok(());
        }
    }
    if social.friends.iter().any(|friend| friend_known(friend, &target)) {
        effects
            .game_messages
            .push(format!("{PLEASE_REMOVE}{name}{FROM_FRIENDS_FIRST}"));
        return Ok(());
    }
    if my_formatted(social)?.as_deref() == Some(target.as_str()) {
        effects.game_messages.push(CANT_ADD_SELF_TO_IGNORE.to_string());
    } else {
        effects
            .packets
            .push(name_packet(ADD_IGNORE, name, Some(i32::from(temporary)))?);
    }
    Ok(())
}

pub(crate) fn ignore_del(
    social: &mut SocialState,
    name: &str,
    effects: &mut Effects,
) -> Result<(), HostError> {
    let Some(target) = format_displayname(name) else {
        return Ok(());
    };
    let found = social.ignores.iter().position(|ignore| {
        let raw = ignore.unfiltered_username.as_deref();
        names_match(
            name,
            Some(&target),
            raw,
            raw.and_then(format_displayname).as_deref(),
        )
    });
    if let Some(index) = found {
        social.ignores.remove(index);
        effects.friend_ignore_updates += 1;
        effects.packets.push(name_packet(REMOVE_IGNORE, name, None)?);
    }
    Ok(())
}

pub(crate) fn friends_chat_kick(
    social: &SocialState,
    name: &str,
    effects: &mut Effects,
) -> Result<(), HostError> {
    if social.fc_players.is_some() {
        effects.packets.push(name_packet(FC_KICK, name, None)?);
    }
    Ok(())
}

pub(crate) fn friends_chat_set_rank(name: &str, rank: i32) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(FC_SET_RANK);
    writer.byte(1 + utf16_length(name) as i32 + 1)?;
    writer.byte(128_i32.wrapping_sub(rank))?;
    writer.string(name)?;
    Ok(writer.finish())
}

pub(crate) fn friends_chat_join(name: &str, effects: &mut Effects) -> Result<(), HostError> {
    if !name.is_empty() {
        effects.packets.push(name_packet(FC_JOIN, name, None)?);
    }
    Ok(())
}

pub(crate) fn friends_chat_leave() -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(FC_JOIN);
    writer.byte(0)?;
    Ok(writer.finish())
}

pub(crate) fn friends_chat_display_name(social: &SocialState) -> String {
    social
        .fc_name
        .as_deref()
        .map(decode_friends_chat_name)
        .unwrap_or_default()
}

pub(crate) fn clan_channel_kick(
    main_channel: bool,
    slot: i32,
    name: &str,
) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(CLANCHANNEL_KICKUSER);
    writer.byte(3 + utf16_length(name) as i32 + 1)?;
    writer.byte(i32::from(main_channel))?;
    writer.short(slot)?;
    writer.string(name)?;
    Ok(writer.finish())
}

pub(crate) fn clan_operation(slot: i32, name: &str) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(UNUSED_CLAN_OP);
    writer.byte(2 + utf16_length(name) as i32 + 1)?;
    writer.short(slot)?;
    writer.string(name)?;
    Ok(writer.finish())
}

pub const CHAT_SET_FILTER: u8 = 20;
pub const CHAT_TYPE: u8 = 30;
pub const REPORT_ABUSE: u8 = 100;

pub(crate) fn chat_set_filter(public: i32, private: i32, trade: i32) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(CHAT_SET_FILTER);
    writer.byte(public)?;
    writer.byte(private)?;
    writer.byte(trade)?;
    Ok(writer.finish())
}

pub(crate) fn chat_set_mode(mode: i32) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(CHAT_TYPE);
    writer.byte(mode)?;
    Ok(writer.finish())
}

pub(crate) fn report_abuse(
    name: &str,
    comment: &str,
    rule: i32,
    mute: i32,
) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(REPORT_ABUSE);
    writer.byte(utf16_length(name) as i32 + 1 + 2 + utf16_length(comment) as i32 + 1)?;
    writer.string(name)?;
    writer.byte(rule.wrapping_sub(1))?;
    writer.byte(mute)?;
    writer.string(comment)?;
    Ok(writer.finish())
}

pub const RESUME_HSLDIALOG: u8 = 11;
pub const GRAND_EXCHANGE_ITEM_SELECT: u8 = 17;
pub const RESUME_COUNTDIALOG: u8 = 58;
pub const RESUME_CLANFORUMQFCDIALOG: u8 = 69;
pub const RESUME_NAMEDIALOG: u8 = 80;
pub const RESUME_TEXTDIALOG: u8 = 87;

pub(crate) fn resume_count(count: i32) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(RESUME_COUNTDIALOG);
    writer.int(count)?;
    Ok(writer.finish())
}

pub(crate) fn resume_short(opcode: u8, value: i32) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(opcode);
    writer.short(value)?;
    Ok(writer.finish())
}

pub(crate) fn resume_text(opcode: u8, text: &str) -> Result<ClientPacket, HostError> {
    name_packet(opcode, text, None)
}

pub const QUICKCHAT_PRIVATE: u8 = 14;
pub const QUICKCHAT_PUBLIC: u8 = 64;

pub(crate) enum QuickChatTarget<'a> {
    Channel(i32),
    Private(&'a str),
}

pub(crate) fn quick_chat_packet(
    target: QuickChatTarget,
    id: Option<i32>,
    sizes: &[Option<i32>],
    values: &[i32],
) -> Result<ClientPacket, HostError> {
    let mut writer = match target {
        QuickChatTarget::Channel(_) => Writer::new(QUICKCHAT_PUBLIC),
        QuickChatTarget::Private(_) => Writer::new(QUICKCHAT_PRIVATE),
    };
    writer.byte(0)?;
    match target {
        QuickChatTarget::Channel(kind) => writer.byte(kind)?,
        QuickChatTarget::Private(name) => writer.string(name)?,
    }
    let Some(id) = id else {
        return Err(no_entity());
    };
    writer.short(id)?;
    for (size, value) in sizes.iter().zip(values) {
        let size = size.ok_or_else(no_entity)?;
        if size > 0 {
            if size > 8 {
                return Err(HostError::State("size must be between 1 and 8".to_string()));
            }
            for shift in (0..size).rev() {
                writer.byte(((i64::from(*value)) >> (shift * 8)) as i32)?;
            }
        }
    }
    let length = writer.bytes.len() - 2;
    writer.bytes[1] = length as u8;
    Ok(writer.finish())
}



pub(crate) fn player_operation(operation: i32, index: i32) -> Result<Option<ClientPacket>, HostError> {
    let opcode = match operation {
        1 => 66,
        4 => 89,
        5 => 103,
        6 => 1,
        7 => 51,
        9 => 53,
        _ => return Ok(None),
    };
    let mut writer = Writer::new(opcode);
    writer.short(index)?;
    writer.byte(128)?;
    Ok(Some(writer.finish()))
}

pub(crate) fn interface_on_player(
    slot: i32,
    index: i32,
    hash: i32,
    item: i32,
) -> Result<ClientPacket, HostError> {
    let mut writer = Writer::new(13);
    writer.short(slot)?;
    writer.short_little(index)?;
    writer.byte(128)?;
    writer.int_inverse_middle(hash)?;
    writer.short_little(item)?;
    Ok(writer.finish())
}
