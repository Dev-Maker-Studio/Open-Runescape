use crate::{buttons::Buttons, orbit::Orbit, walker::Walker};
use renderer::{frame::Frame, stream::Input};

pub const TICK_HZ: u32 = 60;
pub const DT: f32 = 1. / TICK_HZ as f32;
pub const NAMES: [&str; 3] = ["orbit", "walker", "buttons"];

pub trait Game: Send {
    fn step(&mut self, dt: f32, inputs: &[Input]);
    fn frame(&mut self, width: u32, height: u32) -> Frame;
}

pub fn create(name: &str) -> Option<Box<dyn Game>> {
    Some(match name {
        "orbit" => Box::new(Orbit::default()),
        "walker" => Box::new(Walker::default()),
        "buttons" => Box::new(Buttons::default()),
        _ => return None,
    })
}

pub fn create_at(name: &str, frame: u32) -> Option<Box<dyn Game>> {
    let mut g = create(name)?;
    (0..frame).for_each(|_| g.step(DT, &[]));
    Some(g)
}
