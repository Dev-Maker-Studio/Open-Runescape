diagnostic(off, derivative_uniformity);

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
    uEyePlaneT: vec4<f32>,
    uAnimTimeR: f32,
    uUse3DTexture_u: u32,
    uTextureEnabled_u: u32,
    uHasWaterfallTexture_u: u32,
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
@group(1) @binding(4) 
var uTexture2D: texture_2d<f32>;
@group(1) @binding(5) 
var uTexture2D_s: sampler;
var<private> vTexCoord_1: vec2<f32>;
var<private> vWaterfallCoord_1: vec3<f32>;
var<private> vColor_1: vec4<f32>;
var<private> vViewPos_1: vec3<f32>;
var<private> vFogFactor_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var baseColor: vec4<f32>;
    var texColor: vec4<f32>;
    var waterfallColor: vec4<f32>;

    let _e49 = global.uTextureEnabled_u;
    if (_e49 != 0u) {
        {
            let _e52 = vTexCoord_1;
            let _e53 = textureSample(uTexture, uTexture_s, _e52);
            texColor = _e53;
            let _e55 = texColor;
            let _e56 = vColor_1;
            baseColor = (_e55 * _e56);
        }
    } else {
        {
            let _e58 = vColor_1;
            baseColor = _e58;
        }
    }
    let _e59 = global.uHasWaterfallTexture_u;
    if (_e59 != 0u) {
        {
            let _e63 = global.uUse3DTexture_u;
            if (_e63 != 0u) {
                {
                    let _e66 = vWaterfallCoord_1;
                    let _e67 = textureSample(uTexture3D, uTexture3D_s, _e66);
                    waterfallColor = _e67;
                }
            } else {
                {
                    let _e68 = vWaterfallCoord_1;
                    let _e70 = textureSample(uTexture2D, uTexture2D_s, _e68.xy);
                    waterfallColor = _e70;
                }
            }
            let _e71 = baseColor;
            let _e73 = baseColor;
            let _e75 = waterfallColor;
            let _e77 = (_e73.xyz * _e75.xyz);
            baseColor.x = _e77.x;
            baseColor.y = _e77.y;
            baseColor.z = _e77.z;
            let _e85 = baseColor;
            let _e87 = waterfallColor;
            baseColor.w = (_e85.w * _e87.w);
        }
    }
    let _e90 = baseColor;
    let _e92 = global.uAlphaThreshold;
    if (_e90.w <= _e92) {
        {
            discard;
        }
    }
    let _e94 = global.uFogColor;
    let _e95 = baseColor;
    let _e96 = vFogFactor_1;
    fragColor = mix(_e94, _e95, vec4(_e96));
    let _e100 = baseColor;
    fragColor.w = _e100.w;
    return;
}

@fragment 
fn main(@location(0) vTexCoord: vec2<f32>, @location(1) vWaterfallCoord: vec3<f32>, @location(2) vColor: vec4<f32>, @location(3) vViewPos: vec3<f32>, @location(4) vFogFactor: f32) -> FragmentOutput {
    vTexCoord_1 = vTexCoord;
    vWaterfallCoord_1 = vWaterfallCoord;
    vColor_1 = vColor;
    vViewPos_1 = vViewPos;
    vFogFactor_1 = vFogFactor;
    main_1();
    let _e71 = fragColor;
    return FragmentOutput(_e71);
}
