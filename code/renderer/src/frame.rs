use crate::{math, pass::Pass};
use std::collections::BTreeMap;

pub const MAGIC: &[u8; 4] = b"ORF3";
pub const MAGIC_V5: &[u8; 4] = b"ORF5";
pub const MAGIC_V4: &[u8; 4] = b"ORF4";
pub const MAGIC_V2: &[u8; 4] = b"ORF2";
pub const MAGIC_V1: &[u8; 4] = b"ORF1";
pub const GL_DEPTH: u32 = 1;
pub const DEFLATE: u32 = 2;
pub const NO_TEXTURE: u32 = u32::MAX;
pub const SHADOW_MAP: u32 = u32::MAX - 1;
pub const REFLECTION: u32 = u32::MAX - 2;
pub const NO_INSTANCES: u32 = u32::MAX;
const SKIP: u32 = 255;
const STORED_MAX: usize = 128;
const MESH: u32 = 0;
const TEXTURE: u32 = 1;
const UNIFORM: u32 = 2;
const INSTANCES: u32 = 3;
const DRAW: u32 = 4;
const END: u32 = 5;
const TYPED_TEXTURE: u32 = 6;
const BLOOM: u32 = 7;
const MAX_RECORD: usize = 32 * 1024 * 1024;
const MAX_FRAME: usize = 128 * 1024 * 1024;

#[derive(Clone)]
pub struct Mesh {
    pub id: u32,
    pub pass: Pass,
    pub vertices: Vec<u8>,
    pub indices: Vec<u32>,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
#[repr(u32)]
pub enum TextureFormat {
    Rgba8 = 0,
    Rgba16Float = 1,
}

impl TextureFormat {
    pub fn from_u32(value: u32) -> Option<Self> {
        match value {
            0 => Some(Self::Rgba8),
            1 => Some(Self::Rgba16Float),
            _ => None,
        }
    }

    pub fn texel_bytes(self) -> usize {
        match self {
            Self::Rgba8 => 4,
            Self::Rgba16Float => 8,
        }
    }

