use crate::loc_anim::{LocAnim, Resolution, SeqLookup};
use crate::world_pose::{PoseCache, TrackStart};
use crate::world_resources::{Lookup, ResourceStore};
use crate::world_scene::{GroundItem, LocChange, LocCustom, WorldScene};
use openrs_entity::config::{
    BasType, EquipmentDefaults, ItemType, NpcType, PlayerAppearance, SpotAnimType,
    item as load_item, load_mesh,
};
use openrs_entity::mesh::Mesh as EntityMesh;
use openrs_entity::pose::Pose;
use openrs_entity::trig::{cosine, sine};
use openrs_protocol::entity_info::{Appearance, ItemShape, WornSlot, decode_appearance};
use openrs_entity::anim::{SeqType, Sequence};
use openrs_scene::loc_place::{
    LocVertices, Orientation, PlaceKind, SceneHeights, mapped_shape, orient, orient_base,
    orient_finish,
    place_scene_with_sequences,
};
use openrs_scene::locs::ScenePlacement;
use openrs_scene::loc_size::loc_mesh;
use openrs_scene::loc_type::{LocType, decode_loc_type};
use openrs_scene::mesh::Mesh as SceneMesh;
use openrs_scene::surface::SurfaceHeights;
use std::collections::HashMap;

pub const TILE_UNITS: i32 = 512;
const ANGLE_MASK: i32 = 16383;
const MAX_ALTERNATIVE_DEPTH: usize = 4;
const GROUND_SHAPE: u8 = 11;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DrawKind {
    Loc,
    GroundItem,
    Player,
    Npc,
    SpotAnim,
    Projectile,
    HintArrow,
    HintTrail,
}

