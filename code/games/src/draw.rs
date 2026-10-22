use renderer::frame::TextureFormat as RecordedTextureFormat;
use bytemuck::{bytes_of, cast_slice};
use renderer::{
    frame::{Draw, Frame, Mesh, Texture, Uniform},
    math::{self, Mat4},
    pass::{Pass, SpriteVertex},
    scene::quad_indices,
    uniforms::SpriteUniform,
};

pub const DIGITS: u32 = 7;
pub const HUD_MESH: u32 = 3;
pub const HUD_UNIFORM: u32 = 20;
pub const UNTEXTURED: f32 = -1.;
pub const WHITE: [u8; 4] = [255; 4];

const GLYPHS: [[u8; 7]; 10] = [
    [0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110],
    [0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110],
    [0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111],
    [0b11111, 0b00010, 0b00100, 0b00010, 0b00001, 0b10001, 0b01110],
    [0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010],
    [0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110],
    [0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110],
    [0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000],
    [0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110],
    [0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100],
];

#[derive(Default)]
pub struct Hud {
    pub vertices: Vec<SpriteVertex>,
    pub indices: Vec<u32>,
}

impl Hud {
    pub fn quad(&mut self, x: f32, y: f32, w: f32, h: f32, c: [u8; 4], tex: f32) {
        quad_indices(&mut self.indices, self.vertices.len() as u32);
        for (cu, cv) in [(0., 0.), (1., 0.), (1., 1.), (0., 1.)] {
            self.vertices.push(SpriteVertex { p: [x + cu * w, y + cv * h], uv: [cu, cv], c, tex, clip: [0., 0., 8192., 8192.] });
        }
    }

    pub fn number(&mut self, x: f32, y: f32, size: f32, value: u32, c: [u8; 4]) {
        let text = value.to_string();
        for (i, d) in text.bytes().enumerate() {
            self.quad(x + i as f32 * size * 0.75, y, size, size, c, (d - b'0') as f32);
        }
    }

    pub fn contains(x: f32, y: f32, r: [f32; 4]) -> bool {
        x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3]
    }

    pub fn finish(self, f: &mut Frame, width: u32, height: u32) {
        f.meshes.push(Mesh { id: HUD_MESH, pass: Pass::Sprite, vertices: cast_slice(&self.vertices).to_vec(), indices: self.indices });
        f.uniforms.push(Uniform { id: HUD_UNIFORM, bytes: bytes_of(&SpriteUniform { projection: math::ortho_top_left(width as f32, height as f32) }).to_vec() });
        f.draws.push(Draw::new(Pass::Sprite, HUD_MESH, HUD_UNIFORM, vec![DIGITS]));
    }
}

pub fn digits() -> Texture {
    let mut rgba = Vec::with_capacity(10 * 8 * 8 * 4);
    for g in &GLYPHS {
        for y in 0..8 {
            for x in 0..8 {
                let on = y < 7 && x < 5 && g[y] >> (4 - x) & 1 == 1;
                rgba.extend([255, 255, 255, if on { 255 } else { 0 }]);
            }
        }
    }
    Texture { sampler: 9, format: RecordedTextureFormat::Rgba8, id: DIGITS, width: 8, height: 8, layers: 10, rgba }
}

pub fn camera(eye: [f32; 3], at: [f32; 3], width: u32, height: u32) -> (Mat4, Mat4) {
    (math::look_at(eye, at), math::perspective(0.96, width as f32 / height.max(1) as f32, 0.1, 100.))
}

pub fn unproject_ground(view: &Mat4, proj: &Mat4, px: f32, py: f32, width: u32, height: u32) -> Option<[f32; 2]> {
    let inv = math::invert(&math::mul(proj, view));
    let (nx, ny) = (px / width as f32 * 2. - 1., 1. - py / height as f32 * 2.);
    let point = |z: f32| {
        let v = [nx, ny, z, 1.];
        let r: [f32; 4] = std::array::from_fn(|i| (0..4).map(|k| inv[k * 4 + i] * v[k]).sum());
        [r[0] / r[3], r[1] / r[3], r[2] / r[3]]
    };
    let (a, b) = (point(0.), point(1.));
    let dy = b[1] - a[1];
    let t = -a[1] / dy;
    (dy.abs() > 1e-6 && t > 0.).then(|| [a[0] + t * (b[0] - a[0]), a[2] + t * (b[2] - a[2])])
}
