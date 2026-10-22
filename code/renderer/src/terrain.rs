use crate::{
    camera::{self, CameraPose, Fog, Viewport},
    frame::{Draw, Frame, Mesh, NO_TEXTURE, Texture, TextureFormat, Uniform},
    math::Mat4,
    pass::Pass,
};

pub const VERTEX_STRIDE: usize = 40;
const MAX_RECORD: usize = 32 * 1024 * 1024;
const MESH_HEADER_BYTES: usize = 16;

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct TerrainVertex {
    pub position: [f32; 3],
    pub rgba: [u8; 4],
    pub texcoord: [f32; 3],
    pub normal: [f32; 3],
}

pub const fn native_state() -> u32 {
    1 | 2 | (1 << 2) | (3 << 7)
}

fn checked_vertex_bytes(vertex_count: usize, index_count: usize) -> Result<usize, String> {
    u32::try_from(vertex_count).map_err(|_| "terrain vertex count exceeds u32".to_string())?;
    u32::try_from(index_count).map_err(|_| "terrain index count exceeds u32".to_string())?;
    let vertex_bytes = vertex_count
        .checked_mul(VERTEX_STRIDE)
        .ok_or("terrain vertex byte size overflow")?;
    let record_bytes = index_count
        .checked_mul(4)
        .and_then(|bytes| bytes.checked_add(vertex_bytes))
        .and_then(|bytes| bytes.checked_add(MESH_HEADER_BYTES))
        .ok_or("terrain mesh byte size overflow")?;
    if record_bytes > MAX_RECORD {
        return Err("terrain mesh exceeds 32 MiB record budget".to_string());
    }
    Ok(vertex_bytes)
}

pub fn encode_mesh(id: u32, vertices: &[TerrainVertex], indices: &[u32]) -> Result<Mesh, String> {
    if !indices.len().is_multiple_of(3) {
        return Err("terrain triangle index count must be divisible by three".to_string());
    }
    let vertex_bytes = checked_vertex_bytes(vertices.len(), indices.len())?;
    for (at, &index) in indices.iter().enumerate() {
        if index as usize >= vertices.len() {
            return Err(format!(
                "terrain index {index} at {at} exceeds vertex count {}",
                vertices.len()
            ));
        }
    }
    let mut encoded = Vec::new();
    encoded
        .try_reserve_exact(vertex_bytes)
        .map_err(|e| format!("terrain vertex allocation failed: {e}"))?;
    let mut copied_indices = Vec::new();
    copied_indices
        .try_reserve_exact(indices.len())
        .map_err(|e| format!("terrain index allocation failed: {e}"))?;
    for vertex in vertices {
        for value in vertex.position {
            encoded.extend_from_slice(&value.to_le_bytes());
        }
        encoded.extend_from_slice(&vertex.rgba);
        for value in vertex.texcoord.into_iter().chain(vertex.normal) {
            encoded.extend_from_slice(&value.to_le_bytes());
        }
    }
    copied_indices.extend_from_slice(indices);
    Ok(Mesh {
        id,
        pass: Pass::Terrain,
        vertices: encoded,
        indices: copied_indices,
    })
}

pub const UNIFORM_BYTES: usize = 5552;
pub const TILE_UNITS: f32 = 512.;
pub const UNTEXTURED_MATERIAL: i32 = -1;
pub const EYE_HEIGHT_TILES: i32 = 14;
pub const EYE_BACK_TILES: i32 = 15;
pub const DEFAULT_SCENE_SIZE_X: i32 = 104;
pub const DEFAULT_FOG_DEPTH: i32 = 0;
pub const EYE_YAW: i32 = 1960;
pub const MAX_LAYERS: usize = 256;
const MESH_BASE: u32 = 1;
const UNIFORM_BASE: u32 = 1000;
const TEXTURE_BASE: u32 = 2000;

pub mod offset {
    pub const VIEW_PROJECTION: usize = 0;
    pub const CLIP_PLANE: usize = 64;
    pub const TEXTURE_SCALE: usize = 80;
    pub const TEXTURE_SCROLL: usize = 88;
    pub const HAS_NORMALS: usize = 96;
    pub const FOG_START: usize = 164;
    pub const FOG_END: usize = 168;
    pub const FOG_ENABLED: usize = 172;
    pub const WATER_MODE: usize = 4288;
    pub const GROUND_SHADOW_SCALE: usize = 5500;
    pub const HAS_TEXTURE: usize = 5504;
    pub const FOG_COLOR: usize = 5520;
    pub const COMBINE_MODE: usize = 5536;
    pub const ALPHA_FROM_VERTEX: usize = 5540;
}

#[derive(Clone, Debug)]
pub struct LayerTexture {
    pub width: u32,
    pub height: u32,
    pub rgba: Vec<u8>,
}