impl DrawKind {
    pub fn code(self) -> u8 {
        match self {
            DrawKind::Loc => 0,
            DrawKind::GroundItem => 1,
            DrawKind::Player => 2,
            DrawKind::Npc => 3,
            DrawKind::SpotAnim => 4,
            DrawKind::Projectile => 5,
            DrawKind::HintArrow => 6,
            DrawKind::HintTrail => 7,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DrawMesh {
    pub kind: DrawKind,
    pub id: i32,
    pub plane: u8,
    pub tile: (i32, i32),
    pub origin: (i32, i32, i32),
    pub positions: Vec<i32>,
    pub colors: Vec<i16>,
    pub alphas: Vec<u8>,
    pub materials: Vec<i16>,
}

impl DrawMesh {
    fn new(kind: DrawKind, id: i32, plane: u8, tile: (i32, i32), origin: (i32, i32, i32)) -> Self {
        Self {
            kind,
            id,
            plane,
            tile,
            origin,
            positions: Vec::new(),
            colors: Vec::new(),
            alphas: Vec::new(),
            materials: Vec::new(),
        }
    }

    pub fn triangles(&self) -> usize {
        self.colors.len()
    }
}

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct DrawStats {
    pub locs: usize,
    pub ground_items: usize,
    pub players: usize,
    pub npcs: usize,
    pub spots: usize,
    pub projectiles: usize,
    pub hint_arrows: usize,
    pub hint_trail_nodes: usize,
    pub posed: usize,
    pub skipped: usize,
    pub missing_heights: usize,
}

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct WorldDraw {
    pub meshes: Vec<DrawMesh>,
    pub stats: DrawStats,
    pub pending: Vec<(u8, u32)>,
}

impl WorldDraw {
    pub fn incomplete(&self) -> bool {
        !self.pending.is_empty()
    }

    pub fn triangles(&self) -> usize {
        self.meshes.iter().map(DrawMesh::triangles).sum()
    }
}

#[derive(Debug, Clone, Default)]
pub struct HeightMap {
    stride: usize,
    planes: usize,
    heights: Vec<Option<i32>>,
}

impl HeightMap {
    pub fn new(tiles: usize, planes: usize, heights: Vec<Option<i32>>) -> Option<Self> {
        let stride = tiles + 1;
        (heights.len() == planes * stride * stride).then_some(Self {
            stride,
            planes,
            heights,
        })
    }

    pub fn from_surface(surface: &SurfaceHeights, planes: usize) -> Self {
        let tiles = surface.layout().map_size().tiles();
        let stride = tiles + 1;
        let mut heights = Vec::with_capacity(planes * stride * stride);
        for plane in 0..planes {
            for x in 0..stride {
                for y in 0..stride {
                    heights.push(surface.height_native_units(plane, x, y));
                }
            }
        }
        Self {
            stride,
            planes,
            heights,
        }
    }

    pub fn is_empty(&self) -> bool {
        self.heights.is_empty()
    }

    fn corner(&self, plane: usize, x: i32, y: i32) -> Option<i32> {
        let x = usize::try_from(x).ok().filter(|x| *x < self.stride)?;
        let y = usize::try_from(y).ok().filter(|y| *y < self.stride)?;
        if plane >= self.planes {
            return None;
        }
        self.heights[(plane * self.stride + x) * self.stride + y]
    }

    pub fn scene_heights(&self) -> SceneHeights {
        let tiles = self.stride.saturating_sub(1) as i32;
        let per_plane = self.stride * self.stride;
        let mut planes: [Vec<i32>; 4] = Default::default();
        for (plane, values) in planes.iter_mut().enumerate() {
            *values = (0..per_plane)
                .map(|index| {
                    if plane < self.planes {
                        self.heights[plane * per_plane + index].unwrap_or(0)
                    } else {
                        0
                    }
                })
                .collect();
        }
        SceneHeights::new(tiles, tiles, planes)
    }

    pub fn sample(&self, plane: u8, x: i32, y: i32) -> Option<i32> {
        let plane = usize::from(plane);
        let (tx, ty) = (x.div_euclid(TILE_UNITS), y.div_euclid(TILE_UNITS));
        let (fx, fy) = (x.rem_euclid(TILE_UNITS), y.rem_euclid(TILE_UNITS));
        let h00 = self.corner(plane, tx, ty)?;
        let h10 = self.corner(plane, tx + 1, ty)?;
        let h01 = self.corner(plane, tx, ty + 1)?;
        let h11 = self.corner(plane, tx + 1, ty + 1)?;
        let low = (h00 * (TILE_UNITS - fx) + h10 * fx) >> 9;
        let high = (h01 * (TILE_UNITS - fx) + h11 * fx) >> 9;
        Some((low * (TILE_UNITS - fy) + high * fy) >> 9)
    }
}

struct Faces<'a> {
    a: &'a [i16],
    b: &'a [i16],
    c: &'a [i16],
    color: Option<&'a [i16]>,
    alpha: Option<&'a [i8]>,
    material: Option<&'a [i16]>,
    render_type: Option<&'a [i8]>,
    count: usize,
}

#[derive(Default)]
struct Local {
    xs: Vec<i32>,
    ys: Vec<i32>,
    zs: Vec<i32>,
    mirrored: bool,
}

fn scale_local(local: &mut Local, resize: [i32; 3]) {
    if resize == [128; 3] {
        return;
    }
    for (values, factor) in [&mut local.xs, &mut local.ys, &mut local.zs]
        .into_iter()
        .zip(resize)
    {
        for value in values.iter_mut() {
            *value = value.wrapping_mul(factor) >> 7;
        }
    }
}

fn ground_tint(plane: u8, x: i32, y: i32) -> [i32; 4] {
    let mut state = (u32::from(plane) << 28 | (y as u32 & 0x3fff) << 14 | (x as u32 & 0x3fff))
        .wrapping_mul(2_654_435_761)
        .wrapping_add(0x9e37_79b9);
    let mut next = |range: u32| {
        state ^= state << 13;
        state ^= state >> 17;
        state ^= state << 5;
        (state % range) as i32
    };
    [32 + next(4), 3 + next(2), 16 + next(3), next(20)]
}

fn tint_colors(colors: &mut [i16], tint: [i32; 4]) {
    let [hue, saturation, lightness, opacity] = tint;
    if opacity == 0 {
        return;
    }
    for color in colors {
        let hsl = i32::from(*color) & 0xffff;
        let mut h = (hsl >> 10) & 0x3f;
        let mut s = (hsl >> 7) & 0x7;
        let mut l = hsl & 0x7f;
        h += hue.wrapping_sub(h).wrapping_mul(opacity) >> 7;
        s += opacity.wrapping_mul(saturation.wrapping_sub(s)) >> 7;
        l += lightness.wrapping_sub(l).wrapping_mul(opacity) >> 7;
        *color = ((h << 10) | (s << 7) | l) as i16;
    }
}

fn rotate_y(xs: &mut [i32], zs: &mut [i32], angle: i32) {
    let angle = (angle & ANGLE_MASK) as usize;
    let (sin, cos) = (sine()[angle], cosine()[angle]);
    for (x, z) in xs.iter_mut().zip(zs.iter_mut()) {
        let next_x = sin.wrapping_mul(*z).wrapping_add(cos.wrapping_mul(*x)) >> 14;
        *z = cos.wrapping_mul(*z).wrapping_sub(sin.wrapping_mul(*x)) >> 14;
        *x = next_x;
    }
}

fn emit(out: &mut DrawMesh, local: &Local, faces: &Faces) {
    let vertex = |index: i16| usize::from(index as u16);
    let origin = out.origin;
    for face in 0..faces.count {
        if faces.render_type.and_then(|t| t.get(face)) == Some(&2) {
            continue;
        }
        let (Some(a), Some(mut b), Some(mut c)) = (
            faces.a.get(face).copied(),
            faces.b.get(face).copied(),
            faces.c.get(face).copied(),
        ) else {
            break;
        };
        if local.mirrored {
            std::mem::swap(&mut b, &mut c);
        }
        let corners = [vertex(a), vertex(b), vertex(c)];
        if corners.iter().any(|v| *v >= local.xs.len()) {
            continue;
        }
        for v in corners {
            out.positions.push(local.xs[v] + origin.0);
            out.positions.push(local.ys[v] + origin.1);
            out.positions.push(local.zs[v] + origin.2);
        }
        out.colors
            .push(faces.color.and_then(|c| c.get(face)).copied().unwrap_or(0));
        out.alphas.push(
            faces
                .alpha
                .and_then(|a| a.get(face))
                .map_or(0, |a| *a as u8),
        );
        out.materials.push(
            faces
                .material
                .and_then(|m| m.get(face))
                .copied()
                .unwrap_or(-1),
        );
    }
}

fn clone_vertices(
    xs: &Option<Vec<i32>>,
    ys: &Option<Vec<i32>>,
    zs: &Option<Vec<i32>>,
    count: i32,
) -> Local {
    let count = usize::try_from(count).unwrap_or(0);
    let take = |values: &Option<Vec<i32>>| {
        let mut values = values.clone().unwrap_or_default();
        values.resize(count, 0);
        values
    };
    Local {
        xs: take(xs),
        ys: take(ys),
        zs: take(zs),
        mirrored: false,
    }
}

fn scene_faces(mesh: &SceneMesh) -> Option<Faces<'_>> {
    Some(Faces {
        a: mesh.faces_a.as_deref()?,
        b: mesh.faces_b.as_deref()?,
        c: mesh.faces_c.as_deref()?,
        color: mesh.faces_color.as_deref(),
        alpha: mesh.faces_alpha.as_deref(),
        material: mesh.faces_material.as_deref(),
        render_type: mesh.face_render_type.as_deref(),
        count: usize::try_from(mesh.num_faces).unwrap_or(0),
    })
}

fn entity_faces(mesh: &EntityMesh) -> Option<Faces<'_>> {
    Some(Faces {
        a: mesh.faces_a.as_deref()?,
        b: mesh.faces_b.as_deref()?,
        c: mesh.faces_c.as_deref()?,
        color: mesh.faces_color.as_deref(),
        alpha: mesh.faces_alpha.as_deref(),
        material: mesh.faces_material.as_deref(),
        render_type: mesh.face_render_type.as_deref(),
        count: usize::try_from(mesh.num_faces).unwrap_or(0),
    })
}

