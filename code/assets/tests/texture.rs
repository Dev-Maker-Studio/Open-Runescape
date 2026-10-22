use flate2::read::GzDecoder;
use openrs_assets::{
    is_opaque_effect, render_material, Error, MaterialSource, MaterialType, TextureGraph,
    TextureSources,
};
use std::collections::{BTreeMap, HashMap};
use std::io::Read;

const FIXTURE: &[u8] = include_bytes!("fixtures/textures.bin.gz");

const K: [u32; 64] = [
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
];

fn sha256(data: &[u8]) -> [u8; 32] {
    let mut h: [u32; 8] = [
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab,
        0x5be0cd19,
    ];
    let mut message = data.to_vec();
    message.push(0x80);
    while message.len() % 64 != 56 {
        message.push(0);
    }
    message.extend_from_slice(&((data.len() as u64) * 8).to_be_bytes());
    for block in message.chunks(64) {
        let mut w = [0u32; 64];
        for i in 0..16 {
            w[i] = u32::from_be_bytes([
                block[4 * i],
                block[4 * i + 1],
                block[4 * i + 2],
                block[4 * i + 3],
            ]);
        }
        for i in 16..64 {
            let s0 = w[i - 15].rotate_right(7) ^ w[i - 15].rotate_right(18) ^ (w[i - 15] >> 3);
            let s1 = w[i - 2].rotate_right(17) ^ w[i - 2].rotate_right(19) ^ (w[i - 2] >> 10);
            w[i] = w[i - 16]
                .wrapping_add(s0)
                .wrapping_add(w[i - 7])
                .wrapping_add(s1);
        }
        let mut v = h;
        for i in 0..64 {
            let s1 = v[4].rotate_right(6) ^ v[4].rotate_right(11) ^ v[4].rotate_right(25);
            let ch = (v[4] & v[5]) ^ (!v[4] & v[6]);
            let t1 = v[7]
                .wrapping_add(s1)
                .wrapping_add(ch)
                .wrapping_add(K[i])
                .wrapping_add(w[i]);
            let s0 = v[0].rotate_right(2) ^ v[0].rotate_right(13) ^ v[0].rotate_right(22);
            let maj = (v[0] & v[1]) ^ (v[0] & v[2]) ^ (v[1] & v[2]);
            let t2 = s0.wrapping_add(maj);
            v[7] = v[6];
            v[6] = v[5];
            v[5] = v[4];
            v[4] = v[3].wrapping_add(t1);
            v[3] = v[2];
            v[2] = v[1];
            v[1] = v[0];
            v[0] = t1.wrapping_add(t2);
        }
        for i in 0..8 {
            h[i] = h[i].wrapping_add(v[i]);
        }
    }
    let mut out = [0u8; 32];
    for (i, word) in h.iter().enumerate() {
        out[4 * i..4 * i + 4].copy_from_slice(&word.to_be_bytes());
    }
    out
}

struct Material {
    id: u32,
    flags: u8,
    effect_id: i8,
    effect_combiner: u8,
    texture: Vec<u8>,
    hash: [u8; 32],
}

struct Fixture {
    sprites: HashMap<u32, Vec<u8>>,
    materials: Vec<Material>,
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
    fn u8(&mut self) -> u8 {
        self.take(1)[0]
    }
    fn u32(&mut self) -> u32 {
        let b = self.take(4);
        u32::from_be_bytes([b[0], b[1], b[2], b[3]])
    }
}

fn load() -> Fixture {
    let mut raw = Vec::new();
    GzDecoder::new(FIXTURE).read_to_end(&mut raw).unwrap();
    let mut c = Cursor {
        data: raw,
        position: 0,
    };
    let mut sprites = HashMap::new();
    for _ in 0..c.u32() {
        let id = c.u32();
        let len = c.u32() as usize;
        sprites.insert(id, c.take(len).to_vec());
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
        materials.push(Material {
            id,
            flags,
            effect_id,
            effect_combiner,
            texture,
            hash,
        });
    }
    Fixture { sprites, materials }
}

impl TextureSources for Fixture {
    fn sprite(&self, id: u32) -> Option<&[u8]> {
        self.sprites.get(&id).map(Vec::as_slice)
    }
    fn material(&self, id: u32) -> Option<MaterialSource<'_>> {
        self.materials
            .iter()
            .find(|m| m.id == id && m.flags & 32 != 0)
            .map(|m| MaterialSource {
                texture: &m.texture,
                is_small: m.flags & 2 != 0,
                is_brick_tile: m.flags & 4 != 0,
            })
    }
}

fn material_type(m: &Material) -> MaterialType {
    MaterialType {
        details_only: false,
        is_small: m.flags & 2 != 0,
        skip_triangles: false,
        brightness: 0,
        alpha: 0,
        effect_id: m.effect_id,
        effect_param1: 0,
        color_hsl: 0,
        texture_speed_u: 0,
        texture_speed_v: 0,
        a_bool2087: false,
        is_brick_tile: m.flags & 4 != 0,
        mipmapping: 0,
        repeat_s: false,
        repeat_t: false,
        hdr: m.flags & 16 != 0,
        combine_mode: 0,
        effect_param2: 0,
        effect_combiner: m.effect_combiner,
    }
}

