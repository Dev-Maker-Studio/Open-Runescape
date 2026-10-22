use crate::{
    draw::{self, Hud, WHITE},
    game::Game,
};
use renderer::{
    frame::Frame,
    stream::{self, Input},
};

const CLEAR: [f32; 4] = [0.12, 0.12, 0.14, 1.];
const BUTTON: [f32; 4] = [40., 60., 120., 48.];
const GAP: f32 = 140.;
const TRACK: [f32; 4] = [40., 180., 400., 8.];
const KNOB: [f32; 2] = [16., 28.];
const COLORS: [[u8; 4]; 3] = [[200, 80, 80, 255], [80, 180, 90, 255], [80, 110, 220, 255]];

#[derive(Default)]
pub struct Buttons {
    pub mouse: [f32; 2],
    pub hover: Option<usize>,
    pub pressed: Option<usize>,
    pub clicks: [u32; 3],
    pub slider: f32,
    pub dragging: bool,
    sent: bool,
}

fn button(i: usize) -> [f32; 4] {
    [BUTTON[0] + i as f32 * GAP, BUTTON[1], BUTTON[2], BUTTON[3]]
}

fn knob(slider: f32) -> [f32; 4] {
    [TRACK[0] + slider * TRACK[2] - KNOB[0] / 2., TRACK[1] + TRACK[3] / 2. - KNOB[1] / 2., KNOB[0], KNOB[1]]
}

impl Buttons {
    fn at(&self, x: f32, y: f32) -> Option<usize> {
        (0..3).find(|&i| Hud::contains(x, y, button(i)))
    }

    fn slide(&mut self, x: f32) {
        self.slider = ((x - TRACK[0]) / TRACK[2]).clamp(0., 1.);
    }
}

impl Game for Buttons {
    fn step(&mut self, _dt: f32, inputs: &[Input]) {
        for i in inputs {
            match i.kind {
                stream::MOVE => {
                    self.mouse = [i.a, i.b];
                    if self.dragging {
                        self.slide(i.a);
                    }
                }
                stream::DOWN if i.button == 0 => {
                    self.mouse = [i.a, i.b];
                    self.pressed = self.at(i.a, i.b);
                    let track = [TRACK[0], TRACK[1] - KNOB[1] / 2., TRACK[2], KNOB[1]];
                    if Hud::contains(i.a, i.b, knob(self.slider)) || Hud::contains(i.a, i.b, track) {
                        self.dragging = true;
                        self.slide(i.a);
                    }
                }
                stream::UP if i.button == 0 => {
                    if let Some(p) = self.pressed.filter(|p| self.at(i.a, i.b) == Some(*p)) {
                        self.clicks[p] += 1;
                    }
                    self.pressed = None;
                    self.dragging = false;
                }
                stream::WHEEL => self.slider = (self.slider - i.b.signum() * 0.05).clamp(0., 1.),
                stream::FOCUS if i.a == 0. => {
                    self.pressed = None;
                    self.dragging = false;
                }
                _ => {}
            }
        }
        self.hover = self.at(self.mouse[0], self.mouse[1]);
    }

    fn frame(&mut self, width: u32, height: u32) -> Frame {
        let mut f = Frame { width, height, clear: CLEAR, ..Default::default() };
        let mut hud = Hud::default();
        hud.quad(24., 40., 440., 180., [40, 40, 48, 255], draw::UNTEXTURED);
        for i in 0..3 {
            let [x, y, w, h] = button(i);
            let mut c = COLORS[i];
            if self.pressed == Some(i) {
                c = c.map(|v| v / 2);
            } else if self.hover == Some(i) {
                c = c.map(|v| v.saturating_add(50));
            }
            hud.quad(x, y, w, h, c, draw::UNTEXTURED);
            hud.number(x + 8., y + 12., 24., self.clicks[i], WHITE);
        }
        hud.quad(TRACK[0], TRACK[1], TRACK[2], TRACK[3], [90, 90, 100, 255], draw::UNTEXTURED);
        let [kx, ky, kw, kh] = knob(self.slider);
        hud.quad(kx, ky, kw, kh, if self.dragging { [255, 230, 120, 255] } else { [220, 200, 90, 255] }, draw::UNTEXTURED);
        hud.number(TRACK[0] + TRACK[2] + 16., TRACK[1] - 8., 24., (self.slider * 100.).round() as u32, WHITE);
        hud.quad(self.mouse[0] - 3., self.mouse[1] - 3., 6., 6., [255, 255, 255, 160], draw::UNTEXTURED);
        hud.finish(&mut f, width, height);
        if !std::mem::replace(&mut self.sent, true) {
            f.textures.push(draw::digits());
        }
        f
    }
}
