use std::collections::HashSet;
use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

use crate::material::{Material, MaterialLookupError, MaterialTable};
use crate::mesh::Mesh;

pub const BYTES_PER_VERTEX: usize = 40;
pub const COLOR_OFFSET: usize = 32;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum BuildError {
    Index,
    MissingArray,
    Material(MaterialLookupError),
}

impl Display for BuildError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        match self {
            Self::Index => write!(f, "model array index out of range"),
            Self::MissingArray => write!(f, "model array required by the build is absent"),
            Self::Material(error) => write!(f, "{error}"),
        }
    }
}

impl Error for BuildError {}

impl From<MaterialLookupError> for BuildError {
    fn from(error: MaterialLookupError) -> Self {
        Self::Material(error)
    }
}

type Res<T> = Result<T, BuildError>;

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct TextureBatch {
    pub material_id: i32,
    pub indices: Vec<u32>,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct BuiltModel {
    pub id: i64,
    pub face_count: i32,
    pub vertex_count: i32,
    pub vertex_data: Vec<u8>,
    pub batches: Vec<TextureBatch>,
    pub skip_billboard_hidden: i32,
    pub skip_render_type: i32,
    pub skip_invalid_vertex: i32,
    pub skip_transparent: i32,
    pub source_face_count: i32,
    pub billboards_seen: i32,
    pub billboards_with_uid: i32,
    pub billboards_out_of_range: i32,
    pub skip_material_filtered: i32,
    pub accepted_faces: Vec<i32>,
    pub accepted_material_ids: Vec<i32>,
}

pub struct BuildInput<'a> {
    pub ambient: i32,
    pub materials: Option<&'a MaterialTable>,
    pub strip_detail_layers: bool,
    pub contrast: i32,
    pub hsl_to_rgb: &'a [u32],
    pub billboard_has_uid: &'a dyn Fn(i32) -> bool,
}

fn get<T: Copy>(array: &[T], index: i64) -> Res<T> {
    usize::try_from(index)
        .ok()
        .and_then(|i| array.get(i))
        .copied()
        .ok_or(BuildError::Index)
}

fn req<T>(array: &Option<Vec<T>>) -> Res<&[T]> {
    array.as_deref().ok_or(BuildError::MissingArray)
}

fn opt_get<T: Copy>(array: &Option<Vec<T>>, index: i64) -> Res<Option<T>> {
    match array {
        None => Ok(None),
        Some(values) => get(values, index).map(Some),
    }
}

fn scale_param(array: &Option<Vec<i32>>, index: i64, fallback: i32) -> i32 {
    match array {
        Some(values) => usize::try_from(index)
            .ok()
            .and_then(|i| values.get(i).copied())
            .unwrap_or(fallback),
        None => fallback,
    }
}

fn non_zero(value: i32) -> i32 {
    if value == 0 {
        1
    } else {
        value
    }
}

pub fn adjust_luminance(hsl: i32, luminance_factor: i32) -> i32 {
    let mut adjusted = (hsl & 0x7f).wrapping_mul(luminance_factor) >> 7;
    adjusted = adjusted.clamp(2, 126);
    (hsl & 0xff80) + adjusted
}

fn material_of(materials: Option<&MaterialTable>, id: i32) -> Res<Option<Material>> {
    match materials {
        Some(table) => Ok(Some(*table.get_material(id & 0xffff)?)),
        None => Ok(None),
    }
}

pub fn face_color_rgba(
    table: &[u32],
    color_hsl: i32,
    material_id: i32,
    ambient: i32,
    alpha: i32,
    materials: Option<&MaterialTable>,
) -> Res<i32> {
    let index = adjust_luminance(color_hsl, ambient);
    let mut rgb = *usize::try_from(index)
        .ok()
        .and_then(|i| table.get(i))
        .ok_or(BuildError::Index)? as i32;
    if material_id != -1 {
        if let Some(material) = material_of(materials, material_id)? {
            let material_alpha = i32::from(material.alpha as u8);
            if material_alpha != 0 {
                let ambient_rgb = if ambient < 0 {
                    0
                } else if ambient > 127 {
                    16_777_215
                } else {
                    ambient.wrapping_mul(131_586)
                };
                let inverse = 256 - material_alpha;
                let red_blue = (ambient_rgb & 0x00ff_00ff)
                    .wrapping_mul(material_alpha)
                    .wrapping_add((rgb & 0x00ff_00ff).wrapping_mul(inverse))
                    & -16_711_936;
                let green = material_alpha
                    .wrapping_mul(ambient_rgb & 0xff00)
                    .wrapping_add(inverse.wrapping_mul(rgb & 0xff00))
                    & 0x00ff_0000;
                rgb = red_blue.wrapping_add(green) >> 8;
            }
            let brightness_raw = i32::from(material.brightness as u8);
            if brightness_raw != 0 {
                let brightness = brightness_raw + 256;
                let scaled_r = brightness
                    .wrapping_mul((rgb & 0x00ff_0000) >> 16)
                    .min(65_535);
                let scaled_g = brightness.wrapping_mul((rgb & 0xff00) >> 8).min(65_535);
                let scaled_b = (rgb & 0xff).wrapping_mul(brightness).min(65_535);
                rgb = (scaled_b >> 8)
                    .wrapping_add(scaled_g & 0xff00)
                    .wrapping_add(scaled_r.wrapping_shl(8) & 0x00ff_0000);
            }
        }
    }
    Ok(rgb.wrapping_shl(8) | (255 - alpha))
}

