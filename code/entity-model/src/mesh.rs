use crate::reader::{at, at_slice, modify, put, put_slice, Buf, ModelError, R};
use crate::trig::{cosine, sine};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Emitter {
    pub producer_id: i32,
    pub face_a: i32,
    pub face_b: i32,
    pub face_c: i32,
    pub priority: i8,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Effector {
    pub effect_id: i32,
    pub applies_on_vertex: i32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Billboard {
    pub id: i32,
    pub applies_on_face: i32,
    pub depth: i32,
    pub distance: i32,
}

#[derive(Debug, Clone, PartialEq, Eq)]
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
    pub particle_emitters: Option<Vec<Emitter>>,
    pub particle_effectors: Option<Vec<Effector>>,
    pub billboards: Option<Vec<Billboard>>,
    pub vertices_submeshes: Option<Vec<i16>>,
    pub faces_submeshes: Option<Vec<i16>>,
}

fn zeros<T: Default + Clone>(count: i32) -> Option<Vec<T>> {
    Some(vec![T::default(); count.max(0) as usize])
}

fn len_of<T>(values: &Option<Vec<T>>) -> R<usize> {
    values.as_ref().map(Vec::len).ok_or(ModelError::Null)
}

fn max_face(mesh: &mut Mesh, first: &mut Buf, second: &mut Buf) -> R<()> {
    let mut a: i16 = 0;
    let mut b: i16 = 0;
    let mut c: i16 = 0;
    let mut tmp: i16 = 0;
    for face in 0..mesh.num_faces {
        let kind = second.u8()?;
        if kind == 1 {
            a = (first.smart()?.wrapping_add(i32::from(tmp))) as i16;
            b = (first.smart()?.wrapping_add(i32::from(a))) as i16;
            c = (first.smart()?.wrapping_add(i32::from(b))) as i16;
            tmp = c;
            put(&mut mesh.faces_a, i64::from(face), a)?;
            put(&mut mesh.faces_b, i64::from(face), b)?;
            put(&mut mesh.faces_c, i64::from(face), c)?;
            for value in [a, b, c] {
                if i32::from(value) > mesh.max_depth {
                    mesh.max_depth = i32::from(value);
                }
            }
        }
        if kind == 2 {
            b = c;
            c = (first.smart()?.wrapping_add(i32::from(tmp))) as i16;
            tmp = c;
            put(&mut mesh.faces_a, i64::from(face), a)?;
            put(&mut mesh.faces_b, i64::from(face), b)?;
            put(&mut mesh.faces_c, i64::from(face), c)?;
            if i32::from(c) > mesh.max_depth {
                mesh.max_depth = i32::from(c);
            }
        }
        if kind == 3 {
            a = c;
            c = (first.smart()?.wrapping_add(i32::from(tmp))) as i16;
            tmp = c;
            put(&mut mesh.faces_a, i64::from(face), a)?;
            put(&mut mesh.faces_b, i64::from(face), b)?;
            put(&mut mesh.faces_c, i64::from(face), c)?;
            if i32::from(c) > mesh.max_depth {
                mesh.max_depth = i32::from(c);
            }
        }
        if kind == 4 {
            std::mem::swap(&mut a, &mut b);
            c = (first.smart()?.wrapping_add(i32::from(tmp))) as i16;
            tmp = c;
            put(&mut mesh.faces_a, i64::from(face), a)?;
            put(&mut mesh.faces_b, i64::from(face), b)?;
            put(&mut mesh.faces_c, i64::from(face), c)?;
            if i32::from(c) > mesh.max_depth {
                mesh.max_depth = i32::from(c);
            }
        }
    }
    mesh.max_depth += 1;
    Ok(())
}

impl Mesh {
    pub fn empty() -> Self {
        Self {
            version: 12,
            num_vertices: 0,
            max_depth: 0,
            num_faces: 0,
            priority: 0,
            num_textures: 0,
            textures_mapping: None,
            vertices_x: None,
            vertices_y: None,
            vertices_z: None,
            faces_a: None,
            faces_b: None,
            faces_c: None,
            vertex_groups: None,
            face_render_type: None,
            faces_layer: None,
            faces_alpha: None,
            face_groups: None,
            faces_material: None,
            faces_color: None,
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

    pub fn decode(data: &[u8]) -> R<Self> {
        let size = data.len() as i64;
        let last = at_slice(data, size - 1)?;
        let before = at_slice(data, size - 2)?;
        if last == 0xff && before == 0xff {
            Self::decode_new(data)
        } else {
            Self::decode_old(data)
        }
    }

    fn decode_new(data: &[u8]) -> R<Self> {
        let mut m = Self::empty();
        let mut first = Buf::new(data);
        let mut second = Buf::new(data);
        let mut third = Buf::new(data);
        let mut fourth = Buf::new(data);
        let mut fifth = Buf::new(data);
        let mut seventh = Buf::new(data);
        let mut eight = Buf::new(data);
        first.pos = data.len() as i64 - 23;
        m.num_vertices = first.u16()?;
        m.num_faces = first.u16()?;
        m.num_textures = first.u8()?;
        let footer_flags = first.u8()?;
        let has_face_types = footer_flags & 0x1 == 1;
        let has_particles = footer_flags & 0x2 == 2;
        let has_billboards = footer_flags & 0x4 == 4;
        let has_version = footer_flags & 0x8 == 8;
        if has_version {
            first.pos -= 7;
            m.version = first.u8()?;
            first.pos += 6;
        }
        let model_priority = first.u8()?;
        let has_face_alpha = first.u8()?;
        let has_face_skins = first.u8()?;
        let has_face_textures = first.u8()?;
        let has_vertex_skins = first.u8()?;
        let model_x = first.u16()?;
        let model_y = first.u16()?;
        let model_z = first.u16()?;
        let face_indices = first.u16()?;
        let texture_indices = first.u16()?;
        let mut simple = 0i64;
        let mut complex = 0i64;
        let mut cube = 0i64;
        if m.num_textures > 0 {
            let mut mapping = vec![0i8; m.num_textures as usize];
            first.pos = 0;
            for slot in mapping.iter_mut() {
                let value = first.i8()? as i8;
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
            m.textures_mapping = Some(mapping);
        }
        let nv = i64::from(m.num_vertices);
        let nf = i64::from(m.num_faces);
        let mut index = i64::from(m.num_textures);
        let vertex_flags_offset = index;
        index += nv;
        let face_types_offset = index;
        if has_face_types {
            index += nf;
        }
        let face_index_types_offset = index;
        index += nf;
        let face_priority_offset = index;
        if model_priority == 255 {
            index += nf;
        }
        let face_skins_offset = index;
        if has_face_skins == 1 {
            index += nf;
        }
        let vertex_skins_offset = index;
        if has_vertex_skins == 1 {
            index += nv;
        }
        let face_alpha_offset = index;
        if has_face_alpha == 1 {
            index += nf;
        }
        let face_indices_offset = index;
        index += i64::from(face_indices);
        let face_material_offset = index;
        if has_face_textures == 1 {
            index += nf * 2;
        }
        let texture_index_offset = index;
        index += i64::from(texture_indices);
        let face_color_offset = index;
        index += nf * 2;
        let vertices_x_offset = index;
        index += i64::from(model_x);
        let vertices_y_offset = index;
        index += i64::from(model_y);
        let vertices_z_offset = index;
        index += i64::from(model_z);
        let simple_offset = index;
        index += simple * 6;
        let complex_vert_offset = index;
        index += complex * 6;
        let texture_bytes: i64 = if m.version == 14 {
            7
        } else if m.version >= 15 {
            9
        } else {
            6
        };
        let complex_scale_offset = index;
        index += complex * texture_bytes;
        let complex_rot_offset = index;
        index += complex;
        let complex_dir_offset = index;
        index += complex;
        let complex_speed_offset = index;
        index = cube * 2 + index + complex;
        m.vertices_x = zeros(m.num_vertices);
        m.vertices_y = zeros(m.num_vertices);
        m.vertices_z = zeros(m.num_vertices);
        m.faces_a = zeros(m.num_faces);
        m.faces_b = zeros(m.num_faces);
        m.faces_c = zeros(m.num_faces);
        if has_vertex_skins == 1 {
            m.vertex_groups = zeros(m.num_vertices);
        }
        if has_face_types {
            m.face_render_type = zeros(m.num_faces);
        }
        if model_priority == 255 {
            m.faces_layer = zeros(m.num_faces);
        } else {
            m.priority = model_priority as i8;
        }
        if has_face_alpha == 1 {
            m.faces_alpha = zeros(m.num_faces);
        }
        if has_face_skins == 1 {
            m.face_groups = zeros(m.num_faces);
        }
        if has_face_textures == 1 {
            m.faces_material = zeros(m.num_faces);
        }
        if has_face_textures == 1 && m.num_textures > 0 {
            m.faces_texture = zeros(m.num_faces);
        }
        m.faces_color = zeros(m.num_faces);
        if m.num_textures > 0 {
            m.texture_face_x = zeros(m.num_textures);
            m.texture_face_y = zeros(m.num_textures);
            m.texture_face_z = zeros(m.num_textures);
            if complex > 0 {
                m.textures_scale_x = zeros(complex as i32);
                m.textures_scale_y = zeros(complex as i32);
                m.textures_scale_z = zeros(complex as i32);
                m.textures_rotation = zeros(complex as i32);
                m.textures_direction = zeros(complex as i32);
                m.textures_speed = zeros(complex as i32);
            }
            if cube > 0 {
                m.textures_utrans = zeros(cube as i32);
                m.textures_vtrans = zeros(cube as i32);
            }
        }
        first.pos = vertex_flags_offset;
        second.pos = vertices_x_offset;
        third.pos = vertices_y_offset;
        fourth.pos = vertices_z_offset;
        fifth.pos = vertex_skins_offset;
        let mut last_x = 0i32;
        let mut last_y = 0i32;
        let mut last_z = 0i32;
        for vertex in 0..nv {
            let flags = first.u8()?;
            let dx = if flags & 1 != 0 { second.smart()? } else { 0 };
            let dy = if flags & 2 != 0 { third.smart()? } else { 0 };
            let dz = if flags & 4 != 0 { fourth.smart()? } else { 0 };
            last_x = last_x.wrapping_add(dx);
            last_y = last_y.wrapping_add(dy);
            last_z = last_z.wrapping_add(dz);
            put(&mut m.vertices_x, vertex, last_x)?;
            put(&mut m.vertices_y, vertex, last_y)?;
            put(&mut m.vertices_z, vertex, last_z)?;
            if has_vertex_skins == 1 {
                put(&mut m.vertex_groups, vertex, fifth.u8()?)?;
            }
        }
        first.pos = face_color_offset;
        second.pos = face_types_offset;
        third.pos = face_priority_offset;
        fourth.pos = face_alpha_offset;
        fifth.pos = face_skins_offset;
        seventh.pos = face_material_offset;
        eight.pos = texture_index_offset;
        for face in 0..nf {
            put(&mut m.faces_color, face, first.u16()? as i16)?;
            if has_face_types {
                put(&mut m.face_render_type, face, second.i8()? as i8)?;
            }
            if model_priority == 255 {
                put(&mut m.faces_layer, face, third.i8()? as i8)?;
            }
            if has_face_alpha == 1 {
                put(&mut m.faces_alpha, face, fourth.i8()? as i8)?;
            }
            if has_face_skins == 1 {
                put(&mut m.face_groups, face, fifth.u8()?)?;
            }
            if has_face_textures == 1 {
                put(&mut m.faces_material, face, (seventh.u16()? - 1) as i16)?;
            }
            if m.faces_texture.is_some() {
                if at(&m.faces_material, face)? != -1 {
                    put(&mut m.faces_texture, face, (eight.u8()? - 1) as i8)?;
                } else {
                    put(&mut m.faces_texture, face, -1)?;
                }
            }
        }
        m.max_depth = -1;
        first.pos = face_indices_offset;
        second.pos = face_index_types_offset;
        max_face(&mut m, &mut first, &mut second)?;
        first.pos = simple_offset;
        second.pos = complex_vert_offset;
        third.pos = complex_scale_offset;
        fourth.pos = complex_rot_offset;
        fifth.pos = complex_dir_offset;
        seventh.pos = complex_speed_offset;
        m.decode_textured(
            &mut first,
            &mut second,
            &mut third,
            &mut fourth,
            &mut fifth,
            &mut seventh,
        )?;
        first.pos = index;
        if has_particles {
            let emitters = first.u8()?;
            if emitters > 0 {
                let mut list = Vec::with_capacity(emitters as usize);
                for _ in 0..emitters {
                    let particle = first.u16()?;
                    let face = first.u16()?;
                    let priority = if model_priority == 255 {
                        at(&m.faces_layer, i64::from(face))?
                    } else {
                        model_priority as i8
                    };
                    list.push(Emitter {
                        producer_id: particle,
                        face_a: i32::from(at(&m.faces_a, i64::from(face))?),
                        face_b: i32::from(at(&m.faces_b, i64::from(face))?),
                        face_c: i32::from(at(&m.faces_c, i64::from(face))?),
                        priority,
                    });
                }
                m.particle_emitters = Some(list);
            }
            let effectors = first.u8()?;
            if effectors > 0 {
                let mut list = Vec::with_capacity(effectors as usize);
                for _ in 0..effectors {
                    let effect_id = first.u16()?;
                    let applies_on_vertex = first.u16()?;
                    list.push(Effector {
                        effect_id,
                        applies_on_vertex,
                    });
                }
                m.particle_effectors = Some(list);
            }
        }
        if has_billboards {
            let count = first.u8()?;
            if count > 0 {
                let mut list = Vec::with_capacity(count as usize);
                for _ in 0..count {
                    let id = first.u16()?;
                    let face = first.u16()?;
                    let depth = first.u8()?;
                    let distance = first.i8()?;
                    list.push(Billboard {
                        id,
                        applies_on_face: face,
                        depth,
                        distance,
                    });
                }
                m.billboards = Some(list);
            }
        }
        Ok(m)
    }

    fn decode_textured(
        &mut self,
        simple: &mut Buf,
        vert: &mut Buf,
        scale: &mut Buf,
        rot: &mut Buf,
        dir: &mut Buf,
        speed: &mut Buf,
    ) -> R<()> {
        for texture in 0..self.num_textures {
            let kind = i32::from(at(&self.textures_mapping, i64::from(texture))? as u8);
            let t = i64::from(texture);
            if kind == 0 {
                put(&mut self.texture_face_x, t, simple.u16()? as i16)?;
                put(&mut self.texture_face_y, t, simple.u16()? as i16)?;
                put(&mut self.texture_face_z, t, simple.u16()? as i16)?;
            }
            if (1..=3).contains(&kind) {
                put(&mut self.texture_face_x, t, vert.u16()? as i16)?;
                put(&mut self.texture_face_y, t, vert.u16()? as i16)?;
                put(&mut self.texture_face_z, t, vert.u16()? as i16)?;
                if self.version < 15 {
                    put(&mut self.textures_scale_x, t, scale.u16()?)?;
                    if self.version < 14 {
                        put(&mut self.textures_scale_y, t, scale.u16()?)?;
                    } else {
                        put(&mut self.textures_scale_y, t, scale.u24()?)?;
                    }
                    put(&mut self.textures_scale_z, t, scale.u16()?)?;
                } else {
                    put(&mut self.textures_scale_x, t, scale.u24()?)?;
                    put(&mut self.textures_scale_y, t, scale.u24()?)?;
                    put(&mut self.textures_scale_z, t, scale.u24()?)?;
                }
                put(&mut self.textures_rotation, t, rot.i8()? as i8)?;
                put(&mut self.textures_direction, t, dir.i8()? as i8)?;
                put(&mut self.textures_speed, t, speed.i8()?)?;
                if kind == 2 {
                    put(&mut self.textures_utrans, t, speed.i8()?)?;
                    put(&mut self.textures_vtrans, t, speed.i8()?)?;
                }
            }
        }
        Ok(())
    }

    fn decode_old(data: &[u8]) -> R<Self> {
        let mut m = Self::empty();
        let mut has_face_types = false;
        let mut has_materials = false;
        let mut header = Buf::new(data);
        let mut vx = Buf::new(data);
        let mut vy = Buf::new(data);
        let mut vz = Buf::new(data);
        let mut skin = Buf::new(data);
        header.pos = data.len() as i64 - 18;
        m.num_vertices = header.u16()?;
        m.num_faces = header.u16()?;
        m.num_textures = header.u8()?;
        let face_type_flag = header.u8()?;
        let model_priority = header.u8()?;
        let has_face_alpha = header.u8()?;
        let has_face_skins = header.u8()?;
        let has_vertex_skins = header.u8()?;
        let size_x = i64::from(header.u16()?);
        let size_y = i64::from(header.u16()?);
        header.u16()?;
        let face_indices_size = i64::from(header.u16()?);
        let nv = i64::from(m.num_vertices);
        let nf = i64::from(m.num_faces);
        let nt = i64::from(m.num_textures);
        let mut offset = nv;
        let face_index_types_offset = offset;
        offset += nf;
        let face_priority_offset = offset;
        if model_priority == 255 {
            offset += nf;
        }
        let face_skins_offset = offset;
        if has_face_skins == 1 {
            offset += nf;
        }
        let face_type_offset = offset;
        if face_type_flag == 1 {
            offset += nf;
        }
        let vertex_skins_offset = offset;
        if has_vertex_skins == 1 {
            offset += nv;
        }
        let face_alpha_offset = offset;
        if has_face_alpha == 1 {
            offset += nf;
        }
        let face_indices_offset = offset;
        offset += face_indices_size;
        let face_color_offset = offset;
        offset += nf * 2;
        let texture_tri_offset = offset;
        offset += nt * 6;
        let vertices_x_offset = offset;
        offset += size_x;
        let vertices_y_offset = offset;
        offset += size_y;
        m.vertices_x = zeros(m.num_vertices);
        m.vertices_y = zeros(m.num_vertices);
        m.vertices_z = zeros(m.num_vertices);
        m.faces_a = zeros(m.num_faces);
        m.faces_b = zeros(m.num_faces);
        m.faces_c = zeros(m.num_faces);
        if m.num_textures > 0 {
            m.textures_mapping = zeros(m.num_textures);
            m.texture_face_x = zeros(m.num_textures);
            m.texture_face_y = zeros(m.num_textures);
            m.texture_face_z = zeros(m.num_textures);
        }
        if has_vertex_skins == 1 {
            m.vertex_groups = zeros(m.num_vertices);
        }
        if face_type_flag == 1 {
            m.face_render_type = zeros(m.num_faces);
            m.faces_texture = zeros(m.num_faces);
            m.faces_material = zeros(m.num_faces);
        }
        if model_priority == 255 {
            m.faces_layer = zeros(m.num_faces);
        } else {
            m.priority = model_priority as i8;
        }
        if has_face_alpha == 1 {
            m.faces_alpha = zeros(m.num_faces);
        }
        if has_face_skins == 1 {
            m.face_groups = zeros(m.num_faces);
        }
        m.faces_color = zeros(m.num_faces);
        header.pos = 0;
        vx.pos = vertices_x_offset;
        vy.pos = vertices_y_offset;
        vz.pos = offset;
        skin.pos = vertex_skins_offset;
        let mut last_x = 0i32;
        let mut last_y = 0i32;
        let mut last_z = 0i32;
        for vertex in 0..nv {
            let flags = header.u8()?;
            let dx = if flags & 1 != 0 { vx.smart()? } else { 0 };
            let dy = if flags & 2 != 0 { vy.smart()? } else { 0 };
            let dz = if flags & 4 != 0 { vz.smart()? } else { 0 };
            last_x = last_x.wrapping_add(dx);
            last_y = last_y.wrapping_add(dy);
            last_z = last_z.wrapping_add(dz);
            put(&mut m.vertices_x, vertex, last_x)?;
            put(&mut m.vertices_y, vertex, last_y)?;
            put(&mut m.vertices_z, vertex, last_z)?;
            if has_vertex_skins == 1 {
                put(&mut m.vertex_groups, vertex, skin.u8()?)?;
            }
        }
        header.pos = face_color_offset;
        vx.pos = face_type_offset;
        vy.pos = face_priority_offset;
        vz.pos = face_alpha_offset;
        skin.pos = face_skins_offset;
        for face in 0..nf {
            put(&mut m.faces_color, face, header.u16()? as i16)?;
            if face_type_flag == 1 {
                let flags = vx.u8()?;
                if flags & 1 == 1 {
                    put(&mut m.face_render_type, face, 1)?;
                    has_face_types = true;
                } else {
                    put(&mut m.face_render_type, face, 0)?;
                }
                if flags & 2 == 2 {
                    put(&mut m.faces_texture, face, (flags >> 2) as i8)?;
                    let color = at(&m.faces_color, face)?;
                    put(&mut m.faces_material, face, color)?;
                    put(&mut m.faces_color, face, 127)?;
                    if at(&m.faces_material, face)? != -1 {
                        has_materials = true;
                    }
                } else {
                    put(&mut m.faces_texture, face, -1)?;
                    put(&mut m.faces_material, face, -1)?;
                }
            }
            if model_priority == 255 {
                put(&mut m.faces_layer, face, vy.i8()? as i8)?;
            }
            if has_face_alpha == 1 {
                put(&mut m.faces_alpha, face, vz.i8()? as i8)?;
            }
            if has_face_skins == 1 {
                put(&mut m.face_groups, face, skin.u8()?)?;
            }
        }
        m.max_depth = -1;
        header.pos = face_indices_offset;
        vx.pos = face_index_types_offset;
        max_face(&mut m, &mut header, &mut vx)?;
        header.pos = texture_tri_offset;
        for texture in 0..nt {
            put(&mut m.textures_mapping, texture, 0)?;
            put(&mut m.texture_face_x, texture, header.u16()? as i16)?;
            put(&mut m.texture_face_y, texture, header.u16()? as i16)?;
            put(&mut m.texture_face_z, texture, header.u16()? as i16)?;
        }
        if m.faces_texture.is_some() {
            let mut has_textures = false;
            for face in 0..nf {
                let texture = i64::from(at(&m.faces_texture, face)? as u8);
                if texture != 255 {
                    let same = at(&m.faces_a, face)? == at(&m.texture_face_x, texture)?
                        && at(&m.faces_b, face)? == at(&m.texture_face_y, texture)?
                        && at(&m.faces_c, face)? == at(&m.texture_face_z, texture)?;
                    if same {
                        put(&mut m.faces_texture, face, -1)?;
                    } else {
                        has_textures = true;
                    }
                }
            }
            if !has_textures {
                m.faces_texture = None;
            }
        }
        if !has_materials {
            m.faces_material = None;
        }
        if !has_face_types {
            m.face_render_type = None;
        }
        Ok(m)
    }

    fn connect(&mut self, mesh: &Mesh, vertex: i64, submesh: i16) -> R<i32> {
        let x = at(&mesh.vertices_x, vertex)?;
        let y = at(&mesh.vertices_y, vertex)?;
        let z = at(&mesh.vertices_z, vertex)?;
        for index in 0..i64::from(self.num_vertices) {
            if x == at(&self.vertices_x, index)?
                && y == at(&self.vertices_y, index)?
                && z == at(&self.vertices_z, index)?
            {
                let flags = at(&self.vertices_submeshes, index)?;
                put(&mut self.vertices_submeshes, index, flags | submesh)?;
                return Ok(index as i32);
            }
        }
        let slot = i64::from(self.num_vertices);
        put(&mut self.vertices_x, slot, x)?;
        put(&mut self.vertices_y, slot, y)?;
        put(&mut self.vertices_z, slot, z)?;
        put(&mut self.vertices_submeshes, slot, submesh)?;
        let group = if mesh.vertex_groups.is_some() {
            at(&mesh.vertex_groups, vertex)?
        } else {
            -1
        };
        put(&mut self.vertex_groups, slot, group)?;
        self.num_vertices += 1;
        Ok(slot as i32)
    }

    pub fn merge(meshes: &[Option<&Mesh>], count: usize) -> R<Self> {
        if count > meshes.len() {
            return Err(ModelError::Bounds);
        }
        let mut m = Self::empty();
        let mut emitters = 0usize;
        let mut effectors = 0usize;
        let mut billboards = 0usize;
        let mut has_render_type = false;
        let mut has_priorities = false;
        let mut has_alpha = false;
        let mut has_face_texture = false;
        let mut has_face_material = false;
        let mut has_face_label = false;
        m.priority = -1;
        for mesh in meshes.iter().take(count).flatten() {
            m.num_vertices += mesh.num_vertices;
            m.num_faces += mesh.num_faces;
            m.num_textures += mesh.num_textures;
            emitters += mesh.particle_emitters.as_ref().map_or(0, Vec::len);
            effectors += mesh.particle_effectors.as_ref().map_or(0, Vec::len);
            billboards += mesh.billboards.as_ref().map_or(0, Vec::len);
            has_render_type |= mesh.face_render_type.is_some();
            if mesh.faces_layer.is_some() {
                has_priorities = true;
            } else {
                if m.priority == -1 {
                    m.priority = mesh.priority;
                }
                if m.priority != mesh.priority {
                    has_priorities = true;
                }
            }
            has_alpha |= mesh.faces_alpha.is_some();
            has_face_texture |= mesh.faces_texture.is_some();
            has_face_material |= mesh.faces_material.is_some();
            has_face_label |= mesh.face_groups.is_some();
        }
        m.vertices_x = zeros(m.num_vertices);
        m.vertices_y = zeros(m.num_vertices);
        m.vertices_z = zeros(m.num_vertices);
        m.vertex_groups = zeros(m.num_vertices);
        m.vertices_submeshes = zeros(m.num_vertices);
        m.faces_a = zeros(m.num_faces);
        m.faces_b = zeros(m.num_faces);
        m.faces_c = zeros(m.num_faces);
        if has_render_type {
            m.face_render_type = zeros(m.num_faces);
        }
        if has_priorities {
            m.faces_layer = zeros(m.num_faces);
        }
        if has_alpha {
            m.faces_alpha = zeros(m.num_faces);
        }
        if has_face_texture {
            m.faces_texture = zeros(m.num_faces);
        }
        m.faces_color = zeros(m.num_faces);
        if has_face_material {
            m.faces_material = zeros(m.num_faces);
        }
        if has_face_label {
            m.face_groups = zeros(m.num_faces);
        }
        m.faces_submeshes = zeros(m.num_faces);
        if m.num_textures > 0 {
            m.textures_mapping = zeros(m.num_textures);
            m.texture_face_x = zeros(m.num_textures);
            m.texture_face_y = zeros(m.num_textures);
            m.texture_face_z = zeros(m.num_textures);
            m.textures_scale_x = zeros(m.num_textures);
            m.textures_scale_y = zeros(m.num_textures);
            m.textures_scale_z = zeros(m.num_textures);
            m.textures_rotation = zeros(m.num_textures);
            m.textures_direction = zeros(m.num_textures);
            m.textures_speed = zeros(m.num_textures);
            m.textures_utrans = zeros(m.num_textures);
            m.textures_vtrans = zeros(m.num_textures);
        }
        if billboards > 0 {
            m.billboards = Some(vec![
                Billboard {
                    id: 0,
                    applies_on_face: 0,
                    depth: 0,
                    distance: 0
                };
                billboards
            ]);
        }
        if emitters > 0 {
            m.particle_emitters = Some(vec![
                Emitter {
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
            m.particle_effectors = Some(vec![
                Effector {
                    effect_id: 0,
                    applies_on_vertex: 0
                };
                effectors
            ]);
        }
        m.num_vertices = 0;
        m.num_faces = 0;
        m.num_textures = 0;
        let mut emitter_at = 0i64;
        let mut effector_at = 0i64;
        let mut billboard_at = 0i64;
        for (mesh_index, entry) in meshes.iter().enumerate().take(count) {
            let flag = 1i32.wrapping_shl(mesh_index as u32) as i16;
            let Some(mesh) = *entry else {
                continue;
            };
            if let Some(list) = &mesh.billboards {
                for billboard in list {
                    let moved = Billboard {
                        applies_on_face: billboard.applies_on_face.wrapping_add(m.num_faces),
                        ..*billboard
                    };
                    put(&mut m.billboards, billboard_at, moved)?;
                    billboard_at += 1;
                }
            }
            for face in 0..mesh.num_faces {
                let f = i64::from(face);
                let n = i64::from(m.num_faces);
                if has_render_type && mesh.face_render_type.is_some() {
                    put(&mut m.face_render_type, n, at(&mesh.face_render_type, f)?)?;
                }
                if has_priorities {
                    if mesh.faces_layer.is_some() {
                        put(&mut m.faces_layer, n, at(&mesh.faces_layer, f)?)?;
                    } else {
                        put(&mut m.faces_layer, n, mesh.priority)?;
                    }
                }
                if has_alpha && mesh.faces_alpha.is_some() {
                    put(&mut m.faces_alpha, n, at(&mesh.faces_alpha, f)?)?;
                }
                if has_face_material {
                    if mesh.faces_material.is_some() {
                        put(&mut m.faces_material, n, at(&mesh.faces_material, f)?)?;
                    } else {
                        put(&mut m.faces_material, n, -1)?;
                    }
                }
                if has_face_label {
                    if mesh.face_groups.is_some() {
                        put(&mut m.face_groups, n, at(&mesh.face_groups, f)?)?;
                    } else {
                        put(&mut m.face_groups, n, -1)?;
                    }
                }
                let a = m.connect(mesh, i64::from(at(&mesh.faces_a, f)?), flag)?;
                put(&mut m.faces_a, n, a as i16)?;
                let b = m.connect(mesh, i64::from(at(&mesh.faces_b, f)?), flag)?;
                put(&mut m.faces_b, n, b as i16)?;
                let c = m.connect(mesh, i64::from(at(&mesh.faces_c, f)?), flag)?;
                put(&mut m.faces_c, n, c as i16)?;
                put(&mut m.faces_submeshes, n, flag)?;
                put(&mut m.faces_color, n, at(&mesh.faces_color, f)?)?;
                m.num_faces += 1;
            }
            if let Some(list) = &mesh.particle_emitters {
                for emitter in list {
                    let x = m.connect(mesh, i64::from(emitter.face_a), flag)?;
                    let y = m.connect(mesh, i64::from(emitter.face_b), flag)?;
                    let z = m.connect(mesh, i64::from(emitter.face_c), flag)?;
                    let moved = Emitter {
                        face_a: x,
                        face_b: y,
                        face_c: z,
                        ..*emitter
                    };
                    put(&mut m.particle_emitters, emitter_at, moved)?;
                    emitter_at += 1;
                }
            }
            if let Some(list) = &mesh.particle_effectors {
                for effector in list {
                    let vertex = m.connect(mesh, i64::from(effector.applies_on_vertex), flag)?;
                    let moved = Effector {
                        applies_on_vertex: vertex,
                        ..*effector
                    };
                    put(&mut m.particle_effectors, effector_at, moved)?;
                    effector_at += 1;
                }
            }
        }
        let mut current_face = 0i64;
        m.max_depth = m.num_vertices;
        for (mesh_index, entry) in meshes.iter().enumerate().take(count) {
            let flag = 1i32.wrapping_shl(mesh_index as u32) as i16;
            let Some(mesh) = *entry else {
                continue;
            };
            for face in 0..mesh.num_faces {
                if has_face_texture {
                    let source = if mesh.faces_texture.is_some()
                        && at(&mesh.faces_texture, i64::from(face))? != -1
                    {
                        (m.num_textures + i32::from(at(&mesh.faces_texture, i64::from(face))?))
                            as i8
                    } else {
                        -1
                    };
                    put(&mut m.faces_texture, current_face, source)?;
                    current_face += 1;
                }
            }
            for texture in 0..mesh.num_textures {
                let t = i64::from(texture);
                let n = i64::from(m.num_textures);
                let mapping = at(&mesh.textures_mapping, t)?;
                put(&mut m.textures_mapping, n, mapping)?;
                if mapping == 0 {
                    let x = m.connect(mesh, i64::from(at(&mesh.texture_face_x, t)?), flag)?;
                    put(&mut m.texture_face_x, n, x as i16)?;
                    let y = m.connect(mesh, i64::from(at(&mesh.texture_face_y, t)?), flag)?;
                    put(&mut m.texture_face_y, n, y as i16)?;
                    let z = m.connect(mesh, i64::from(at(&mesh.texture_face_z, t)?), flag)?;
                    put(&mut m.texture_face_z, n, z as i16)?;
                }
                if (1..=3).contains(&mapping) {
                    put(&mut m.texture_face_x, n, at(&mesh.texture_face_x, t)?)?;
                    put(&mut m.texture_face_y, n, at(&mesh.texture_face_y, t)?)?;
                    put(&mut m.texture_face_z, n, at(&mesh.texture_face_z, t)?)?;
                    put(&mut m.textures_scale_x, n, at(&mesh.textures_scale_x, t)?)?;
                    put(&mut m.textures_scale_y, n, at(&mesh.textures_scale_y, t)?)?;
                    put(&mut m.textures_scale_z, n, at(&mesh.textures_scale_z, t)?)?;
                    put(&mut m.textures_rotation, n, at(&mesh.textures_rotation, t)?)?;
                    put(
                        &mut m.textures_direction,
                        n,
                        at(&mesh.textures_direction, t)?,
                    )?;
                    put(&mut m.textures_speed, n, at(&mesh.textures_speed, t)?)?;
                }
                if mapping == 2 {
                    put(&mut m.textures_utrans, n, at(&mesh.textures_utrans, t)?)?;
                    put(&mut m.textures_vtrans, n, at(&mesh.textures_vtrans, t)?)?;
                }
                m.num_textures += 1;
            }
        }
        Ok(m)
    }

    pub fn create_vertex_groups(&self, from_num_vertices: bool) -> R<Vec<Vec<i32>>> {
        let mut counts = [0usize; 256];
        let mut count = 0i32;
        let max = if from_num_vertices {
            self.num_vertices
        } else {
            self.max_depth
        };
        for vertex in 0..max {
            let group = at(&self.vertex_groups, i64::from(vertex))?;
            if group >= 0 {
                *counts.get_mut(group as usize).ok_or(ModelError::Bounds)? += 1;
                if group > count {
                    count = group;
                }
            }
        }
        let mut grouped: Vec<Vec<i32>> = Vec::new();
        for group in 0..=count {
            grouped.push(vec![0; at_slice(&counts, i64::from(group))?]);
            counts[group as usize] = 0;
        }
        for vertex in 0..max {
            let group = at(&self.vertex_groups, i64::from(vertex))?;
            if group >= 0 {
                let slot = counts[group as usize];
                put_slice(&mut grouped[group as usize], slot as i64, vertex)?;
                counts[group as usize] += 1;
            }
        }
        Ok(grouped)
    }

    pub fn create_face_groups(&self) -> R<Vec<Vec<i32>>> {
        let mut counts = [0usize; 256];
        let mut count = 0i32;
        for face in 0..self.num_faces {
            let group = at(&self.face_groups, i64::from(face))?;
            if group >= 0 {
                *counts.get_mut(group as usize).ok_or(ModelError::Bounds)? += 1;
                if group > count {
                    count = group;
                }
            }
        }
        let mut grouped: Vec<Vec<i32>> = Vec::new();
        for group in 0..=count {
            grouped.push(vec![0; at_slice(&counts, i64::from(group))?]);
            counts[group as usize] = 0;
        }
        for face in 0..self.num_faces {
            let group = at(&self.face_groups, i64::from(face))?;
            if group >= 0 {
                let slot = counts[group as usize];
                put_slice(&mut grouped[group as usize], slot as i64, face)?;
                counts[group as usize] += 1;
            }
        }
        Ok(grouped)
    }

    pub fn create_billboard_groups(&self) -> R<Vec<Vec<i32>>> {
        let list = self.billboards.as_ref().ok_or(ModelError::Null)?;
        let mut counts = [0usize; 256];
        let mut count = 0i32;
        for billboard in list {
            let group = billboard.depth;
            if group >= 0 {
                *counts.get_mut(group as usize).ok_or(ModelError::Bounds)? += 1;
                if group > count {
                    count = group;
                }
            }
        }
        let mut grouped: Vec<Vec<i32>> = Vec::new();
        for group in 0..=count {
            grouped.push(vec![0; at_slice(&counts, i64::from(group))?]);
            counts[group as usize] = 0;
        }
        for (index, billboard) in list.iter().enumerate() {
            let group = billboard.depth;
            if group >= 0 {
                let slot = counts[group as usize];
                put_slice(&mut grouped[group as usize], slot as i64, index as i32)?;
                counts[group as usize] += 1;
            }
        }
        Ok(grouped)
    }

    pub fn recolor(&mut self, src: i16, dst: i16) -> R<()> {
        for face in 0..self.num_faces {
            if at(&self.faces_color, i64::from(face))? == src {
                put(&mut self.faces_color, i64::from(face), dst)?;
            }
        }
        Ok(())
    }

    pub fn retexture(&mut self, src: i16, dst: i16) -> R<()> {
        if self.faces_material.is_some() {
            for face in 0..self.num_faces {
                if at(&self.faces_material, i64::from(face))? == src {
                    put(&mut self.faces_material, i64::from(face), dst)?;
                }
            }
        }
        Ok(())
    }

    pub fn rotate_without_normals(&mut self, x: i32, y: i32, z: i32) -> R<()> {
        let count = i64::from(self.num_vertices);
        if z != 0 {
            let sin = at_slice(sine(), i64::from(z))?;
            let cos = at_slice(cosine(), i64::from(z))?;
            for v in 0..count {
                let vy = at(&self.vertices_y, v)?;
                let vx = at(&self.vertices_x, v)?;
                let rotated = sin.wrapping_mul(vy).wrapping_add(cos.wrapping_mul(vx)) >> 14;
                put(
                    &mut self.vertices_y,
                    v,
                    cos.wrapping_mul(vy).wrapping_sub(sin.wrapping_mul(vx)) >> 14,
                )?;
                put(&mut self.vertices_x, v, rotated)?;
            }
        }
        if x != 0 {
            let sin = at_slice(sine(), i64::from(x))?;
            let cos = at_slice(cosine(), i64::from(x))?;
            for v in 0..count {
                let vy = at(&self.vertices_y, v)?;
                let vz = at(&self.vertices_z, v)?;
                let rotated = cos.wrapping_mul(vy).wrapping_sub(sin.wrapping_mul(vz)) >> 14;
                put(
                    &mut self.vertices_z,
                    v,
                    sin.wrapping_mul(vy).wrapping_add(cos.wrapping_mul(vz)) >> 14,
                )?;
                put(&mut self.vertices_y, v, rotated)?;
            }
        }
        if y != 0 {
            let sin = at_slice(sine(), i64::from(y))?;
            let cos = at_slice(cosine(), i64::from(y))?;
            for v in 0..count {
                let vz = at(&self.vertices_z, v)?;
                let vx = at(&self.vertices_x, v)?;
                let rotated = sin.wrapping_mul(vz).wrapping_add(cos.wrapping_mul(vx)) >> 14;
                put(
                    &mut self.vertices_z,
                    v,
                    cos.wrapping_mul(vz).wrapping_sub(sin.wrapping_mul(vx)) >> 14,
                )?;
                put(&mut self.vertices_x, v, rotated)?;
            }
        }
        Ok(())
    }

    pub fn upscale(&mut self) -> R<()> {
        for v in 0..i64::from(self.num_vertices) {
            modify(&mut self.vertices_x, v, |a| a.wrapping_shl(2))?;
            modify(&mut self.vertices_y, v, |a| a.wrapping_shl(2))?;
            modify(&mut self.vertices_z, v, |a| a.wrapping_shl(2))?;
        }
        if self.num_textures > 0 && self.textures_scale_x.is_some() {
            for t in 0..len_of(&self.textures_scale_x)? as i64 {
                modify(&mut self.textures_scale_x, t, |a| a.wrapping_shl(2))?;
                modify(&mut self.textures_scale_y, t, |a| a.wrapping_shl(2))?;
                if at(&self.textures_mapping, t)? != 1 {
                    modify(&mut self.textures_scale_z, t, |a| a.wrapping_shl(2))?;
                }
            }
        }
        Ok(())
    }

    pub fn translate(&mut self, dx: i32, dy: i32, dz: i32) -> R<()> {
        for v in 0..i64::from(self.num_vertices) {
            modify(&mut self.vertices_x, v, |a| a.wrapping_add(dx))?;
            modify(&mut self.vertices_y, v, |a| a.wrapping_add(dy))?;
            modify(&mut self.vertices_z, v, |a| a.wrapping_add(dz))?;
        }
        Ok(())
    }
}
