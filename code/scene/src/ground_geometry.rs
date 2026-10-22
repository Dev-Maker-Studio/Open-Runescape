mod tables;

use crate::floor::{blend_hsl, Overlay, Underlay};
use crate::palette::PaletteCell;
use crate::surface_tiles::SurfaceTile;
use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};
use tables::*;

pub const MAX_DIMENSION: usize = 168;
pub const MAX_VERTICES_PER_TILE: usize = 36;
pub const MAX_OUTPUT_VERTICES: usize = MAX_DIMENSION * MAX_DIMENSION * MAX_VERTICES_PER_TILE;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct GroundGeometryOptions {
    pub overlay_hidden: bool,
    pub secondary_ground_heights: bool,
    pub max_vertices: usize,
}

impl GroundGeometryOptions {
    pub fn surface(max_vertices: usize) -> Self {
        Self {
            overlay_hidden: false,
            secondary_ground_heights: false,
            max_vertices,
        }
    }
}

#[derive(Clone, Copy, Debug)]
pub struct GroundGeometryInput<'a> {
    pub width: usize,
    pub height: usize,
    pub tiles: &'a [Option<SurfaceTile>],
    pub ground_heights: &'a [Option<i32>],
    pub palette: &'a [PaletteCell],
    pub underlays: &'a [Option<Underlay>],
    pub overlays: &'a [Option<Overlay>],
    pub overlay_render_colors: &'a [Option<i32>],
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum GroundGeometryError {
    UnsupportedUnderwater,
    UnsupportedSecondaryGround,
    Dimensions {
        width: usize,
        height: usize,
    },
    InputLength {
        input: &'static str,
        expected: usize,
        actual: usize,
    },
    UnavailableTile {
        x: usize,
        y: usize,
    },
    UnavailableHeight {
        x: usize,
        y: usize,
    },
    UnavailablePalette {
        x: usize,
        y: usize,
    },
    InvalidShape {
        x: usize,
        y: usize,
        shape: u8,
    },
    InvalidRotation {
        x: usize,
        y: usize,
        rotation: u8,
    },
    UnavailableUnderlay {
        definition_id: u8,
    },
    UnavailableOverlay {
        definition_id: u8,
    },
    UnavailableOverlayRenderColor {
        definition_id: u8,
    },
    InvalidScale {
        overlay: bool,
        definition_id: u8,
        scale: u32,
    },
    VertexBudget {
        required: usize,
        limit: usize,
    },
}

impl Display for GroundGeometryError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        write!(f, "ground geometry: {self:?}")
    }
}

