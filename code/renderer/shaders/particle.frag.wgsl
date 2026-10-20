diagnostic(off, derivative_uniformity);

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

    let _e25 = global.uTextureEnabled_u;
    if (_e25 != 0u) {
        {
            let _e28 = vTexCoord_1;
            let _e29 = textureSample(uTexture, uTexture_s, _e28);
            texColor = _e29;
            let _e31 = global.uCombineMode;
            if (_e31 == 1i) {
                {
                    let _e34 = texColor;
                    color = _e34;
                }
            } else {
                let _e35 = global.uCombineMode;
                if (_e35 == 3i) {
                    {
                        let _e38 = texColor;
                        let _e40 = vColor_1;
                        let _e47 = clamp((_e38.xyz + _e40.xyz), vec3(0f), vec3(1f));
                        let _e48 = texColor;
                        let _e50 = vColor_1;
                        color = vec4<f32>(_e47.x, _e47.y, _e47.z, (_e48.w * _e50.w));
                    }
                } else {
                    let _e57 = global.uCombineMode;
                    if (_e57 == 4i) {
                        {
                            let _e60 = texColor;
                            let _e61 = vColor_1;
                            color = clamp((_e60 - _e61), vec4(0f), vec4(1f));
                        }
                    } else {
                        {
                            let _e68 = texColor;
                            let _e69 = vColor_1;
                            color = (_e68 * _e69);
                        }
                    }
                }
            }
        }
    } else {
        {
            let _e71 = vColor_1;
            color = _e71;
        }
    }
    let _e72 = color;
    if (_e72.w < 0.004f) {
        {
            discard;
        }
    }
    let _e76 = global.uFogColor;
    let _e77 = color;
    let _e78 = vFogFactor_1;
    fragColor = mix(_e76, _e77, vec4(_e78));
    let _e82 = color;
    fragColor.w = _e82.w;
    return;
}

@fragment 
fn main(@location(0) vTexCoord: vec2<f32>, @location(1) vColor: vec4<f32>, @location(2) vFogFactor: f32) -> FragmentOutput {
    vTexCoord_1 = vTexCoord;
    vColor_1 = vColor;
    vFogFactor_1 = vFogFactor;
    main_1();
    let _e37 = fragColor;
    return FragmentOutput(_e37);
}
