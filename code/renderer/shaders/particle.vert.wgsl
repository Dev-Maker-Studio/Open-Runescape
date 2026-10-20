struct P {
    uViewProjection: mat4x4<f32>,
    uTextureScroll: vec2<f32>,
    uAmbient: vec3<f32>,
    uFogEnabled_u: u32,
    uFogStart: f32,
    uFogEnd: f32,
    uTextureEnabled_u: u32,
    uCombineMode: i32,
    uFogColor: vec4<f32>,
}

struct VertexOutput {
    @location(0) vTexCoord: vec2<f32>,
    @location(1) vColor: vec4<f32>,
    @location(2) vFogFactor: f32,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uTexture: texture_2d<f32>;
@group(1) @binding(1) 
var uTexture_s: sampler;
var<private> aPosition_1: vec3<f32>;
var<private> aTexCoord_1: vec2<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vTexCoord: vec2<f32>;
var<private> vColor: vec4<f32>;
var<private> vFogFactor: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var dist: f32;

    let _e27 = global.uViewProjection;
    let _e28 = aPosition_1;
    gl_Position = (_e27 * vec4<f32>(_e28.x, _e28.y, _e28.z, 1f));
    let _e35 = aTexCoord_1;
    let _e36 = global.uTextureScroll;
    vTexCoord = (_e35 + _e36);
    let _e38 = aColor_1;
    let _e40 = global.uAmbient;
    let _e46 = clamp((_e38.xyz * _e40), vec3(0f), vec3(1f));
    let _e47 = aColor_1;
    vColor = vec4<f32>(_e46.x, _e46.y, _e46.z, _e47.w);
    let _e53 = global.uFogEnabled_u;
    if (_e53 != 0u) {
        {
            let _e56 = gl_Position;
            dist = _e56.w;
            let _e59 = global.uFogEnd;
            let _e60 = dist;
            let _e62 = global.uFogEnd;
            let _e63 = global.uFogStart;
            vFogFactor = clamp(((_e59 - _e60) / (_e62 - _e63)), 0f, 1f);
            return;
        }
    } else {
        {
            vFogFactor = 1f;
            return;
        }
    }
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(2) aTexCoord: vec2<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    main_1();
    let _e41 = vTexCoord;
    let _e43 = vColor;
    let _e45 = vFogFactor;
    let _e47 = gl_Position;
    return VertexOutput(_e41, _e43, _e45, _e47);
}
