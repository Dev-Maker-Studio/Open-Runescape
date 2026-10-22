use crate::mesh::Mesh;
use crate::reader::{at, at_slice, put_slice, ModelError, R};
use openrs_scene::material::{Material, MaterialTable};
use openrs_scene::model_build::face_color_rgba;
use std::collections::HashMap;

pub const MASK_ALTERS_VERTICES: i32 = 0x20;
pub const MASK_ALTERS_COLOR: i32 = 0x80;
pub const MASK_ALTERS_ALPHA: i32 = 0x100;
pub const MASK_ALTERS_BILLBOARDS: i32 = 0x400;
pub const MASK_SHADED: i32 = 0x10000;
pub const PREFERENCE_DISABLE_TEXTURES: i32 = 0x40;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct BillboardType {
    pub has_uid: bool,
    pub material_id: i32,
}

pub struct Env<'a> {
    pub materials: &'a MaterialTable,
    pub hsl_to_rgb: &'a [u32],
    pub billboards: &'a HashMap<i32, BillboardType>,
}

impl Env<'_> {
    fn material(&self, id: i32) -> R<Material> {
        self.materials
            .get_material(id)
            .copied()
            .map_err(|_| ModelError::Invalid)
    }

    fn billboard(&self, id: i32) -> R<BillboardType> {
        self.billboards.get(&id).copied().ok_or(ModelError::Null)
    }
}

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct Released {
    pub vertex_index: bool,
    pub normals: bool,
    pub uv: bool,
    pub color: bool,
    pub alpha: bool,
    pub material: bool,
    pub faces: bool,
    pub batch_offsets: bool,
}

fn add_normal(target: &mut MergedNormals, index: i64, add: [i32; 4]) -> R<()> {
    let i = usize::try_from(index).map_err(|_| ModelError::Bounds)?;
    if i >= target.normals_x.len() || i >= target.magnitudes.len() {
        return Err(ModelError::Bounds);
    }
    target.normals_x[i] = target.normals_x[i].wrapping_add(add[0] as i16);
    target.normals_y[i] = target.normals_y[i].wrapping_add(add[1] as i16);
    target.normals_z[i] = target.normals_z[i].wrapping_add(add[2] as i16);
    target.magnitudes[i] = target.magnitudes[i].wrapping_add(add[3] as i8);
    Ok(())
}

fn live<T: Copy>(released: bool, values: &[T], index: i64) -> R<T> {
    if released {
        Err(ModelError::Null)
    } else {
        at_slice(values, index)
    }
}

fn vertex_of(values: &Option<Vec<i32>>, index: usize) -> R<i32> {
    at_slice(values.as_ref().ok_or(ModelError::Null)?, index as i64)
}

fn first_normal(model: &Model, vertex: usize) -> R<(i32, i32, i32)> {
    let gone = model.released.vertex_index;
    let start = live(gone, &model.triangles, vertex as i64)?;
    let end = live(gone, &model.triangles, vertex as i64 + 1)?;
    let mut normal = -1i32;
    for slot in start..end {
        normal = i32::from(live(gone, &model.vertex_normal_indices, i64::from(slot))?) - 1;
        if normal == -1 || live(model.released.normals, &model.magnitudes, i64::from(normal))? != 0
        {
            break;
        }
    }
    Ok((normal, start, end))
}

fn normal_of(model: &Model, index: i64) -> R<[i32; 4]> {
    let gone = model.released.normals;
    Ok([
        i32::from(live(gone, &model.normals_x, index)?),
        i32::from(live(gone, &model.normals_y, index)?),
        i32::from(live(gone, &model.normals_z, index)?),
        i32::from(live(gone, &model.magnitudes, index)?),
    ])
}

fn copy_normals(model: &Model) -> MergedNormals {
    MergedNormals {
        normals_x: model.normals_x.clone(),
        normals_y: model.normals_y.clone(),
        normals_z: model.normals_z.clone(),
        magnitudes: model.magnitudes.clone(),
    }
}

fn accumulate(
    model: &Model,
    target: &mut MergedNormals,
    start: i32,
    end: i32,
    add: [i32; 4],
) -> R<()> {
    let gone = model.released.vertex_index;
    for slot in start..end {
        let index = i32::from(live(gone, &model.vertex_normal_indices, i64::from(slot))?) - 1;
        if index == -1 {
            break;
        }
        if at_slice(&target.magnitudes, i64::from(index))? != 0 {
            add_normal(target, i64::from(index), add)?;
        }
    }
    Ok(())
}

fn shade_across(
    this: &Model,
    other: &Model,
    offset: [i32; 3],
    bounds: [i32; 6],
    depths: [usize; 2],
    merged: (&mut Option<MergedNormals>, &mut Option<MergedNormals>),
) -> R<()> {
    let (merged, other_merged) = merged;
    let [x, y, z] = offset;
    let [min_x, max_x, min_y, max_y, min_z, max_z] = bounds;
    for vertex in 0..depths[0] {
        let vy = vertex_of(&this.vertices_y, vertex)?.wrapping_sub(y);
        if vy < min_y || vy > max_y {
            continue;
        }
        let vx = vertex_of(&this.vertices_x, vertex)?.wrapping_sub(x);
        if vx < min_x || vx > max_x {
            continue;
        }
        let vz = vertex_of(&this.vertices_z, vertex)?.wrapping_sub(z);
        if vz < min_z || vz > max_z {
            continue;
        }
        let (this_normal, start, end) = first_normal(this, vertex)?;
        if this_normal == -1 {
            continue;
        }
        for candidate in 0..depths[1] {
            if vx != vertex_of(&other.vertices_x, candidate)?
                || vz != vertex_of(&other.vertices_z, candidate)?
                || vy != vertex_of(&other.vertices_y, candidate)?
            {
                continue;
            }
            let (other_normal, other_start, other_end) = first_normal(other, candidate)?;
            if other_normal == -1 {
                continue;
            }
            let m = merged.get_or_insert_with(|| copy_normals(this));
            let om = other_merged.get_or_insert_with(|| copy_normals(other));
            let t = normal_of(this, i64::from(this_normal))?;
            accumulate(other, om, other_start, other_end, t)?;
            let o = normal_of(other, i64::from(other_normal))?;
            accumulate(this, m, start, end, o)?;
        }
    }
    Ok(())
}

fn has_textures(mask: i32) -> bool {
    (mask & 0x800) != 0
}

fn has_normal_buffer(mask: i32, preference: i32) -> bool {
    has_textures(mask) && (preference & 0x37) != 0
}

fn alters_color(mask: i32) -> bool {
    (mask & (0x80000 | 0x4000 | 0x80)) != 0
}

fn alters_alpha(mask: i32) -> bool {
    (mask & (0x100000 | 0x100)) != 0
}

fn alters_facing(mask: i32) -> bool {
    (mask & 0x10) != 0
}

fn alters_shading(mask: i32) -> bool {
    (mask & 0x10000) != 0
}

fn alters_light(mask: i32) -> bool {
    (mask & (0x200 | 0x20)) == 544 || (mask & (0x10 | 0x8)) != 0
}

fn alters_x(mask: i32) -> bool {
    (mask & (0x20 | 0x1)) != 0
}

fn alters_y(mask: i32) -> bool {
    (mask & (0x20 | 0x2)) != 0
}

fn alters_z(mask: i32) -> bool {
    (mask & (0x20 | 0x4 | 0x10)) != 0
}

fn alters_any_vertex(mask: i32) -> bool {
    (alters_x(mask) || alters_y(mask) || alters_z(mask)) && has_textures(mask)
}

fn requires_color_rebuild(mask: i32, preference: i32) -> bool {
    has_textures(mask)
        && ((mask & 0xb000) != 0
            || alters_color(mask)
            || alters_alpha(mask)
            || ((preference & 0x37) == 0 && (alters_light(mask) || alters_shading(mask))))
}

fn requires_normals_rebuild(mask: i32, preference: i32) -> bool {
    ((mask & 0x2000) != 0 || alters_light(mask) || alters_shading(mask))
        && has_normal_buffer(mask, preference)
}

