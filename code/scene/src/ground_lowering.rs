use crate::ground_geometry::{
    GroundGeometry, MAX_DIMENSION, MAX_OUTPUT_VERTICES, MAX_VERTICES_PER_TILE,
};
use crate::material::Material;
use crate::rgb::HslRgbLut;
use std::collections::HashMap;
use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

pub const DEFAULT_SURFACE_WATER: [i32; 7] = [1583160, 40, 127, 63, 0, 0, 0];
pub const MAX_UNIQUE_VERTICES: usize = 65536;
pub const MAX_MATERIAL_ID: i32 = 16383;
pub const MAX_TEXTURE_SCALE: i32 = 65535;
pub const MAX_MATERIAL_COLOR_ENTRIES: usize = 1 << 24;
pub const UNTEXTURED_MATERIAL_ID: i32 = -1;

#[derive(Clone, Copy, Debug)]
pub struct OrdinaryGroundInput<'a> {
    pub width: usize,
    pub height: usize,
    pub geometry: &'a GroundGeometry,
    pub position_heights: &'a [i32],
    pub normal_heights: &'a [i32],
    pub baked_shadows: &'a [u8],
    pub rgb: &'a HslRgbLut,
    pub texture_scale: i32,
    pub tile_units: u32,
    pub surface_water: [i32; 7],
    pub underwater: bool,
    pub secondary_ground_heights: bool,
    pub hd_lighting: bool,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, Hash)]
pub struct GroundMaterialShading {
    pub material_id: i32,
    pub alpha: u8,
    pub brightness: u8,
    pub effect_id: i8,
}

impl GroundMaterialShading {
    pub fn from_material(material_id: i32, material: &Material) -> Self {
        Self {
            material_id,
            alpha: material.alpha as u8,
            brightness: material.brightness as u8,
            effect_id: material.effect_id,
        }
    }
}

#[derive(Clone, Copy, Debug)]
pub struct TexturedGroundInput<'a> {
    pub width: usize,
    pub height: usize,
    pub geometry: &'a GroundGeometry,
    pub position_heights: &'a [i32],
    pub normal_heights: &'a [i32],
    pub baked_shadows: &'a [u8],
    pub rgb: &'a HslRgbLut,
    pub materials: &'a [GroundMaterialShading],
    pub tile_units: u32,
    pub surface_water: [i32; 7],
    pub underwater: bool,
    pub secondary_ground_heights: bool,
    pub hd_lighting: bool,
}

