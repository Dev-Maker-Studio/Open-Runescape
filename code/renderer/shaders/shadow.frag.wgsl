diagnostic(off, derivative_uniformity);

struct P {
    uLightViewProjection: mat4x4<f32>,
    uModelMatrix: mat4x4<f32>,
    uAlphaTestEnabled_u: u32,
    uAlphaThreshold: f32,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uTexture: texture_2d<f32>;
@group(1) @binding(1) 
var uTexture_s: sampler;
var<private> vTexCoord_1: vec2<f32>;

fn main_1() {
    var alpha: f32;

    let _e11 = global.uAlphaTestEnabled_u;
    if (_e11 != 0u) {
        {
            let _e14 = vTexCoord_1;
            let _e16 = textureSampleLevel(uTexture, uTexture_s, _e14, 0f);
            alpha = _e16.w;
            let _e19 = alpha;
            let _e20 = global.uAlphaThreshold;
            if (_e19 <= _e20) {
                {
                    discard;
                }
            } else {
                return;
            }
        }
    } else {
        return;
    }
}

@fragment 
fn main(@location(0) vTexCoord: vec2<f32>) {
    vTexCoord_1 = vTexCoord;
    main_1();
    return;
}
