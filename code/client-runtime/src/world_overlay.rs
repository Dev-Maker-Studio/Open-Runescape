use crate::world_draw::{
    ARROW_SLOTS, HintArrowSpec, HintSet, HintTrailSpec, TRAIL_SLOTS,
};
use openrs_cs2::{CHAT_LINE_CAPACITY, ChatLine as StoredLine, ChatState};
use openrs_login::world_packets::chat::{ChatHuffman, ChatKind, ChatLine, ChatSession};
use openrs_login::world_packets::misc::{self, Misc};
use openrs_login::world_packets::scene::{
    Cutscene, HintArrow, HintTrail, OverheadText, SceneEvent, SceneSession,
};
use openrs_protocol::payload::PayloadReader;
use openrs_protocol::registry::ServerPacket;
use serde::Serialize;
use std::collections::BTreeMap;

pub const TYPE_UNFILTERABLE: i32 = 0;
pub const TYPE_STAFF_CHAT: i32 = 1;
pub const TYPE_PUBLIC_CHAT: i32 = 2;
pub const TYPE_PRIVATE_MESSAGE: i32 = 3;
pub const TYPE_GAME: i32 = 4;
pub const TYPE_FRIENDS_STATUS: i32 = 5;
pub const TYPE_PRIVATE_MESSAGE_ECHO: i32 = 6;
pub const TYPE_PRIVATE_STAFF: i32 = 7;
pub const TYPE_FC_CHAT: i32 = 9;
pub const TYPE_FC_NOTIFICATION: i32 = 11;
pub const TYPE_PUBLIC_QUICKCHAT: i32 = 17;
pub const TYPE_CLAN_CHAT: i32 = 41;
pub const TYPE_GUEST_CLAN_CHAT: i32 = 44;
pub const TYPE_DEV_CONSOLE_CLEAR: i32 = 98;
pub const TYPE_DEV_CONSOLE: i32 = 99;
const KNOWN_TYPES: [i32; 15] = [
    TYPE_UNFILTERABLE,
    TYPE_STAFF_CHAT,
    TYPE_PUBLIC_CHAT,
    TYPE_PRIVATE_MESSAGE,
    TYPE_GAME,
    TYPE_FRIENDS_STATUS,
    TYPE_PRIVATE_MESSAGE_ECHO,
    TYPE_PRIVATE_STAFF,
    TYPE_FC_CHAT,
    TYPE_FC_NOTIFICATION,
    TYPE_PUBLIC_QUICKCHAT,
    TYPE_CLAN_CHAT,
    TYPE_GUEST_CLAN_CHAT,
    TYPE_DEV_CONSOLE_CLEAR,
    TYPE_DEV_CONSOLE,
];
const CONSOLE_LINES: usize = 200;

pub fn chat_type_id(kind: ChatKind) -> i32 {
    match kind {
        ChatKind::PrivateMessage => TYPE_PRIVATE_MESSAGE,
        ChatKind::PrivateMessageEcho => TYPE_PRIVATE_MESSAGE_ECHO,
        ChatKind::PrivateStaff => TYPE_PRIVATE_STAFF,
        ChatKind::FriendsChat => TYPE_FC_CHAT,
        ChatKind::ClanChat => TYPE_CLAN_CHAT,
        ChatKind::GuestClanChat => TYPE_GUEST_CLAN_CHAT,
        ChatKind::PublicChat => TYPE_PUBLIC_CHAT,
        ChatKind::StaffChat => TYPE_STAFF_CHAT,
        ChatKind::PublicQuickchat => TYPE_PUBLIC_QUICKCHAT,
    }
}

