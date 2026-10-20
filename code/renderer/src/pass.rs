use bytemuck::{Pod, Zeroable};
use wgpu::{TextureViewDimension as D, VertexAttribute, VertexFormat as F};

#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
pub enum Pass {
    Model = 0,
    Ground = 1,
    Sprite = 2,
    Terrain = 3,
    Instanced = 4,
    Shadow = 5,
    Particle = 6,
    HdWater = 7,
    Material = 8,
    Water = 9,
    WaterHD = 10,
    WaterLapping = 11,
    WaterReflecting = 12,
    WaterRippling = 13,
    Waterfall = 14,
    SurfaceMask = 15,
}

use Pass::*;

pub const ALL: [Pass; 16] =
    [Model, Ground, Sprite, Terrain, Instanced, Shadow, Particle, HdWater, Material, Water, WaterHD, WaterLapping, WaterReflecting, WaterRippling, Waterfall, SurfaceMask];

const SHADERS: [&str; 16] = [
    "model", "ground", "sprite", "groundTextured", "instanced", "shadow", "particle", "hdWater", "materialCubeSpec", "water", "waterHD",
    "waterLapping", "waterReflecting", "waterRippling", "waterfall", "surfaceMask",
];

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Bind {
    Flat(usize),
    Clamp(usize),
    Layered(usize),
    Volume(usize),
    Cube(usize),
    Depth(usize),
    Reflection(usize),
    SceneColor,
    SceneDepth,
}

pub const NONE: usize = usize::MAX;

impl Bind {
    pub fn dimension(self) -> D {
        match self {
            Bind::Layered(_) => D::D2Array,
            Bind::Volume(_) => D::D3,
            Bind::Cube(_) => D::Cube,
            _ => D::D2,
        }
    }

    pub fn filterable(self) -> bool {
        !matches!(self, Bind::Depth(_) | Bind::SceneDepth)
    }

