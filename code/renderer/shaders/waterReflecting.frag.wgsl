diagnostic(off, derivative_uniformity);

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

struct FragmentOutput {
    @location(0) fragColor: vec4<f32>,
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
var<private> vColor_1: vec4<f32>;
var<private> vTexCoord0_1: vec2<f32>;
var<private> vReflectDir_1: vec3<f32>;
var<private> vFogCoord_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var baseColor: vec4<f32>;
    var texColor: vec4<f32>;
    var cubeMapColor: vec4<f32>;
    var blendFactor: f32;
    var fogDist: f32;
    var fogFactor: f32;

    let _e30 = global.uHasBaseTexture_u;
    if (_e30 != 0u) {
        {
            let _e33 = vTexCoord0_1;
            let _e34 = textureSample(uBaseTexture, uBaseTexture_s, _e33);
            texColor = _e34;
            let _e36 = texColor;
            let _e37 = vColor_1;
            baseColor = (_e36 * _e37);
        }
    } else {
        {
            let _e39 = vColor_1;
            baseColor = _e39;
        }
    }
    let _e40 = global.uReflectionEnabled_u;
    if (_e40 != 0u) {
        {
            let _e43 = vReflectDir_1;
            let _e44 = textureSample(uCubeMap, uCubeMap_s, _e43);
            cubeMapColor = _e44;
            let _e46 = baseColor;
            blendFactor = _e46.w;
            let _e49 = fragColor;
            let _e51 = baseColor;
            let _e53 = cubeMapColor;
            let _e55 = blendFactor;
            let _e57 = mix(_e51.xyz, _e53.xyz, vec3(_e55));
            fragColor.x = _e57.x;
            fragColor.y = _e57.y;
            fragColor.z = _e57.z;
            let _e65 = vColor_1;
            fragColor.w = _e65.w;
        }
    } else {
        {
            let _e67 = baseColor;
            fragColor = _e67;
        }
    }
    let _e68 = global.uFogEnabled_u;
    if (_e68 != 0u) {
        {
            let _e71 = vFogCoord_1;
            fogDist = abs(_e71);
            let _e74 = global.uFogEnd;
            let _e75 = fogDist;
            let _e77 = global.uFogEnd;
            let _e78 = global.uFogStart;
            fogFactor = clamp(((_e74 - _e75) / (_e77 - _e78)), 0f, 1f);
            let _e85 = fragColor;
            let _e87 = global.uFogColor;
            let _e89 = fragColor;
            let _e91 = fogFactor;
            let _e93 = mix(_e87.xyz, _e89.xyz, vec3(_e91));
            fragColor.x = _e93.x;
            fragColor.y = _e93.y;
            fragColor.z = _e93.z;
            return;
        }
    } else {
        return;
    }
}

@fragment 
fn main(@location(0) vColor: vec4<f32>, @location(1) vTexCoord0_: vec2<f32>, @location(2) vReflectDir: vec3<f32>, @location(3) vFogCoord: f32) -> FragmentOutput {
    vColor_1 = vColor;
    vTexCoord0_1 = vTexCoord0_;
    vReflectDir_1 = vReflectDir;
    vFogCoord_1 = vFogCoord;
    main_1();
    let _e47 = fragColor;
    return FragmentOutput(_e47);
}