pub fn message_type_for(id: i32) -> i32 {
    if KNOWN_TYPES.contains(&id) {
        id
    } else {
        TYPE_GAME
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OverheadEntry {
    pub player_index: u16,
    pub text: String,
    pub colour: i32,
    pub effect: i32,
    pub ticks_left: i32,
    pub ticks_visible: i32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CutsceneState {
    pub key: u16,
    pub xtea_sets: usize,
    pub buffer_bytes: usize,
    pub started_cycle: u64,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TrailEntry {
    pub model_id: i32,
    pub plane: u8,
    pub placements: Vec<(i32, i32)>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OverlayEffect {
    pub action: &'static str,
    pub index: Option<u32>,
}

impl OverlayEffect {
    fn new(action: &'static str) -> Self {
        Self {
            action,
            index: None,
        }
    }

    fn at(action: &'static str, index: u32) -> Self {
        Self {
            action,
            index: Some(index),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HintArrowReport {
    pub slot: u8,
    pub target_type: u8,
    pub sprite: u8,
    pub x: i32,
    pub y: i32,
    pub plane: u8,
    pub height: i32,
    pub distance: u16,
    pub target_index: u16,
    pub model_id: i32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OverlayReport {
    pub overhead: Vec<OverheadEntry>,
    pub arrows: Vec<HintArrowReport>,
    pub trails: usize,
    pub cutscene: Option<CutsceneState>,
    pub chat_lines: i32,
    pub console_lines: usize,
}

pub struct MessageEntry<'a> {
    pub kind: i32,
    pub effect_flags: i32,
    pub crowned_name: &'a str,
    pub name: Option<&'a str>,
    pub simple_name: &'a str,
    pub message: &'a str,
    pub clan: Option<&'a str>,
    pub quickchat_message_id: i32,
}

pub struct SceneOverlay {
    chat: ChatSession,
    state: ChatState,
    identifier: i32,
    sprite_count: usize,
    overhead: BTreeMap<u16, OverheadEntry>,
    arrows: [Option<HintArrow>; ARROW_SLOTS],
    trails: [Option<TrailEntry>; TRAIL_SLOTS],
    cutscene: Option<CutsceneState>,
    console: Vec<String>,
    console_text: String,
    cycle: u64,
}

impl Default for SceneOverlay {
    fn default() -> Self {
        Self {
            chat: ChatSession::new(None),
            state: ChatState::default(),
            identifier: 0,
            sprite_count: 256,
            overhead: BTreeMap::new(),
            arrows: Default::default(),
            trails: Default::default(),
            cutscene: None,
            console: Vec::new(),
            console_text: String::new(),
            cycle: 0,
        }
    }
}

impl SceneOverlay {
    pub fn handles(packet: ServerPacket) -> bool {
        matches!(
            packet,
            ServerPacket::MESSAGE_PUBLIC
                | ServerPacket::HINT_ARROW
                | ServerPacket::HINT_TRAIL
                | ServerPacket::CUTSCENE
                | ServerPacket::GAME_MESSAGE
                | ServerPacket::MESSAGE_PRIVATE
                | ServerPacket::MESSAGE_PRIVATE_ECHO
                | ServerPacket::MESSAGE_FRIENDS_CHAT
                | ServerPacket::MESSAGE_CLANCHANNEL
        )
    }

    pub fn reset_world(&mut self) {
        let huffman = self.chat.huffman.take();
        let ignored = std::mem::take(&mut self.chat.ignored);
        let state = std::mem::take(&mut self.state);
        let identifier = self.identifier;
        let sprite_count = self.sprite_count;
        *self = Self::default();
        self.chat.huffman = huffman;
        self.chat.ignored = ignored;
        self.state = state;
        self.identifier = identifier;
        self.sprite_count = sprite_count;
    }

    pub fn set_huffman(&mut self, code_lengths: &[i8]) {
        self.chat.huffman = Some(ChatHuffman::new(code_lengths));
    }

    pub fn cycle(&self) -> u64 {
        self.cycle
    }

    pub fn set_hint_sprite_count(&mut self, count: usize) {
        self.sprite_count = count;
    }

    pub fn set_ignored(&mut self, ignored: Vec<openrs_login::world_packets::chat::IgnoredName>) {
        self.chat.ignored = ignored;
    }

    pub fn chat_state(&mut self) -> &mut ChatState {
        &mut self.state
    }

    pub fn chat(&mut self) -> &mut ChatSession {
        &mut self.chat
    }

    pub fn console_lines(&self) -> &[String] {
        &self.console
    }

    pub fn console_text(&self) -> &str {
        &self.console_text
    }

    pub fn cutscene(&self) -> Option<&CutsceneState> {
        self.cutscene.as_ref()
    }

    pub fn overhead(&self) -> impl Iterator<Item = &OverheadEntry> {
        self.overhead.values()
    }

    pub fn arrow(&self, slot: usize) -> Option<&HintArrow> {
        self.arrows.get(slot).and_then(Option::as_ref)
    }

    pub fn trail(&self, slot: usize) -> Option<&TrailEntry> {
        self.trails.get(slot).and_then(Option::as_ref)
    }

    pub fn hint_set(&self) -> HintSet {
        let arrows = self
            .arrows
            .iter()
            .flatten()
            .map(|arrow| HintArrowSpec {
                slot: arrow.slot,
                target_type: arrow.target_type,
                x: arrow.x,
                y: arrow.y,
                distance: arrow.distance,
                target_index: arrow.target_index,
                model_id: arrow.model_id,
            })
            .collect();
        let trails = self
            .trails
            .iter()
            .enumerate()
            .filter_map(|(slot, trail)| {
                let trail = trail.as_ref()?;
                Some(HintTrailSpec {
                    slot: slot as u8,
                    model_id: trail.model_id,
                    plane: trail.plane,
                    placements: trail.placements.clone(),
                })
            })
            .collect();
        HintSet { arrows, trails }
    }

    pub fn report(&self) -> OverlayReport {
        OverlayReport {
            overhead: self.overhead.values().cloned().collect(),
            arrows: self
                .arrows
                .iter()
                .flatten()
                .map(|arrow| HintArrowReport {
                    slot: arrow.slot,
                    target_type: arrow.target_type,
                    sprite: arrow.sprite,
                    x: arrow.x,
                    y: arrow.y,
                    plane: arrow.plane,
                    height: arrow.height,
                    distance: arrow.distance,
                    target_index: arrow.target_index,
                    model_id: arrow.model_id,
                })
                .collect(),
            trails: self.trails.iter().flatten().count(),
            cutscene: self.cutscene.clone(),
            chat_lines: self.state.line_count,
            console_lines: self.console.len(),
        }
    }

    pub fn session_template(&self, base: (i32, i32)) -> SceneSession {
        SceneSession {
            map_base_x: base.0,
            map_base_y: base.1,
            hint_sprite_count: self.sprite_count,
            ..SceneSession::default()
        }
    }

    pub fn advance_cycles(&mut self, cycles: u32, cycle: u64) {
        self.cycle = cycle;
        for _ in 0..cycles {
            if self.overhead.is_empty() {
                break;
            }
            self.overhead.retain(|_, entry| {
                entry.ticks_left -= 1;
                entry.ticks_left != 0
            });
        }
    }

    pub fn map_rebuilt(&mut self) -> bool {
        self.cutscene.take().is_some()
    }

    pub fn write_line(&mut self, kind: i32, effect_flags: i32, line: &ChatLine) {
        self.write_message(MessageEntry {
            kind,
            effect_flags,
            crowned_name: &line.crowned_name,
            name: Some(&line.name),
            simple_name: &line.simple_name,
            message: &line.message,
            clan: line.clan.as_deref(),
            quickchat_message_id: line.quickchat_id,
        });
    }

    pub fn write_message(&mut self, entry: MessageEntry<'_>) {
        let lines = &mut self.state.lines;
        lines.truncate(CHAT_LINE_CAPACITY - 1);
        lines.insert(
            0,
            Some(StoredLine {
                identifier: self.identifier,
                time: self.cycle as i32,
                kind: Some(entry.kind),
                effect_flags: entry.effect_flags,
                crowned_name: Some(entry.crowned_name.to_string()),
                name: entry.name.map(str::to_string),
                simple_name: Some(entry.simple_name.to_string()),
                clan: entry.clan.map(str::to_string),
                quickchat_message_id: entry.quickchat_message_id,
                message: Some(entry.message.to_string()),
            }),
        );
        self.identifier = self.identifier.wrapping_add(1);
        self.state.line_count = self.state.line_count.wrapping_add(1);
    }

    pub fn apply(
        &mut self,
        packet: ServerPacket,
        payload: &[u8],
        session: &SceneSession,
        plane: u8,
    ) -> OverlayEffect {
        match packet {
            ServerPacket::GAME_MESSAGE => self.game_message(packet, payload),
            ServerPacket::MESSAGE_PRIVATE
            | ServerPacket::MESSAGE_PRIVATE_ECHO
            | ServerPacket::MESSAGE_FRIENDS_CHAT
            | ServerPacket::MESSAGE_CLANCHANNEL => match self.chat.decode(packet, payload) {
                Ok(Some(line)) => {
                    self.write_line(chat_type_id(line.kind), 0, &line);
                    OverlayEffect::at("chatLine", chat_type_id(line.kind) as u32)
                }
                Ok(None) => OverlayEffect::new("ignored"),
                Err(_) => OverlayEffect::new("decodeError"),
            },
            _ => match session.decode(&self.chat, packet, payload) {
                Ok(Some(event)) => self.scene_event(event, plane),
                Ok(None) => OverlayEffect::new("ignored"),
                Err(_) => OverlayEffect::new("decodeError"),
            },
        }
    }

    fn game_message(&mut self, packet: ServerPacket, payload: &[u8]) -> OverlayEffect {
        let mut reader = PayloadReader::new(payload);
        let decoded = misc::decode(packet, payload.len(), &mut reader);
        let Ok(Misc::GameMessage {
            kind,
            effect_flags,
            flags: _,
            name,
            name_formatted,
            message,
        }) = decoded
        else {
            return OverlayEffect::new("decodeError");
        };
        if reader.position() != payload.len() {
            return OverlayEffect::new("decodeError");
        }
        let kind = message_type_for(i32::from(kind));
        let simple = name.unwrap_or_default();
        let formatted = name_formatted.unwrap_or_else(|| simple.clone());
        match kind {
            TYPE_DEV_CONSOLE => {
                self.console.push(message);
                if self.console.len() > CONSOLE_LINES {
                    self.console.remove(0);
                }
                OverlayEffect::new("devConsole")
            }
            TYPE_DEV_CONSOLE_CLEAR => {
                self.console_text = message;
                OverlayEffect::new("devConsoleText")
            }
            _ => {
                if !formatted.is_empty() && self.chat.is_ignored(&formatted) {
                    return OverlayEffect::new("ignored");
                }
                self.write_message(MessageEntry {
                    kind,
                    effect_flags,
                    crowned_name: &simple,
                    name: Some(&formatted),
                    simple_name: &simple,
                    message: &message,
                    clan: None,
                    quickchat_message_id: -1,
                });
                OverlayEffect::at("chatLine", kind as u32)
            }
        }
    }

    fn scene_event(&mut self, event: SceneEvent, plane: u8) -> OverlayEffect {
        match event {
            SceneEvent::PublicChat(chat) => {
                let OverheadText {
                    player_index,
                    text,
                    colour,
                    effect,
                    ticks,
                } = chat.overhead;
                self.overhead.insert(
                    player_index,
                    OverheadEntry {
                        player_index,
                        text,
                        colour,
                        effect,
                        ticks_left: ticks,
                        ticks_visible: ticks,
                    },
                );
                self.write_line(chat_type_id(chat.line.kind), 0, &chat.line);
                OverlayEffect::at("publicChat", u32::from(player_index))
            }
            SceneEvent::PublicQuickchat(quick) => {
                OverlayEffect::at("publicQuickchat", u32::from(quick.player_index))
            }
            SceneEvent::HintArrow(arrow) => {
                let slot = usize::from(arrow.slot);
                let index = u32::from(arrow.slot);
                if let Some(entry) = self.arrows.get_mut(slot) {
                    *entry = Some(arrow);
                }
                OverlayEffect::at("hintArrow", index)
            }
            SceneEvent::HintArrowClear { slot } => {
                if let Some(entry) = self.arrows.get_mut(usize::from(slot)) {
                    *entry = None;
                }
                OverlayEffect::at("hintArrowClear", u32::from(slot))
            }
            SceneEvent::HintTrail(HintTrail {
                slot,
                model_id,
                placements,
            }) => {
                if let Some(entry) = self.trails.get_mut(usize::from(slot)) {
                    *entry = Some(TrailEntry {
                        model_id,
                        plane,
                        placements,
                    });
                }
                OverlayEffect::at("hintTrail", u32::from(slot))
            }
            SceneEvent::HintTrailClear { slot } => {
                if let Some(entry) = self.trails.get_mut(usize::from(slot)) {
                    *entry = None;
                }
                OverlayEffect::at("hintTrailClear", u32::from(slot))
            }
            SceneEvent::Cutscene(Cutscene { key, xteas, buffer }) => {
                self.cutscene = Some(CutsceneState {
                    key,
                    xtea_sets: xteas.len(),
                    buffer_bytes: buffer.len(),
                    started_cycle: self.cycle,
                });
                OverlayEffect::at("cutscene", u32::from(key))
            }
        }
    }
}

#[cfg(test)]
mod tests;
