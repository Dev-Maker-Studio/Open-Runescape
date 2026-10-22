use crate::anim::{AnimBase, AnimFrame};
use crate::mesh::Mesh;
use crate::reader::{at, at_slice, modify, put, ModelError, R};
use crate::trig::{cosine, sine};

pub const WHOLE_MODEL: i32 = 65535;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Pose {
    pub num_vertices: i32,
    pub vertices_x: Vec<i32>,
    pub vertices_y: Vec<i32>,
    pub vertices_z: Vec<i32>,
    pub vertex_groups: Option<Vec<Vec<i32>>>,
    pub face_groups: Option<Vec<Vec<i32>>>,
    pub faces_alpha: Option<Vec<i8>>,
    pub faces_color: Option<Vec<i16>>,
    pub origin_x: i32,
    pub origin_y: i32,
    pub origin_z: i32,
}

fn rot(angle: i32) -> R<(i32, i32)> {
    Ok((
        at_slice(sine(), i64::from(angle))?,
        at_slice(cosine(), i64::from(angle))?,
    ))
}

fn mul_add(a: i32, b: i32, c: i32, d: i32, round: i32) -> i32 {
    a.wrapping_mul(b)
        .wrapping_add(c.wrapping_mul(d))
        .wrapping_add(round)
        >> 14
}

fn group(groups: &Option<Vec<Vec<i32>>>, label: i32) -> Option<&Vec<i32>> {
    let list = groups.as_ref()?;
    if label < list.len() as i32 {
        list.get(label as usize)
    } else {
        None
    }
}

fn labels_of(base: &AnimBase, index: i32) -> R<Vec<i32>> {
    if index < 0 {
        return Err(ModelError::Bounds);
    }
    base.labels
        .get(index as usize)
        .cloned()
        .ok_or(ModelError::Bounds)
}

fn divide(value: i32, by: i32) -> R<i32> {
    value.checked_div(by).ok_or(ModelError::Invalid)
}

impl Pose {
    pub fn from_mesh(mesh: &Mesh) -> R<Self> {
        let vertex_groups = if mesh.vertex_groups.is_some() {
            Some(mesh.create_vertex_groups(false)?)
        } else {
            None
        };
        let face_groups = if mesh.face_groups.is_some() {
            Some(mesh.create_face_groups()?)
        } else {
            None
        };
        let faces = mesh.num_faces.max(0) as usize;
        let take = |values: &Option<Vec<i32>>| values.clone().unwrap_or_default();
        Ok(Self {
            num_vertices: mesh.num_vertices,
            vertices_x: take(&mesh.vertices_x),
            vertices_y: take(&mesh.vertices_y),
            vertices_z: take(&mesh.vertices_z),
            vertex_groups,
            face_groups,
            faces_alpha: Some(mesh.faces_alpha.clone().unwrap_or_else(|| vec![0; faces])),
            faces_color: Some(mesh.faces_color.clone().unwrap_or_else(|| vec![0; faces])),
            origin_x: 0,
            origin_y: 0,
            origin_z: 0,
        })
    }

    pub fn start_animation(&mut self) -> bool {
        if self.vertex_groups.is_none() {
            return false;
        }
        for v in 0..self.num_vertices.max(0) as usize {
            for axis in [
                &mut self.vertices_x,
                &mut self.vertices_y,
                &mut self.vertices_z,
            ] {
                if let Some(slot) = axis.get_mut(v) {
                    *slot = slot.wrapping_shl(4);
                }
            }
        }
        self.origin_x = 0;
        self.origin_y = 0;
        self.origin_z = 0;
        true
    }

    pub fn end_animation(&mut self) {
        for v in 0..self.num_vertices.max(0) as usize {
            for axis in [
                &mut self.vertices_x,
                &mut self.vertices_y,
                &mut self.vertices_z,
            ] {
                if let Some(slot) = axis.get_mut(v) {
                    *slot = slot.wrapping_add(7) >> 4;
                }
            }
        }
    }

