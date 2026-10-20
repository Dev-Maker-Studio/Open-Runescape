use crate::{
    frame::{Draw, Frame, Mesh, NO_TEXTURE, SHADOW_MAP, Texture, Uniform},
    math::{self, Mat4},
    pass::{GroundVertex, MaskVertex, ModelVertex, ParticleVertex, Pass, SpriteVertex, WaterVertex},
    renderer::SHADOW_MAP_SIZE,
    uniforms::{GroundUniform, HdWaterUniform, MaterialUniform, ModelUniform, ParticleUniform, ShadowUniform, SpriteUniform},
};
use bytemuck::{Zeroable, bytes_of, cast_slice};

pub const NAMES: [&str; 8] = ["model", "ground", "sprite", "shadow", "particle", "water", "material", "all"];

const FOG: [f32; 4] = [0.45, 0.6, 0.8, 1.];
const SUN: [f32; 3] = [0.4, 0.8, 0.45];
const GRID: usize = 24;
const PALETTE: [[u8; 4]; 6] = [[230, 90, 80, 255], [90, 200, 110, 255], [90, 130, 230, 255], [240, 210, 90, 255], [200, 100, 220, 255], [100, 220, 220, 255]];
const CUBES: [(f32, f32, f32, f32); 3] = [(-2.5, 1.4, 0., 2.), (2.5, 1.1, 1., 1.5), (0., 0.9, 3., 1.)];

fn hash(x: usize, z: usize) -> u32 {
    (x as u32).wrapping_mul(73856093) ^ (z as u32).wrapping_mul(19349663)
}

fn height(x: f32, z: f32) -> f32 {
    0.5 * (0.45 * x).sin() + 0.4 * (0.35 * z).cos() + 0.25 * (0.2 * (x + z)).sin()
}

fn quad_indices(indices: &mut Vec<u32>, base: u32) {
    indices.extend([base, base + 1, base + 2, base, base + 2, base + 3]);
}

fn ground_mesh() -> Mesh {
    let half = GRID as f32 / 2.;
    let mut vertices = vec![];
    for iz in 0..=GRID {
        for ix in 0..=GRID {
            let (x, z) = (ix as f32 - half, iz as f32 - half);
            let h = height(x, z);
            let dx = 0.225 * (0.45 * x).cos() + 0.05 * (0.2 * (x + z)).cos();
            let dz = -0.14 * (0.35 * z).sin() + 0.05 * (0.2 * (x + z)).cos();
            let l = (dx * dx + 1. + dz * dz).sqrt();
            let j = (hash(ix, iz) % 24) as f32;
            let c = [(40. + j) as u8, (90. + 40. * h + j * 2.).clamp(0., 255.) as u8, 40, 255];
            vertices.push(GroundVertex { p: [x, h, z], n: [-dx / l, 1. / l, -dz / l], c });
        }
    }
    let mut indices = vec![];
    for iz in 0..GRID {
        for ix in 0..GRID {
            let a = (iz * (GRID + 1) + ix) as u32;
            let b = a + GRID as u32 + 1;
            indices.extend([a, b, a + 1, a + 1, b, b + 1]);
        }
    }
    Mesh { id: 1, pass: Pass::Ground, vertices: cast_slice(&vertices).to_vec(), indices }
}

fn cube_mesh() -> Mesh {
    let (mut vertices, mut indices) = (vec![], vec![]);
    for face in 0..6 {
        let (axis, sign) = (face / 2, if face % 2 == 0 { 1. } else { -1. });
        let unit = |a: usize, s: f32| std::array::from_fn(|i| if i == a % 3 { s } else { 0. });
        let (n, u, v): ([f32; 3], [f32; 3], [f32; 3]) = (unit(axis, sign), unit(axis + 1, 1.), unit(axis + 2, 1.));
        quad_indices(&mut indices, vertices.len() as u32);
        for (cu, cv) in [(0., 0.), (1., 0.), (1., 1.), (0., 1.)] {
            let p = std::array::from_fn(|i| 0.5 * (n[i] + (cu * 2. - 1.) * u[i] + (cv * 2. - 1.) * v[i]));
            vertices.push(ModelVertex { p, n, uv: [cu, cv], c: PALETTE[face] });
        }
    }
    Mesh { id: 2, pass: Pass::Model, vertices: cast_slice(&vertices).to_vec(), indices }
}

