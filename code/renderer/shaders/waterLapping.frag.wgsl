struct P {
    uModelView: mat4x4<f32>,
    uProjection: mat4x4<f32>,
    uTextureMatrix: mat4x4<f32>,
    uInverseViewRotation: mat4x4<f32>,
    uNoise: array<vec4<f32>, 64>,
    uTurbulence: f32,
    uAmbientLight: vec3<f32>,
    uTime: f32,
    uUse3DTexture_u: u32,
    uHasOverlay_u: u32,
    uHasBaseTexture_u: u32,
    uOverlayConstColor: vec4<f32>,
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
var uOverlayTexture3D: texture_3d<f32>;
@group(1) @binding(3) 
var uOverlayTexture3D_s: sampler;
@group(1) @binding(4) 
var uOverlayTexture2D: texture_2d<f32>;
@group(1) @binding(5) 
var uOverlayTexture2D_s: sampler;
var<private> vColor_1: vec4<f32>;
var<private> vTexCoord0_1: vec2<f32>;
var<private> vTexCoord1_1: vec3<f32>;
var<private> vFogCoord_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var baseColor: vec4<f32>;
    var texColor: vec4<f32>;
    var overlayColor: vec4<f32>;
    var fogDist: f32;
    var fogFactor: f32;

    let _e44 = global.uHasBaseTexture_u;
    if (_e44 != 0u) {
        {
            let _e47 = vTexCoord0_1;
            let _e48 = textureSample(uBaseTexture, uBaseTexture_s, _e47);
            texColor = _e48;
            let _e50 = texColor;
            let _e51 = vColor_1;
            baseColor = (_e50 * _e51);
        }
    } else {
        {
            let _e53 = vColor_1;
            baseColor = _e53;
        }
    }
    let _e54 = global.uHasOverlay_u;
    if (_e54 != 0u) {
        {
            let _e58 = global.uUse3DTexture_u;
            if (_e58 != 0u) {
                {
                    let _e61 = vTexCoord1_1;
                    let _e62 = textureSample(uOverlayTexture3D, uOverlayTexture3D_s, _e61);
                    overlayColor = _e62;
                }
            } else {
                {
                    let _e63 = vTexCoord1_1;
                    let _e65 = textureSample(uOverlayTexture2D, uOverlayTexture2D_s, _e63.xy);
                    overlayColor = _e65;
                }
            }
            let _e66 = baseColor;
            let _e68 = baseColor;
            let _e70 = overlayColor;
            let _e77 = clamp((_e68.xyz + _e70.xyz), vec3(0f), vec3(1f));
            baseColor.x = _e77.x;
            baseColor.y = _e77.y;
            baseColor.z = _e77.z;
            let _e85 = global.uOverlayConstColor;
            baseColor.w = _e85.w;
        }
    }
    let _e87 = baseColor;
    fragColor = _e87;
    let _e88 = global.uFogEnabled_u;
    if (_e88 != 0u) {
        {
            let _e91 = vFogCoord_1;
            fogDist = abs(_e91);
            let _e94 = global.uFogEnd;
            let _e95 = fogDist;
            let _e97 = global.uFogEnd;
            let _e98 = global.uFogStart;
            fogFactor = clamp(((_e94 - _e95) / (_e97 - _e98)), 0f, 1f);
            let _e105 = fragColor;
            let _e107 = global.uFogColor;
            let _e109 = fragColor;
            let _e111 = fogFactor;
            let _e113 = mix(_e107.xyz, _e109.xyz, vec3(_e111));
            fragColor.x = _e113.x;
            fragColor.y = _e113.y;
            fragColor.z = _e113.z;
            return;
        }
    } else {
        return;
    }
}

@fragment 
fn main(@location(0) vColor: vec4<f32>, @location(1) vTexCoord0_: vec2<f32>, @location(2) vTexCoord1_: vec3<f32>, @location(3) vFogCoord: f32) -> FragmentOutput {
    vColor_1 = vColor;
    vTexCoord0_1 = vTexCoord0_;
    vTexCoord1_1 = vTexCoord1_;
    vFogCoord_1 = vFogCoord;
    main_1();
    let _e63 = fragColor;
    return FragmentOutput(_e63);
}
