use super::Error;
use super::chat::{ChatKind, ChatLine, ChatSession, crown, escape_angle_brackets, read_huffman_message};
use openrs_protocol::payload::PayloadReader;
use openrs_protocol::registry::ServerPacket;
use std::collections::BTreeMap;
use std::fmt;

const HINT_TRAIL_SLOTS: u8 = 9;
const HINT_ARROW_SLOTS: u8 = 8;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SceneError {
    Decode(Error),
    ArrayIndexOutOfBounds { index: i32, length: i32 },
    NegativeArraySize(i32),
}

impl fmt::Display for SceneError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Decode(error) => error.fmt(f),
            Self::ArrayIndexOutOfBounds { index, length } => {
                write!(f, "index {index} out of bounds for length {length}")
            }
            Self::NegativeArraySize(size) => write!(f, "negative array size {size}"),
        }
    }
}

impl std::error::Error for SceneError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Decode(error) => Some(error),
            _ => None,
        }
    }
}

impl From<Error> for SceneError {
    fn from(error: Error) -> Self {
        Self::Decode(error)
    }
}

impl From<openrs_protocol::payload::PayloadError> for SceneError {
    fn from(error: openrs_protocol::payload::PayloadError) -> Self {
        Self::Decode(Error::Payload(error))
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct KnownPlayer {
    pub username: String,
    pub display_name: Option<String>,
    pub has_appearance: bool,
    pub prefix_title: Option<String>,
    pub postfix_title: Option<String>,
}

impl KnownPlayer {
    pub fn username_with_title(&self) -> String {
        let mut full = self.prefix_title.clone().unwrap_or_default();
        full.push_str(&self.username);
        if let Some(postfix) = &self.postfix_title {
            full.push_str(postfix);
        }
        full
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SceneSession {
    pub players: BTreeMap<u16, KnownPlayer>,
    pub map_base_x: i32,
    pub map_base_y: i32,
    pub hint_sprite_count: usize,
    pub cycles_per_second: i32,
    pub message_duration: i32,
}

impl Default for SceneSession {
    fn default() -> Self {
        Self {
            players: BTreeMap::new(),
            map_base_x: 0,
            map_base_y: 0,
            hint_sprite_count: 0,
            cycles_per_second: 50,
            message_duration: 3,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OverheadText {
    pub player_index: u16,
    pub text: String,
    pub colour: i32,
    pub effect: i32,
    pub ticks: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PublicChat {
    pub player_index: u16,
    pub overhead: OverheadText,
    pub line: ChatLine,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PublicQuickchat {
    pub player_index: u16,
    pub effects: u16,
    pub icon: u8,
    pub remainder: Vec<u8>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct HintArrow {
    pub slot: u8,
    pub target_type: u8,
    pub sprite: u8,
    pub x: i32,
    pub y: i32,
    pub plane: u8,
    pub height: i32,
    pub distance: u16,
    pub target_index: u16,
    pub denominator: u16,
    pub model_id: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct HintTrail {
    pub slot: u8,
    pub model_id: i32,
    pub placements: Vec<(i32, i32)>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Cutscene {
    pub key: u16,
    pub xteas: Vec<[i32; 4]>,
    pub buffer: Vec<u8>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SceneEvent {
    PublicChat(PublicChat),
    PublicQuickchat(PublicQuickchat),
    HintArrow(HintArrow),
    HintArrowClear { slot: u8 },
    HintTrail(HintTrail),
    HintTrailClear { slot: u8 },
    Cutscene(Cutscene),
}

impl SceneSession {
    pub fn decode(
        &self,
        chat: &ChatSession,
        packet: ServerPacket,
        payload: &[u8],
    ) -> Result<Option<SceneEvent>, SceneError> {
        let mut r = PayloadReader::new(payload);
        let event = self.decode_body(chat, packet, &mut r, payload)?;
        if r.position() != payload.len() {
            return Err(SceneError::Decode(Error::TrailingBytes {
                consumed: r.position(),
                length: payload.len(),
            }));
        }
        Ok(event)
    }

    fn decode_body(
        &self,
        chat: &ChatSession,
        packet: ServerPacket,
        r: &mut PayloadReader<'_>,
        payload: &[u8],
    ) -> Result<Option<SceneEvent>, SceneError> {
        match packet {
            ServerPacket::MESSAGE_PUBLIC => self.public_chat(chat, r, payload),
            ServerPacket::HINT_ARROW => self.hint_arrow(r),
            ServerPacket::HINT_TRAIL => Self::hint_trail(r),
            ServerPacket::CUTSCENE => Self::cutscene(r),
            other => Err(SceneError::Decode(Error::UnsupportedOpcode(other))),
        }
    }

    fn public_chat(
        &self,
        chat: &ChatSession,
        r: &mut PayloadReader<'_>,
        payload: &[u8],
    ) -> Result<Option<SceneEvent>, SceneError> {
        let player_index = r.read_u16_be()?;
        let Some(player) = self.players.get(&player_index) else {
            return Ok(None);
        };
        let effects = r.read_u16_be()?;
        let icon = r.read_u8()?;
        let quickchat = effects & 0x8000 != 0;
        let Some(display_name) = player.display_name.as_deref() else {
            return Ok(None);
        };
        if !player.has_appearance {
            return Ok(None);
        }
        if chat.public_ignored(icon, quickchat, display_name) {
            return Ok(None);
        }
        if quickchat {
            let remainder = r.read_bytes(r.remaining())?.to_vec();
            return Ok(Some(SceneEvent::PublicQuickchat(PublicQuickchat {
                player_index,
                effects,
                icon,
                remainder,
            })));
        }
        let message = escape_angle_brackets(&read_huffman_message(chat.huffman.as_ref(), r, payload)?);
        let kind = if icon == 1 || icon == 2 {
            ChatKind::StaffChat
        } else {
            ChatKind::PublicChat
        };
        let with_title = player.username_with_title();
        let (crowned_name, name) = match icon {
            2 => (crown(1) + &with_title, crown(1) + display_name),
            1 => (crown(0) + &with_title, crown(0) + display_name),
            _ => (with_title, display_name.to_string()),
        };
        let overhead = OverheadText {
            player_index,
            text: message.trim_matches(|c: char| c <= ' ').to_string(),
            colour: i32::from(effects >> 8),
            effect: i32::from(effects & 0xff),
            ticks: self
                .cycles_per_second
                .wrapping_mul(self.message_duration),
        };
        Ok(Some(SceneEvent::PublicChat(PublicChat {
            player_index,
            overhead,
            line: ChatLine {
                kind,
                crowned_name,
                name,
                simple_name: player.username.clone(),
                message,
                clan: None,
                quickchat_id: -1,
            },
        })))
    }

    fn hint_arrow(&self, r: &mut PayloadReader<'_>) -> Result<Option<SceneEvent>, SceneError> {
        let flags = r.read_u8()?;
        let slot = flags >> 5;
        debug_assert!(slot < HINT_ARROW_SLOTS);
        let mut target_type = flags & 0x1f;
        if target_type == 0 {
            return Ok(Some(SceneEvent::HintArrowClear { slot }));
        }
        let sprite = r.read_u8()?;
        if usize::from(sprite) >= self.hint_sprite_count {
            return Ok(None);
        }
        let mut arrow = HintArrow {
            slot,
            target_type,
            sprite,
            x: 0,
            y: 0,
            plane: 0,
            height: 0,
            distance: 0,
            target_index: 0,
            denominator: 0,
            model_id: 0,
        };
        if target_type == 1 || target_type == 10 {
            arrow.target_index = r.read_u16_be()?;
            arrow.denominator = r.read_u16_be()?;
            r.read_bytes(4)?;
        } else if target_type <= 6 {
            (arrow.x, arrow.y) = match target_type {
                2 => (256, 256),
                3 => (0, 256),
                4 => (512, 256),
                5 => (256, 0),
                6 => (256, 512),
                _ => (0, 0),
            };
            target_type = 2;
            arrow.target_type = target_type;
            arrow.plane = r.read_u8()?;
            let x = i32::from(r.read_u16_be()?);
            let y = i32::from(r.read_u16_be()?);
            arrow.x = arrow
                .x
                .wrapping_add(x.wrapping_sub(self.map_base_x).wrapping_shl(9));
            arrow.y = arrow
                .y
                .wrapping_add(y.wrapping_sub(self.map_base_y).wrapping_shl(9));
            arrow.height = i32::from(r.read_u8()?) << 2;
            arrow.distance = r.read_u16_be()?;
        }
        arrow.model_id = r.read_i32_be()?;
        Ok(Some(SceneEvent::HintArrow(arrow)))
    }

    fn hint_trail(r: &mut PayloadReader<'_>) -> Result<Option<SceneEvent>, SceneError> {
        let slot = r.read_u8()?;
        let model_id = r.read_big_smart()?;
        if slot >= HINT_TRAIL_SLOTS {
            return Err(SceneError::ArrayIndexOutOfBounds {
                index: i32::from(slot),
                length: i32::from(HINT_TRAIL_SLOTS),
            });
        }
        if model_id == -1 {
            return Ok(Some(SceneEvent::HintTrailClear { slot }));
        }
        let length = i32::from(r.read_smart()?);
        if length < 0 {
            return Err(SceneError::NegativeArraySize(length));
        }
        let offset_x = i32::from(r.read_u16_be()?);
        let offset_y = i32::from(r.read_u16_be()?);
        let mut placements = Vec::with_capacity(length as usize);
        for _ in 0..length {
            let x = offset_x + i32::from(r.read_i8()?);
            let y = offset_y + i32::from(r.read_i8()?);
            placements.push((x, y));
        }
        Ok(Some(SceneEvent::HintTrail(HintTrail {
            slot,
            model_id,
            placements,
        })))
    }

    fn cutscene(r: &mut PayloadReader<'_>) -> Result<Option<SceneEvent>, SceneError> {
        let key = r.read_u16_be()?;
        let count = usize::from(r.read_u16_be()?);
        let mut xteas = Vec::with_capacity(count.min(r.remaining() / 16 + 1));
        for _ in 0..count {
            xteas.push([
                r.read_i32_be()?,
                r.read_i32_be()?,
                r.read_i32_be()?,
                r.read_i32_be()?,
            ]);
        }
        let size = usize::from(r.read_u8()?);
        let buffer = r.read_bytes(size)?.to_vec();
        Ok(Some(SceneEvent::Cutscene(Cutscene { key, xteas, buffer })))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn session() -> SceneSession {
        let mut session = SceneSession {
            hint_sprite_count: 4,
            map_base_x: 3200,
            map_base_y: 3200,
            ..SceneSession::default()
        };
        session.players.insert(
            7,
            KnownPlayer {
                username: "zezima".to_string(),
                display_name: Some("Zezima".to_string()),
                has_appearance: true,
                prefix_title: Some("Sir ".to_string()),
                postfix_title: None,
            },
        );
        session
    }

    #[test]
    fn arrow_world_position_is_relative_to_the_map_base() {
        let mut payload = vec![2 << 5 | 3, 1, 1];
        payload.extend_from_slice(&3210_u16.to_be_bytes());
        payload.extend_from_slice(&3195_u16.to_be_bytes());
        payload.push(10);
        payload.extend_from_slice(&600_u16.to_be_bytes());
        payload.extend_from_slice(&(-5_i32).to_be_bytes());
        let event = session()
            .decode(&ChatSession::new(None), ServerPacket::HINT_ARROW, &payload)
            .unwrap();
        let Some(SceneEvent::HintArrow(arrow)) = event else {
            panic!("expected an arrow");
        };
        assert_eq!(arrow.slot, 2);
        assert_eq!(arrow.target_type, 2);
        assert_eq!((arrow.x, arrow.y), (10 * 512, 256 - 5 * 512));
        assert_eq!((arrow.plane, arrow.height, arrow.distance), (1, 40, 600));
        assert_eq!(arrow.model_id, -5);
    }

    #[test]
    fn arrow_clear_and_out_of_range_sprite() {
        let chat = ChatSession::new(None);
        assert_eq!(
            session().decode(&chat, ServerPacket::HINT_ARROW, &[3 << 5]),
            Ok(Some(SceneEvent::HintArrowClear { slot: 3 }))
        );
        assert_eq!(
            session().decode(&chat, ServerPacket::HINT_ARROW, &[1, 4]),
            Ok(None)
        );
    }

    #[test]
    fn trail_errors_follow_the_native_exceptions() {
        let chat = ChatSession::new(None);
        assert_eq!(
            session().decode(&chat, ServerPacket::HINT_TRAIL, &[9, 0, 1]),
            Err(SceneError::ArrayIndexOutOfBounds {
                index: 9,
                length: 9
            })
        );
        assert_eq!(
            session().decode(&chat, ServerPacket::HINT_TRAIL, &[0, 0, 1, 10]),
            Err(SceneError::NegativeArraySize(-54))
        );
        assert_eq!(
            session().decode(&chat, ServerPacket::HINT_TRAIL, &[4, 0x7f, 0xff]),
            Ok(Some(SceneEvent::HintTrailClear { slot: 4 }))
        );
        let payload = [1, 0, 5, 65, 0, 10, 0, 20, 1, 0xff];
        let Ok(Some(SceneEvent::HintTrail(trail))) =
            session().decode(&chat, ServerPacket::HINT_TRAIL, &payload)
        else {
            panic!("expected a trail");
        };
        assert_eq!(trail.placements, vec![(11, 19)]);
    }

    #[test]
    fn cutscene_reads_keys_and_buffer() {
        let mut payload = vec![0, 9, 0, 1];
        for value in [1_i32, -2, 3, -4] {
            payload.extend_from_slice(&value.to_be_bytes());
        }
        payload.extend_from_slice(&[2, 0xaa, 0xbb]);
        let event = session()
            .decode(&ChatSession::new(None), ServerPacket::CUTSCENE, &payload)
            .unwrap();
        assert_eq!(
            event,
            Some(SceneEvent::Cutscene(Cutscene {
                key: 9,
                xteas: vec![[1, -2, 3, -4]],
                buffer: vec![0xaa, 0xbb],
            }))
        );
    }

    #[test]
    fn public_chat_needs_a_known_player_with_appearance() {
        let chat = ChatSession::new(None);
        let payload = [0, 9, 0, 0, 0, 5];
        assert_eq!(
            session().decode(&chat, ServerPacket::MESSAGE_PUBLIC, &payload),
            Err(SceneError::Decode(Error::TrailingBytes {
                consumed: 2,
                length: 6
            }))
        );
        let payload = [0, 7, 0x12, 0x34, 2, 0];
        let Ok(Some(SceneEvent::PublicChat(line))) =
            session().decode(&chat, ServerPacket::MESSAGE_PUBLIC, &payload)
        else {
            panic!("expected a line");
        };
        assert_eq!(line.line.kind, ChatKind::StaffChat);
        assert_eq!(line.line.crowned_name, "<img=1>Sir zezima");
        assert_eq!(line.line.name, "<img=1>Zezima");
        assert_eq!((line.overhead.colour, line.overhead.effect), (0x12, 0x34));
    }
}
