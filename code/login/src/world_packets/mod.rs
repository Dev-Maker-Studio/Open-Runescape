pub mod chat;
pub mod misc;
pub mod pulse;
pub mod scene;
pub mod zone;

use openrs_protocol::payload::{PayloadError, PayloadReader};
use openrs_protocol::registry::ServerPacket;
use pulse::*;
use std::fmt;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    TrailingBytes { consumed: usize, length: usize },
    UnsupportedOpcode(ServerPacket),
    InvalidSubPacket(u8),
    Payload(PayloadError),
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::TrailingBytes { consumed, length } => {
                write!(f, "packet consumed {consumed} of {length} bytes")
            }
            Self::UnsupportedOpcode(opcode) => write!(f, "unsupported opcode {opcode:?}"),
            Self::InvalidSubPacket(index) => write!(f, "invalid zone sub-packet {index}"),
            Self::Payload(error) => write!(f, "payload error: {error}"),
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

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum WorldPacket {
    VarBit { id: u16, value: i32 },
    Pulse(PulseUpdate),
    OpenTop(OpenTop),
    OpenSub(OpenSub),
    CloseSub { parent: i32 },
    MoveSub { from: i32, to: i32 },
    SetTargetParam(TargetParam),
    ResetTargetParam(ResetTargetParam),
    RunCs2Script(RunScript),
    PlayerOption(PlayerOption),
    Empty,
    Value(i32),
    Flag(bool),
    Misc(misc::Misc),
    Zone(zone::Zone),
    OwnPlayerContent { hash: i32, kind: i32 },
}