    fn vertex(&self, v: i32) -> R<(i32, i32, i32)> {
        let i = i64::from(v);
        Ok((
            at_slice(&self.vertices_x, i)?,
            at_slice(&self.vertices_y, i)?,
            at_slice(&self.vertices_z, i)?,
        ))
    }

    fn set_vertex(&mut self, v: i32, x: i32, y: i32, z: i32) -> R<()> {
        let i = i64::from(v);
        crate::reader::put_slice(&mut self.vertices_x, i, x)?;
        crate::reader::put_slice(&mut self.vertices_y, i, y)?;
        crate::reader::put_slice(&mut self.vertices_z, i, z)
    }

    pub fn animate_partial_transform(
        &mut self,
        kind: i32,
        labels: &[i32],
        x_in: i32,
        y_in: i32,
        z_in: i32,
        rotation: i32,
    ) -> R<()> {
        let mut x = x_in;
        let mut y = y_in;
        let mut z = z_in;
        match kind {
            0 => {
                x = x.wrapping_shl(4);
                y = y.wrapping_shl(4);
                z = z.wrapping_shl(4);
                let mut count = 0i32;
                self.origin_x = 0;
                self.origin_y = 0;
                self.origin_z = 0;
                for &label in labels {
                    if let Some(members) = group(&self.vertex_groups, label) {
                        for &v in members {
                            let (vx, vy, vz) = self.vertex(v)?;
                            self.origin_x = self.origin_x.wrapping_add(vx);
                            self.origin_y = self.origin_y.wrapping_add(vy);
                            self.origin_z = self.origin_z.wrapping_add(vz);
                            count += 1;
                        }
                    }
                }
                if count > 0 {
                    self.origin_x = x.wrapping_add(divide(self.origin_x, count)?);
                    self.origin_y = y.wrapping_add(divide(self.origin_y, count)?);
                    self.origin_z = z.wrapping_add(divide(self.origin_z, count)?);
                } else {
                    self.origin_x = x;
                    self.origin_y = y;
                    self.origin_z = z;
                }
            }
            1 => {
                x = x.wrapping_shl(4);
                y = y.wrapping_shl(4);
                z = z.wrapping_shl(4);
                for &label in labels {
                    if let Some(members) = group(&self.vertex_groups, label).cloned() {
                        for v in members {
                            let (vx, vy, vz) = self.vertex(v)?;
                            self.set_vertex(
                                v,
                                vx.wrapping_add(x),
                                vy.wrapping_add(y),
                                vz.wrapping_add(z),
                            )?;
                        }
                    }
                }
            }
            2 => {
                let (ox, oy, oz) = (self.origin_x, self.origin_y, self.origin_z);
                for &label in labels {
                    if let Some(members) = group(&self.vertex_groups, label).cloned() {
                        for v in members {
                            let (mut vx, mut vy, mut vz) = self.vertex(v)?;
                            vx = vx.wrapping_sub(ox);
                            vy = vy.wrapping_sub(oy);
                            vz = vz.wrapping_sub(oz);
                            let order: [u8; 3] = if rotation & 1 == 0 {
                                [2, 0, 1]
                            } else {
                                [0, 2, 1]
                            };
                            for step in order {
                                match step {
                                    2 => {
                                        if z != 0 {
                                            let (s, c) = rot(z)?;
                                            let tmp = mul_add(s, vy, c, vx, 16383);
                                            vy = mul_add(c, vy, -s, vx, 16383);
                                            vx = tmp;
                                        }
                                    }
                                    0 => {
                                        if x != 0 {
                                            let (s, c) = rot(x)?;
                                            let tmp = mul_add(c, vy, -s, vz, 16383);
                                            vz = mul_add(s, vy, c, vz, 16383);
                                            vy = tmp;
                                        }
                                    }
                                    _ => {
                                        if y != 0 {
                                            let (s, c) = rot(y)?;
                                            let tmp = mul_add(s, vz, c, vx, 16383);
                                            vz = mul_add(c, vz, -s, vx, 16383);
                                            vx = tmp;
                                        }
                                    }
                                }
                            }
                            self.set_vertex(
                                v,
                                vx.wrapping_add(ox),
                                vy.wrapping_add(oy),
                                vz.wrapping_add(oz),
                            )?;
                        }
                    }
                }
            }
            3 => {
                let (ox, oy, oz) = (self.origin_x, self.origin_y, self.origin_z);
                for &label in labels {
                    if let Some(members) = group(&self.vertex_groups, label).cloned() {
                        for v in members {
                            let (vx, vy, vz) = self.vertex(v)?;
                            let nx = x.wrapping_mul(vx.wrapping_sub(ox)) >> 7;
                            let ny = y.wrapping_mul(vy.wrapping_sub(oy)) >> 7;
                            let nz = z.wrapping_mul(vz.wrapping_sub(oz)) >> 7;
                            self.set_vertex(
                                v,
                                nx.wrapping_add(ox),
                                ny.wrapping_add(oy),
                                nz.wrapping_add(oz),
                            )?;
                        }
                    }
                }
            }
            5 if self.face_groups.is_some() => {
                for &label in labels {
                    if let Some(members) = group(&self.face_groups, label).cloned() {
                        for face in members {
                            let alpha = (at(&self.faces_alpha, i64::from(face))? as i32 & 0xff)
                                .wrapping_add(x.wrapping_mul(8))
                                .clamp(0, 255);
                            put(&mut self.faces_alpha, i64::from(face), alpha as i8)?;
                        }
                    }
                }
            }
            7 if self.face_groups.is_some() => {
                for &label in labels {
                    if let Some(members) = group(&self.face_groups, label).cloned() {
                        for face in members {
                            let hsl = at(&self.faces_color, i64::from(face))? as i32 & 0xffff;
                            let mut h = (hsl >> 10) & 0x3f;
                            let mut s = (hsl >> 7) & 0x7;
                            let mut l = hsl & 0x7f;
                            h = x.wrapping_add(h) & 0x3f;
                            s = (s + y / 4).clamp(0, 7);
                            l = l.wrapping_add(z).clamp(0, 127);
                            modify(&mut self.faces_color, i64::from(face), |_| {
                                ((h << 10) | (s << 7) | l) as i16
                            })?;
                        }
                    }
                }
            }
            _ => {}
        }
        Ok(())
    }