fn loc_local(orientation: &Orientation<'_>, mesh: &SceneMesh, heights: &SceneHeights) -> Local {
    let base = clone_vertices(
        &mesh.vertices_x,
        &mesh.vertices_y,
        &mesh.vertices_z,
        mesh.num_vertices,
    );
    let depth = usize::try_from(mesh.max_depth).unwrap_or(0);
    let mirrored = orientation.loc.inverted
        || (mapped_shape(orientation.shape) == 2 && orientation.rotation > 3);
    let LocVertices { xs, ys, zs } = orient(
        orientation,
        LocVertices {
            xs: base.xs,
            ys: base.ys,
            zs: base.zs,
        },
        depth,
        heights,
    );
    Local {
        xs,
        ys,
        zs,
        mirrored,
    }
}

fn customized(loc: &LocType, shape: u8, custom: &LocCustom) -> LocType {
    let mut loc = loc.clone();
    if let Some(models) = &custom.models {
        let slot = loc
            .shapes
            .as_ref()
            .and_then(|shapes| shapes.iter().position(|value| *value == shape as i8));
        if let (Some(slot), Some(all)) = (slot, loc.model_ids.as_mut())
            && let Some(entry) = all.get_mut(slot)
        {
            *entry = models.clone();
        }
    }
    if let Some(recolors) = &custom.recolors {
        loc.recolor_dst = Some(recolors.iter().map(|v| *v as i16).collect());
    }
    if let Some(retextures) = &custom.retextures {
        loc.retexture_dst = Some(retextures.iter().map(|v| *v as i16).collect());
    }
    loc
}

pub struct DrawBuilder {
    store: ResourceStore,
    pose: PoseCache,
    loc_types: HashMap<i32, Option<LocType>>,
    varbit_defs: HashMap<i32, Option<openrs_cs2::Varbit>>,
    defaults: Option<EquipmentDefaults>,
    spot_types: HashMap<i32, Option<SpotAnimType>>,
    loc_anims: HashMap<LocAnimKey, LocAnimEntry>,
    hints: HintSet,
}

type LocAnimKey = (u8, i32, i32, u8, i32);

struct LocAnimEntry {
    signature: (i32, u8, u8, u64),
    seen: bool,
    anim: LocAnim,
}

struct LocEntry {
    key: crate::world_scene::LocKey,
    id: i32,
    shape: u8,
    rotation: u8,
    custom: Option<LocCustom>,
    animation: Option<u32>,
    order: u64,
}

struct AnimStep {
    visible: bool,
    pose: Option<(Sequence, SeqType)>,
}

fn entity_mesh_of(mesh: &SceneMesh, vertices: &LocVertices) -> EntityMesh {
    EntityMesh {
        version: mesh.version,
        num_vertices: mesh.num_vertices,
        max_depth: mesh.max_depth,
        num_faces: mesh.num_faces,
        priority: mesh.priority,
        num_textures: 0,
        textures_mapping: None,
        vertices_x: Some(vertices.xs.clone()),
        vertices_y: Some(vertices.ys.clone()),
        vertices_z: Some(vertices.zs.clone()),
        faces_a: None,
        faces_b: None,
        faces_c: None,
        vertex_groups: mesh.vertex_groups.clone(),
        face_render_type: None,
        faces_layer: None,
        faces_alpha: mesh.faces_alpha.clone(),
        face_groups: mesh.face_groups.clone(),
        faces_material: None,
        faces_color: mesh.faces_color.clone(),
        faces_texture: None,
        texture_face_x: None,
        texture_face_y: None,
        texture_face_z: None,
        textures_scale_x: None,
        textures_scale_y: None,
        textures_scale_z: None,
        textures_rotation: None,
        textures_direction: None,
        textures_speed: None,
        textures_utrans: None,
        textures_vtrans: None,
        particle_emitters: None,
        particle_effectors: None,
        billboards: None,
        vertices_submeshes: None,
        faces_submeshes: None,
    }
}

enum Resolved {
    Pending,
    Hidden,
    Type(Box<LocType>),
}

impl DrawBuilder {
    pub fn new(store: ResourceStore) -> Self {
        Self {
            pose: PoseCache::new(store.clone()),
            store,
            loc_types: HashMap::new(),
            varbit_defs: HashMap::new(),
            defaults: None,
            spot_types: HashMap::new(),
            loc_anims: HashMap::new(),
            hints: HintSet::default(),
        }
    }

    pub fn store(&self) -> &ResourceStore {
        &self.store
    }

    pub fn clear(&mut self) {
        self.pose.clear();
        self.loc_types.clear();
        self.varbit_defs.clear();
        self.loc_anims.clear();
        self.defaults = None;
    }

    fn loc_type(&mut self, id: i32) -> Option<Option<LocType>> {
        if let Some(cached) = self.loc_types.get(&id) {
            return Some(cached.clone());
        }
        let decoded = match self.store.loc_type(id) {
            Lookup::Unknown => return None,
            Lookup::Absent => decode_loc_type(id, None, true).ok(),
            Lookup::Present(bytes) => decode_loc_type(id, Some(&bytes), true).ok(),
        };
        self.loc_types.insert(id, decoded.clone());
        Some(decoded)
    }

    fn varbit_value(
        &mut self,
        id: i32,
        scene: &WorldScene,
        varp: &dyn Fn(i32) -> i32,
    ) -> Option<i32> {
        let definition = match self.varbit_defs.get(&id) {
            Some(cached) => *cached,
            None => {
                let decoded = match self.store.varbit_type(id) {
                    Lookup::Unknown => return None,
                    Lookup::Absent => None,
                    Lookup::Present(bytes) => crate::vars::varbit::decode_definition(&bytes).ok(),
                };
                self.varbit_defs.insert(id, decoded);
                decoded
            }
        };
        Some(match definition {
            Some(definition) => {
                crate::vars::varbit::entity_read_bits(&definition, varp(i32::from(definition.varp)))
            }
            None => u16::try_from(id)
                .ok()
                .and_then(|bit| scene.varbit(bit))
                .unwrap_or(0),
        })
    }

