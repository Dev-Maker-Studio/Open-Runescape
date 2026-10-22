use super::{
    read_face_entity, read_hits, read_movement_animations, AnimationEntry, EquipmentOverride,
    ForceMove, HitBlock, HitStyle, InfoError, InfoResult, SpotAnimation, Tint, VarEntry,
};
use crate::payload::PayloadReader;
use std::collections::BTreeMap;

pub const MAX_PLAYERS: usize = 2048;
pub const MOVEMENT_STATIONARY: i8 = -1;
pub const MOVEMENT_WALKING: i8 = 1;
pub const MOVEMENT_TELEPORT: i8 = 127;

pub const ONE_TILE_DELTAS: [(i32, i32); 8] = [
    (-1, -1),
    (0, -1),
    (1, -1),
    (-1, 0),
    (1, 0),
    (-1, 1),
    (0, 1),
    (1, 1),
];

pub const TWO_TILE_DELTAS: [(i32, i32); 16] = [
    (-2, -2),
    (-1, -2),
    (0, -2),
    (1, -2),
    (2, -2),
    (-2, -1),
    (2, -1),
    (-2, 0),
    (2, 0),
    (-2, 1),
    (2, 1),
    (-2, 2),
    (-1, 2),
    (0, 2),
    (1, 2),
    (2, 2),
];

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct PlayerRecord {
    pub x: i32,
    pub y: i32,
    pub plane: u8,
    pub face_entity: i32,
    pub pmapdot: bool,
    pub reduce_attack_priority: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct PlayerSaved {
    pub packed: i32,
    pub face_entity: i32,
    pub pmapdot: bool,
    pub reduce_attack_priority: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PlayerEvent {
    Moved {
        index: u16,
        x: i32,
        y: i32,
        plane: u8,
        movement: i8,
    },
    Left {
        index: u16,
    },
    Entered {
        index: u16,
        x: i32,
        y: i32,
        plane: u8,
        movement: i8,
        face_entity: i32,
        pmapdot: bool,
        reduce_attack_priority: bool,
    },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PlayerBlock {
    Tint(Tint),
    ConfigSet(Vec<VarEntry>),
    Spot(SpotAnimation),
    FaceDirection(u16),
    ConfigReset(Vec<VarEntry>),
    ReduceAttackPriority(bool),
    Equipment(Vec<EquipmentOverride>),
    Chat(String),
    PublicChat { message: String, flags: u8 },
    Pmapdot(bool),
    Hits(HitBlock),
    Appearance(Vec<u8>),
    MovementAnimations { ids: [i32; 4], speed: u8 },
    FaceEntity(i32),
    DefaultMoveSpeed(i8),
    Animate(Vec<AnimationEntry>),
    MovementType(i8),
    ForceMove(ForceMove),
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct PlayerFrame {
    pub events: Vec<PlayerEvent>,
    pub updates: Vec<(u16, Vec<PlayerBlock>)>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct OwnPlayer {
    pub x: i32,
    pub y: i32,
    pub plane: u8,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PlayerView {
    pub players: Vec<Option<PlayerRecord>>,
    pub saved: Vec<Option<PlayerSaved>>,
    pub state: Vec<u8>,
    pub movement_types: Vec<i8>,
    pub appearance_buffers: Vec<Option<Vec<u8>>>,
    pub active: Vec<u16>,
    pub inactive: Vec<u16>,
    pub my_index: u16,
    pub my_plane: u8,
}

struct Pass<'a> {
    reader: PayloadReader<'a>,
    mask_list: Vec<u16>,
    events: Vec<PlayerEvent>,
    pending: BTreeMap<u16, (i32, i32)>,
}

impl PlayerView {
    pub fn new(my_index: u16) -> Self {
        Self {
            players: vec![None; MAX_PLAYERS],
            saved: vec![None; MAX_PLAYERS],
            state: vec![0; MAX_PLAYERS],
            movement_types: vec![MOVEMENT_WALKING; MAX_PLAYERS],
            appearance_buffers: vec![None; MAX_PLAYERS],
            active: Vec::new(),
            inactive: Vec::new(),
            my_index,
            my_plane: 0,
        }
    }

    pub fn reset(&mut self) {
        self.active.clear();
        for index in 0..MAX_PLAYERS {
            self.appearance_buffers[index] = None;
            self.movement_types[index] = MOVEMENT_WALKING;
            self.saved[index] = None;
        }
    }

    pub fn player(&self, index: u16) -> Option<&PlayerRecord> {
        self.players.get(usize::from(index)).and_then(Option::as_ref)
    }

    pub fn decode_own_player(&mut self, payload: &[u8]) -> InfoResult<OwnPlayer> {
        let mut next = self.clone();
        let own = next.decode_own_in_place(payload)?;
        *self = next;
        Ok(own)
    }

    fn decode_own_in_place(&mut self, payload: &[u8]) -> InfoResult<OwnPlayer> {
        let mut reader = PayloadReader::new(payload);
        reader.start_bit_access()?;
        let my = usize::from(self.my_index);
        if my >= MAX_PLAYERS {
            return Err(InfoError::MissingEntity(self.my_index));
        }
        let coordinates = reader.read_bits(30)?;
        let plane = (coordinates >> 28) as u8;
        let x = ((coordinates >> 14) & 0x3fff) as i32;
        let y = (coordinates & 0x3fff) as i32;
        self.players[my] = Some(PlayerRecord {
            x,
            y,
            plane,
            face_entity: -1,
            pmapdot: false,
            reduce_attack_priority: false,
        });
        self.my_plane = plane;
        self.active.clear();
        self.active.push(self.my_index);
        self.state[my] = 0;
        self.inactive.clear();
        for index in 1..MAX_PLAYERS {
            if index == my {
                continue;
            }
            let other = reader.read_bits(18)? as i32;
            let other_plane = other >> 16;
            let other_x = (other >> 8) & 0xff;
            let other_y = other & 0xff;
            self.saved[index] = Some(PlayerSaved {
                packed: (other_x << 14)
                    .wrapping_add(other_y)
                    .wrapping_add(other_plane << 28),
                face_entity: -1,
                pmapdot: false,
                reduce_attack_priority: false,
            });
            self.inactive.push(index as u16);
            self.state[index] = 0;
        }
        reader.finish_bit_access()?;
        Ok(OwnPlayer { x, y, plane })
    }

    pub fn decode(&mut self, payload: &[u8]) -> InfoResult<PlayerFrame> {
        let mut next = self.clone();
        let frame = next.decode_in_place(payload)?;
        *self = next;
        Ok(frame)
    }

    fn decode_in_place(&mut self, payload: &[u8]) -> InfoResult<PlayerFrame> {
        let mut pass = Pass {
            reader: PayloadReader::new(payload),
            mask_list: Vec::new(),
            events: Vec::new(),
            pending: BTreeMap::new(),
        };
        self.decode_passes(&mut pass)?;
        let mut frame = PlayerFrame::default();
        let mask_list = std::mem::take(&mut pass.mask_list);
        for ptr in mask_list {
            let blocks = self.decode_blocks(&mut pass, ptr)?;
            frame.updates.push((ptr, blocks));
        }
        if pass.reader.position() != payload.len() {
            return Err(InfoError::PositionMismatch {
                position: pass.reader.position(),
                size: payload.len(),
            });
        }
        frame.events = pass.events;
        Ok(frame)
    }

    fn decode_passes(&mut self, pass: &mut Pass<'_>) -> InfoResult<()> {
        let active = self.active.clone();
        let inactive = self.inactive.clone();
        self.run_pass(pass, &active, true, false)?;
        self.run_pass(pass, &active, true, true)?;
        self.run_pass(pass, &inactive, false, true)?;
        self.run_pass(pass, &inactive, false, false)?;
        self.active.clear();
        self.inactive.clear();
        for index in 1..MAX_PLAYERS {
            self.state[index] >>= 1;
            if self.players[index].is_some() {
                self.active.push(index as u16);
            } else {
                self.inactive.push(index as u16);
            }
        }
        Ok(())
    }

    fn run_pass(
        &mut self,
        pass: &mut Pass<'_>,
        list: &[u16],
        is_active: bool,
        state_bit: bool,
    ) -> InfoResult<()> {
        let mut skip = 0u32;
        pass.reader.start_bit_access()?;
        for &ptr in list {
            let slot = usize::from(ptr);
            if (self.state[slot] & 1 != 0) != state_bit {
                continue;
            }
            if skip > 0 {
                skip -= 1;
                self.state[slot] |= 2;
            } else if pass.reader.read_bits(1)? == 0 {
                skip = read_skip_count(&mut pass.reader)?;
                self.state[slot] |= 2;
            } else if is_active {
                self.decode_position_update(pass, ptr)?;
            } else if self.decode_movement_update(pass, ptr)? {
                self.state[slot] |= 2;
            }
        }
        pass.reader.finish_bit_access()?;
        if skip != 0 {
            return Err(InfoError::SkipCountPending(skip));
        }
        Ok(())
    }

    fn apply_move(&mut self, pass: &mut Pass<'_>, index: u16, x: i32, y: i32, has_mask: bool) {
        if has_mask {
            pass.pending.insert(index, (x, y));
            return;
        }
        let slot = usize::from(index);
        let movement = self.movement_types[slot];
        if let Some(record) = self.players[slot].as_mut() {
            record.x = x;
            record.y = y;
            pass.events.push(PlayerEvent::Moved {
                index,
                x,
                y,
                plane: record.plane,
                movement,
            });
        }
    }

    fn decode_position_update(&mut self, pass: &mut Pass<'_>, index: u16) -> InfoResult<()> {
        let slot = usize::from(index);
        let has_mask = pass.reader.read_bits(1)? == 1;
        if has_mask {
            pass.mask_list.push(index);
        }
        let size = pass.reader.read_bits(2)?;
        let player = self.players[slot].ok_or(InfoError::MissingEntity(index))?;
        match size {
            0 => {
                if has_mask {
                    pass.pending.remove(&index);
                } else {
                    if index == self.my_index {
                        return Err(InfoError::OwnPlayerHidden(index));
                    }
                    let packed = (i32::from(player.plane) << 28)
                        .wrapping_add(player.y >> 6)
                        .wrapping_add((player.x >> 6) << 14);
                    self.saved[slot] = Some(PlayerSaved {
                        packed,
                        face_entity: player.face_entity,
                        pmapdot: player.pmapdot,
                        reduce_attack_priority: player.reduce_attack_priority,
                    });
                    self.players[slot] = None;
                    pass.events.push(PlayerEvent::Left { index });
                    if pass.reader.read_bits(1)? != 0 {
                        self.decode_movement_update(pass, index)?;
                    }
                }
            }
            1 => {
                let (dx, dy) = ONE_TILE_DELTAS[pass.reader.read_bits(3)? as usize];
                self.apply_move(pass, index, player.x + dx, player.y + dy, has_mask);
            }
            2 => {
                let (dx, dy) = TWO_TILE_DELTAS[pass.reader.read_bits(4)? as usize];
                self.apply_move(pass, index, player.x + dx, player.y + dy, has_mask);
            }
            _ => {
                if pass.reader.read_bits(1)? == 0 {
                    let data = pass.reader.read_bits(12)? as i32;
                    let plane_offset = data >> 10;
                    let mut dx = (data >> 5) & 0x1f;
                    if dx > 15 {
                        dx -= 32;
                    }
                    let mut dy = data & 0x1f;
                    if dy > 15 {
                        dy -= 32;
                    }
                    let plane = ((plane_offset + i32::from(player.plane)) & 3) as u8;
                    if let Some(record) = self.players[slot].as_mut() {
                        record.plane = plane;
                    }
                    self.apply_move(pass, index, player.x + dx, player.y + dy, has_mask);
                    if index == self.my_index && plane != self.my_plane {
                        self.my_plane = plane;
                    }
                } else {
                    let coordinates = pass.reader.read_bits(30)? as i32;
                    let plane_offset = coordinates >> 28;
                    let x = (coordinates >> 14) & 0x3fff;
                    let y = coordinates & 0x3fff;
                    let new_x = (x + player.x) & 0x3fff;
                    let new_y = (y + player.y) & 0x3fff;
                    let plane = ((plane_offset + i32::from(player.plane)) & 3) as u8;
                    if let Some(record) = self.players[slot].as_mut() {
                        record.plane = plane;
                    }
                    self.apply_move(pass, index, new_x, new_y, has_mask);
                    if index == self.my_index {
                        self.my_plane = plane;
                    }
                }
            }
        }
        Ok(())
    }

    fn decode_movement_update(&mut self, pass: &mut Pass<'_>, ptr: u16) -> InfoResult<bool> {
        let slot = usize::from(ptr);
        match pass.reader.read_bits(2)? {
            0 => {
                if pass.reader.read_bits(1)? != 0 {
                    self.decode_movement_update(pass, ptr)?;
                }
                let x_in = pass.reader.read_bits(6)? as i32;
                let y_in = pass.reader.read_bits(6)? as i32;
                if pass.reader.read_bits(1)? == 1 {
                    pass.mask_list.push(ptr);
                }
                if self.players[slot].is_some() {
                    return Err(InfoError::PlayerAlreadyPresent(ptr));
                }
                let saved = self.saved[slot].ok_or(InfoError::MissingSavedPosition(ptr))?;
                let height = saved.packed >> 28;
                let region_x = (saved.packed >> 14) & 0xff;
                let region_y = saved.packed & 0xff;
                let x = x_in + (region_x << 6);
                let y = y_in + (region_y << 6);
                let plane = height as u8;
                self.players[slot] = Some(PlayerRecord {
                    x,
                    y,
                    plane,
                    face_entity: saved.face_entity,
                    pmapdot: saved.pmapdot,
                    reduce_attack_priority: saved.reduce_attack_priority,
                });
                pass.pending.remove(&ptr);
                self.saved[slot] = None;
                pass.events.push(PlayerEvent::Entered {
                    index: ptr,
                    x,
                    y,
                    plane,
                    movement: self.movement_types[slot],
                    face_entity: saved.face_entity,
                    pmapdot: saved.pmapdot,
                    reduce_attack_priority: saved.reduce_attack_priority,
                });
                Ok(true)
            }
            1 => {
                let height = pass.reader.read_bits(2)? as i32;
                let saved = self.saved[slot]
                    .as_mut()
                    .ok_or(InfoError::MissingSavedPosition(ptr))?;
                let packed = saved.packed;
                saved.packed = (packed & 0x0fff_ffff)
                    .wrapping_add((((packed >> 28).wrapping_add(height)) & 3) << 28);
                Ok(false)
            }
            2 => {
                let bits = pass.reader.read_bits(5)? as i32;
                let height_offset = bits >> 3;
                let direction = (bits & 7) as usize;
                let saved = self.saved[slot]
                    .as_mut()
                    .ok_or(InfoError::MissingSavedPosition(ptr))?;
                let packed = saved.packed;
                let height = ((packed >> 28).wrapping_add(height_offset)) & 3;
                let (dx, dy) = ONE_TILE_DELTAS[direction];
                let x = ((packed >> 14) & 0xff) + dx;
                let y = (packed & 0xff) + dy;
                saved.packed = (x << 14).wrapping_add(y).wrapping_add(height << 28);
                Ok(false)
            }
            _ => {
                let bits = pass.reader.read_bits(18)? as i32;
                let height_offset = bits >> 16;
                let offset_x = (bits >> 8) & 0xff;
                let offset_y = bits & 0xff;
                let saved = self.saved[slot]
                    .as_mut()
                    .ok_or(InfoError::MissingSavedPosition(ptr))?;
                let packed = saved.packed;
                let height = ((packed >> 28).wrapping_add(height_offset)) & 3;
                let x = offset_x.wrapping_add(packed >> 14) & 0xff;
                let y = offset_y.wrapping_add(packed) & 0xff;
                saved.packed = (x << 14).wrapping_add(y).wrapping_add(height << 28);
                Ok(false)
            }
        }
    }

    fn decode_blocks(&mut self, pass: &mut Pass<'_>, ptr: u16) -> InfoResult<Vec<PlayerBlock>> {
        let slot = usize::from(ptr);
        let reader = &mut pass.reader;
        let mut flags = u32::from(reader.read_u8()?);
        if flags & 0x8 != 0 {
            flags = flags.wrapping_add(u32::from(reader.read_u8()?) << 8);
        }
        if flags & 0x100 != 0 {
            flags = flags.wrapping_add(u32::from(reader.read_u8()?) << 16);
        }
        if self.players[slot].is_none() {
            return Err(InfoError::MissingEntity(ptr));
        }
        let mut default_speed = MOVEMENT_STATIONARY;
        let mut blocks = Vec::new();

        if flags & 0x20000 != 0 {
            let hue = reader.read_i8()?;
            let saturation = reader.read_i8_c()?;
            let lightness = reader.read_i8_s()?;
            let opacity = reader.read_u8_s()?;
            let start = reader.read_u16_be_a()?;
            let end = reader.read_u16_be()?;
            blocks.push(PlayerBlock::Tint(Tint {
                hue,
                saturation,
                lightness,
                opacity,
                start,
                end,
            }));
        }
        if flags & 0x400000 != 0 {
            blocks.push(PlayerBlock::ConfigSet(read_config_pairs(reader)?));
        }
        if flags & 0x200 != 0 {
            let raw = reader.read_u16_be()?;
            let packed = reader.read_u32_inverse_middle()? as i32;
            let spot_flags = reader.read_u8_s()?;
            blocks.push(PlayerBlock::Spot(SpotAnimation::new(1, raw, packed, spot_flags)));
        }
        if flags & 0x20 != 0 {
            blocks.push(PlayerBlock::FaceDirection(reader.read_u16_be_a()?));
        }
        if flags & 0x800000 != 0 {
            blocks.push(PlayerBlock::ConfigReset(read_config_pairs(reader)?));
        }
        if flags & 0x10000 != 0 {
            let value = reader.read_u8_s()? == 1;
            if let Some(record) = self.players[slot].as_mut() {
                record.reduce_attack_priority = value;
            }
            blocks.push(PlayerBlock::ReduceAttackPriority(value));
        }
        if flags & 0x100000 != 0 {
            let count = reader.read_u8_s()?;
            let mut entries = Vec::new();
            for _ in 0..count {
                let first = reader.read_u16_be_a()?;
                let target = super::read_equipment_overrides_target(first, reader, |r| {
                    r.read_u16_be_a()
                })?;
                let mask = reader.read_u16_be()?;
                entries.push(EquipmentOverride { target, mask });
            }
            blocks.push(PlayerBlock::Equipment(entries));
        }
        if flags & 0x4000 != 0 {
            blocks.push(PlayerBlock::Chat(reader.read_cp1252()?));
        }
        if flags & 0x80000 != 0 {
            let message = reader.read_cp1252()?;
            let chat_flags = reader.read_u8_a()?;
            blocks.push(PlayerBlock::PublicChat {
                message,
                flags: chat_flags,
            });
        }
        if flags & 0x40000 != 0 {
            let raw = reader.read_u16_le_a()?;
            let packed = reader.read_u32_inverse_middle()? as i32;
            let spot_flags = reader.read_u8_a()?;
            blocks.push(PlayerBlock::Spot(SpotAnimation::new(2, raw, packed, spot_flags)));
        }
        if flags & 0x8000 != 0 {
            let value = reader.read_u8_s()? == 1;
            if let Some(record) = self.players[slot].as_mut() {
                record.pmapdot = value;
            }
            blocks.push(PlayerBlock::Pmapdot(value));
        }
        if flags & 0x40 != 0 {
            blocks.push(PlayerBlock::Hits(read_hits(reader, HitStyle::Player)?));
        }
        if flags & 0x1 != 0 {
            let count = usize::from(reader.read_u8_c()?);
            let bytes = reader.read_bytes(count)?.to_vec();
            self.appearance_buffers[slot] = Some(bytes.clone());
            blocks.push(PlayerBlock::Appearance(bytes));
        }
        if flags & 0x10 != 0 {
            let (ids, speed) = read_movement_animations(reader)?;
            blocks.push(PlayerBlock::MovementAnimations { ids, speed });
        }
        if flags & 0x2 != 0 {
            let face = read_face_entity(reader.read_u16_be_a()?);
            if let Some(record) = self.players[slot].as_mut() {
                record.face_entity = face;
            }
            blocks.push(PlayerBlock::FaceEntity(face));
        }
        if flags & 0x1000 != 0 {
            default_speed = reader.read_i8_c()?;
            blocks.push(PlayerBlock::DefaultMoveSpeed(default_speed));
        }
        if flags & 0x2000 != 0 {
            let count = reader.read_u8_s()?;
            let mut entries = Vec::new();
            for _ in 0..count {
                let id = reader.read_big_smart()?;
                let speed = reader.read_u8_c()?;
                let slot_mask = reader.read_u16_le_a()?;
                entries.push(AnimationEntry {
                    id,
                    speed,
                    slot_mask,
                });
            }
            blocks.push(PlayerBlock::Animate(entries));
        }
        if flags & 0x4 != 0 {
            let value = reader.read_i8_s()?;
            self.movement_types[slot] = value;
            blocks.push(PlayerBlock::MovementType(value));
        }
        if flags & 0x200000 != 0 {
            let raw = reader.read_u16_le()?;
            let packed = reader.read_u32_le()? as i32;
            let spot_flags = reader.read_u8_s()?;
            blocks.push(PlayerBlock::Spot(SpotAnimation::new(3, raw, packed, spot_flags)));
        }
        if flags & 0x800 != 0 {
            let first_x = reader.read_i8_c()?;
            let first_y = reader.read_i8_s()?;
            let second_x = reader.read_i8_a()?;
            let second_y = reader.read_i8_c()?;
            let first_delay = reader.read_u16_le_a()?;
            let second_delay = reader.read_u16_le()?;
            let direction = reader.read_u16_be_a()?;
            blocks.push(PlayerBlock::ForceMove(ForceMove {
                first_x,
                first_y,
                second_x,
                second_y,
                first_delay,
                second_delay,
                direction,
            }));
        }
        if flags & 0x80 != 0 {
            let raw = reader.read_u16_le()?;
            let packed = reader.read_i32_be()?;
            let spot_flags = reader.read_u8_a()?;
            blocks.push(PlayerBlock::Spot(SpotAnimation::new(0, raw, packed, spot_flags)));
        }
        if let Some((x, y)) = pass.pending.remove(&ptr) {
            let movement = if default_speed == MOVEMENT_TELEPORT {
                MOVEMENT_TELEPORT
            } else if default_speed != MOVEMENT_STATIONARY {
                default_speed
            } else {
                self.movement_types[slot]
            };
            if let Some(record) = self.players[slot].as_mut() {
                record.x = x;
                record.y = y;
                pass.events.push(PlayerEvent::Moved {
                    index: ptr,
                    x,
                    y,
                    plane: record.plane,
                    movement,
                });
            }
        }
        Ok(blocks)
    }
}

fn read_skip_count(reader: &mut PayloadReader<'_>) -> InfoResult<u32> {
    Ok(match reader.read_bits(2)? {
        0 => 0,
        1 => reader.read_bits(5)?,
        2 => reader.read_bits(8)?,
        _ => reader.read_bits(11)?,
    })
}

fn read_config_pairs(reader: &mut PayloadReader<'_>) -> InfoResult<Vec<VarEntry>> {
    let count = reader.read_u8()?;
    let mut entries = Vec::new();
    for _ in 0..count {
        let key = reader.read_u16_le()?;
        let value = reader.read_i32_be()?;
        entries.push(VarEntry { key, value });
    }
    Ok(entries)
}
