use renderer::frame::TextureFormat as RecordedTextureFormat;
use renderer::frame::{Frame, NO_INSTANCES, NO_TEXTURE, REFLECTION, SHADOW_MAP};
use std::collections::{HashMap, HashSet};

#[derive(Clone, Copy)]
pub struct Limits {
    pub ids: usize,
    pub bytes: usize,
    pub frames: usize,
}

impl Default for Limits {
    fn default() -> Self {
        Self {
            ids: 65536,
            bytes: 512 * 1024 * 1024,
            frames: 4096,
        }
    }
}

#[derive(Default)]
pub struct Resources {
    ids: HashMap<[u8; 32], u32>,
    bytes: usize,
    limits: Limits,
    frames: usize,
}

impl Resources {
    pub fn new(limits: Limits) -> Self {
        Self {
            limits,
            ..Self::default()
        }
    }

    fn id(&mut self, hash: [u8; 32], bytes: usize) -> (u32, bool) {
        if let Some(id) = self.ids.get(&hash) {
            return (*id, false);
        }
        let id = self.ids.len() as u32 + 1;
        self.ids.insert(hash, id);
        self.bytes += bytes;
        (id, true)
    }

    #[cfg(test)]
    pub fn split(&mut self, f: &mut Frame) -> Result<Frame, String> {
        self.split_epoch(f, false)?
            .map(|(r, _)| r)
            .ok_or("resource epoch requires drain".into())
    }

