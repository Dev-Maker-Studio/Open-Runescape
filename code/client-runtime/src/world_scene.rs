use openrs_login::world_packets::zone::{CustomizeCounts, Tile, Zone, ZoneOrigin, ZoneUpdate};
use openrs_login::world_packets::{self, WorldPacket};
use openrs_login::world_state::{LoadingType, MapLayout, decode_dynamic_map, decode_standard_map};
use openrs_protocol::entity_info::{
    AnimationEntry, MOVEMENT_TELEPORT, NpcAdd, NpcBlock, NpcCustomisation, NpcFrame, NpcTypeShape,
    NpcView, OwnPlayer, PlayerBlock, PlayerEvent, PlayerFrame, PlayerView, SpotAnimation,
};
use openrs_protocol::registry::ServerPacket;
use std::cell::RefCell;
use std::collections::{BTreeMap, BTreeSet, VecDeque};

pub const OBJECT_TYPE_SLOTS: [u8; 23] = [
    0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3,
];
pub const SPOT_REMOVE: i32 = 0xffff;
pub const MAX_DEFERRED: usize = 512;
pub const ORIENTATION_UNITS: i32 = 2048;
const FACE_PLAYER_BASE: i32 = 32768;

pub fn loc_layer(shape: u8) -> u8 {
    OBJECT_TYPE_SLOTS
        .get(usize::from(shape))
        .copied()
        .unwrap_or(2)
}

pub fn step_orientation(dx: i32, dy: i32) -> Option<i32> {
    if dx == 0 && dy == 0 {
        return None;
    }
    let angle = f64::atan2(-f64::from(dx), -f64::from(dy));
    let units = (angle * f64::from(ORIENTATION_UNITS) / std::f64::consts::TAU).round() as i32;
    Some(units.rem_euclid(ORIENTATION_UNITS))
}