    /// `ObjectType.getMultiLoc`: one level, the var picks an alternative and the last entry is
    /// the fallback; an id of -1 hides the loc.
    fn resolve_loc(
        &mut self,
        loc: &LocType,
        scene: &WorldScene,
        varp: &dyn Fn(i32) -> i32,
    ) -> Resolved {
        let Some(alternatives) = loc.alternative_ids.as_ref().filter(|a| !a.is_empty()) else {
            return Resolved::Type(Box::new(loc.clone()));
        };
        let value = if loc.varpbit_id != -1 {
            match self.varbit_value(loc.varpbit_id, scene, varp) {
                Some(value) => value,
                None => return Resolved::Pending,
            }
        } else if loc.varp_id != -1 {
            varp(loc.varp_id)
        } else {
            -1
        };
        let last = alternatives.len() - 1;
        let id = match usize::try_from(value) {
            Ok(index) if index < alternatives.len() && alternatives[index] != -1 => {
                alternatives[index]
            }
            _ => alternatives[last],
        };
        if id < 0 {
            return Resolved::Hidden;
        }
        match self.loc_type(id) {
            None => Resolved::Pending,
            Some(None) => Resolved::Hidden,
            Some(Some(resolved)) => Resolved::Type(Box::new(resolved)),
        }
    }

    fn resolve_npc(
        &mut self,
        type_id: i32,
        scene: &WorldScene,
        varp: &dyn Fn(i32) -> i32,
    ) -> Option<NpcType> {
        let mut id = type_id;
        for _ in 0..MAX_ALTERNATIVE_DEPTH {
            if id < 0 || matches!(self.store.npc_type(id), Lookup::Unknown) {
                return None;
            }
            let ty = NpcType::load(&self.store, id).ok()?;
            let Some(alternatives) = ty.alternative_ids.as_ref().filter(|a| !a.is_empty()) else {
                return Some(ty);
            };
            let value = if ty.varpbit != -1 {
                self.varbit_value(ty.varpbit, scene, varp)?
            } else if ty.varp != -1 {
                varp(ty.varp)
            } else {
                -1
            };
            let last = alternatives.len() - 1;
            id = match usize::try_from(value) {
                Ok(index) if index < last && alternatives[index] != -1 => alternatives[index],
                _ => alternatives[last],
            };
        }
        None
    }

    fn defaults(&mut self) -> Option<EquipmentDefaults> {
        if self.defaults.is_none()
            && let Lookup::Present(bytes) = self.store.equipment_defaults()
        {
            self.defaults = EquipmentDefaults::decode(&bytes).ok();
        }
        self.defaults.clone()
    }

    pub fn build(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        varp: &dyn Fn(i32) -> i32,
    ) -> WorldDraw {
        let mut draw = WorldDraw::default();
        self.build_locs(scene, heights, varp, &mut draw);
        self.build_ground_items(scene, heights, &mut draw);
        self.build_npcs(scene, heights, varp, &mut draw);
        self.build_players(scene, heights, varp, &mut draw);
        self.build_spots(scene, heights, &mut draw);
        self.build_projectiles(scene, heights, &mut draw);
        self.build_hints(scene, heights, &mut draw);
        draw.pending = self.store.needs();
        draw
    }

    fn ground(&self, heights: &HeightMap, draw: &mut WorldDraw, plane: u8, x: i32, z: i32) -> i32 {
        match heights.sample(plane, x, z) {
            Some(height) => height,
            None => {
                draw.stats.missing_heights += 1;
                0
            }
        }
    }

    fn build_locs(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        varp: &dyn Fn(i32) -> i32,
        draw: &mut WorldDraw,
    ) {
        let base = scene.base();
        let scene_heights = heights.scene_heights();
        let cycle = scene.cycle();
        for entry in self.loc_anims.values_mut() {
            entry.seen = false;
        }
        let mut entries: Vec<LocEntry> = scene
            .locs()
            .iter()
            .filter_map(|(key, slot)| match slot.change {
                LocChange::Added {
                    id,
                    shape,
                    rotation,
                } => Some(LocEntry {
                    key: *key,
                    id,
                    shape,
                    rotation,
                    custom: slot.custom.clone(),
                    animation: slot.animation,
                    order: slot.order,
                }),
                _ => None,
            })
            .collect();
        entries.sort_by_key(|entry| entry.order);
        let mut raw: HashMap<i32, LocType> = HashMap::new();
        let mut valid: Vec<LocEntry> = Vec::with_capacity(entries.len());
        for entry in entries {
            match self.loc_type(entry.id) {
                None => continue,
                Some(None) => {
                    draw.stats.skipped += 1;
                    continue;
                }
                Some(Some(ty)) => {
                    raw.entry(entry.id).or_insert(ty);
                }
            }
            valid.push(entry);
        }
        let placements: Vec<ScenePlacement> = valid
            .iter()
            .map(|entry| ScenePlacement {
                id: entry.id,
                plane: i32::from(entry.key.0),
                scene_x: entry.key.1 - base.0,
                scene_y: entry.key.2 - base.1,
                kind: i32::from(entry.shape),
                rotation: i32::from(entry.rotation),
            })
            .collect();
        let sequences: Vec<i32> = valid
            .iter()
            .map(|entry| entry.animation.map_or(-1, |id| id as i32))
            .collect();
        let placed =
            place_scene_with_sequences(&placements, &sequences, &scene_heights, |id| raw.get(&id));
        for piece in placed {
            let Some(entry) = usize::try_from(piece.seq - 1)
                .ok()
                .and_then(|index| valid.get(index))
            else {
                continue;
            };
            let Some(original) = raw.get(&entry.id) else {
                continue;
            };
            let computed = match self.resolve_loc(original, scene, varp) {
                Resolved::Pending => continue,
                Resolved::Hidden => None,
                Resolved::Type(loc) => Some(*loc),
            };
            let animated = piece.kind == PlaceKind::Animated;
            let step = if animated {
                let Some(step) = self.loc_anim_step(entry, piece.role, original, &computed, cycle)
                else {
                    continue;
                };
                Some(step)
            } else {
                None
            };
            if computed.is_none() || step.as_ref().is_some_and(|step| !step.visible) {
                draw.stats.skipped += 1;
                continue;
            }
            let Some(loc) = computed else {
                continue;
            };
            let loc = match &entry.custom {
                Some(custom) => customized(&loc, entry.shape, custom),
                None => loc,
            };
            let Some(mesh) = self.loc_scene_mesh(&loc, entry.shape) else {
                draw.stats.skipped += 1;
                continue;
            };
            let Some(faces) = scene_faces(&mesh) else {
                draw.stats.skipped += 1;
                continue;
            };
            let plane_index = usize::from(entry.key.0);
            if heights
                .sample(entry.key.0, piece.scene_x, piece.scene_y)
                .is_none()
            {
                draw.stats.missing_heights += 1;
            }
            let orientation = Orientation {
                loc: &loc,
                shape: piece.shape,
                rotation: piece.rotation,
                plane: plane_index,
                scene_x: piece.scene_x,
                scene_y: piece.scene_y,
                average_height: piece.average_height,
            };
            let mut out = DrawMesh::new(
                DrawKind::Loc,
                entry.id,
                entry.key.0,
                (entry.key.1, entry.key.2),
                (
                    piece.scene_x + piece.pos_x,
                    piece.average_height,
                    piece.scene_y + piece.pos_y,
                ),
            );
            let posed = step.and_then(|step| step.pose);
            if let Some((sequence, ty)) = posed {
                let posed_ok = self.emit_animated_loc(
                    &mut out,
                    &orientation,
                    &mesh,
                    &faces,
                    &scene_heights,
                    (&sequence, &ty),
                );
                if posed_ok {
                    draw.stats.posed += 1;
                }
            } else {
                let local = loc_local(&orientation, &mesh, &scene_heights);
                emit(&mut out, &local, &faces);
            }
            draw.stats.locs += 1;
            draw.meshes.push(out);
        }
        self.loc_anims.retain(|_, entry| entry.seen);
    }