fn retains_normals(mask: i32, preference: i32) -> bool {
    alters_shading(mask)
        || alters_light(mask)
        || requires_normals_rebuild(mask, preference)
        || ((preference & 0x37) == 0 && requires_color_rebuild(mask, preference))
}

fn retains_x(mask: i32) -> bool {
    (mask & 0x70000) != 0 || alters_x(mask) || alters_any_vertex(mask)
}

fn retains_y(mask: i32) -> bool {
    (mask & 0x70000) != 0 || alters_y(mask) || alters_any_vertex(mask)
}

fn retains_z(mask: i32) -> bool {
    (mask & 0x70000) != 0 || alters_z(mask) || alters_any_vertex(mask)
}

#[derive(Debug, Clone, PartialEq, Default)]
pub struct MergedNormals {
    pub normals_x: Vec<i16>,
    pub normals_y: Vec<i16>,
    pub normals_z: Vec<i16>,
    pub magnitudes: Vec<i8>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Model {
    pub function_mask: i32,
    pub preference_flags: i32,
    pub num_faces: usize,
    pub rendered_face_count: usize,
    pub mesh_face_indices: Vec<i32>,
    pub num_vertices: i32,
    pub max_depth: i32,
    pub vertices_x: Option<Vec<i32>>,
    pub vertices_y: Option<Vec<i32>>,
    pub vertices_z: Option<Vec<i32>>,
    pub triangles: Vec<i32>,
    pub normals_x: Vec<i16>,
    pub normals_y: Vec<i16>,
    pub normals_z: Vec<i16>,
    pub magnitudes: Vec<i8>,
    pub merged_normals: Option<MergedNormals>,
    pub texture_u: Vec<f32>,
    pub texture_v: Vec<f32>,
    pub vertex_normal_indices: Vec<i16>,
    pub faces_color: Vec<i16>,
    pub faces_alpha: Vec<i8>,
    pub faces_a: Vec<i16>,
    pub faces_b: Vec<i16>,
    pub faces_c: Vec<i16>,
    pub faces_material: Vec<i16>,
    pub faces_submeshes: Option<Vec<i16>>,
    pub material_batch_offsets: Vec<i32>,
    pub ambient: i16,
    pub contrast: i16,
    pub num_normals: usize,
    pub has_alpha_transition: bool,
    pub texture_animating: bool,
    pub vertex_groups: Option<Vec<Vec<i32>>>,
    pub billboard_groups: Option<Vec<Vec<i32>>>,
    pub face_groups: Option<Vec<Vec<i32>>>,
    pub released: Released,
    pub billboard_count: usize,
    pins_vertices: bool,
    cached_bounds: Option<[i32; 6]>,
    ctor_textured: bool,
    ctor_normal_buffer: bool,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Batch {
    pub material_id: i32,
    pub indices: Vec<i32>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Built {
    pub vertex_words: Vec<i32>,
    pub vertex_count: usize,
    pub face_count: usize,
    pub batches: Vec<Batch>,
}

struct TexMatrix {
    center_x: Vec<i32>,
    center_y: Vec<i32>,
    center_z: Vec<i32>,
    matrix: Vec<Vec<f32>>,
}

fn quicksort(longs: &mut [i64], ints: &mut [i32], start: i64, end: i64) {
    if start < end {
        let (s, e) = (start as usize, end as usize);
        let center = ((end + start) / 2) as usize;
        let offset = longs[center];
        longs.swap(center, e);
        let pivot = ints[center];
        ints.swap(center, e);
        let mask: i64 = if offset == i64::MAX { 0 } else { 1 };
        let mut current = s;
        for index in s..e {
            if longs[index] < ((index as i64) & mask).wrapping_add(offset) {
                longs.swap(index, current);
                ints.swap(index, current);
                current += 1;
            }
        }
        longs[e] = longs[current];
        longs[current] = offset;
        ints[e] = ints[current];
        ints[current] = pivot;
        quicksort(longs, ints, start, current as i64 - 1);
        quicksort(longs, ints, current as i64 + 1, end);
    }
}

fn len_of(value: i32) -> R<usize> {
    usize::try_from(value).map_err(|_| ModelError::Invalid)
}

fn vtx(values: &Option<Vec<i32>>, index: i16) -> R<i32> {
    at(values, i64::from(index))
}

fn vtxf(values: &Option<Vec<i32>>, index: i16) -> R<f32> {
    Ok(at(values, i64::from(index))? as f32)
}

fn ksin(angle: f32) -> f32 {
    libm::sin(f64::from(angle)) as f32
}

fn kcos(angle: f32) -> f32 {
    libm::cos(f64::from(angle)) as f32
}

fn ksqrt(value: f32) -> f32 {
    f64::from(value).sqrt() as f32
}

fn create_texture_matrix(
    n: i32,
    m: i32,
    p: i32,
    rotation: i32,
    sx: f32,
    sy: f32,
    sz: f32,
) -> Vec<f32> {
    let angle = rotation as f32 * 0.024543693f32;
    let cos_r = kcos(angle);
    let sin_r = ksin(angle);
    let rotation_map = [cos_r, 0.0, sin_r, 0.0, 1.0, 0.0, -sin_r, 0.0, cos_r];
    let mut result = [0.0f32; 9];
    let mut x = 1.0f32;
    let mut y = 0.0f32;
    let cos = m as f32 / 32767.0f32;
    let sin = -ksqrt(1.0f32 - cos * cos);
    let omc = 1.0f32 - cos;
    let size = ksqrt(p.wrapping_mul(p).wrapping_add(n.wrapping_mul(n)) as f32);
    if size == 0.0 && cos == 0.0 {
        result = rotation_map;
    } else {
        if size != 0.0 {
            x = (-p) as f32 / size;
            y = n as f32 / size;
        }
        let a = [
            cos + x * x * omc,
            y * sin,
            y * x * omc,
            -y * sin,
            cos,
            x * sin,
            x * y * omc,
            -x * sin,
            cos + y * y * omc,
        ];
        for row in 0..3 {
            for col in 0..3 {
                result[row * 3 + col] = rotation_map[row * 3] * a[col]
                    + rotation_map[row * 3 + 1] * a[3 + col]
                    + rotation_map[row * 3 + 2] * a[6 + col];
            }
        }
    }
    for (i, value) in result.iter_mut().enumerate() {
        *value *= [sx, sy, sz][i / 3];
    }
    result.to_vec()
}

fn generate_texture_matrix(mesh: &Mesh, faces: &[i32], num_faces: usize) -> R<Option<TexMatrix>> {
    if mesh.faces_texture.is_none() {
        return Ok(None);
    }
    let count = len_of(mesh.num_textures)?;
    let mut min = [
        vec![i32::MAX; count],
        vec![i32::MAX; count],
        vec![i32::MAX; count],
    ];
    let mut max = [
        vec![-2147483647; count],
        vec![-2147483647; count],
        vec![-2147483647; count],
    ];
    for &face in &faces[..num_faces] {
        let texture = at(&mesh.faces_texture, i64::from(face))?;
        if texture != -1 {
            let index = i64::from(texture) & 0xff;
            for axis in 0..3 {
                let vertex = match axis {
                    0 => at(&mesh.faces_a, i64::from(face))?,
                    1 => at(&mesh.faces_b, i64::from(face))?,
                    _ => at(&mesh.faces_c, i64::from(face))?,
                };
                let values = [
                    vtx(&mesh.vertices_x, vertex)?,
                    vtx(&mesh.vertices_y, vertex)?,
                    vtx(&mesh.vertices_z, vertex)?,
                ];
                for (c, value) in values.iter().enumerate() {
                    if *value < at_slice(&min[c], index)? {
                        put_slice(&mut min[c], index, *value)?;
                    }
                    if *value > at_slice(&max[c], index)? {
                        put_slice(&mut max[c], index, *value)?;
                    }
                }
            }
        }
    }
    let mut center = [vec![0i32; count], vec![0i32; count], vec![0i32; count]];
    let mut matrix: Vec<Vec<f32>> = vec![Vec::new(); count];
    for i in 0..count {
        let mapping = at(&mesh.textures_mapping, i as i64)?;
        if mapping > 0 {
            for c in 0..3 {
                center[c][i] = min[c][i].wrapping_add(max[c][i]) / 2;
            }
            let (sx, sy, sz);
            if mapping == 1 {
                let x = at(&mesh.textures_scale_x, i as i64)?;
                if x == 0 {
                    sx = 1.0f32;
                    sz = 1.0f32;
                } else if x > 0 {
                    sx = 1.0f32;
                    sz = x as f32 / 1024.0f32;
                } else {
                    sz = 1.0f32;
                    sx = x.wrapping_neg() as f32 / 1024.0f32;
                }
                sy = 64.0f32 / at(&mesh.textures_scale_y, i as i64)? as f32;
            } else if mapping == 2 {
                sx = 64.0f32 / at(&mesh.textures_scale_x, i as i64)? as f32;
                sy = 64.0f32 / at(&mesh.textures_scale_y, i as i64)? as f32;
                sz = 64.0f32 / at(&mesh.textures_scale_z, i as i64)? as f32;
            } else {
                sx = at(&mesh.textures_scale_x, i as i64)? as f32 / 1024.0f32;
                sy = at(&mesh.textures_scale_y, i as i64)? as f32 / 1024.0f32;
                sz = at(&mesh.textures_scale_z, i as i64)? as f32 / 1024.0f32;
            }
            matrix[i] = create_texture_matrix(
                i32::from(at(&mesh.texture_face_x, i as i64)?),
                i32::from(at(&mesh.texture_face_y, i as i64)?),
                i32::from(at(&mesh.texture_face_z, i as i64)?),
                i32::from(at(&mesh.textures_rotation, i as i64)?) & 0xff,
                sx,
                sy,
                sz,
            );
        }
    }
    let [center_x, center_y, center_z] = center;
    Ok(Some(TexMatrix {
        center_x,
        center_y,
        center_z,
        matrix,
    }))
}

fn project(vertex: [i32; 3], center: [i32; 3], matrix: &[f32]) -> R<[f32; 3]> {
    let v = [
        vertex[0].wrapping_sub(center[0]) as f32,
        vertex[1].wrapping_sub(center[1]) as f32,
        vertex[2].wrapping_sub(center[2]) as f32,
    ];
    let m = |i: i64| at_slice(matrix, i);
    Ok([
        v[0] * m(0)? + v[1] * m(1)? + v[2] * m(2)?,
        v[0] * m(3)? + v[1] * m(4)? + v[2] * m(5)?,
        v[0] * m(6)? + v[1] * m(7)? + v[2] * m(8)?,
    ])
}

fn apply_direction(u: f32, v: f32, direction: i32) -> (f32, f32) {
    match direction {
        1 => (-v, u),
        2 => (-u, -v),
        3 => (v, -u),
        _ => (u, v),
    }
}

fn map_cylindrical(
    vertex: [i32; 3],
    center: [i32; 3],
    matrix: &[f32],
    offset: f32,
    direction: i32,
    speed: f32,
) -> R<(f32, f32)> {
    let [y, height, x] = project(vertex, center, matrix)?;
    let mut u = libm::atan2(f64::from(y), f64::from(x)) as f32 / 6.2831855f32 + 0.5f32;
    if offset != 1.0 {
        u *= offset;
    }
    let v = height + 0.5f32 + speed;
    Ok(apply_direction(u, v, direction))
}

fn map_spherical(
    vertex: [i32; 3],
    center: [i32; 3],
    matrix: &[f32],
    direction: i32,
    speed: f32,
) -> R<(f32, f32)> {
    let [tx, ty, tz] = project(vertex, center, matrix)?;
    let dist = ksqrt(tx * tx + ty * ty + tz * tz);
    let u = libm::atan2(f64::from(tx), f64::from(tz)) as f32 / 6.2831855f32 + 0.5f32;
    let v = libm::asin(f64::from(ty / dist)) as f32 / std::f32::consts::PI + 0.5f32 + speed;
    Ok(apply_direction(u, v, direction))
}

#[allow(clippy::too_many_arguments)]
fn map_rectangular(
    vertex: [i32; 3],
    center: [i32; 3],
    cube: i32,
    matrix: &[f32],
    direction: i32,
    speed: f32,
    u_trans: f32,
    v_trans: f32,
) -> R<(f32, f32)> {
    let [tx, ty, tz] = project(vertex, center, matrix)?;
    let (u, v) = match cube {
        0 => (tx + speed + 0.5f32, -tz + v_trans + 0.5f32),
        1 => (tx + speed + 0.5f32, tz + v_trans + 0.5f32),
        2 => (-tx + speed + 0.5f32, -ty + u_trans + 0.5f32),
        3 => (tx + speed + 0.5f32, -ty + u_trans + 0.5f32),
        4 => (tz + v_trans + 0.5f32, -ty + u_trans + 0.5f32),
        _ => (-tz + v_trans + 0.5f32, -ty + u_trans + 0.5f32),
    };
    Ok(apply_direction(u, v, direction))
}

fn cube_face(nx: f32, ny: f32, nz: f32) -> i32 {
    let ax = if nx < 0.0 { -nx } else { nx };
    let ay = if ny < 0.0 { -ny } else { ny };
    let az = if nz < 0.0 { -nz } else { nz };
    if ay > ax && ay > az {
        if ny > 0.0 {
            0
        } else {
            1
        }
    } else if az > ax && az > ay {
        if nz > 0.0 {
            2
        } else {
            3
        }
    } else if nx > 0.0 {
        4
    } else {
        5
    }
}

fn wrap_pair(a: &mut f32, other: f32, half: f32, shift: f32) -> i64 {
    if *a - other > half {
        *a -= shift;
        1
    } else if other - *a > half {
        *a += shift;
        2
    } else {
        0
    }
}

struct Uvs {
    a: (f32, f32),
    b: (f32, f32),
    c: (f32, f32),
    offset_b: i64,
    offset_c: i64,
    cube: i32,
}

impl Model {
    pub fn new(
        env: &Env,
        mesh: &Mesh,
        function_mask: i32,
        ambient: i32,
        contrast: i32,
        preference_flags: i32,
    ) -> R<Model> {
        let mesh_faces = len_of(mesh.num_faces)?;
        let max_depth = mesh.max_depth;
        let mut model = Model {
            function_mask,
            preference_flags,
            num_faces: 0,
            rendered_face_count: 0,
            mesh_face_indices: Vec::new(),
            num_vertices: mesh.num_vertices,
            max_depth,
            vertices_x: mesh.vertices_x.clone(),
            vertices_y: mesh.vertices_y.clone(),
            vertices_z: mesh.vertices_z.clone(),
            triangles: vec![0; len_of(max_depth)? + 1],
            normals_x: Vec::new(),
            merged_normals: None,
            normals_y: Vec::new(),
            normals_z: Vec::new(),
            magnitudes: Vec::new(),
            texture_u: Vec::new(),
            texture_v: Vec::new(),
            vertex_normal_indices: Vec::new(),
            faces_color: Vec::new(),
            faces_alpha: Vec::new(),
            faces_a: Vec::new(),
            faces_b: Vec::new(),
            faces_c: Vec::new(),
            faces_material: Vec::new(),
            faces_submeshes: None,
            material_batch_offsets: Vec::new(),
            ambient: ambient as i16,
            contrast: contrast as i16,
            num_normals: 0,
            has_alpha_transition: false,
            texture_animating: false,
            vertex_groups: None,
            billboard_groups: None,
            face_groups: None,
            released: Released::default(),
            billboard_count: mesh.billboards.as_ref().map_or(0, Vec::len),
            cached_bounds: None,
            pins_vertices: mesh.billboards.is_some()
                || mesh.particle_emitters.is_some()
                || mesh.particle_effectors.is_some(),
            ctor_textured: has_textures(function_mask),
            ctor_normal_buffer: has_normal_buffer(function_mask, preference_flags),
        };
        let mut faces = vec![0i32; mesh_faces];
        for face in 0..mesh.num_faces {
            if mesh.face_render_type.is_some() && at(&mesh.face_render_type, i64::from(face))? == 2
            {
                continue;
            }
            if mesh.faces_material.is_some() {
                let id = at(&mesh.faces_material, i64::from(face))?;
                if id != -1 {
                    let material = env.material(i32::from(id) & 0xffff)?;
                    if ((preference_flags & PREFERENCE_DISABLE_TEXTURES) == 0
                        || !material.details_only)
                        && material.skip_triangles
                    {
                        continue;
                    }
                }
            }
            faces[model.num_faces] = face;
            model.num_faces += 1;
            for list in [&mesh.faces_a, &mesh.faces_b, &mesh.faces_c] {
                let vertex = at(list, i64::from(face))?;
                let slot = model
                    .triangles
                    .get_mut(usize::try_from(vertex).map_err(|_| ModelError::Bounds)?)
                    .ok_or(ModelError::Bounds)?;
                *slot += 1;
            }
        }
        model.rendered_face_count = model.num_faces;
        let selected = model.num_faces;
        let mut uids = vec![0i64; selected];
        let has_transitions = (function_mask & 0x100) != 0;
        for t in 0..selected {
            let face = faces[t];
            let mut material: Option<Material> = None;
            let mut uid_high: i32 = 0;
            let mut effect_id: i8 = 0;
            let mut effect_param1: i8 = 0;
            if let Some(billboards) = &mesh.billboards {
                let mut has_uid = false;
                for billboard in billboards {
                    if face == billboard.applies_on_face {
                        let kind = env.billboard(billboard.id)?;
                        if kind.has_uid {
                            has_uid = true;
                        }
                        if kind.material_id != -1 {
                            let used = env.material(kind.material_id)?;
                            if used.effect_combiner == 2 {
                                model.has_alpha_transition = true;
                            }
                        }
                    }
                }
                if has_uid {
                    uids[t] = i64::MAX;
                    model.rendered_face_count -= 1;
                    continue;
                }
            }
            let mut face_material_id: i16 = -1;
            if mesh.faces_material.is_some() {
                face_material_id = at(&mesh.faces_material, i64::from(face))?;
                if face_material_id != -1 {
                    let found = env.material(i32::from(face_material_id) & 0xffff)?;
                    if (preference_flags & PREFERENCE_DISABLE_TEXTURES) != 0 && found.details_only {
                        face_material_id = -1;
                    } else {
                        material = Some(found);
                        effect_id = found.effect_id;
                        effect_param1 = found.effect_param1;
                        if found.texture_speed_u != 0 || found.texture_speed_v != 0 {
                            model.texture_animating = true;
                        }
                    }
                }
            }
            let alpha_transition = (mesh.faces_alpha.is_some()
                && at(&mesh.faces_alpha, i64::from(face))? != 0)
                || material.is_some_and(|m| m.effect_combiner != 0);
            if (has_transitions || alpha_transition) && mesh.faces_layer.is_some() {
                uid_high =
                    uid_high.wrapping_add(i32::from(at(&mesh.faces_layer, i64::from(face))?) << 17);
            }
            if alpha_transition {
                uid_high = uid_high.wrapping_add(65536);
            }
            uid_high = uid_high.wrapping_add((i32::from(effect_id) & 0xff) << 8);
            uid_high = uid_high.wrapping_add(i32::from(effect_param1) & 0xff);
            let mut uid_low: i32 = (i32::from(face_material_id) & 0xffff) << 16;
            uid_low = uid_low.wrapping_add(t as i32 & 0xffff);
            uids[t] = (i64::from(uid_high) << 32).wrapping_add(i64::from(uid_low));
            model.has_alpha_transition |= alpha_transition;
            model.texture_animating |=
                material.is_some_and(|m| m.texture_speed_u != 0 || m.texture_speed_v != 0);
        }
        let last = uids.len() as i64 - 1;
        quicksort(&mut uids, &mut faces, 0, last);
        model.mesh_face_indices = faces[..selected].to_vec();
        if let Some(billboards) = &mesh.billboards {
            for billboard in billboards {
                env.billboard(billboard.id)?;
                if !faces[..selected].contains(&billboard.applies_on_face) {
                    return Err(ModelError::Invalid);
                }
                let color = at(&mesh.faces_color, i64::from(billboard.applies_on_face))?;
                at_slice(env.hsl_to_rgb, i64::from(color as u16))?;
                if mesh.faces_alpha.is_some() {
                    at(&mesh.faces_alpha, i64::from(billboard.applies_on_face))?;
                }
                at(&mesh.faces_a, i64::from(billboard.applies_on_face))?;
                at(&mesh.faces_b, i64::from(billboard.applies_on_face))?;
                at(&mesh.faces_c, i64::from(billboard.applies_on_face))?;
            }
        }
        let capacity = selected * 3;
        model.normals_x = vec![0; capacity];
        model.normals_y = vec![0; capacity];
        model.normals_z = vec![0; capacity];
        model.magnitudes = vec![0; capacity];
        model.texture_u = vec![0.0; capacity];
        model.texture_v = vec![0.0; capacity];
        model.faces_color = vec![0; selected];
        model.faces_alpha = vec![0; selected];
        model.faces_a = vec![0; selected];
        model.faces_b = vec![0; selected];
        model.faces_c = vec![0; selected];
        model.faces_material = vec![0; selected];
        if mesh.faces_submeshes.is_some() {
            model.faces_submeshes = Some(vec![0; selected]);
        }
        model.vertex_normal_indices = vec![0; capacity];
        let mut normals_uid = vec![0i64; capacity];
        let depth = len_of(max_depth)?;
        let mut vertex_normals = vec![[0i32; 4]; depth];
        let mut offset = 0i32;
        for v in 0..depth {
            let count = model.triangles[v];
            model.triangles[v] = offset;
            offset += count;
        }
        model.triangles[depth] = offset;
        let transform = generate_texture_matrix(mesh, &faces, selected)?;
        let mut face_normals: Vec<Option<[i32; 3]>> = vec![None; mesh_faces];
        for face in 0..mesh.num_faces {
            let fa = at(&mesh.faces_a, i64::from(face))?;
            let fb = at(&mesh.faces_b, i64::from(face))?;
            let fc = at(&mesh.faces_c, i64::from(face))?;
            let (xa, ya, za) = (
                vtx(&model.vertices_x, fa)?,
                vtx(&model.vertices_y, fa)?,
                vtx(&model.vertices_z, fa)?,
            );
            let dx1 = vtx(&model.vertices_x, fb)?.wrapping_sub(xa);
            let dy1 = vtx(&model.vertices_y, fb)?.wrapping_sub(ya);
            let dz1 = vtx(&model.vertices_z, fb)?.wrapping_sub(za);
            let dx2 = vtx(&model.vertices_x, fc)?.wrapping_sub(xa);
            let dy2 = vtx(&model.vertices_y, fc)?.wrapping_sub(ya);
            let dz2 = vtx(&model.vertices_z, fc)?.wrapping_sub(za);
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
            x = x.wrapping_mul(256) / magnitude;
            y = y.wrapping_mul(256) / magnitude;
            z = z.wrapping_mul(256) / magnitude;
            let render_type = if mesh.face_render_type.is_none() {
                0
            } else {
                at(&mesh.face_render_type, i64::from(face))?
            };
            if render_type == 0 {
                for vertex in [fa, fb, fc] {
                    let entry = vertex_normals
                        .get_mut(usize::try_from(vertex).map_err(|_| ModelError::Bounds)?)
                        .ok_or(ModelError::Bounds)?;
                    entry[0] = entry[0].wrapping_add(x);
                    entry[1] = entry[1].wrapping_add(y);
                    entry[2] = entry[2].wrapping_add(z);
                    entry[3] = entry[3].wrapping_add(1);
                }
            } else if render_type == 1 {
                face_normals[face as usize] = Some([x, y, z]);
            }
        }
        for t in 0..selected {
            let face = faces[t];
            let color = i32::from(at(&mesh.faces_color, i64::from(face))?) & 0xffff;
            let textured: i32 = if mesh.faces_texture.is_none() {
                -1
            } else {
                i32::from(at(&mesh.faces_texture, i64::from(face))?)
            };
            let alpha_amt: i32 = if mesh.faces_alpha.is_none() {
                0
            } else {
                i32::from(at(&mesh.faces_alpha, i64::from(face))?) & 0xff
            };
            let mut material_id: i16 = if mesh.faces_material.is_none() {
                -1
            } else {
                at(&mesh.faces_material, i64::from(face))?
            };
            if material_id != -1 && (preference_flags & PREFERENCE_DISABLE_TEXTURES) != 0 {
                let found = env.material(i32::from(material_id) & 0xffff)?;
                if found.details_only {
                    material_id = -1;
                }
            }
            let mut uvs = Uvs {
                a: (0.0, 0.0),
                b: (0.0, 0.0),
                c: (0.0, 0.0),
                offset_b: 0,
                offset_c: 0,
                cube: 0,
            };
            if material_id != -1 {
                if textured == -1 {
                    uvs.a = (0.0, 1.0);
                    uvs.b = (1.0, 1.0);
                    uvs.offset_b = 1;
                    uvs.c = (0.0, 0.0);
                    uvs.offset_c = 2;
                } else {
                    uvs = model.texture_coordinates(
                        mesh,
                        transform.as_ref(),
                        face,
                        textured & 0xff,
                    )?;
                }
            }
            let render_type = if mesh.face_render_type.is_none() {
                0
            } else {
                at(&mesh.face_render_type, i64::from(face))?
            };
            let fa = at(&mesh.faces_a, i64::from(face))?;
            let fb = at(&mesh.faces_b, i64::from(face))?;
            let fc = at(&mesh.faces_c, i64::from(face))?;
            let shifted = ((i64::from(uvs.cube) << 24)
                .wrapping_add(i64::from(color) << 8)
                .wrapping_add(i64::from(alpha_amt)))
                << 32;
            if render_type == 0 {
                let uid = (i64::from(textured) << 2).wrapping_add(shifted);
                let normal = |vertex: i16| -> R<[i32; 4]> {
                    vertex_normals
                        .get(usize::try_from(vertex).map_err(|_| ModelError::Bounds)?)
                        .copied()
                        .ok_or(ModelError::Bounds)
                };
                let na = normal(fa)?;
                let slot = model.connect_normal(&mut normals_uid, i32::from(fa), uid, na, uvs.a)?;
                model.faces_a[t] = slot;
                let nb = normal(fb)?;
                let slot = model.connect_normal(
                    &mut normals_uid,
                    i32::from(fb),
                    uid.wrapping_add(uvs.offset_b),
                    nb,
                    uvs.b,
                )?;
                model.faces_b[t] = slot;
                let nc = normal(fc)?;
                let slot = model.connect_normal(
                    &mut normals_uid,
                    i32::from(fc),
                    uid.wrapping_add(uvs.offset_c),
                    nc,
                    uvs.c,
                )?;
                model.faces_c[t] = slot;
            } else if render_type == 1 {
                let flat = face_normals[face as usize].ok_or(ModelError::Null)?;
                let uid = (i64::from(textured) << 2)
                    .wrapping_add(if flat[0] > 0 { 1024 } else { 2048 })
                    .wrapping_add(i64::from(flat[1].wrapping_add(256)) << 12)
                    .wrapping_add(i64::from(flat[2].wrapping_add(256)) << 22)
                    .wrapping_add(shifted);
                let normal = [flat[0], flat[1], flat[2], 0];
                let slot =
                    model.connect_normal(&mut normals_uid, i32::from(fa), uid, normal, uvs.a)?;
                model.faces_a[t] = slot;
                let slot = model.connect_normal(
                    &mut normals_uid,
                    i32::from(fb),
                    uid.wrapping_add(uvs.offset_b),
                    normal,
                    uvs.b,
                )?;
                model.faces_b[t] = slot;
                let slot = model.connect_normal(
                    &mut normals_uid,
                    i32::from(fc),
                    uid.wrapping_add(uvs.offset_c),
                    normal,
                    uvs.c,
                )?;
                model.faces_c[t] = slot;
            }
            if mesh.faces_alpha.is_some() {
                model.faces_alpha[t] = at(&mesh.faces_alpha, i64::from(face))?;
            }
            if mesh.faces_submeshes.is_some() {
                let value = at(&mesh.faces_submeshes, i64::from(face))?;
                if let Some(list) = model.faces_submeshes.as_mut() {
                    list[t] = value;
                }
            }
            model.faces_color[t] = at(&mesh.faces_color, i64::from(face))?;
            model.faces_material[t] = material_id;
        }
        let mut last: i16 = -10000;
        let mut offsets: Vec<i32> = Vec::new();
        for face in 0..model.rendered_face_count {
            let id = model.faces_material[face];
            if id != last {
                offsets.push(face as i32);
                last = id;
            }
        }
        offsets.push(model.rendered_face_count as i32);
        model.material_batch_offsets = offsets;
        let kept = model.num_normals;
        model.normals_x.truncate(kept);
        model.normals_y.truncate(kept);
        model.normals_z.truncate(kept);
        model.magnitudes.truncate(kept);
        model.texture_u.truncate(kept);
        model.texture_v.truncate(kept);
        if mesh.vertex_groups.is_some() && (function_mask & MASK_ALTERS_VERTICES) != 0 {
            model.vertex_groups = Some(mesh.create_vertex_groups(false)?);
        }
        if mesh.billboards.is_some() && (function_mask & MASK_ALTERS_BILLBOARDS) != 0 {
            model.billboard_groups = Some(mesh.create_billboard_groups()?);
        }
        if mesh.face_groups.is_some()
            && (function_mask & (MASK_ALTERS_ALPHA | MASK_ALTERS_COLOR)) != 0
        {
            let mut max_group = 0usize;
            let mut counts = [0usize; 256];
            for &face in &faces[..selected] {
                let group = at(&mesh.face_groups, i64::from(face))?;
                if group >= 0 {
                    let slot = counts.get_mut(group as usize).ok_or(ModelError::Bounds)?;
                    *slot += 1;
                    if group as usize > max_group {
                        max_group = group as usize;
                    }
                }
            }
            let mut grouped: Vec<Vec<i32>> = Vec::new();
            for count in counts.iter_mut().take(max_group + 1) {
                grouped.push(vec![0; *count]);
                *count = 0;
            }
            for (t, &face) in faces[..selected].iter().enumerate() {
                let group = at(&mesh.face_groups, i64::from(face))?;
                if group >= 0 {
                    let g = group as usize;
                    grouped[g][counts[g]] = t as i32;
                    counts[g] += 1;
                }
            }
            model.face_groups = Some(grouped);
        }
        Ok(model)
    }

    fn texture_coordinates(
        &self,
        mesh: &Mesh,
        transform: Option<&TexMatrix>,
        face: i32,
        tex: i32,
    ) -> R<Uvs> {
        let tex_index = i64::from(tex);
        let render_type = at(&mesh.textures_mapping, tex_index)?;
        let fa = at(&mesh.faces_a, i64::from(face))?;
        let fb = at(&mesh.faces_b, i64::from(face))?;
        let fc = at(&mesh.faces_c, i64::from(face))?;
        let vertex = |index: i16| -> R<[i32; 3]> {
            Ok([
                vtx(&mesh.vertices_x, index)?,
                vtx(&mesh.vertices_y, index)?,
                vtx(&mesh.vertices_z, index)?,
            ])
        };
        let mut uvs = Uvs {
            a: (0.0, 0.0),
            b: (0.0, 0.0),
            c: (0.0, 0.0),
            offset_b: 0,
            offset_c: 0,
            cube: 0,
        };
        if render_type == 0 {
            let origin_index = at(&mesh.texture_face_x, tex_index)?;
            let ox = vtxf(&mesh.vertices_x, origin_index)?;
            let oy = vtxf(&mesh.vertices_y, origin_index)?;
            let oz = vtxf(&mesh.vertices_z, origin_index)?;
            let index_b = at(&mesh.texture_face_y, tex_index)?;
            let e1 = [
                vtxf(&mesh.vertices_x, index_b)? - ox,
                vtxf(&mesh.vertices_y, index_b)? - oy,
                vtxf(&mesh.vertices_z, index_b)? - oz,
            ];
            let index_c = at(&mesh.texture_face_z, tex_index)?;
            let e2 = [
                vtxf(&mesh.vertices_x, index_c)? - ox,
                vtxf(&mesh.vertices_y, index_c)? - oy,
                vtxf(&mesh.vertices_z, index_c)? - oz,
            ];
            let offsets = |index: i16| -> R<[f32; 3]> {
                Ok([
                    vtxf(&mesh.vertices_x, index)? - ox,
                    vtxf(&mesh.vertices_y, index)? - oy,
                    vtxf(&mesh.vertices_z, index)? - oz,
                ])
            };
            let (oa, ob, oc) = (offsets(fa)?, offsets(fb)?, offsets(fc)?);
            let cross = [
                e1[1] * e2[2] - e1[2] * e2[1],
                e1[2] * e2[0] - e1[0] * e2[2],
                e1[0] * e2[1] - e1[1] * e2[0],
            ];
            let basis = [
                e2[1] * cross[2] - e2[2] * cross[1],
                e2[2] * cross[0] - e2[0] * cross[2],
                e2[0] * cross[1] - e2[1] * cross[0],
            ];
            let dot = |b: &[f32; 3], o: &[f32; 3]| b[0] * o[0] + b[1] * o[1] + b[2] * o[2];
            let inverse = 1.0f32 / dot(&basis, &e1);
            let (au, bu, cu) = (
                dot(&basis, &oa) * inverse,
                dot(&basis, &ob) * inverse,
                dot(&basis, &oc) * inverse,
            );
            let basis = [
                e1[1] * cross[2] - e1[2] * cross[1],
                e1[2] * cross[0] - e1[0] * cross[2],
                e1[0] * cross[1] - e1[1] * cross[0],
            ];
            let inverse = 1.0f32 / dot(&basis, &e2);
            let (av, bv, cv) = (
                dot(&basis, &oa) * inverse,
                dot(&basis, &ob) * inverse,
                dot(&basis, &oc) * inverse,
            );
            uvs.a = (au, av);
            uvs.b = (bu, bv);
            uvs.c = (cu, cv);
            return Ok(uvs);
        }
        let transform = transform.ok_or(ModelError::Null)?;
        let center = [
            at_slice(&transform.center_x, tex_index)?,
            at_slice(&transform.center_y, tex_index)?,
            at_slice(&transform.center_z, tex_index)?,
        ];
        let matrix: &[f32] = transform
            .matrix
            .get(tex as usize)
            .ok_or(ModelError::Bounds)?;
        let direction = i32::from(at(&mesh.textures_direction, tex_index)?);
        let speed = at(&mesh.textures_speed, tex_index)? as f32 / 256.0f32;
        let (va, vb, vc) = (vertex(fa)?, vertex(fb)?, vertex(fc)?);
        if render_type == 1 {
            let scale = at(&mesh.textures_scale_z, tex_index)? as f32 / 1024.0f32;
            uvs.a = map_cylindrical(va, center, matrix, scale, direction, speed)?;
            uvs.b = map_cylindrical(vb, center, matrix, scale, direction, speed)?;
            uvs.c = map_cylindrical(vc, center, matrix, scale, direction, speed)?;
            let half = scale / 2.0f32;
            if direction & 1 == 0 {
                let a = uvs.a.0;
                uvs.offset_b = wrap_pair(&mut uvs.b.0, a, half, scale);
                uvs.offset_c = wrap_pair(&mut uvs.c.0, a, half, scale);
            } else {
                let a = uvs.a.1;
                uvs.offset_b = wrap_pair(&mut uvs.b.1, a, half, scale);
                uvs.offset_c = wrap_pair(&mut uvs.c.1, a, half, scale);
            }
        } else if render_type == 2 {
            let u_trans = at(&mesh.textures_utrans, tex_index)? as f32 / 256.0f32;
            let v_trans = at(&mesh.textures_vtrans, tex_index)? as f32 / 256.0f32;
            let dx = vb[0].wrapping_sub(va[0]);
            let dy = vb[1].wrapping_sub(va[1]);
            let dz = vb[2].wrapping_sub(va[2]);
            let dx2 = vc[0].wrapping_sub(va[0]);
            let dy2 = vc[1].wrapping_sub(va[1]);
            let dz2 = vc[2].wrapping_sub(va[2]);
            let cx = dy.wrapping_mul(dz2).wrapping_sub(dy2.wrapping_mul(dz));
            let cy = dz.wrapping_mul(dx2).wrapping_sub(dz2.wrapping_mul(dx));
            let cz = dx.wrapping_mul(dy2).wrapping_sub(dx2.wrapping_mul(dy));
            let factor_x = 64.0f32 / at(&mesh.textures_scale_x, tex_index)? as f32;
            let factor_y = 64.0f32 / at(&mesh.textures_scale_y, tex_index)? as f32;
            let factor_z = 64.0f32 / at(&mesh.textures_scale_z, tex_index)? as f32;
            let (cxf, cyf, czf) = (cx as f32, cy as f32, cz as f32);
            let m = |i: i64| at_slice(matrix, i);
            let nx = (cxf * m(0)? + cyf * m(1)? + czf * m(2)?) / factor_x;
            let ny = (cxf * m(3)? + cyf * m(4)? + czf * m(5)?) / factor_y;
            let nz = (cxf * m(6)? + cyf * m(7)? + czf * m(8)?) / factor_z;
            uvs.cube = cube_face(nx, ny, nz);
            uvs.a = map_rectangular(
                va, center, uvs.cube, matrix, direction, speed, u_trans, v_trans,
            )?;
            uvs.b = map_rectangular(
                vb, center, uvs.cube, matrix, direction, speed, u_trans, v_trans,
            )?;
            uvs.c = map_rectangular(
                vc, center, uvs.cube, matrix, direction, speed, u_trans, v_trans,
            )?;
        } else if render_type == 3 {
            uvs.a = map_spherical(va, center, matrix, direction, speed)?;
            uvs.b = map_spherical(vb, center, matrix, direction, speed)?;
            uvs.c = map_spherical(vc, center, matrix, direction, speed)?;
            if direction & 1 == 0 {
                let a = uvs.a.0;
                uvs.offset_b = wrap_pair(&mut uvs.b.0, a, 0.5f32, 1.0f32);
                uvs.offset_c = wrap_pair(&mut uvs.c.0, a, 0.5f32, 1.0f32);
            } else {
                let a = uvs.a.1;
                uvs.offset_b = wrap_pair(&mut uvs.b.1, a, 0.5f32, 1.0f32);
                uvs.offset_c = wrap_pair(&mut uvs.c.1, a, 0.5f32, 1.0f32);
            }
        }
        Ok(uvs)
    }

    fn connect_normal(
        &mut self,
        uids: &mut [i64],
        face: i32,
        uid: i64,
        normal: [i32; 4],
        uv: (f32, f32),
    ) -> R<i16> {
        let start = at_slice(&self.triangles, i64::from(face))?;
        let end = at_slice(&self.triangles, i64::from(face) + 1)?;
        let mut slot: i64 = 0;
        for search in start..end {
            let reference = at_slice(&self.vertex_normal_indices, i64::from(search))?;
            if reference == 0 {
                slot = i64::from(search);
                break;
            }
            if at_slice(uids, i64::from(search))? == uid {
                return Ok(reference.wrapping_sub(1));
            }
        }
        let n = self.num_normals as i64;
        put_slice(
            &mut self.vertex_normal_indices,
            slot,
            (self.num_normals as i32 + 1) as i16,
        )?;
        put_slice(uids, slot, uid)?;
        put_slice(&mut self.normals_x, n, normal[0] as i16)?;
        put_slice(&mut self.normals_y, n, normal[1] as i16)?;
        put_slice(&mut self.normals_z, n, normal[2] as i16)?;
        put_slice(&mut self.magnitudes, n, normal[3] as i8)?;
        put_slice(&mut self.texture_u, n, uv.0)?;
        put_slice(&mut self.texture_v, n, uv.1)?;
        self.num_normals += 1;
        Ok(n as i16)
    }

    pub fn set_ambient(&mut self, ambient: i32) {
        self.ambient = ambient as i16;
    }

    pub fn set_contrast(&mut self, contrast: i32) {
        self.contrast = contrast as i16;
    }

    pub fn recolor(&mut self, src: i16, dst: i16) -> R<()> {
        if self.released.color && (self.num_faces > 0 || self.billboard_count > 0) {
            return Err(ModelError::Null);
        }
        for color in self.faces_color.iter_mut().take(self.num_faces) {
            if *color == src {
                *color = dst;
            }
        }
        Ok(())
    }

    pub fn rematerial(&mut self, env: &Env, src: i16, dst: i16) -> R<()> {
        if self.released.material && self.num_faces > 0 {
            return Err(ModelError::Null);
        }
        for material in self.faces_material.iter_mut().take(self.num_faces) {
            if *material == src {
                *material = dst;
            }
        }
        let (mut src_alpha, mut src_brightness) = (0i8, 0i8);
        if src != -1 {
            let found = env.material(i32::from(src) & 0xffff)?;
            src_alpha = found.alpha;
            src_brightness = found.brightness;
        }
        let (mut dst_alpha, mut dst_brightness) = (0i8, 0i8);
        if dst != -1 {
            let found = env.material(i32::from(dst) & 0xffff)?;
            dst_alpha = found.alpha;
            dst_brightness = found.brightness;
            if found.texture_speed_u != 0 || found.texture_speed_v != 0 {
                self.texture_animating = true;
            }
        }
        if (src_alpha != dst_alpha || src_brightness != dst_brightness)
            && self.billboard_count > 0
            && self.released.color
        {
            return Err(ModelError::Null);
        }
        Ok(())
    }

    pub fn tint(&mut self, hue: i32, saturation: i32, lightness: i32, opacity: i32) -> R<()> {
        if self.released.color && (self.num_faces > 0 || self.billboard_count > 0) {
            return Err(ModelError::Null);
        }
        for color in self.faces_color.iter_mut().take(self.num_faces) {
            let hsl = i32::from(*color) & 0xffff;
            let mut h = (hsl >> 10) & 0x3f;
            let mut s = (hsl >> 7) & 0x7;
            let mut l = hsl & 0x7f;
            if hue != -1 {
                h += hue.wrapping_sub(h).wrapping_mul(opacity) >> 7;
            }
            if saturation != -1 {
                s += opacity.wrapping_mul(saturation.wrapping_sub(s)) >> 7;
            }
            if lightness != -1 {
                l += lightness.wrapping_sub(l).wrapping_mul(opacity) >> 7;
            }
            *color = ((h << 10) | (s << 7) | l) as i16;
        }
        Ok(())
    }

    pub fn apply_environment_alpha(&mut self, alpha: i8, transparency: Option<&[i8]>) -> R<()> {
        if self.released.alpha && self.num_faces > 0 {
            return Err(ModelError::Null);
        }
        for face in 0..self.num_faces {
            match transparency {
                None => self.faces_alpha[face] = alpha,
                Some(list) => {
                    let other = i32::from(at_slice(list, face as i64)?) & 0xff;
                    let value = 255 - (255 - (i32::from(alpha) & 0xff)) * (255 - other) / 255;
                    self.faces_alpha[face] = value as i8;
                }
            }
        }
        Ok(())
    }

    pub fn apply_shading(&mut self, mask: i32) {
        self.function_mask = mask;
        if (mask & 0x10000) == 0 {
            if let Some(merged) = self.merged_normals.take() {
                self.normals_x = merged.normals_x;
                self.normals_y = merged.normals_y;
                self.normals_z = merged.normals_z;
                self.magnitudes = merged.magnitudes;
            }
        }
        self.release_uploaded_data();
    }

    fn ensure_bounds(&mut self) -> [i32; 6] {
        if let Some(bounds) = self.cached_bounds {
            return bounds;
        }
        let bounds = self.measure_bounds();
        self.cached_bounds = Some(bounds);
        bounds
    }

    fn measure_bounds(&self) -> [i32; 6] {
        let (Some(vx), Some(vy), Some(vz)) = (&self.vertices_x, &self.vertices_y, &self.vertices_z)
        else {
            return [0; 6];
        };
        let depth = usize::try_from(self.max_depth)
            .unwrap_or(0)
            .min(vx.len())
            .min(vy.len())
            .min(vz.len());
        let mut b = [32767i32, -32768, 32767, -32768, 32767, -32768];
        for v in 0..depth {
            for (axis, value) in [vx[v], vy[v], vz[v]].into_iter().enumerate() {
                b[axis * 2] = b[axis * 2].min(value);
                b[axis * 2 + 1] = b[axis * 2 + 1].max(value);
            }
        }
        b.map(|value| i32::from(value as i16))
    }

    pub fn apply_shading_model(&mut self, other: &mut Model, x: i32, y: i32, z: i32) -> R<()> {
        if self.num_faces == 0 || other.num_faces == 0 {
            return Ok(());
        }
        let [min_x, max_x, min_y, max_y, min_z, max_z] = other.ensure_bounds();
        let this_depth = len_of(self.max_depth)?;
        let other_depth = len_of(other.max_depth)?;
        let mut merged = self.merged_normals.take();
        let mut other_merged = other.merged_normals.take();
        let result = shade_across(
            self,
            other,
            [x, y, z],
            [min_x, max_x, min_y, max_y, min_z, max_z],
            [this_depth, other_depth],
            (&mut merged, &mut other_merged),
        );
        self.merged_normals = merged;
        other.merged_normals = other_merged;
        result
    }

    fn release_uploaded_data(&mut self) {
        let mask = self.function_mask;
        let preference = self.preference_flags;
        let textured = self.ctor_textured;
        let normal_buffer = self.ctor_normal_buffer;
        if !self.pins_vertices && (mask & (0x20 | 0x10 | 0x7)) == 0 {
            let drops = !textured
                && ((self.vertices_x.is_some() && !retains_x(mask))
                    || (self.vertices_y.is_some() && !retains_y(mask))
                    || (self.vertices_z.is_some() && !retains_z(mask)));
            if drops {
                self.ensure_bounds();
            }
            if !textured && !retains_x(mask) {
                self.vertices_x = None;
            }
            if !textured && !retains_y(mask) {
                self.vertices_y = None;
            }
            if !textured && !retains_z(mask) {
                self.vertices_z = None;
            }
        }
        if !self.released.vertex_index
            && self.vertices_x.is_none()
            && self.vertices_y.is_none()
            && self.vertices_z.is_none()
        {
            self.released.vertex_index = true;
        }
        if !normal_buffer && !textured && !retains_normals(mask, preference) {
            self.released.normals = true;
        }
        let rebuild = requires_color_rebuild(mask, preference);
        if !textured && !alters_color(mask) && !rebuild {
            self.released.color = true;
        }
        if !textured && (mask & 0x40000) == 0 && !alters_alpha(mask) && !rebuild {
            self.released.alpha = true;
        }
        self.released.uv = self.released.uv || !textured;
        if !textured && !has_textures(mask) && (mask & 0x8000) == 0 && !rebuild {
            self.released.material = true;
        }
        let copies_elements = alters_facing(mask) && has_textures(mask);
        if !textured
            && (mask & (0x20000 | 0x40000)) == 0
            && !alters_facing(mask)
            && !rebuild
            && !copies_elements
        {
            self.released.faces = true;
        }
        if self.face_groups.is_some() && (mask & (0x100 | 0x80)) == 0 {
            self.face_groups = None;
            self.faces_submeshes = None;
        }
        if (mask & 0x20) == 0 {
            self.vertex_groups = None;
        }
        if (mask & 0x400) == 0 {
            self.billboard_groups = None;
        }
        if (mask & 0x800) == 0 && (mask & 0x40000) == 0 {
            self.released.batch_offsets = true;
        }
    }

    fn face_rgba(&self, env: &Env, hsl: i16, material: i16, alpha: i8) -> R<i32> {
        face_color_rgba(
            env.hsl_to_rgb,
            i32::from(hsl),
            i32::from(material),
            i32::from(self.ambient),
            i32::from(alpha as u8),
            Some(env.materials),
        )
        .map_err(|_| ModelError::Invalid)
    }

    pub fn build_model_cache(&self, env: &Env) -> R<Option<Built>> {
        let faces = self.rendered_face_count;
        if faces == 0 || self.released.faces {
            return Ok(None);
        }
        let (Some(vx), Some(vy), Some(vz)) = (&self.vertices_x, &self.vertices_y, &self.vertices_z)
        else {
            return Ok(None);
        };
        let direct = self.released.vertex_index;
        let has_normals = !self.released.normals;
        let (nx, ny, nz, mags) = match &self.merged_normals {
            Some(m) => (&m.normals_x, &m.normals_y, &m.normals_z, &m.magnitudes),
            None => (
                &self.normals_x,
                &self.normals_y,
                &self.normals_z,
                &self.magnitudes,
            ),
        };
        let depth = len_of(self.max_depth)?;
        let normals = self.num_normals;
        let mut normal_to_vertex = vec![-1i32; if direct { 0 } else { normals }];
        if !direct {
            for vertex in 0..depth {
                let start = at_slice(&self.triangles, vertex as i64)?;
                let end = at_slice(&self.triangles, vertex as i64 + 1)?;
                for i in start..end {
                    let normal =
                        i32::from(at_slice(&self.vertex_normal_indices, i64::from(i))?) - 1;
                    if normal == -1 {
                        break;
                    }
                    if normal >= 0 && (normal as usize) < normals {
                        normal_to_vertex[normal as usize] = vertex as i32;
                    }
                }
            }
        }
        let mut resolved: Vec<Option<[i32; 3]>> = Vec::with_capacity(faces);
        let mut valid = 0usize;
        for face in 0..faces {
            let idx = [
                usize::from(at_slice(&self.faces_a, face as i64)? as u16),
                usize::from(at_slice(&self.faces_b, face as i64)? as u16),
                usize::from(at_slice(&self.faces_c, face as i64)? as u16),
            ];
            let vert = idx.map(|i| {
                if direct {
                    i as i32
                } else if i < normals {
                    normal_to_vertex[i]
                } else {
                    -1
                }
            });
            if vert.iter().all(|v| *v >= 0 && (*v as usize) < depth) {
                valid += 1;
                resolved.push(Some(vert));
            } else {
                resolved.push(None);
            }
        }
        if valid == 0 {
            return Ok(None);
        }
        let contrast = i32::from(self.contrast);
        let contrast_scale = if contrast != 0 {
            768f32 / contrast as f32
        } else {
            1f32
        };
        let contrast_for_normals = if contrast == 0 { 768 } else { contrast };
        let flat_scale = 2f32 / contrast_for_normals as f32;
        let smooth_scale = 3f32 / contrast_for_normals as f32;
        let slot_count = if direct { depth } else { normals };
        let mut slot_color = vec![0i32; slot_count];
        let mut slot_stamped = vec![false; slot_count];
        let color_faces = self
            .num_faces
            .min(self.faces_a.len())
            .min(self.faces_b.len())
            .min(self.faces_c.len());
        let mut face_rgba_cache = vec![0i32; faces];
        for (face, cached) in face_rgba_cache.iter_mut().enumerate().take(color_faces) {
            let hsl = if self.released.color {
                0
            } else {
                at_slice(&self.faces_color, face as i64)?
            };
            let material = if self.released.material {
                -1
            } else {
                at_slice(&self.faces_material, face as i64)?
            };
            let alpha = if self.released.alpha {
                0
            } else {
                at_slice(&self.faces_alpha, face as i64)?
            };
            let rgba = self.face_rgba(env, hsl, material, alpha)?;
            *cached = rgba;
            for slot in [self.faces_a[face], self.faces_b[face], self.faces_c[face]] {
                let slot = usize::from(slot as u16);
                if slot < slot_count {
                    slot_color[slot] = rgba;
                    slot_stamped[slot] = true;
                }
            }
        }
        let mut remap: Vec<Option<i32>> = vec![None; slot_count];
        let mut words: Vec<i32> = Vec::with_capacity(valid * 30);
        let mut emitted = 0i32;
        let mut face_index: Vec<i32> = Vec::with_capacity(valid * 3);
        let mut material_ids: Vec<i32> = Vec::with_capacity(valid);
        for face in 0..faces {
            let Some(vert) = resolved[face] else {
                continue;
            };
            let idx = [
                usize::from(self.faces_a[face] as u16),
                usize::from(self.faces_b[face] as u16),
                usize::from(self.faces_c[face] as u16),
            ];
            let material_raw = if self.released.material {
                -1
            } else {
                at_slice(&self.faces_material, face as i64)?
            };
            material_ids.push(i32::from(material_raw));
            let face_rgba = if face < color_faces {
                face_rgba_cache[face]
            } else {
                let hsl = if self.released.color {
                    0
                } else {
                    at_slice(&self.faces_color, face as i64)?
                };
                let alpha = if self.released.alpha {
                    0
                } else {
                    at_slice(&self.faces_alpha, face as i64)?
                };
                self.face_rgba(env, hsl, material_raw, alpha)?
            };
            for k in 0..3 {
                let slot = idx[k];
                if let Some(out) = remap[slot] {
                    face_index.push(out);
                    continue;
                }
                let out = emitted;
                emitted += 1;
                remap[slot] = Some(out);
                let rgba = if slot < slot_count && slot_stamped[slot] {
                    slot_color[slot]
                } else {
                    face_rgba
                };
                let v = vert[k] as usize;
                words.push((at_slice(vx, v as i64)? as f32).to_bits() as i32);
                words.push((at_slice(vy, v as i64)? as f32).to_bits() as i32);
                words.push((at_slice(vz, v as i64)? as f32).to_bits() as i32);
                if has_normals && slot < nx.len() {
                    let magnitude = i32::from(mags[slot] as u8);
                    let k = if magnitude == 0 {
                        flat_scale
                    } else {
                        smooth_scale / magnitude as f32
                    };
                    words.push((f32::from(nx[slot]) * k).to_bits() as i32);
                    words.push((f32::from(ny[slot]) * k).to_bits() as i32);
                    words.push((f32::from(nz[slot]) * k).to_bits() as i32);
                } else {
                    words.push(0);
                    words.push(contrast_scale.to_bits() as i32);
                    words.push(0);
                }
                let uv_present = !self.released.uv;
                words.push(
                    self.texture_u
                        .get(slot)
                        .filter(|_| uv_present)
                        .map_or(0, |value| value.to_bits() as i32),
                );
                words.push(
                    self.texture_v
                        .get(slot)
                        .filter(|_| uv_present)
                        .map_or(0, |value| value.to_bits() as i32),
                );
                words.push(rgba.swap_bytes());
                words.push(0);
                face_index.push(out);
            }
        }
        let mut batches: Vec<Batch> = Vec::new();
        let mut previous = i32::MIN;
        for (face, raw) in material_ids.iter().enumerate() {
            let id = if *raw < 0 { -1 } else { *raw };
            if id != previous {
                batches.push(Batch {
                    material_id: id,
                    indices: Vec::new(),
                });
                previous = id;
            }
            if let Some(batch) = batches.last_mut() {
                batch
                    .indices
                    .extend_from_slice(&face_index[face * 3..face * 3 + 3]);
            }
        }
        Ok(Some(Built {
            vertex_words: words,
            vertex_count: emitted as usize,
            face_count: valid,
            batches,
        }))
    }
}
