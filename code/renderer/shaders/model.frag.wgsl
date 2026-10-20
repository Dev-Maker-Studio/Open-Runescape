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
    uPreLitColors_u: u32,
    uTextureEnabled_u: u32,
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
var<private> vTexCoord_1: vec2<f32>;
var<private> vColor_1: vec4<f32>;
var<private> vNormal_1: vec3<f32>;
var<private> vViewPos_1: vec3<f32>;
var<private> vFogFactor_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var color: vec4<f32>;
    var texColor: vec4<f32>;

    let _e37 = global.uTextureEnabled_u;
    if (_e37 != 0u) {
        {
            let _e40 = vTexCoord_1;
            let _e41 = textureSample(uTexture, uTexture_s, _e40);
            texColor = _e41;
            let _e43 = texColor;
            let _e44 = vColor_1;
            color = (_e43 * _e44);
        }
    } else {
        {
            let _e46 = vColor_1;
            color = _e46;
        }
    }
    let _e47 = color;
    let _e49 = global.uAlphaThreshold;
    if (_e47.w <= _e49) {
        {
            discard;
        }
    }
    let _e51 = global.uFogColor;
    let _e52 = color;
    let _e53 = vFogFactor_1;
    fragColor = mix(_e51, _e52, vec4(_e53));
    let _e57 = color;
    fragColor.w = _e57.w;
    return;
}

@fragment 
fn main(@location(0) vTexCoord: vec2<f32>, @location(1) vColor: vec4<f32>, @location(2) vNormal: vec3<f32>, @location(3) vViewPos: vec3<f32>, @location(4) vFogFactor: f32) -> FragmentOutput {
    vTexCoord_1 = vTexCoord;
    vColor_1 = vColor;
    vNormal_1 = vNormal;
    vViewPos_1 = vViewPos;
    vFogFactor_1 = vFogFactor;
    main_1();
    let _e55 = fragColor;
    return FragmentOutput(_e55);
}
