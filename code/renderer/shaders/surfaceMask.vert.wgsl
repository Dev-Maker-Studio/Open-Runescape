struct P {
    uProjection: mat4x4<f32>,
}

struct VertexOutput {
    @location(0) vBaseUV: vec2<f32>,
    @location(1) vMaskUV: vec2<f32>,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0)
var<uniform> global: P;

@vertex
fn main(@location(0) aPosition: vec2<f32>, @location(1) aBaseUV: vec2<f32>, @location(2) aMaskUV: vec2<f32>) -> VertexOutput {
    return VertexOutput(aBaseUV, aMaskUV, (global.uProjection * vec4<f32>(aPosition.x, aPosition.y, 0f, 1f)));
}