struct Transforms {
    center_x: Vec<i32>,
    center_y: Vec<i32>,
    center_z: Vec<i32>,
    matrix: Vec<[f32; 9]>,
    present: Vec<bool>,
}

fn create_texture_matrix(
    n: i32,
    m: i32,
    p: i32,
    rotation: i32,
    scale_x: f32,
    scale_y: f32,
    scale_z: f32,
) -> [f32; 9] {
    let angle = rotation as f32 * 0.024_543_693_f32;
    let cos_r = f64::from(angle).cos() as f32;
    let sin_r = f64::from(angle).sin() as f32;
    let rotation_map = [cos_r, 0.0, sin_r, 0.0, 1.0, 0.0, -sin_r, 0.0, cos_r];
    let cos_a = m as f32 / 32767.0_f32;
    let sin_a = -((1.0_f32 - cos_a * cos_a).sqrt());
    let omc = 1.0_f32 - cos_a;
    let size = ((p.wrapping_mul(p)).wrapping_add(n.wrapping_mul(n)) as f32).sqrt();
    if size == 0.0 && cos_a == 0.0 {
        return scale_texture_matrix(rotation_map, scale_x, scale_y, scale_z);
    }
    let (mut x, mut y) = (1.0_f32, 0.0_f32);
    if size != 0.0 {
        x = (p.wrapping_neg() as f32) / size;
        y = n as f32 / size;
    }
    let mx = [
        cos_a + x * x * omc,
        y * sin_a,
        y * x * omc,
        -y * sin_a,
        cos_a,
        x * sin_a,
        x * y * omc,
        -x * sin_a,
        cos_a + y * y * omc,
    ];
    let mut result = [0.0_f32; 9];
    for row in 0..3 {
        for col in 0..3 {
            result[row * 3 + col] = rotation_map[row * 3] * mx[col]
                + rotation_map[row * 3 + 1] * mx[3 + col]
                + rotation_map[row * 3 + 2] * mx[6 + col];
        }
    }
    scale_texture_matrix(result, scale_x, scale_y, scale_z)
}

fn scale_texture_matrix(mut result: [f32; 9], sx: f32, sy: f32, sz: f32) -> [f32; 9] {
    for value in &mut result[0..3] {
        *value *= sx;
    }
    for value in &mut result[3..6] {
        *value *= sy;
    }
    for value in &mut result[6..9] {
        *value *= sz;
    }
    result
}

