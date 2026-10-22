use crate::loc_type::LocType;
use crate::locs::ScenePlacement;
use crate::mesh::Mesh;
use std::cell::RefCell;
use std::collections::HashMap;

pub const SCENERY_INTERACT: i32 = 10;
pub const GROUND_INTERACT: i32 = 11;
const ANGLE_STEP: f64 = 3.834_951_969_714_103E-4;
const QUARTER_TURN: usize = 4096;
const EIGHTH_TURN: usize = 2048;

pub(crate) fn sine_cosine(angle: usize) -> (i32, i32) {
    let radians = angle as f64 * ANGLE_STEP;
    (
        (16384.0 * radians.sin()) as i32,
        (16384.0 * radians.cos()) as i32,
    )
}

pub(crate) fn rotate(xs: &mut [i32], zs: &mut [i32], angle: usize) {
    let (sin, cos) = sine_cosine(angle);
    for (x, z) in xs.iter_mut().zip(zs.iter_mut()) {
        let next_x = sin.wrapping_mul(*z).wrapping_add(cos.wrapping_mul(*x)) >> 14;
        *z = cos.wrapping_mul(*z).wrapping_sub(sin.wrapping_mul(*x)) >> 14;
        *x = next_x;
    }
}

pub fn radius_2d(xs: &[i32], zs: &[i32]) -> i32 {
    let mut squared = 0i32;
    for (x, z) in xs.iter().zip(zs) {
        let value = x.wrapping_mul(*x).wrapping_add(z.wrapping_mul(*z));
        if value > squared {
            squared = value;
        }
    }
    ((f64::from(squared)).sqrt() + 0.99) as i32
}

pub fn transformed_size2d(loc: &LocType, kind: i32, rotation: i32, xs: &[i32], zs: &[i32]) -> i32 {
    let mut xs = xs.to_vec();
    let mut zs = zs.to_vec();
    if loc.inverted {
        for z in zs.iter_mut() {
            *z = z.wrapping_neg();
        }
    }
    let effective_rotation = if kind == GROUND_INTERACT {
        rotation + 4
    } else {
        rotation
    };
    match effective_rotation & 3 {
        1 => rotate(&mut xs, &mut zs, QUARTER_TURN),
        2 => rotate(&mut xs, &mut zs, QUARTER_TURN * 2),
        3 => rotate(&mut xs, &mut zs, QUARTER_TURN * 3),
        _ => {}
    }
    if loc.resize_x != 128 || loc.resize_z != 128 {
        for x in xs.iter_mut() {
            *x = x.wrapping_mul(loc.resize_x) >> 7;
        }
        for z in zs.iter_mut() {
            *z = z.wrapping_mul(loc.resize_z) >> 7;
        }
    }
    if loc.offset_x != 0 || loc.offset_z != 0 {
        for x in xs.iter_mut() {
            *x = x.wrapping_add(loc.offset_x);
        }
        for z in zs.iter_mut() {
            *z = z.wrapping_add(loc.offset_z);
        }
    }
    if effective_rotation > 3 {
        rotate(&mut xs, &mut zs, EIGHTH_TURN);
    }
    if loc.shadow_offset_x != 0 || loc.shadow_offset_z != 0 {
        for x in xs.iter_mut() {
            *x = x.wrapping_add(loc.shadow_offset_x);
        }
        for z in zs.iter_mut() {
            *z = z.wrapping_add(loc.shadow_offset_z);
        }
    }
    radius_2d(&xs, &zs)
}

pub fn loc_mesh<'a>(
    loc: &LocType,
    kind: i32,
    models: impl Fn(i32) -> Option<&'a [u8]>,
) -> Option<Mesh> {
    let shape = if kind == GROUND_INTERACT {
        SCENERY_INTERACT
    } else {
        kind
    };
    let shapes = loc.shapes.as_ref()?;
    let slot = shapes.iter().position(|value| i32::from(*value) == shape)?;
    let ids = loc.model_ids.as_ref()?.get(slot)?;
    if ids.is_empty() {
        return None;
    }
    let mut meshes = Vec::with_capacity(ids.len());
    for id in ids {
        let mut mesh = Mesh::from_bytes(models(*id)?).ok()?;
        if mesh.version < 13 {
            mesh.upscale().ok()?;
        }
        meshes.push(mesh);
    }
    if meshes.len() == 1 {
        return meshes.pop();
    }
    let refs: Vec<Option<&Mesh>> = meshes.iter().map(Some).collect();
    Mesh::combine(&refs).ok()
}

pub fn loc_size2d<'a>(
    loc: &LocType,
    kind: i32,
    rotation: i32,
    models: impl Fn(i32) -> Option<&'a [u8]>,
) -> Option<i32> {
    let mesh = loc_mesh(loc, kind, models)?;
    let xs = mesh.vertices_x.as_ref()?;
    let ys = mesh.vertices_y.as_ref()?;
    let zs = mesh.vertices_z.as_ref()?;
    let depth = usize::try_from(mesh.max_depth.min(mesh.num_vertices))
        .ok()?
        .min(xs.len())
        .min(ys.len())
        .min(zs.len());
    Some(transformed_size2d(
        loc,
        kind,
        rotation,
        &xs[..depth],
        &zs[..depth],
    ))
}

pub struct Size2dResolver<'a, F: Fn(i32) -> Option<&'a [u8]>> {
    models: F,
    cache: RefCell<HashMap<(i32, i32, i32), Option<i32>>>,
}

impl<'a, F: Fn(i32) -> Option<&'a [u8]>> Size2dResolver<'a, F> {
    pub fn new(models: F) -> Self {
        Self {
            models,
            cache: RefCell::new(HashMap::new()),
        }
    }

    pub fn size2d(&self, placement: &ScenePlacement, loc: &LocType) -> Option<i32> {
        let key = (loc.id, placement.kind, placement.rotation & 3);
        if let Some(found) = self.cache.borrow().get(&key) {
            return *found;
        }
        let value = loc_size2d(loc, placement.kind, placement.rotation, &self.models);
        self.cache.borrow_mut().insert(key, value);
        value
    }
}
