struct P {
    uModelView: mat4x4<f32>,
    uProjection: mat4x4<f32>,
    uTextureMatrix: mat4x4<f32>,
    uFogParams: vec4<f32>,
    uWaterPlane: vec4<f32>,
    uUseLighting_u: u32,
    uUseDepthPlane_u: u32,
    uLight0Direction: vec3<f32>,
    uLight1Direction: vec3<f32>,
    uLight0Diffuse: vec4<f32>,
    uLight1Diffuse: vec4<f32>,
    uAmbientColor: vec4<f32>,
    uWaterColor: vec3<f32>,
    uHasBaseTexture_u: u32,
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
var uTexture: texture_2d<f32>;
@group(1) @binding(1) 
var uTexture_s: sampler;
@group(1) @binding(2) 
var uEdgeTexture: texture_2d<f32>;
@group(1) @binding(3) 
var uEdgeTexture_s: sampler;
var<private> vColor_1: vec4<f32>;
var<private> vTexCoord0_1: vec2<f32>;
var<private> vBlendFactor_1: vec4<f32>;
var<private> vFogCoord_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var baseColor: vec4<f32>;
    var texColor: vec4<f32>;
    var edgeBlend: f32;
    var blendedColor: vec3<f32>;
    var fogDist: f32;
    var fogFactor: f32;

    let _e46 = global.uHasBaseTexture_u;
    if (_e46 != 0u) {
        {
            let _e49 = vTexCoord0_1;
            let _e50 = textureSample(uTexture, uTexture_s, _e49);
            texColor = _e50;
            let _e52 = texColor;
            let _e53 = vColor_1;
            baseColor = (_e52 * _e53);
        }
    } else {
        {
            let _e55 = vColor_1;
            baseColor = _e55;
        }
    }
    let _e56 = vBlendFactor_1;
    edgeBlend = _e56.x;
    let _e59 = baseColor;
    let _e61 = global.uWaterColor;
    let _e62 = edgeBlend;
    blendedColor = mix(_e59.xyz, _e61, vec3(_e62));
    let _e66 = blendedColor;
    let _e67 = baseColor;
    fragColor = vec4<f32>(_e66.x, _e66.y, _e66.z, _e67.w);
    let _e73 = global.uFogEnabled_u;
    if (_e73 != 0u) {
        {
            let _e76 = vFogCoord_1;
            fogDist = abs(_e76);
            let _e79 = global.uFogEnd;
            let _e80 = fogDist;
            let _e82 = global.uFogEnd;
            let _e83 = global.uFogStart;
            fogFactor = clamp(((_e79 - _e80) / (_e82 - _e83)), 0f, 1f);
            let _e90 = fragColor;
            let _e92 = global.uFogColor;
            let _e94 = fragColor;
            let _e96 = fogFactor;
            let _e98 = mix(_e92.xyz, _e94.xyz, vec3(_e96));
            fragColor.x = _e98.x;
            fragColor.y = _e98.y;
            fragColor.z = _e98.z;
            return;
        }
    } else {
        return;
    }
}

@fragment 
fn main(@location(0) vColor: vec4<f32>, @location(1) vTexCoord0_: vec2<f32>, @location(2) vBlendFactor: vec4<f32>, @location(3) vFogCoord: f32) -> FragmentOutput {
    vColor_1 = vColor;
    vTexCoord0_1 = vTexCoord0_;
    vBlendFactor_1 = vBlendFactor;
    vFogCoord_1 = vFogCoord;
    main_1();
    let _e63 = fragColor;
    return FragmentOutput(_e63);
}
