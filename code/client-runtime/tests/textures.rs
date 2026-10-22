use flate2::read::GzDecoder;
use openrs_assets::{MaterialTable, MaterialType};
use openrs_client_runtime::textures::{TextureSet, argb_le_bytes};
use sha2::{Digest, Sha256};
use std::io::Read;

const FIXTURE: &[u8] = include_bytes!("../../assets/tests/fixtures/textures.bin.gz");

struct Entry {
    id: u32,
    flags: u8,
    effect_id: i8,
    effect_combiner: u8,
    texture: Vec<u8>,
    hash: [u8; 32],
}

struct Oracle {
    sprites: Vec<(u32, Vec<u8>)>,
    materials: Vec<Entry>,
}

struct Cursor {
    data: Vec<u8>,
    position: usize,
}

impl Cursor {
    fn take(&mut self, count: usize) -> &[u8] {
        let slice = &self.data[self.position..self.position + count];
        self.position += count;
        slice
    }
    fn u32(&mut self) -> u32 {
        let b = self.take(4);
        u32::from_be_bytes([b[0], b[1], b[2], b[3]])
    }
    fn u8(&mut self) -> u8 {
        self.take(1)[0]
    }
}

fn load() -> Oracle {
    let mut raw = Vec::new();
    GzDecoder::new(FIXTURE).read_to_end(&mut raw).unwrap();
    let mut c = Cursor {
        data: raw,
        position: 0,
    };
    let mut sprites = Vec::new();
    for _ in 0..c.u32() {
        let id = c.u32();
        let len = c.u32() as usize;
        sprites.push((id, c.take(len).to_vec()));
    }
    let mut materials = Vec::new();
    for _ in 0..c.u32() {
        let id = c.u32();
        let flags = c.u8();
        let effect_id = c.u8() as i8;
        let effect_combiner = c.u8();
        let len = c.u32() as usize;
        let texture = c.take(len).to_vec();
        let mut hash = [0u8; 32];
        hash.copy_from_slice(c.take(32));
        materials.push(Entry {
            id,
            flags,
            effect_id,
            effect_combiner,
            texture,
            hash,
        });
    }
    Oracle { sprites, materials }
}

fn kind(entry: &Entry) -> MaterialType {
    MaterialType {
        details_only: false,
        is_small: entry.flags & 2 != 0,
        skip_triangles: false,
        brightness: 0,
        alpha: 0,
        effect_id: entry.effect_id,
        effect_param1: 0,
        color_hsl: 0,
        texture_speed_u: 0,
        texture_speed_v: 0,
        a_bool2087: false,
        is_brick_tile: entry.flags & 4 != 0,
        mipmapping: 0,
        repeat_s: false,
        repeat_t: false,
        hdr: entry.flags & 16 != 0,
        combine_mode: 0,
        effect_param2: 0,
        effect_combiner: entry.effect_combiner,
    }
}

fn set(oracle: &Oracle) -> TextureSet {
    let top = oracle.materials.iter().map(|m| m.id).max().unwrap() as usize;
    let mut slots: Vec<Option<MaterialType>> = vec![None; top + 1];
    for entry in &oracle.materials {
        if entry.flags & 32 != 0 {
            slots[entry.id as usize] = Some(kind(entry));
        }
    }
    let mut set = TextureSet::new();
    set.install_table(MaterialTable { materials: slots });
    for (id, bytes) in &oracle.sprites {
        set.add_sprite(*id, bytes).unwrap();
    }
    for entry in &oracle.materials {
        if entry.flags & 32 != 0 {
            set.add_graph(entry.id, &entry.texture).unwrap();
        }
    }
    set
}

#[test]
fn every_cached_oracle_material_renders_to_the_oracle_hash() {
    let oracle = load();
    let set = set(&oracle);
    let mut exact = 0usize;
    let mut cached = 0usize;
    for entry in oracle.materials.iter().filter(|m| m.flags & 1 != 0) {
        cached += 1;
        let pixels = set.render_argb(entry.id).unwrap();
        let digest: [u8; 32] = Sha256::digest(argb_le_bytes(&pixels)).into();
        assert_eq!(digest, entry.hash, "material {}", entry.id);
        let rgba = set.render_rgba(entry.id).unwrap();
        assert_eq!(rgba.len(), pixels.len() * 4);
        let size = set.size(entry.id).unwrap();
        assert_eq!(pixels.len(), size * size);
        let mut bgra = Vec::with_capacity(rgba.len());
        for chunk in rgba.chunks_exact(4) {
            bgra.extend_from_slice(&[chunk[2], chunk[1], chunk[0], chunk[3]]);
        }
        assert_eq!(bgra, argb_le_bytes(&pixels));
        exact += 1;
    }
    assert_eq!(exact, cached);
    assert!(cached > 2000);
}

#[test]
fn missing_inputs_are_reported_and_block_rendering() {
    let oracle = load();
    let entry = oracle
        .materials
        .iter()
        .find(|m| m.flags & 33 == 33 && !oracle.sprites.is_empty())
        .unwrap();
    let mut set = TextureSet::new();
    assert!(set.needs(entry.id).is_err());
    let top = oracle.materials.iter().map(|m| m.id).max().unwrap() as usize;
    let mut slots: Vec<Option<MaterialType>> = vec![None; top + 1];
    for other in &oracle.materials {
        if other.flags & 32 != 0 {
            slots[other.id as usize] = Some(kind(other));
        }
    }
    set.install_table(MaterialTable { materials: slots });
    let needs = set.needs(entry.id).unwrap();
    assert_eq!(needs.graphs, vec![entry.id]);
    assert!(set.render_rgba(entry.id).is_err());
    set.add_graph(entry.id, &entry.texture).unwrap();
    assert!(set.needs(entry.id).unwrap().graphs.is_empty());
    assert!(set.add_graph(entry.id, &[1, 2, 3]).is_err());
}
