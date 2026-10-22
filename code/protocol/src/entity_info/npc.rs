use super::{
    read_face_entity, read_hits, read_medium_alternate_order, read_movement_animations,
    AnimationEntry, EquipmentOverride, ForceMove, HitBlock, HitStyle, InfoError, InfoResult,
    SpotAnimation, Tint, VarEntry, MOVE_HALF_WALK, MOVE_RUNNING, MOVE_WALKING,
};
use crate::payload::PayloadReader;
use std::collections::BTreeMap;

pub const NPC_WIRE_DELTAS: [(i32, i32); 8] = [
    (0, 1),
    (1, 1),
    (1, 0),
    (1, -1),
    (0, -1),
    (-1, -1),
    (-1, 0),
    (-1, 1),
];

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct NpcTypeShape {
    pub models: usize,
    pub colors: usize,
    pub textures: usize,
    pub head_models: usize,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct NpcRecord {
    pub type_id: i32,
    pub x: i32,
    pub y: i32,
    pub plane: u8,
    pub last_update: u32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct NpcStep {
    pub dx: i32,
    pub dy: i32,
    pub movement: u8,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NpcMove {
    pub id: u16,
    pub steps: Vec<NpcStep>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NpcAdd {
    pub id: u16,
    pub created: bool,
    pub type_id: i32,
    pub x: i32,
    pub y: i32,
    pub plane: u8,
    pub jump: bool,
    pub rotation: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum NpcCustomisation {
    Clear,
    Set {
        models: Option<Vec<i32>>,
        colors: Option<Vec<u16>>,
        textures: Option<Vec<u16>>,
    },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NpcHeadCustomisation {
    pub models: Option<Vec<i32>>,
    pub colors: Option<Vec<u16>>,
    pub texture_count: Option<usize>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NpcStat {
    pub index: u8,
    pub max: i32,
    pub current: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum NpcBlock {
    MovementAnimations { ids: [i32; 4], speed: u8 },
    Customise(NpcCustomisation),
    ForceMove(ForceMove),
    RenderAnim(i32),
    Varns(Vec<VarEntry>),
    Spot(SpotAnimation),
    FaceEntity(i32),
    Animate(Vec<AnimationEntry>),
    Hits(HitBlock),
    SetType(i32),
    Stats(Vec<NpcStat>),
    HeadCustomisation(Option<NpcHeadCustomisation>),
    CombatLevel(Option<u16>),
    VarnsReset(Vec<VarEntry>),
    FaceTile { x: u16, y: u16 },
    Tint(Tint),
    Name(String),
    Overhead(String),
    Equipment(Vec<EquipmentOverride>),
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct NpcFrame {
    pub moved: Vec<NpcMove>,
    pub added: Vec<NpcAdd>,
    pub removed: Vec<u16>,
    pub updates: Vec<(u16, Vec<NpcBlock>)>,
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct NpcView {
    pub npcs: BTreeMap<u16, NpcRecord>,
    pub update_indices: Vec<u16>,
    pub cycle: u32,
}

fn wire_step(code: u32, movement: u8) -> NpcStep {
    let (dx, dy) = NPC_WIRE_DELTAS[code as usize & 7];
    NpcStep { dx, dy, movement }
}

fn signed_offset(raw: u32, large_view: bool) -> i32 {
    let raw = raw as i32;
    if large_view {
        if raw > 127 {
            raw - 256
        } else {
            raw
        }
    } else if raw > 15 {
        raw - 32
    } else {
        raw
    }
}

impl NpcView {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn len(&self) -> usize {
        self.npcs.len()
    }

    pub fn is_empty(&self) -> bool {
        self.npcs.is_empty()
    }

    pub fn decode(
        &mut self,
        payload: &[u8],
        large_view: bool,
        my_base: (i32, i32),
        types: &dyn Fn(i32) -> Option<NpcTypeShape>,
    ) -> InfoResult<NpcFrame> {
        let mut next = self.clone();
        let frame = next.decode_in_place(payload, large_view, my_base, types)?;
        *self = next;
        Ok(frame)
    }

    fn decode_in_place(
        &mut self,
        payload: &[u8],
        large_view: bool,
        my_base: (i32, i32),
        types: &dyn Fn(i32) -> Option<NpcTypeShape>,
    ) -> InfoResult<NpcFrame> {
        self.cycle = self.cycle.wrapping_add(1);
        let cycle = self.cycle;
        let mut frame = NpcFrame::default();
        let mut reader = PayloadReader::new(payload);
        let mut local: Vec<u16> = Vec::new();
        let mut mask_list: Vec<u16> = Vec::new();

        reader.start_bit_access()?;
        let size = reader.read_bits(8)? as usize;
        let previous = std::mem::take(&mut self.update_indices);
        if size > previous.len() {
            return Err(InfoError::TooManyEntries {
                size,
                limit: previous.len(),
            });
        }
        local.extend_from_slice(&previous[size..]);

        for &id in &previous[..size] {
            if !self.npcs.contains_key(&id) {
                continue;
            }
            if reader.read_bits(1)? == 0 {
                self.touch(id, cycle);
                continue;
            }
            let mut steps = Vec::new();
            match reader.read_bits(2)? {
                0 => {
                    self.touch(id, cycle);
                    mask_list.push(id);
                }
                1 => {
                    self.touch(id, cycle);
                    steps.push(wire_step(reader.read_bits(3)?, MOVE_WALKING));
                    if reader.read_bits(1)? == 1 {
                        mask_list.push(id);
                    }
                }
                2 => {
                    self.touch(id, cycle);
                    if reader.read_bits(1)? == 1 {
                        steps.push(wire_step(reader.read_bits(3)?, MOVE_RUNNING));
                        steps.push(wire_step(reader.read_bits(3)?, MOVE_RUNNING));
                    } else {
                        steps.push(wire_step(reader.read_bits(3)?, MOVE_HALF_WALK));
                    }
                    if reader.read_bits(1)? == 1 {
                        mask_list.push(id);
                    }
                }
                _ => {
                    local.push(id);
                }
            }
            if !steps.is_empty() {
                if let Some(record) = self.npcs.get_mut(&id) {
                    for step in &steps {
                        record.x += step.dx;
                        record.y += step.dy;
                    }
                }
                frame.moved.push(NpcMove { id, steps });
            }
        }

        while reader.remaining_bits() >= 15 {
            let id = reader.read_bits(15)? as u16;
            if id == 32767 {
                break;
            }
            let created = !self.npcs.contains_key(&id);
            if created {
                self.npcs.insert(
                    id,
                    NpcRecord {
                        type_id: -1,
                        x: 0,
                        y: 0,
                        plane: 0,
                        last_update: cycle,
                    },
                );
            }
            self.update_indices.push(id);
            if reader.read_bits(1)? == 1 {
                mask_list.push(id);
            }
            let off_y = signed_offset(reader.read_bits(if large_view { 8 } else { 5 })?, large_view);
            let rotation = (((reader.read_bits(3)? + 4) << 11) & 0x3fff) as u16;
            let type_id = reader.read_bits(15)? as i32;
            let off_x = signed_offset(reader.read_bits(if large_view { 8 } else { 5 })?, large_view);
            let jump = reader.read_bits(1)? == 1;
            let plane = reader.read_bits(2)? as u8;
            let x = my_base.0 + off_x;
            let y = my_base.1 + off_y;
            if let Some(record) = self.npcs.get_mut(&id) {
                record.last_update = cycle;
                record.type_id = type_id;
                record.x = x;
                record.y = y;
                record.plane = plane;
            }
            frame.added.push(NpcAdd {
                id,
                created,
                type_id,
                x,
                y,
                plane,
                jump,
                rotation,
            });
        }
        reader.finish_bit_access()?;

        for &id in &mask_list {
            if !self.npcs.contains_key(&id) {
                continue;
            }
            let blocks = self.decode_blocks(&mut reader, id, types)?;
            frame.updates.push((id, blocks));
        }

        for &id in &local {
            let stale = self
                .npcs
                .get(&id)
                .is_some_and(|record| record.last_update != cycle);
            if stale {
                self.npcs.remove(&id);
                frame.removed.push(id);
            }
        }

        if reader.position() != payload.len() {
            return Err(InfoError::PositionMismatch {
                position: reader.position(),
                size: payload.len(),
            });
        }
        for id in &self.update_indices {
            if !self.npcs.contains_key(id) {
                return Err(InfoError::Inconsistent("update index not in the npc map"));
            }
        }
        if self.npcs.len() != self.update_indices.len() {
            return Err(InfoError::Inconsistent("npc count differs from update count"));
        }
        if self.npcs.values().any(|record| record.last_update != cycle) {
            return Err(InfoError::Inconsistent("npc not updated this cycle"));
        }
        Ok(frame)
    }

    fn touch(&mut self, id: u16, cycle: u32) {
        self.update_indices.push(id);
        if let Some(record) = self.npcs.get_mut(&id) {
            record.last_update = cycle;
        }
    }

    fn decode_blocks(
        &mut self,
        reader: &mut PayloadReader<'_>,
        id: u16,
        types: &dyn Fn(i32) -> Option<NpcTypeShape>,
    ) -> InfoResult<Vec<NpcBlock>> {
        let mut flags = u32::from(reader.read_u8()?);
        if flags & 0x40 != 0 {
            flags = flags.wrapping_add(u32::from(reader.read_u8()?) << 8);
        }
        if flags & 0x2000 != 0 {
            flags = flags.wrapping_add(u32::from(reader.read_u8()?) << 16);
        }
        if flags & 0x100000 != 0 {
            flags = flags.wrapping_add(u32::from(reader.read_u8()?) << 24);
        }
        let shape_of = |view: &NpcView| {
            view.npcs
                .get(&id)
                .and_then(|record| types(record.type_id))
                .unwrap_or_default()
        };
        let mut blocks = Vec::new();

        if flags & 0x10 != 0 {
            let (ids, speed) = read_movement_animations(reader)?;
            blocks.push(NpcBlock::MovementAnimations { ids, speed });
        }
        if flags & 0x100 != 0 {
            let shape = shape_of(self);
            let action = reader.read_u8_s()?;
            if action & 1 == 1 {
                blocks.push(NpcBlock::Customise(NpcCustomisation::Clear));
            } else {
                let models = if action & 2 != 0 {
                    Some(read_big_smarts(reader, shape.models)?)
                } else {
                    None
                };
                let colors = if action & 4 != 0 {
                    Some(read_u16s(reader, shape.colors)?)
                } else {
                    None
                };
                let textures = if action & 8 != 0 {
                    Some(read_u16s(reader, shape.textures)?)
                } else {
                    None
                };
                blocks.push(NpcBlock::Customise(NpcCustomisation::Set {
                    models,
                    colors,
                    textures,
                }));
            }
        }
        if flags & 0x400 != 0 {
            let first_x = reader.read_i8_s()?;
            let first_y = reader.read_i8_a()?;
            let second_x = reader.read_i8_c()?;
            let second_y = reader.read_i8_a()?;
            let first_delay = reader.read_u16_le()?;
            let second_delay = reader.read_u16_le()?;
            let direction = reader.read_u16_le_a()?;
            blocks.push(NpcBlock::ForceMove(ForceMove {
                first_x,
                first_y,
                second_x,
                second_y,
                first_delay,
                second_delay,
                direction,
            }));
        }
        if flags & 0x1000 != 0 {
            blocks.push(NpcBlock::RenderAnim(read_face_entity(reader.read_u16_be()?)));
        }
        if flags & 0x200000 != 0 {
            let count = reader.read_u8()?;
            let mut entries = Vec::new();
            for _ in 0..count {
                let key = reader.read_u16_le_a()?;
                let value = reader.read_u32_middle()? as i32;
                entries.push(VarEntry { key, value });
            }
            blocks.push(NpcBlock::Varns(entries));
        }
        if flags & 0x1000000 != 0 {
            let raw = reader.read_u16_be_a()?;
            let packed = reader.read_u32_inverse_middle()? as i32;
            let spot_flags = reader.read_u8_a()?;
            blocks.push(NpcBlock::Spot(SpotAnimation::new(3, raw, packed, spot_flags)));
        }
        if flags & 0x80 != 0 {
            blocks.push(NpcBlock::FaceEntity(read_face_entity(reader.read_u16_le()?)));
        }
        if flags & 0x8000 != 0 {
            let count = reader.read_u8_c()?;
            let mut entries = Vec::new();
            for _ in 0..count {
                let sequence = reader.read_big_smart()?;
                let speed = reader.read_u8()?;
                let slot_mask = reader.read_u16_le()?;
                entries.push(AnimationEntry {
                    id: sequence,
                    speed,
                    slot_mask,
                });
            }
            blocks.push(NpcBlock::Animate(entries));
        }
        if flags & 0x800 != 0 {
            let raw = reader.read_u16_le()?;
            let packed = reader.read_i32_be()?;
            let spot_flags = reader.read_u8_a()?;
            blocks.push(NpcBlock::Spot(SpotAnimation::new(1, raw, packed, spot_flags)));
        }
        if flags & 0x1 != 0 {
            blocks.push(NpcBlock::Hits(read_hits(reader, HitStyle::Npc)?));
        }
        if flags & 0x8 != 0 {
            let type_id = reader.read_big_smart()?;
            if let Some(record) = self.npcs.get_mut(&id) {
                record.type_id = type_id;
            }
            blocks.push(NpcBlock::SetType(type_id));
        }
        if flags & 0x2000000 != 0 {
            let raw = reader.read_u16_le()?;
            let packed = reader.read_u32_le()? as i32;
            let spot_flags = reader.read_u8()?;
            blocks.push(NpcBlock::Spot(SpotAnimation::new(2, raw, packed, spot_flags)));
        }
        if flags & 0x80000 != 0 {
            let count = reader.read_u8()?;
            let mut entries = Vec::new();
            for _ in 0..count {
                let index = reader.read_u8_a()?;
                let max = reader.read_i32_be()?;
                let current = read_medium_alternate_order(reader)?;
                entries.push(NpcStat {
                    index,
                    max,
                    current,
                });
            }
            blocks.push(NpcBlock::Stats(entries));
        }
        if flags & 0x40000 != 0 {
            let shape = shape_of(self);
            let action = reader.read_u8_a()?;
            if action & 1 != 1 {
                let models = if action & 2 != 0 {
                    Some(read_big_smarts(reader, shape.head_models)?)
                } else {
                    None
                };
                let colors = if action & 4 != 0 {
                    Some(read_u16s(reader, shape.colors)?)
                } else {
                    None
                };
                let texture_count = if action & 8 != 0 {
                    Some(shape.textures)
                } else {
                    None
                };
                blocks.push(NpcBlock::HeadCustomisation(Some(NpcHeadCustomisation {
                    models,
                    colors,
                    texture_count,
                })));
            } else {
                blocks.push(NpcBlock::HeadCustomisation(None));
            }
        }
        if flags & 0x10000 != 0 {
            let level = reader.read_u16_be_a()?;
            blocks.push(NpcBlock::CombatLevel(if level == 0xffff {
                None
            } else {
                Some(level)
            }));
        }
        if flags & 0x400000 != 0 {
            let count = reader.read_u8()?;
            let mut entries = Vec::new();
            for _ in 0..count {
                let key = reader.read_u16_be()?;
                let value = reader.read_u32_middle()? as i32;
                entries.push(VarEntry { key, value });
            }
            blocks.push(NpcBlock::VarnsReset(entries));
        }
        if flags & 0x4 != 0 {
            let x = reader.read_u16_le_a()?;
            let y = reader.read_u16_le()?;
            blocks.push(NpcBlock::FaceTile { x, y });
        }
        if flags & 0x20000 != 0 {
            let hue = reader.read_i8_s()?;
            let saturation = reader.read_i8()?;
            let lightness = reader.read_i8()?;
            let opacity = reader.read_u8_s()?;
            let start = reader.read_u16_le()?;
            let end = reader.read_u16_be()?;
            blocks.push(NpcBlock::Tint(Tint {
                hue,
                saturation,
                lightness,
                opacity,
                start,
                end,
            }));
        }
        if flags & 0x800000 != 0 {
            blocks.push(NpcBlock::Name(reader.read_cp1252()?));
        }
        if flags & 0x2 != 0 {
            blocks.push(NpcBlock::Overhead(reader.read_cp1252()?));
        }
        if flags & 0x4000 != 0 {
            let count = reader.read_u8_s()?;
            let mut entries = Vec::new();
            for _ in 0..count {
                let first = reader.read_u16_le_a()?;
                let target = super::read_equipment_overrides_target(first, reader, |r| {
                    r.read_u16_be()
                })?;
                let mask = reader.read_u16_le_a()?;
                entries.push(EquipmentOverride { target, mask });
            }
            blocks.push(NpcBlock::Equipment(entries));
        }
        if flags & 0x20 != 0 {
            let raw = reader.read_u16_be()?;
            let packed = reader.read_u32_inverse_middle()? as i32;
            let spot_flags = reader.read_u8_c()?;
            blocks.push(NpcBlock::Spot(SpotAnimation::new(0, raw, packed, spot_flags)));
        }
        Ok(blocks)
    }
}

fn read_big_smarts(reader: &mut PayloadReader<'_>, count: usize) -> InfoResult<Vec<i32>> {
    let mut values = Vec::new();
    for _ in 0..count {
        values.push(reader.read_big_smart()?);
    }
    Ok(values)
}

fn read_u16s(reader: &mut PayloadReader<'_>, count: usize) -> InfoResult<Vec<u16>> {
    let mut values = Vec::new();
    for _ in 0..count {
        values.push(reader.read_u16_be()?);
    }
    Ok(values)
}
