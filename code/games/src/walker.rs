use crate::{
    draw::{self, Hud, WHITE},
    game::Game,
};
use renderer::{
    frame::{Draw, Frame, NO_TEXTURE, SHADOW_MAP},
    math,
    pass::Pass,
    scene,
    stream::{self, Input},
};
use std::collections::HashSet;

const CLEAR: [f32; 4] = [0.45, 0.6, 0.8, 1.];
const SPEED: f32 = 4.;
const LIMIT: f32 = 11.;
const OFFSET: [f32; 3] = [0., 6., 9.];

#[derive(Default)]
pub struct Walker {
    pub pos: [f32; 2],
    pub target: Option<[f32; 2]>,
    pub keys: HashSet<String>,
    pub clicks: u32,
    size: (u32, u32),
    sent: bool,
}

impl Walker {
    fn view(&self, width: u32, height: u32) -> ([f32; 16], [f32; 16]) {
        let at = [self.pos[0], scene::height(self.pos[0], self.pos[1]) + 0.5, self.pos[1]];
        draw::camera([at[0] + OFFSET[0], at[1] + OFFSET[1], at[2] + OFFSET[2]], at, width, height)
    }

    fn axis(&self, neg: [&str; 2], pos: [&str; 2]) -> f32 {
        let on = |k: [&str; 2]| k.iter().any(|c| self.keys.contains(*c)) as u8 as f32;
        on(pos) - on(neg)
    }
}

impl Game for Walker {
    fn step(&mut self, dt: f32, inputs: &[Input]) {
        for i in inputs {
            match i.kind {
                stream::KEY_DOWN => {
                    self.keys.insert(i.code.clone());
                    self.target = None;
                }
                stream::KEY_UP => {
                    self.keys.remove(&i.code);
                }
                stream::RESIZE => self.size = (i.a as u32, i.b as u32),
                stream::DOWN if i.button == 0 && self.size.0 > 0 => {
                    let (view, proj) = self.view(self.size.0, self.size.1);
                    self.target = draw::unproject_ground(&view, &proj, i.a, i.b, self.size.0, self.size.1).map(|p| p.map(|v| v.clamp(-LIMIT, LIMIT)));
                    self.clicks += 1;
                }
                stream::FOCUS if i.a == 0. => self.keys.clear(),
                _ => {}
            }
        }
        let mut d = [self.axis(["KeyA", "ArrowLeft"], ["KeyD", "ArrowRight"]), self.axis(["KeyW", "ArrowUp"], ["KeyS", "ArrowDown"])];
        if d == [0., 0.] {
            if let Some(t) = self.target {
                let (dx, dz) = (t[0] - self.pos[0], t[1] - self.pos[1]);
                let len = (dx * dx + dz * dz).sqrt();
                if len < SPEED * dt {
                    self.pos = t;
                    self.target = None;
                    return;
                }
                d = [dx / len, dz / len];
            }
        }
        let len = (d[0] * d[0] + d[1] * d[1]).sqrt().max(1.);
        self.pos = [(self.pos[0] + d[0] / len * SPEED * dt).clamp(-LIMIT, LIMIT), (self.pos[1] + d[1] / len * SPEED * dt).clamp(-LIMIT, LIMIT)];
    }

    fn frame(&mut self, width: u32, height: u32) -> Frame {
        self.size = (width, height);
        let mut f = Frame { width, height, clear: CLEAR, ..Default::default() };
        let (view, proj) = self.view(width, height);
        f.uniforms.push(scene::ground_uniform(&view, &proj, true));
        f.draws.push(Draw::new(Pass::Ground, 1, 10, vec![NO_TEXTURE, SHADOW_MAP]));
        let body = math::mul(&math::translate(self.pos[0], scene::height(self.pos[0], self.pos[1]) + 0.5, self.pos[1]), &math::scale(0.8));
        f.uniforms.push(scene::model_uniform(11, &view, &proj, &body));
        f.draws.push(Draw::new(Pass::Model, 2, 11, vec![1]));
        f.uniforms.push(scene::shadow_uniform(50, &body));
        f.draws.push(Draw::new(Pass::Shadow, 2, 50, vec![NO_TEXTURE]));
        if let Some(t) = self.target {
            let marker = math::mul(&math::translate(t[0], scene::height(t[0], t[1]) + 0.1, t[1]), &math::scale(0.3));
            f.uniforms.push(scene::model_uniform(12, &view, &proj, &marker));
            f.draws.push(Draw::new(Pass::Model, 2, 12, vec![NO_TEXTURE]));
        }
        let mut hud = Hud::default();
        hud.number(16., 16., 20., ((self.pos[0] + LIMIT) * 10.) as u32, WHITE);
        hud.number(16., 40., 20., ((self.pos[1] + LIMIT) * 10.) as u32, WHITE);
        hud.number(16., 64., 20., self.clicks, [255, 220, 80, 255]);
        hud.finish(&mut f, width, height);
        if !std::mem::replace(&mut self.sent, true) {
            f.meshes.extend([scene::ground_mesh(), scene::cube_mesh()]);
            f.textures.extend([scene::checker(), draw::digits()]);
        }
        f
    }
}