    /// Advances (or creates) the sequence state of one animated loc piece. `None` means a
    /// sequence config is still in flight and the loc is skipped until the next build.
    fn loc_anim_step(
        &mut self,
        entry: &LocEntry,
        role: i32,
        original: &LocType,
        computed: &Option<LocType>,
        cycle: u64,
    ) -> Option<AnimStep> {
        let key: LocAnimKey = (entry.key.0, entry.key.1, entry.key.2, entry.key.3, role);
        let signature = (entry.id, entry.shape, entry.rotation, entry.order);
        let mut seed = 0x9e37_79b9_7f4a_7c15u64;
        for part in [
            i64::from(key.0),
            i64::from(key.1),
            i64::from(key.2),
            i64::from(key.3),
            i64::from(key.4),
        ] {
            seed = (seed ^ part as u64)
                .wrapping_mul(0x0100_0000_01b3)
                .rotate_left(23);
        }
        let resolution = Resolution {
            original,
            computed: computed.as_ref(),
        };
        let pose = &mut self.pose;
        let mut lookup = |id: i32| match pose.sequence_state(id) {
            None => SeqLookup::Pending,
            Some(None) => SeqLookup::Absent,
            Some(Some(ty)) => SeqLookup::Type(ty),
        };
        let existing = self
            .loc_anims
            .remove(&key)
            .filter(|state| state.signature == signature);
        let mut anim = match existing {
            Some(state) => state.anim,
            None => LocAnim::new(seed, -1, cycle, resolution, &mut lookup).ok()?,
        };
        if let Some(sequence) = entry.animation
            && anim.applied != Some(sequence)
        {
            anim.play(sequence as i32, resolution, cycle, &mut lookup)
                .ok()?;
            anim.applied = Some(sequence);
        }
        let visible = anim.advance(resolution, cycle, &mut lookup).ok()?;
        let pose = anim.ty.clone().map(|ty| (anim.sequence.clone(), ty));
        self.loc_anims.insert(
            key,
            LocAnimEntry {
                signature,
                seen: true,
                anim,
            },
        );
        Some(AnimStep { visible, pose })
    }

    /// The animated vertex pipeline of `AnimatedGameObject`: orient, pose the frame, finish.
    fn emit_animated_loc(
        &mut self,
        out: &mut DrawMesh,
        orientation: &Orientation<'_>,
        mesh: &SceneMesh,
        faces: &Faces<'_>,
        heights: &SceneHeights,
        sequence: (&Sequence, &SeqType),
    ) -> bool {
        let base = clone_vertices(
            &mesh.vertices_x,
            &mesh.vertices_y,
            &mesh.vertices_z,
            mesh.num_vertices,
        );
        let depth = usize::try_from(mesh.max_depth).unwrap_or(0);
        let mirrored = orientation.loc.inverted
            || (mapped_shape(orientation.shape) == 2 && orientation.rotation > 3);
        let oriented = orient_base(
            orientation,
            LocVertices {
                xs: base.xs,
                ys: base.ys,
                zs: base.zs,
            },
            depth,
        );
        let count = usize::try_from(mesh.num_vertices).unwrap_or(0);
        let finish = |vertices: LocVertices| {
            let mut vertices = vertices;
            vertices.xs.resize(count, 0);
            vertices.ys.resize(count, 0);
            vertices.zs.resize(count, 0);
            let LocVertices { xs, ys, zs } = orient_finish(orientation, vertices, depth, heights, true);
            Local {
                xs,
                ys,
                zs,
                mirrored,
            }
        };
        let entity = entity_mesh_of(mesh, &oriented);
        let Ok(mut pose) = Pose::from_mesh(&entity) else {
            emit(out, &finish(oriented), faces);
            return false;
        };
        let posed = self
            .pose
            .pose_sequence(&mut pose, sequence.0, sequence.1, orientation.rotation & 3);
        let local = finish(LocVertices {
            xs: pose.vertices_x.clone(),
            ys: pose.vertices_y.clone(),
            zs: pose.vertices_z.clone(),
        });
        let colored = Faces {
            color: pose.faces_color.as_deref().or(faces.color),
            alpha: pose.faces_alpha.as_deref().or(faces.alpha),
            ..*faces
        };
        emit(out, &local, &colored);
        posed
    }