    pub fn byte_size(self, width: u32, height: u32, layers: u32) -> Option<usize> {
        if !(1..=8192).contains(&width)
            || !(1..=8192).contains(&height)
            || !(1..=2048).contains(&layers)
            || self != Self::Rgba8 && layers != 1
        {
            return None;
        }
        let size = (width as usize)
            .checked_mul(height as usize)?
            .checked_mul(layers as usize)?
            .checked_mul(self.texel_bytes())?;
        (size <= MAX_RECORD - if self == Self::Rgba8 { 16 } else { 28 }).then_some(size)
    }
}

#[derive(Clone)]
pub struct Texture {
    pub id: u32,
    pub width: u32,
    pub height: u32,
    pub layers: u32,
    pub rgba: Vec<u8>,
    pub format: TextureFormat,
    pub sampler: u32,
}

#[derive(Clone)]
pub struct Uniform {
    pub id: u32,
    pub bytes: Vec<u8>,
}

#[derive(Clone)]
pub struct Instances {
    pub id: u32,
    pub count: u32,
    pub bytes: Vec<u8>,
}

#[derive(Clone)]
pub struct Draw {
    pub pass: Pass,
    pub mesh: u32,
    pub uniform: u32,
    pub textures: Vec<u32>,
    pub state: u32,
    pub viewport: [u32; 4],
    pub scissor: [u32; 4],
    pub instances: u32,
    pub skip: Option<String>,
}

impl Draw {
    pub fn new(pass: Pass, mesh: u32, uniform: u32, textures: Vec<u32>) -> Self {
        Draw {
            pass,
            mesh,
            uniform,
            textures,
            state: pass.default_state(),
            viewport: [0; 4],
            scissor: [0; 4],
            instances: NO_INSTANCES,
            skip: None,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct BloomParams {
    pub white_point: f32,
    pub bloom: f32,
    pub brightpass: f32,
}

impl BloomParams {
    pub const DEFAULT: BloomParams = BloomParams {
        white_point: 1.,
        bloom: 1.,
        brightpass: 0.25,
    };

    pub fn valid(&self) -> bool {
        self.white_point.is_finite()
            && self.brightpass.is_finite()
            && self.bloom.is_finite()
            && self.bloom > 0.
    }
}

fn bloom_record(r: &mut Reader) -> Option<BloomParams> {
    let p = BloomParams {
        white_point: r.f32()?,
        bloom: r.f32()?,
        brightpass: r.f32()?,
    };
    p.valid().then_some(p)
}

#[derive(Default, Clone)]
pub struct Frame {
    pub width: u32,
    pub height: u32,
    pub clear: [f32; 4],
    pub bloom: Option<BloomParams>,
    pub meshes: Vec<Mesh>,
    pub textures: Vec<Texture>,
    pub uniforms: Vec<Uniform>,
    pub instances: Vec<Instances>,
    pub draws: Vec<Draw>,
}

impl Frame {
    pub fn skipped(&self) -> BTreeMap<&str, u32> {
        let mut out = BTreeMap::new();
        self.draws
            .iter()
            .filter_map(|d| d.skip.as_deref())
            .for_each(|n| *out.entry(n).or_insert(0) += 1);
        out
    }

    pub fn summary(&self) -> String {
        let mut drawn = BTreeMap::new();
        self.draws
            .iter()
            .filter(|d| d.skip.is_none())
            .for_each(|d| *drawn.entry(d.pass.shader()).or_insert(0u32) += 1);
        let list = |m: Vec<(&str, u32)>| {
            m.iter()
                .map(|(n, c)| format!("{n}={c}"))
                .collect::<Vec<_>>()
                .join(",")
        };
        let skipped = self.skipped();
        format!(
            "replay: {} draws, drawn {} [{}], skipped {} [{}]",
            self.draws.len(),
            drawn.values().sum::<u32>(),
            list(drawn.into_iter().collect()),
            skipped.values().sum::<u32>(),
            list(skipped.into_iter().collect())
        )
    }
}

struct Reader<'a>(&'a [u8]);

impl<'a> Reader<'a> {
    fn slice(&mut self, n: usize) -> Option<&'a [u8]> {
        let (head, tail) = self.0.split_at_checked(n)?;
        self.0 = tail;
        Some(head)
    }

    fn bytes(&mut self, n: usize) -> Option<Vec<u8>> {
        self.slice(n).map(<[u8]>::to_vec)
    }

    fn u32(&mut self) -> Option<u32> {
        Some(u32::from_le_bytes(self.slice(4)?.try_into().ok()?))
    }

    fn f32(&mut self) -> Option<f32> {
        self.u32().map(f32::from_bits)
    }

    fn array<const N: usize>(&mut self) -> Option<[u32; N]> {
        let mut out = [0; N];
        for v in &mut out {
            *v = self.u32()?;
        }
        Some(out)
    }

    fn list<T>(&mut self, mut item: impl FnMut(&mut Self) -> Option<T>) -> Option<Vec<T>> {
        let n = self.u32()?;
        if n as usize > self.0.len() / 4 {
            return None;
        }
        (0..n).map(|_| item(self)).collect()
    }
}

fn mesh(r: &mut Reader) -> Option<Mesh> {
    let (id, pass, vc, ic) = (r.u32()?, Pass::from_u32(r.u32()?)?, r.u32()?, r.u32()?);
    let vertices = r.slice((vc as usize).checked_mul(pass.stride() as usize)?)?;
    let indices = bytemuck::pod_collect_to_vec(r.slice((ic as usize).checked_mul(4)?)?);
    if indices.iter().any(|&i| i >= vc) {
        return None;
    }
    Some(Mesh {
        id,
        pass,
        vertices: vertices.to_vec(),
        indices,
    })
}

fn texture(r: &mut Reader) -> Option<Texture> {
    let [id, width, height, layers] = r.array()?;
    let format = TextureFormat::Rgba8;
    let rgba = r.bytes(format.byte_size(width, height, layers)?)?;
    Some(Texture {
        id,
        width,
        height,
        layers,
        rgba,
        format,
        sampler: 9,
    })
}

pub fn valid_sampler(bits: u32) -> bool {
    bits & !255 == 0 && bits & 7 <= 5 && (bits >> 4) & 3 <= 2 && (bits >> 6) & 3 <= 2
}

fn typed_texture(r: &mut Reader) -> Option<Texture> {
    let [id, width, height, layers, raw_format, len, sampler] = r.array()?;
    let format = TextureFormat::from_u32(raw_format)?;
    if !valid_sampler(sampler)
        || format != TextureFormat::Rgba16Float
        || len as usize != format.byte_size(width, height, layers)?
    {
        return None;
    }
    Some(Texture {
        id,
        width,
        height,
        layers,
        format,
        sampler,
        rgba: r.bytes(len as usize)?,
    })
}

fn uniform(r: &mut Reader) -> Option<Uniform> {
    let (id, len) = (r.u32()?, r.u32()?);
    if len > 65536 {
        return None;
    }
    Some(Uniform {
        id,
        bytes: r.bytes(len as usize)?,
    })
}

fn instances(r: &mut Reader) -> Option<Instances> {
    let (id, count, len) = (r.u32()?, r.u32()?, r.u32()?);
    if u64::from(count) * crate::pass::INSTANCE_STRIDE != u64::from(len) {
        return None;
    }
    Some(Instances {
        id,
        count,
        bytes: r.bytes(len as usize)?,
    })
}

fn draw(r: &mut Reader, v1: bool) -> Option<Draw> {
    let (raw, mesh, uniform) = (r.u32()?, r.u32()?, r.u32()?);
    let pass = match raw {
        SKIP if !v1 => Pass::Model,
        _ => Pass::from_u32(raw)?,
    };
    let n = r.u32()?;
    if n > 16 {
        return None;
    }
    let textures = (0..n).map(|_| r.u32()).collect::<Option<Vec<_>>>()?;
    if v1 {
        return Some(Draw::new(pass, mesh, uniform, textures));
    }
    let (state, viewport, scissor, inst) = (r.u32()?, r.array()?, r.array()?, r.u32()?);
    let len = r.u32()? as usize;
    if len > 1024 {
        return None;
    }
    let name = r.slice(len)?;
    let skip = (raw == SKIP).then(|| String::from_utf8_lossy(name).into_owned());
    Some(Draw {
        pass,
        mesh,
        uniform,
        textures,
        state,
        viewport,
        scissor,
        instances: inst,
        skip,
    })
}

fn append<T>(
    payload: &[u8],
    records: &mut Vec<T>,
    decode: impl FnOnce(&mut Reader) -> Option<T>,
    legacy_padding: bool,
) -> Option<()> {
    let mut r = Reader(payload);
    let value = decode(&mut r)?;
    if !r.0.is_empty() && (!legacy_padding || r.0 != [0, 0, 0, 0]) {
        return None;
    }
    records.push(value);
    Some(())
}

pub fn append_record(kind: u32, payload: &[u8], f: &mut Frame) -> Option<()> {
    if payload.len() > MAX_RECORD {
        return None;
    }
    match kind {
        MESH => append(payload, &mut f.meshes, mesh, false),
        TEXTURE => append(payload, &mut f.textures, texture, false),
        TYPED_TEXTURE => append(payload, &mut f.textures, typed_texture, false),
        UNIFORM => append(payload, &mut f.uniforms, uniform, false),
        INSTANCES => append(payload, &mut f.instances, instances, false),
        DRAW => append(payload, &mut f.draws, |r| draw(r, false), true),
        BLOOM if f.bloom.is_none() => {
            let mut found = Vec::with_capacity(1);
            append(payload, &mut found, bloom_record, false)?;
            f.bloom = found.pop();
            Some(())
        }
        _ => None,
    }
}

pub fn inflate_record(data: &[u8], raw: usize) -> Option<Vec<u8>> {
    use miniz_oxide::inflate::{
        TINFLStatus,
        core::{DecompressorOxide, decompress, inflate_flags},
    };
    if raw > MAX_RECORD || data.len() > MAX_RECORD {
        return None;
    }
    let mut out = vec![0; raw];
    let (status, consumed, written) = decompress(
        &mut DecompressorOxide::default(),
        data,
        &mut out,
        0,
        inflate_flags::TINFL_FLAG_PARSE_ZLIB_HEADER
            | inflate_flags::TINFL_FLAG_USING_NON_WRAPPING_OUTPUT_BUF,
    );
    (status == TINFLStatus::Done && consumed == data.len() && written == raw).then_some(out)
}

pub fn normalize_depth(f: &mut Frame, flags: u32) -> Option<()> {
    if flags & GL_DEPTH != 0 {
        for u in &mut f.uniforms {
            let m: &mut [f32; 16] = bytemuck::try_from_bytes_mut(u.bytes.get_mut(..64)?).ok()?;
            math::gl_to_wgpu_depth(m);
        }
    }
    Some(())
}

fn chunks(r: &mut Reader, f: &mut Frame, version: u32) -> Option<()> {
    let typed = version >= 4;
    let last = if version >= 5 { BLOOM } else { TYPED_TEXTURE };
    let mut total = 32usize;
    let mut count = 0usize;
    loop {
        let [kind, raw, packed] = r.array()?;
        total = total.checked_add(raw as usize)?.checked_add(12)?;
        count += 1;
        if raw as usize > MAX_RECORD
            || packed as usize > MAX_RECORD
            || total > MAX_FRAME
            || count > 100000
        {
            return None;
        }
        if kind == END {
            return (raw == 0 && packed == 0 && r.0.is_empty()).then_some(());
        }
        if kind > last || kind == TYPED_TEXTURE && !typed {
            return None;
        }
        let inflated;
        let payload = if packed == 0 {
            r.slice(raw as usize)?
        } else {
            inflated = inflate_record(r.slice(packed as usize)?, raw as usize)?;
            &inflated
        };
        append_record(kind, payload, f)?;
    }
}

pub fn parse(data: &[u8]) -> Option<Frame> {
    if data.len() > MAX_FRAME {
        return None;
    }
    let mut r = Reader(data);
    let version = match r.slice(4)? {
        m if m == MAGIC => 3,
        m if m == MAGIC_V5 => 5,
        m if m == MAGIC_V4 => 4,
        m if m == MAGIC_V2 => 2,
        m if m == MAGIC_V1 => 1,
        _ => return None,
    };
    let flags = r.u32()?;
    let (width, height) = (r.u32()?, r.u32()?);
    let clear = [r.f32()?, r.f32()?, r.f32()?, r.f32()?];
    let mut f = Frame {
        width,
        height,
        clear,
        ..Default::default()
    };
    if version >= 3 {
        chunks(&mut r, &mut f, version)?;
    } else {
        let inflated;
        if flags & DEFLATE != 0 {
            inflated =
                miniz_oxide::inflate::decompress_to_vec_zlib_with_limit(r.0, MAX_FRAME).ok()?;
            r = Reader(&inflated);
        }
        f.meshes = r.list(mesh)?;
        f.textures = r.list(texture)?;
        f.uniforms = r.list(uniform)?;
        f.instances = if version == 2 {
            r.list(instances)?
        } else {
            vec![]
        };
        f.draws = r.list(|r| draw(r, version == 1))?;
    }
    normalize_depth(&mut f, flags)?;
    Some(f)
}

fn put(o: &mut Vec<u8>, v: &[u32]) {
    v.iter().for_each(|x| o.extend(x.to_le_bytes()));
}

fn chunk(out: &mut Vec<u8>, kind: u32, record: &[u8]) {
    if record.len() < STORED_MAX {
        put(out, &[kind, record.len() as u32, 0]);
        out.extend(record);
        return;
    }
    let packed = miniz_oxide::deflate::compress_to_vec_zlib(record, 3);
    put(out, &[kind, record.len() as u32, packed.len() as u32]);
    out.extend(packed);
}

pub fn write(frame: &Frame) -> Vec<u8> {
    let mut out = if frame.bloom.is_some() {
        MAGIC_V5
    } else if frame
        .textures
        .iter()
        .any(|t| t.format != TextureFormat::Rgba8)
    {
        MAGIC_V4
    } else {
        MAGIC
    }
    .to_vec();
    put(&mut out, &[DEFLATE, frame.width, frame.height]);
    frame.clear.iter().for_each(|c| out.extend(c.to_le_bytes()));
    let mut o = vec![];
    for m in &frame.meshes {
        put(
            &mut o,
            &[
                m.id,
                m.pass as u32,
                (m.vertices.len() as u64 / m.pass.stride()) as u32,
                m.indices.len() as u32,
            ],
        );
        o.extend(&m.vertices);
        o.extend(bytemuck::cast_slice::<u32, u8>(&m.indices));
        chunk(&mut out, MESH, &std::mem::take(&mut o));
    }
    for t in &frame.textures {
        put(&mut o, &[t.id, t.width, t.height, t.layers]);
        assert_eq!(
            t.format.byte_size(t.width, t.height, t.layers),
            Some(t.rgba.len())
        );
        let kind = if t.format == TextureFormat::Rgba8 {
            TEXTURE
        } else {
            assert!(valid_sampler(t.sampler));
            put(&mut o, &[t.format as u32, t.rgba.len() as u32, t.sampler]);
            TYPED_TEXTURE
        };
        o.extend(&t.rgba);
        chunk(&mut out, kind, &std::mem::take(&mut o));
    }
    for u in &frame.uniforms {
        put(&mut o, &[u.id, u.bytes.len() as u32]);
        o.extend(&u.bytes);
        chunk(&mut out, UNIFORM, &std::mem::take(&mut o));
    }
    for i in &frame.instances {
        put(&mut o, &[i.id, i.count, i.bytes.len() as u32]);
        o.extend(&i.bytes);
        chunk(&mut out, INSTANCES, &std::mem::take(&mut o));
    }
    for d in &frame.draws {
        put(
            &mut o,
            &[
                d.skip.as_ref().map_or(d.pass as u32, |_| SKIP),
                d.mesh,
                d.uniform,
                d.textures.len() as u32,
            ],
        );
        put(&mut o, &d.textures);
        put(&mut o, &[d.state]);
        put(&mut o, &d.viewport);
        put(&mut o, &d.scissor);
        put(&mut o, &[d.instances]);
        let name = d.skip.as_deref().unwrap_or("");
        put(&mut o, &[name.len() as u32]);
        o.extend(name.as_bytes());
        chunk(&mut out, DRAW, &std::mem::take(&mut o));
    }
    if let Some(p) = frame.bloom {
        assert!(p.valid());
        [p.white_point, p.bloom, p.brightpass]
            .iter()
            .for_each(|v| o.extend(v.to_le_bytes()));
        chunk(&mut out, BLOOM, &std::mem::take(&mut o));
    }
    put(&mut out, &[END, 0, 0]);
    out
}

#[cfg(test)]
mod texture_tests {
    use super::*;

