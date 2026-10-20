use crate::{math, pass::Pass};
use std::collections::BTreeMap;

pub const MAGIC: &[u8; 4] = b"ORF3";
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

#[derive(Clone)]
pub struct Mesh {
    pub id: u32,
    pub pass: Pass,
    pub vertices: Vec<u8>,
    pub indices: Vec<u32>,
}

#[derive(Clone)]
pub struct Texture {
    pub id: u32,
    pub width: u32,
    pub height: u32,
    pub layers: u32,
    pub rgba: Vec<u8>,
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
        Draw { pass, mesh, uniform, textures, state: pass.default_state(), viewport: [0; 4], scissor: [0; 4], instances: NO_INSTANCES, skip: None }
    }
}

#[derive(Default, Clone)]
pub struct Frame {
    pub width: u32,
    pub height: u32,
    pub clear: [f32; 4],
    pub meshes: Vec<Mesh>,
    pub textures: Vec<Texture>,
    pub uniforms: Vec<Uniform>,
    pub instances: Vec<Instances>,
    pub draws: Vec<Draw>,
}

impl Frame {
    pub fn skipped(&self) -> BTreeMap<&str, u32> {
        let mut out = BTreeMap::new();
        self.draws.iter().filter_map(|d| d.skip.as_deref()).for_each(|n| *out.entry(n).or_insert(0) += 1);
        out
    }

    pub fn summary(&self) -> String {
        let mut drawn = BTreeMap::new();
        self.draws.iter().filter(|d| d.skip.is_none()).for_each(|d| *drawn.entry(d.pass.shader()).or_insert(0u32) += 1);
        let list = |m: Vec<(&str, u32)>| m.iter().map(|(n, c)| format!("{n}={c}")).collect::<Vec<_>>().join(",");
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
        (0..n).map(|_| item(self)).collect()
    }
}

fn mesh(r: &mut Reader) -> Option<Mesh> {
    let (id, pass, vc, ic) = (r.u32()?, Pass::from_u32(r.u32()?)?, r.u32()?, r.u32()?);
    let vertices = r.bytes(vc as usize * pass.stride() as usize)?;
    let indices = bytemuck::pod_collect_to_vec(r.slice(ic as usize * 4)?);
    Some(Mesh { id, pass, vertices, indices })
}

fn texture(r: &mut Reader) -> Option<Texture> {
    let [id, width, height, layers] = r.array()?;
    let rgba = r.bytes(width as usize * height as usize * layers as usize * 4)?;
    Some(Texture { id, width, height, layers, rgba })
}

fn uniform(r: &mut Reader) -> Option<Uniform> {
    let (id, len) = (r.u32()?, r.u32()?);
    Some(Uniform { id, bytes: r.bytes(len as usize)? })
}

fn instances(r: &mut Reader) -> Option<Instances> {
    let (id, count, len) = (r.u32()?, r.u32()?, r.u32()?);
    Some(Instances { id, count, bytes: r.bytes(len as usize)? })
}

fn draw(r: &mut Reader, v1: bool) -> Option<Draw> {
    let (raw, mesh, uniform) = (r.u32()?, r.u32()?, r.u32()?);
    let textures = r.list(|r| r.u32())?;
    if v1 {
        return Some(Draw::new(Pass::from_u32(raw)?, mesh, uniform, textures));
    }
    let (state, viewport, scissor, inst) = (r.u32()?, r.array()?, r.array()?, r.u32()?);
    let len = r.u32()? as usize;
    let name = String::from_utf8_lossy(r.slice(len)?).into_owned();
    let skip = (raw == SKIP).then_some(name);
    let pass = Pass::from_u32(raw).unwrap_or(Pass::Model);
    Some(Draw { pass, mesh, uniform, textures, state, viewport, scissor, instances: inst, skip })
}

fn chunks(r: &mut Reader, f: &mut Frame) -> Option<()> {
    loop {
        let [kind, raw, packed] = r.array()?;
        if kind == END {
            return Some(());
        }
        let payload = if packed == 0 { r.bytes(raw as usize)? } else { miniz_oxide::inflate::decompress_to_vec_zlib(r.slice(packed as usize)?).ok()? };
        if payload.len() != raw as usize {
            return None;
        }
        let mut c = Reader(&payload);
        match kind {
            MESH => f.meshes.push(mesh(&mut c)?),
            TEXTURE => f.textures.push(texture(&mut c)?),
            UNIFORM => f.uniforms.push(uniform(&mut c)?),
            INSTANCES => f.instances.push(instances(&mut c)?),
            DRAW => f.draws.push(draw(&mut c, false)?),
            _ => return None,
        }
    }
}

pub fn parse(data: &[u8]) -> Option<Frame> {
    let mut r = Reader(data);
    let version = match r.slice(4)? {
        m if m == MAGIC => 3,
        m if m == MAGIC_V2 => 2,
        m if m == MAGIC_V1 => 1,
        _ => return None,
    };
    let flags = r.u32()?;
    let (width, height) = (r.u32()?, r.u32()?);
    let clear = [r.f32()?, r.f32()?, r.f32()?, r.f32()?];
    let mut f = Frame { width, height, clear, ..Default::default() };
    if version == 3 {
        chunks(&mut r, &mut f)?;
    } else {
        let inflated;
        if flags & DEFLATE != 0 {
            inflated = miniz_oxide::inflate::decompress_to_vec_zlib(r.0).ok()?;
            r = Reader(&inflated);
        }
        f.meshes = r.list(mesh)?;
        f.textures = r.list(texture)?;
        f.uniforms = r.list(uniform)?;
        f.instances = if version == 2 { r.list(instances)? } else { vec![] };
        f.draws = r.list(|r| draw(r, version == 1))?;
    }
    if flags & GL_DEPTH != 0 {
        for u in f.uniforms.iter_mut() {
            let m: &mut [f32; 16] = bytemuck::try_from_bytes_mut(u.bytes.get_mut(..64)?).ok()?;
            math::gl_to_wgpu_depth(m);
        }
    }
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
    let mut out = MAGIC.to_vec();
    put(&mut out, &[DEFLATE, frame.width, frame.height]);
    frame.clear.iter().for_each(|c| out.extend(c.to_le_bytes()));
    let mut o = vec![];
    for m in &frame.meshes {
        put(&mut o, &[m.id, m.pass as u32, (m.vertices.len() as u64 / m.pass.stride()) as u32, m.indices.len() as u32]);
        o.extend(&m.vertices);
        o.extend(bytemuck::cast_slice::<u32, u8>(&m.indices));
        chunk(&mut out, MESH, &std::mem::take(&mut o));
    }
    for t in &frame.textures {
        put(&mut o, &[t.id, t.width, t.height, t.layers]);
        o.extend(&t.rgba);
        chunk(&mut out, TEXTURE, &std::mem::take(&mut o));
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
        put(&mut o, &[d.skip.as_ref().map_or(d.pass as u32, |_| SKIP), d.mesh, d.uniform, d.textures.len() as u32]);
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
    put(&mut out, &[END, 0, 0]);
    out
}