fn sprite_mesh() -> Mesh {
    let (mut vertices, mut indices) = (vec![], vec![]);
    let mut quad = |x: f32, y: f32, w: f32, h: f32, c: [u8; 4], tex: f32| {
        quad_indices(&mut indices, vertices.len() as u32);
        for (cu, cv) in [(0., 0.), (1., 0.), (1., 1.), (0., 1.)] {
            vertices.push(SpriteVertex { p: [x + cu * w, y + cv * h], uv: [cu, cv], c, tex, clip: [0., 0., 4096., 4096.] });
        }
    };
    for k in 0..8 {
        quad(16. + (k % 4) as f32 * 56., 16. + (k / 4) as f32 * 56., 48., 48., [255; 4], k as f32);
    }
    quad(260., 16., 100., 60., [255, 0, 0, 128], -1.);
    quad(300., 50., 100., 60., [0, 0, 255, 128], -1.);
    Mesh { id: 3, pass: Pass::Sprite, vertices: cast_slice(&vertices).to_vec(), indices }
}

fn mask_mesh() -> Mesh {
    let vertices: Vec<MaskVertex> = [(0., 0.), (1., 0.), (1., 1.), (0., 1.)].map(|(u, v)| MaskVertex { p: [420. + u * 100., 16. + v * 100.], base: [u, v], mask: [u, v] }).to_vec();
    Mesh { id: 6, pass: Pass::SurfaceMask, vertices: cast_slice(&vertices).to_vec(), indices: vec![0, 1, 2, 0, 2, 3] }
}

fn particle_mesh(view: &Mat4) -> Mesh {
    let (right, up) = ([view[0], view[4], view[8]], [view[1], view[5], view[9]]);
    let (mut vertices, mut indices) = (vec![], vec![]);
    for k in 0..12 {
        let (x, y, z) = ((k % 4) as f32 * 1.5 - 2.2, 2.2 + (k / 4) as f32 * 0.8, 4. - (k % 3) as f32);
        let c = [255, 200 - 12 * k as u8, 120, 160];
        quad_indices(&mut indices, vertices.len() as u32);
        for (cu, cv) in [(0., 0.), (1., 0.), (1., 1.), (0., 1.)] {
            let (su, sv) = (cu * 2. - 1., cv * 2. - 1.);
            vertices.push(ParticleVertex { p: std::array::from_fn(|i| [x, y, z][i] + 0.5 * (su * right[i] + sv * up[i])), uv: [cu, cv], c });
        }
    }
    Mesh { id: 4, pass: Pass::Particle, vertices: cast_slice(&vertices).to_vec(), indices }
}

fn water_mesh() -> Mesh {
    let corners = [(-10., -10.), (10., -10.), (10., 10.), (-10., 10.)];
    let vertices: Vec<WaterVertex> = corners.iter().map(|&(x, z)| WaterVertex { p: [x, 0.3, z], c: [40, 90, 140, 255], uv: [0., 0., 64.] }).collect();
    Mesh { id: 5, pass: Pass::HdWater, vertices: cast_slice(&vertices).to_vec(), indices: vec![0, 1, 2, 0, 2, 3] }
}

fn checker() -> Texture {
    let rgba = (0..64 * 64).flat_map(|i| if ((i % 64) / 8 + (i / 64) / 8) % 2 == 0 { [255, 255, 255, 255] } else { [150, 150, 170, 255] }).collect();
    Texture { id: 1, width: 64, height: 64, layers: 1, rgba }
}

fn sprite_layers() -> Texture {
    let mut rgba = vec![];
    for k in 0..8u32 {
        for y in 0..32u32 {
            for x in 0..32u32 {
                let (dx, dy) = (x as f32 - 15.5, y as f32 - 15.5);
                let alpha = if k == 7 && dx * dx + dy * dy > 200. { 0 } else { 255 };
                rgba.extend([(k * 36) as u8, (x * 8) as u8, (y * 8) as u8, alpha]);
            }
        }
    }
    Texture { id: 2, width: 32, height: 32, layers: 8, rgba }
}

fn disc_mask() -> Texture {
    let rgba = (0..32 * 32).flat_map(|i| {
        let (dx, dy) = ((i % 32) as f32 - 15.5, (i / 32) as f32 - 15.5);
        [255, 255, 255, if dx * dx + dy * dy < 220. { 255 } else { 0 }]
    }).collect();
    Texture { id: 4, width: 32, height: 32, layers: 1, rgba }
}

fn wave_noise() -> Texture {
    let n = 8usize;
    let rgba = (0..n * n * n)
        .flat_map(|i| {
            let (x, y, z) = ((i % n) as f32, ((i / n) % n) as f32, (i / n / n) as f32);
            let v = 0.5 + 0.25 * (x * 0.8 + z * 0.9).sin() + 0.25 * (y * 0.7 - x * 0.5).cos();
            [(v * 255.) as u8; 4]
        })
        .collect();
    Texture { id: 3, width: n as u32, height: n as u32, layers: n as u32, rgba }
}