impl WorldPacket {
    pub fn own_player_pulse(hash: i32, kind: i32, my_player_index: i32) -> PulseUpdate {
        PulseUpdate::new(IF_SETCONTENT, hash).with(kind, my_player_index, 0)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OpenTop {
    pub interface_id: u16,
    pub kind: u8,
    pub xtea: [i32; 4],
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SubTarget {
    None,
    Player(u16),
    Npc(u16),
    Object {
        id: i32,
        position: u32,
        shape: u8,
        rotation: u8,
    },
    GroundItem {
        id: u16,
        position: u32,
    },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OpenSub {
    pub interface_id: i32,
    pub parent: i32,
    pub overlay: u8,
    pub xtea: [i32; 4],
    pub target: SubTarget,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TargetParam {
    pub from_slot: i32,
    pub to_slot: i32,
    pub hash: i32,
    pub settings: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ResetTargetParam {
    pub hash: i32,
    pub interface_id: u16,
    pub from_slot: i32,
    pub to_slot: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ScriptArg {
    Int(i32),
    Text(String),
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RunScript {
    pub script_id: i32,
    pub args: Vec<ScriptArg>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PlayerOption {
    pub option: String,
    pub slot: u8,
    pub cursor: i32,
    pub on_top: bool,
}

pub(crate) fn none_if_ffff(value: u16) -> i32 {
    if value == 0xffff {
        -1
    } else {
        i32::from(value)
    }
}

pub fn decode(packet: ServerPacket, payload: &[u8]) -> Result<WorldPacket, Error> {
    decode_with(packet, payload, &mut |_, _| {
        zone::CustomizeCounts::default()
    })
}

pub fn decode_with(
    packet: ServerPacket,
    payload: &[u8],
    counts: &mut impl FnMut(i32, u8) -> zone::CustomizeCounts,
) -> Result<WorldPacket, Error> {
    let mut r = PayloadReader::new(payload);
    let decoded = decode_body(packet, payload.len(), &mut r, counts)?;
    if r.position() != payload.len() {
        return Err(Error::TrailingBytes {
            consumed: r.position(),
            length: payload.len(),
        });
    }
    Ok(decoded)
}

fn decode_body(
    packet: ServerPacket,
    size: usize,
    r: &mut PayloadReader<'_>,
    counts: &mut impl FnMut(i32, u8) -> zone::CustomizeCounts,
) -> Result<WorldPacket, Error> {
    use ServerPacket as P;
    let decoded = match packet {
        P::VARBIT_SMALL => {
            let id = r.read_u16_be()?;
            let value = i32::from(r.read_u8_a()?);
            WorldPacket::VarBit { id, value }
        }
        P::VARBIT_LARGE => {
            let value = r.read_i32_inverse_middle()?;
            let id = r.read_u16_be_a()?;
            WorldPacket::VarBit { id, value }
        }
        P::CLIENT_SETVARC_SMALL => {
            let value = i32::from(r.read_i8_s()?);
            let key = r.read_u16_le()?;
            WorldPacket::Pulse(PulseUpdate::new(CLIENT_SETVARC, i32::from(key)).with(value, 0, 0))
        }
        P::CLIENT_SETVARC_LARGE => {
            let key = r.read_u16_le()?;
            let value = r.read_i32_inverse_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(CLIENT_SETVARC, i32::from(key)).with(value, 0, 0))
        }
        P::CLIENT_SETVARCSTR_SMALL => {
            let text = r.read_cp1252()?;
            let key = r.read_u16_le_a()?;
            WorldPacket::Pulse(PulseUpdate::new(CLIENT_SETVARCSTR, i32::from(key)).with_text(text))
        }
        P::CLIENT_SETVARCSTR_LARGE => {
            let key = r.read_u16_le()?;
            let text = r.read_cp1252()?;
            WorldPacket::Pulse(PulseUpdate::new(CLIENT_SETVARCSTR, i32::from(key)).with_text(text))
        }
        P::IF_SETMODEL => {
            let hash = r.read_i32_be()?;
            let model = r.read_i32_inverse_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETCONTENT, hash).with(1, model, -1))
        }
        P::IF_SETANGLE => {
            let hash = r.read_i32_be()?;
            let pitch = i32::from(r.read_u16_le()?);
            let roll = i32::from(r.read_u16_le()?);
            let scale = i32::from(r.read_u16_le_a()?);
            WorldPacket::Pulse(PulseUpdate::new(IF_SETANGLE, hash).with(pitch, roll, scale))
        }
        P::IF_SETTEXT => {
            let text = r.read_cp1252()?;
            let hash = r.read_i32_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETTEXT, hash).with_text(text))
        }
        P::IF_SETPLAYERMODEL_OTHER => {
            let primary = i32::from(r.read_u16_be_a()?);
            let hash = r.read_i32_middle()?;
            let secondary = r.read_i32_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETCONTENT, hash).with(5, primary, secondary))
        }
        P::IF_SETPLAYERHEAD_OTHER => {
            let key = r.read_i32_be()?;
            let flags = i32::from(r.read_u16_le_a()?);
            let hash = r.read_i32_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETCONTENT, hash).with(3, flags, key))
        }
        P::IF_SETNPCHEAD => {
            let hash = r.read_i32_le()?;
            let npc = r.read_i32_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETCONTENT, hash).with(2, npc, -1))
        }
        P::IF_SETPLAYERHEAD_IGNOREWORN => {
            let kit1 = r.read_u16_le()? as i32;
            let meme = i32::from(r.read_u16_be_a()?);
            let kit2 = i32::from(r.read_u16_be_a()?);
            let hash = r.read_i32_inverse_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETCONTENT, hash).with(
                7,
                (kit1 << 16) | kit2,
                meme,
            ))
        }
        P::IF_SETITEM => {
            let item = none_if_ffff(r.read_u16_be()?);
            let hash = r.read_i32_be()?;
            let amount = r.read_i32_inverse_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETITEM, hash).with(item, amount, 0))
        }
        P::IF_SETPLAYERMODEL => WorldPacket::OwnPlayerContent {
            hash: r.read_i32_middle()?,
            kind: 5,
        },
        P::IF_SETPLAYERHEAD => WorldPacket::OwnPlayerContent {
            hash: r.read_i32_le()?,
            kind: 3,
        },
        P::IF_SETSCROLLPOS => {
            let value = i32::from(r.read_u16_be()?);
            let hash = r.read_i32_be()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETSCROLLPOS, hash).with(value, 0, 0))
        }
        P::IF_SETCOLOR => {
            let hash = r.read_i32_le()?;
            let color = i32::from(r.read_u16_be_a()?);
            WorldPacket::Pulse(PulseUpdate::new(IF_SETCOLOR, hash).with(color, 0, 0))
        }
        P::IF_SETCLICKMASK => {
            let enabled = r.read_u8_s()? == 1;
            let mask = r.read_i32_le()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETCLICKMASK, mask).with(
                i32::from(enabled),
                0,
                0,
            ))
        }
        P::IF_SETPOSITION => {
            let y = i32::from(r.read_i16_be_a()?);
            let hash = r.read_i32_inverse_middle()?;
            let x = i32::from(r.read_i16_le_a()?);
            WorldPacket::Pulse(PulseUpdate::new(IF_SETPOSITION, hash).with(x, y, 0))
        }
        P::IF_SETHIDE => {
            let hidden = i32::from(r.read_u8_a()?);
            let hash = r.read_i32_be()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETHIDE, hash).with(hidden, 0, 0))
        }
        P::IF_SETANIM => {
            let anim = r.read_i32_inverse_middle()?;
            let hash = r.read_i32_be()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETANIM, hash).with(anim, 0, 0))
        }
        P::IF_SETRECOL => {
            let hash = r.read_i32_inverse_middle()?;
            let src = i32::from(r.read_u16_le()?);
            let dst = i32::from(r.read_u16_le()?);
            let index = i32::from(r.read_u8()?);
            WorldPacket::Pulse(PulseUpdate::keyed(IF_SETRECOL, index, hash).with(src, dst, 0))
        }
        P::IF_SETRETEX => {
            let dst = i32::from(r.read_u16_le()?);
            let src = i32::from(r.read_u16_be()?);
            let index = i32::from(r.read_u8_s()?);
            let hash = r.read_i32_le()?;
            WorldPacket::Pulse(PulseUpdate::keyed(IF_SETRETEX, index, hash).with(src, dst, 0))
        }
        P::IF_SETTEXTANTIMACRO => {
            let enabled = r.read_u8_s()? == 1;
            let hash = r.read_i32_inverse_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETTEXTANTIMACRO, hash).with(
                i32::from(enabled),
                0,
                0,
            ))
        }
        P::IF_SETTEXTFONT => {
            let font = r.read_i32_be()?;
            let hash = r.read_i32_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETFONT, hash).with(font, 0, 0))
        }
        P::IF_SETGRAPHIC => {
            let graphic = r.read_i32_inverse_middle()?;
            let hash = r.read_i32_inverse_middle()?;
            WorldPacket::Pulse(PulseUpdate::new(IF_SETGRAPHIC, hash).with(graphic, 0, 0))
        }
        P::OUTDATED_INTERFACE_PULSE_EVENT => {
            let key = i32::from(r.read_u16_be_a()?);
            let hash = r.read_i32_inverse_middle()?;
            let component = i32::from(r.read_u16_le()?);
            WorldPacket::Pulse(PulseUpdate::new(T13, hash).with(key + (component << 16), 0, 0))
        }
        P::OUTDATED_SET_THEORA_STRING_SOMETHING => {
            let value = i32::from(r.read_u16_be_a()?);
            let hash = r.read_i32_le()?;
            WorldPacket::Pulse(PulseUpdate::new(T17, hash).with(value, 0, 0))
        }
        P::MINIMAP_FLAG => {
            let x = i32::from(r.read_u8_s()?);
            let y = i32::from(r.read_u8_s()?);
            let (x, y) = if x == 255 { (-1, -1) } else { (x, y) };
            WorldPacket::Pulse(
                PulseUpdate::new(MINIMAP_FLAG, 0)
                    .with(x, y, 0)
                    .without_var_verify(),
            )
        }
        P::IF_CLOSESUB => WorldPacket::CloseSub {
            parent: r.read_i32_be()?,
        },
        P::IF_MOVESUB => {
            let from = r.read_i32_inverse_middle()?;
            let to = r.read_i32_be()?;
            WorldPacket::MoveSub { from, to }
        }
        P::IF_SETTARGETPARAM => {
            let to = none_if_ffff(r.read_u16_le_a()?);
            let hash = r.read_i32_inverse_middle()?;
            let from = none_if_ffff(r.read_u16_be()?);
            let settings = r.read_i32_le()?;
            WorldPacket::SetTargetParam(TargetParam {
                from_slot: from,
                to_slot: to,
                hash,
                settings,
            })
        }
        P::IF_RESETTARGETPARAM => {
            let hash = r.read_i32_middle()?;
            let interface_id = r.read_u16_le_a()?;
            let to_slot = r.read_u16_be()?;
            let from = none_if_ffff(r.read_u16_be_a()?);
            WorldPacket::ResetTargetParam(ResetTargetParam {
                hash,
                interface_id,
                from_slot: from,
                to_slot,
            })
        }
        P::IF_OPENTOP => {
            let key3 = r.read_i32_middle()?;
            let key4 = r.read_i32_middle()?;
            let interface_id = r.read_u16_be_a()?;
            let kind = r.read_u8_s()?;
            let key2 = r.read_i32_be()?;
            let key1 = r.read_i32_be()?;
            WorldPacket::OpenTop(OpenTop {
                interface_id,
                kind,
                xtea: [key1, key2, key3, key4],
            })
        }
        P::IF_OPENSUB => {
            let key = r.read_i32_be()?;
            let flags = r.read_i32_inverse_middle()?;
            let xtea2 = r.read_i32_inverse_middle()?;
            let interface_id = i32::from(r.read_u16_le_a()?);
            let xtea4 = r.read_i32_le()?;
            let parent = r.read_i32_be()?;
            let overlay = r.read_u8_c()?;
            WorldPacket::OpenSub(OpenSub {
                interface_id,
                parent,
                overlay,
                xtea: [key, xtea2, flags, xtea4],
                target: SubTarget::None,
            })
        }
        P::IF_OPENSUB_ACTIVE_PLAYER => {
            let key = r.read_i32_le()?;
            let overlay = r.read_u8()?;
            let xtea1 = r.read_i32_middle()?;
            let sub = i32::from(r.read_u16_be()?);
            let xtea2 = r.read_i32_be()?;
            let parent = r.read_i32_middle()?;
            let xtea3 = r.read_i32_middle()?;
            let player = r.read_u16_le()?;
            WorldPacket::OpenSub(OpenSub {
                interface_id: sub,
                parent,
                overlay,
                xtea: [xtea1, xtea2, xtea3, key],
                target: SubTarget::Player(player),
            })
        }
        P::IF_OPENSUB_ACTIVE_NPC => {
            let npc = r.read_u16_be_a()?;
            let flags = r.read_i32_le()?;
            let overlay = r.read_u8_c()?;
            let parent = r.read_i32_be()?;
            let xtea4 = r.read_i32_le()?;
            let sub = i32::from(r.read_u16_le_a()?);
            let xtea3 = r.read_i32_be()?;
            let xtea1 = r.read_i32_be()?;
            WorldPacket::OpenSub(OpenSub {
                interface_id: sub,
                parent,
                overlay,
                xtea: [xtea1, flags, xtea3, xtea4],
                target: SubTarget::Npc(npc),
            })
        }
        P::IF_OPENSUB_ACTIVE_OBJECT => {
            let parent = r.read_i32_inverse_middle()?;
            let xtea3 = r.read_i32_inverse_middle()?;
            let overlay = r.read_u8_a()?;
            let xtea2 = r.read_i32_be()?;
            let xtea1 = r.read_i32_middle()?;
            let sub = i32::from(r.read_u16_be_a()?);
            let packed = r.read_u8_s()?;
            let xtea4 = r.read_i32_be()?;
            let position = r.read_u32_inverse_middle()?;
            let id = r.read_i32_be()?;
            WorldPacket::OpenSub(OpenSub {
                interface_id: sub,
                parent,
                overlay,
                xtea: [xtea1, xtea2, xtea3, xtea4],
                target: SubTarget::Object {
                    id,
                    position,
                    shape: packed >> 2,
                    rotation: packed & 3,
                },
            })
        }
        P::IF_OPENSUB_ACTIVE_GROUNDITEM => {
            let parent = r.read_i32_inverse_middle()?;
            let xtea3 = r.read_i32_le()?;
            let sub = i32::from(r.read_u16_le()?);
            let xtea4 = r.read_i32_be()?;
            let xtea1 = r.read_i32_middle()?;
            let overlay = r.read_u8_s()?;
            let position = r.read_u32_middle()?;
            let xtea2 = r.read_i32_middle()?;
            let id = r.read_u16_le_a()?;
            WorldPacket::OpenSub(OpenSub {
                interface_id: sub,
                parent,
                overlay,
                xtea: [xtea1, xtea2, xtea3, xtea4],
                target: SubTarget::GroundItem { id, position },
            })
        }
        P::TRIGGER_ONDIALOGABORT
        | P::CAM_SMOOTHRESET
        | P::CAM_RESET
        | P::RESET_ALL_ANIMATIONS
        | P::RESET_SOUNDS
        | P::FRIENDLIST_LOADED
        | P::PING => WorldPacket::Empty,
        P::RUN_CS2_SCRIPT => {
            let types = r.read_cp1252()?;
            let bytes = types.as_bytes();
            let mut args = vec![ScriptArg::Int(0); bytes.len()];
            for index in (0..bytes.len()).rev() {
                args[index] = if bytes[index] == b's' {
                    ScriptArg::Text(r.read_cp1252()?)
                } else {
                    ScriptArg::Int(r.read_i32_be()?)
                };
            }
            let script_id = r.read_i32_be()?;
            WorldPacket::RunCs2Script(RunScript { script_id, args })
        }
        P::PLAYER_OPTION => {
            let option = r.read_cp1252()?;
            let slot = r.read_u8_a()?;
            let cursor = none_if_ffff(r.read_u16_le_a()?);
            let on_top = r.read_u8_c()? == 1;
            WorldPacket::PlayerOption(PlayerOption {
                option,
                slot,
                cursor,
                on_top,
            })
        }
        other => {
            if zone::is_zone_packet(other) {
                WorldPacket::Zone(zone::decode(other, size, r, counts)?)
            } else {
                WorldPacket::Misc(misc::decode(other, size, r)?)
            }
        }
    };
    Ok(decoded)
}