    fn loc_scene_mesh(&mut self, loc: &LocType, shape: u8) -> Option<SceneMesh> {
        let slot = loc.shapes.as_ref()?.iter().position(|value| {
            i32::from(*value)
                == if shape == GROUND_SHAPE {
                    10
                } else {
                    mapped_shape(i32::from(shape))
                }
        })?;
        let ids = loc.model_ids.as_ref()?.get(slot)?.clone();
        let mut models: HashMap<i32, Vec<u8>> = HashMap::new();
        let mut waiting = false;
        for id in ids {
            match self.store.model(id) {
                Lookup::Present(bytes) => {
                    models.insert(id, bytes);
                }
                Lookup::Unknown => waiting = true,
                Lookup::Absent => return None,
            }
        }
        if waiting {
            return None;
        }
        let mut mesh = loc_mesh(loc, mapped_shape(i32::from(shape)), |id| {
            models.get(&id).map(Vec::as_slice)
        })?;
        if let (Some(src), Some(dst)) = (&loc.recolor_src, &loc.recolor_dst) {
            for (from, to) in src.iter().zip(dst) {
                mesh.recolor(*from, *to).ok()?;
            }
        }
        if let (Some(src), Some(dst)) = (&loc.retexture_src, &loc.retexture_dst) {
            for (from, to) in src.iter().zip(dst) {
                mesh.retexture(*from, *to).ok()?;
            }
        }
        Some(mesh)
    }

    fn build_ground_items(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        draw: &mut WorldDraw,
    ) {
        let base = scene.base();
        let tiles: Vec<_> = scene
            .ground_items()
            .iter()
            .map(|(key, items)| (*key, items.clone()))
            .collect();
        for ((plane, abs_x, abs_y), items) in tiles {
            let stack = self.ground_stack(&items);
            if stack.is_empty() {
                continue;
            }
            let tint = ground_tint(plane, abs_x, abs_y);
            let x = (abs_x - base.0) * TILE_UNITS + 256;
            let z = (abs_y - base.1) * TILE_UNITS + 256;
            let y = self.ground(heights, draw, plane, x, z);
            for (id, amount) in stack {
                let Some((mesh, resize)) = self.item_mesh(id, amount) else {
                    continue;
                };
                let Some(faces) = entity_faces(&mesh) else {
                    continue;
                };
                let mut local = clone_vertices(
                    &mesh.vertices_x,
                    &mesh.vertices_y,
                    &mesh.vertices_z,
                    mesh.num_vertices,
                );
                scale_local(&mut local, resize);
                for vy in &mut local.ys {
                    *vy = vy.wrapping_sub(10);
                }
                let mut out =
                    DrawMesh::new(DrawKind::GroundItem, id, plane, (abs_x, abs_y), (x, y, z));
                emit(&mut out, &local, &faces);
                tint_colors(&mut out.colors, tint);
                draw.stats.ground_items += 1;
                draw.meshes.push(out);
            }
        }
    }

    fn ground_order(&self, items: &[GroundItem]) -> Vec<(i32, u32)> {
        let mut ordered: Vec<(i32, i32, u32)> = Vec::with_capacity(items.len());
        for item in items {
            let (value, stackable) = load_item(&self.store, i32::from(item.id))
                .map_or((0, 0), |ty| (ty.value, ty.stackable));
            let value = if stackable == 1 {
                value.wrapping_mul((item.amount as i32).wrapping_add(1))
            } else {
                value
            };
            let at = ordered
                .iter()
                .position(|(existing, _, _)| value > *existing)
                .unwrap_or(ordered.len());
            ordered.insert(at, (value, i32::from(item.id), item.amount));
        }
        ordered
            .into_iter()
            .map(|(_, id, amount)| (id, amount))
            .collect()
    }

    fn ground_stack(&mut self, items: &[GroundItem]) -> Vec<(i32, u32)> {
        let mut nodes = self.ground_order(items).into_iter();
        let Some(top) = nodes.next() else {
            return Vec::new();
        };
        let mut last = None;
        let mut third = None;
        for (id, amount) in nodes.by_ref() {
            if id != top.0 {
                last = Some((id, amount));
                break;
            }
        }
        if let Some(second) = last {
            for (id, amount) in nodes {
                if id != top.0 && id != second.0 {
                    third = Some((id, amount));
                }
            }
        }
        let mut stack = Vec::with_capacity(3);
        stack.extend(third);
        stack.extend(last);
        stack.push(top);
        stack
    }

    fn item_mesh(&mut self, id: i32, amount: u32) -> Option<(EntityMesh, [i32; 3])> {
        let ty = load_item(&self.store, id).ok()?;
        if let (Some(ids), Some(triggers)) = (&ty.stack_ids, &ty.stack_trigger)
            && amount > 1
        {
            let mut stack = -1;
            for (index, trigger) in triggers.iter().enumerate().take(10) {
                if *trigger != 0 && i64::from(amount) >= i64::from(*trigger) {
                    stack = ids.get(index).copied().unwrap_or(-1);
                }
            }
            if stack != -1 {
                return self.item_mesh(stack, 1);
            }
        }
        let model = ty.model_id;
        let mut mesh = load_mesh(&self.store, model).ok()??;
        if mesh.version < 13 {
            mesh.upscale().ok()?;
        }
        if let (Some(src), Some(dst)) = (&ty.recolor_src, &ty.recolor_dst) {
            for (from, to) in src.iter().zip(dst) {
                mesh.recolor(*from, *to).ok()?;
            }
        }
        if let (Some(src), Some(dst)) = (&ty.retex_src, &ty.retex_dst) {
            for (from, to) in src.iter().zip(dst) {
                mesh.retexture(*from, *to).ok()?;
            }
        }
        Some((mesh, ty.resize))
    }