    #[test]
    fn record_helper_and_chunk_parser_keep_allocation_bounds() {
        let oversized = vec![0; MAX_RECORD + 1];
        assert!(append_record(UNIFORM, &oversized, &mut Frame::default()).is_none());
        drop(oversized);
        let header = write(&Frame::default())[..32].to_vec();
        for (raw, packed, payload) in [
            (MAX_RECORD as u32 + 1, 0, vec![]),
            (8, MAX_RECORD as u32 + 1, vec![]),
            (8, 0, vec![0; 7]),
            (8, 0, vec![0; 12]),
        ] {
            let mut b = header.clone();
            put(&mut b, &[UNIFORM, raw, packed]);
            b.extend(payload);
            put(&mut b, &[END, 0, 0]);
            assert!(parse(&b).is_none());
        }
        let packed = miniz_oxide::deflate::compress_to_vec_zlib(&vec![0; 65536], 3);
        let mut b = header.clone();
        put(&mut b, &[UNIFORM, 8, packed.len() as u32]);
        b.extend(packed);
        put(&mut b, &[END, 0, 0]);
        assert!(parse(&b).is_none());
        let mut b = header;
        for _ in 0..100001 {
            put(&mut b, &[UNIFORM, 8, 0, 0, 0]);
        }
        put(&mut b, &[END, 0, 0]);
        assert!(parse(&b).is_none());
    }