pub trait ShapeSource {
    fn npc_shape(&self, type_id: i32) -> Option<NpcTypeShape>;
    fn customize_counts(&self, loc_id: i32, shape: u8) -> Option<CustomizeCounts>;
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub enum Miss {
    NpcType(i32),
    LocType(i32),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LocChange {
    Added { id: i32, shape: u8, rotation: u8 },
    Removed,
    Retained,
}

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct LocCustom {
    pub loc_id: i32,
    pub shape: u8,
    pub models: Option<Vec<i32>>,
    pub recolors: Option<Vec<u16>>,
    pub retextures: Option<Vec<u16>>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LocSlot {
    pub change: LocChange,
    pub custom: Option<LocCustom>,
    pub animation: Option<u32>,
    /// Arrival order of the slot's last Create/Destroy, the stream order `SceneGraph.addObject`
    /// ran in; wall decorations read the wall that was placed before them.
    pub order: u64,
}

pub type LocKey = (u8, i32, i32, u8);

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct GroundItem {
    pub id: u16,
    pub amount: u32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SpotAnimEntry {
    pub id: i32,
    pub height: u16,
    pub speed: u16,
    pub rotation: u8,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Projectile {
    pub plane: u8,
    pub start_x: i32,
    pub start_y: i32,
    pub flags: u8,
    pub floor_height: bool,
    pub half_square: bool,
    pub source: i32,
    pub lock_on: i32,
    pub spot_anim: u16,
    pub start_height: i32,
    pub end_height: u16,
    pub start_time: u16,
    pub end_time: u16,
    pub angle: i32,
    pub slope: u16,
    pub x_off: i8,
    pub y_off: i8,
    pub received_cycle: u64,
}

impl Projectile {
    pub fn scene_start(&self, base: (i32, i32)) -> (i32, i32) {
        (
            (self.start_x - base.0) * 512 + 256 + i32::from(self.x_off),
            (self.start_y - base.1) * 512 + 256 + i32::from(self.y_off),
        )
    }

    pub fn expires_at(&self) -> u64 {
        self.received_cycle + u64::from(self.end_time)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PlayerState {
    pub appearance: Option<Vec<u8>>,
    pub appearance_rev: u32,
    pub animations: Vec<AnimationEntry>,
    pub animation_started: u64,
    pub movement_anims: [i32; 4],
    pub movement_speed: u8,
    pub default_move_speed: i8,
    pub movement_type: i8,
    pub face_entity: i32,
    pub spots: Vec<SpotAnimation>,
    pub orientation: i32,
    pub last_position: Option<(i32, i32)>,
    pub moved_tick: Option<u32>,
}

impl PlayerState {
    fn new(position: Option<(i32, i32)>) -> Self {
        Self {
            appearance: None,
            appearance_rev: 0,
            animations: Vec::new(),
            animation_started: 0,
            movement_anims: [-1; 4],
            movement_speed: 0,
            default_move_speed: 0,
            movement_type: 0,
            face_entity: -1,
            spots: Vec::new(),
            orientation: 0,
            last_position: position,
            moved_tick: None,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NpcState {
    pub type_override: Option<i32>,
    pub animations: Vec<AnimationEntry>,
    pub animation_started: u64,
    pub movement_anims: Option<[i32; 4]>,
    pub movement_speed: u8,
    pub render_anim: Option<i32>,
    pub customisation: Option<NpcCustomisation>,
    pub face_entity: i32,
    pub face_tile: Option<(u16, u16)>,
    pub spots: Vec<SpotAnimation>,
    pub orientation: i32,
    pub moved_tick: Option<u32>,
}

impl NpcState {
    fn new(orientation: i32) -> Self {
        Self {
            type_override: None,
            animations: Vec::new(),
            animation_started: 0,
            movement_anims: None,
            movement_speed: 0,
            render_anim: None,
            customisation: None,
            face_entity: -1,
            face_tile: None,
            spots: Vec::new(),
            orientation,
            moved_tick: None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SceneEffect {
    VarBit {
        id: u16,
        value: i32,
    },
    ZoneOrigin {
        x: i32,
        y: i32,
        plane: u8,
        full: bool,
    },
    ZoneUpdates {
        count: usize,
        applied: usize,
    },
    Players {
        entered: usize,
        moved: usize,
        left: usize,
        updated: usize,
    },
    Npcs {
        added: usize,
        moved: usize,
        removed: usize,
        updated: usize,
    },
    MapChanged,
    Deferred {
        queued: usize,
    },
    Dropped,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SceneError(pub String);

struct Pending {
    packet: ServerPacket,
    payload: Vec<u8>,
}

pub struct WorldScene {
    base: (i32, i32),
    map_size: i32,
    ignore_zone_limits: bool,
    zone: Option<(i32, i32, u8)>,
    locs: BTreeMap<LocKey, LocSlot>,
    loc_order: u64,
    ground_items: BTreeMap<(u8, i32, i32), Vec<GroundItem>>,
    spots: BTreeMap<(u8, i32, i32), SpotAnimEntry>,
    projectiles: Vec<Projectile>,
    pub players: PlayerView,
    pub player_states: BTreeMap<u16, PlayerState>,
    pub npcs: NpcView,
    pub npc_states: BTreeMap<u16, NpcState>,
    varbits: BTreeMap<u16, i32>,
    tick: u32,
    cycle: u64,
    deferred: VecDeque<Pending>,
    misses: BTreeSet<Miss>,
    errors: usize,
    last_error: Option<String>,
    own_index: u16,
    audio: Vec<i32>,
}

impl WorldScene {
    pub fn new(own_index: u16, map: &MapLayout, own: Option<OwnPlayer>) -> Self {
        let mut scene = Self {
            base: (map.base.x, map.base.y),
            map_size: i32::from(map.map_size.size()),
            ignore_zone_limits: ignores_limits(map.loading_type),
            zone: None,
            locs: BTreeMap::new(),
            loc_order: 0,
            ground_items: BTreeMap::new(),
            spots: BTreeMap::new(),
            projectiles: Vec::new(),
            players: PlayerView::new(own_index),
            player_states: BTreeMap::new(),
            npcs: NpcView::new(),
            npc_states: BTreeMap::new(),
            varbits: BTreeMap::new(),
            tick: 0,
            cycle: 0,
            deferred: VecDeque::new(),
            misses: BTreeSet::new(),
            errors: 0,
            last_error: None,
            own_index,
            audio: Vec::new(),
        };
        if let Some(own) = own {
            scene.players.my_plane = own.plane;
            scene
                .player_states
                .insert(own_index, PlayerState::new(Some((own.x, own.y))));
        }
        scene
    }

    pub fn from_initial(
        own_index: u16,
        map: &MapLayout,
        payload: &[u8],
    ) -> Result<Self, SceneError> {
        let mut players = PlayerView::new(own_index);
        let own = players
            .decode_own_player(payload)
            .map_err(|error| SceneError(format!("{error:?}")))?;
        let mut scene = Self::new(own_index, map, Some(own));
        scene.players = players;
        Ok(scene)
    }

    pub fn handles(packet: ServerPacket) -> bool {
        matches!(
            packet,
            ServerPacket::VARBIT_SMALL
                | ServerPacket::VARBIT_LARGE
                | ServerPacket::PLAYER_UPDATE
                | ServerPacket::NPC_UPDATE
                | ServerPacket::NPC_UPDATE_LARGE
                | ServerPacket::REGION
                | ServerPacket::DYNAMIC_MAP_REGION
        ) || world_packets::zone::is_zone_packet(packet)
    }

    pub fn add_static_locs(&mut self, placements: &[openrs_scene::locs::ScenePlacement]) -> usize {
        let mut added = 0;
        for placement in placements {
            let (Ok(plane), Ok(shape), Ok(rotation)) = (
                u8::try_from(placement.plane),
                u8::try_from(placement.kind),
                u8::try_from(placement.rotation),
            ) else {
                continue;
            };
            if placement.id < 0 {
                continue;
            }
            let key = (
                plane,
                placement.scene_x + self.base.0,
                placement.scene_y + self.base.1,
                loc_layer(shape),
            );
            let change = LocChange::Added {
                id: placement.id,
                shape,
                rotation,
            };
            match self.locs.get_mut(&key) {
                Some(slot) if slot.change == LocChange::Retained => {
                    slot.change = change;
                    added += 1;
                }
                Some(_) => {}
                None => {
                    self.loc_order += 1;
                    self.locs.insert(
                        key,
                        LocSlot {
                            change,
                            custom: None,
                            animation: None,
                            order: self.loc_order,
                        },
                    );
                    added += 1;
                }
            }
        }
        added
    }

    pub fn base(&self) -> (i32, i32) {
        self.base
    }

    pub fn map_size(&self) -> i32 {
        self.map_size
    }

    pub fn own_index(&self) -> u16 {
        self.own_index
    }

    pub fn tick(&self) -> u32 {
        self.tick
    }

    pub fn cycle(&self) -> u64 {
        self.cycle
    }

    pub fn error_count(&self) -> usize {
        self.errors
    }

    pub fn last_error(&self) -> Option<&str> {
        self.last_error.as_deref()
    }

    pub fn deferred_count(&self) -> usize {
        self.deferred.len()
    }

    pub fn misses(&self) -> Vec<Miss> {
        self.misses.iter().copied().collect()
    }

    pub fn zone_origin(&self) -> Option<(i32, i32, u8)> {
        self.zone
    }

    pub fn varbits(&self) -> &BTreeMap<u16, i32> {
        &self.varbits
    }

    pub fn varbit(&self, id: u16) -> Option<i32> {
        self.varbits.get(&id).copied()
    }

    pub fn locs(&self) -> &BTreeMap<LocKey, LocSlot> {
        &self.locs
    }

    pub fn ground_items(&self) -> &BTreeMap<(u8, i32, i32), Vec<GroundItem>> {
        &self.ground_items
    }

    pub fn spots(&self) -> &BTreeMap<(u8, i32, i32), SpotAnimEntry> {
        &self.spots
    }

    pub fn projectiles(&self) -> &[Projectile] {
        &self.projectiles
    }

    pub fn own_position(&self) -> Option<(i32, i32, u8)> {
        self.players
            .player(self.own_index)
            .map(|record| (record.x, record.y, record.plane))
    }

    pub fn advance_cycles(&mut self, cycles: u32) {
        self.cycle += u64::from(cycles);
        let cycle = self.cycle;
        self.projectiles.retain(|p| p.expires_at() > cycle);
    }

    pub fn to_scene(&self, x: i32, y: i32) -> (i32, i32) {
        (x - self.base.0, y - self.base.1)
    }

    pub fn player_position(&self, index: u16) -> Option<(i32, i32, u8)> {
        self.players
            .player(index)
            .map(|record| (record.x, record.y, record.plane))
    }

    pub fn npc_position(&self, index: u16) -> Option<(i32, i32, u8)> {
        self.npcs
            .npcs
            .get(&index)
            .map(|record| (record.x, record.y, record.plane))
    }

    fn face_target(&self, entity: i32) -> Option<(i32, i32)> {
        if entity < 0 {
            return None;
        }
        if entity >= FACE_PLAYER_BASE {
            let index = (entity - FACE_PLAYER_BASE) as u16;
            self.player_position(index).map(|p| (p.0, p.1))
        } else {
            self.npc_position(entity as u16).map(|p| (p.0, p.1))
        }
    }

    pub fn player_orientation(&self, index: u16) -> i32 {
        let Some(state) = self.player_states.get(&index) else {
            return 0;
        };
        let Some((x, y, _)) = self.player_position(index) else {
            return state.orientation;
        };
        if state.moved_tick != Some(self.tick)
            && let Some((tx, ty)) = self.face_target(state.face_entity)
            && let Some(orientation) = step_orientation(tx - x, ty - y)
        {
            return orientation;
        }
        state.orientation
    }

    pub fn npc_orientation(&self, index: u16) -> i32 {
        let Some(state) = self.npc_states.get(&index) else {
            return 0;
        };
        let Some((x, y, _)) = self.npc_position(index) else {
            return state.orientation;
        };
        if state.moved_tick == Some(self.tick) {
            return state.orientation;
        }
        if let Some((tx, ty)) = self.face_target(state.face_entity)
            && let Some(orientation) = step_orientation(tx - x, ty - y)
        {
            return orientation;
        }
        if let Some((fx, fy)) = state.face_tile
            && let Some(orientation) =
                step_orientation(i32::from(fx) / 2 - x, i32::from(fy) / 2 - y)
        {
            return orientation;
        }
        state.orientation
    }

    pub fn npc_type_id(&self, index: u16) -> Option<i32> {
        let record = self.npcs.npcs.get(&index)?;
        let state = self.npc_states.get(&index);
        Some(
            state
                .and_then(|state| state.type_override)
                .unwrap_or(record.type_id),
        )
    }

    pub fn apply(
        &mut self,
        packet: ServerPacket,
        payload: &[u8],
        src: &dyn ShapeSource,
    ) -> Vec<SceneEffect> {
        if !self.deferred.is_empty() && !is_varbit(packet) {
            if self.deferred.len() >= MAX_DEFERRED {
                self.note_error("deferred packet queue overflow".to_string());
                return vec![SceneEffect::Dropped];
            }
            self.deferred.push_back(Pending {
                packet,
                payload: payload.to_vec(),
            });
            return vec![SceneEffect::Deferred {
                queued: self.deferred.len(),
            }];
        }
        match self.try_apply(packet, payload, src) {
            Ok(effect) => vec![effect],
            Err(Failure::Missing) => {
                self.deferred.push_back(Pending {
                    packet,
                    payload: payload.to_vec(),
                });
                vec![SceneEffect::Deferred {
                    queued: self.deferred.len(),
                }]
            }
            Err(Failure::Invalid(error)) => {
                self.note_error(error);
                vec![SceneEffect::Dropped]
            }
        }
    }

    pub fn retry(&mut self, src: &dyn ShapeSource) -> Vec<SceneEffect> {
        let mut effects = Vec::new();
        while let Some(front) = self.deferred.front() {
            let (packet, payload) = (front.packet, front.payload.clone());
            match self.try_apply(packet, &payload, src) {
                Ok(effect) => {
                    self.deferred.pop_front();
                    effects.push(effect);
                }
                Err(Failure::Missing) => break,
                Err(Failure::Invalid(error)) => {
                    self.deferred.pop_front();
                    self.note_error(error);
                    effects.push(SceneEffect::Dropped);
                }
            }
        }
        effects
    }

    fn note_error(&mut self, error: String) {
        self.errors += 1;
        self.last_error = Some(error);
    }

    fn try_apply(
        &mut self,
        packet: ServerPacket,
        payload: &[u8],
        src: &dyn ShapeSource,
    ) -> Result<SceneEffect, Failure> {
        self.misses.clear();
        match packet {
            ServerPacket::VARBIT_SMALL | ServerPacket::VARBIT_LARGE => {
                match world_packets::decode(packet, payload).map_err(invalid)? {
                    WorldPacket::VarBit { id, value } => {
                        self.varbits.insert(id, value);
                        Ok(SceneEffect::VarBit { id, value })
                    }
                    _ => Err(Failure::Invalid("unexpected varbit decode".to_string())),
                }
            }
            ServerPacket::REGION | ServerPacket::DYNAMIC_MAP_REGION => {
                let map = if packet == ServerPacket::REGION {
                    decode_standard_map(payload)
                } else {
                    decode_dynamic_map(payload)
                }
                .map_err(invalid)?;
                self.set_map(&map);
                Ok(SceneEffect::MapChanged)
            }
            ServerPacket::PLAYER_UPDATE => {
                let mut next = self.players.clone();
                let frame = next.decode(payload).map_err(invalid)?;
                self.players = next;
                Ok(self.apply_players(frame))
            }
            ServerPacket::NPC_UPDATE | ServerPacket::NPC_UPDATE_LARGE => {
                let Some((bx, by, _)) = self.own_position() else {
                    return Err(Failure::Invalid("npc update before own player".to_string()));
                };
                let missed = RefCell::new(BTreeSet::new());
                let types = |type_id: i32| {
                    let shape = src.npc_shape(type_id);
                    if shape.is_none() {
                        missed.borrow_mut().insert(Miss::NpcType(type_id));
                    }
                    Some(shape.unwrap_or_default())
                };
                let mut next = self.npcs.clone();
                let frame = next.decode(
                    payload,
                    packet == ServerPacket::NPC_UPDATE_LARGE,
                    (bx, by),
                    &types,
                );
                let missed = missed.into_inner();
                if !missed.is_empty() {
                    self.misses = missed;
                    return Err(Failure::Missing);
                }
                let frame = frame.map_err(invalid)?;
                self.npcs = next;
                Ok(self.apply_npcs(frame))
            }
            other => {
                let missed = RefCell::new(BTreeSet::new());
                let mut counts = |loc_id: i32, shape: u8| match src.customize_counts(loc_id, shape)
                {
                    Some(counts) => counts,
                    None => {
                        missed.borrow_mut().insert(Miss::LocType(loc_id));
                        CustomizeCounts::default()
                    }
                };
                let decoded = world_packets::decode_with(other, payload, &mut counts);
                let missed = missed.into_inner();
                if !missed.is_empty() {
                    self.misses = missed;
                    return Err(Failure::Missing);
                }
                match decoded.map_err(invalid)? {
                    WorldPacket::Zone(zone) => Ok(self.apply_zone(other, zone)),
                    _ => Err(Failure::Invalid("unexpected zone decode".to_string())),
                }
            }
        }
    }

    fn set_map(&mut self, map: &MapLayout) {
        self.base = (map.base.x, map.base.y);
        self.map_size = i32::from(map.map_size.size());
        self.ignore_zone_limits = ignores_limits(map.loading_type);
        self.zone = None;
        self.locs.clear();
        self.ground_items.clear();
        self.spots.clear();
        self.projectiles.clear();
    }

    fn set_zone(&mut self, origin: ZoneOrigin) -> (i32, i32, u8) {
        let zone = (self.base.0 + origin.x, self.base.1 + origin.y, origin.plane);
        self.zone = Some(zone);
        zone
    }

    fn clear_zone(&mut self, zone: (i32, i32, u8)) {
        let (zx, zy, plane) = zone;
        let inside = |x: i32, y: i32| (zx..zx + 8).contains(&x) && (zy..zy + 8).contains(&y);
        self.locs
            .retain(|key, _| !(key.0 == plane && inside(key.1, key.2)));
        self.ground_items
            .retain(|key, _| !(key.0 == plane && inside(key.1, key.2)));
        self.spots
            .retain(|key, _| !(key.0 == plane && inside(key.1, key.2)));
    }

    fn apply_zone(&mut self, packet: ServerPacket, zone: Zone) -> SceneEffect {
        match zone {
            Zone::Origin(origin) => {
                let (x, y, plane) = self.set_zone(origin);
                let full = packet == ServerPacket::UPDATE_ZONE_FULL_FOLLOWS;
                if full {
                    self.clear_zone((x, y, plane));
                }
                SceneEffect::ZoneOrigin { x, y, plane, full }
            }
            Zone::Enclosed { origin, updates } => {
                let zone = self.set_zone(origin);
                let applied = updates
                    .iter()
                    .filter(|update| self.apply_update(zone, update))
                    .count();
                SceneEffect::ZoneUpdates {
                    count: updates.len(),
                    applied,
                }
            }
            Zone::Update(update) => {
                let applied = match self.zone {
                    Some(zone) => usize::from(self.apply_update(zone, &update)),
                    None => 0,
                };
                SceneEffect::ZoneUpdates { count: 1, applied }
            }
        }
    }

    fn within(&self, x: i32, y: i32) -> bool {
        self.ignore_zone_limits
            || ((0..self.map_size).contains(&(x - self.base.0))
                && (0..self.map_size).contains(&(y - self.base.1)))
    }

    pub fn take_audio(&mut self) -> Vec<i32> {
        std::mem::take(&mut self.audio)
    }

    fn apply_update(&mut self, zone: (i32, i32, u8), update: &ZoneUpdate) -> bool {
        if let Some(ints) = crate::audio_events::zone_ints((zone.0 - self.base.0, zone.1 - self.base.1, zone.2), update) {
            self.audio.extend(ints);
        }
        let abs = |tile: &Tile| (zone.0 + i32::from(tile.x), zone.1 + i32::from(tile.y));
        let plane = zone.2;
        match update {
            ZoneUpdate::CreateGroundItem { amount, item, tile } => {
                let (x, y) = abs(tile);
                self.within(x, y) && {
                    self.add_ground_item(plane, x, y, *item, *amount);
                    true
                }
            }
            ZoneUpdate::RemoveGroundItem { tile, item } => {
                let (x, y) = abs(tile);
                self.within(x, y) && self.remove_ground_item(plane, x, y, *item)
            }
            ZoneUpdate::GroundItemReveal {
                player,
                tile,
                amount,
                item,
            } => {
                let (x, y) = abs(tile);
                *player != self.own_index && self.within(x, y) && {
                    self.add_ground_item(plane, x, y, *item, *amount);
                    true
                }
            }
            ZoneUpdate::GroundItemCount {
                tile,
                item,
                old_amount,
                new_amount,
            } => {
                let (x, y) = abs(tile);
                let id = item & 0x7fff;
                self.within(x, y)
                    && self
                        .ground_items
                        .get_mut(&(plane, x, y))
                        .and_then(|items| {
                            items
                                .iter_mut()
                                .find(|node| node.id == id && node.amount == u32::from(*old_amount))
                        })
                        .map(|node| node.amount = u32::from(*new_amount))
                        .is_some()
            }
            ZoneUpdate::CreateObject {
                object,
                tile,
                shape,
                rotation,
            } => {
                let (x, y) = abs(tile);
                if !self.within(x, y) {
                    return false;
                }
                let change = if *object < 0 {
                    LocChange::Removed
                } else {
                    LocChange::Added {
                        id: *object,
                        shape: *shape,
                        rotation: *rotation,
                    }
                };
                self.loc_order += 1;
                self.locs.insert(
                    (plane, x, y, loc_layer(*shape)),
                    LocSlot {
                        change,
                        custom: None,
                        animation: None,
                        order: self.loc_order,
                    },
                );
                true
            }
            ZoneUpdate::DestroyObject { shape, tile, .. } => {
                let (x, y) = abs(tile);
                self.within(x, y) && {
                    self.loc_order += 1;
                    self.locs.insert(
                        (plane, x, y, loc_layer(*shape)),
                        LocSlot {
                            change: LocChange::Removed,
                            custom: None,
                            animation: None,
                            order: self.loc_order,
                        },
                    );
                    true
                }
            }
            ZoneUpdate::CustomizeObject {
                loc_id,
                shape,
                transform_flags,
                tile,
                models,
                recolors,
                retextures,
            } => {
                let (x, y) = abs(tile);
                if !self.within(x, y) {
                    return false;
                }
                let order = self.loc_order + 1;
                let slot = self
                    .locs
                    .entry((plane, x, y, loc_layer(*shape)))
                    .or_insert(LocSlot {
                        change: LocChange::Retained,
                        custom: None,
                        animation: None,
                        order,
                    });
                slot.custom = if transform_flags & 1 == 1 {
                    None
                } else {
                    Some(LocCustom {
                        loc_id: *loc_id,
                        shape: *shape,
                        models: models.clone(),
                        recolors: recolors.clone(),
                        retextures: retextures.clone(),
                    })
                };
                true
            }
            ZoneUpdate::ObjAnim {
                shape,
                tile,
                animation,
                ..
            } => {
                let (x, y) = abs(tile);
                if !self.within(x, y) {
                    return false;
                }
                let order = self.loc_order + 1;
                let slot = self
                    .locs
                    .entry((plane, x, y, loc_layer(*shape)))
                    .or_insert(LocSlot {
                        change: LocChange::Retained,
                        custom: None,
                        animation: None,
                        order,
                    });
                slot.animation = Some(*animation);
                true
            }
            ZoneUpdate::SpotAnim {
                tile,
                spot_anim,
                height,
                speed,
                rotation,
            } => {
                let (x, y) = abs(tile);
                if !self.within(x, y) {
                    return false;
                }
                if *spot_anim < 0 {
                    self.spots.remove(&(plane, x, y));
                } else {
                    self.spots.insert(
                        (plane, x, y),
                        SpotAnimEntry {
                            id: *spot_anim,
                            height: *height,
                            speed: *speed,
                            rotation: *rotation,
                        },
                    );
                }
                true
            }
            ZoneUpdate::MapProjanim {
                flags,
                floor_height,
                tile,
                x_off,
                y_off,
                lock_on,
                spot_anim,
                start_height,
                end_height,
                start_time,
                end_time,
                angle,
                slope,
            } => {
                let (x, y) = abs(tile);
                self.within(x, y) && {
                    self.projectiles.push(Projectile {
                        plane,
                        start_x: x,
                        start_y: y,
                        flags: *flags,
                        floor_height: *floor_height,
                        half_square: false,
                        source: -1,
                        lock_on: i32::from(*lock_on),
                        spot_anim: *spot_anim,
                        start_height: i32::from(*start_height),
                        end_height: *end_height,
                        start_time: *start_time,
                        end_time: *end_time,
                        angle: *angle,
                        slope: *slope,
                        x_off: *x_off,
                        y_off: *y_off,
                        received_cycle: self.cycle,
                    });
                    true
                }
            }
            ZoneUpdate::MapProjanimHalfsq {
                coords,
                flags,
                x_off,
                y_off,
                source,
                lock_on,
                spot_anim,
                start_height,
                end_height,
                start_time,
                end_time,
                angle,
                slope,
            } => {
                let tile = Tile {
                    x: (coords >> 4) & 7,
                    y: coords & 7,
                };
                let (x, y) = abs(&tile);
                self.within(x, y) && {
                    self.projectiles.push(Projectile {
                        plane,
                        start_x: x,
                        start_y: y,
                        flags: *flags,
                        floor_height: false,
                        half_square: true,
                        source: i32::from(*source),
                        lock_on: i32::from(*lock_on),
                        spot_anim: *spot_anim,
                        start_height: *start_height,
                        end_height: *end_height,
                        start_time: *start_time,
                        end_time: *end_time,
                        angle: *angle,
                        slope: *slope,
                        x_off: *x_off,
                        y_off: *y_off,
                        received_cycle: self.cycle,
                    });
                    true
                }
            }
            ZoneUpdate::ObjectPrefetch { .. }
            | ZoneUpdate::MidiSongLocation { .. }
            | ZoneUpdate::TileMessage { .. }
            | ZoneUpdate::SoundArea { .. } => false,
        }
    }

    fn add_ground_item(&mut self, plane: u8, x: i32, y: i32, item: u16, amount: u16) {
        self.ground_items
            .entry((plane, x, y))
            .or_default()
            .push(GroundItem {
                id: item & 0x7fff,
                amount: u32::from(amount),
            });
    }

    fn remove_ground_item(&mut self, plane: u8, x: i32, y: i32, item: u16) -> bool {
        let id = item & 0x7fff;
        let key = (plane, x, y);
        let Some(items) = self.ground_items.get_mut(&key) else {
            return false;
        };
        let Some(position) = items.iter().position(|node| node.id == id) else {
            return false;
        };
        items.remove(position);
        if items.is_empty() {
            self.ground_items.remove(&key);
        }
        true
    }

    fn apply_players(&mut self, frame: PlayerFrame) -> SceneEffect {
        self.tick += 1;
        let tick = self.tick;
        let cycle = self.cycle;
        let (mut entered, mut moved, mut left) = (0, 0, 0);
        for event in &frame.events {
            match event {
                PlayerEvent::Entered { index, x, y, .. } => {
                    entered += 1;
                    self.player_states
                        .insert(*index, PlayerState::new(Some((*x, *y))));
                }
                PlayerEvent::Moved {
                    index,
                    x,
                    y,
                    movement,
                    ..
                } => {
                    moved += 1;
                    let state = self
                        .player_states
                        .entry(*index)
                        .or_insert_with(|| PlayerState::new(None));
                    if *movement != MOVEMENT_TELEPORT
                        && let Some((px, py)) = state.last_position
                        && let Some(orientation) = step_orientation(*x - px, *y - py)
                    {
                        state.orientation = orientation;
                    }
                    state.last_position = Some((*x, *y));
                    state.moved_tick = Some(tick);
                }
                PlayerEvent::Left { index } => {
                    left += 1;
                    self.player_states.remove(index);
                }
            }
        }
        let updated = frame.updates.len();
        for (index, blocks) in frame.updates {
            let position = self.player_position(index).map(|p| (p.0, p.1));
            let state = self
                .player_states
                .entry(index)
                .or_insert_with(|| PlayerState::new(position));
            for block in blocks {
                let animate = matches!(block, PlayerBlock::Animate(_));
                apply_player_block(state, block, tick);
                if animate {
                    state.animation_started = cycle;
                }
            }
        }
        SceneEffect::Players {
            entered,
            moved,
            left,
            updated,
        }
    }

    fn apply_npcs(&mut self, frame: NpcFrame) -> SceneEffect {
        let tick = self.tick;
        let cycle = self.cycle;
        let (added, moved, removed, updated) = (
            frame.added.len(),
            frame.moved.len(),
            frame.removed.len(),
            frame.updates.len(),
        );
        for id in &frame.removed {
            self.npc_states.remove(id);
        }
        for add in &frame.added {
            self.apply_npc_add(add);
        }
        for step in &frame.moved {
            let state = self
                .npc_states
                .entry(step.id)
                .or_insert_with(|| NpcState::new(0));
            if let Some(orientation) = step
                .steps
                .iter()
                .rev()
                .find_map(|step| step_orientation(step.dx, step.dy))
            {
                state.orientation = orientation;
            }
            state.moved_tick = Some(tick);
        }
        for (id, blocks) in frame.updates {
            let state = self
                .npc_states
                .entry(id)
                .or_insert_with(|| NpcState::new(0));
            for block in blocks {
                let animate = matches!(block, NpcBlock::Animate(_));
                apply_npc_block(state, block);
                if animate {
                    state.animation_started = cycle;
                }
            }
        }
        SceneEffect::Npcs {
            added,
            moved,
            removed,
            updated,
        }
    }

    fn apply_npc_add(&mut self, add: &NpcAdd) {
        let orientation = i32::from(add.rotation >> 3) & (ORIENTATION_UNITS - 1);
        let mut state = NpcState::new(orientation);
        if add.jump {
            state.moved_tick = Some(self.tick);
        }
        self.npc_states.insert(add.id, state);
    }
}

fn ignores_limits(loading_type: LoadingType) -> bool {
    matches!(
        loading_type,
        LoadingType::InstanceLarge | LoadingType::InstanceLargeNoReloadUpdateZones
    )
}

fn is_varbit(packet: ServerPacket) -> bool {
    matches!(
        packet,
        ServerPacket::VARBIT_SMALL | ServerPacket::VARBIT_LARGE
    )
}

enum Failure {
    Missing,
    Invalid(String),
}

fn invalid(error: impl std::fmt::Debug) -> Failure {
    Failure::Invalid(format!("{error:?}"))
}

fn upsert_spot(spots: &mut Vec<SpotAnimation>, spot: SpotAnimation) {
    spots.retain(|existing| existing.index != spot.index);
    if spot.id != -1 && spot.id != SPOT_REMOVE {
        spots.push(spot);
    }
}

fn apply_player_block(state: &mut PlayerState, block: PlayerBlock, tick: u32) {
    match block {
        PlayerBlock::Appearance(bytes) => {
            state.appearance = Some(bytes);
            state.appearance_rev += 1;
        }
        PlayerBlock::MovementAnimations { ids, speed } => {
            state.movement_anims = ids;
            state.movement_speed = speed;
        }
        PlayerBlock::Animate(entries) => state.animations = entries,
        PlayerBlock::FaceDirection(direction) if state.moved_tick != Some(tick) => {
            state.orientation = i32::from(direction) & (ORIENTATION_UNITS - 1);
        }
        PlayerBlock::FaceEntity(entity) => state.face_entity = entity,
        PlayerBlock::DefaultMoveSpeed(speed) => state.default_move_speed = speed,
        PlayerBlock::MovementType(kind) => state.movement_type = kind,
        PlayerBlock::Spot(spot) => upsert_spot(&mut state.spots, spot),
        _ => {}
    }
}

fn apply_npc_block(state: &mut NpcState, block: NpcBlock) {
    match block {
        NpcBlock::MovementAnimations { ids, speed } => {
            state.movement_anims = Some(ids);
            state.movement_speed = speed;
        }
        NpcBlock::Customise(customisation) => {
            state.customisation = match customisation {
                NpcCustomisation::Clear => None,
                set => Some(set),
            };
        }
        NpcBlock::RenderAnim(id) => state.render_anim = Some(id),
        NpcBlock::Spot(spot) => upsert_spot(&mut state.spots, spot),
        NpcBlock::FaceEntity(entity) => state.face_entity = entity,
        NpcBlock::Animate(entries) => state.animations = entries,
        NpcBlock::SetType(id) => {
            state.type_override = if id < 0 { None } else { Some(id) };
        }
        NpcBlock::FaceTile { x, y } => {
            state.face_tile = if x == 0 && y == 0 { None } else { Some((x, y)) };
        }
        _ => {}
    }
}

#[cfg(test)]
mod tests;