fn view_projection(w: u32, h: u32) -> (Mat4, Mat4) {
    let view = math::look_at([0., 7., 13.], [0., 0.5, 0.]);
    (view, math::perspective(0.96, w as f32 / h as f32, 0.1, 100.))
}

fn light() -> Mat4 {
    let eye = SUN.map(|c| c * 30.);
    math::mul(&math::ortho(14., 1., 60.), &math::look_at(eye, [0., 0., 0.]))
}

fn cube_model(index: u32, k: usize) -> Mat4 {
    let (x, y, z, s) = CUBES[k];
    let angle = 0.4 + 0.03 * index as f32;
    math::mul(&math::translate(x, y, z), &math::mul(&math::rotate_y(angle + k as f32), &math::scale(s)))
}

fn model_uniform(id: u32, view: &Mat4, proj: &Mat4, model: &Mat4) -> Uniform {
    let mv = math::mul(view, model);
    let u = ModelUniform {
        mvp: math::mul(proj, &mv),
        mv,
        normal: math::normal_matrix(&mv),
        sun_dir: math::rotate_vec(view, SUN),
        sun_color: [1., 0.95, 0.85],
        ambient: 0.4,
        contrast: 0.8,
        fog_start: 15.,
        fog_end: 40.,
        fog_enabled: 1,
        pre_lit: 0,
        texture_enabled: 1,
        fog_color: FOG,
        alpha_threshold: 0.01,
        ..Zeroable::zeroed()
    };
    Uniform { id, bytes: bytes_of(&u).to_vec() }
}

fn material_uniform(id: u32, view: &Mat4, proj: &Mat4, model: &Mat4) -> Uniform {
    let mv = math::mul(view, model);
    let u = MaterialUniform {
        mvp: math::mul(proj, &mv),
        mv,
        normal: math::normal_matrix(&mv),
        sun_dir: math::rotate_vec(view, SUN),
        sun_color: [1., 0.95, 0.85],
        ambient: 0.4,
        contrast: 0.8,
        fog_start: 15.,
        fog_end: 40.,
        fog_enabled: 1,
        texture_enabled: 1,
        fog_color: FOG,
        alpha_threshold: 0.01,
        specular_level: 2,
        effect_enabled: 1,
        ..Zeroable::zeroed()
    };
    Uniform { id, bytes: bytes_of(&u).to_vec() }
}

fn ground_uniform(view: &Mat4, proj: &Mat4, shadow: bool) -> Uniform {
    let mv = *view;
    let u = GroundUniform {
        mvp: math::mul(proj, &mv),
        mv,
        normal: math::normal_matrix(&mv),
        sun_dir: math::rotate_vec(view, SUN),
        sun_color: [1., 0.95, 0.85],
        ambient: 0.45,
        contrast: 0.8,
        fog_start: 15.,
        fog_end: 40.,
        fog_enabled: 1,
        shadow_matrix: light(),
        shadow_enabled: shadow as u32,
        fog_color: FOG,
        shadow_bias: 0.002,
        shadow_intensity: 0.4,
        shadow_map_size: [SHADOW_MAP_SIZE as f32; 2],
        ..Zeroable::zeroed()
    };
    Uniform { id: 10, bytes: bytes_of(&u).to_vec() }
}

fn shadow_uniform(id: u32, model: &Mat4) -> Uniform {
    let mut light_vp = light();
    math::gl_to_wgpu_depth(&mut light_vp);
    let u = ShadowUniform { light_vp, model: *model, alpha_test: 0, alpha_threshold: 0.5, pad: [0.; 2] };
    Uniform { id, bytes: bytes_of(&u).to_vec() }
}

fn particle_uniform(view: &Mat4, proj: &Mat4) -> Uniform {
    let u = ParticleUniform {
        vp: math::mul(proj, view),
        ambient: [1.; 3],
        fog_enabled: 1,
        fog_start: 15.,
        fog_end: 40.,
        texture_enabled: 1,
        fog_color: FOG,
        ..Zeroable::zeroed()
    };
    Uniform { id: 30, bytes: bytes_of(&u).to_vec() }
}

