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

@vertex
fn main(
    @location(0) position: vec3<f32>,
    @location(1) normal: vec3<f32>,
    @location(2) uv: vec2<f32>,
    @location(3) color: vec4<f32>,
    @location(5) c0: vec4<f32>,
    @location(6) c1: vec4<f32>,
    @location(7) c2: vec4<f32>,
    @location(8) c3: vec4<f32>,
    @location(9) lights: vec4<f32>,
) -> VertexOutput {
    let world = mat4x4<f32>(c0, c1, c2, c3) * vec4<f32>(position, 1.0);
    let n = mat3x3<f32>(c0.xyz, c1.xyz, c2.xyz) * normal;
    let d = dot(g.uSunDirection, n);
    var lit = color.rgb * g.uSunColor * (g.uSunBrightness + d * select(g.uContrastPos, g.uContrastNeg, d < 0.0));
    for (var k = 0; k < 4; k++) {
        let i = i32(lights[k]);
        if (i < 0 || i >= 128) {
            continue;
        }
        let to = g.uPointLightPos[i].xyz - world.xyz;
        let att = 1.0 / max(g.uPointLightPos[i].w * dot(to, to), 1e-6);
        lit += color.rgb * g.uPointLightColor[i].xyz * (att * max(dot(n, normalize(to)), 0.0));
    }
    var out: VertexOutput;
    out.position = g.uViewProjection * world;
    out.uv = uv;
    out.color = vec4<f32>(clamp(lit, vec3<f32>(0.0), vec3<f32>(1.0)), color.a);
    out.fog = 1.0;
    if (g.uFogEnabled != 0u) {
        out.fog = clamp((g.uFogEnd - -(g.uViewMatrix * world).z) / max(g.uFogEnd - g.uFogStart, 1e-4), 0.0, 1.0);
    }
    out.clip = dot(world, g.uClipPlane);
    return out;
}
