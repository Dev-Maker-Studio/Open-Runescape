struct P {
    uProjection: mat4x4<f32>,
}

struct FragmentOutput {
    @location(0) fragColor: vec4<f32>,
}

@group(0) @binding(0)
var<uniform> global: P;
@group(1) @binding(0)
var uBase: texture_2d<f32>;
@group(1) @binding(1)
var uBase_s: sampler;
@group(1) @binding(2)
var uMask: texture_2d<f32>;
@group(1) @binding(3)
var uMask_s: sampler;

@fragment
fn main(@location(0) vBaseUV: vec2<f32>, @location(1) vMaskUV: vec2<f32>) -> FragmentOutput {
    let base = textureSample(uBase, uBase_s, vBaseUV);
    let mask = textureSample(uMask, uMask_s, vMaskUV).w;
    return FragmentOutput(vec4<f32>(base.xyz, (base.w * mask)));
}