    #[test]
    fn record_append_is_atomic_and_only_draws_allow_legacy_padding() {
        let mut mesh = Vec::new();
        put(&mut mesh, &[7, Pass::Model as u32, 1, 1]);
        mesh.resize(16 + Pass::Model.stride() as usize + 4, 0);
        let mut texture = Vec::new();
        put(&mut texture, &[7, 1, 1, 1]);
        texture.extend([0; 4]);
        let mut instances = Vec::new();
        put(&mut instances, &[7, 1, crate::pass::INSTANCE_STRIDE as u32]);
        instances.resize(12 + crate::pass::INSTANCE_STRIDE as usize, 0);
        let mut draw = vec![0; 60];
        draw[..4].copy_from_slice(&SKIP.to_le_bytes());
        let fixture = include_bytes!("../tests/fixtures/typed.orf");
        let typed = fixture[44..fixture.len() - 12].to_vec();
        for (kind, payload) in [
            (MESH, mesh),
            (TEXTURE, texture),
            (UNIFORM, vec![0; 8]),
            (INSTANCES, instances),
            (DRAW, draw),
            (TYPED_TEXTURE, typed),
        ] {
            let mut f = Frame::default();
            append_record(kind, &payload, &mut f).unwrap();
            let original = write(&f);
            for len in 0..payload.len() {
                assert!(
                    append_record(kind, &payload[..len], &mut f).is_none(),
                    "kind {kind} prefix {len}"
                );
                assert_eq!(write(&f), original);
            }
            for extra in 1..=8 {
                let mut padded = payload.clone();
                padded.resize(payload.len() + extra, 0);
                if kind == DRAW && extra == 4 {
                    append_record(kind, &padded, &mut Frame::default()).unwrap();
                    padded[payload.len()] = 1;
                }
                assert!(
                    append_record(kind, &padded, &mut f).is_none(),
                    "kind {kind} tail {extra}"
                );
                assert_eq!(write(&f), original);
            }
        }
    }

