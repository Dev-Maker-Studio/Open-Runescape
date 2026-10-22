use crate::loc_size::{loc_mesh, rotate, sine_cosine};
use crate::loc_type::LocType;
use crate::locs::ScenePlacement;
use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;

pub const TILE_UNITS: i32 = 512;
pub const TILE_SCALE: i32 = 9;
const PLANES: usize = 4;
const DX: [i32; 4] = [1, 0, -1, 0];
const DY: [i32; 4] = [0, -1, 0, 1];
const ADX: [i32; 4] = [1, -1, -1, 1];
const ADY: [i32; 4] = [-1, -1, 1, 1];
const RADIANS_TO_ANGLE: f64 = 2_607.594_587_617_613_3;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum PlaceKind {
    Wall,
    Interactive,
    Decoration,
    Ground,
    Animated,
}

impl PlaceKind {
    pub fn code(self) -> char {
        match self {
            PlaceKind::Wall => 'W',
            PlaceKind::Interactive => 'I',
            PlaceKind::Decoration => 'D',
            PlaceKind::Ground => 'G',
            PlaceKind::Animated => 'A',
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct PlacedLoc {
    pub seq: i32,
    pub role: i32,
    pub plane: i32,
    pub x: i32,
    pub y: i32,
    pub loc_id: i32,
    pub shape: i32,
    pub rotation: i32,
    pub kind: PlaceKind,
    pub pos_x: i32,
    pub pos_y: i32,
    pub average_height: i32,
    pub scene_x: i32,
    pub scene_y: i32,
}

#[derive(Clone, Debug)]
pub struct SceneHeights {
    size_x: i32,
    size_y: i32,
    planes: [Vec<i32>; PLANES],
}

impl SceneHeights {
    pub fn new(size_x: i32, size_y: i32, planes: [Vec<i32>; PLANES]) -> Self {
        Self {
            size_x,
            size_y,
            planes,
        }
    }

    pub fn from_big_endian(size_x: i32, size_y: i32, bytes: &[u8]) -> Option<Self> {
        let per_plane = (size_x as usize + 1) * (size_y as usize + 1);
        if bytes.len() != per_plane * PLANES * 4 {
            return None;
        }
        let mut values = bytes
            .chunks_exact(4)
            .map(|chunk| i32::from_be_bytes([chunk[0], chunk[1], chunk[2], chunk[3]]));
        let mut planes: [Vec<i32>; PLANES] = Default::default();
        for plane in planes.iter_mut() {
            *plane = values.by_ref().take(per_plane).collect();
        }
        Some(Self::new(size_x, size_y, planes))
    }

    pub fn size_x(&self) -> i32 {
        self.size_x
    }

    pub fn size_y(&self) -> i32 {
        self.size_y
    }

    pub fn raw(&self, plane: usize, x: i32, y: i32) -> i32 {
        if x < 0 || y < 0 || x > self.size_x || y > self.size_y {
            return 0;
        }
        let index = x as usize * (self.size_y as usize + 1) + y as usize;
        self.planes
            .get(plane)
            .and_then(|heights| heights.get(index))
            .copied()
            .unwrap_or(0)
    }

    pub fn average(&self, plane: usize, x: i32, y: i32) -> i32 {
        let scaled_x = x >> TILE_SCALE;
        let scaled_y = y >> TILE_SCALE;
        if scaled_x >= 0 && scaled_y >= 0 && scaled_x < self.size_x && scaled_y < self.size_y {
            let offset_x = x & (TILE_UNITS - 1);
            let offset_y = y & (TILE_UNITS - 1);
            let height1 = (self
                .raw(plane, scaled_x, scaled_y)
                .wrapping_mul(TILE_UNITS - offset_x)
                .wrapping_add(offset_x.wrapping_mul(self.raw(plane, scaled_x + 1, scaled_y))))
                >> TILE_SCALE;
            let height2 = (self
                .raw(plane, scaled_x, scaled_y + 1)
                .wrapping_mul(TILE_UNITS - offset_x)
                .wrapping_add(
                    self.raw(plane, scaled_x + 1, scaled_y + 1)
                        .wrapping_mul(offset_x),
                ))
                >> TILE_SCALE;
            return (height2
                .wrapping_mul(offset_y)
                .wrapping_add(height1.wrapping_mul(TILE_UNITS - offset_y)))
                >> TILE_SCALE;
        }
        0
    }
}

#[derive(Default)]
struct Tile {
    wall_id: Option<i32>,
    primary: Option<usize>,
    secondary: Option<usize>,
}

struct Site {
    plane: i32,
    x: i32,
    y: i32,
    loc_id: i32,
    scene_x: i32,
    scene_y: i32,
    average_height: i32,
    stationary: bool,
    seq: i32,
}

struct Placer<'a, F: Fn(i32) -> Option<&'a LocType>> {
    types: F,
    out: Vec<PlacedLoc>,
    tiles: HashMap<(i32, i32, i32), Tile>,
    seq: i32,
}

impl<'a, F: Fn(i32) -> Option<&'a LocType>> Placer<'a, F> {
    #[allow(clippy::too_many_arguments)]
    fn make(
        &mut self,
        site: &Site,
        role: i32,
        kind: PlaceKind,
        shape: i32,
        rotation: i32,
        pos_x: i32,
        pos_y: i32,
    ) -> usize {
        self.out.push(PlacedLoc {
            seq: site.seq,
            role,
            plane: site.plane,
            x: site.x,
            y: site.y,
            loc_id: site.loc_id,
            shape,
            rotation,
            kind: if site.stationary {
                kind
            } else {
                PlaceKind::Animated
            },
            pos_x,
            pos_y,
            average_height: site.average_height,
            scene_x: site.scene_x,
            scene_y: site.scene_y,
        });
        self.out.len() - 1
    }

    fn tile(&mut self, site: &Site) -> &mut Tile {
        self.tiles.entry((site.plane, site.x, site.y)).or_default()
    }

    fn offset_decorations(&mut self, site: &Site, displacement: i32) {
        let Some(tile) = self.tiles.get(&(site.plane, site.x, site.y)) else {
            return;
        };
        let targets = [tile.primary, tile.secondary];
        for index in targets.into_iter().flatten() {
            let entity = &mut self.out[index];
            entity.pos_x = i32::from((displacement.wrapping_mul(entity.pos_x) / 64) as i16);
            entity.pos_y = i32::from((displacement.wrapping_mul(entity.pos_y) / 64) as i16);
        }
    }

    fn add_wall(&mut self, site: &Site, loc: &LocType, shape: i32, rotation: i32) -> bool {
        match shape {
            0 => {
                self.make(site, 0, PlaceKind::Wall, 0, rotation, 0, 0);
                self.tile(site).wall_id = Some(loc.id);
                if loc.decor_displacement != 64 {
                    self.offset_decorations(site, loc.decor_displacement);
                }
                true
            }
            1 | 3 => {
                self.make(site, 0, PlaceKind::Wall, shape, rotation, 0, 0);
                self.tile(site).wall_id = Some(loc.id);
                true
            }
            2 => {
                self.make(site, 0, PlaceKind::Wall, 2, rotation + 4, 0, 0);
                self.make(site, 1, PlaceKind::Wall, 2, (rotation + 1) & 3, 0, 0);
                self.tile(site).wall_id = Some(loc.id);
                if loc.decor_displacement != 64 {
                    self.offset_decorations(site, loc.decor_displacement);
                }
                true
            }
            9 => {
                self.make(site, 0, PlaceKind::Interactive, 9, rotation, 0, 0);
                self.tile(site);
                if loc.decor_displacement != 64 {
                    self.offset_decorations(site, loc.decor_displacement);
                }
                true
            }
            _ => false,
        }
    }

    fn wall_displacement(&self, site: &Site) -> Option<i32> {
        let wall_id = self.tiles.get(&(site.plane, site.x, site.y))?.wall_id?;
        (self.types)(wall_id).map(|wall| wall.decor_displacement)
    }

    fn add_wall_decoration(&mut self, site: &Site, shape: i32, rotation: i32) {
        let slots = decoration_slots_for(
            shape,
            rotation,
            self.wall_displacement(site),
            !site.stationary,
        );
        let mut made = slots.iter().map(|slot| {
            self.make(
                site,
                slot.role,
                PlaceKind::Decoration,
                shape,
                slot.rotation,
                slot.pos_x,
                slot.pos_y,
            )
        });
        let primary = made.next();
        let secondary = made.next();
        let Some(primary) = primary else {
            return;
        };
        let tile = self.tile(site);
        tile.primary = Some(primary);
        tile.secondary = secondary;
    }

    fn add_object(
        &mut self,
        placement: &ScenePlacement,
        sequence_id: i32,
        heights: &SceneHeights,
    ) {
        let Some(loc) = (self.types)(placement.id) else {
            return;
        };
        let (x, y) = (placement.scene_x, placement.scene_y);
        let plane = placement.plane.clamp(0, PLANES as i32 - 1) as usize;
        let (scene_x, scene_y, average_height) =
            footprint_site(loc, placement.rotation, x, y, plane, heights);
        self.seq += 1;
        let site = Site {
            plane: placement.plane,
            x,
            y,
            loc_id: placement.id,
            scene_x,
            scene_y,
            average_height,
            stationary: sequence_id == -1
                && loc.animations.is_none()
                && loc.alternative_ids.is_none()
                && !loc.has_animation
                && !loc.transforms,
            seq: self.seq,
        };
        let shape = placement.kind;
        let rotation = placement.rotation;
        if shape == 22 {
            self.make(&site, 0, PlaceKind::Ground, 22, rotation, 0, 0);
        } else if shape != 10 && shape != 11 {
            if !(12..=21).contains(&shape) {
                if !self.add_wall(&site, loc, shape, rotation) {
                    self.add_wall_decoration(&site, shape, rotation);
                }
            } else {
                self.make(&site, 0, PlaceKind::Interactive, shape, rotation, 0, 0);
            }
        } else {
            let rotation = if shape == 11 { rotation + 4 } else { rotation };
            self.make(&site, 0, PlaceKind::Interactive, 10, rotation, 0, 0);
        }
    }
}

pub fn place_scene<'a, F>(
    placements: &[ScenePlacement],
    heights: &SceneHeights,
    types: F,
) -> Vec<PlacedLoc>
where
    F: Fn(i32) -> Option<&'a LocType>,
{
    let sequences = vec![-1; placements.len()];
    place_scene_with_sequences(placements, &sequences, heights, types)
}

/// `place_scene` for placements that carry a server sequence id (`SceneGraph.addObject`'s
/// `sequenceId`); a placement with a sequence is never stationary. `sequences` pairs with
/// `placements` and a missing entry means -1.
pub fn place_scene_with_sequences<'a, F>(
    placements: &[ScenePlacement],
    sequences: &[i32],
    heights: &SceneHeights,
    types: F,
) -> Vec<PlacedLoc>
where
    F: Fn(i32) -> Option<&'a LocType>,
{
    let mut placer = Placer {
        types,
        out: Vec::new(),
        tiles: HashMap::new(),
        seq: 0,
    };
    for (index, placement) in placements.iter().enumerate() {
        let sequence_id = sequences.get(index).copied().unwrap_or(-1);
        placer.add_object(placement, sequence_id, heights);
    }
    placer.out
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct LocVertices {
    pub xs: Vec<i32>,
    pub ys: Vec<i32>,
    pub zs: Vec<i32>,
}

struct Base {
    vertices: LocVertices,
    depth: usize,
}

struct Bounds {
    min_x: i32,
    max_x: i32,
    min_y: i32,
    max_y: i32,
    min_z: i32,
    max_z: i32,
}

fn bounds(vertices: &LocVertices, depth: usize) -> Bounds {
    let mut result = Bounds {
        min_x: 32767,
        max_x: -32768,
        min_y: 32767,
        max_y: -32768,
        min_z: 32767,
        max_z: -32768,
    };
    for index in 0..depth {
        let (x, y, z) = (vertices.xs[index], vertices.ys[index], vertices.zs[index]);
        result.min_x = result.min_x.min(x);
        result.max_x = result.max_x.max(x);
        result.min_y = result.min_y.min(y);
        result.max_y = result.max_y.max(y);
        result.min_z = result.min_z.min(z);
        result.max_z = result.max_z.max(z);
    }
    for value in [
        &mut result.min_x,
        &mut result.max_x,
        &mut result.min_y,
        &mut result.max_y,
        &mut result.min_z,
        &mut result.max_z,
    ] {
        *value = i32::from(*value as i16);
    }
    result
}

fn translate(vertices: &mut LocVertices, depth: usize, dx: i32, dy: i32, dz: i32) {
    for index in 0..depth {
        vertices.xs[index] = vertices.xs[index].wrapping_add(dx);
        vertices.ys[index] = vertices.ys[index].wrapping_add(dy);
        vertices.zs[index] = vertices.zs[index].wrapping_add(dz);
    }
}

fn rotate_x_axis(vertices: &mut LocVertices, depth: usize, angle: usize) {
    let (sin, cos) = sine_cosine(angle);
    for index in 0..depth {
        let y = vertices.ys[index];
        let z = vertices.zs[index];
        vertices.ys[index] = cos.wrapping_mul(y).wrapping_sub(sin.wrapping_mul(z)) >> 14;
        vertices.zs[index] = sin.wrapping_mul(y).wrapping_add(cos.wrapping_mul(z)) >> 14;
    }
}

fn rotate_z_axis(vertices: &mut LocVertices, depth: usize, angle: usize) {
    let (sin, cos) = sine_cosine(angle);
    for index in 0..depth {
        let x = vertices.xs[index];
        let y = vertices.ys[index];
        vertices.xs[index] = cos.wrapping_mul(x).wrapping_add(sin.wrapping_mul(y)) >> 14;
        vertices.ys[index] = cos.wrapping_mul(y).wrapping_sub(sin.wrapping_mul(x)) >> 14;
    }
}

fn clamp_angle(mut rotation: i32, limit: i32) -> i32 {
    if limit != 0 {
        if rotation > 8192 {
            let inverse = 16384 - limit;
            if rotation < inverse {
                rotation = inverse;
            }
        } else if rotation > limit {
            rotation = limit;
        }
    }
    rotation
}

#[allow(clippy::too_many_arguments)]
fn hill_change(
    vertices: &mut LocVertices,
    depth: usize,
    heights: &SceneHeights,
    plane: usize,
    offset_x: i32,
    y: i32,
    offset_y: i32,
    width: i32,
    length: i32,
    xan: i32,
    zan: i32,
) {
    let south_west = heights.average(plane, -width / 2 + offset_x, -length / 2 + offset_y);
    let south_east = heights.average(plane, width / 2 + offset_x, -length / 2 + offset_y);
    let north_west = heights.average(plane, -width / 2 + offset_x, length / 2 + offset_y);
    let north_east = heights.average(plane, width / 2 + offset_x, length / 2 + offset_y);
    let lowest_south = south_west.min(south_east);
    let lowest_north = north_west.min(north_east);
    let lowest_east = south_east.min(north_east);
    let lowest_west = south_west.min(north_west);
    if length != 0 {
        let rotation = ((f64::from(lowest_south - lowest_north).atan2(f64::from(length))
            * RADIANS_TO_ANGLE) as i32)
            & 0x3fff;
        if rotation != 0 {
            rotate_x_axis(vertices, depth, clamp_angle(rotation, xan) as usize);
        }
    }
    if width != 0 {
        let rotation = ((f64::from(lowest_west - lowest_east).atan2(f64::from(width))
            * RADIANS_TO_ANGLE) as i32)
            & 0x3fff;
        if rotation != 0 {
            rotate_z_axis(vertices, depth, clamp_angle(rotation, zan) as usize);
        }
    }
    let mut height = south_west.wrapping_add(north_east);
    if south_east.wrapping_add(north_west) < height {
        height = south_east.wrapping_add(north_west);
    }
    height = (height >> 1).wrapping_sub(y);
    if height != 0 {
        translate(vertices, depth, 0, height, 0);
    }
}

#[allow(clippy::too_many_arguments)]
fn contour_to_ground(
    vertices: &mut LocVertices,
    depth: usize,
    contour_type: i32,
    modifier: i32,
    heights: &SceneHeights,
    plane: usize,
    scene_x: i32,
    average_height: i32,
    scene_y: i32,
) {
    let next_plane = plane + 1;
    let has_next = next_plane < PLANES;
    let extent = bounds(vertices, depth);
    let mut min_tile_x = scene_x.wrapping_add(extent.min_x);
    let mut max_tile_x = scene_x.wrapping_add(extent.max_x);
    let mut min_tile_y = scene_y.wrapping_add(extent.min_z);
    let mut max_tile_y = scene_y.wrapping_add(extent.max_z);
    let width = heights.size_x();
    let length = heights.size_y();
    let inside = min_tile_x >= 0
        && ((max_tile_x + TILE_UNITS) >> TILE_SCALE) < width
        && min_tile_y >= 0
        && ((max_tile_y + TILE_UNITS) >> TILE_SCALE) < length;
    if matches!(contour_type, 1 | 2 | 3 | 5) && !inside {
        return;
    }
    if contour_type != 4 && contour_type != 5 {
        min_tile_x >>= TILE_SCALE;
        max_tile_x = (max_tile_x + (TILE_UNITS - 1)) >> TILE_SCALE;
        min_tile_y >>= TILE_SCALE;
        max_tile_y = (max_tile_y + (TILE_UNITS - 1)) >> TILE_SCALE;
        if heights.raw(plane, min_tile_x, min_tile_y) == average_height
            && heights.raw(plane, max_tile_x, min_tile_y) == average_height
            && heights.raw(plane, min_tile_x, max_tile_y) == average_height
            && heights.raw(plane, max_tile_x, max_tile_y) == average_height
        {
            return;
        }
    } else if !has_next
        || min_tile_x < 0
        || ((TILE_UNITS + max_tile_x) >> TILE_SCALE) >= width
        || min_tile_y < 0
        || ((TILE_UNITS + max_tile_y) >> TILE_SCALE) >= length
    {
        return;
    }
    match contour_type {
        1 => {
            for index in 0..depth {
                let ground = heights.average(
                    plane,
                    vertices.xs[index].wrapping_add(scene_x),
                    vertices.zs[index].wrapping_add(scene_y),
                );
                vertices.ys[index] = vertices.ys[index]
                    .wrapping_add(ground)
                    .wrapping_sub(average_height);
            }
        }
        2 => {
            let min_y = extent.min_y;
            if min_y == 0 {
                return;
            }
            for index in 0..depth {
                let ratio = vertices.ys[index].wrapping_shl(16) / min_y;
                if ratio < modifier {
                    let ground = heights.average(
                        plane,
                        vertices.xs[index].wrapping_add(scene_x),
                        vertices.zs[index].wrapping_add(scene_y),
                    );
                    let delta = ground
                        .wrapping_sub(average_height)
                        .wrapping_mul(modifier - ratio)
                        .checked_div(modifier)
                        .unwrap_or(0);
                    vertices.ys[index] = vertices.ys[index].wrapping_add(delta);
                }
            }
        }
        3 => {
            let width_units = (modifier & 0xff) * 4;
            let length_units = ((modifier >> 8) & 0xff) * 4;
            let xan = ((modifier >> 16) & 0xff) << 6;
            let zan = ((modifier >> 24) & 0xff) << 6;
            if scene_x - (width_units >> 1) < 0
                || scene_x + TILE_UNITS + (width_units >> 1) >= (width << TILE_SCALE)
                || scene_y - (length_units >> 1) < 0
                || TILE_UNITS + (length_units >> 1) + scene_y >= (length << TILE_SCALE)
            {
                return;
            }
            hill_change(
                vertices,
                depth,
                heights,
                plane,
                scene_x,
                average_height,
                scene_y,
                width_units,
                length_units,
                xan,
                zan,
            );
        }
        4 => {
            let size_y = extent.max_y - extent.min_y;
            for index in 0..depth {
                let ground = heights.average(
                    next_plane,
                    vertices.xs[index].wrapping_add(scene_x),
                    vertices.zs[index].wrapping_add(scene_y),
                );
                vertices.ys[index] = vertices.ys[index]
                    .wrapping_add(ground.wrapping_sub(average_height))
                    .wrapping_add(size_y);
            }
        }
        5 => {
            let size_y = extent.max_y - extent.min_y;
            for index in 0..depth {
                let x = scene_x.wrapping_add(vertices.xs[index]);
                let z = scene_y.wrapping_add(vertices.zs[index]);
                let current = heights.average(plane, x, z);
                let next = heights.average(next_plane, x, z);
                let delta = current.wrapping_sub(next).wrapping_sub(modifier);
                let scaled = vertices.ys[index]
                    .wrapping_shl(8)
                    .checked_div(size_y)
                    .unwrap_or(0);
                vertices.ys[index] = (delta.wrapping_mul(scaled) >> 8)
                    .wrapping_sub(average_height.wrapping_sub(current));
            }
        }
        _ => {}
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct DecorSlot {
    pub role: i32,
    pub rotation: i32,
    pub pos_x: i32,
    pub pos_y: i32,
}

pub fn decoration_slots(shape: i32, rotation: i32, wall: Option<i32>) -> Vec<DecorSlot> {
    decoration_slots_for(shape, rotation, wall, false)
}

/// `animated` selects the animated wall decoration tables: `SceneGraph.addWallDecoration`
/// displaces an animated shape 6 along the diagonal (ADX/ADY) where a stationary one uses DX/DY.
pub fn decoration_slots_for(
    shape: i32,
    rotation: i32,
    wall: Option<i32>,
    animated: bool,
) -> Vec<DecorSlot> {
    let r = (rotation & 3) as usize;
    let slot = |role, rotation, pos_x, pos_y| DecorSlot {
        role,
        rotation,
        pos_x,
        pos_y,
    };
    match shape {
        4 => vec![slot(0, rotation, 0, 0)],
        5 => {
            let d = wall.map_or(65, |value| value + 1);
            vec![slot(0, rotation, d * DX[r], d * DY[r])]
        }
        6 => {
            let d = wall.map_or(33, |value| value / 2 + 1);
            let (dx, dy) = if animated {
                (ADX[r], ADY[r])
            } else {
                (DX[r], DY[r])
            };
            vec![slot(0, rotation + 4, d * dx, d * dy)]
        }
        7 => vec![slot(0, ((rotation + 2) & 3) + 4, 0, 0)],
        8 => {
            let d = wall.map_or(33, |value| value / 2 + 1);
            vec![
                slot(0, rotation + 4, d * ADX[r], d * ADY[r]),
                slot(1, ((rotation + 2) & 3) + 4, 0, 0),
            ]
        }
        _ => Vec::new(),
    }
}

pub fn footprint_site(
    loc: &LocType,
    rotation: i32,
    x: i32,
    y: i32,
    plane: usize,
    heights: &SceneHeights,
) -> (i32, i32, i32) {
    let (mut size_x, mut size_y) = (loc.size_x, loc.size_y);
    if rotation == 1 || rotation == 3 {
        size_x = loc.size_y;
        size_y = loc.size_x;
    }
    let (x_a, x_b) = if size_x + x <= heights.size_x() {
        ((size_x >> 1) + x, ((size_x + 1) >> 1) + x)
    } else {
        (x, x + 1)
    };
    let (y_a, y_b) = if size_y + y <= heights.size_y() {
        (y + (size_y >> 1), y + ((size_y + 1) >> 1))
    } else {
        (y, y + 1)
    };
    let sum = heights.raw(plane, x_a, y_a)
        + heights.raw(plane, x_b, y_a)
        + heights.raw(plane, x_a, y_b)
        + heights.raw(plane, x_b, y_b);
    ((x << 9) + (size_x << 8), (y << 9) + (size_y << 8), sum >> 2)
}

pub fn mapped_shape(shape: i32) -> i32 {
    if (4..=8).contains(&shape) {
        4
    } else {
        shape
    }
}

pub struct Orientation<'a> {
    pub loc: &'a LocType,
    pub shape: i32,
    pub rotation: i32,
    pub plane: usize,
    pub scene_x: i32,
    pub scene_y: i32,
    pub average_height: i32,
}

pub fn orient(
    orientation: &Orientation<'_>,
    vertices: LocVertices,
    depth: usize,
    heights: &SceneHeights,
) -> LocVertices {
    let vertices = orient_base(orientation, vertices, depth);
    orient_finish(orientation, vertices, depth, heights, false)
}

/// `getStationaryModel`: mirror, shape 4 pre-rotation, rotation, recolour scale and offset.
/// An animated loc applies its frame between this and `orient_finish`.
pub fn orient_base(
    orientation: &Orientation<'_>,
    mut vertices: LocVertices,
    depth: usize,
) -> LocVertices {
    let Orientation { loc, rotation, .. } = *orientation;
    let shape = mapped_shape(orientation.shape);
    let depth = depth
        .min(vertices.xs.len())
        .min(vertices.ys.len())
        .min(vertices.zs.len());
    if loc.inverted || (shape == 2 && rotation > 3) {
        for z in vertices.zs.iter_mut().take(depth) {
            *z = z.wrapping_neg();
        }
    }
    if shape == 4 && rotation > 3 {
        rotate(&mut vertices.xs[..depth], &mut vertices.zs[..depth], 2048);
        translate(&mut vertices, depth, 180, 0, -180);
    }
    match rotation & 3 {
        1 => rotate(&mut vertices.xs[..depth], &mut vertices.zs[..depth], 4096),
        2 => rotate(&mut vertices.xs[..depth], &mut vertices.zs[..depth], 8192),
        3 => rotate(&mut vertices.xs[..depth], &mut vertices.zs[..depth], 12288),
        _ => {}
    }
    if loc.resize_x != 128 || loc.resize_y != 128 || loc.resize_z != 128 {
        for index in 0..depth {
            vertices.xs[index] = vertices.xs[index].wrapping_mul(loc.resize_x) >> 7;
            vertices.ys[index] = vertices.ys[index].wrapping_mul(loc.resize_y) >> 7;
            vertices.zs[index] = vertices.zs[index].wrapping_mul(loc.resize_z) >> 7;
        }
    }
    if loc.offset_x != 0 || loc.offset_y != 0 || loc.offset_z != 0 {
        translate(
            &mut vertices,
            depth,
            loc.offset_x,
            loc.offset_y,
            loc.offset_z,
        );
    }
    vertices
}

/// The tail of `ObjectType.getPair`/`getAnimatedModel`: shape 10 pre-rotation, ground contour
/// and the shadow offset. The animated path contours without the `modifier < 16384` test.
pub fn orient_finish(
    orientation: &Orientation<'_>,
    mut vertices: LocVertices,
    depth: usize,
    heights: &SceneHeights,
    animated: bool,
) -> LocVertices {
    let Orientation {
        loc,
        rotation,
        plane,
        ..
    } = *orientation;
    let shape = mapped_shape(orientation.shape);
    let depth = depth
        .min(vertices.xs.len())
        .min(vertices.ys.len())
        .min(vertices.zs.len());
    if shape == 10 && rotation > 3 {
        rotate(&mut vertices.xs[..depth], &mut vertices.zs[..depth], 2048);
    }
    if loc.ground_contour_type != 0 && (animated || loc.ground_contour_modifier < 16384) {
        contour_to_ground(
            &mut vertices,
            depth,
            i32::from(loc.ground_contour_type),
            loc.ground_contour_modifier,
            heights,
            plane,
            orientation.scene_x,
            orientation.average_height,
            orientation.scene_y,
        );
    }
    if loc.shadow_offset_x != 0 || loc.shadow_offset_y != 0 || loc.shadow_offset_z != 0 {
        translate(
            &mut vertices,
            depth,
            loc.shadow_offset_x,
            loc.shadow_offset_y,
            loc.shadow_offset_z,
        );
    }
    vertices
}

pub struct VertexBuilder<'a, M: Fn(i32) -> Option<&'a [u8]>> {
    models: M,
    cache: RefCell<BaseCache>,
}

type BaseCache = HashMap<(i32, i32), Option<Rc<Base>>>;

impl<'a, M: Fn(i32) -> Option<&'a [u8]>> VertexBuilder<'a, M> {
    pub fn new(models: M) -> Self {
        Self {
            models,
            cache: RefCell::new(HashMap::new()),
        }
    }

    fn base(&self, loc: &LocType, shape: i32) -> Option<Rc<Base>> {
        let key = (loc.id, shape);
        if let Some(found) = self.cache.borrow().get(&key) {
            return found.clone();
        }
        let built = loc_mesh(loc, shape, &self.models).and_then(|mesh| {
            let count = usize::try_from(mesh.num_vertices).ok()?;
            let take = |array: &Option<Vec<i32>>| {
                let mut values = array.clone()?;
                values.resize(count, 0);
                Some(values)
            };
            let vertices = LocVertices {
                xs: take(&mesh.vertices_x)?,
                ys: take(&mesh.vertices_y)?,
                zs: take(&mesh.vertices_z)?,
            };
            let depth = usize::try_from(mesh.max_depth).ok()?.min(count);
            Some(Rc::new(Base { vertices, depth }))
        });
        self.cache.borrow_mut().insert(key, built.clone());
        built
    }

    pub fn build(
        &self,
        loc: &LocType,
        placed: &PlacedLoc,
        heights: &SceneHeights,
    ) -> Option<LocVertices> {
        let base = self.base(loc, mapped_shape(placed.shape))?;
        Some(orient(
            &Orientation {
                loc,
                shape: placed.shape,
                rotation: placed.rotation,
                plane: placed.plane.clamp(0, PLANES as i32 - 1) as usize,
                scene_x: placed.scene_x,
                scene_y: placed.scene_y,
                average_height: placed.average_height,
            },
            base.vertices.clone(),
            base.depth,
            heights,
        ))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn animated_shape_six_uses_the_diagonal_table() {
        for rotation in 0..4 {
            let still = decoration_slots_for(6, rotation, Some(32), false);
            let moving = decoration_slots_for(6, rotation, Some(32), true);
            let r = rotation as usize;
            assert_eq!(still[0].pos_x, 17 * DX[r]);
            assert_eq!(moving[0].pos_x, 17 * ADX[r]);
            assert_eq!(moving[0].pos_y, 17 * ADY[r]);
            assert_eq!(still[0].rotation, moving[0].rotation);
        }
    }

    #[test]
    fn only_shapes_five_to_eight_depend_on_the_wall() {
        assert_eq!(
            decoration_slots_for(4, 1, Some(10), false),
            decoration_slots_for(4, 1, None, false)
        );
        assert_ne!(
            decoration_slots_for(5, 1, Some(10), false),
            decoration_slots_for(5, 1, None, false)
        );
        assert_eq!(
            decoration_slots_for(7, 2, Some(10), true),
            decoration_slots_for(7, 2, None, false)
        );
    }
}