fn pixel_hash(pixels: &[u32]) -> [u8; 32] {
    let mut bytes = Vec::with_capacity(pixels.len() * 4);
    for pixel in pixels {
        bytes.extend_from_slice(&pixel.to_le_bytes());
    }
    sha256(&bytes)
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn sha256_matches_known_vector() {
    let digest = sha256(b"abc");
    assert_eq!(
        digest[..4],
        [0xba, 0x78, 0x16, 0xbf],
        "sha-256 of abc must start with ba7816bf"
    );
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn texture_pixels_match_the_oracle_byte_for_byte() {
    let fixture = load();
    let mut exact = 0usize;
    let mut cached = 0usize;
    let mut unsupported: BTreeMap<u8, usize> = BTreeMap::new();
    let mut other_errors: BTreeMap<String, usize> = BTreeMap::new();
    let mut mismatched = Vec::new();
    let mut mismatch_ops: BTreeMap<&'static str, usize> = BTreeMap::new();
    let mut exact_ops: BTreeMap<&'static str, usize> = BTreeMap::new();
    let mut path_disagreements = 0usize;
    for material in &fixture.materials {
        let kind = material_type(material);
        let argb = material.flags & 8 != 0;
        let opaque =
            material.effect_combiner != 2 && is_opaque_effect(i32::from(material.effect_id));
        if material.flags & 1 != 0 && opaque == argb {
            path_disagreements += 1;
        }
        if material.flags & 1 == 0 {
            continue;
        }
        cached += 1;
        match render_material(&material.texture, &kind, &fixture) {
            Ok(pixels) => {
                if pixel_hash(&pixels) == material.hash {
                    exact += 1;
                    if let Ok(want) = std::env::var("TEXTURE_EMB") {
                        if let Ok(graph) = TextureGraph::decode(&material.texture) {
                            for kind in graph.debug_kinds() {
                                if kind.starts_with(&want) {
                                    eprintln!("EXACT {} {kind}", material.id);
                                }
                            }
                        }
                    }
                    if let Ok(graph) = TextureGraph::decode(&material.texture) {
                        let mut names = graph.operation_names();
                        names.sort_unstable();
                        names.dedup();
                        for name in names {
                            *exact_ops.entry(name).or_default() += 1;
                        }
                    }
                } else {
                    mismatched.push(material.id);
                    #[cfg(not(target_arch = "wasm32"))]
                    if let Ok(dir) = std::env::var("TEXTURE_GOT") {
                        let bytes: Vec<u8> = pixels.iter().flat_map(|p| p.to_le_bytes()).collect();
                        std::fs::write(format!("{dir}/got-{}.bgra", material.id), bytes).unwrap();
                    }
                    #[cfg(not(target_arch = "wasm32"))]
                    if let Ok(dir) = std::env::var("TEXTURE_DUMPS") {
                        if let Ok(expected) =
                            std::fs::read(format!("{dir}/material-{}.bgra", material.id))
                        {
                            let size = if kind.is_small { 64 } else { 128 };
                            let mut differing = 0usize;
                            let mut first = None;
                            for (index, pixel) in pixels.iter().enumerate() {
                                let want = u32::from_le_bytes([
                                    expected[4 * index],
                                    expected[4 * index + 1],
                                    expected[4 * index + 2],
                                    expected[4 * index + 3],
                                ]);
                                if want != *pixel {
                                    differing += 1;
                                    if first.is_none() {
                                        first = Some((index % size, index / size, want, *pixel));
                                    }
                                }
                            }
                            eprintln!(
                                "diff {} count={differing} first(x,y,want,got)={first:x?}",
                                material.id
                            );
                        }
                    }
                    #[cfg(not(target_arch = "wasm32"))]
                    if std::env::var("TEXTURE_VERBOSE").is_ok() {
                        if let Ok(graph) = TextureGraph::decode(&material.texture) {
                            eprintln!(
                                "mismatch {} argb={} ops={:?} out={:?}",
                                material.id,
                                argb,
                                graph.operation_names(),
                                graph.output_indices()
                            );
                            eprintln!("structure {:?}", graph.structure());
                            for kind in graph.debug_kinds() {
                                eprintln!("  kind {kind}");
                            }
                        }
                    }
                    if let Ok(graph) = TextureGraph::decode(&material.texture) {
                        let mut names = graph.operation_names();
                        names.sort_unstable();
                        names.dedup();
                        for name in names {
                            *mismatch_ops.entry(name).or_default() += 1;
                        }
                    }
                }
            }
            Err(Error::UnsupportedTextureOperation(id)) => *unsupported.entry(id).or_default() += 1,
            Err(other) => *other_errors.entry(other.to_string()).or_default() += 1,
        }
    }
    let report = format!(
        "cached={cached} exact={exact} mismatched={} unsupported_materials={} other_errors={:?} path_disagreements={path_disagreements}\nunsupported_by_first_missing_impl={unsupported:?}\nexact_ops={exact_ops:?}\nmismatch_ops={mismatch_ops:?}\nfirst_mismatches={:?}\n",
        mismatched.len(),
        unsupported.values().sum::<usize>(),
        other_errors,
        &mismatched[..mismatched.len().min(20)],
    );
    eprintln!("{report}");
    #[cfg(not(target_arch = "wasm32"))]
    if let Ok(path) = std::env::var("TEXTURE_REPORT") {
        std::fs::write(path, &report).unwrap();
    }
    assert_eq!(path_disagreements, 0);
    assert!(
        mismatched.is_empty(),
        "{} materials differ from the oracle",
        mismatched.len()
    );
    assert!(exact > 0);
}