    pub fn source(self) -> Option<usize> {
        match self {
            Bind::Flat(k) | Bind::Clamp(k) | Bind::Layered(k) | Bind::Volume(k) | Bind::Cube(k) | Bind::Depth(k) | Bind::Reflection(k) => Some(k),
            _ => None,
        }
    }
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct ModelVertex {
    pub p: [f32; 3],
    pub n: [f32; 3],
    pub uv: [f32; 2],
    pub c: [u8; 4],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct GroundVertex {
    pub p: [f32; 3],
    pub n: [f32; 3],
    pub c: [u8; 4],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct SpriteVertex {
    pub p: [f32; 2],
    pub uv: [f32; 2],
    pub c: [u8; 4],
    pub tex: f32,
    pub clip: [f32; 4],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct MaskVertex {
    pub p: [f32; 2],
    pub base: [f32; 2],
    pub mask: [f32; 2],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct ParticleVertex {
    pub p: [f32; 3],
    pub uv: [f32; 2],
    pub c: [u8; 4],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct WaterVertex {
    pub p: [f32; 3],
    pub c: [u8; 4],
    pub uv: [f32; 3],
}

const fn attr(format: F, offset: u64, shader_location: u32) -> VertexAttribute {
    VertexAttribute { format, offset, shader_location }
}

const MODEL: [VertexAttribute; 4] = [attr(F::Float32x3, 0, 0), attr(F::Float32x3, 12, 1), attr(F::Float32x2, 24, 2), attr(F::Unorm8x4, 32, 3)];
const GROUND: [VertexAttribute; 3] = [attr(F::Float32x3, 0, 0), attr(F::Float32x3, 12, 1), attr(F::Unorm8x4, 24, 3)];
const SPRITE: [VertexAttribute; 5] =
    [attr(F::Float32x2, 0, 0), attr(F::Float32x2, 8, 2), attr(F::Unorm8x4, 16, 3), attr(F::Float32, 20, 4), attr(F::Float32x4, 24, 5)];
const TERRAIN: [VertexAttribute; 4] = [attr(F::Float32x3, 0, 0), attr(F::Unorm8x4, 12, 1), attr(F::Float32x3, 16, 2), attr(F::Float32x3, 28, 3)];
const PARTICLE: [VertexAttribute; 3] = [attr(F::Float32x3, 0, 0), attr(F::Float32x2, 12, 2), attr(F::Unorm8x4, 20, 3)];
const HDWATER: [VertexAttribute; 3] = [attr(F::Float32x3, 0, 0), attr(F::Unorm8x4, 12, 1), attr(F::Float32x3, 16, 2)];
const MASK: [VertexAttribute; 3] = [attr(F::Float32x2, 0, 0), attr(F::Float32x2, 8, 1), attr(F::Float32x2, 16, 2)];
const INSTANCE: [VertexAttribute; 5] =
    [attr(F::Float32x4, 0, 5), attr(F::Float32x4, 16, 6), attr(F::Float32x4, 32, 7), attr(F::Float32x4, 48, 8), attr(F::Float32x4, 64, 9)];

pub const INSTANCE_STRIDE: u64 = 80;

const DEPTH_TEST: u32 = 1;
const DEPTH_WRITE: u32 = 2;
const ALPHA: u32 = 1 << 2;
const CULL_FRONT: u32 = 2 << 4;
const LESS: u32 = 1 << 7;
const LEQUAL: u32 = 3 << 7;
const ALWAYS: u32 = 7 << 7;

impl Pass {
    pub fn from_u32(v: u32) -> Option<Pass> {
        ALL.get(v as usize).copied()
    }

    pub fn from_shader(name: &str) -> Option<Pass> {
        SHADERS.iter().position(|s| *s == name).map(|i| ALL[i])
    }

    pub fn shader(self) -> &'static str {
        SHADERS[self as usize]
    }

    pub fn stride(self) -> u64 {
        match self {
            Ground => size_of::<GroundVertex>(),
            Sprite | Terrain => 40,
            Particle => size_of::<ParticleVertex>(),
            HdWater => size_of::<WaterVertex>(),
            SurfaceMask => size_of::<MaskVertex>(),
            _ => size_of::<ModelVertex>(),
        }
        .try_into()
        .unwrap()
    }

    pub fn attributes(self) -> &'static [VertexAttribute] {
        match self {
            Ground => &GROUND,
            Sprite => &SPRITE,
            Terrain => &TERRAIN,
            Particle => &PARTICLE,
            HdWater => &HDWATER,
            SurfaceMask => &MASK,
            _ => &MODEL,
        }
    }

    pub fn instance_attributes(self) -> Option<&'static [VertexAttribute]> {
        (self == Instanced).then_some(&INSTANCE[..])
    }

    pub fn bindings(self) -> &'static [Bind] {
        use Bind::*;
        match self {
            Model | Instanced | Shadow | Particle | Material => &[Flat(0)],
            Ground => &[Clamp(0), Depth(1)],
            Sprite => &[Layered(0)],
            Terrain => &[Volume(2), SceneDepth, Clamp(1), Flat(0)],
            HdWater => &[Volume(0), SceneColor, SceneDepth, Volume(1), Cube(2), Depth(3), Reflection(4)],
            Water | SurfaceMask => &[Flat(0), Flat(1)],
            WaterHD => &[Volume(0), Cube(1)],
            WaterLapping | Waterfall => &[Flat(0), Volume(1), Flat(2)],
            WaterReflecting => &[Flat(0), Cube(1)],
            WaterRippling => &[Flat(0), Volume(1)],
        }
    }

    pub fn block_size(self) -> usize {
        [272, 4448, 64, 5552, 4320, 144, 128, 4432, 272, 384, 336, 1392, 272, 304, 304, 64][self as usize]
    }

    pub fn default_state(self) -> u32 {
        match self {
            Sprite | SurfaceMask => ALPHA | ALWAYS,
            Shadow => DEPTH_TEST | DEPTH_WRITE | CULL_FRONT | LESS,
            Particle => DEPTH_TEST | ALPHA | LESS,
            HdWater => DEPTH_TEST | DEPTH_WRITE | ALPHA | LEQUAL,
            _ => DEPTH_TEST | DEPTH_WRITE | LESS,
        }
    }

    pub fn offscreen(self) -> bool {
        self == Shadow
    }
}
