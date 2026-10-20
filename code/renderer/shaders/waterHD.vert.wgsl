struct P {
    uModelView: mat4x4<f32>,
    uProjection: mat4x4<f32>,
    uTextureMatrix: mat4x4<f32>,
    uNormalMatrix: mat3x3<f32>,
    uTime: f32,
    uScale: f32,
    uFogStart: f32,
    uFogEnd: f32,
    uSunDir: vec3<f32>,
    uSunColour: vec4<f32>,
    uSunExponent: f32,
    uBreakWaterDepth: f32,
    uBreakWaterOffset: f32,
    uUseWaveEffect_u: u32,
    uWaveIntensity: vec2<f32>,
    uWaveExponent: f32,
    uFogColor: vec4<f32>,
}

struct VertexOutput {
    @location(0) vWorldViewNormal: vec3<f32>,
    @location(1) vWaterDepth: f32,
    @location(2) vTexCoord: vec3<f32>,
    @location(3) vFogFactor: f32,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uNormalSampler: texture_3d<f32>;
@group(1) @binding(1) 
var uNormalSampler_s: sampler;
@group(1) @binding(2) 
var uEnvMapSampler: texture_cube<f32>;
@group(1) @binding(3) 
var uEnvMapSampler_s: sampler;
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aTexCoord_1: vec3<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vWorldViewNormal: vec3<f32>;
var<private> vWaterDepth: f32;
var<private> vTexCoord: vec3<f32>;
var<private> vFogFactor: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var viewPos: vec4<f32>;
    var tc4_: vec4<f32>;
    var fogScale: f32;

    let _e46 = global.uModelView;
    let _e47 = aPosition_1;
    viewPos = (_e46 * vec4<f32>(_e47.x, _e47.y, _e47.z, 1f));
    let _e55 = aTexCoord_1;
    vWaterDepth = _e55.z;
    let _e60 = global.uNormalMatrix[0];
    let _e61 = viewPos;
    vWorldViewNormal.x = dot(_e60, _e61.xyz);
    let _e67 = global.uNormalMatrix[1];
    let _e68 = viewPos;
    vWorldViewNormal.y = dot(_e67, _e68.xyz);
    let _e74 = global.uNormalMatrix[2];
    let _e75 = viewPos;
    vWorldViewNormal.z = dot(_e74, _e75.xyz);
    let _e78 = aTexCoord_1;
    tc4_ = vec4<f32>(_e78.x, _e78.y, _e78.z, 1f);
    let _e88 = global.uTextureMatrix[0];
    let _e89 = tc4_;
    let _e91 = global.uScale;
    vTexCoord.x = (dot(_e88, _e89) * _e91);
    let _e96 = global.uTextureMatrix[1];
    let _e97 = tc4_;
    let _e99 = global.uScale;
    vTexCoord.y = (dot(_e96, _e97) * _e99);
    let _e102 = global.uTime;
    vTexCoord.z = _e102;
    let _e104 = global.uFogEnd;
    let _e105 = global.uFogStart;
    fogScale = (1f / (_e104 - _e105));
    let _e109 = viewPos;
    let _e111 = global.uFogStart;
    let _e113 = fogScale;
    vFogFactor = clamp(((_e109.z - _e111) * _e113), 0f, 1f);
    let _e119 = global.uProjection;
    let _e120 = viewPos;
    gl_Position = (_e119 * _e120);
    return;
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aNormal: vec3<f32>, @location(2) aTexCoord: vec3<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aNormal_1 = aNormal;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    main_1();
    let _e67 = vWorldViewNormal;
    let _e69 = vWaterDepth;
    let _e71 = vTexCoord;
    let _e73 = vFogFactor;
    let _e75 = gl_Position;
    return VertexOutput(_e67, _e69, _e71, _e73, _e75);
}