impl Error for GroundGeometryError {}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct GroundVertex {
    pub offset_x: i32,
    pub offset_z: i32,
    pub main_color: i32,
    pub texture: i32,
    pub texture_scale: i32,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct BlendedGroundTile {
    pub x: usize,
    pub y: usize,
    pub shape: u8,
    pub rotation: u8,
    pub split_edges: [bool; 4],
    pub vertices: Box<[GroundVertex]>,
    pub blending_colors: Option<Box<[i32]>>,
    pub emitted_vertices: usize,
    pub has_shadows: bool,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct GroundGeometry {
    pub tiles: Box<[BlendedGroundTile]>,
    pub vertex_count: usize,
}

pub fn build_blended_ground(
    input: GroundGeometryInput<'_>,
    options: GroundGeometryOptions,
) -> Result<GroundGeometry, GroundGeometryError> {
    validate(&input, options)?;
    let mut result = GroundGeometry {
        tiles: Box::new([]),
        vertex_count: 0,
    };
    let mut tiles = Vec::new();
    for x in 0..input.width {
        for y in 0..input.height {
            let raw = input.tile(x, y);
            if raw.overlay_id == 0 && raw.underlay_id == 0 {
                continue;
            }
            let mut builder = TileBuilder::new(&input, x, y);
            builder.prepare();
            let face_count = builder.layout.overlay_count + builder.layout.underlay_count;
            if face_count == 0 {
                continue;
            }
            let vertex_count =
                (face_count + builder.splits.iter().filter(|&&split| split).count()) * 3;
            let required = result.vertex_count + vertex_count;
            if required > options.max_vertices {
                return Err(GroundGeometryError::VertexBudget {
                    required,
                    limit: options.max_vertices,
                });
            }
            tiles.push(builder.emit(vertex_count));
            result.vertex_count = required;
        }
    }
    result.tiles = tiles.into_boxed_slice();
    Ok(result)
}

fn validate(
    input: &GroundGeometryInput<'_>,
    options: GroundGeometryOptions,
) -> Result<(), GroundGeometryError> {
    if options.overlay_hidden {
        return Err(GroundGeometryError::UnsupportedUnderwater);
    }
    if options.secondary_ground_heights {
        return Err(GroundGeometryError::UnsupportedSecondaryGround);
    }
    if input.width == 0
        || input.height == 0
        || input.width > MAX_DIMENSION
        || input.height > MAX_DIMENSION
    {
        return Err(GroundGeometryError::Dimensions {
            width: input.width,
            height: input.height,
        });
    }
    for (name, expected, actual) in [
        ("tiles", input.width * input.height, input.tiles.len()),
        ("palette", input.width * input.height, input.palette.len()),
        (
            "ground_heights",
            (input.width + 1) * (input.height + 1),
            input.ground_heights.len(),
        ),
    ] {
        if expected != actual {
            return Err(GroundGeometryError::InputLength {
                input: name,
                expected,
                actual,
            });
        }
    }
    for x in 0..=input.width {
        for y in 0..=input.height {
            if input.ground_heights[x * (input.height + 1) + y].is_none() {
                return Err(GroundGeometryError::UnavailableHeight { x, y });
            }
        }
    }
    for x in 0..input.width {
        for y in 0..input.height {
            let tile = input.tiles[x * input.height + y]
                .ok_or(GroundGeometryError::UnavailableTile { x, y })?;
            if tile.overlay_shape > 12 {
                return Err(GroundGeometryError::InvalidShape {
                    x,
                    y,
                    shape: tile.overlay_shape,
                });
            }
            if tile.overlay_rotation > 3 {
                return Err(GroundGeometryError::InvalidRotation {
                    x,
                    y,
                    rotation: tile.overlay_rotation,
                });
            }
            if matches!(
                input.palette[x * input.height + y],
                PaletteCell::Unavailable
            ) {
                return Err(GroundGeometryError::UnavailablePalette { x, y });
            }
            if let Some(id) = tile.underlay_id.checked_sub(1) {
                let underlay = input
                    .underlays
                    .get(usize::from(id))
                    .and_then(Option::as_ref)
                    .ok_or(GroundGeometryError::UnavailableUnderlay { definition_id: id })?;
                if underlay.scale > i32::MAX as u32 {
                    return Err(GroundGeometryError::InvalidScale {
                        overlay: false,
                        definition_id: id,
                        scale: underlay.scale,
                    });
                }
            }
            if let Some(id) = tile.overlay_id.checked_sub(1) {
                let overlay = input
                    .overlays
                    .get(usize::from(id))
                    .and_then(Option::as_ref)
                    .ok_or(GroundGeometryError::UnavailableOverlay { definition_id: id })?;
                if overlay.texture_scale > i32::MAX as u32 {
                    return Err(GroundGeometryError::InvalidScale {
                        overlay: true,
                        definition_id: id,
                        scale: overlay.texture_scale,
                    });
                }
                if input
                    .overlay_render_colors
                    .get(usize::from(id))
                    .and_then(Option::as_ref)
                    .is_none()
                {
                    return Err(GroundGeometryError::UnavailableOverlayRenderColor {
                        definition_id: id,
                    });
                }
            }
        }
    }
    Ok(())
}

impl GroundGeometryInput<'_> {
    fn tile(&self, x: usize, y: usize) -> SurfaceTile {
        self.tiles[x * self.height + y].unwrap()
    }

    fn height_at(&self, x: usize, y: usize) -> i32 {
        self.ground_heights[x * (self.height + 1) + y].unwrap()
    }

    fn color(&self, x: usize, y: usize) -> i32 {
        match self.palette[x * self.height + y] {
            PaletteCell::Known(color) => color,
            PaletteCell::Unavailable => unreachable!(),
        }
    }

    fn underlay(&self, id: u8) -> Option<&Underlay> {
        id.checked_sub(1)
            .map(|id| self.underlays[usize::from(id)].as_ref().unwrap())
    }

    fn rendered_color(&self, id: u8) -> i32 {
        self.overlay_render_colors[usize::from(id - 1)].unwrap()
    }
}

#[derive(Clone, Copy, Default)]
struct Slot {
    priority: i32,
    color: i32,
    rendered: i32,
    texture: i32,
    scale: i32,
    mask: i32,
}

#[derive(Clone, Copy, Default)]
struct Layout {
    a: &'static [usize],
    b: &'static [usize],
    c: &'static [usize],
    mapping: Option<&'static [i32]>,
    overlay_count: usize,
    overlay_skip: usize,
    underlay_count: usize,
}

struct TileBuilder<'a, 'b> {
    input: &'b GroundGeometryInput<'a>,
    x: usize,
    y: usize,
    next_x: usize,
    next_y: usize,
    raw: SurfaceTile,
    overlay: Option<&'a Overlay>,
    underlay: Option<&'a Underlay>,
    shape: usize,
    rotation: usize,
    supports_blending: bool,
    multi_color: bool,
    slots: [Slot; 8],
    splits: [bool; 4],
    layout: Layout,
}

impl<'a, 'b> TileBuilder<'a, 'b> {
    fn new(input: &'b GroundGeometryInput<'a>, x: usize, y: usize) -> Self {
        let raw = input.tile(x, y);
        Self {
            input,
            x,
            y,
            next_x: (x + 1).min(input.width - 1),
            next_y: (y + 1).min(input.height - 1),
            raw,
            overlay: raw
                .overlay_id
                .checked_sub(1)
                .map(|id| input.overlays[usize::from(id)].as_ref().unwrap()),
            underlay: raw
                .underlay_id
                .checked_sub(1)
                .map(|id| input.underlays[usize::from(id)].as_ref().unwrap()),
            shape: usize::from(raw.overlay_shape),
            rotation: usize::from(raw.overlay_rotation),
            supports_blending: false,
            multi_color: false,
            slots: [Slot {
                priority: -1,
                mask: 1,
                ..Slot::default()
            }; 8],
            splits: [false; 4],
            layout: Layout::default(),
        }
    }