fn compute_transforms(mesh: &Mesh) -> Res<Transforms> {
    let count = usize::try_from(mesh.num_textures).map_err(|_| BuildError::Index)?;
    let faces_texture = req(&mesh.faces_texture)?;
    let vx = req(&mesh.vertices_x)?;
    let vy = req(&mesh.vertices_y)?;
    let vz = req(&mesh.vertices_z)?;
    let mut min_x = vec![i32::MAX; count];
    let mut max_x = vec![i32::MIN; count];
    let mut min_y = vec![i32::MAX; count];
    let mut max_y = vec![i32::MIN; count];
    let mut min_z = vec![i32::MAX; count];
    let mut max_z = vec![i32::MIN; count];
    for face in 0..i64::from(mesh.num_faces) {
        let pointer = get(faces_texture, face)?;
        if pointer == -1 {
            continue;
        }
        let texture = usize::from(pointer as u8);
        if texture >= count {
            continue;
        }
        for source in [&mesh.faces_a, &mesh.faces_b, &mesh.faces_c] {
            let vertex = i64::from(get(req(source)?, face)? as u16);
            if vertex >= i64::from(mesh.num_vertices) {
                continue;
            }
            let (x, y, z) = (get(vx, vertex)?, get(vy, vertex)?, get(vz, vertex)?);
            min_x[texture] = min_x[texture].min(x);
            max_x[texture] = max_x[texture].max(x);
            min_y[texture] = min_y[texture].min(y);
            max_y[texture] = max_y[texture].max(y);
            min_z[texture] = min_z[texture].min(z);
            max_z[texture] = max_z[texture].max(z);
        }
    }
    let mut transforms = Transforms {
        center_x: vec![0; count],
        center_y: vec![0; count],
        center_z: vec![0; count],
        matrix: vec![[0.0; 9]; count],
        present: vec![false; count],
    };
    let mapping = req(&mesh.textures_mapping)?;
    for texture in 0..count {
        let t = texture as i64;
        let kind = i32::from(get(mapping, t)?);
        if kind <= 0 {
            continue;
        }
        transforms.center_x[texture] = min_x[texture].wrapping_add(max_x[texture]) / 2;
        transforms.center_y[texture] = min_y[texture].wrapping_add(max_y[texture]) / 2;
        transforms.center_z[texture] = min_z[texture].wrapping_add(max_z[texture]) / 2;
        let (sx, sy, sz);
        if kind == 1 {
            let x = scale_param(&mesh.textures_scale_x, t, 0);
            if x == 0 {
                sx = 1.0_f32;
                sz = 1.0_f32;
            } else if x > 0 {
                sx = 1.0_f32;
                sz = x as f32 / 1024.0_f32;
            } else {
                sz = 1.0_f32;
                sx = x.wrapping_neg() as f32 / 1024.0_f32;
            }
            sy = 64.0_f32 / non_zero(scale_param(&mesh.textures_scale_y, t, 1)) as f32;
        } else if kind == 2 {
            sx = 64.0_f32 / non_zero(scale_param(&mesh.textures_scale_x, t, 1)) as f32;
            sy = 64.0_f32 / non_zero(scale_param(&mesh.textures_scale_y, t, 1)) as f32;
            sz = 64.0_f32 / non_zero(scale_param(&mesh.textures_scale_z, t, 1)) as f32;
        } else {
            sx = scale_param(&mesh.textures_scale_x, t, 1024) as f32 / 1024.0_f32;
            sy = scale_param(&mesh.textures_scale_y, t, 1024) as f32 / 1024.0_f32;
            sz = scale_param(&mesh.textures_scale_z, t, 1024) as f32 / 1024.0_f32;
        }
        let rotation = opt_get(&mesh.textures_rotation, t)?.map_or(0, |r| i32::from(r as u8));
        transforms.matrix[texture] = create_texture_matrix(
            i32::from(get(req(&mesh.texture_face_x)?, t)?),
            i32::from(get(req(&mesh.texture_face_y)?, t)?),
            i32::from(get(req(&mesh.texture_face_z)?, t)?),
            rotation,
            sx,
            sy,
            sz,
        );
        transforms.present[texture] = true;
    }
    Ok(transforms)
}

fn apply_direction(direction: i32, u: f32, v: f32) -> (f32, f32) {
    match direction {
        1 => (-v, u),
        2 => (-u, -v),
        3 => (v, -u),
        _ => (u, v),
    }
}

fn rotate(dx: f32, dy: f32, dz: f32, matrix: &[f32; 9]) -> (f32, f32, f32) {
    (
        dx * matrix[0] + dy * matrix[1] + dz * matrix[2],
        dx * matrix[3] + dy * matrix[4] + dz * matrix[5],
        dx * matrix[6] + dy * matrix[7] + dz * matrix[8],
    )
}

const DEFAULT_UV: [f32; 6] = [0.0, 1.0, 1.0, 1.0, 0.0, 0.0];

struct Vertices<'a> {
    x: &'a [i32],
    y: &'a [i32],
    z: &'a [i32],
}

impl Vertices<'_> {
    fn at(&self, index: i64) -> Res<(i32, i32, i32)> {
        Ok((
            get(self.x, index)?,
            get(self.y, index)?,
            get(self.z, index)?,
        ))
    }
}

fn wrap_pair(uv: &mut [f32; 6], first: usize, other: usize, limit: f32, shift: f32) {
    if uv[other] - uv[first] > limit {
        uv[other] -= shift;
    } else if uv[first] - uv[other] > limit {
        uv[other] += shift;
    }
}