    fn pose_entity(&mut self, pose: &mut Pose, spec: &EntitySpec) -> bool {
        let key = (i32::from(spec.kind.code()), spec.id);
        let explicit = spec.sequence >= 0 && spec.sequence != spec.base_sequence;
        if spec.sequence >= 0 {
            let start = TrackStart {
                seq_id: spec.sequence,
                started: spec.started,
                speed: spec.speed,
                replay: 0,
            };
            let animated = self
                .pose
                .animate_tracked(pose, key, start, spec.cycle, !explicit);
            if animated || !explicit || !self.pose.track_finished(key) {
                return animated;
            }
        }
        if spec.base_sequence < 0 {
            return false;
        }
        let start = TrackStart {
            seq_id: spec.base_sequence,
            started: 0,
            speed: 0,
            replay: 0,
        };
        self.pose
            .animate_tracked(pose, (key.0, -1 - key.1), start, spec.cycle, true)
    }

    fn entity_draw(
        &mut self,
        spec: EntitySpec,
        mesh: &EntityMesh,
        heights: &HeightMap,
        draw: &mut WorldDraw,
    ) {
        let Some(faces) = entity_faces(mesh) else {
            draw.stats.skipped += 1;
            return;
        };
        let Ok(mut pose) = Pose::from_mesh(mesh) else {
            draw.stats.skipped += 1;
            return;
        };
        if self.pose_entity(&mut pose, &spec) {
            draw.stats.posed += 1;
        }
        let count = usize::try_from(pose.num_vertices).unwrap_or(0);
        let mut local = Local {
            xs: pose.vertices_x.clone(),
            ys: pose.vertices_y.clone(),
            zs: pose.vertices_z.clone(),
            mirrored: false,
        };
        local.xs.resize(count, 0);
        local.ys.resize(count, 0);
        local.zs.resize(count, 0);
        if let Some((scale_xz, scale_y)) = spec.scale {
            if scale_xz != 128 {
                for value in local.xs.iter_mut().chain(local.zs.iter_mut()) {
                    *value = value.wrapping_mul(scale_xz) >> 7;
                }
            }
            if scale_y != 128 {
                for value in &mut local.ys {
                    *value = value.wrapping_mul(scale_y) >> 7;
                }
            }
        }
        if spec.orientation & 2047 != 0 {
            rotate_y(&mut local.xs, &mut local.zs, (spec.orientation & 2047) * 8);
        }
        let x = spec.tile.0 * TILE_UNITS + spec.size * 256;
        let z = spec.tile.1 * TILE_UNITS + spec.size * 256;
        let y = self.ground(heights, draw, spec.plane, x, z);
        let mut out = DrawMesh::new(spec.kind, spec.id, spec.plane, spec.absolute, (x, y, z));
        let colors = pose.faces_color.clone();
        let alphas = pose.faces_alpha.clone();
        let posed = Faces {
            color: colors.as_deref().or(faces.color),
            alpha: alphas.as_deref().or(faces.alpha),
            ..faces
        };
        emit(&mut out, &local, &posed);
        match spec.kind {
            DrawKind::Player => draw.stats.players += 1,
            _ => draw.stats.npcs += 1,
        }
        draw.meshes.push(out);
    }

    fn build_npcs(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        varp: &dyn Fn(i32) -> i32,
        draw: &mut WorldDraw,
    ) {
        let base = scene.base();
        let indices: Vec<u16> = scene.npc_states.keys().copied().collect();
        for index in indices {
            let (Some((x, y, plane)), Some(type_id)) =
                (scene.npc_position(index), scene.npc_type_id(index))
            else {
                continue;
            };
            let Some(state) = scene.npc_states.get(&index) else {
                continue;
            };
            let orientation = scene.npc_orientation(index);
            let moved = state.moved_tick == Some(scene.tick());
            let entry = state.animations.iter().find(|entry| entry.id >= 0);
            let explicit = entry.map(|entry| entry.id);
            let entry_speed = entry.map_or(0, |entry| i32::from(entry.speed));
            let render_anim = state.render_anim;
            let mut spec = EntitySpec {
                kind: DrawKind::Npc,
                id: i32::from(index),
                plane,
                absolute: (x, y),
                tile: (x - base.0, y - base.1),
                size: 1,
                orientation,
                sequence: -1,
                cycle: scene.cycle(),
                started: state.animation_started,
                speed: entry_speed,
                base_sequence: -1,
                scale: None,
            };
            if let Some(mesh) = self.npc_mesh(
                type_id,
                explicit,
                render_anim,
                moved,
                &mut spec,
                (scene, varp),
            ) {
                self.entity_draw(spec, &mesh, heights, draw);
            }
        }
    }

    fn npc_mesh(
        &mut self,
        type_id: i32,
        explicit: Option<i32>,
        render_anim: Option<i32>,
        moved: bool,
        spec: &mut EntitySpec,
        ctx: (&WorldScene, &dyn Fn(i32) -> i32),
    ) -> Option<EntityMesh> {
        let ty = self.resolve_npc(type_id, ctx.0, ctx.1)?;
        let defaults = self.defaults()?;
        let bas_id = render_anim.filter(|id| *id >= 0).unwrap_or(ty.bas_id);
        let bas = if bas_id >= 0 {
            BasType::load(&self.store, bas_id, &defaults).ok()
        } else {
            None
        };
        spec.size = ty.size.max(1);
        spec.scale = Some((ty.scale_xz, ty.scale_y));
        spec.base_sequence = match &bas {
            Some(bas) if moved => bas.walk_sequence,
            Some(bas) => bas.stand_sequence,
            None => -1,
        };
        spec.sequence = explicit.unwrap_or(spec.base_sequence);
        ty.body_mesh(&self.store, bas.as_ref()).ok()
    }