    fn prepare(&mut self) {
        if self.shape == 0 && self.overlay.is_none() {
            self.shape = 12;
        }
        if let Some(overlay) = self.overlay {
            if overlay.tile_color_hsl == -1 && overlay.minimap_color_hsl == -1 {
                self.overlay = None;
            } else if self.underlay.is_some() && self.shape != 0 {
                self.supports_blending = overlay.blends_with_underlay;
            }
        }
        self.rotate_underlay();
        self.neighbors();
        self.resolve_edges();
        self.select_layout();
    }

    fn rotate_underlay(&mut self) {
        if !matches!(self.shape, 0 | 12) || self.x == 0 || self.y == 0 {
            return;
        }
        let same = |x, y| {
            if i32::from(self.input.tile(x, y).underlay_id as i8) == i32::from(self.raw.underlay_id)
            {
                1_i32
            } else {
                -1_i32
            }
        };
        let north = same(self.x, self.y - 1);
        let east = same(self.next_x, self.y);
        let south = same(self.x, self.next_y);
        let west = same(self.x - 1, self.y);
        let sw = same(self.x - 1, self.y - 1) + north + west;
        let se = same(self.next_x, self.y - 1) + north + east;
        let ne = same(self.next_x, self.next_y) + east + south;
        let nw = same(self.x - 1, self.next_y) + south + west;
        let mut a = (sw - ne).abs();
        let mut b = (se - nw).abs();
        if a == b {
            a = self
                .input
                .height_at(self.x, self.y)
                .wrapping_sub(self.input.height_at(self.next_x, self.next_y))
                .wrapping_abs();
            b = self
                .input
                .height_at(self.next_x, self.y)
                .wrapping_sub(self.input.height_at(self.x, self.next_y))
                .wrapping_abs();
        }
        self.rotation = usize::from(a < b);
    }