impl<'a> OrdinaryGroundInput<'a> {
    fn textured(&self) -> TexturedGroundInput<'a> {
        TexturedGroundInput {
            width: self.width,
            height: self.height,
            geometry: self.geometry,
            position_heights: self.position_heights,
            normal_heights: self.normal_heights,
            baked_shadows: self.baked_shadows,
            rgb: self.rgb,
            materials: &[],
            tile_units: self.tile_units,
            surface_water: self.surface_water,
            underwater: self.underwater,
            secondary_ground_heights: self.secondary_ground_heights,
            hd_lighting: self.hd_lighting,
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct GroundLoweringLimits {
    pub max_input_vertices: usize,
    pub max_unique_vertices: usize,
}

impl Default for GroundLoweringLimits {
    fn default() -> Self {
        Self {
            max_input_vertices: MAX_OUTPUT_VERTICES,
            max_unique_vertices: MAX_UNIQUE_VERTICES,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum GroundLoweringError {
    Unsupported(&'static str),
    Dimensions {
        width: usize,
        height: usize,
    },
    InputLength {
        input: &'static str,
        expected: usize,
        actual: usize,
    },
    InvalidTile {
        x: usize,
        z: usize,
    },
    DuplicateTile {
        x: usize,
        z: usize,
    },
    InvalidVertex {
        x: usize,
        z: usize,
        vertex: usize,
        field: &'static str,
    },
    InvalidMaterial {
        material_id: i32,
    },
    MaterialIndex {
        index: usize,
        count: usize,
    },
    VertexCount {
        declared: usize,
        actual: usize,
    },
    Budget {
        resource: &'static str,
        required: usize,
        limit: usize,
    },
    NonFiniteNormal {
        x: usize,
        z: usize,
    },
    Allocation,
}

impl Display for GroundLoweringError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        write!(f, "ground lowering: {self:?}")
    }
}

impl Error for GroundLoweringError {}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct OrdinaryGroundVertex {
    pub position: [f32; 3],
    pub rgba: [u8; 4],
    pub texcoord: [f32; 3],
    pub normal: [f32; 3],
}

#[derive(Clone, Debug, PartialEq)]
pub struct CompiledGround {
    width: usize,
    height: usize,
    texture_scale: i32,
    vertices: Box<[OrdinaryGroundVertex]>,
    blend_colors: Box<[[u8; 4]]>,
    tile_indices: Box<[Option<Box<[u16]>>]>,
    tile_masks: Box<[u32]>,
}

impl CompiledGround {
    pub fn width(&self) -> usize {
        self.width
    }
    pub fn height(&self) -> usize {
        self.height
    }
    pub fn texture_scale(&self) -> i32 {
        self.texture_scale
    }
    pub fn vertices(&self) -> &[OrdinaryGroundVertex] {
        &self.vertices
    }
    pub fn blend_colors(&self) -> &[[u8; 4]] {
        &self.blend_colors
    }
    pub fn tile_indices(&self) -> &[Option<Box<[u16]>>] {
        &self.tile_indices
    }
    pub fn tile_masks(&self) -> &[u32] {
        &self.tile_masks
    }
}

#[derive(Clone, Debug, PartialEq)]
pub struct CompiledGroundMaterial {
    material_id: i32,
    texture_scale: i32,
    uid: i64,
    colors: Box<[[u8; 4]]>,
    tile_masks: Box<[u32]>,
}

impl CompiledGroundMaterial {
    pub fn material_id(&self) -> i32 {
        self.material_id
    }
    pub fn texture_scale(&self) -> i32 {
        self.texture_scale
    }
    pub fn uid(&self) -> i64 {
        self.uid
    }
    pub fn colors(&self) -> &[[u8; 4]] {
        &self.colors
    }
    pub fn tile_masks(&self) -> &[u32] {
        &self.tile_masks
    }
}

#[derive(Clone, Debug, PartialEq)]
pub struct CompiledTexturedGround {
    width: usize,
    height: usize,
    vertices: Box<[OrdinaryGroundVertex]>,
    tile_indices: Box<[Option<Box<[u16]>>]>,
    materials: Box<[CompiledGroundMaterial]>,
}

impl CompiledTexturedGround {
    pub fn width(&self) -> usize {
        self.width
    }
    pub fn height(&self) -> usize {
        self.height
    }
    pub fn vertices(&self) -> &[OrdinaryGroundVertex] {
        &self.vertices
    }
    pub fn tile_indices(&self) -> &[Option<Box<[u16]>>] {
        &self.tile_indices
    }
    pub fn materials(&self) -> &[CompiledGroundMaterial] {
        &self.materials
    }
}

#[derive(Clone, Debug, PartialEq)]
pub struct SelectedGroundMesh {
    pub vertices: Box<[OrdinaryGroundVertex]>,
    pub indices: Box<[u32]>,
    pub source_first_vertex: u32,
    pub texture_scale: i32,
    pub material_id: i32,
}

pub fn ground_material_uid(material_id: i32, texture_scale: i32, surface_water: [i32; 7]) -> i64 {
    ((surface_water[2] as i64) << 48)
        | ((surface_water[1] as i64) << 42)
        | ((surface_water[0] as i64) << 28)
        | ((texture_scale as i64) << 14)
        | (material_id as i64)
}

struct MaterialEntry {
    material_id: i32,
    texture_scale: i32,
    uid: i64,
    shading: Option<GroundMaterialShading>,
}

struct Lowered {
    vertices: Vec<OrdinaryGroundVertex>,
    tile_indices: Vec<Option<Box<[u16]>>>,
    materials: Vec<CompiledGroundMaterial>,
}

fn allocate<T>(count: usize) -> Result<Vec<T>, GroundLoweringError> {
    let mut result = Vec::new();
    result
        .try_reserve_exact(count)
        .map_err(|_| GroundLoweringError::Allocation)?;
    Ok(result)
}

fn budget(
    resource: &'static str,
    required: usize,
    limit: usize,
) -> Result<(), GroundLoweringError> {
    if required > limit {
        Err(GroundLoweringError::Budget {
            resource,
            required,
            limit,
        })
    } else {
        Ok(())
    }
}

fn validate(
    input: &TexturedGroundInput<'_>,
    limits: GroundLoweringLimits,
    ordinary_scale: Option<i32>,
) -> Result<Vec<Option<usize>>, GroundLoweringError> {
    if input.underwater {
        return Err(GroundLoweringError::Unsupported("underwater"));
    }
    if input.secondary_ground_heights {
        return Err(GroundLoweringError::Unsupported("secondary ground heights"));
    }
    if !input.hd_lighting {
        return Err(GroundLoweringError::Unsupported("non-HD lighting"));
    }
    if input.tile_units != 512 {
        return Err(GroundLoweringError::Unsupported(
            "tile units other than 512",
        ));
    }
    if input.surface_water != DEFAULT_SURFACE_WATER {
        return Err(GroundLoweringError::Unsupported(
            "non-default surface water",
        ));
    }
    if ordinary_scale.is_some_and(|scale| scale <= 0) {
        return Err(GroundLoweringError::Unsupported(
            "nonpositive texture scale",
        ));
    }
    for (i, shading) in input.materials.iter().enumerate() {
        if !(0..=MAX_MATERIAL_ID).contains(&shading.material_id)
            || input.materials[..i]
                .iter()
                .any(|other| other.material_id == shading.material_id)
        {
            return Err(GroundLoweringError::InvalidMaterial {
                material_id: shading.material_id,
            });
        }
    }
    let (width, height) = (input.width, input.height);
    if width == 0 || height == 0 || width > MAX_DIMENSION || height > MAX_DIMENSION {
        return Err(GroundLoweringError::Dimensions { width, height });
    }
    let grid_count = (width + 1) * (height + 1);
    for (name, actual) in [
        ("position heights", input.position_heights.len()),
        ("normal heights", input.normal_heights.len()),
        ("baked shadows", input.baked_shadows.len()),
    ] {
        if actual != grid_count {
            return Err(GroundLoweringError::InputLength {
                input: name,
                expected: grid_count,
                actual,
            });
        }
    }
    let mut tile_lookup = allocate(width * height)?;
    tile_lookup.resize(width * height, None);
    let mut count = 0usize;
    for (i, tile) in input.geometry.tiles.iter().enumerate() {
        if tile.x >= width
            || tile.y >= height
            || tile.shape > 14
            || tile.rotation > 3
            || tile.vertices.len() > MAX_VERTICES_PER_TILE
            || tile.vertices.len() % 3 != 0
            || tile.emitted_vertices > tile.vertices.len()
            || tile.emitted_vertices % 3 != 0
        {
            return Err(GroundLoweringError::InvalidTile {
                x: tile.x,
                z: tile.y,
            });
        }
        let slot = &mut tile_lookup[tile.x + tile.y * width];
        if slot.replace(i).is_some() {
            return Err(GroundLoweringError::DuplicateTile {
                x: tile.x,
                z: tile.y,
            });
        }
        if let Some(blend) = &tile.blending_colors {
            if blend.len() != tile.vertices.len() {
                return Err(GroundLoweringError::InputLength {
                    input: "blending colors",
                    expected: tile.vertices.len(),
                    actual: blend.len(),
                });
            }
        }
        for (j, v) in tile.vertices.iter().enumerate() {
            let blend = tile
                .blending_colors
                .as_ref()
                .map_or(v.main_color, |colors| colors[j]);
            let bad = if !(0..=512).contains(&v.offset_x) || !(0..=512).contains(&v.offset_z) {
                Some("offset")
            } else if let Some(scale) = ordinary_scale {
                if v.texture != UNTEXTURED_MATERIAL_ID {
                    Some("texture")
                } else if v.texture_scale != scale {
                    Some("material scale")
                } else if !(0..=65535).contains(&v.main_color) {
                    Some("main color")
                } else if !(0..=65535).contains(&blend) {
                    Some("blend color")
                } else {
                    None
                }
            } else if v.texture != UNTEXTURED_MATERIAL_ID
                && !(0..=MAX_MATERIAL_ID).contains(&v.texture)
            {
                Some("texture")
            } else if v.texture != UNTEXTURED_MATERIAL_ID
                && !input
                    .materials
                    .iter()
                    .any(|shading| shading.material_id == v.texture)
            {
                Some("material shading")
            } else if !(0..=MAX_TEXTURE_SCALE).contains(&v.texture_scale) {
                Some("material scale")
            } else if v.main_color != -1 && !(0..=65535).contains(&v.main_color) {
                Some("main color")
            } else if blend != -1 && !(0..=65535).contains(&blend) {
                Some("blend color")
            } else {
                None
            };
            if let Some(field) = bad {
                return Err(GroundLoweringError::InvalidVertex {
                    x: tile.x,
                    z: tile.y,
                    vertex: j,
                    field,
                });
            }
        }
        count += tile.vertices.len();
    }
    if count != input.geometry.vertex_count {
        return Err(GroundLoweringError::VertexCount {
            declared: input.geometry.vertex_count,
            actual: count,
        });
    }
    budget(
        "input vertices",
        count,
        limits.max_input_vertices.min(MAX_OUTPUT_VERTICES),
    )?;
    Ok(tile_lookup)
}

type MaterialKeyIndex = HashMap<(i32, i32), usize>;

fn material_table(
    input: &TexturedGroundInput<'_>,
) -> Result<(Vec<MaterialEntry>, MaterialKeyIndex), GroundLoweringError> {
    let mut entries: Vec<MaterialEntry> = allocate(0)?;
    let mut by_key = MaterialKeyIndex::new();
    for tile in input.geometry.tiles.iter() {
        for v in tile.vertices.iter() {
            let key = (v.texture, v.texture_scale);
            if by_key.contains_key(&key) {
                continue;
            }
            entries
                .try_reserve(1)
                .map_err(|_| GroundLoweringError::Allocation)?;
            by_key
                .try_reserve(1)
                .map_err(|_| GroundLoweringError::Allocation)?;
            let shading = if v.texture == UNTEXTURED_MATERIAL_ID {
                None
            } else {
                input
                    .materials
                    .iter()
                    .copied()
                    .find(|shading| shading.material_id == v.texture)
            };
            by_key.insert(key, entries.len());
            entries.push(MaterialEntry {
                material_id: v.texture,
                texture_scale: v.texture_scale,
                uid: ground_material_uid(v.texture, v.texture_scale, input.surface_water),
                shading,
            });
        }
    }
    let mut order: Vec<usize> = allocate(entries.len())?;
    order.extend(0..entries.len());
    order.sort_by_key(|&index| entries[index].uid & (MATERIAL_TABLE_BUCKETS - 1));
    let mut keys: Vec<i64> = allocate(entries.len())?;
    keys.extend(order.iter().map(|&index| entries[index].uid));
    if !entries.is_empty() {
        native_quicksort(&mut keys, &mut order, 0, entries.len() as isize - 1);
    }
    let mut sorted: Vec<Option<MaterialEntry>> = allocate(entries.len())?;
    sorted.extend(entries.into_iter().map(Some));
    let mut entries: Vec<MaterialEntry> = allocate(sorted.len())?;
    for (position, &index) in order.iter().enumerate() {
        let entry = sorted[index]
            .take()
            .ok_or(GroundLoweringError::Allocation)?;
        by_key.insert((entry.material_id, entry.texture_scale), position);
        entries.push(entry);
    }
    Ok((entries, by_key))
}

const MATERIAL_TABLE_BUCKETS: i64 = 128;

fn native_quicksort(keys: &mut [i64], values: &mut [usize], start: isize, end: isize) {
    if start >= end {
        return;
    }
    let mid = (end + start) / 2;
    let pivot = keys[mid as usize];
    keys.swap(mid as usize, end as usize);
    values.swap(mid as usize, end as usize);
    let tie_break_mask = if pivot == i64::MAX { 0 } else { 1 };
    let mut partition = start;
    for i in start..end {
        if keys[i as usize] < (i as i64 & tie_break_mask) + pivot {
            keys.swap(i as usize, partition as usize);
            values.swap(i as usize, partition as usize);
            partition += 1;
        }
    }
    keys.swap(end as usize, partition as usize);
    values.swap(end as usize, partition as usize);
    native_quicksort(keys, values, start, partition - 1);
    native_quicksort(keys, values, partition + 1, end);
}

pub fn lighting_normals(
    normal_heights: &[i32],
    width: usize,
    height: usize,
) -> Result<Vec<[f32; 3]>, GroundLoweringError> {
    let grid_width = width + 1;
    let grid_count = grid_width * (height + 1);
    if normal_heights.len() != grid_count {
        return Err(GroundLoweringError::Allocation);
    }
    let mut normals = allocate(grid_count)?;
    normals.resize(grid_count, [0.0f32; 3]);
    for z in 1..height {
        for x in 1..width {
            let at = x + z * grid_width;
            let dx = normal_heights[at + 1].wrapping_sub(normal_heights[at - 1]);
            let dz = normal_heights[at + grid_width].wrapping_sub(normal_heights[at - grid_width]);
            let denominator = 1048576i32
                .wrapping_add(dx.wrapping_mul(dx))
                .wrapping_add(dz.wrapping_mul(dz));
            if denominator <= 0 {
                return Err(GroundLoweringError::NonFiniteNormal { x, z });
            }
            let inverse = (1.0 / (denominator as f64).sqrt()) as f32;
            normals[at] = [dx as f32 * inverse, -1024.0 * inverse, dz as f32 * inverse];
        }
    }
    Ok(normals)
}

pub fn lighting_intensity(
    baked_shadows: &[u8],
    width: usize,
    height: usize,
) -> Result<Vec<u8>, GroundLoweringError> {
    let grid_width = width + 1;
    let grid_count = grid_width * (height + 1);
    if baked_shadows.len() != grid_count {
        return Err(GroundLoweringError::Allocation);
    }
    let mut intensity = allocate(grid_count)?;
    intensity.resize(grid_count, 0u8);
    for z in 1..height {
        for x in 1..width {
            let at = x + z * grid_width;
            let signed = |i: usize| baked_shadows[i] as i8 as i32;
            intensity[at] = ((signed(at - grid_width) >> 2)
                + (signed(at) >> 1)
                + (signed(at - 1) >> 2)
                + (signed(at + grid_width) >> 3)
                + (signed(at + 1) >> 3)) as u8;
        }
    }
    Ok(intensity)
}

fn lower(
    input: &TexturedGroundInput<'_>,
    limits: GroundLoweringLimits,
    ordinary_scale: Option<i32>,
) -> Result<Lowered, GroundLoweringError> {
    let lookup = validate(input, limits, ordinary_scale)?;
    let (width, height) = (input.width, input.height);
    let grid_width = width + 1;
    let normals = lighting_normals(input.normal_heights, width, height)?;
    let intensity = lighting_intensity(input.baked_shadows, width, height)?;
    let (entries, by_key) = material_table(input)?;
    let capacity = input
        .geometry
        .vertex_count
        .min(limits.max_unique_vertices)
        .min(MAX_UNIQUE_VERTICES);
    budget(
        "material color entries",
        entries.len().saturating_mul(capacity),
        MAX_MATERIAL_COLOR_ENTRIES,
    )?;
    let mut vertices: Vec<OrdinaryGroundVertex> = allocate(capacity)?;
    let mut owners: Vec<Option<usize>> = allocate(capacity)?;
    let mut colors: Vec<Vec<[u8; 4]>> = allocate(entries.len())?;
    let mut masks: Vec<Vec<u32>> = allocate(entries.len())?;
    for _ in &entries {
        colors.push(allocate(capacity)?);
        let mut material_masks = allocate(width * height)?;
        material_masks.resize(width * height, 0u32);
        masks.push(material_masks);
    }
    let mut tile_indices = allocate(width * height)?;
    tile_indices.resize_with(width * height, || None);
    let mut welded = HashMap::<i64, u16>::new();
    welded
        .try_reserve(capacity)
        .map_err(|_| GroundLoweringError::Allocation)?;
    let mut tile_materials: Vec<usize> = allocate(MAX_VERTICES_PER_TILE)?;
    for x in 0..width {
        for z in 0..height {
            let tile_at = x + z * width;
            let Some(tile_id) = lookup[tile_at] else {
                continue;
            };
            let tile = &input.geometry.tiles[tile_id];
            tile_materials.clear();
            for source in tile.vertices.iter() {
                let material = by_key[&(source.texture, source.texture_scale)];
                if !tile_materials.contains(&material) {
                    tile_materials.push(material);
                }
            }
            let mut indices = allocate(tile.vertices.len())?;
            let southwest = x + z * grid_width;
            let corners = [
                southwest,
                southwest + grid_width,
                southwest + grid_width + 1,
                southwest + 1,
            ];
            for (i, source) in tile.vertices.iter().enumerate() {
                let ox = source.offset_x;
                let oz = source.offset_z;
                let sx = (x as i32 * 512) + ox;
                let sz = (z as i32 * 512) + oz;
                let main = source.main_color;
                let blend = tile
                    .blending_colors
                    .as_ref()
                    .map_or(main, |colors| colors[i]);
                let key = ((blend as i64) << 48)
                    | ((main as i64) << 32)
                    | (((sx >> 7) as i64) << 16)
                    | ((sz >> 7) as i64);
                let corner = match (ox, oz) {
                    (0, 0) => Some(0),
                    (0, 512) => Some(1),
                    (512, 512) => Some(2),
                    (512, 0) => Some(3),
                    _ => None,
                };
                let (normal, strength) = if let Some(c) = corner {
                    (normals[corners[c]], 74 - intensity[corners[c]] as i32)
                } else {
                    let fx = ox as f32 / 512.0;
                    let fz = oz as f32 / 512.0;
                    let mut normal = [0.0; 3];
                    for (axis, result) in normal.iter_mut().enumerate() {
                        let south = normals[corners[0]][axis]
                            + (normals[corners[3]][axis] - normals[corners[0]][axis]) * fx;
                        let north = normals[corners[1]][axis]
                            + (normals[corners[2]][axis] - normals[corners[1]][axis]) * fx;
                        *result = south + (north - south) * fz;
                    }
                    let sw = intensity[corners[0]] as i32;
                    let se = intensity[corners[3]] as i32;
                    let nw = intensity[corners[1]] as i32;
                    let ne = intensity[corners[2]] as i32;
                    let south = sw + ((ox * (se - sw)) >> 9);
                    let north = nw + ((ox * (ne - nw)) >> 9);
                    (normal, 74 - (south + ((oz * (north - south)) >> 9)))
                };
                let direct = if main != -1 {
                    lit_rgb(main, strength, input.rgb)
                } else {
                    0
                };
                let material = by_key[&(source.texture, source.texture_scale)];
                let existing = if sx & 127 == 0 && sz & 127 == 0 {
                    welded.get(&key).copied()
                } else {
                    None
                };
                let index = if let Some(index) = existing {
                    if main != -1 {
                        let Some(owner) = owners[index as usize] else {
                            return Err(GroundLoweringError::Unsupported(
                                "colored vertex welded onto a colorless vertex",
                            ));
                        };
                        if entries[material].uid < entries[owner].uid {
                            owners[index as usize] = Some(material);
                        }
                    }
                    index
                } else {
                    budget(
                        "unique vertices",
                        vertices.len() + 1,
                        limits.max_unique_vertices.min(MAX_UNIQUE_VERTICES),
                    )?;
                    let index = vertices.len() as u16;
                    let stream = if blend != main {
                        lit_rgb(blend, strength, input.rgb)
                    } else {
                        direct
                    };
                    vertices.push(OrdinaryGroundVertex {
                        position: [
                            sx as f32,
                            sample_height(width, height, input.position_heights, sx, sz) as f32,
                            sz as f32,
                        ],
                        rgba: rgba(stream),
                        texcoord: [sx as f32, sz as f32, 0.0],
                        normal,
                    });
                    owners.push((main != -1).then_some(material));
                    for material_colors in &mut colors {
                        material_colors.push([0; 4]);
                    }
                    welded.entry(key).or_insert(index);
                    index
                };
                for &tile_material in &tile_materials {
                    let shaded = shade(entries[tile_material].shading, direct, strength);
                    let slot = &mut colors[tile_material][index as usize];
                    slot[0] = (shaded >> 16) as u8;
                    slot[1] = (shaded >> 8) as u8;
                    slot[2] = shaded as u8;
                }
                indices.push(index);
            }
            tile_indices[tile_at] = Some(indices.into_boxed_slice());
        }
    }
    for (index, owner) in owners.iter().enumerate() {
        if let Some(owner) = owner {
            colors[*owner][index][3] = 255;
        }
    }
    for x in 0..width {
        for z in 0..height {
            let tile_at = x + z * width;
            let Some(tile) = &tile_indices[tile_at] else {
                continue;
            };
            for (face, triangle) in tile.chunks_exact(3).enumerate() {
                let face_owners = [
                    owners[triangle[0] as usize],
                    owners[triangle[1] as usize],
                    owners[triangle[2] as usize],
                ];
                let mut lowest: Option<usize> = None;
                for &owner in face_owners.iter().flatten() {
                    masks[owner][tile_at] |= 1 << face;
                    lowest = Some(match lowest {
                        Some(current) if entries[owner].uid >= entries[current].uid => current,
                        _ => owner,
                    });
                }
                if let Some(lowest) = lowest {
                    for (&vertex, owner) in triangle.iter().zip(face_owners) {
                        if owner.is_some() {
                            colors[lowest][vertex as usize][3] = 255;
                        }
                    }
                    masks[lowest][tile_at] |= 1 << face;
                }
            }
        }
    }
    let mut materials = allocate(entries.len())?;
    for ((entry, material_colors), material_masks) in entries.into_iter().zip(colors).zip(masks) {
        materials.push(CompiledGroundMaterial {
            material_id: entry.material_id,
            texture_scale: entry.texture_scale,
            uid: entry.uid,
            colors: material_colors.into_boxed_slice(),
            tile_masks: material_masks.into_boxed_slice(),
        });
    }
    Ok(Lowered {
        vertices,
        tile_indices,
        materials,
    })
}

pub fn lower_ordinary_untextured_ground(
    input: OrdinaryGroundInput<'_>,
    limits: GroundLoweringLimits,
) -> Result<CompiledGround, GroundLoweringError> {
    let lowered = lower(&input.textured(), limits, Some(input.texture_scale))?;
    let (width, height) = (input.width, input.height);
    let mut vertices = lowered.vertices;
    let mut blend_colors = allocate(vertices.len())?;
    blend_colors.extend(vertices.iter().map(|v| v.rgba));
    let tile_masks = match lowered.materials.into_iter().next() {
        Some(material) => {
            for (vertex, color) in vertices.iter_mut().zip(material.colors.iter()) {
                vertex.rgba = *color;
            }
            material.tile_masks
        }
        None => {
            let mut masks = allocate(width * height)?;
            masks.resize(width * height, 0u32);
            masks.into_boxed_slice()
        }
    };
    Ok(CompiledGround {
        width,
        height,
        texture_scale: input.texture_scale,
        vertices: vertices.into_boxed_slice(),
        blend_colors: blend_colors.into_boxed_slice(),
        tile_indices: lowered.tile_indices.into_boxed_slice(),
        tile_masks,
    })
}

pub fn lower_textured_ground(
    input: TexturedGroundInput<'_>,
    limits: GroundLoweringLimits,
) -> Result<CompiledTexturedGround, GroundLoweringError> {
    let lowered = lower(&input, limits, None)?;
    Ok(CompiledTexturedGround {
        width: input.width,
        height: input.height,
        vertices: lowered.vertices.into_boxed_slice(),
        tile_indices: lowered.tile_indices.into_boxed_slice(),
        materials: lowered.materials.into_boxed_slice(),
    })
}

pub fn shade_ground_color(
    shading: Option<GroundMaterialShading>,
    color: i32,
    lightness: i32,
) -> i32 {
    shade(shading, color, lightness)
}

fn shade(shading: Option<GroundMaterialShading>, color: i32, lightness: i32) -> i32 {
    let Some(shading) = shading else {
        return color;
    };
    let mut color = color;
    let contrast = shading.alpha as i32;
    if contrast != 0 && shading.effect_id != 4 {
        let light = if lightness < 0 {
            0
        } else if lightness > 127 {
            16777215
        } else {
            lightness * 131586
        };
        let blend = 256 - contrast;
        let high = (light & 0xff00ff)
            .wrapping_mul(contrast)
            .wrapping_add(blend.wrapping_mul(color & 0xff00ff))
            & -16711936;
        let mid = blend
            .wrapping_mul(color & 0xff00)
            .wrapping_add(contrast.wrapping_mul(light & 0xff00))
            & 0xff0000;
        color = high.wrapping_add(mid) >> 8;
    }
    let brightness = shading.brightness as i32;
    if brightness != 0 {
        let brightness = brightness + 256;
        let red = (((color & 0xff0000) >> 16) * brightness).min(65535);
        let green = (((color & 0xff00) >> 8) * brightness).min(65535);
        let blue = (brightness * (color & 0xff)).min(65535);
        color = (blue >> 8) + (green & 0xff00) + ((red << 8) & 0xff0000);
    }
    color
}

fn lit_rgb(hsl: i32, strength: i32, rgb: &HslRgbLut) -> i32 {
    let lightness = ((strength * (hsl & 127)) >> 7).clamp(2, 126);
    rgb.hsl_to_rgb()[((hsl & 0xff80) | lightness) as usize] as i32
}

fn rgba(color: i32) -> [u8; 4] {
    [(color >> 16) as u8, (color >> 8) as u8, color as u8, 255]
}

fn sample_height(width: usize, height: usize, heights: &[i32], sx: i32, sz: i32) -> i32 {
    let x = (sx >> 9) as usize;
    let z = (sz >> 9) as usize;
    if x >= width || z >= height {
        return 0;
    }
    let at = x + z * (width + 1);
    let ox = sx & 511;
    let oz = sz & 511;
    let south = heights[at]
        .wrapping_mul(512 - ox)
        .wrapping_add(ox.wrapping_mul(heights[at + 1]))
        >> 9;
    let north = heights[at + width + 1]
        .wrapping_mul(512 - ox)
        .wrapping_add(heights[at + width + 2].wrapping_mul(ox))
        >> 9;
    north
        .wrapping_mul(oz)
        .wrapping_add(south.wrapping_mul(512 - oz))
        >> 9
}

struct FaceSelection {
    indices: Vec<u32>,
    low: usize,
    high: usize,
}

fn select_faces(
    width: usize,
    height: usize,
    tile_indices: &[Option<Box<[u16]>>],
    tile_masks: &[u32],
    visible_tiles: &[bool],
    max_indices: usize,
) -> Result<Option<FaceSelection>, GroundLoweringError> {
    let tile_count = width * height;
    if visible_tiles.len() != tile_count {
        return Err(GroundLoweringError::InputLength {
            input: "visible tiles",
            expected: tile_count,
            actual: visible_tiles.len(),
        });
    }
    let chunks_x = width.div_ceil(8);
    let chunks_z = height.div_ceil(8);
    let mut indices = allocate::<u32>(0)?;
    let limit = max_indices.min(MAX_OUTPUT_VERTICES);
    let mut low = usize::MAX;
    let mut high = 0usize;
    for chunk_z in 0..chunks_z {
        for chunk_x in 0..chunks_x {
            for z in chunk_z * 8..((chunk_z + 1) * 8).min(height) {
                for x in chunk_x * 8..((chunk_x + 1) * 8).min(width) {
                    let at = x + z * width;
                    if !visible_tiles[at] {
                        continue;
                    }
                    let Some(tile) = &tile_indices[at] else {
                        continue;
                    };
                    for (face, triangle) in tile.chunks_exact(3).enumerate() {
                        if tile_masks[at] & (1 << face) == 0 {
                            continue;
                        }
                        budget("selected indices", indices.len() + 3, limit)?;
                        indices
                            .try_reserve(3)
                            .map_err(|_| GroundLoweringError::Allocation)?;
                        for &index in triangle {
                            low = low.min(index as usize);
                            high = high.max(index as usize);
                            indices.push(index as u32);
                        }
                    }
                }
            }
        }
    }
    if indices.is_empty() {
        return Ok(None);
    }
    for index in &mut indices {
        *index -= low as u32;
    }
    Ok(Some(FaceSelection { indices, low, high }))
}

pub fn select_ordinary_ground(
    ground: &CompiledGround,
    visible_tiles: &[bool],
    max_indices: usize,
) -> Result<SelectedGroundMesh, GroundLoweringError> {
    let selection = select_faces(
        ground.width,
        ground.height,
        &ground.tile_indices,
        &ground.tile_masks,
        visible_tiles,
        max_indices,
    )?;
    let Some(selection) = selection else {
        return Ok(SelectedGroundMesh {
            vertices: Box::new([]),
            indices: Box::new([]),
            source_first_vertex: 0,
            texture_scale: ground.texture_scale,
            material_id: UNTEXTURED_MATERIAL_ID,
        });
    };
    let mut vertices = allocate(selection.high - selection.low + 1)?;
    vertices.extend_from_slice(&ground.vertices[selection.low..=selection.high]);
    Ok(SelectedGroundMesh {
        vertices: vertices.into_boxed_slice(),
        indices: selection.indices.into_boxed_slice(),
        source_first_vertex: selection.low as u32,
        texture_scale: ground.texture_scale,
        material_id: UNTEXTURED_MATERIAL_ID,
    })
}

pub fn select_textured_ground(
    ground: &CompiledTexturedGround,
    material: usize,
    visible_tiles: &[bool],
    max_indices: usize,
) -> Result<SelectedGroundMesh, GroundLoweringError> {
    let Some(compiled) = ground.materials.get(material) else {
        return Err(GroundLoweringError::MaterialIndex {
            index: material,
            count: ground.materials.len(),
        });
    };
    let selection = select_faces(
        ground.width,
        ground.height,
        &ground.tile_indices,
        &compiled.tile_masks,
        visible_tiles,
        max_indices,
    )?;
    let Some(selection) = selection else {
        return Ok(SelectedGroundMesh {
            vertices: Box::new([]),
            indices: Box::new([]),
            source_first_vertex: 0,
            texture_scale: compiled.texture_scale,
            material_id: compiled.material_id,
        });
    };
    let mut vertices = allocate(selection.high - selection.low + 1)?;
    for (vertex, color) in ground.vertices[selection.low..=selection.high]
        .iter()
        .zip(&compiled.colors[selection.low..=selection.high])
    {
        vertices.push(OrdinaryGroundVertex {
            rgba: *color,
            ..*vertex
        });
    }
    Ok(SelectedGroundMesh {
        vertices: vertices.into_boxed_slice(),
        indices: selection.indices.into_boxed_slice(),
        source_first_vertex: selection.low as u32,
        texture_scale: compiled.texture_scale,
        material_id: compiled.material_id,
    })
}