fn wrap_uv(uv: &mut [f32; 6], direction: i32, limit: f32, shift: f32) {
    if direction & 1 == 0 {
        wrap_pair(uv, 0, 2, limit, shift);
        wrap_pair(uv, 0, 4, limit, shift);
    } else {
        wrap_pair(uv, 1, 3, limit, shift);
        wrap_pair(uv, 1, 5, limit, shift);
    }
}

#[allow(clippy::too_many_lines)]
fn compute_uvs(
    mesh: &Mesh,
    transforms: Option<&Transforms>,
    face: i64,
    material_id: i32,
) -> Res<[f32; 6]> {
    let mut uv = [0.0_f32; 6];
    if material_id == -1 {
        return Ok(uv);
    }
    let Some(pointer) = opt_get(&mesh.faces_texture, face)? else {
        return Ok(DEFAULT_UV);
    };
    if pointer == -1 {
        return Ok(DEFAULT_UV);
    }
    let texture = i64::from(pointer as u8);
    if texture >= i64::from(mesh.num_textures) {
        return Ok(DEFAULT_UV);
    }
    let kind = i32::from(get(req(&mesh.textures_mapping)?, texture)?);
    if !(0..=3).contains(&kind) {
        return Ok(uv);
    }
    let vertices = Vertices {
        x: req(&mesh.vertices_x)?,
        y: req(&mesh.vertices_y)?,
        z: req(&mesh.vertices_z)?,
    };
    let a = i64::from(get(req(&mesh.faces_a)?, face)? as u16);
    let b = i64::from(get(req(&mesh.faces_b)?, face)? as u16);
    let c = i64::from(get(req(&mesh.faces_c)?, face)? as u16);
    if kind == 0 {
        let tva = i64::from(get(req(&mesh.texture_face_x)?, texture)? as u16);
        let tvb = i64::from(get(req(&mesh.texture_face_y)?, texture)? as u16);
        let tvc = i64::from(get(req(&mesh.texture_face_z)?, texture)? as u16);
        let limit = i64::from(mesh.num_vertices);
        if tva >= limit || tvb >= limit || tvc >= limit {
            return Ok(DEFAULT_UV);
        }
        let (ox, oy, oz) = vertices.at(tva)?;
        let (ox, oy, oz) = (ox as f32, oy as f32, oz as f32);
        let (bx, by, bz) = vertices.at(tvb)?;
        let (cx, cy, cz) = vertices.at(tvc)?;
        let (e1x, e1y, e1z) = (bx as f32 - ox, by as f32 - oy, bz as f32 - oz);
        let (e2x, e2y, e2z) = (cx as f32 - ox, cy as f32 - oy, cz as f32 - oz);
        let offsets = [a, b, c].map(|v| {
            vertices
                .at(v)
                .map(|(x, y, z)| (x as f32 - ox, y as f32 - oy, z as f32 - oz))
        });
        let [off_a, off_b, off_c] = offsets;
        let (off_a, off_b, off_c) = (off_a?, off_b?, off_c?);
        let cross_x = e1y * e2z - e1z * e2y;
        let cross_y = e1z * e2x - e1x * e2z;
        let cross_z = e1x * e2y - e1y * e2x;
        let mut basis_x = e2y * cross_z - e2z * cross_y;
        let mut basis_y = e2z * cross_x - e2x * cross_z;
        let mut basis_z = e2x * cross_y - e2y * cross_x;
        let mut determinant = basis_x * e1x + basis_y * e1y + basis_z * e1z;
        if determinant == 0.0 {
            determinant = 1.0;
        }
        let mut scale = 1.0_f32 / determinant;
        uv[0] = (basis_x * off_a.0 + basis_y * off_a.1 + basis_z * off_a.2) * scale;
        uv[2] = (basis_x * off_b.0 + basis_y * off_b.1 + basis_z * off_b.2) * scale;
        uv[4] = (basis_x * off_c.0 + basis_y * off_c.1 + basis_z * off_c.2) * scale;
        basis_x = e1y * cross_z - e1z * cross_y;
        basis_y = e1z * cross_x - e1x * cross_z;
        basis_z = e1x * cross_y - e1y * cross_x;
        determinant = basis_x * e2x + basis_y * e2y + basis_z * e2z;
        if determinant == 0.0 {
            determinant = 1.0;
        }
        scale = 1.0_f32 / determinant;
        uv[1] = (basis_x * off_a.0 + basis_y * off_a.1 + basis_z * off_a.2) * scale;
        uv[3] = (basis_x * off_b.0 + basis_y * off_b.1 + basis_z * off_b.2) * scale;
        uv[5] = (basis_x * off_c.0 + basis_y * off_c.1 + basis_z * off_c.2) * scale;
        return Ok(uv);
    }
    let Some(transforms) = transforms else {
        return Ok(uv);
    };
    let t = texture as usize;
    if !transforms.present.get(t).copied().unwrap_or(false) {
        return Ok(uv);
    }
    let matrix = &transforms.matrix[t];
    let (cx, cy, cz) = (
        transforms.center_x[t],
        transforms.center_y[t],
        transforms.center_z[t],
    );
    let direction = opt_get(&mesh.textures_direction, texture)?.map_or(0, i32::from);
    let speed = scale_param(&mesh.textures_speed, texture, 0) as f32 / 256.0_f32;
    let delta = |v: i64| -> Res<(f32, f32, f32)> {
        let (x, y, z) = vertices.at(v)?;
        Ok((
            x.wrapping_sub(cx) as f32,
            y.wrapping_sub(cy) as f32,
            z.wrapping_sub(cz) as f32,
        ))
    };
    let corners = [a, b, c];
    match kind {
        1 => {
            let offset = scale_param(&mesh.textures_scale_z, texture, 1024) as f32 / 1024.0_f32;
            for (slot, vertex) in corners.into_iter().enumerate() {
                let (dx, dy, dz) = delta(vertex)?;
                let y = dx * matrix[0] + dy * matrix[1] + dz * matrix[2];
                let height = dx * matrix[3] + dy * matrix[4] + dz * matrix[5];
                let x = dx * matrix[6] + dy * matrix[7] + dz * matrix[8];
                let mut u = (f64::from(y).atan2(f64::from(x)) as f32) / 6.283_185_5_f32 + 0.5_f32;
                if offset != 1.0 {
                    u *= offset;
                }
                let v = height + 0.5_f32 + speed;
                let (ru, rv) = apply_direction(direction, u, v);
                uv[slot * 2] = ru;
                uv[slot * 2 + 1] = rv;
            }
            wrap_uv(&mut uv, direction, offset / 2.0_f32, offset);
        }
        2 => {
            let u_trans = scale_param(&mesh.textures_utrans, texture, 0) as f32 / 256.0_f32;
            let v_trans = scale_param(&mesh.textures_vtrans, texture, 0) as f32 / 256.0_f32;
            let (ax, ay, az) = vertices.at(a)?;
            let (bx, by, bz) = vertices.at(b)?;
            let (qx, qy, qz) = vertices.at(c)?;
            let e1 = (
                bx.wrapping_sub(ax),
                by.wrapping_sub(ay),
                bz.wrapping_sub(az),
            );
            let e2 = (
                qx.wrapping_sub(ax),
                qy.wrapping_sub(ay),
                qz.wrapping_sub(az),
            );
            let cross_x =
                e1.1.wrapping_mul(e2.2)
                    .wrapping_sub(e2.1.wrapping_mul(e1.2)) as f32;
            let cross_y =
                e1.2.wrapping_mul(e2.0)
                    .wrapping_sub(e2.2.wrapping_mul(e1.0)) as f32;
            let cross_z =
                e1.0.wrapping_mul(e2.1)
                    .wrapping_sub(e2.0.wrapping_mul(e1.1)) as f32;
            let scale_x =
                64.0_f32 / non_zero(scale_param(&mesh.textures_scale_x, texture, 1)) as f32;
            let scale_y =
                64.0_f32 / non_zero(scale_param(&mesh.textures_scale_y, texture, 1)) as f32;
            let scale_z =
                64.0_f32 / non_zero(scale_param(&mesh.textures_scale_z, texture, 1)) as f32;
            let nx = (cross_x * matrix[0] + cross_y * matrix[1] + cross_z * matrix[2]) / scale_x;
            let ny = (cross_x * matrix[3] + cross_y * matrix[4] + cross_z * matrix[5]) / scale_y;
            let nz = (cross_x * matrix[6] + cross_y * matrix[7] + cross_z * matrix[8]) / scale_z;
            let (abs_x, abs_y, abs_z) = (nx.abs_java(), ny.abs_java(), nz.abs_java());
            let cube_face = if abs_y > abs_x && abs_y > abs_z {
                if ny > 0.0 {
                    0
                } else {
                    1
                }
            } else if abs_z > abs_x && abs_z > abs_y {
                if nz > 0.0 {
                    2
                } else {
                    3
                }
            } else if nx > 0.0 {
                4
            } else {
                5
            };
            for (slot, vertex) in corners.into_iter().enumerate() {
                let (dx, dy, dz) = delta(vertex)?;
                let (tx, ty, tz) = rotate(dx, dy, dz, matrix);
                let (u, v) = match cube_face {
                    0 => (tx + speed + 0.5_f32, -tz + v_trans + 0.5_f32),
                    1 => (tx + speed + 0.5_f32, tz + v_trans + 0.5_f32),
                    2 => (-tx + speed + 0.5_f32, -ty + u_trans + 0.5_f32),
                    3 => (tx + speed + 0.5_f32, -ty + u_trans + 0.5_f32),
                    4 => (tz + v_trans + 0.5_f32, -ty + u_trans + 0.5_f32),
                    _ => (-tz + v_trans + 0.5_f32, -ty + u_trans + 0.5_f32),
                };
                let (ru, rv) = apply_direction(direction, u, v);
                uv[slot * 2] = ru;
                uv[slot * 2 + 1] = rv;
            }
        }
        _ => {
            for (slot, vertex) in corners.into_iter().enumerate() {
                let (dx, dy, dz) = delta(vertex)?;
                let (tx, ty, tz) = rotate(dx, dy, dz, matrix);
                let dist = (tx * tx + ty * ty + tz * tz).sqrt();
                let u = (f64::from(tx).atan2(f64::from(tz)) as f32) / 6.283_185_5_f32 + 0.5_f32;
                let v =
                    (f64::from(ty / dist).asin() as f32) / std::f32::consts::PI + 0.5_f32 + speed;
                let (ru, rv) = apply_direction(direction, u, v);
                uv[slot * 2] = ru;
                uv[slot * 2 + 1] = rv;
            }
            wrap_uv(&mut uv, direction, 0.5_f32, 1.0_f32);
        }
    }
    Ok(uv)
}

