struct P {
    uViewProjection: mat4x4<f32>,
    uClipPlane: vec4<f32>,
    uViewMatrix: mat4x4<f32>,
    uSunDirection: vec3<f32>,
    uSunBrightness: f32,
    uSunColor: vec3<f32>,
    uContrastPos: f32,
    uContrastNeg: f32,
    uFogStart: f32,
    uFogEnd: f32,
    uFogEnabled: u32,
    uFogColor2: vec3<f32>,
    uAlphaThreshold: f32,
    uTextureTime: f32,
    uMaterialFlags: u32,
    uTextureSpeed: vec2<f32>,
    uPointLightPos: array<vec4<f32>, 128>,
    uPointLightColor: array<vec4<f32>, 128>,
}

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) color: vec4<f32>,
    @location(2) fog: f32,
    @location(3) clip: f32,
}

@group(0) @binding(0)
var<uniform> g: P;
@group(1) @binding(0)
var uTexture: texture_2d<f32>;
@group(1) @binding(1)
var uTexture_s: sampler;

@fragment
fn main(in: VertexOutput) -> @location(0) vec4<f32> {
    let scroll = g.uTextureTime * g.uTextureSpeed / 64.0;
    let t = textureSample(uTexture, uTexture_s, in.uv + scroll - trunc(scroll));
    if (in.clip < 0.0) {
        discard;
    }
    var color = in.color;
    let flags = g.uMaterialFlags;
    if ((flags & 1u) != 0u) {
        let mode = (flags >> 1u) & 7u;
        if (mode == 1u) {
            color = t;
        } else if (mode == 3u) {
            color = vec4<f32>(clamp(t.rgb + in.color.rgb, vec3<f32>(0.0), vec3<f32>(1.0)), t.a * in.color.a);
        } else if (mode == 4u) {
            color = clamp(t - in.color, vec4<f32>(0.0), vec4<f32>(1.0));
        } else {
            color = t * in.color;
        }
        if ((flags & 16u) == 0u) {
            color.a = in.color.a;
        }
    }
    if (color.a <= g.uAlphaThreshold) {
        discard;
    }
    return vec4<f32>(mix(g.uFogColor2, color.rgb, in.fog), color.a);
}
