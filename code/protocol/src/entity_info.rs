use crate::payload::{PayloadError, PayloadReader};
use std::fmt;

mod appearance;
mod npc;
mod player;

pub use appearance::{
    decode_appearance, decode_item_effects, Appearance, ItemEffects, ItemShape, WornSlot,
};
pub use npc::{
    NpcAdd, NpcBlock, NpcCustomisation, NpcFrame, NpcHeadCustomisation, NpcMove, NpcRecord,
    NpcStat, NpcStep, NpcTypeShape, NpcView, NPC_WIRE_DELTAS,
};
pub use player::{
    OwnPlayer, PlayerBlock, PlayerEvent, PlayerFrame, PlayerRecord, PlayerSaved, PlayerView,
    MAX_PLAYERS, MOVEMENT_STATIONARY, MOVEMENT_TELEPORT, MOVEMENT_WALKING, ONE_TILE_DELTAS,
    TWO_TILE_DELTAS,
};

pub const MOVE_HALF_WALK: u8 = 0;
pub const MOVE_WALKING: u8 = 1;
pub const MOVE_RUNNING: u8 = 2;

pub type InfoResult<T> = Result<T, InfoError>;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum InfoError {
    Payload(PayloadError),
    TooManyEntries { size: usize, limit: usize },
    SkipCountPending(u32),
    MissingEntity(u16),
    MissingSavedPosition(u16),
    PlayerAlreadyPresent(u16),
    OwnPlayerHidden(u16),
    PositionMismatch { position: usize, size: usize },
    Inconsistent(&'static str),
    UnknownItem(i32),
    ItemNotWorn(usize),
    ItemEffectTarget(u8),
    ItemEffectSlot { slot: u8, len: usize },
}

impl fmt::Display for InfoError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Payload(error) => write!(f, "{error}"),
            other => write!(f, "{other:?}"),
        }
    }
}

impl std::error::Error for InfoError {}

impl From<PayloadError> for InfoError {
    fn from(error: PayloadError) -> Self {
        Self::Payload(error)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SpotAnimation {
    pub index: u8,
    pub id: i32,
    pub packed: i32,
    pub rotation: u8,
    pub slot: i8,
    pub adjust_replay: bool,
}

impl SpotAnimation {
    pub(crate) fn new(index: u8, raw_id: u16, packed: i32, flags: u8) -> Self {
        let slot = (flags >> 3) & 0xf;
        Self {
            index,
            id: if raw_id == 0xffff { -1 } else { i32::from(raw_id) },
            packed,
            rotation: flags & 7,
            slot: if slot == 15 { -1 } else { slot as i8 },
            adjust_replay: (flags >> 7) & 1 == 1,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Hitsplat {
    pub id: i32,
    pub text: i32,
    pub custom_text: Option<String>,
    pub soak_id: i32,
    pub soak_text: i32,
    pub soak_custom_text: Option<String>,
    pub delay: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Hitbar {
    Hide {
        id: u16,
    },
    Show {
        id: u16,
        lifetime: u16,
        delay: u16,
        from: u8,
        to: u8,
    },
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct HitBlock {
    pub hits: Vec<Hitsplat>,
    pub bars: Vec<Hitbar>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum HitStyle {
    Npc,
    Player,
}

pub(crate) fn read_hits(
    reader: &mut PayloadReader<'_>,
    style: HitStyle,
) -> InfoResult<HitBlock> {
    let hit_count = match style {
        HitStyle::Npc => reader.read_u8_a()?,
        HitStyle::Player => reader.read_u8_s()?,
    };
    let mut block = HitBlock::default();
    for _ in 0..hit_count {
        let mut hit = Hitsplat {
            id: i32::from(reader.read_u_smart()?),
            text: -1,
            custom_text: None,
            soak_id: -1,
            soak_text: -1,
            soak_custom_text: None,
            delay: 0,
        };
        match hit.id {
            32767 => {
                hit.id = i32::from(reader.read_u_smart()?);
                hit.text = i32::from(reader.read_u_smart()?);
                hit.soak_id = i32::from(reader.read_u_smart()?);
                hit.soak_text = i32::from(reader.read_u_smart()?);
            }
            32766 => {
                hit.id = -1;
                hit.text = i32::from(match style {
                    HitStyle::Npc => reader.read_u8()?,
                    HitStyle::Player => reader.read_u8_c()?,
                });
            }
            32764 => {
                hit.id = i32::from(reader.read_u_smart()?);
                hit.custom_text = Some(reader.read_cp1252()?);
                hit.soak_id = i32::from(reader.read_u_smart()?);
                hit.soak_custom_text = Some(reader.read_cp1252()?);
            }
            32765 => {
                hit.id = i32::from(reader.read_u_smart()?);
                hit.custom_text = Some(reader.read_cp1252()?);
            }
            _ => hit.text = i32::from(reader.read_u_smart()?),
        }
        hit.delay = reader.read_u_smart()?;
        block.hits.push(hit);
    }
    let bar_count = match style {
        HitStyle::Npc => reader.read_u8_a()?,
        HitStyle::Player => reader.read_u8()?,
    };
    for _ in 0..bar_count {
        let id = reader.read_u_smart()?;
        let lifetime = reader.read_u_smart()?;
        if lifetime == 32767 {
            block.bars.push(Hitbar::Hide { id });
        } else {
            let delay = reader.read_u_smart()?;
            let from = reader.read_u8_s()?;
            let to = if lifetime > 0 {
                reader.read_u8_a()?
            } else {
                from
            };
            block.bars.push(Hitbar::Show {
                id,
                lifetime,
                delay,
                from,
                to,
            });
        }
    }
    Ok(block)
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AnimationEntry {
    pub id: i32,
    pub speed: u8,
    pub slot_mask: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ForceMove {
    pub first_x: i8,
    pub first_y: i8,
    pub second_x: i8,
    pub second_y: i8,
    pub first_delay: u16,
    pub second_delay: u16,
    pub direction: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Tint {
    pub hue: i8,
    pub saturation: i8,
    pub lightness: i8,
    pub opacity: u8,
    pub start: u16,
    pub end: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EquipmentOverride {
    pub target: i32,
    pub mask: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VarEntry {
    pub key: u16,
    pub value: i32,
}

pub(crate) fn read_movement_animations(
    reader: &mut PayloadReader<'_>,
) -> InfoResult<([i32; 4], u8)> {
    let mut ids = [0; 4];
    for id in &mut ids {
        *id = reader.read_big_smart()?;
    }
    Ok((ids, reader.read_u8()?))
}

pub(crate) fn read_face_entity(raw: u16) -> i32 {
    if raw == 0xffff {
        -1
    } else {
        i32::from(raw)
    }
}

pub(crate) fn read_medium_alternate_order(reader: &mut PayloadReader<'_>) -> InfoResult<i32> {
    let high = i32::from(reader.read_u8()?);
    let low = i32::from(reader.read_u8()?);
    let middle = i32::from(reader.read_u8()?);
    Ok((high << 16) | low | (middle << 8))
}

pub(crate) fn read_equipment_overrides_target(
    first: u16,
    reader: &mut PayloadReader<'_>,
    extended: impl FnOnce(&mut PayloadReader<'_>) -> Result<u16, PayloadError>,
) -> InfoResult<i32> {
    if first & 0xc000 == 49152 {
        let extension = extended(reader)?;
        Ok((i32::from(first) << 16) | i32::from(extension))
    } else {
        Ok(i32::from(first))
    }
}