trait AbsJava {
    fn abs_java(self) -> f32;
}

impl AbsJava for f32 {
    fn abs_java(self) -> f32 {
        if self < 0.0 {
            -self
        } else {
            self
        }
    }
}

struct NormalPass<'a> {
    vertices: Vertices<'a>,
    a: &'a [i16],
    b: &'a [i16],
    c: &'a [i16],
}

impl NormalPass<'_> {
    fn indices(&self, face: i64) -> Res<(i64, i64, i64)> {
        Ok((
            i64::from(get(self.a, face)? as u16),
            i64::from(get(self.b, face)? as u16),
            i64::from(get(self.c, face)? as u16),
        ))
    }

    fn face_normal(&self, face: i64) -> Res<[f32; 3]> {
        let (a, b, c) = self.indices(face)?;
        let (ax, ay, az) = self.vertices.at(a)?;
        let (bx, by, bz) = self.vertices.at(b)?;
        let (cx, cy, cz) = self.vertices.at(c)?;
        let (dx1, dy1, dz1) = (
            bx.wrapping_sub(ax),
            by.wrapping_sub(ay),
            bz.wrapping_sub(az),
        );
        let (dx2, dy2, dz2) = (
            cx.wrapping_sub(ax),
            cy.wrapping_sub(ay),
            cz.wrapping_sub(az),
        );
        let mut x = dy1.wrapping_mul(dz2).wrapping_sub(dy2.wrapping_mul(dz1));
        let mut y = dz1.wrapping_mul(dx2).wrapping_sub(dz2.wrapping_mul(dx1));
        let mut z = dx1.wrapping_mul(dy2).wrapping_sub(dx2.wrapping_mul(dy1));
        while x > 8192 || y > 8192 || z > 8192 || x < -8192 || y < -8192 || z < -8192 {
            x >>= 1;
            y >>= 1;
            z >>= 1;
        }
        let squared = x
            .wrapping_mul(x)
            .wrapping_add(y.wrapping_mul(y))
            .wrapping_add(z.wrapping_mul(z));
        let mut magnitude = f64::from(squared).sqrt() as i32;
        if magnitude <= 0 {
            magnitude = 1;
        }
        Ok([
            (x.wrapping_mul(256) / magnitude) as f32,
            (y.wrapping_mul(256) / magnitude) as f32,
            (z.wrapping_mul(256) / magnitude) as f32,
        ])
    }
}

