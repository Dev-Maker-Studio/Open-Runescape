use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum MeshError {
    Truncated,
    Index,
    MissingArray,
}

impl Display for MeshError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        match self {
            Self::Truncated => write!(f, "mesh data read past its bounds"),
            Self::Index => write!(f, "mesh array index out of range"),
            Self::MissingArray => write!(f, "mesh array required by the data is absent"),
        }
    }
}

impl Error for MeshError {}

type Res<T> = Result<T, MeshError>;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct Billboard {
    pub billboard_id: i32,
    pub applies_on_face: i32,
    pub depth: i32,
    pub distance: i32,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct EmissiveTriangle {
    pub producer_id: i32,
    pub face_a: i32,
    pub face_b: i32,
    pub face_c: i32,
    pub priority: i8,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct EffectiveVertex {
    pub effect_id: i32,
    pub applies_on_vertex: i32,
}

#[derive(Clone, Debug, Default, PartialEq)]
pub struct Mesh {
    pub version: i32,
    pub num_vertices: i32,
    pub max_depth: i32,
    pub num_faces: i32,
    pub priority: i8,
    pub num_textures: i32,
    pub textures_mapping: Option<Vec<i8>>,
    pub vertices_x: Option<Vec<i32>>,
    pub vertices_y: Option<Vec<i32>>,
    pub vertices_z: Option<Vec<i32>>,
    pub faces_a: Option<Vec<i16>>,
    pub faces_b: Option<Vec<i16>>,
    pub faces_c: Option<Vec<i16>>,
    pub vertex_groups: Option<Vec<i32>>,
    pub face_render_type: Option<Vec<i8>>,
    pub faces_layer: Option<Vec<i8>>,
    pub faces_alpha: Option<Vec<i8>>,
    pub face_groups: Option<Vec<i32>>,
    pub faces_material: Option<Vec<i16>>,
    pub faces_color: Option<Vec<i16>>,
    pub faces_texture: Option<Vec<i8>>,
    pub texture_face_x: Option<Vec<i16>>,
    pub texture_face_y: Option<Vec<i16>>,
    pub texture_face_z: Option<Vec<i16>>,
    pub textures_scale_x: Option<Vec<i32>>,
    pub textures_scale_y: Option<Vec<i32>>,
    pub textures_scale_z: Option<Vec<i32>>,
    pub textures_rotation: Option<Vec<i8>>,
    pub textures_direction: Option<Vec<i8>>,
    pub textures_speed: Option<Vec<i32>>,
    pub textures_utrans: Option<Vec<i32>>,
    pub textures_vtrans: Option<Vec<i32>>,
    pub particle_emitters: Option<Vec<EmissiveTriangle>>,
    pub particle_effectors: Option<Vec<EffectiveVertex>>,
    pub billboards: Option<Vec<Billboard>>,
    pub vertices_submeshes: Option<Vec<i16>>,
    pub faces_submeshes: Option<Vec<i16>>,
}

struct Cursor<'a> {
    data: &'a [u8],
    pos: i64,
}

impl<'a> Cursor<'a> {
    fn new(data: &'a [u8]) -> Self {
        Self { data, pos: 0 }
    }

    fn set(&mut self, pos: i64) {
        self.pos = pos;
    }

    fn peek(&self) -> Res<u8> {
        let index = usize::try_from(self.pos).map_err(|_| MeshError::Truncated)?;
        self.data.get(index).copied().ok_or(MeshError::Truncated)
    }

    fn byte(&mut self) -> Res<u8> {
        let value = self.peek()?;
        self.pos += 1;
        Ok(value)
    }

    fn signed(&mut self) -> Res<i8> {
        Ok(self.byte()? as i8)
    }

    fn ubyte(&mut self) -> Res<i32> {
        Ok(i32::from(self.byte()?))
    }

    fn ushort(&mut self) -> Res<i32> {
        let high = self.ubyte()?;
        let low = self.ubyte()?;
        Ok((high << 8) | low)
    }

    fn umedium(&mut self) -> Res<i32> {
        let high = self.ubyte()?;
        let middle = self.ubyte()?;
        let low = self.ubyte()?;
        Ok((high << 16) | (middle << 8) | low)
    }

    fn smart(&mut self) -> Res<i32> {
        if self.peek()? < 128 {
            Ok(self.ubyte()? - 64)
        } else {
            Ok(self.ushort()? - 49152)
        }
    }
}

fn at<T: Copy>(array: &Option<Vec<T>>, index: i64) -> Res<T> {
    let array = array.as_ref().ok_or(MeshError::MissingArray)?;
    let index = usize::try_from(index).map_err(|_| MeshError::Index)?;
    array.get(index).copied().ok_or(MeshError::Index)
}

fn put<T: Copy>(array: &mut Option<Vec<T>>, index: i64, value: T) -> Res<()> {
    let array = array.as_mut().ok_or(MeshError::MissingArray)?;
    let index = usize::try_from(index).map_err(|_| MeshError::Index)?;
    *array.get_mut(index).ok_or(MeshError::Index)? = value;
    Ok(())
}

fn zeros<T: Clone + Default>(count: i32) -> Option<Vec<T>> {
    Some(vec![T::default(); usize::try_from(count).unwrap_or(0)])
}

impl Mesh {
    pub fn from_bytes(data: &[u8]) -> Res<Mesh> {
        let length = data.len();
        if length == 0 {
            return Err(MeshError::Truncated);
        }
        let mut mesh = Mesh {
            version: 12,
            ..Mesh::default()
        };
        if length >= 2 && data[length - 1] == 0xff && data[length - 2] == 0xff {
            mesh.decode_new_format(data)?;
        } else {
            mesh.decode_old_format(data)?;
        }
        Ok(mesh)
    }

    fn decode_vertices(
        &mut self,
        flags: &mut Cursor,
        xs: &mut Cursor,
        ys: &mut Cursor,
        zs: &mut Cursor,
        skins: &mut Cursor,
        has_skins: bool,
    ) -> Res<()> {
        let (mut last_x, mut last_y, mut last_z) = (0i32, 0i32, 0i32);
        for vertex in 0..i64::from(self.num_vertices) {
            let bits = flags.ubyte()?;
            let dx = if bits & 1 != 0 { xs.smart()? } else { 0 };
            let dy = if bits & 2 != 0 { ys.smart()? } else { 0 };
            let dz = if bits & 4 != 0 { zs.smart()? } else { 0 };
            last_x = last_x.wrapping_add(dx);
            last_y = last_y.wrapping_add(dy);
            last_z = last_z.wrapping_add(dz);
            put(&mut self.vertices_x, vertex, last_x)?;
            put(&mut self.vertices_y, vertex, last_y)?;
            put(&mut self.vertices_z, vertex, last_z)?;
            if has_skins {
                let group = skins.ubyte()?;
                put(&mut self.vertex_groups, vertex, group)?;
            }
        }
        Ok(())
    }

    fn calculate_max_depth(&mut self, first: &mut Cursor, second: &mut Cursor) -> Res<()> {
        let (mut a, mut b, mut c, mut tmp) = (0i16, 0i16, 0i16, 0i16);
        for face in 0..i64::from(self.num_faces) {
            let kind = second.ubyte()?;
            match kind {
                1 => {
                    a = (first.smart()?.wrapping_add(i32::from(tmp))) as i16;
                    b = (first.smart()?.wrapping_add(i32::from(a))) as i16;
                    c = (first.smart()?.wrapping_add(i32::from(b))) as i16;
                    tmp = c;
                    put(&mut self.faces_a, face, a)?;
                    put(&mut self.faces_b, face, b)?;
                    put(&mut self.faces_c, face, c)?;
                    for value in [a, b, c] {
                        if i32::from(value) > self.max_depth {
                            self.max_depth = i32::from(value);
                        }
                    }
                }
                2..=4 => {
                    let stored_b = match kind {
                        2 => {
                            b = c;
                            b
                        }
                        3 => {
                            a = c;
                            b
                        }
                        _ => {
                            let previous = a;
                            std::mem::swap(&mut a, &mut b);
                            previous
                        }
                    };
                    c = (first.smart()?.wrapping_add(i32::from(tmp))) as i16;
                    tmp = c;
                    put(&mut self.faces_a, face, a)?;
                    put(&mut self.faces_b, face, stored_b)?;
                    put(&mut self.faces_c, face, c)?;
                    if i32::from(c) > self.max_depth {
                        self.max_depth = i32::from(c);
                    }
                }
                _ => {}
            }
        }
        self.max_depth += 1;
        Ok(())
    }

    #[allow(clippy::too_many_lines)]
    fn decode_new_format(&mut self, data: &[u8]) -> Res<()> {
        let mut cursors: [Cursor; 7] = std::array::from_fn(|_| Cursor::new(data));
        let size = data.len() as i64;
        cursors[0].set(size - 23);
        self.num_vertices = cursors[0].ushort()?;
        self.num_faces = cursors[0].ushort()?;
        self.num_textures = cursors[0].ubyte()?;
        let footer_flags = cursors[0].ubyte()?;
        let has_face_types = footer_flags & 1 == 1;
        let has_particles = footer_flags & 2 == 2;
        let has_billboards = footer_flags & 4 == 4;
        if footer_flags & 8 == 8 {
            cursors[0].pos -= 7;
            self.version = cursors[0].ubyte()?;
            cursors[0].pos += 6;
        }
        let model_priority = cursors[0].ubyte()?;
        let has_face_alpha = cursors[0].ubyte()?;
        let has_face_skins = cursors[0].ubyte()?;
        let has_face_textures = cursors[0].ubyte()?;
        let has_vertex_skins = cursors[0].ubyte()?;
        let size_x = cursors[0].ushort()?;
        let size_y = cursors[0].ushort()?;
        let size_z = cursors[0].ushort()?;
        let face_indices = cursors[0].ushort()?;
        let texture_indices = cursors[0].ushort()?;
        let (mut simple, mut complex, mut cube) = (0i64, 0i64, 0i64);
        if self.num_textures > 0 {
            let mut mapping = vec![0i8; self.num_textures as usize];
            cursors[0].set(0);
            for slot in &mut mapping {
                let value = cursors[0].signed()?;
                *slot = value;
                if value == 0 {
                    simple += 1;
                }
                if (1..=3).contains(&value) {
                    complex += 1;
                }
                if value == 2 {
                    cube += 1;
                }
            }
            self.textures_mapping = Some(mapping);
        }
        let vertices = i64::from(self.num_vertices);
        let faces = i64::from(self.num_faces);
        let mut index = i64::from(self.num_textures);
        let vertex_flags = index;
        index += vertices;
        let face_types = index;
        if has_face_types {
            index += faces;
        }
        let face_index_types = index;
        index += faces;
        let face_priority = index;
        if model_priority == 255 {
            index += faces;
        }
        let face_skins = index;
        if has_face_skins == 1 {
            index += faces;
        }
        let vertex_skins = index;
        if has_vertex_skins == 1 {
            index += vertices;
        }
        let face_alpha = index;
        if has_face_alpha == 1 {
            index += faces;
        }
        let face_indices_data = index;
        index += i64::from(face_indices);
        let face_material = index;
        if has_face_textures == 1 {
            index += faces * 2;
        }
        let texture_index_data = index;
        index += i64::from(texture_indices);
        let face_color = index;
        index += faces * 2;
        let vertices_x = index;
        index += i64::from(size_x);
        let vertices_y = index;
        index += i64::from(size_y);
        let vertices_z = index;
        index += i64::from(size_z);
        let simple_offset = index;
        index += simple * 6;
        let complex_vert = index;
        index += complex * 6;
        let texture_bytes: i64 = if self.version == 14 {
            7
        } else if self.version >= 15 {
            9
        } else {
            6
        };
        let complex_scale = index;
        index += complex * texture_bytes;
        let complex_rot = index;
        index += complex;
        let complex_dir = index;
        index += complex;
        let complex_speed = index;
        index = cube * 2 + index + complex;
        let complex_count = complex as i32;
        let cube_count = cube as i32;
        self.vertices_x = zeros(self.num_vertices);
        self.vertices_y = zeros(self.num_vertices);
        self.vertices_z = zeros(self.num_vertices);
        self.faces_a = zeros(self.num_faces);
        self.faces_b = zeros(self.num_faces);
        self.faces_c = zeros(self.num_faces);
        if has_vertex_skins == 1 {
            self.vertex_groups = zeros(self.num_vertices);
        }
        if has_face_types {
            self.face_render_type = zeros(self.num_faces);
        }
        if model_priority == 255 {
            self.faces_layer = zeros(self.num_faces);
        } else {
            self.priority = model_priority as i8;
        }
        if has_face_alpha == 1 {
            self.faces_alpha = zeros(self.num_faces);
        }
        if has_face_skins == 1 {
            self.face_groups = zeros(self.num_faces);
        }
        if has_face_textures == 1 {
            self.faces_material = zeros(self.num_faces);
        }
        if has_face_textures == 1 && self.num_textures > 0 {
            self.faces_texture = zeros(self.num_faces);
        }
        self.faces_color = zeros(self.num_faces);
        if self.num_textures > 0 {
            self.texture_face_x = zeros(self.num_textures);
            self.texture_face_y = zeros(self.num_textures);
            self.texture_face_z = zeros(self.num_textures);
            if complex_count > 0 {
                self.textures_scale_x = zeros(complex_count);
                self.textures_scale_y = zeros(complex_count);
                self.textures_scale_z = zeros(complex_count);
                self.textures_rotation = zeros(complex_count);
                self.textures_direction = zeros(complex_count);
                self.textures_speed = zeros(complex_count);
            }
            if cube_count > 0 {
                self.textures_utrans = zeros(cube_count);
                self.textures_vtrans = zeros(cube_count);
            }
        }
        cursors[0].set(vertex_flags);
        cursors[1].set(vertices_x);
        cursors[2].set(vertices_y);
        cursors[3].set(vertices_z);
        cursors[4].set(vertex_skins);
        {
            let [flags, xs, ys, zs, skins, ..] = &mut cursors;
            self.decode_vertices(flags, xs, ys, zs, skins, has_vertex_skins == 1)?;
        }
        cursors[0].set(face_color);
        cursors[1].set(face_types);
        cursors[2].set(face_priority);
        cursors[3].set(face_alpha);
        cursors[4].set(face_skins);
        cursors[5].set(face_material);
        cursors[6].set(texture_index_data);
        for face in 0..faces {
            let color = cursors[0].ushort()? as i16;
            put(&mut self.faces_color, face, color)?;
            if has_face_types {
                let value = cursors[1].signed()?;
                put(&mut self.face_render_type, face, value)?;
            }
            if model_priority == 255 {
                let value = cursors[2].signed()?;
                put(&mut self.faces_layer, face, value)?;
            }
            if has_face_alpha == 1 {
                let value = cursors[3].signed()?;
                put(&mut self.faces_alpha, face, value)?;
            }
            if has_face_skins == 1 {
                let value = cursors[4].ubyte()?;
                put(&mut self.face_groups, face, value)?;
            }
            if has_face_textures == 1 {
                let value = (cursors[5].ushort()? - 1) as i16;
                put(&mut self.faces_material, face, value)?;
            }
            if self.faces_texture.is_some() {
                let value = if at(&self.faces_material, face)? != -1 {
                    (cursors[6].ubyte()? - 1) as i8
                } else {
                    -1
                };
                put(&mut self.faces_texture, face, value)?;
            }
        }
        self.max_depth = -1;
        cursors[0].set(face_indices_data);
        cursors[1].set(face_index_types);
        {
            let [first, second, ..] = &mut cursors;
            self.calculate_max_depth(first, second)?;
        }
        cursors[0].set(simple_offset);
        cursors[1].set(complex_vert);
        cursors[2].set(complex_scale);
        cursors[3].set(complex_rot);
        cursors[4].set(complex_dir);
        cursors[5].set(complex_speed);
        self.decode_textured_triangles(&mut cursors)?;
        cursors[0].set(index);
        let first = &mut cursors[0];
        if has_particles {
            let emitters = first.ubyte()?;
            if emitters > 0 {
                let mut list = Vec::with_capacity(emitters as usize);
                for _ in 0..emitters {
                    let producer_id = first.ushort()?;
                    let face = i64::from(first.ushort()?);
                    let priority = if model_priority == 255 {
                        at(&self.faces_layer, face)?
                    } else {
                        model_priority as i8
                    };
                    list.push(EmissiveTriangle {
                        producer_id,
                        face_a: i32::from(at(&self.faces_a, face)?),
                        face_b: i32::from(at(&self.faces_b, face)?),
                        face_c: i32::from(at(&self.faces_c, face)?),
                        priority,
                    });
                }
                self.particle_emitters = Some(list);
            }
            let effectors = first.ubyte()?;
            if effectors > 0 {
                let mut list = Vec::with_capacity(effectors as usize);
                for _ in 0..effectors {
                    let effect_id = first.ushort()?;
                    let applies_on_vertex = first.ushort()?;
                    list.push(EffectiveVertex {
                        effect_id,
                        applies_on_vertex,
                    });
                }
                self.particle_effectors = Some(list);
            }
        }
        if has_billboards {
            let count = first.ubyte()?;
            if count > 0 {
                let mut list = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    let billboard_id = first.ushort()?;
                    let applies_on_face = first.ushort()?;
                    let depth = first.ubyte()?;
                    let distance = i32::from(first.signed()?);
                    list.push(Billboard {
                        billboard_id,
                        applies_on_face,
                        depth,
                        distance,
                    });
                }
                self.billboards = Some(list);
            }
        }
        Ok(())
    }

    fn decode_textured_triangles(&mut self, cursors: &mut [Cursor; 7]) -> Res<()> {
        for texture in 0..i64::from(self.num_textures) {
            let kind = i32::from(at(&self.textures_mapping, texture)? as u8);
            if kind == 0 {
                let x = cursors[0].ushort()? as i16;
                let y = cursors[0].ushort()? as i16;
                let z = cursors[0].ushort()? as i16;
                put(&mut self.texture_face_x, texture, x)?;
                put(&mut self.texture_face_y, texture, y)?;
                put(&mut self.texture_face_z, texture, z)?;
            }
            if (1..=3).contains(&kind) {
                let x = cursors[1].ushort()? as i16;
                let y = cursors[1].ushort()? as i16;
                let z = cursors[1].ushort()? as i16;
                put(&mut self.texture_face_x, texture, x)?;
                put(&mut self.texture_face_y, texture, y)?;
                put(&mut self.texture_face_z, texture, z)?;
                let (sx, sy, sz);
                if self.version < 15 {
                    sx = cursors[2].ushort()?;
                    sy = if self.version < 14 {
                        cursors[2].ushort()?
                    } else {
                        cursors[2].umedium()?
                    };
                    sz = cursors[2].ushort()?;
                } else {
                    sx = cursors[2].umedium()?;
                    sy = cursors[2].umedium()?;
                    sz = cursors[2].umedium()?;
                }
                put(&mut self.textures_scale_x, texture, sx)?;
                put(&mut self.textures_scale_y, texture, sy)?;
                put(&mut self.textures_scale_z, texture, sz)?;
                let rotation = cursors[3].signed()?;
                let direction = cursors[4].signed()?;
                let speed = i32::from(cursors[5].signed()?);
                put(&mut self.textures_rotation, texture, rotation)?;
                put(&mut self.textures_direction, texture, direction)?;
                put(&mut self.textures_speed, texture, speed)?;
                if kind == 2 {
                    let utrans = i32::from(cursors[5].signed()?);
                    let vtrans = i32::from(cursors[5].signed()?);
                    put(&mut self.textures_utrans, texture, utrans)?;
                    put(&mut self.textures_vtrans, texture, vtrans)?;
                }
            }
        }
        Ok(())
    }

    #[allow(clippy::too_many_lines)]
    fn decode_old_format(&mut self, data: &[u8]) -> Res<()> {
        let mut has_face_types = false;
        let mut has_materials = false;
        let mut cursors: [Cursor; 5] = std::array::from_fn(|_| Cursor::new(data));
        cursors[0].set(data.len() as i64 - 18);
        self.num_vertices = cursors[0].ushort()?;
        self.num_faces = cursors[0].ushort()?;
        self.num_textures = cursors[0].ubyte()?;
        let face_type_flag = cursors[0].ubyte()?;
        let model_priority = cursors[0].ubyte()?;
        let has_face_alpha = cursors[0].ubyte()?;
        let has_face_skins = cursors[0].ubyte()?;
        let has_vertex_skins = cursors[0].ubyte()?;
        let size_x = i64::from(cursors[0].ushort()?);
        let size_y = i64::from(cursors[0].ushort()?);
        let size_z = i64::from(cursors[0].ushort()?);
        let face_indices_size = i64::from(cursors[0].ushort()?);
        let vertices = i64::from(self.num_vertices);
        let faces = i64::from(self.num_faces);
        let mut offset = vertices;
        let face_index_types = offset;
        offset += faces;
        let face_priority = offset;
        if model_priority == 255 {
            offset += faces;
        }
        let face_skins = offset;
        if has_face_skins == 1 {
            offset += faces;
        }
        let face_type = offset;
        if face_type_flag == 1 {
            offset += faces;
        }
        let vertex_skins = offset;
        if has_vertex_skins == 1 {
            offset += vertices;
        }
        let face_alpha = offset;
        if has_face_alpha == 1 {
            offset += faces;
        }
        let face_indices_data = offset;
        offset += face_indices_size;
        let face_color = offset;
        offset += faces * 2;
        let texture_tri = offset;
        offset += i64::from(self.num_textures) * 6;
        let vertices_x = offset;
        offset += size_x;
        let vertices_y = offset;
        offset += size_y;
        let vertices_z = offset;
        let _ = size_z;
        self.vertices_x = zeros(self.num_vertices);
        self.vertices_y = zeros(self.num_vertices);
        self.vertices_z = zeros(self.num_vertices);
        self.faces_a = zeros(self.num_faces);
        self.faces_b = zeros(self.num_faces);
        self.faces_c = zeros(self.num_faces);
        if self.num_textures > 0 {
            self.textures_mapping = zeros(self.num_textures);
            self.texture_face_x = zeros(self.num_textures);
            self.texture_face_y = zeros(self.num_textures);
            self.texture_face_z = zeros(self.num_textures);
        }
        if has_vertex_skins == 1 {
            self.vertex_groups = zeros(self.num_vertices);
        }
        if face_type_flag == 1 {
            self.face_render_type = zeros(self.num_faces);
            self.faces_texture = zeros(self.num_faces);
            self.faces_material = zeros(self.num_faces);
        }
        if model_priority == 255 {
            self.faces_layer = zeros(self.num_faces);
        } else {
            self.priority = model_priority as i8;
        }
        if has_face_alpha == 1 {
            self.faces_alpha = zeros(self.num_faces);
        }
        if has_face_skins == 1 {
            self.face_groups = zeros(self.num_faces);
        }
        self.faces_color = zeros(self.num_faces);
        cursors[0].set(0);
        cursors[1].set(vertices_x);
        cursors[2].set(vertices_y);
        cursors[3].set(vertices_z);
        cursors[4].set(vertex_skins);
        {
            let [flags, xs, ys, zs, skins] = &mut cursors;
            self.decode_vertices(flags, xs, ys, zs, skins, has_vertex_skins == 1)?;
        }
        cursors[0].set(face_color);
        cursors[1].set(face_type);
        cursors[2].set(face_priority);
        cursors[3].set(face_alpha);
        cursors[4].set(face_skins);
        for face in 0..faces {
            let color = cursors[0].ushort()? as i16;
            put(&mut self.faces_color, face, color)?;
            if face_type_flag == 1 {
                let bits = cursors[1].ubyte()?;
                if bits & 1 == 1 {
                    put(&mut self.face_render_type, face, 1)?;
                    has_face_types = true;
                } else {
                    put(&mut self.face_render_type, face, 0)?;
                }
                if bits & 2 == 2 {
                    put(&mut self.faces_texture, face, (bits >> 2) as i8)?;
                    put(&mut self.faces_material, face, color)?;
                    put(&mut self.faces_color, face, 127)?;
                    if color != -1 {
                        has_materials = true;
                    }
                } else {
                    put(&mut self.faces_texture, face, -1)?;
                    put(&mut self.faces_material, face, -1)?;
                }
            }
            if model_priority == 255 {
                let value = cursors[2].signed()?;
                put(&mut self.faces_layer, face, value)?;
            }
            if has_face_alpha == 1 {
                let value = cursors[3].signed()?;
                put(&mut self.faces_alpha, face, value)?;
            }
            if has_face_skins == 1 {
                let value = cursors[4].ubyte()?;
                put(&mut self.face_groups, face, value)?;
            }
        }
        self.max_depth = -1;
        cursors[0].set(face_indices_data);
        cursors[1].set(face_index_types);
        {
            let [first, second, ..] = &mut cursors;
            self.calculate_max_depth(first, second)?;
        }
        cursors[0].set(texture_tri);
        for texture in 0..i64::from(self.num_textures) {
            put(&mut self.textures_mapping, texture, 0)?;
            let x = cursors[0].ushort()? as i16;
            let y = cursors[0].ushort()? as i16;
            let z = cursors[0].ushort()? as i16;
            put(&mut self.texture_face_x, texture, x)?;
            put(&mut self.texture_face_y, texture, y)?;
            put(&mut self.texture_face_z, texture, z)?;
        }
        if self.faces_texture.is_some() {
            let mut has_textures = false;
            for face in 0..faces {
                let texture = i64::from(at(&self.faces_texture, face)? as u8);
                if texture != 255 {
                    let same = at(&self.faces_a, face)? == at(&self.texture_face_x, texture)?
                        && at(&self.faces_b, face)? == at(&self.texture_face_y, texture)?
                        && at(&self.faces_c, face)? == at(&self.texture_face_z, texture)?;
                    if same {
                        put(&mut self.faces_texture, face, -1)?;
                    } else {
                        has_textures = true;
                    }
                }
            }
            if !has_textures {
                self.faces_texture = None;
            }
        }
        if !has_materials {
            self.faces_material = None;
        }
        if !has_face_types {
            self.face_render_type = None;
        }
        Ok(())
    }

    pub fn upscale(&mut self) -> Res<()> {
        for array in [
            &mut self.vertices_x,
            &mut self.vertices_y,
            &mut self.vertices_z,
        ] {
            let array = array.as_mut().ok_or(MeshError::MissingArray)?;
            let count = usize::try_from(self.num_vertices).map_err(|_| MeshError::Index)?;
            for value in array.get_mut(..count).ok_or(MeshError::Index)? {
                *value = value.wrapping_shl(2);
            }
        }
        if self.num_textures > 0 {
            if let Some(count) = self.textures_scale_x.as_ref().map(Vec::len) {
                for texture in 0..count {
                    let index = texture as i64;
                    let x = at(&self.textures_scale_x, index)?.wrapping_shl(2);
                    put(&mut self.textures_scale_x, index, x)?;
                    let y = at(&self.textures_scale_y, index)?.wrapping_shl(2);
                    put(&mut self.textures_scale_y, index, y)?;
                    if at(&self.textures_mapping, index)? != 1 {
                        let z = at(&self.textures_scale_z, index)?.wrapping_shl(2);
                        put(&mut self.textures_scale_z, index, z)?;
                    }
                }
            }
        }
        Ok(())
    }

    pub fn recolor(&mut self, src: i16, dst: i16) -> Res<()> {
        let count = usize::try_from(self.num_faces).map_err(|_| MeshError::Index)?;
        let colors = self.faces_color.as_mut().ok_or(MeshError::MissingArray)?;
        for value in colors.get_mut(..count).ok_or(MeshError::Index)? {
            if *value == src {
                *value = dst;
            }
        }
        Ok(())
    }

    pub fn retexture(&mut self, src: i16, dst: i16) -> Res<()> {
        let count = usize::try_from(self.num_faces).map_err(|_| MeshError::Index)?;
        if let Some(materials) = self.faces_material.as_mut() {
            for value in materials.get_mut(..count).ok_or(MeshError::Index)? {
                if *value == src {
                    *value = dst;
                }
            }
        }
        Ok(())
    }

    pub fn translate(&mut self, dx: i32, dy: i32, dz: i32) -> Res<()> {
        let count = usize::try_from(self.num_vertices).map_err(|_| MeshError::Index)?;
        for (array, delta) in [
            (&mut self.vertices_x, dx),
            (&mut self.vertices_y, dy),
            (&mut self.vertices_z, dz),
        ] {
            let array = array.as_mut().ok_or(MeshError::MissingArray)?;
            for value in array.get_mut(..count).ok_or(MeshError::Index)? {
                *value = value.wrapping_add(delta);
            }
        }
        Ok(())
    }

    fn connect(&mut self, source: &Mesh, vertex: i64, flag: i16) -> Res<i32> {
        let x = at(&source.vertices_x, vertex)?;
        let y = at(&source.vertices_y, vertex)?;
        let z = at(&source.vertices_z, vertex)?;
        for index in 0..i64::from(self.num_vertices) {
            if x == at(&self.vertices_x, index)?
                && y == at(&self.vertices_y, index)?
                && z == at(&self.vertices_z, index)?
            {
                let merged = at(&self.vertices_submeshes, index)? | flag;
                put(&mut self.vertices_submeshes, index, merged)?;
                return Ok(index as i32);
            }
        }
        let slot = i64::from(self.num_vertices);
        put(&mut self.vertices_x, slot, x)?;
        put(&mut self.vertices_y, slot, y)?;
        put(&mut self.vertices_z, slot, z)?;
        put(&mut self.vertices_submeshes, slot, flag)?;
        let group = if source.vertex_groups.is_some() {
            at(&source.vertex_groups, vertex)?
        } else {
            -1
        };
        put(&mut self.vertex_groups, slot, group)?;
        self.num_vertices += 1;
        Ok(slot as i32)
    }

    #[allow(clippy::too_many_lines)]
    pub fn combine(meshes: &[Option<&Mesh>]) -> Res<Mesh> {
        let mut out = Mesh {
            version: 12,
            priority: -1,
            ..Mesh::default()
        };
        let (mut emitters, mut effectors, mut billboards) = (0usize, 0usize, 0usize);
        let (mut has_render_type, mut has_priorities, mut has_alpha) = (false, false, false);
        let (mut has_face_texture, mut has_face_material, mut has_face_label) =
            (false, false, false);
        for mesh in meshes.iter().flatten() {
            out.num_vertices += mesh.num_vertices;
            out.num_faces += mesh.num_faces;
            out.num_textures += mesh.num_textures;
            emitters += mesh.particle_emitters.as_ref().map_or(0, Vec::len);
            effectors += mesh.particle_effectors.as_ref().map_or(0, Vec::len);
            billboards += mesh.billboards.as_ref().map_or(0, Vec::len);
            has_render_type |= mesh.face_render_type.is_some();
            if mesh.faces_layer.is_some() {
                has_priorities = true;
            } else {
                if out.priority == -1 {
                    out.priority = mesh.priority;
                }
                if out.priority != mesh.priority {
                    has_priorities = true;
                }
            }
            has_alpha |= mesh.faces_alpha.is_some();
            has_face_texture |= mesh.faces_texture.is_some();
            has_face_material |= mesh.faces_material.is_some();
            has_face_label |= mesh.face_groups.is_some();
        }
        out.vertices_x = zeros(out.num_vertices);
        out.vertices_y = zeros(out.num_vertices);
        out.vertices_z = zeros(out.num_vertices);
        out.vertex_groups = zeros(out.num_vertices);
        out.vertices_submeshes = zeros(out.num_vertices);
        out.faces_a = zeros(out.num_faces);
        out.faces_b = zeros(out.num_faces);
        out.faces_c = zeros(out.num_faces);
        if has_render_type {
            out.face_render_type = zeros(out.num_faces);
        }
        if has_priorities {
            out.faces_layer = zeros(out.num_faces);
        }
        if has_alpha {
            out.faces_alpha = zeros(out.num_faces);
        }
        if has_face_texture {
            out.faces_texture = zeros(out.num_faces);
        }
        out.faces_color = zeros(out.num_faces);
        if has_face_material {
            out.faces_material = zeros(out.num_faces);
        }
        if has_face_label {
            out.face_groups = zeros(out.num_faces);
        }
        out.faces_submeshes = zeros(out.num_faces);
        if out.num_textures > 0 {
            out.textures_mapping = zeros(out.num_textures);
            out.texture_face_x = zeros(out.num_textures);
            out.texture_face_y = zeros(out.num_textures);
            out.texture_face_z = zeros(out.num_textures);
            out.textures_scale_x = zeros(out.num_textures);
            out.textures_scale_y = zeros(out.num_textures);
            out.textures_scale_z = zeros(out.num_textures);
            out.textures_rotation = zeros(out.num_textures);
            out.textures_direction = zeros(out.num_textures);
            out.textures_speed = zeros(out.num_textures);
            out.textures_utrans = zeros(out.num_textures);
            out.textures_vtrans = zeros(out.num_textures);
        }
        if billboards > 0 {
            out.billboards = Some(vec![
                Billboard {
                    billboard_id: 0,
                    applies_on_face: 0,
                    depth: 0,
                    distance: 0
                };
                billboards
            ]);
        }
        if emitters > 0 {
            out.particle_emitters = Some(vec![
                EmissiveTriangle {
                    producer_id: 0,
                    face_a: 0,
                    face_b: 0,
                    face_c: 0,
                    priority: 0
                };
                emitters
            ]);
        }
        if effectors > 0 {
            out.particle_effectors = Some(vec![
                EffectiveVertex {
                    effect_id: 0,
                    applies_on_vertex: 0
                };
                effectors
            ]);
        }
        out.num_vertices = 0;
        out.num_faces = 0;
        out.num_textures = 0;
        let (mut emitter_at, mut effector_at, mut billboard_at) = (0i64, 0i64, 0i64);
        for (position, entry) in meshes.iter().enumerate() {
            let flag = (1i32.wrapping_shl(position as u32)) as i16;
            let Some(mesh) = entry else { continue };
            if let Some(list) = &mesh.billboards {
                for billboard in list {
                    let moved = Billboard {
                        applies_on_face: billboard.applies_on_face.wrapping_add(out.num_faces),
                        ..*billboard
                    };
                    if let Some(target) = out.billboards.as_mut() {
                        *target
                            .get_mut(billboard_at as usize)
                            .ok_or(MeshError::Index)? = moved;
                    }
                    billboard_at += 1;
                }
            }
            for face in 0..i64::from(mesh.num_faces) {
                let slot = i64::from(out.num_faces);
                if has_render_type && mesh.face_render_type.is_some() {
                    put(
                        &mut out.face_render_type,
                        slot,
                        at(&mesh.face_render_type, face)?,
                    )?;
                }
                if has_priorities {
                    let value = if mesh.faces_layer.is_some() {
                        at(&mesh.faces_layer, face)?
                    } else {
                        mesh.priority
                    };
                    put(&mut out.faces_layer, slot, value)?;
                }
                if has_alpha && mesh.faces_alpha.is_some() {
                    put(&mut out.faces_alpha, slot, at(&mesh.faces_alpha, face)?)?;
                }
                if has_face_material {
                    let value = if mesh.faces_material.is_some() {
                        at(&mesh.faces_material, face)?
                    } else {
                        -1
                    };
                    put(&mut out.faces_material, slot, value)?;
                }
                if has_face_label {
                    let value = if mesh.face_groups.is_some() {
                        at(&mesh.face_groups, face)?
                    } else {
                        -1
                    };
                    put(&mut out.face_groups, slot, value)?;
                }
                let a = out.connect(mesh, i64::from(at(&mesh.faces_a, face)?), flag)?;
                put(&mut out.faces_a, slot, a as i16)?;
                let b = out.connect(mesh, i64::from(at(&mesh.faces_b, face)?), flag)?;
                put(&mut out.faces_b, slot, b as i16)?;
                let c = out.connect(mesh, i64::from(at(&mesh.faces_c, face)?), flag)?;
                put(&mut out.faces_c, slot, c as i16)?;
                put(&mut out.faces_submeshes, slot, flag)?;
                put(&mut out.faces_color, slot, at(&mesh.faces_color, face)?)?;
                out.num_faces += 1;
            }
            if let Some(list) = &mesh.particle_emitters {
                for emitter in list {
                    let a = out.connect(mesh, i64::from(emitter.face_a), flag)?;
                    let b = out.connect(mesh, i64::from(emitter.face_b), flag)?;
                    let c = out.connect(mesh, i64::from(emitter.face_c), flag)?;
                    if let Some(target) = out.particle_emitters.as_mut() {
                        *target
                            .get_mut(emitter_at as usize)
                            .ok_or(MeshError::Index)? = EmissiveTriangle {
                            face_a: a,
                            face_b: b,
                            face_c: c,
                            ..*emitter
                        };
                    }
                    emitter_at += 1;
                }
            }
            if let Some(list) = &mesh.particle_effectors {
                for effector in list {
                    let vertex = out.connect(mesh, i64::from(effector.applies_on_vertex), flag)?;
                    if let Some(target) = out.particle_effectors.as_mut() {
                        *target
                            .get_mut(effector_at as usize)
                            .ok_or(MeshError::Index)? = EffectiveVertex {
                            applies_on_vertex: vertex,
                            ..*effector
                        };
                    }
                    effector_at += 1;
                }
            }
        }
        let mut current_face = 0i64;
        out.max_depth = out.num_vertices;
        for (position, entry) in meshes.iter().enumerate() {
            let flag = (1i32.wrapping_shl(position as u32)) as i16;
            let Some(mesh) = entry else { continue };
            for face in 0..i64::from(mesh.num_faces) {
                if has_face_texture {
                    let value = match &mesh.faces_texture {
                        Some(_) if at(&mesh.faces_texture, face)? != -1 => {
                            (out.num_textures
                                .wrapping_add(i32::from(at(&mesh.faces_texture, face)?)))
                                as i8
                        }
                        _ => -1,
                    };
                    put(&mut out.faces_texture, current_face, value)?;
                    current_face += 1;
                }
            }
            for texture in 0..i64::from(mesh.num_textures) {
                let slot = i64::from(out.num_textures);
                let mapping = at(&mesh.textures_mapping, texture)?;
                put(&mut out.textures_mapping, slot, mapping)?;
                if mapping == 0 {
                    let x =
                        out.connect(mesh, i64::from(at(&mesh.texture_face_x, texture)?), flag)?;
                    put(&mut out.texture_face_x, slot, x as i16)?;
                    let y =
                        out.connect(mesh, i64::from(at(&mesh.texture_face_y, texture)?), flag)?;
                    put(&mut out.texture_face_y, slot, y as i16)?;
                    let z =
                        out.connect(mesh, i64::from(at(&mesh.texture_face_z, texture)?), flag)?;
                    put(&mut out.texture_face_z, slot, z as i16)?;
                }
                if (1..=3).contains(&mapping) {
                    put(
                        &mut out.texture_face_x,
                        slot,
                        at(&mesh.texture_face_x, texture)?,
                    )?;
                    put(
                        &mut out.texture_face_y,
                        slot,
                        at(&mesh.texture_face_y, texture)?,
                    )?;
                    put(
                        &mut out.texture_face_z,
                        slot,
                        at(&mesh.texture_face_z, texture)?,
                    )?;
                    put(
                        &mut out.textures_scale_x,
                        slot,
                        at(&mesh.textures_scale_x, texture)?,
                    )?;
                    put(
                        &mut out.textures_scale_y,
                        slot,
                        at(&mesh.textures_scale_y, texture)?,
                    )?;
                    put(
                        &mut out.textures_scale_z,
                        slot,
                        at(&mesh.textures_scale_z, texture)?,
                    )?;
                    put(
                        &mut out.textures_rotation,
                        slot,
                        at(&mesh.textures_rotation, texture)?,
                    )?;
                    put(
                        &mut out.textures_direction,
                        slot,
                        at(&mesh.textures_direction, texture)?,
                    )?;
                    put(
                        &mut out.textures_speed,
                        slot,
                        at(&mesh.textures_speed, texture)?,
                    )?;
                }
                if mapping == 2 {
                    put(
                        &mut out.textures_utrans,
                        slot,
                        at(&mesh.textures_utrans, texture)?,
                    )?;
                    put(
                        &mut out.textures_vtrans,
                        slot,
                        at(&mesh.textures_vtrans, texture)?,
                    )?;
                }
                out.num_textures += 1;
            }
        }
        Ok(out)
    }
}
