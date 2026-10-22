use super::{openrs, place, s, save, Opts, Res};
use std::path::Path;

pub fn client_frames(o: &Opts, dir: &Path, count: u32, every: u32) -> Res {
    place(o)?;
    openrs(&["do", "dump-frames", s(dir), &count.to_string(), &format!("every={every}")])?;
    save(&dir.join("state.json"), &openrs(&["do", "state"])?)?;
    save(&dir.join("describe.txt"), &openrs(&["do", "describe"])?)
}

pub fn web_frames(client: &Path, dir: &Path, count: u32) -> Res {
    openrs(&["web", "up"])?;
    for i in 0..count {
        let name = format!("frame-{i:04}");
        openrs(&["web", "do", "replay", s(&client.join(format!("{name}.orf"))), ";", "wait-frames", "2", ";", "screenshot", s(&dir.join(format!("{name}.png"))), ";", "dump-frame", s(&dir.join(format!("{name}.orf")))])?;
    }
    save(&dir.join("state.json"), &openrs(&["web", "do", "state"])?)?;
    save(&dir.join("describe.txt"), &openrs(&["web", "do", "describe"])?)
}

pub fn python_args(count: u32, every: u32) -> [String; 4] {
    ["--frames".into(), count.to_string(), "--every".into(), every.to_string()]
}