    pub fn split_epoch(
        &mut self,
        f: &mut Frame,
        drained: bool,
    ) -> Result<Option<(Frame, bool)>, String> {
        let hashes: Vec<([u8; 32], usize)> = f
            .meshes
            .iter()
            .map(|m| {
                let mut h = blake3::Hasher::new();
                h.update(&[0]);
                h.update(&(m.pass as u32).to_le_bytes());
                h.update(&(m.vertices.len() as u64).to_le_bytes());
                h.update(&m.vertices);
                h.update(bytemuck::cast_slice(&m.indices));
                (*h.finalize().as_bytes(), m.vertices.len() + m.indices.len() * 4)
            })
            .chain(f.textures.iter().map(|t| {
                let mut h = blake3::Hasher::new();
                h.update(&[1]);
                h.update(&t.width.to_le_bytes());
                h.update(&t.height.to_le_bytes());
                h.update(&t.layers.to_le_bytes());
                h.update(&(t.format as u32).to_le_bytes());
                if t.format != RecordedTextureFormat::Rgba8 { h.update(&t.sampler.to_le_bytes()); }
                h.update(&t.rgba);
                (*h.finalize().as_bytes(), t.rgba.len())
            }))
            .collect();
        let unique: HashMap<_, _> = hashes.iter().copied().collect();
        let bytes: usize = unique.values().sum();
        if unique.len() > self.limits.ids || bytes > self.limits.bytes {
            return Err("single frame exceeds resource epoch budget".into());
        }
        let (fresh, fresh_bytes) = unique
            .iter()
            .filter(|(h, _)| !self.ids.contains_key(*h))
            .fold((0, 0), |(n, bytes), (_, size)| (n + 1, bytes + size));
        let rotate = self.ids.len() + fresh > self.limits.ids
            || self.bytes + fresh_bytes > self.limits.bytes
            || self.frames >= self.limits.frames;
        if rotate && !drained {
            return Ok(None);
        }
        if rotate {
            self.ids.clear();
            self.bytes = 0;
            self.frames = 0;
        }
        let mut hashes = hashes.into_iter();
        let mut r = Frame::default();
        let mut meshes = HashMap::new();
        let mut textures = HashMap::new();
        for mut m in std::mem::take(&mut f.meshes) {
            let (hash, bytes) = hashes.next().unwrap();
            let (id, fresh) = self.id(hash, bytes);
            if meshes.insert(m.id, (id, m.pass)).is_some() {
                return Err("duplicate mesh id".into());
            }
            m.id = id;
            if fresh {
                r.meshes.push(m);
            }
        }
        for mut t in std::mem::take(&mut f.textures) {
            let (hash, bytes) = hashes.next().unwrap();
            let (id, fresh) = self.id(hash, bytes);
            if textures.insert(t.id, id).is_some() {
                return Err("duplicate texture id".into());
            }
            t.id = id;
            if fresh {
                r.textures.push(t);
            }
        }
        let uniforms: HashSet<_> = f.uniforms.iter().map(|u| u.id).collect();
        let instances: HashSet<_> = f.instances.iter().map(|i| i.id).collect();
        if uniforms.len() != f.uniforms.len() || instances.len() != f.instances.len() {
            return Err("duplicate frame id".into());
        }
        for d in &mut f.draws {
            if d.skip.is_some() {
                continue;
            }
            let (id, pass) = meshes.get(&d.mesh).ok_or("missing mesh")?;
            if pass.stride() != d.pass.stride()
                || !uniforms.contains(&d.uniform)
                || (d.instances != NO_INSTANCES && !instances.contains(&d.instances))
            {
                return Err("invalid draw reference".into());
            }
            d.mesh = *id;
            for t in &mut d.textures {
                if ![NO_TEXTURE, SHADOW_MAP, REFLECTION].contains(t) {
                    *t = *textures.get(t).ok_or("missing texture")?;
                }
            }
        }
        self.frames += 1;
        Ok(Some((r, rotate)))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use renderer::{scene, stream};

    fn frame(n: u32) -> Frame {
        let mut f = scene::build("model", 0, 64, 64).unwrap();
        f.meshes[0].vertices[..4].copy_from_slice(&(n as f32).to_le_bytes());
        f
    }

    #[test]
    fn thousands_of_changes_keep_host_and_consumer_within_current_epoch() {
        let mut host = Resources::new(Limits {
            ids: 4,
            bytes: 1024 * 1024,
            frames: 32,
        });
        let mut consumer = stream::Stream::default();
        consumer
            .push(&stream::epoch_hello("native", 64, 64, 5))
            .unwrap();
        let mut epoch = 0;
        for n in 1..=4096 {
            let mut f = frame(n);
            let (r, rotate) = host.split_epoch(&mut f, true).unwrap().unwrap();
            if rotate {
                epoch += 1;
                consumer.push(&stream::reset(epoch, n - 1)).unwrap();
                assert_eq!(r.textures.len(), 1);
            }
            consumer.push(&stream::resource(&r)).unwrap();
            consumer.push(&stream::frame_message(n, n, 0, &f)).unwrap();
            let displayed = consumer.take().unwrap();
            assert_eq!(displayed.draws[0].mesh, f.draws[0].mesh);
            assert_eq!(consumer.stats.shown, n);
            assert!(host.ids.len() <= 4);
            assert!(host.bytes <= 1024 * 1024);
            let stats: serde_json::Value = serde_json::from_str(&consumer.json()).unwrap();
            assert_eq!(stats["epochResources"], host.ids.len());
            assert_eq!(stats["epochBytes"], host.bytes);
        }
        assert_eq!(epoch, 1365);
        assert_eq!(consumer.stats.received, 4096);
    }

    #[test]
    fn byte_budget_and_frame_ceiling_rotate_without_partial_mutation() {
        let first = frame(1);
        let size = first
            .meshes
            .iter()
            .map(|m| m.vertices.len() + m.indices.len() * 4)
            .sum::<usize>()
            + first.textures.iter().map(|t| t.rgba.len()).sum::<usize>();
        let mut host = Resources::new(Limits {
            ids: 64,
            bytes: size,
            frames: 4096,
        });
        host.split_epoch(&mut first.clone(), true).unwrap().unwrap();
        let mut next = frame(2);
        let original = renderer::frame::write(&next);
        assert!(host.split_epoch(&mut next, false).unwrap().is_none());
        assert_eq!(renderer::frame::write(&next), original);
        assert_eq!((host.ids.len(), host.bytes), (2, size));
        let (r, rotate) = host.split_epoch(&mut next, true).unwrap().unwrap();
        assert!(rotate);
        assert_eq!((r.meshes.len(), r.textures.len()), (1, 1));
        let mut host = Resources::new(Limits {
            ids: 64,
            bytes: size,
            frames: 3,
        });
        for n in 0..10 {
            let (_, rotate) = host.split_epoch(&mut first.clone(), true).unwrap().unwrap();
            assert_eq!(rotate, n > 0 && n % 3 == 0);
            assert!(host.frames <= 3);
        }
        let mut host = Resources::new(Limits {
            ids: 1,
            bytes: size,
            frames: 3,
        });
        assert!(host.split_epoch(&mut first.clone(), true).is_err());
        assert!(host.ids.is_empty());
    }
}

#[cfg(test)]
mod typed_tests {
    use super::*;
    use renderer::{frame, scene, stream};
    use crate::source;