    fn neighbor(&self, x: isize, y: isize) -> Option<(SurfaceTile, &'a Overlay)> {
        if x < 0 || y < 0 || x >= self.input.width as isize || y >= self.input.height as isize {
            return None;
        }
        let tile = self.input.tile(x as usize, y as usize);
        tile.overlay_id
            .checked_sub(1)
            .map(|id| (tile, self.input.overlays[usize::from(id)].as_ref().unwrap()))
    }

    fn assign(
        &mut self,
        slot: usize,
        tile: SurfaceTile,
        overlay: &Overlay,
        mask: i32,
        merge: bool,
    ) {
        let previous = self.slots[slot];
        self.slots[slot] = Slot {
            priority: overlay.slot,
            color: overlay.tile_color_hsl,
            rendered: self.input.rendered_color(tile.overlay_id),
            texture: overlay.texture,
            scale: overlay.texture_scale as i32,
            mask: if merge && previous.priority == overlay.slot {
                previous.mask | mask
            } else {
                mask
            },
        };
    }

    fn neighbors(&mut self) {
        let x = self.x as isize;
        let y = self.y as isize;
        for (dx, dy, slot, rotation_add, mask) in [
            (-1, -1, 0, 4, 256),
            (1, -1, 2, 6, 512),
            (-1, 1, 6, 2, 64),
            (1, 1, 4, 0, 128),
        ] {
            if let Some((tile, overlay)) = self.neighbor(x + dx, y + dy) {
                let corner = (usize::from(tile.overlay_rotation) * 2 + rotation_add) & 7;
                if overlay.tile_color_hsl != -1
                    && overlay.blends_with_underlay
                    && OVERLAY_SHAPE_SUPPORTS_HEIGHT[usize::from(tile.overlay_shape)][corner]
                {
                    self.assign(slot, tile, overlay, mask, false);
                }
            }
        }
        let blending = if self.overlay.is_some_and(|o| o.blends_with_underlay) {
            PATH_BLENDINGS[self.shape]
        } else {
            PATH_SQUARES[self.shape]
        };
        for (dx, dy, target, source_add, direction, neighbor_edge, step, mask) in [
            (0, -1, 2_i32, 4, 0, 2, 1_i32, 32),
            (0, 1, 4, 2, 2, 0, -1, 16),
            (-1, 0, 6, 4, 3, 1, -1, 8),
            (1, 0, 4, 6, 1, 3, 1, 4),
        ] {
            if let Some((tile, overlay)) = self.neighbor(x + dx, y + dy) {
                if overlay.tile_color_hsl == -1 {
                    continue;
                }
                let shape = usize::from(tile.overlay_shape);
                let rotation = usize::from(tile.overlay_rotation);
                if overlay.blends_with_underlay {
                    for corner in 0..3_i32 {
                        let source =
                            ((rotation as i32 * 2 + source_add + step * corner) & 7) as usize;
                        let dest = ((target - step * corner) & 7) as usize;
                        if OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][source]
                            && self.slots[dest].priority <= overlay.slot
                        {
                            self.assign(dest, tile, overlay, mask, true);
                        }
                    }
                }
                if !blending[(self.rotation + direction) & 3] {
                    self.splits[direction] = if overlay.blends_with_underlay {
                        PATH_BLENDINGS[shape][(rotation + neighbor_edge) & 3]
                    } else {
                        PATH_SQUARES[shape][(rotation + neighbor_edge) & 3]
                    };
                }
            }
        }
        if let Some(overlay) = self.overlay.filter(|o| o.blends_with_underlay) {
            for (corner, &supports_height) in OVERLAY_SHAPE_SUPPORTS_HEIGHT[self.shape]
                .iter()
                .take(8)
                .enumerate()
            {
                let slot = (corner + 8 - self.rotation * 2) & 7;
                if supports_height && self.slots[slot].priority <= overlay.slot {
                    self.assign(slot, self.raw, overlay, 2, true);
                }
            }
        }
    }