    #[test]
    fn record_shapes_enforce_uniform_instance_mesh_and_draw_limits() {
        let check = |kind, payload: &[u8], valid| {
            assert_eq!(
                append_record(kind, payload, &mut Frame::default()).is_some(),
                valid
            )
        };
        let mut uniform = Vec::new();
        put(&mut uniform, &[7, 65536]);
        uniform.resize(8 + 65536, 0);
        check(UNIFORM, &uniform, true);
        uniform[4..8].copy_from_slice(&65537u32.to_le_bytes());
        uniform.push(0);
        check(UNIFORM, &uniform, false);
        let mut instances = Vec::new();
        put(&mut instances, &[7, u32::MAX, u32::MAX.wrapping_mul(80)]);
        check(INSTANCES, &instances, false);
        let mut mesh = Vec::new();
        put(&mut mesh, &[7, Pass::Model as u32, 1, 1]);
        mesh.resize(16 + Pass::Model.stride() as usize, 0);
        mesh.extend(1u32.to_le_bytes());
        check(MESH, &mesh, false);
        mesh[8..12].copy_from_slice(&u32::MAX.to_le_bytes());
        check(MESH, &mesh, false);
        for (textures, name, valid) in [(16, 1024, true), (17, 1024, false), (16, 1025, false)] {
            let mut draw = vec![0; 60 + textures * 4];
            draw[..4].copy_from_slice(&SKIP.to_le_bytes());
            draw[12..16].copy_from_slice(&(textures as u32).to_le_bytes());
            draw[56 + textures * 4..60 + textures * 4]
                .copy_from_slice(&(name as u32).to_le_bytes());
            draw.resize(draw.len() + name, b'x');
            check(DRAW, &draw, valid);
            draw[..4].copy_from_slice(&254u32.to_le_bytes());
            check(DRAW, &draw, false);
        }
    }

