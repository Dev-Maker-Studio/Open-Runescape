struct P {
    uModelViewProjection: mat4x4<f32>,
    uModelView: mat4x4<f32>,
    uNormalMatrix: mat3x3<f32>,
    uViewRotation: mat3x3<f32>,
    uHasBaseTexture_u: u32,
    uReflectionEnabled_u: u32,
    uFogStart: f32,
    uFogEnd: f32,
    uFogColor: vec4<f32>,
    uFogEnabled_u: u32,
}

struct VertexOutput {
    @location(0) vColor: vec4<f32>,
    @location(1) vTexCoord0_: vec2<f32>,
    @location(2) vReflectDir: vec3<f32>,
    @location(3) vFogCoord: f32,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uBaseTexture: texture_2d<f32>;
@group(1) @binding(1) 
var uBaseTexture_s: sampler;
@group(1) @binding(2) 
var uCubeMap: texture_cube<f32>;
@group(1) @binding(3) 
var uCubeMap_s: sampler;
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aTexCoord_1: vec2<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vColor: vec4<f32>;
var<private> vTexCoord0_: vec2<f32>;
var<private> vReflectDir: vec3<f32>;
var<private> vFogCoord: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var viewPos4_: vec4<f32>;
    var viewPos: vec3<f32>;
    var viewNormal: vec3<f32>;
    var eyeDir: vec3<f32>;
    var reflectVec: vec3<f32>;

    let _e33 = global.uModelViewProjection;
    let _e34 = aPosition_1;
    gl_Position = (_e33 * vec4<f32>(_e34.x, _e34.y, _e34.z, 1f));
    let _e41 = global.uModelView;
    let _e42 = aPosition_1;
    viewPos4_ = (_e41 * vec4<f32>(_e42.x, _e42.y, _e42.z, 1f));
    let _e50 = viewPos4_;
    viewPos = _e50.xyz;
    let _e53 = viewPos;
    vFogCoord = _e53.z;
    let _e55 = aTexCoord_1;
    vTexCoord0_ = _e55;
    let _e56 = aColor_1;
    vColor = _e56;
    let _e57 = global.uNormalMatrix;
    let _e58 = aNormal_1;
    viewNormal = normalize((_e57 * _e58));
    let _e62 = viewPos;
    eyeDir = normalize(_e62);
    let _e65 = eyeDir;
    let _e66 = viewNormal;
    reflectVec = reflect(_e65, _e66);
    let _e69 = global.uViewRotation;
    let _e70 = reflectVec;
    vReflectDir = (_e69 * _e70);
    return;
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aNormal: vec3<f32>, @location(2) aTexCoord: vec2<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aNormal_1 = aNormal;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    main_1();
    let _e53 = vColor;
    let _e55 = vTexCoord0_;
    let _e57 = vReflectDir;
    let _e59 = vFogCoord;
    let _e61 = gl_Position;
    return VertexOutput(_e53, _e55, _e57, _e59, _e61);
}