fn content_hash(vertex_data: &[u8], batches: &[TextureBatch]) -> i64 {
    let mut hash: i64 = -0x340d_631b_7bdd_dcdb_i64;
    for byte in vertex_data {
        hash ^= i64::from(*byte);
        hash = hash.wrapping_mul(0x0000_0100_0000_01b3);
    }
    for batch in batches {
        hash ^= i64::from(batch.material_id);
        hash = hash.wrapping_mul(0x0000_0100_0000_01b3);
    }
    hash
}

pub fn build(mesh: &Mesh, input: &BuildInput) -> Res<Option<BuiltModel>> {
    let num_faces = mesh.num_faces;
    let num_vertices = mesh.num_vertices;
    if num_faces <= 0 || num_vertices <= 0 {
        return Ok(None);
    }
    let (Some(faces_a), Some(faces_b), Some(faces_c)) =
        (&mesh.faces_a, &mesh.faces_b, &mesh.faces_c)
    else {
        return Ok(None);
    };
    let (Some(vx), Some(vy), Some(vz)) = (&mesh.vertices_x, &mesh.vertices_y, &mesh.vertices_z)
    else {
        return Ok(None);
    };
    if input.hsl_to_rgb.is_empty() {
        return Ok(None);
    }
    let materials = input.materials;
    let pass = NormalPass {
        vertices: Vertices {
            x: vx,
            y: vy,
            z: vz,
        },
        a: faces_a,
        b: faces_b,
        c: faces_c,
    };
    let count = num_vertices as usize;
    let limit = i64::from(num_vertices);
    let mut smooth = vec![[0i32; 3]; count];
    let mut smooth_count = vec![0i32; count];
    let contrast = if input.contrast == 0 {
        768
    } else {
        input.contrast
    };
    let flat_scale = 2.0_f32 / contrast as f32;
    let smooth_scale = 3.0_f32 / contrast as f32;
    let render_type =
        |face: i64| -> Res<i32> { Ok(opt_get(&mesh.face_render_type, face)?.map_or(0, i32::from)) };
    for face in 0..i64::from(num_faces) {
        if render_type(face)? != 0 {
            continue;
        }
        let (a, b, c) = pass.indices(face)?;
        if a >= limit || b >= limit || c >= limit {
            continue;
        }
        let normal = pass.face_normal(face)?;
        let n = [normal[0] as i32, normal[1] as i32, normal[2] as i32];
        for vertex in [a, b, c] {
            let slot = vertex as usize;
            for axis in 0..3 {
                smooth[slot][axis] = smooth[slot][axis].wrapping_add(n[axis]);
            }
            smooth_count[slot] += 1;
        }
    }
    let mut hidden: Option<HashSet<i32>> = None;
    let (mut billboards_seen, mut billboards_with_uid, mut billboards_out_of_range) = (0, 0, 0);
    if let Some(list) = &mesh.billboards {
        for billboard in list {
            billboards_seen += 1;
            if billboard.applies_on_face < 0 || billboard.applies_on_face >= num_faces {
                billboards_out_of_range += 1;
                continue;
            }
            if (input.billboard_has_uid)(billboard.billboard_id) {
                billboards_with_uid += 1;
                hidden
                    .get_or_insert_with(HashSet::new)
                    .insert(billboard.applies_on_face);
            }
        }
    }
    let mut accepted_faces: Vec<i32> = Vec::new();
    let mut accepted_materials: Vec<i32> = Vec::new();
    let (mut skip_billboard_hidden, mut skip_render_type, mut skip_invalid_vertex) = (0, 0, 0);
    let mut skip_material_filtered = 0;
    for face in 0..num_faces {
        let f = i64::from(face);
        if hidden.as_ref().is_some_and(|set| set.contains(&face)) {
            skip_billboard_hidden += 1;
            continue;
        }
        if render_type(f)? == 2 {
            skip_render_type += 1;
            continue;
        }
        let (a, b, c) = pass.indices(f)?;
        if a >= limit || b >= limit || c >= limit {
            skip_invalid_vertex += 1;
            continue;
        }
        let mut material_id = opt_get(&mesh.faces_material, f)?.map_or(-1, i32::from);
        if material_id != -1 {
            if let Some(material) = material_of(materials, material_id)? {
                if input.strip_detail_layers && material.details_only {
                    material_id = -1;
                } else if material.skip_triangles {
                    skip_material_filtered += 1;
                    continue;
                }
            }
        }
        accepted_faces.push(face);
        accepted_materials.push(material_id);
    }
    let accepted_count = accepted_faces.len();
    if accepted_count == 0 {
        return Ok(None);
    }
    let mut keys = Vec::with_capacity(accepted_count);
    for index in 0..accepted_count {
        let face = i64::from(accepted_faces[index]);
        let material_id = accepted_materials[index];
        let mut alpha_face = opt_get(&mesh.faces_alpha, face)?.map_or(0, i32::from) != 0;
        if !alpha_face && material_id >= 0 {
            if let Some(material) = material_of(materials, material_id)? {
                if material.effect_combiner != 0 {
                    alpha_face = true;
                }
            }
        }
        let key =
            (if alpha_face { 1 << 24 } else { 0 }) | (material_id.wrapping_add(1) & 0x00ff_ffff);
        keys.push(key);
    }
    let mut order: Vec<usize> = (0..accepted_count).collect();
    order.sort_by_key(|&index| keys[index]);
    let faces: Vec<i32> = order.iter().map(|&i| accepted_faces[i]).collect();
    let material_ids: Vec<i32> = order.iter().map(|&i| accepted_materials[i]).collect();
    let transforms = if mesh.faces_texture.is_some() && mesh.num_textures > 0 {
        Some(compute_transforms(mesh)?)
    } else {
        None
    };
    let vertex_count = accepted_count * 3;
    let mut data: Vec<u8> = Vec::with_capacity(vertex_count * BYTES_PER_VERTEX);
    let mut write_vertex = |vertex: i64, normal: [f32; 3], u: f32, v: f32, rgba: i32| -> Res<()> {
        let (x, y, z) = pass.vertices.at(vertex)?;
        for value in [
            x as f32, y as f32, z as f32, normal[0], normal[1], normal[2], u, v,
        ] {
            data.extend_from_slice(&value.to_le_bytes());
        }
        let bits = rgba as u32;
        data.extend_from_slice(&[
            (bits >> 24) as u8,
            (bits >> 16) as u8,
            (bits >> 8) as u8,
            bits as u8,
        ]);
        data.extend_from_slice(&0.0_f32.to_le_bytes());
        Ok(())
    };
    for index in 0..accepted_count {
        let face = faces[index];
        let f = i64::from(face);
        let material_id = material_ids[index];
        let (a, b, c) = pass.indices(f)?;
        let uv = compute_uvs(mesh, transforms.as_ref(), f, material_id)?;
        let hsl = opt_get(&mesh.faces_color, f)?.map_or(0, |v| i32::from(v as u16));
        let alpha = opt_get(&mesh.faces_alpha, f)?.map_or(0, |v| i32::from(v as u8));
        let rgba = face_color_rgba(
            input.hsl_to_rgb,
            hsl,
            material_id,
            input.ambient,
            alpha,
            materials,
        )?;
        if render_type(f)? == 0 {
            let normal_for = |vertex: i64| -> [f32; 3] {
                let slot = vertex as usize;
                let n = smooth_count[slot];
                let k = if n == 0 {
                    0.0_f32
                } else {
                    smooth_scale / n as f32
                };
                [
                    smooth[slot][0] as f32 * k,
                    smooth[slot][1] as f32 * k,
                    smooth[slot][2] as f32 * k,
                ]
            };
            write_vertex(a, normal_for(a), uv[0], uv[1], rgba)?;
            write_vertex(b, normal_for(b), uv[2], uv[3], rgba)?;
            write_vertex(c, normal_for(c), uv[4], uv[5], rgba)?;
        } else {
            let n = pass.face_normal(f)?;
            let flat = [n[0] * flat_scale, n[1] * flat_scale, n[2] * flat_scale];
            write_vertex(a, flat, uv[0], uv[1], rgba)?;
            write_vertex(b, flat, uv[2], uv[3], rgba)?;
            write_vertex(c, flat, uv[4], uv[5], rgba)?;
        }
    }
    let mut batches: Vec<TextureBatch> = Vec::new();
    let normalized = |value: i32| if value < 0 { -1 } else { value };
    let mut start = 0;
    while start < accepted_count {
        let material_id = normalized(material_ids[start]);
        let mut end = start + 1;
        while end < accepted_count && normalized(material_ids[end]) == material_id {
            end += 1;
        }
        let mut indices = Vec::with_capacity((end - start) * 3);
        for index in start..end {
            let base = (index * 3) as u32;
            indices.extend_from_slice(&[base, base + 1, base + 2]);
        }
        batches.push(TextureBatch {
            material_id,
            indices,
        });
        start = end;
    }
    Ok(Some(BuiltModel {
        id: content_hash(&data, &batches),
        face_count: accepted_count as i32,
        vertex_count: vertex_count as i32,
        vertex_data: data,
        batches,
        skip_billboard_hidden,
        skip_render_type,
        skip_invalid_vertex,
        skip_transparent: 0,
        source_face_count: num_faces,
        billboards_seen,
        billboards_with_uid,
        billboards_out_of_range,
        skip_material_filtered,
        accepted_faces: faces,
        accepted_material_ids: material_ids,
    }))
}