    #[allow(clippy::too_many_arguments)]
    fn animate_transform(
        &mut self,
        base_type: i32,
        labels: &[i32],
        transform_x: i32,
        transform_y: i32,
        transform_z: i32,
        adjustment: i32,
        submesh: i32,
    ) -> R<()> {
        if submesh != WHOLE_MODEL {
            return Err(ModelError::Invalid);
        }
        let mut x = transform_x;
        let mut z = transform_z;
        let plain = base_type == 0 || base_type == 1;
        match adjustment {
            1 => {
                if plain {
                    let y = x.wrapping_neg();
                    x = z;
                    z = y;
                } else if base_type == 3 {
                    std::mem::swap(&mut x, &mut z);
                } else if base_type == 2 {
                    let y = x;
                    x = z.wrapping_neg() & 0x3fff;
                    z = y & 0x3fff;
                }
            }
            2 => {
                if plain {
                    x = x.wrapping_neg();
                    z = z.wrapping_neg();
                } else if base_type == 2 {
                    x = x.wrapping_neg() & 0x3fff;
                    z = z.wrapping_neg() & 0x3fff;
                }
            }
            3 => {
                if plain {
                    let y = x;
                    x = z.wrapping_neg();
                    z = y;
                } else if base_type == 3 {
                    std::mem::swap(&mut x, &mut z);
                } else if base_type == 2 {
                    let y = x;
                    x = z & 0x3fff;
                    z = y.wrapping_neg() & 0x3fff;
                }
            }
            _ => {}
        }
        self.animate_partial_transform(base_type, labels, x, transform_y, z, adjustment)
    }