    fn build_players(
        &mut self,
        scene: &WorldScene,
        heights: &HeightMap,
        varp: &dyn Fn(i32) -> i32,
        draw: &mut WorldDraw,
    ) {
        let base = scene.base();
        let indices: Vec<u16> = scene.player_states.keys().copied().collect();
        for index in indices {
            let Some((x, y, plane)) = scene.player_position(index) else {
                continue;
            };
            let Some(state) = scene.player_states.get(&index) else {
                continue;
            };
            let Some(bytes) = state.appearance.clone() else {
                continue;
            };
            let entry = state.animations.iter().find(|entry| entry.id >= 0);
            let explicit = entry.map(|entry| entry.id);
            let entry_speed = entry.map_or(0, |entry| i32::from(entry.speed));
            let moved = state.moved_tick == Some(scene.tick());
            let mut spec = EntitySpec {
                kind: DrawKind::Player,
                id: i32::from(index),
                plane,
                absolute: (x, y),
                tile: (x - base.0, y - base.1),
                size: 1,
                orientation: scene.player_orientation(index),
                sequence: -1,
                cycle: scene.cycle(),
                started: state.animation_started,
                speed: entry_speed,
                base_sequence: -1,
                scale: None,
            };
            if let Some(mesh) = self.player_mesh(&bytes, explicit, moved, &mut spec, (scene, varp))
            {
                self.entity_draw(spec, &mesh, heights, draw);
            }
        }
    }

    fn decode_worn(&self, bytes: &[u8], defaults: &EquipmentDefaults) -> Option<Appearance> {
        let store = self.store.clone();
        let shapes = move |id: i32| -> Option<ItemShape> {
            let Lookup::Present(bytes) = store.item_type(id) else {
                return None;
            };
            let ty = ItemType::decode(id, &bytes).ok()?;
            Some(ItemShape {
                team_id: ty.team_id,
                has_body2: ty.male_equip[1] != -1 || ty.female_equip[1] != -1,
                has_body3: ty.male_equip[2] != -1 || ty.female_equip[2] != -1,
                has_head2: ty.male_head[1] != -1 || ty.female_head[1] != -1,
                recolors: ty.recolor_src.as_ref().map(Vec::len),
                retextures: ty.retex_src.as_ref().map(Vec::len),
            })
        };
        decode_appearance(bytes, &defaults.customizable_obj_slots, &shapes).ok()
    }

    pub fn local_appearance(&mut self, bytes: &[u8]) -> Option<openrs_cs2::PlayerAppearance> {
        let defaults = self.defaults()?;
        let appearance = self.decode_worn(bytes, &defaults)?;
        if appearance.hidden || appearance.morph >= 0 {
            return None;
        }
        let mut recolors = vec![0i32; 10];
        for (target, value) in recolors.iter_mut().zip(appearance.static_recolors) {
            *target = i32::from(value);
        }
        let mut worn = openrs_cs2::PlayerAppearance {
            bas_id: i32::from(appearance.render_emote),
            appearance: Some(
                appearance
                    .slots
                    .iter()
                    .map(|slot| match slot {
                        WornSlot::Empty => 0,
                        WornSlot::Item(id) => 0x4000_0000 | id,
                        WornSlot::Kit(id) => (0x8000_0000u32 as i32) | id,
                    })
                    .collect(),
            ),
            mesh_modifiers: None,
            recolor_dst_indices: Some(recolors),
            female: !appearance.male,
            hash: 0,
        };
        worn.calculate_hash().ok()?;
        Some(worn)
    }

    fn player_mesh(
        &mut self,
        bytes: &[u8],
        explicit: Option<i32>,
        moved: bool,
        spec: &mut EntitySpec,
        ctx: (&WorldScene, &dyn Fn(i32) -> i32),
    ) -> Option<EntityMesh> {
        let defaults = self.defaults()?;
        let appearance = self.decode_worn(bytes, &defaults)?;
        if appearance.hidden {
            return None;
        }
        if appearance.morph >= 0 {
            return self.npc_mesh(appearance.morph, explicit, None, moved, spec, ctx);
        }
        let parts: Vec<i32> = appearance
            .slots
            .iter()
            .map(|slot| match slot {
                WornSlot::Empty => 0,
                WornSlot::Item(id) => 0x4000_0000 | id,
                WornSlot::Kit(id) => (0x8000_0000u32 as i32) | id,
            })
            .collect();
        let mut recolors = [0i32; 10];
        for (target, value) in recolors.iter_mut().zip(appearance.static_recolors) {
            *target = i32::from(value);
        }
        let bas_id = i32::from(appearance.render_emote);
        let bas = BasType::load(&self.store, bas_id, &defaults).ok();
        let worn = PlayerAppearance {
            appearance: parts,
            modifiers: None,
            female: !appearance.male,
            bas_id,
            recolor_dst_indices: recolors,
        };
        spec.base_sequence = match &bas {
            Some(bas) if moved => bas.walk_sequence,
            Some(bas) => bas.stand_sequence,
            None => -1,
        };
        spec.sequence = explicit.unwrap_or(spec.base_sequence);
        let (shield, weapon) = explicit
            .and_then(|id| self.pose.sequence(id))
            .map_or((-1, -1), |ty| {
                (ty.replacement_shield, ty.replacement_weapon)
            });
        worn.body_mesh(&self.store, bas.as_ref(), &defaults, shield, weapon)
            .ok()
    }
}

struct EntitySpec {
    kind: DrawKind,
    id: i32,
    plane: u8,
    absolute: (i32, i32),
    tile: (i32, i32),
    size: i32,
    orientation: i32,
    sequence: i32,
    cycle: u64,
    started: u64,
    speed: i32,
    base_sequence: i32,
    scale: Option<(i32, i32)>,
}

mod overlay;
mod spots;
pub use overlay::{
    ARROW_SLOTS, HintArrowSpec, HintSet, HintTrailSpec, PlayerIdentity, TRAIL_SLOTS,
    arrow_dimension, arrow_rotation, trail_nodes,
};

mod hover;
pub use hover::InteractionColours;

#[cfg(test)]
mod tests;