    #[test]
    fn typed_resource_identity_and_epoch_round_trip() {
        let typed = source::parse(include_bytes!("../../renderer/tests/fixtures/typed.orf")).unwrap().textures.remove(0);
        let mut host = Resources::new(Limits { ids: 8, bytes: 1024 * 1024, frames: 2 });
        let mut consumer = stream::Stream::default();
        consumer.push(&stream::epoch_hello("native", 64, 64, 5)).unwrap();
        let mut last_id = 0;
        for n in 0..4 {
            let mut f = scene::build("model", 0, 64, 64).unwrap();
            let mut t = typed.clone();
            t.id = f.textures[0].id;
            t.sampler = if n == 1 { 0 } else { 9 };
            f.textures[0] = t.clone();
            let (r, rotate) = host.split_epoch(&mut f, true).unwrap().unwrap();
            assert_eq!(rotate, n == 2);
            if rotate { consumer.push(&stream::reset(1, n)).unwrap(); }
            assert_eq!(r.textures.len(), if n == 3 { 0 } else { 1 });
            if n == 1 { assert_ne!(last_id, f.draws[0].textures[0]); }
            last_id = f.draws[0].textures[0];
            let parsed = frame::parse(&frame::write(&r)).unwrap();
            if let Some(t2) = parsed.textures.first() {
                assert_eq!((t2.format, t2.sampler, &t2.rgba), (t.format, t.sampler, &t.rgba));
            }
            consumer.push(&stream::resource(&r)).unwrap();
            consumer.push(&stream::frame_message(n + 1, n, 0, &f)).unwrap();
            let shown = consumer.take().unwrap();
            if n != 3 {
                assert_eq!(shown.textures[0].format, RecordedTextureFormat::Rgba16Float);
                assert_eq!(shown.textures[0].rgba, t.rgba);
            }
            assert_eq!(shown.draws[0].textures, f.draws[0].textures);
        }
    }

    #[test]
    #[ignore]
    fn benchmark_immutable_resource_identity() {
        let path = std::env::var("OPENRS_NATIVE_BENCH_ORF").expect("provide immutable recorded ORF");
        let bytes = std::fs::read(path).unwrap();
        let frame = crate::source::parse(&bytes).unwrap();
        let mut resources = Resources::new(Limits::default());
        let mut times = Vec::new();
        for n in 0..16 {
            let mut current = frame.clone();
            let start = std::time::Instant::now();
            let _ = resources.split_epoch(&mut current, true).unwrap().unwrap();
            if n > 0 { times.push(start.elapsed().as_secs_f64() * 1000.0); }
        }
        times.sort_by(f64::total_cmp);
        println!("resource_identity median_ms={:.3} retained_ids={} resource_bytes={}", times[times.len()/2], resources.ids.len(), resources.bytes);
    }

}