    fn label_transform(&mut self, base: &AnimBase, label: i32, rotation: i32) -> R<()> {
        let labels = labels_of(base, label)?;
        let mask = at_slice(&base.submeshes, i64::from(label))?;
        self.animate_transform(0, &labels, 0, 0, 0, rotation, WHOLE_MODEL & mask)
    }

    #[allow(clippy::too_many_arguments)]
    pub fn interpolate_frames(
        &mut self,
        base: &AnimBase,
        current: &AnimFrame,
        next: Option<&AnimFrame>,
        cycle: i32,
        length: i32,
        rotation: i32,
        animate_labels: Option<&[bool]>,
        condition: bool,
    ) -> R<()> {
        match next {
            Some(next) if cycle != 0 => self.tweened(
                base,
                current,
                next,
                cycle,
                length,
                rotation,
                animate_labels,
                condition,
            ),
            _ => self.plain(base, current, rotation, animate_labels, condition),
        }
    }

    fn plain(
        &mut self,
        base: &AnimBase,
        current: &AnimFrame,
        rotation: i32,
        animate_labels: Option<&[bool]>,
        condition: bool,
    ) -> R<()> {
        for t in 0..current.num_transformations {
            let index = i32::from(at(&current.base_indices, i64::from(t))?);
            let kind = at_slice(&base.transformation_type, i64::from(index))?;
            if let Some(mask) = animate_labels {
                if at_slice(mask, i64::from(index))? != condition && kind != 0 {
                    continue;
                }
            }
            let label = i32::from(at(&current.labels, i64::from(t))?);
            if label != -1 {
                self.label_transform(base, label, rotation)?;
            }
            let labels = labels_of(base, index)?;
            let mask = at_slice(&base.submeshes, i64::from(index))?;
            self.animate_transform(
                kind,
                &labels,
                i32::from(at(&current.transformation_x, i64::from(t))?),
                i32::from(at(&current.transformation_y, i64::from(t))?),
                i32::from(at(&current.transformation_z, i64::from(t))?),
                rotation,
                WHOLE_MODEL & mask,
            )?;
        }
        Ok(())
    }

