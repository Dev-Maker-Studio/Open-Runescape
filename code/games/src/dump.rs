use crate::game::{self, DT};
use renderer::frame::{self, Frame};
use std::{collections::BTreeMap, path::{Path, PathBuf}};

pub fn frames(name: &str, start: u32, count: u32, width: u32, height: u32) -> Option<Vec<Frame>> {
    let mut g = game::create_at(name, start)?;
    let (mut meshes, mut textures) = (BTreeMap::new(), BTreeMap::new());
    let mut out = vec![];
    for _ in 0..count {
        let mut f = g.frame(width, height);
        f.meshes.drain(..).for_each(|m| drop(meshes.insert(m.id, m)));
        f.textures.drain(..).for_each(|t| drop(textures.insert(t.id, t)));
        f.meshes = meshes.values().cloned().collect();
        f.textures = textures.values().cloned().collect();
        out.push(f);
        g.step(DT, &[]);
    }
    Some(out)
}

pub fn write(name: &str, start: u32, count: u32, dir: &Path, width: u32, height: u32) -> Result<Vec<PathBuf>, String> {
    let frames = frames(name, start, count, width, height).ok_or_else(|| format!("unknown game {name}, games: {}", game::NAMES.join(" ")))?;
    std::fs::create_dir_all(dir).map_err(|e| format!("{}: {e}", dir.display()))?;
    frames
        .iter()
        .enumerate()
        .map(|(i, f)| {
            let path = dir.join(format!("frame-{}.orf", start + i as u32));
            std::fs::write(&path, frame::write(f)).map_err(|e| format!("{}: {e}", path.display()))?;
            Ok(path)
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::host::hash;

    #[test]
    fn orbit_is_deterministic_by_frame_number() {
        let a = frames("orbit", 10, 3, 320, 240).unwrap();
        let b = frames("orbit", 10, 3, 320, 240).unwrap();
        let c = frames("orbit", 0, 13, 320, 240).unwrap();
        for i in 0..3 {
            let bytes = frame::write(&a[i]);
            assert_eq!(hash(&bytes), hash(&frame::write(&b[i])));
            assert_eq!(hash(&bytes), hash(&frame::write(&c[10 + i])));
        }
        assert_ne!(hash(&frame::write(&a[0])), hash(&frame::write(&a[1])));
        assert_eq!((a[0].meshes.len(), a[0].textures.len()), (5, 3));
    }

    #[test]
    fn every_game_dumps_and_parses() {
        let dir = std::env::temp_dir().join(format!("openrs-games-{}", std::process::id()));
        for name in game::NAMES {
            let paths = write(name, 2, 2, &dir, 160, 120).unwrap();
            assert_eq!(paths.len(), 2);
            let f = frame::parse(&std::fs::read(&paths[1]).unwrap()).unwrap();
            assert!(!f.draws.is_empty() && !f.textures.is_empty());
            assert!(f.draws.iter().all(|d| f.meshes.iter().any(|m| m.id == d.mesh)));
        }
        assert!(write("nope", 0, 1, &dir, 8, 8).is_err());
        std::fs::remove_dir_all(dir).unwrap();
    }
}
