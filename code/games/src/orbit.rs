use crate::{
    draw::{self, Hud, WHITE},
    game::Game,
};
use bytemuck::cast_slice;
use renderer::{
    frame::{Draw, Frame, Mesh, NO_TEXTURE, SHADOW_MAP},
    math::{self, Mat4},
    pass::{ParticleVertex, Pass},
    scene::{self, quad_indices},
    stream::Input,
};

const CLEAR: [f32; 4] = [0.45, 0.6, 0.8, 1.];
const CUBES: [(f32, f32, f32, f32); 3] = [(-2.5, 1.4, 0., 2.), (2.5, 1.1, 1., 1.5), (0., 0.9, 3., 1.)];
const PARTICLES: u32 = 24;
pub const WATER: bool = true;

#[derive(Default)]
pub struct Orbit {
    pub tick: u32,
    sent: bool,
}

fn particles(view: &Mat4, t: f32) -> Mesh {
    let (right, up) = ([view[0], view[4], view[8]], [view[1], view[5], view[9]]);
    let (mut vertices, mut indices) = (vec![], vec![]);
    for k in 0..PARTICLES {
        let phase = (t * 0.5 + k as f32 / PARTICLES as f32).fract();
        let angle = k as f32 * 2.399 + t * 0.7;
        let (x, y, z) = (4. * angle.cos() * (1. - phase * 0.5), 0.8 + phase * 4., 4. * angle.sin() * (1. - phase * 0.5));
        let c = [255, 200 - (k * 5) as u8, 120, (200. * (1. - phase)) as u8];
        let s = 0.25 + 0.3 * phase;
        quad_indices(&mut indices, vertices.len() as u32);
        for (cu, cv) in [(0., 0.), (1., 0.), (1., 1.), (0., 1.)] {
            let (su, sv) = (cu * 2. - 1., cv * 2. - 1.);
            vertices.push(ParticleVertex { p: std::array::from_fn(|i| [x, y, z][i] + s * (su * right[i] + sv * up[i])), uv: [cu, cv], c });
        }
    }
    Mesh { id: 4, pass: Pass::Particle, vertices: cast_slice(&vertices).to_vec(), indices }
}

impl Game for Orbit {
    fn step(&mut self, _dt: f32, _inputs: &[Input]) {
        self.tick += 1;
    }

    fn frame(&mut self, width: u32, height: u32) -> Frame {
        let t = self.tick as f32 / 60.;
        let mut f = Frame { width, height, clear: CLEAR, ..Default::default() };
        let eye = [13. * (0.3 * t).cos(), 7. + 2. * (0.2 * t).sin(), 13. * (0.3 * t).sin()];
        let (view, proj) = draw::camera(eye, [0., 0.5, 0.], width, height);
        f.uniforms.push(scene::ground_uniform(&view, &proj, true));
        f.draws.push(Draw::new(Pass::Ground, 1, 10, vec![NO_TEXTURE, SHADOW_MAP]));
        for (k, &(x, y, z, s)) in CUBES.iter().enumerate() {
            let model = math::mul(&math::translate(x, y + 0.3 * (t * 2. + k as f32).sin(), z), &math::mul(&math::rotate_y(t * (1. + 0.5 * k as f32)), &math::scale(s)));
            let (id, shadow) = (11 + k as u32, 50 + k as u32);
            f.uniforms.push(scene::model_uniform(id, &view, &proj, &model));
            f.draws.push(Draw::new(Pass::Model, 2, id, vec![1]));
            f.uniforms.push(scene::shadow_uniform(shadow, &model));
            f.draws.push(Draw::new(Pass::Shadow, 2, shadow, vec![NO_TEXTURE]));
        }
        if WATER {
            f.uniforms.push(scene::water_uniform(self.tick, &view, &proj, width, height));
            f.draws.push(Draw::new(Pass::HdWater, 5, 40, vec![3, NO_TEXTURE, NO_TEXTURE, NO_TEXTURE, NO_TEXTURE]));
        }
        f.uniforms.push(scene::particle_uniform(&view, &proj));
        f.draws.push(Draw::new(Pass::Particle, 4, 30, vec![1]));
        f.meshes.push(particles(&view, t));
        let mut hud = Hud::default();
        hud.quad(16., 16., 8. + (self.tick % 120) as f32 * 2., 12., [255, 220, 80, 220], draw::UNTEXTURED);
        hud.number(16., 36., 24., self.tick, WHITE);
        hud.finish(&mut f, width, height);
        if !std::mem::replace(&mut self.sent, true) {
            f.meshes.extend([scene::ground_mesh(), scene::cube_mesh(), scene::water_mesh()]);
            f.textures.extend([scene::checker(), scene::wave_noise(), draw::digits()]);
        }
        f
    }
}