#[derive(Clone, Debug)]
pub struct Layer {
    pub material: i32,
    pub texture_scale: i32,
    pub vertices: Vec<u8>,
    pub indices: Vec<u32>,
    pub texture: Option<LayerTexture>,
    pub overlay: bool,
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Camera {
    pub pose: CameraPose,
    pub scaled_fov: i32,
    pub scene_size_x: i32,
}

fn put_f32(block: &mut [u8], at: usize, value: f32) {
    block[at..at + 4].copy_from_slice(&value.to_le_bytes());
}

fn put_u32(block: &mut [u8], at: usize, value: u32) {
    block[at..at + 4].copy_from_slice(&value.to_le_bytes());
}

pub fn tile_center(tile: [i32; 2]) -> [f32; 2] {
    [
        tile[0] as f32 * TILE_UNITS + TILE_UNITS * 0.5,
        tile[1] as f32 * TILE_UNITS + TILE_UNITS * 0.5,
    ]
}

pub fn vertex_at(bytes: &[u8], index: usize) -> Option<([f32; 3], [u8; 4])> {
    let record = bytes.get(index * VERTEX_STRIDE..(index + 1) * VERTEX_STRIDE)?;
    let f = |at: usize| f32::from_le_bytes(record[at..at + 4].try_into().unwrap());
    Some(([f(0), f(4), f(8)], record[12..16].try_into().unwrap()))
}

pub fn ground_height(layers: &[Layer], x: f32, z: f32) -> Option<f32> {
    let mut best: Option<(f32, f32)> = None;
    for layer in layers {
        for index in 0..layer.vertices.len() / VERTEX_STRIDE {
            let (p, _) = vertex_at(&layer.vertices, index)?;
            let d = (p[0] - x).powi(2) + (p[2] - z).powi(2);
            if best.is_none_or(|(b, _)| d < b) {
                best = Some((d, p[1]));
            }
        }
    }
    best.map(|(_, y)| y)
}

pub const GROUND_REACH_TILES: f32 = 2.;

pub fn ground_at(layers: &[Layer], tile: [i32; 2]) -> Result<f32, String> {
    let centre = tile_center(tile);
    let mut best: Option<(f32, f32)> = None;
    for layer in layers {
        for index in 0..layer.vertices.len() / VERTEX_STRIDE {
            let (p, _) = vertex_at(&layer.vertices, index).ok_or("terrain vertex out of range")?;
            let d = (p[0] - centre[0]).powi(2) + (p[2] - centre[1]).powi(2);
            if best.is_none_or(|(b, _)| d < b) {
                best = Some((d, p[1]));
            }
        }
    }
    let (distance, y) = best.ok_or("terrain has no vertices")?;
    let reach = GROUND_REACH_TILES * TILE_UNITS;
    if distance > reach * reach {
        return Err(format!(
            "camera tile {},{} is outside the terrain (nearest vertex {:.0} units away)",
            tile[0],
            tile[1],
            distance.sqrt()
        ));
    }
    Ok(y)
}

pub struct SceneBatch {
    pub material: i32,
    pub vertices: Vec<u8>,
    pub indices: Vec<u32>,
}

pub fn scene_batches(
    positions: &[i32],
    hsl: &[i16],
    alphas: &[u8],
    materials: &[i16],
    rgb: &[u32],
) -> Result<Vec<SceneBatch>, String> {
    let triangles = hsl.len();
    if positions.len() != triangles * 9 || alphas.len() != triangles || materials.len() != triangles
    {
        return Err(format!(
            "scene triangle arrays disagree: {} positions, {} colors, {} alphas, {} materials",
            positions.len(),
            triangles,
            alphas.len(),
            materials.len()
        ));
    }
    let mut batches: std::collections::BTreeMap<i16, SceneBatch> = Default::default();
    for triangle in 0..triangles {
        let material = materials[triangle];
        let batch = batches.entry(material).or_insert_with(|| SceneBatch {
            material: i32::from(material),
            vertices: Vec::new(),
            indices: Vec::new(),
        });
        let color = rgb
            .get(usize::from(hsl[triangle] as u16))
            .copied()
            .unwrap_or(0);
        let rgba = [
            (color >> 16) as u8,
            (color >> 8) as u8,
            color as u8,
            255 - alphas[triangle],
        ];
        for corner in 0..3 {
            let at = triangle * 9 + corner * 3;
            let x = positions[at] as f32;
            let y = positions[at + 1] as f32;
            let z = positions[at + 2] as f32;
            let first = (batch.vertices.len() / VERTEX_STRIDE) as u32;
            for value in [x, y, z] {
                batch.vertices.extend_from_slice(&value.to_le_bytes());
            }
            batch.vertices.extend_from_slice(&rgba);
            for value in [x, z, 0., 0., -1., 0.] {
                batch.vertices.extend_from_slice(&value.to_le_bytes());
            }
            batch.indices.push(first);
        }
    }
    Ok(batches.into_values().collect())
}

pub fn scaled_fov_for(height: u32) -> i32 {
    (height as f32 / (4. * (0.45f32).tan())).round() as i32
}

pub fn camera(tile: [i32; 2], height: f32, viewport_height: u32) -> Camera {
    let [x, z] = tile_center(tile);
    let pose = CameraPose {
        x: x as i32,
        y: z as i32 - EYE_BACK_TILES * TILE_UNITS as i32,
        z: height as i32 - EYE_HEIGHT_TILES * TILE_UNITS as i32,
        yaw: EYE_YAW,
        roll: 0,
        pitch: 0,
    };
    Camera {
        pose,
        scaled_fov: scaled_fov_for(viewport_height),
        scene_size_x: DEFAULT_SCENE_SIZE_X,
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Environment {
    pub fog_enabled: bool,
    pub fog_color: i32,
    pub fog_depth: i32,
    pub fog_offset: i32,
    pub sky_argb: i32,
}

impl Default for Environment {
    fn default() -> Self {
        Self {
            fog_enabled: true,
            fog_color: camera::DEFAULT_FOG_COLOR,
            fog_depth: DEFAULT_FOG_DEPTH,
            fog_offset: 0,
            sky_argb: camera::DEFAULT_FOG_COLOR,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct View {
    pub camera: Camera,
    pub environment: Environment,
    pub bloom: Option<crate::frame::BloomParams>,
}

pub const MAX_FOG_DEPTH: i32 = 0xffff;
pub const MAX_FOG_OFFSET: i32 = 1 << 20;
pub const MAX_SCENE_SIZE_X: i32 = 256;
pub const MAX_CAMERA_COORDINATE: i32 = 1 << 24;

impl View {
    pub fn default_for(tile: [i32; 2], ground: f32, viewport_height: u32) -> Self {
        Self {
            camera: camera(tile, ground, viewport_height),
            environment: Environment::default(),
            bloom: None,
        }
    }

    pub fn validate(&self) -> Result<(), String> {
        let c = &self.camera;
        let e = &self.environment;
        if c.scaled_fov <= 0 {
            return Err(format!(
                "camera needs a positive field of view, got {}",
                c.scaled_fov
            ));
        }
        if !(1..=MAX_SCENE_SIZE_X).contains(&c.scene_size_x) {
            return Err(format!("scene size {} is out of range", c.scene_size_x));
        }
        for value in [c.pose.x, c.pose.y, c.pose.z] {
            if value.unsigned_abs() > MAX_CAMERA_COORDINATE as u32 {
                return Err(format!("camera coordinate {value} is out of range"));
            }
        }
        if !(0..=MAX_FOG_DEPTH).contains(&e.fog_depth) {
            return Err(format!("fog depth {} is out of range", e.fog_depth));
        }
        if !(0..=MAX_FOG_OFFSET).contains(&e.fog_offset) {
            return Err(format!("fog offset {} is out of range", e.fog_offset));
        }
        if self.bloom.is_some_and(|b| !b.valid()) {
            return Err("bloom needs finite parameters and a positive bloom".to_string());
        }
        Ok(())
    }
}

pub fn follow(
    tile: [i32; 2],
    ground: f32,
    yaw: i32,
    roll: i32,
    pitch: i32,
    viewport_height: u32,
) -> Camera {
    let mut cam = camera(tile, ground, viewport_height);
    cam.pose.yaw = yaw;
    cam.pose.roll = roll;
    cam.pose.pitch = pitch;
    cam
}

pub struct OrbitInput {
    pub tile: [i32; 2],
    pub ground: f32,
    pub elapsed_ms: i64,
    pub cycles: u32,
    pub yaw_direction: i32,
    pub pitch_direction: i32,
    pub jitter_bits: i32,
}

pub fn orbit_step(
    orbit: &mut crate::orbit::Orbit,
    input: &OrbitInput,
    ground: &dyn crate::orbit::Ground,
    plane: i32,
    viewport_height: u32,
) -> Camera {
    let [cx, cz] = tile_center(input.tile);
    let (player_x, player_y) = (cx as i32, cz as i32);
    for _ in 0..input.cycles {
        orbit.apply_yaw(input.yaw_direction);
        orbit.apply_pitch(input.pitch_direction);
        orbit.settle_velocities();
        orbit.jitter.tick(input.jitter_bits);
    }
    orbit.follow(input.elapsed_ms, player_x, player_y, ground, plane);
    let eye = orbit.eye(player_x, player_y, ground, plane, viewport_height as i32);
    Camera {
        pose: eye.pose(),
        scaled_fov: scaled_fov_for(viewport_height),
        scene_size_x: DEFAULT_SCENE_SIZE_X,
    }
}

#[allow(clippy::too_many_arguments)]
pub fn scripted_step(
    orbit: &mut crate::orbit::Orbit,
    scripted: &mut crate::scripted_camera::ScriptedCamera,
    input: &OrbitInput,
    base: (i32, i32),
    ground: &dyn crate::orbit::Ground,
    plane: i32,
    viewport_height: u32,
    random: &mut dyn FnMut() -> f64,
) -> Camera {
    let [cx, cz] = tile_center(input.tile);
    let (player_x, player_y) = (cx as i32, cz as i32);
    for _ in 0..input.cycles {
        orbit.apply_yaw(input.yaw_direction);
        orbit.apply_pitch(input.pitch_direction);
        orbit.settle_velocities();
        orbit.jitter.tick(input.jitter_bits);
        scripted.tick_cycle(base.0, base.1, ground, plane);
        scripted.advance_cycle();
    }
    let at = crate::scripted_camera::Placement {
        player_x,
        player_y,
        plane,
        area_height: viewport_height as i32,
    };
    let pose = scripted.frame(orbit, input.elapsed_ms, ground, &at, random);
    Camera {
        pose,
        scaled_fov: scaled_fov_for(viewport_height),
        scene_size_x: DEFAULT_SCENE_SIZE_X,
    }
}

pub fn flat_ground(level: f32) -> crate::orbit::FlatGround {
    crate::orbit::FlatGround {
        size_x: DEFAULT_SCENE_SIZE_X,
        size_y: DEFAULT_SCENE_SIZE_X,
        level: level as i32,
    }
}

pub fn viewport(camera: &Camera, width: u32, height: u32) -> Viewport {
    let (start, end) =
        camera::view_range(camera.scene_size_x, true, camera::FOG_DISTANCE_MULTIPLIER);
    Viewport {
        width: width as i32,
        height: height as i32,
        scaled_fov: camera.scaled_fov,
        view_range_start: start,
        view_range_end: end,
    }
}

pub fn native_view_projection(camera: &Camera, width: u32, height: u32) -> Mat4 {
    let vp = viewport(camera, width, height);
    camera::view_projection(&camera::view_matrix(&camera.pose), &camera::projection(&vp))
}

pub fn view_projection(camera: &Camera, width: u32, height: u32) -> Mat4 {
    let vp = viewport(camera, width, height);
    let native =
        camera::view_projection(&camera::view_matrix(&camera.pose), &camera::projection(&vp));
    camera::wgpu_view_projection(&native)
}

pub fn view_fog(view: &View, width: u32, height: u32) -> Fog {
    let proj = camera::projection(&viewport(&view.camera, width, height));
    let e = &view.environment;
    camera::fog(
        &proj,
        e.fog_enabled,
        e.fog_color,
        camera::environment_density(e.fog_depth, true),
        e.fog_offset,
    )
}

pub fn frame_fog(camera: &Camera, width: u32, height: u32) -> Fog {
    view_fog(
        &View {
            camera: *camera,
            environment: Environment::default(),
            bloom: None,
        },
        width,
        height,
    )
}

pub fn view_sky(view: &View) -> [f32; 4] {
    camera::clear_color_opaque(view.environment.sky_argb)
}

pub fn sky() -> [f32; 4] {
    camera::clear_color_opaque(camera::DEFAULT_FOG_COLOR)
}

pub fn uniform_block(
    view_projection: &Mat4,
    fog: &Fog,
    texture_scale: i32,
    textured: bool,
) -> Result<Vec<u8>, String> {
    if textured && texture_scale <= 0 {
        return Err(format!(
            "textured terrain layer needs a positive texture scale, got {texture_scale}"
        ));
    }
    let mut block = vec![0u8; UNIFORM_BYTES];
    for (i, value) in view_projection.iter().enumerate() {
        put_f32(&mut block, offset::VIEW_PROJECTION + i * 4, *value);
    }
    put_f32(
        &mut block,
        offset::TEXTURE_SCALE,
        if textured {
            1. / texture_scale as f32
        } else {
            1.
        },
    );
    put_f32(&mut block, offset::FOG_START, fog.start);
    put_f32(&mut block, offset::FOG_END, fog.end);
    put_u32(&mut block, offset::FOG_ENABLED, fog.enabled as u32);
    put_u32(&mut block, offset::HAS_TEXTURE, textured as u32);
    for (i, value) in fog.color.iter().enumerate() {
        put_f32(&mut block, offset::FOG_COLOR + i * 4, *value);
    }
    put_u32(&mut block, offset::COMBINE_MODE, 0);
    put_u32(&mut block, offset::ALPHA_FROM_VERTEX, 1);
    Ok(block)
}

fn layer_mesh(id: u32, layer: &Layer) -> Result<Mesh, String> {
    if !layer.vertices.len().is_multiple_of(VERTEX_STRIDE) {
        return Err(format!(
            "terrain vertex bytes {} are not a multiple of {VERTEX_STRIDE}",
            layer.vertices.len()
        ));
    }
    let count = layer.vertices.len() / VERTEX_STRIDE;
    if !layer.indices.len().is_multiple_of(3) {
        return Err("terrain triangle index count must be divisible by three".to_string());
    }
    if let Some((at, &index)) = layer
        .indices
        .iter()
        .enumerate()
        .find(|(_, i)| **i as usize >= count)
    {
        return Err(format!(
            "terrain index {index} at {at} exceeds vertex count {count}"
        ));
    }
    checked_vertex_bytes(count, layer.indices.len())?;
    Ok(Mesh {
        id,
        pass: Pass::Terrain,
        vertices: layer.vertices.clone(),
        indices: layer.indices.clone(),
    })
}

fn check_inputs(layers: &[Layer], width: u32, height: u32) -> Result<(), String> {
    if layers.is_empty() || layers.len() > MAX_LAYERS {
        return Err(format!(
            "terrain needs 1 to {MAX_LAYERS} layers, got {}",
            layers.len()
        ));
    }
    if width == 0 || height == 0 {
        return Err("terrain frame needs a non-empty viewport".to_string());
    }
    Ok(())
}

pub fn build_frame(
    layers: &[Layer],
    tile: [i32; 2],
    width: u32,
    height: u32,
) -> Result<Frame, String> {
    check_inputs(layers, width, height)?;
    let ground = ground_at(layers, tile)?;
    build_frame_with(
        layers,
        &View::default_for(tile, ground, height),
        width,
        height,
    )
}

pub fn build_frame_with(
    layers: &[Layer],
    view: &View,
    width: u32,
    height: u32,
) -> Result<Frame, String> {
    check_inputs(layers, width, height)?;
    view.validate()?;
    let view_projection = view_projection(&view.camera, width, height);
    let fog = view_fog(view, width, height);
    let mut frame = Frame {
        width,
        height,
        clear: view_sky(view),
        bloom: view.bloom,
        ..Default::default()
    };
    for (at, layer) in layers.iter().enumerate() {
        let at = at as u32;
        let textured = layer.texture.is_some() && layer.material != UNTEXTURED_MATERIAL;
        frame.meshes.push(layer_mesh(MESH_BASE + at, layer)?);
        frame.uniforms.push(Uniform {
            id: UNIFORM_BASE + at,
            bytes: uniform_block(&view_projection, &fog, layer.texture_scale, textured)?,
        });
        let mut texture = NO_TEXTURE;
        if let (true, Some(t)) = (textured, layer.texture.as_ref()) {
            let expected = TextureFormat::Rgba8
                .byte_size(t.width, t.height, 1)
                .ok_or("terrain texture size out of range")?;
            if t.rgba.len() != expected {
                return Err(format!(
                    "terrain texture has {} bytes, expected {expected}",
                    t.rgba.len()
                ));
            }
            texture = TEXTURE_BASE + at;
            frame.textures.push(Texture {
                id: texture,
                width: t.width,
                height: t.height,
                layers: 1,
                rgba: t.rgba.clone(),
                format: TextureFormat::Rgba8,
                sampler: 0,
            });
        }
        let mut draw = Draw::new(
            Pass::Terrain,
            MESH_BASE + at,
            UNIFORM_BASE + at,
            vec![texture],
        );
        draw.state = if layer.overlay {
            native_state() & !3
        } else {
            native_state()
        };
        frame.draws.push(draw);
    }
    Ok(frame)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::frame::append_record;
    use crate::math;

    fn vertex() -> TerrainVertex {
        TerrainVertex {
            position: [1., -2., 0.5],
            rgba: [1, 35, 69, 103],
            texcoord: [4., 8., -16.],
            normal: [-1., 0., 1.],
        }
    }

    #[test]
    fn overlay_layers_draw_without_depth_test_or_write() {
        let mut layer = layer(UNTEXTURED_MATERIAL, -1, false);
        let view = View {
            camera: follow([3200, 3200], 0., 0, 0, 0, 480),
            environment: Environment::default(),
            bloom: Default::default(),
        };
        let plain = build_frame_with(&[layer.clone()], &view, 64, 48).unwrap();
        layer.overlay = true;
        let over = build_frame_with(&[layer], &view, 64, 48).unwrap();
        assert_eq!(plain.draws[0].state & 3, 3);
        assert_eq!(over.draws[0].state & 3, 0);
        assert_eq!(over.draws[0].state >> 2, plain.draws[0].state >> 2);
    }

    #[test]
    fn canonical_little_endian_layout() {
        let mesh = encode_mesh(u32::MAX, &[vertex()], &[0, 0, 0]).unwrap();
        assert_eq!(mesh.id, u32::MAX);
        assert_eq!(mesh.pass, Pass::Terrain);
        assert_eq!(mesh.indices, [0, 0, 0]);
        assert_eq!(VERTEX_STRIDE as u64, Pass::Terrain.stride());
        assert_eq!(
            mesh.vertices,
            [
                0x00, 0x00, 0x80, 0x3f, 0x00, 0x00, 0x00, 0xc0, 0x00, 0x00, 0x00, 0x3f, 0x01, 0x23,
                0x45, 0x67, 0x00, 0x00, 0x80, 0x40, 0x00, 0x00, 0x00, 0x41, 0x00, 0x00, 0x80, 0xc1,
                0x00, 0x00, 0x80, 0xbf, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x3f,
            ]
        );
    }

    #[test]
    fn preserves_float_bits_and_vertex_and_triangle_order() {
        let bits = [
            0x80000000, 0x00000000, 0x7fc12345, 0xffc54321, 0x7f800001, 0x7f800000, 0xff800000,
            0x00000001, 0x80000001,
        ];
        let values = bits.map(f32::from_bits);
        let first = TerrainVertex {
            position: values[..3].try_into().unwrap(),
            rgba: [0, 127, 128, 255],
            texcoord: values[3..6].try_into().unwrap(),
            normal: values[6..].try_into().unwrap(),
        };
        let vertices = [first, vertex()];
        let indices = [1, 0, 1, 0, 1, 0];
        let mesh = encode_mesh(7, &vertices, &indices).unwrap();
        assert_eq!(mesh.vertices.len(), 2 * VERTEX_STRIDE);
        for (offset, expected) in [0, 4, 8, 16, 20, 24, 28, 32, 36].into_iter().zip(bits) {
            assert_eq!(&mesh.vertices[offset..offset + 4], &expected.to_le_bytes());
        }
        assert_eq!(&mesh.vertices[12..16], &first.rgba);
        let second = encode_mesh(8, &[vertices[1]], &[]).unwrap();
        assert_eq!(&mesh.vertices[VERTEX_STRIDE..], &second.vertices);
        assert_eq!(mesh.indices, indices);
    }

    #[test]
    fn rejects_incomplete_triangles() {
        for count in [1, 2, 4, 5] {
            let error = encode_mesh(0, &[vertex()], &vec![0; count]).err().unwrap();
            assert!(error.contains("divisible by three"));
        }
    }

    #[test]
    fn checks_every_index_against_vertex_count() {
        let vertices = [vertex(); 3];
        for at in 0..6 {
            for invalid in [3, u32::MAX] {
                let mut indices = [0, 2, 1, 2, 0, 1];
                indices[at] = invalid;
                let error = encode_mesh(0, &vertices, &indices).err().unwrap();
                assert_eq!(
                    error,
                    format!("terrain index {invalid} at {at} exceeds vertex count 3")
                );
            }
        }
        assert!(encode_mesh(0, &vertices, &[0, 2, 1]).is_ok());
    }

    #[test]
    fn empty_meshes_are_valid_but_cannot_have_indices() {
        let mesh = encode_mesh(12, &[], &[]).unwrap();
        assert_eq!(mesh.id, 12);
        assert_eq!(mesh.pass, Pass::Terrain);
        assert!(mesh.vertices.is_empty());
        assert!(mesh.indices.is_empty());
        assert!(encode_mesh(0, &[vertex()], &[]).is_ok());
        assert!(encode_mesh(0, &[], &[0, 0, 0]).is_err());
    }

    #[test]
    fn size_budget_includes_both_buffers_and_record_header() {
        let max_indices = (MAX_RECORD - MESH_HEADER_BYTES - VERTEX_STRIDE) / 4;
        assert!(max_indices.is_multiple_of(3));
        assert_eq!(checked_vertex_bytes(1, max_indices).unwrap(), VERTEX_STRIDE);
        assert!(checked_vertex_bytes(1, max_indices + 3).is_err());
        let max_vertices = (MAX_RECORD - MESH_HEADER_BYTES) / VERTEX_STRIDE;
        assert_eq!(
            checked_vertex_bytes(max_vertices, 0).unwrap(),
            max_vertices * VERTEX_STRIDE
        );
        assert!(checked_vertex_bytes(max_vertices + 1, 0).is_err());
        assert!(checked_vertex_bytes(max_vertices, 6).is_err());
        assert_eq!(checked_vertex_bytes(0, 0).unwrap(), 0);
        assert!(checked_vertex_bytes(usize::MAX, 0).is_err());
        assert!(checked_vertex_bytes(0, usize::MAX).is_err());
    }

    #[test]
    fn encoder_rejects_oversized_vertex_buffer() {
        let vertices = vec![vertex(); (MAX_RECORD - MESH_HEADER_BYTES) / VERTEX_STRIDE + 1];
        let error = encode_mesh(0, &vertices, &[]).err().unwrap();
        assert!(error.contains("32 MiB record budget"));
    }

    #[test]
    fn encoded_mesh_is_accepted_by_frame_decoder() {
        let mesh = encode_mesh(44, &[vertex(); 3], &[2, 0, 1]).unwrap();
        let mut payload = Vec::new();
        for value in [mesh.id, mesh.pass as u32, 3, 3] {
            payload.extend_from_slice(&value.to_le_bytes());
        }
        payload.extend_from_slice(&mesh.vertices);
        for value in &mesh.indices {
            payload.extend_from_slice(&value.to_le_bytes());
        }
        let mut frame = Frame::default();
        append_record(0, &payload, &mut frame).unwrap();
        assert_eq!(frame.meshes.len(), 1);
        assert_eq!(frame.meshes[0].id, mesh.id);
        assert_eq!(frame.meshes[0].pass, mesh.pass);
        assert_eq!(frame.meshes[0].vertices, mesh.vertices);
        assert_eq!(frame.meshes[0].indices, mesh.indices);
    }

    #[test]
    fn native_state_is_lequal_depth_writing_unculled_src_alpha() {
        let state = native_state();
        assert_eq!(state, 1 | 2 | (1 << 2) | (3 << 7));
        assert_eq!(state & 3, 3);
        assert_eq!((state >> 2) & 3, 1);
        assert_eq!((state >> 4) & 3, 0);
        assert_eq!((state >> 7) & 7, 3);
        assert_ne!(state, Pass::Terrain.default_state());
    }

    fn bytes(vertices: &[([f32; 3], [u8; 4])]) -> Vec<u8> {
        let list: Vec<TerrainVertex> = vertices
            .iter()
            .map(|(position, rgba)| TerrainVertex {
                position: *position,
                rgba: *rgba,
                texcoord: [position[0], position[2], 0.],
                normal: [0., -1., 0.],
            })
            .collect();
        encode_mesh(0, &list, &[]).unwrap().vertices
    }

    fn layer(material: i32, scale: i32, textured: bool) -> Layer {
        let vertices = bytes(&[
            ([0., -100., 0.], [9, 9, 9, 255]),
            ([1024., -300., 0.], [9, 9, 9, 255]),
            ([0., -100., 1024.], [9, 9, 9, 0]),
            ([1280., -500., 1280.], [9, 9, 9, 255]),
        ]);
        let texture = textured.then(|| LayerTexture {
            width: 2,
            height: 2,
            rgba: (0..16).collect(),
        });
        Layer {
            material,
            texture_scale: scale,
            vertices,
            indices: vec![0, 1, 2, 1, 3, 2],
            texture,
            overlay: false,
        }
    }

    fn word(block: &[u8], at: usize) -> u32 {
        u32::from_le_bytes(block[at..at + 4].try_into().unwrap())
    }

    fn float(block: &[u8], at: usize) -> f32 {
        f32::from_bits(word(block, at))
    }

    #[test]
    fn uniform_block_matches_the_pass_block_size_and_shader_offsets() {
        assert_eq!(UNIFORM_BYTES, Pass::Terrain.block_size());
        assert_eq!(offset::WATER_MODE, crate::water::TERRAIN_WATER_MODE);
        assert_eq!(offset::CLIP_PLANE, crate::water::CLIP_PLANE);
        let projection: Mat4 = core::array::from_fn(|i| i as f32 + 1.);
        let fog = Fog {
            enabled: true,
            start: 1234.5,
            end: 5678.25,
            color: [0.25, 0.5, 0.75],
        };
        let block = uniform_block(&projection, &fog, 1024, true).unwrap();
        assert_eq!(block.len(), 5552);
        for i in 0..16 {
            assert_eq!(float(&block, i * 4), i as f32 + 1.);
        }
        assert_eq!(float(&block, offset::TEXTURE_SCALE), 1. / 1024.);
        assert_eq!(word(&block, offset::HAS_TEXTURE), 1);
        assert_eq!(word(&block, offset::ALPHA_FROM_VERTEX), 1);
        assert_eq!(word(&block, offset::COMBINE_MODE), 0);
        assert_eq!(word(&block, offset::FOG_ENABLED), 1);
        assert_eq!(word(&block, offset::HAS_NORMALS), 0);
        assert_eq!(word(&block, offset::WATER_MODE), 0);
        assert_eq!(float(&block, offset::FOG_START), 1234.5);
        assert_eq!(float(&block, offset::FOG_END), 5678.25);
        for (i, value) in fog.color.iter().enumerate() {
            assert_eq!(float(&block, offset::FOG_COLOR + i * 4), *value);
        }
        assert_eq!(block[offset::CLIP_PLANE..offset::CLIP_PLANE + 16], [0; 16]);
    }

    #[test]
    fn untextured_uniform_disables_the_texture_and_ignores_the_scale() {
        let block =
            uniform_block(&math::IDENTITY, &test_fog(), UNTEXTURED_MATERIAL, false).unwrap();
        assert_eq!(word(&block, offset::HAS_TEXTURE), 0);
        assert_eq!(float(&block, offset::TEXTURE_SCALE), 1.);
        for scale in [0, -1] {
            assert!(uniform_block(&math::IDENTITY, &test_fog(), scale, true).is_err());
        }
    }

    fn test_fog() -> Fog {
        Fog {
            enabled: true,
            start: 1.,
            end: 2.,
            color: [0., 0., 0.],
        }
    }

    #[test]
    fn default_camera_centres_the_player_tile_with_native_depth_and_orientation() {
        let tile = [51, 55];
        let cam = camera(tile, -240., 720);
        let vp = view_projection(&cam, 1280, 720);
        let project = |p: [f32; 3]| -> [f32; 3] {
            let v = [p[0], p[1], p[2], 1.];
            let c: Vec<f32> = (0..4)
                .map(|r| (0..4).map(|k| vp[k * 4 + r] * v[k]).sum())
                .collect();
            [c[0] / c[3], c[1] / c[3], c[2] / c[3]]
        };
        let centre = tile_center(tile);
        let at = project([centre[0], -240., centre[1]]);
        assert!(at[0].abs() < 0.02 && at[1].abs() < 0.02, "{at:?}");
        assert!(at[2] > 0. && at[2] < 1.);
        let east = project([centre[0] + 2048., -240., centre[1]]);
        assert!(east[0] > 0.);
        let north = project([centre[0], -240., centre[1] + 2048.]);
        assert!(north[1] > 0.);
        let up = project([centre[0], -240. - 2048., centre[1]]);
        assert!(up[1] > 0.);
    }

    #[test]
    fn sky_is_the_default_fog_colour_with_forced_opaque_alpha() {
        let c = camera::color_channels(camera::DEFAULT_FOG_COLOR);
        assert_eq!(sky(), [c[0], c[1], c[2], 1.]);
    }

    #[test]
    fn a_camera_tile_far_outside_the_terrain_is_an_error_not_a_fog_frame() {
        let layers = [layer(-1, -1, false)];
        let error = ground_at(&layers, [7955, 8511]).unwrap_err();
        assert!(error.contains("outside the terrain"), "{error}");
        assert!(build_frame(&layers, [7955, 8511], 640, 360).is_err());
        assert_eq!(ground_at(&layers, [2, 2]), Ok(-500.));
    }

    #[test]
    fn default_view_reproduces_the_fixed_frame_exactly() {
        let layers = [layer(-1, -1, false), layer(128, 1024, true)];
        let fixed = build_frame(&layers, [2, 2], 640, 360).unwrap();
        let ground =
            ground_height(&layers, tile_center([2, 2])[0], tile_center([2, 2])[1]).unwrap();
        let view = View::default_for([2, 2], ground, 360);
        let live = build_frame_with(&layers, &view, 640, 360).unwrap();
        assert_eq!(fixed.clear, live.clear);
        for (a, b) in fixed.uniforms.iter().zip(&live.uniforms) {
            assert_eq!(a.bytes, b.bytes);
        }
    }

    #[test]
    fn live_bloom_parameters_reach_the_frame_and_are_validated() {
        let layers = [layer(-1, -1, false)];
        let mut view = View::default_for([2, 2], -300., 360);
        assert!(
            build_frame_with(&layers, &view, 64, 64)
                .unwrap()
                .bloom
                .is_none()
        );
        let p = crate::frame::BloomParams {
            white_point: 0.5,
            bloom: 3.,
            brightpass: 0.4,
        };
        view.bloom = Some(p);
        let f = build_frame_with(&layers, &view, 64, 64).unwrap();
        assert_eq!(f.bloom, Some(p));
        assert_eq!(
            crate::frame::parse(&crate::frame::write(&f)).unwrap().bloom,
            Some(p)
        );
        view.bloom = Some(crate::frame::BloomParams { bloom: 0., ..p });
        assert!(build_frame_with(&layers, &view, 64, 64).is_err());
    }

    #[test]
    fn live_pose_changes_the_matrix_and_live_environment_changes_fog_and_sky() {
        let layers = [layer(-1, -1, false)];
        let base = View::default_for([2, 2], -300., 360);
        let mut turned = base;
        turned.camera = follow([2, 2], -300., 1500, 0, 4096, 360);
        let mut foggy = base;
        foggy.environment = Environment {
            fog_enabled: false,
            fog_color: 0x336699,
            fog_depth: 40,
            fog_offset: 100,
            sky_argb: 0x00aa5500,
        };
        let a = build_frame_with(&layers, &base, 64, 64).unwrap();
        let b = build_frame_with(&layers, &turned, 64, 64).unwrap();
        let c = build_frame_with(&layers, &foggy, 64, 64).unwrap();
        let vp = |f: &Frame| f.uniforms[0].bytes[..64].to_vec();
        assert_ne!(vp(&a), vp(&b));
        assert_eq!(vp(&a), vp(&c));
        assert_ne!(a.clear, c.clear);
        assert_eq!(c.clear, camera::clear_color_opaque(0x00aa5500));
        assert_eq!(word(&c.uniforms[0].bytes, offset::FOG_ENABLED), 0);
        assert_eq!(word(&a.uniforms[0].bytes, offset::FOG_ENABLED), 1);
        let color = camera::batcher_color(0x336699);
        for (i, value) in color.iter().enumerate() {
            assert_eq!(
                float(&c.uniforms[0].bytes, offset::FOG_COLOR + i * 4),
                *value
            );
        }
        assert_ne!(
            float(&a.uniforms[0].bytes, offset::FOG_END),
            float(&c.uniforms[0].bytes, offset::FOG_END)
        );
    }

    #[test]
    fn invalid_live_views_are_rejected() {
        let layers = [layer(-1, -1, false)];
        let base = View::default_for([2, 2], -300., 360);
        let mut cases = vec![base; 5];
        cases[0].camera.scaled_fov = 0;
        cases[1].camera.scene_size_x = 0;
        cases[2].camera.pose.x = i32::MAX;
        cases[3].environment.fog_depth = -1;
        cases[4].environment.fog_offset = MAX_FOG_OFFSET + 1;
        for view in cases {
            assert!(build_frame_with(&layers, &view, 64, 64).is_err());
        }
    }

    #[test]
    fn ground_height_picks_the_vertex_nearest_to_the_tile_centre() {
        let layers = [layer(-1, -1, false)];
        assert_eq!(ground_height(&layers, 1000., 10.), Some(-300.));
        assert_eq!(ground_height(&layers, 1200., 1200.), Some(-500.));
        assert_eq!(ground_height(&[], 0., 0.), None);
        assert_eq!(vertex_at(&layers[0].vertices, 4), None);
        assert_eq!(
            vertex_at(&layers[0].vertices, 2),
            Some(([0., -100., 1024.], [9, 9, 9, 0]))
        );
    }

    #[test]
    fn frame_has_one_native_state_draw_per_layer_and_textures_only_for_textured_layers() {
        let layers = [
            layer(-1, -1, false),
            layer(128, 1024, true),
            layer(154, 692, true),
        ];
        let frame = build_frame(&layers, [2, 2], 640, 360).unwrap();
        assert_eq!((frame.width, frame.height, frame.clear), (640, 360, sky()));
        assert_eq!(
            (
                frame.meshes.len(),
                frame.uniforms.len(),
                frame.draws.len(),
                frame.textures.len()
            ),
            (3, 3, 3, 2)
        );
        for (at, draw) in frame.draws.iter().enumerate() {
            assert_eq!(draw.pass, Pass::Terrain);
            assert_eq!(draw.state, native_state());
            assert_eq!(draw.mesh, frame.meshes[at].id);
            assert_eq!(draw.uniform, frame.uniforms[at].id);
            assert_eq!(frame.uniforms[at].bytes.len(), UNIFORM_BYTES);
            assert_eq!(frame.meshes[at].vertices, layers[at].vertices);
            assert_eq!(frame.meshes[at].indices, layers[at].indices);
        }
        assert_eq!(frame.draws[0].textures, [NO_TEXTURE]);
        assert_eq!(word(&frame.uniforms[0].bytes, offset::HAS_TEXTURE), 0);
        assert_eq!(frame.draws[1].textures, [frame.textures[0].id]);
        assert_eq!(frame.draws[2].textures, [frame.textures[1].id]);
        assert_eq!(
            float(&frame.uniforms[1].bytes, offset::TEXTURE_SCALE),
            1. / 1024.
        );
        assert_eq!(
            float(&frame.uniforms[2].bytes, offset::TEXTURE_SCALE),
            1. / 692.
        );
        assert_eq!(frame.textures[0].format, TextureFormat::Rgba8);
        assert_eq!(frame.textures[0].rgba, (0..16).collect::<Vec<u8>>());
    }

    #[test]
    fn frame_survives_the_orf_writer_and_parser() {
        let layers = [layer(-1, -1, false), layer(128, 1024, true)];
        let frame = build_frame(&layers, [2, 2], 320, 200).unwrap();
        let parsed = crate::frame::parse(&crate::frame::write(&frame)).unwrap();
        assert_eq!((parsed.width, parsed.height), (320, 200));
        assert_eq!(
            (
                parsed.meshes.len(),
                parsed.textures.len(),
                parsed.uniforms.len(),
                parsed.draws.len()
            ),
            (2, 1, 2, 2)
        );
        assert_eq!(parsed.meshes[1].vertices, layers[1].vertices);
        assert_eq!(parsed.uniforms[1].bytes, frame.uniforms[1].bytes);
        assert_eq!(parsed.draws[1].state, native_state());
        assert_eq!(parsed.textures[0].rgba, frame.textures[0].rgba);
    }

    #[test]
    fn frame_builder_rejects_malformed_layers() {
        let tile = [2, 2];
        assert!(
            build_frame(&[], tile, 64, 64)
                .err()
                .unwrap()
                .contains("1 to")
        );
        assert!(build_frame(&[layer(-1, -1, false)], tile, 0, 64).is_err());
        let mut partial = layer(-1, -1, false);
        partial.vertices.push(0);
        assert!(build_frame(&[partial], tile, 64, 64).is_err());
        let mut stray = layer(-1, -1, false);
        stray.indices[5] = 4;
        assert!(
            build_frame(&[stray], tile, 64, 64)
                .err()
                .unwrap()
                .contains("exceeds vertex count 4")
        );
        let mut open = layer(-1, -1, false);
        open.indices.pop();
        assert!(
            build_frame(&[open], tile, 64, 64)
                .err()
                .unwrap()
                .contains("divisible by three")
        );
        let mut short = layer(128, 1024, true);
        short.texture.as_mut().unwrap().rgba.pop();
        assert!(
            build_frame(&[short], tile, 64, 64)
                .err()
                .unwrap()
                .contains("expected 16")
        );
        assert!(build_frame(&[layer(128, 0, true)], tile, 64, 64).is_err());
        let mut empty = layer(-1, -1, false);
        empty.vertices.clear();
        empty.indices.clear();
        assert!(
            build_frame(&[empty], tile, 64, 64)
                .err()
                .unwrap()
                .contains("no vertices")
        );
        assert!(build_frame(&vec![layer(-1, -1, false); 257], tile, 64, 64).is_err());
    }

    #[test]
    fn untextured_marker_ignores_an_attached_texture() {
        let mut marked = layer(UNTEXTURED_MATERIAL, -1, true);
        marked.texture = Some(LayerTexture {
            width: 1,
            height: 1,
            rgba: vec![1, 2, 3, 4],
        });
        let frame = build_frame(&[marked], [1, 1], 64, 64).unwrap();
        assert!(frame.textures.is_empty());
        assert_eq!(frame.draws[0].textures, [NO_TEXTURE]);
    }

    #[test]
    fn scene_batches_group_by_material_and_convert_colors() {
        let mut rgb = vec![0u32; 70000];
        rgb[5] = 0x0011_2233;
        rgb[65535] = 0x00aa_bbcc;
        let positions = [
            0, -10, 4, 512, -10, 4, 0, -10, 516, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 1, 0, 0, 0, 0,
            1,
        ];
        let batches =
            scene_batches(&positions, &[5, -1, 5], &[0, 100, 255], &[-1, 7, -1], &rgb).unwrap();
        assert_eq!(batches.len(), 2);
        assert_eq!(batches[0].material, -1);
        assert_eq!(batches[1].material, 7);
        assert_eq!(batches[0].indices, [0, 1, 2, 3, 4, 5]);
        assert_eq!(batches[0].vertices.len(), 6 * VERTEX_STRIDE);
        assert_eq!(&batches[0].vertices[12..16], &[0x11, 0x22, 0x33, 255]);
        let second = &batches[0].vertices[3 * VERTEX_STRIDE + 12..3 * VERTEX_STRIDE + 16];
        assert_eq!(second, &[0x11, 0x22, 0x33, 0]);
        assert_eq!(&batches[1].vertices[12..16], &[0xaa, 0xbb, 0xcc, 155]);
        let x = f32::from_le_bytes(batches[0].vertices[16..20].try_into().unwrap());
        let z = f32::from_le_bytes(batches[0].vertices[20..24].try_into().unwrap());
        assert_eq!((x, z), (0., 4.));
        let normal_y = f32::from_le_bytes(batches[0].vertices[32..36].try_into().unwrap());
        assert_eq!(normal_y, -1.);
    }

    #[test]
    fn scene_batches_reject_mismatched_arrays() {
        assert!(scene_batches(&[0; 8], &[0], &[0], &[0], &[0]).is_err());
        assert!(scene_batches(&[0; 9], &[0], &[], &[0], &[0]).is_err());
        assert!(scene_batches(&[], &[], &[], &[], &[]).unwrap().is_empty());
    }
}