    #[test]
    fn compressed_records_require_exact_output_checksum_and_input_consumption() {
        let payload = vec![7; 65536];
        let packed = miniz_oxide::deflate::compress_to_vec_zlib(&payload, 3);
        assert_eq!(inflate_record(&packed, payload.len()).unwrap(), payload);
        assert!(inflate_record(&packed, payload.len() - 1).is_none());
        assert!(inflate_record(&packed, payload.len() + 1).is_none());
        assert!(inflate_record(&packed, MAX_RECORD + 1).is_none());
        for len in 0..packed.len() {
            assert!(inflate_record(&packed[..len], payload.len()).is_none());
        }
        let mut corrupt = packed.clone();
        *corrupt.last_mut().unwrap() ^= 1;
        assert!(inflate_record(&corrupt, payload.len()).is_none());
        for tail in [vec![0], packed.clone()] {
            let mut extra = packed.clone();
            extra.extend(tail);
            assert!(inflate_record(&extra, payload.len()).is_none());
        }
    }

    #[test]
    fn typed_fixture_preserves_half_bits_and_requires_orf4() {
        let bytes = include_bytes!("../tests/fixtures/typed.orf");
        let f = parse(bytes).unwrap();
        let t = &f.textures[0];
        assert_eq!(
            (t.id, t.width, t.height, t.layers, t.format, t.sampler),
            (7, 2, 1, 1, TextureFormat::Rgba16Float, 9)
        );
        assert_eq!(
            t.rgba,
            [0x4400u16, 0xb800, 0, 0x3c00]
                .repeat(2)
                .into_iter()
                .flat_map(u16::to_le_bytes)
                .collect::<Vec<_>>()
        );
        let round_trip = parse(&write(&f)).unwrap();
        assert_eq!(round_trip.textures[0].format, t.format);
        assert_eq!(round_trip.textures[0].rgba, t.rgba);
        let mut old = bytes.to_vec();
        old[..4].copy_from_slice(MAGIC);
        assert!(parse(&old).is_none());
        for (offset, value) in [(60, 2u32), (64, 15), (68, 256), (56, 6), (48, u32::MAX)] {
            let mut bad = bytes.to_vec();
            bad[offset..offset + 4].copy_from_slice(&value.to_le_bytes());
            assert!(parse(&bad).is_none(), "accepted offset {offset}");
        }
    }