    fn resolve_edges(&mut self) {
        self.multi_color = self
            .overlay
            .is_some_and(|o| o.tile_color_hsl != o.minimap_color_hsl)
            || self
                .slots
                .iter()
                .any(|slot| slot.priority >= 0 && slot.rendered != slot.color);
        let blendings = if self.overlay.is_some_and(|o| o.blends_with_underlay) {
            PATH_BLENDINGS[self.shape]
        } else {
            PATH_SQUARES[self.shape]
        };
        for (edge, a, b) in [(1, 2, 4), (3, 6, 0), (0, 0, 2), (2, 4, 6)] {
            if !blendings[(self.rotation + edge) & 3] {
                self.splits[edge] |= (self.slots[a].mask & self.slots[b].mask) == 0;
            }
        }
        if !self.supports_blending && matches!(self.shape, 0 | 12) {
            let rotation = match self.splits {
                [true, false, false, true] => Some(0),
                [true, true, false, false] => Some(3),
                [false, true, true, false] => Some(2),
                [false, false, true, true] => Some(1),
                _ => None,
            };
            if let Some(rotation) = rotation {
                self.splits = [false; 4];
                self.shape = if self.shape == 0 { 13 } else { 14 };
                self.rotation = rotation;
            }
        }
    }

    fn select_layout(&mut self) {
        let shape = self.shape;
        let (a, b, c, mapping, overlays, underlays) =
            if !self.supports_blending && !self.splits.iter().any(|&split| split) {
                (
                    SHAPE_VERTEX_A,
                    SHAPE_VERTEX_B,
                    SHAPE_VERTEX_C,
                    None,
                    OVERLAY_FACE_COUNT.as_slice(),
                    UNDERLAY_FACE_COUNT.as_slice(),
                )
            } else if self.supports_blending {
                (
                    BLENDING_SHAPE_VERTEX_A,
                    BLENDING_SHAPE_VERTEX_B,
                    BLENDING_SHAPE_VERTEX_C,
                    Some(BLENDING_FACE_MAPPING),
                    OVERLAY_FACE_COUNT_BLENDED.as_slice(),
                    UNDERLAY_FACE_COUNT_BLENDED.as_slice(),
                )
            } else {
                (
                    BLENDED_SHAPE_VERTEX_A,
                    BLENDED_SHAPE_VERTEX_B,
                    BLENDED_SHAPE_VERTEX_C,
                    Some(BLENDED_FACE_MAPPING),
                    OVERLAY_FACE_COUNTS.as_slice(),
                    UNDERLAY_FACE_COUNTS.as_slice(),
                )
            };
        self.layout = Layout {
            a: a[shape],
            b: b[shape],
            c: c[shape],
            mapping: mapping.map(|mapping| mapping[shape]),
            overlay_count: if self.overlay.is_some() {
                overlays[shape]
            } else {
                0
            },
            overlay_skip: overlays[shape],
            underlay_count: if self.underlay.is_some() {
                underlays[shape]
            } else {
                0
            },
        };
    }

    fn face_vertices(&self, face: usize) -> ([usize; 6], usize) {
        let a = self.layout.a[face];
        let b = self.layout.b[face];
        let c = self.layout.c[face];
        if let Some(mapping) = self.layout.mapping {
            for (edge, middle) in [(0, 1), (2, 5), (1, 3), (3, 7)] {
                if self.splits[(edge + 4 - self.rotation) & 3] && mapping[edge] == face as i32 {
                    return ([a, middle, c, middle, b, c], 6);
                }
            }
        }
        ([a, b, c, 0, 0, 0], 3)
    }

    fn offsets(&self, vertex: usize) -> (i32, i32) {
        let x = TILE_SIZE_DELTAS_X[vertex] as i32;
        let y = TILE_SIZE_DELTAS_Y[vertex] as i32;
        match self.rotation {
            1 => (y, 512 - x),
            2 => (512 - x, 512 - y),
            3 => (512 - y, x),
            _ => (x, y),
        }
    }

