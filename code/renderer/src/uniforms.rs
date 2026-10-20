use bytemuck::{Pod, Zeroable};

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct ModelUniform {
    pub mvp: [f32; 16],
    pub mv: [f32; 16],
    pub normal: [[f32; 4]; 3],
    pub sun_dir: [f32; 3],
    pub pad0: f32,
    pub sun_color: [f32; 3],
    pub ambient: f32,
    pub contrast: f32,
    pub fog_start: f32,
    pub fog_end: f32,
    pub fog_enabled: u32,
    pub pre_lit: u32,
    pub texture_enabled: u32,
    pub pad1: [u32; 2],
    pub fog_color: [f32; 4],
    pub alpha_threshold: f32,
    pub pad2: [f32; 3],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct GroundUniform {
    pub mvp: [f32; 16],
    pub mv: [f32; 16],
    pub normal: [[f32; 4]; 3],
    pub sun_dir: [f32; 3],
    pub pad0: f32,
    pub sun_color: [f32; 3],
    pub ambient: f32,
    pub contrast: f32,
    pub fog_start: f32,
    pub fog_end: f32,
    pub fog_enabled: u32,
    pub shadow_matrix: [f32; 16],
    pub shadow_enabled: u32,
    pub point_count: i32,
    pub pad1: [u32; 2],
    pub point_pos: [[f32; 4]; 128],
    pub point_color: [[f32; 4]; 128],
    pub ground_shadow_enabled: u32,
    pub ground_shadow_scale: f32,
    pub pad2: [u32; 2],
    pub fog_color: [f32; 4],
    pub shadow_bias: f32,
    pub shadow_intensity: f32,
    pub shadow_map_size: [f32; 2],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct SpriteUniform {
    pub projection: [f32; 16],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct ShadowUniform {
    pub light_vp: [f32; 16],
    pub model: [f32; 16],
    pub alpha_test: u32,
    pub alpha_threshold: f32,
    pub pad: [f32; 2],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct ParticleUniform {
    pub vp: [f32; 16],
    pub scroll: [f32; 2],
    pub pad0: [f32; 2],
    pub ambient: [f32; 3],
    pub fog_enabled: u32,
    pub fog_start: f32,
    pub fog_end: f32,
    pub texture_enabled: u32,
    pub combine_mode: i32,
    pub fog_color: [f32; 4],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct HdWaterUniform {
    pub vp: [f32; 16],
    pub has_wave_noise: u32,
    pub wave_scale: f32,
    pub time: f32,
    pub pad0: f32,
    pub flow: [f32; 2],
    pub wave_amplitude: f32,
    pub has_water_depth: u32,
    pub use_planar: u32,
    pub planar_distort: f32,
    pub has_foam_noise: u32,
    pub has_env_map: u32,
    pub shadow_enabled: u32,
    pub pad1: [u32; 3],
    pub inv_vp: [f32; 16],
    pub viewport: [f32; 4],
    pub framebuffer_size: [f32; 2],
    pub pad2: [f32; 2],
    pub sun_dir: [f32; 3],
    pub pad3: f32,
    pub sun_color: [f32; 3],
    pub ambient: f32,
    pub sky_color: [f32; 3],
    pub pad4: f32,
    pub water_color: [f32; 3],
    pub absorb_distance: f32,
    pub scatter: f32,
    pub refraction: f32,
    pub wave_strength: f32,
    pub slow_time: f32,
    pub shadow_scale: f32,
    pub reflection_steps: i32,
    pub fog_enabled: u32,
    pub fog_start: f32,
    pub fog_end: f32,
    pub point_count: i32,
    pub pad5: [u32; 2],
    pub point_pos: [[f32; 4]; 128],
    pub point_color: [[f32; 4]; 128],
}

#[repr(C)]
#[derive(Clone, Copy, Pod, Zeroable)]
pub struct MaterialUniform {
    pub mvp: [f32; 16],
    pub mv: [f32; 16],
    pub normal: [[f32; 4]; 3],
    pub sun_dir: [f32; 3],
    pub pad0: f32,
    pub sun_color: [f32; 3],
    pub ambient: f32,
    pub contrast: f32,
    pub pre_lit: u32,
    pub fog_start: f32,
    pub fog_end: f32,
    pub fog_enabled: u32,
    pub texture_enabled: u32,
    pub pad1: [u32; 2],
    pub fog_color: [f32; 4],
    pub alpha_threshold: f32,
    pub specular_level: i32,
    pub effect_enabled: u32,
    pub limited_texture_units: u32,
}