    #[test]
    fn bloom_record_round_trips_in_orf5_and_older_streams_have_none() {
        let typed = parse(include_bytes!("../tests/fixtures/typed.orf")).unwrap();
        assert!(typed.bloom.is_none());
        assert_eq!(&write(&typed)[..4], MAGIC_V4);
        let mut f = typed.clone();
        let p = BloomParams {
            white_point: 0.75,
            bloom: 2.5,
            brightpass: 0.125,
        };
        f.bloom = Some(p);
        let bytes = write(&f);
        assert_eq!(&bytes[..4], MAGIC_V5);
        let back = parse(&bytes).unwrap();
        assert_eq!(back.bloom, Some(p));
        assert_eq!(back.textures[0].rgba, typed.textures[0].rgba);
        let plain = Frame {
            bloom: Some(BloomParams::DEFAULT),
            ..Frame::default()
        };
        assert_eq!(
            parse(&write(&plain)).unwrap().bloom,
            Some(BloomParams::DEFAULT)
        );
    }

    #[test]
    fn bloom_record_is_rejected_by_older_magics_and_when_malformed() {
        let f = Frame {
            bloom: Some(BloomParams::DEFAULT),
            ..Frame::default()
        };
        let bytes = write(&f);
        for magic in [MAGIC, MAGIC_V4] {
            let mut old = bytes.clone();
            old[..4].copy_from_slice(magic);
            assert!(parse(&old).is_none());
        }
        let record = |values: &[f32]| {
            let mut b = bytes[..32].to_vec();
            put(&mut b, &[BLOOM, values.len() as u32 * 4, 0]);
            values.iter().for_each(|v| b.extend(v.to_le_bytes()));
            put(&mut b, &[END, 0, 0]);
            parse(&b)
        };
        assert!(record(&[1., 1., 0.25]).is_some());
        assert!(record(&[1., 0., 0.25]).is_none());
        assert!(record(&[1., -1., 0.25]).is_none());
        assert!(record(&[f32::NAN, 1., 0.25]).is_none());
        assert!(record(&[1., f32::INFINITY, 0.25]).is_none());
        assert!(record(&[1., 1.]).is_none());
        assert!(record(&[1., 1., 0.25, 0.]).is_none());
        let mut twice = bytes[..32].to_vec();
        for _ in 0..2 {
            put(&mut twice, &[BLOOM, 12, 0]);
            [1f32, 1., 0.25]
                .iter()
                .for_each(|v| twice.extend(v.to_le_bytes()));
        }
        put(&mut twice, &[END, 0, 0]);
        assert!(parse(&twice).is_none());
    }

    #[test]
    fn normalized_legacy_versions_keep_their_type_and_bytes() {
        for version in [MAGIC_V1, MAGIC_V2] {
            let mut b = version.to_vec();
            put(&mut b, &[0, 2, 1]);
            b.extend([0; 16]);
            put(&mut b, &[0, 1, 7, 1, 1, 1]);
            b.extend([0, 127, 255, 255]);
            put(&mut b, &[0, 0]);
            if version == MAGIC_V2 {
                put(&mut b, &[0]);
            }
            let f = parse(&b).unwrap();
            assert_eq!(f.textures[0].format, TextureFormat::Rgba8);
            assert_eq!(f.textures[0].rgba, [0, 127, 255, 255]);
            assert_eq!(&write(&f)[..4], MAGIC);
            let mut compressed = b[..32].to_vec();
            compressed[4..8].copy_from_slice(&DEFLATE.to_le_bytes());
            compressed.extend(miniz_oxide::deflate::compress_to_vec_zlib(&b[32..], 3));
            assert_eq!(
                parse(&compressed).unwrap().textures[0].rgba,
                f.textures[0].rgba
            );
        }
    }
}