    fn emit(&self, vertex_count: usize) -> BlendedGroundTile {
        let mut result = BlendedGroundTile {
            x: self.x,
            y: self.y,
            shape: self.shape as u8,
            rotation: self.rotation as u8,
            split_edges: self.splits,
            vertices: vec![GroundVertex::default(); vertex_count].into_boxed_slice(),
            blending_colors: self
                .multi_color
                .then(|| vec![0; vertex_count].into_boxed_slice()),
            emitted_vertices: 0,
            has_shadows: false,
        };
        if let Some(overlay) = self.overlay {
            for face in 0..self.layout.overlay_count {
                let (vertices, count) = self.face_vertices(face);
                for &vertex in &vertices[..count] {
                    let (x, z) = self.offsets(vertex);
                    let slot = self.slots[(vertex + 8 - self.rotation * 2) & 7];
                    let (color, rendered, texture, scale) =
                        if vertex < 8 && slot.priority > overlay.slot {
                            (slot.color, slot.rendered, slot.texture, slot.scale)
                        } else {
                            (
                                overlay.tile_color_hsl,
                                self.input.rendered_color(self.raw.overlay_id),
                                overlay.texture,
                                overlay.texture_scale as i32,
                            )
                        };
                    result.push(
                        GroundVertex {
                            offset_x: x,
                            offset_z: z,
                            main_color: color,
                            texture,
                            texture_scale: scale,
                        },
                        rendered,
                    );
                }
            }
            result.has_shadows =
                self.shape != 12 && overlay.tile_color_hsl != -1 && overlay.shadowed;
        }
        if let Some(underlay) = self.underlay {
            for face in
                self.layout.overlay_skip..self.layout.overlay_skip + self.layout.underlay_count
            {
                let (vertices, count) = self.face_vertices(face);
                for &vertex in &vertices[..count] {
                    let (x, z) = self.offsets(vertex);
                    let slot = self.slots[(vertex + 8 - self.rotation * 2) & 7];
                    let (color, rendered, texture, scale) = if vertex < 8 && slot.priority >= 0 {
                        (slot.color, slot.rendered, slot.texture, slot.scale)
                    } else {
                        let (color, texture, scale) = self.underlay_vertex(vertex, x, z);
                        (color, color, texture, scale)
                    };
                    result.push(
                        GroundVertex {
                            offset_x: x,
                            offset_z: z,
                            main_color: color,
                            texture,
                            texture_scale: scale,
                        },
                        rendered,
                    );
                }
            }
            result.has_shadows |= self.shape != 0 && underlay.shadowed;
        }
        result
    }

    fn underlay_vertex(&self, vertex: usize, x: i32, z: i32) -> (i32, i32, i32) {
        if self.supports_blending && OVERLAY_SHAPE_SUPPORTS_HEIGHT[self.shape][vertex] {
            let overlay = self.overlay.unwrap();
            return (
                overlay.tile_color_hsl,
                overlay.texture,
                overlay.texture_scale as i32,
            );
        }
        let corner = match (x < 256, z < 256) {
            (true, true) => (self.x, self.y),
            (true, false) => (self.x, self.next_y),
            (false, false) => (self.next_x, self.next_y),
            (false, true) => (self.next_x, self.y),
        };
        let id = self.input.tile(corner.0, corner.1).underlay_id;
        let underlay = self
            .input
            .underlay(if id == 0 { self.raw.underlay_id } else { id })
            .unwrap();
        let color = match (x, z) {
            (0, 0) => self.input.color(self.x, self.y),
            (0, 512) => self.input.color(self.x, self.next_y),
            (512, 512) => self.input.color(self.next_x, self.next_y),
            (512, 0) => self.input.color(self.next_x, self.y),
            _ => {
                let a = blend_hsl(
                    self.input.color(self.x, self.y),
                    self.input.color(self.next_x, self.y),
                    (x * 128) >> 9,
                );
                let b = blend_hsl(
                    self.input.color(self.x, self.next_y),
                    self.input.color(self.next_x, self.next_y),
                    (x * 128) >> 9,
                );
                blend_hsl(a, b, (z * 128) >> 9)
            }
        };
        (color, underlay.texture, underlay.scale as i32)
    }
}

impl BlendedGroundTile {
    fn push(&mut self, vertex: GroundVertex, rendered: i32) {
        self.vertices[self.emitted_vertices] = vertex;
        if let Some(colors) = self.blending_colors.as_mut() {
            colors[self.emitted_vertices] = rendered;
        }
        self.emitted_vertices += 1;
    }
}