pub fn requests_var_verify(packet: ServerPacket) -> bool {
    use ServerPacket as P;
    matches!(
        packet,
        P::CAM_FORCEANGLE
            | P::CAM_LOOKAT
            | P::CAM_MOVETO
            | P::CAM_RESET
            | P::CAM_SHAKE
            | P::CAM_SMOOTHRESET
            | P::CLIENT_SETVARC_SMALL
            | P::CLIENT_SETVARC_LARGE
            | P::CLIENT_SETVARCSTR_SMALL
            | P::CLIENT_SETVARCSTR_LARGE
            | P::IF_CLOSESUB
            | P::IF_MOVESUB
            | P::IF_OPENSUB
            | P::IF_OPENSUB_ACTIVE_GROUNDITEM
            | P::IF_OPENSUB_ACTIVE_NPC
            | P::IF_OPENSUB_ACTIVE_OBJECT
            | P::IF_OPENSUB_ACTIVE_PLAYER
            | P::IF_OPENTOP
            | P::IF_RESETTARGETPARAM
            | P::IF_SETANGLE
            | P::IF_SETANIM
            | P::IF_SETCLICKMASK
            | P::IF_SETCOLOR
            | P::IF_SETGRAPHIC
            | P::IF_SETHIDE
            | P::IF_SETITEM
            | P::IF_SETMODEL
            | P::IF_SETNPCHEAD
            | P::IF_SETPLAYERHEAD
            | P::IF_SETPLAYERHEAD_IGNOREWORN
            | P::IF_SETPLAYERHEAD_OTHER
            | P::IF_SETPLAYERMODEL
            | P::IF_SETPLAYERMODEL_OTHER
            | P::IF_SETPOSITION
            | P::IF_SETRECOL
            | P::IF_SETRETEX
            | P::IF_SETSCROLLPOS
            | P::IF_SETTARGETPARAM
            | P::IF_SETTEXT
            | P::IF_SETTEXTANTIMACRO
            | P::IF_SETTEXTFONT
            | P::OUTDATED_INTERFACE_PULSE_EVENT
            | P::OUTDATED_SET_THEORA_STRING_SOMETHING
            | P::RUN_CS2_SCRIPT
            | P::SET_CAMERA_TARGET
            | P::SET_DRAW_ORDER
            | P::SET_SONG_VOLUME
            | P::SET_VORBIS_SOUND
            | P::SHOW_FACE_HERE
            | P::STOP_VORBIS_STREAM
            | P::VORBIS_PRELOAD_SOUND
    )
}
