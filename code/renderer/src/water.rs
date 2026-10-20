use crate::pass::WaterVertex;

pub const TESS_PIXELS: f32 = 24.;
pub const MAX_LEVEL: usize = 16;
pub const VIEW_PROJECTION: usize = 0;
pub const HAS_WAVE_NOISE: usize = 64;

pub fn waving(uniform: &[u8]) -> bool {
    uniform.get(HAS_WAVE_NOISE..HAS_WAVE_NOISE + 4).is_some_and(|b| b != [0; 4])
}

fn clip(vp: &[f32; 16], p: [f32; 3]) -> [f32; 4] {
    std::array::from_fn(|i| vp[i] * p[0] + vp[4 + i] * p[1] + vp[8 + i] * p[2] + vp[12 + i])
}

fn screen(c: [f32; 4], viewport: [u32; 4]) -> [f32; 2] {
    let w = c[3].max(1.);
    [c[0] / w * 0.5 * viewport[2] as f32, c[1] / w * 0.5 * viewport[3] as f32]
}

fn level(vp: &[f32; 16], a: [f32; 3], b: [f32; 3], viewport: [u32; 4]) -> f32 {
    let (ca, cb) = (clip(vp, a), clip(vp, b));
    if ca[3] <= 0. && cb[3] <= 0. {
        return 1.;
    }
    let (sa, sb) = (screen(ca, viewport), screen(cb, viewport));
    (((sa[0] - sb[0]).powi(2) + (sa[1] - sb[1]).powi(2)).sqrt() / TESS_PIXELS).clamp(1., MAX_LEVEL as f32)
}

fn lerp(v: [&WaterVertex; 3], b: [f32; 3]) -> WaterVertex {
    let mix = |f: &dyn Fn(&WaterVertex) -> f32| f(v[0]) * b[0] + f(v[1]) * b[1] + f(v[2]) * b[2];
    WaterVertex {
        p: std::array::from_fn(|i| mix(&|v| v.p[i])),
        c: std::array::from_fn(|i| mix(&|v| v.c[i] as f32).round() as u8),
        uv: std::array::from_fn(|i| mix(&|v| v.uv[i])),
    }
}

pub fn tessellate(vertices: &[WaterVertex], indices: &[u32], vp: &[f32; 16], viewport: [u32; 4]) -> (Vec<WaterVertex>, Vec<u32>) {
    let (mut out, mut idx) = (Vec::new(), Vec::new());
    for t in indices.chunks_exact(3) {
        let v = [&vertices[t[0] as usize], &vertices[t[1] as usize], &vertices[t[2] as usize]];
        let n = [(1, 2), (2, 0), (0, 1)].map(|(a, b)| level(vp, v[a].p, v[b].p, viewport)).into_iter().fold(1f32, f32::max).ceil() as usize;
        let base = out.len() as u32;
        let row = |i: usize| (i * (2 * n + 3 - i) / 2) as u32;
        for i in 0..=n {
            for j in 0..=n - i {
                let (x, y) = (i as f32 / n as f32, j as f32 / n as f32);
                out.push(lerp(v, [1. - x - y, x, y]));
            }
        }
        for i in 0..n {
            for j in 0..n - i {
                let (a, b, c) = (base + row(i) + j as u32, base + row(i + 1) + j as u32, base + row(i) + j as u32 + 1);
                idx.extend([a, b, c]);
                if j + 1 < n - i {
                    idx.extend([c, b, base + row(i + 1) + j as u32 + 1]);
                }
            }
        }
    }
    (out, idx)
}

pub const CLIP_PLANE: usize = 64;
pub const USE_PLANAR: usize = 96;
pub const SKY_COLOUR: usize = 256;
pub const TERRAIN_WATER_MODE: usize = 4288;
pub const TERRAIN_DEPTH_FOG: usize = 5404;
pub const CLIP_BIAS: f32 = 4.;

pub fn flag(uniform: &[u8], at: usize) -> bool {
    uniform.get(at..at + 4).is_some_and(|b| b != [0; 4])
}

pub fn f32_at(uniform: &[u8], at: usize) -> f32 {
    uniform.get(at..at + 4).map_or(0., |b| f32::from_le_bytes(b.try_into().unwrap()))
}

pub fn plane(vertices: &[WaterVertex], indices: &[u32]) -> f32 {
    let step = (indices.len() / 64).max(1);
    let ys: Vec<f32> = indices.iter().step_by(step).map(|&i| vertices[i as usize].p[1]).collect();
    if ys.is_empty() { f32::NAN } else { ys.iter().sum::<f32>() / ys.len() as f32 }
}

pub fn mirror(uniform: &mut [u8], plane: f32, terrain: bool) {
    let m: &mut [f32; 16] = bytemuck::from_bytes_mut(&mut uniform[..64]);
    for i in 0..4 {
        m[12 + i] += 2. * plane * m[4 + i];
        m[4 + i] = -m[4 + i];
    }
    let clip = [0f32, -1., 0., plane + CLIP_BIAS];
    uniform[CLIP_PLANE..CLIP_PLANE + 16].copy_from_slice(bytemuck::cast_slice(&clip));
    if terrain {
        uniform[TERRAIN_WATER_MODE..TERRAIN_WATER_MODE + 4].fill(0);
    }
}
