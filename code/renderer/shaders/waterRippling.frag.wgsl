struct P {
    uModelViewProjection: mat4x4<f32>,
    uModelView: mat4x4<f32>,
    uNormalMatrix: mat3x3<f32>,
    uSunDirection: vec3<f32>,
    uSunColor: vec3<f32>,
    uAmbient: f32,
    uContrast: f32,
    uFogStart: f32,
    uFogEnd: f32,
    uFogEnabled_u: u32,
    uEyePlaneS: vec4<f32>,
    uTextureScale: f32,
    uFogDensity: f32,
    uFogPosition: f32,
    uTextureEnabled_u: u32,
    uUse3DTexture_u: u32,
    uAnimTime: f32,
    uFogColor: vec4<f32>,
    uAlphaThreshold: f32,
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
var uTexture3D: texture_3d<f32>;
@group(1) @binding(3) 
var uTexture3D_s: sampler;
var<private> vTexCoord_1: vec2<f32>;
var<private> vColor_1: vec4<f32>;
var<private> vViewPos_1: vec3<f32>;
var<private> vFogFactor_1: f32;
var<private> vFogAlpha_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var baseColor: vec4<f32>;
    var texColor: vec4<f32>;

    let _e48 = vColor_1;
    baseColor = _e48;
    let _e50 = global.uTextureEnabled_u;
    if (_e50 != 0u) {
        {
            let _e54 = global.uUse3DTexture_u;
            if (_e54 != 0u) {
                {
                    let _e57 = vTexCoord_1;
                    let _e58 = global.uAnimTime;
                    let _e62 = textureSample(uTexture3D, uTexture3D_s, vec3<f32>(_e57.x, _e57.y, _e58));
                    texColor = _e62;
                }
            } else {
                {
                    let _e63 = vTexCoord_1;
                    let _e64 = textureSample(uTexture, uTexture_s, _e63);
                    texColor = _e64;
                }
            }
            let _e65 = baseColor;
            let _e67 = baseColor;
            let _e69 = texColor;
            let _e78 = clamp(((_e67.xyz + _e69.xyz) * 2f), vec3(0f), vec3(1f));
            baseColor.x = _e78.x;
            baseColor.y = _e78.y;
            baseColor.z = _e78.z;
            let _e86 = baseColor;
            let _e88 = texColor;
            baseColor.w = (_e86.w * _e88.w);
        }
    }
    let _e92 = baseColor;
    let _e94 = vFogAlpha_1;
    baseColor.w = clamp((_e92.w + _e94), 0f, 1f);
    let _e99 = baseColor;
    let _e101 = global.uAlphaThreshold;
    if (_e99.w <= _e101) {
        {
            discard;
        }
    }
    let _e103 = global.uFogColor;
    let _e104 = baseColor;
    let _e105 = vFogFactor_1;
    fragColor = mix(_e103, _e104, vec4(_e105));
    let _e109 = baseColor;
    fragColor.w = _e109.w;
    return;
}

@fragment 
fn main(@location(0) vTexCoord: vec2<f32>, @location(1) vColor: vec4<f32>, @location(2) vViewPos: vec3<f32>, @location(3) vFogFactor: f32, @location(4) vFogAlpha: f32) -> FragmentOutput {
    vTexCoord_1 = vTexCoord;
    vColor_1 = vColor;
    vViewPos_1 = vViewPos;
    vFogFactor_1 = vFogFactor;
    vFogAlpha_1 = vFogAlpha;
    main_1();
    let _e69 = fragColor;
    return FragmentOutput(_e69);
}