    #[allow(clippy::too_many_arguments)]
    fn tweened(
        &mut self,
        base: &AnimBase,
        current: &AnimFrame,
        next: &AnimFrame,
        cycle: i32,
        length: i32,
        rotation: i32,
        animate_labels: Option<&[bool]>,
        condition: bool,
    ) -> R<()> {
        let mut ci = 0i32;
        let mut ni = 0i32;
        for index in 0..base.num_transformation {
            let has_current = ci < current.num_transformations
                && index == i32::from(at(&current.base_indices, i64::from(ci))?);
            let has_next = ni < next.num_transformations
                && index == i32::from(at(&next.base_indices, i64::from(ni))?);
            if !has_current && !has_next {
                continue;
            }
            let kind = at_slice(&base.transformation_type, i64::from(index))?;
            if let Some(mask) = animate_labels {
                if at_slice(mask, i64::from(index))? != condition && kind != 0 {
                    if has_current {
                        ci += 1;
                    }
                    if has_next {
                        ni += 1;
                    }
                    continue;
                }
            }
            let default: i32 = if kind == 3 || kind == 10 { 128 } else { 0 };
            let (cx, cy, cz, cl, cf) = if has_current {
                let t = i64::from(ci);
                let out = (
                    i32::from(at(&current.transformation_x, t)?),
                    i32::from(at(&current.transformation_y, t)?),
                    i32::from(at(&current.transformation_z, t)?),
                    i32::from(at(&current.labels, t)?),
                    i32::from(at(&current.tweening_properties, t)?),
                );
                ci += 1;
                out
            } else {
                (default, default, default, -1, 0)
            };
            let (nx, ny, nz, nl, nf) = if has_next {
                let t = i64::from(ni);
                let out = (
                    i32::from(at(&next.transformation_x, t)?),
                    i32::from(at(&next.transformation_y, t)?),
                    i32::from(at(&next.transformation_z, t)?),
                    i32::from(at(&next.labels, t)?),
                    i32::from(at(&next.tweening_properties, t)?),
                );
                ni += 1;
                out
            } else {
                (default, default, default, -1, 0)
            };
            let lerp = |from: i32, to: i32| -> R<i32> {
                Ok(from.wrapping_add(divide(to.wrapping_sub(from).wrapping_mul(cycle), length)?))
            };
            let wrap = |from: i32, to: i32, mask: i32, half: i32| -> R<i32> {
                let mut delta = to.wrapping_sub(from) & mask;
                if delta >= half {
                    delta -= half * 2;
                }
                Ok(from.wrapping_add(divide(delta.wrapping_mul(cycle), length)?) & mask)
            };
            let (ix, iy, iz) = if cf & 0x2 == 0 && nf & 0x1 == 0 {
                match kind {
                    2 => (
                        wrap(cx, nx, 0x3fff, 8192)?,
                        wrap(cy, ny, 0x3fff, 8192)?,
                        wrap(cz, nz, 0x3fff, 8192)?,
                    ),
                    9 => (wrap(cx, nx, 0x3fff, 8192)?, 0, 0),
                    7 => (wrap(cx, nx, 0x3f, 32)?, lerp(cy, ny)?, lerp(cz, nz)?),
                    _ => (lerp(cx, nx)?, lerp(cy, ny)?, lerp(cz, nz)?),
                }
            } else {
                (cx, cy, cz)
            };
            if cl != -1 {
                self.label_transform(base, cl, rotation)?;
            } else if nl != -1 {
                self.label_transform(base, nl, rotation)?;
            }
            let labels = labels_of(base, index)?;
            let mask = at_slice(&base.submeshes, i64::from(index))?;
            self.animate_transform(kind, &labels, ix, iy, iz, rotation, WHOLE_MODEL & mask)?;
        }
        Ok(())
    }

    #[allow(clippy::too_many_arguments)]
    pub fn animate_model_frame(
        &mut self,
        base: &AnimBase,
        first: &AnimFrame,
        next: Option<&AnimFrame>,
        cycle: i32,
        length: i32,
        rotation: i32,
    ) -> R<()> {
        if !self.start_animation() {
            return Ok(());
        }
        self.interpolate_frames(base, first, next, cycle, length, rotation, None, false)?;
        self.end_animation();
        Ok(())
    }

    #[allow(clippy::too_many_arguments)]
    pub fn double_animation(
        &mut self,
        base: &AnimBase,
        animating: &AnimFrame,
        animating_next: Option<&AnimFrame>,
        animating_cycle: i32,
        animating_duration: i32,
        idle: Option<(&AnimBase, &AnimFrame, Option<&AnimFrame>, i32, i32)>,
        animate_labels: Option<&[bool]>,
    ) -> R<()> {
        let (Some(mask), Some((idle_base, idle_frame, idle_next, idle_cycle, idle_duration))) =
            (animate_labels, idle)
        else {
            return self.animate_model_frame(
                base,
                animating,
                animating_next,
                animating_cycle,
                animating_duration,
                0,
            );
        };
        if !self.start_animation() {
            return Ok(());
        }
        self.interpolate_frames(
            base,
            animating,
            animating_next,
            animating_cycle,
            animating_duration,
            0,
            Some(mask),
            false,
        )?;
        self.animate_partial_transform(0, &[], 0, 0, 0, 0)?;
        self.interpolate_frames(
            idle_base,
            idle_frame,
            idle_next,
            idle_cycle,
            idle_duration,
            0,
            Some(mask),
            true,
        )?;
        self.end_animation();
        Ok(())
    }
}