fn water_uniform(index: u32, view: &Mat4, proj: &Mat4, width: u32, height: u32) -> Uniform {
    let vp = math::mul(proj, view);
    let u = HdWaterUniform {
        vp,
        has_wave_noise: 1,
        wave_scale: 6.,
        time: (index as f32 * 0.004).fract(),
        wave_amplitude: 0.6,
        has_water_depth: 1,
        inv_vp: math::invert(&vp),
        viewport: [0., 0., width as f32, height as f32],
        framebuffer_size: [width as f32, height as f32],
        sun_dir: SUN,
        sun_color: [1., 0.95, 0.85],
        ambient: 0.4,
        sky_color: [FOG[0], FOG[1], FOG[2]],
        water_color: [0.1, 0.3, 0.5],
        absorb_distance: 20.,
        scatter: 0.6,
        refraction: 24.,
        wave_strength: 1.3,
        slow_time: (index as f32 * 0.001).fract(),
        reflection_steps: 24,
        fog_enabled: 1,
        fog_start: 15.,
        fog_end: 40.,
        ..Zeroable::zeroed()
    };
    Uniform { id: 40, bytes: bytes_of(&u).to_vec() }
}

pub fn build(name: &str, index: u32, width: u32, height: u32) -> Option<Frame> {
    let all = name == "all";
    let first = index == 0;
    let want = |n: &str| all || name == n;
    let mut f = Frame { width, height, clear: FOG, ..Default::default() };
    let (view, proj) = view_projection(width, height);
    let cubes = want("model") || want("shadow");
    if want("ground") || want("shadow") {
        f.uniforms.push(ground_uniform(&view, &proj, want("shadow")));
        f.draws.push(Draw::new(Pass::Ground, 1, 10, vec![NO_TEXTURE, if want("shadow") { SHADOW_MAP } else { NO_TEXTURE }]));
        if first {
            f.meshes.push(ground_mesh());
        }
    }
    if cubes {
        for k in 0..CUBES.len() {
            let model = cube_model(index, k);
            let (id, shadow) = (11 + k as u32, 50 + k as u32);
            f.uniforms.push(model_uniform(id, &view, &proj, &model));
            f.draws.push(Draw::new(Pass::Model, 2, id, vec![1]));
            if want("shadow") {
                f.uniforms.push(shadow_uniform(shadow, &model));
                f.draws.push(Draw::new(Pass::Shadow, 2, shadow, vec![NO_TEXTURE]));
            }
        }
    }
    if want("material") {
        f.uniforms.push(material_uniform(60, &view, &proj, &math::mul(&math::translate(0., 1.2, -3.5), &math::rotate_y(0.7 + 0.02 * index as f32))));
        f.draws.push(Draw::new(Pass::Material, 2, 60, vec![1]));
    }
    if (cubes || want("material") || want("particle")) && first {
        f.meshes.push(cube_mesh());
        f.textures.push(checker());
    }
    if want("water") {
        f.uniforms.push(water_uniform(index, &view, &proj, width, height));
        f.draws.push(Draw::new(Pass::HdWater, 5, 40, vec![3, NO_TEXTURE, NO_TEXTURE, NO_TEXTURE, NO_TEXTURE]));
        if first {
            f.meshes.push(water_mesh());
            f.textures.push(wave_noise());
        }
    }
    if want("particle") {
        f.uniforms.push(particle_uniform(&view, &proj));
        f.draws.push(Draw::new(Pass::Particle, 4, 30, vec![1]));
        if first {
            f.meshes.push(particle_mesh(&view));
        }
    }
    if want("sprite") {
        let projection = math::ortho_top_left(width as f32, height as f32);
        f.uniforms.push(Uniform { id: 20, bytes: bytes_of(&SpriteUniform { projection }).to_vec() });
        f.draws.push(Draw::new(Pass::Sprite, 3, 20, vec![2]));
        f.draws.push(Draw::new(Pass::SurfaceMask, 6, 20, vec![2, 4]));
        if first {
            f.meshes.extend([sprite_mesh(), mask_mesh()]);
            f.textures.extend([sprite_layers(), disc_mask()]);
        }
    }
    (!f.draws.is_empty()).then_some(f)
}

pub struct Source {
    scene: String,
    replay: Option<Frame>,
}

impl Source {
    pub fn scene(name: &str) -> Self {
        Source { scene: name.into(), replay: None }
    }

    pub fn replay(frame: Frame) -> Self {
        Source { scene: String::new(), replay: Some(frame) }
    }

    pub fn next(&mut self, index: u32, width: u32, height: u32) -> Frame {
        match &mut self.replay {
            Some(f) => {
                let out = f.clone();
                f.meshes.clear();
                f.textures.clear();
                f.instances.clear();
                out
            }
            None => build(&self.scene, index, width, height).expect("unknown scene"),
        }
    }
}
