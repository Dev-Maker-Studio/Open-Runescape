struct P {
    uTextureEnabled_u: u32,
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
var<private> vFogFactor_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var color: vec4<f32>;
    var texColor: vec4<f32>;

    let _e11 = global.uTextureEnabled_u;
    if (_e11 != 0u) {
        {
            let _e14 = vTexCoord_1;
            let _e15 = textureSample(uTexture, uTexture_s, _e14);
            texColor = _e15;
            let _e17 = texColor;
            let _e18 = vColor_1;
            color = (_e17 * _e18);
        }
    } else {
        {
            let _e20 = vColor_1;
            color = _e20;
        }
    }
    let _e21 = color;
    let _e23 = global.uAlphaThreshold;
    if (_e21.w <= _e23) {
        {
            discard;
        }
    }
    let _e25 = color;
    fragColor = _e25;
    return;
}

@fragment 
fn main(@location(0) vTexCoord: vec2<f32>, @location(1) vColor: vec4<f32>, @location(2) vFogFactor: f32) -> FragmentOutput {
    vTexCoord_1 = vTexCoord;
    vColor_1 = vColor;
    vFogFactor_1 = vFogFactor;
    main_1();
    let _e23 = fragColor;
    return FragmentOutput(_e23);
}
