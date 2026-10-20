diagnostic(off, derivative_uniformity);

struct P {
    uProjection: mat4x4<f32>,
}

struct FragmentOutput {
    @location(0) fragColor: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uTextures: texture_2d_array<f32>;
@group(1) @binding(1) 
var uTextures_s: sampler;
var<private> vTexCoord_1: vec2<f32>;
var<private> vColor_1: vec4<f32>;
var<private> vTexIndex_1: i32;
var<private> vClip_1: vec4<f32>;
var<private> fragColor: vec4<f32>;
var<private> gl_FragCoord_1: vec4<f32>;

fn main_1() {
    var texColor: vec4<f32>;

    let _e10 = gl_FragCoord_1;
    let _e12 = vClip_1;
    let _e15 = gl_FragCoord_1;
    let _e17 = vClip_1;
    let _e21 = gl_FragCoord_1;
    let _e23 = vClip_1;
    let _e27 = gl_FragCoord_1;
    let _e29 = vClip_1;
    if ((((_e10.x < _e12.x) || (_e15.x >= _e17.z)) || (_e21.y < _e23.y)) || (_e27.y >= _e29.w)) {
        {
            discard;
        }
    }
    let _e33 = vTexIndex_1;
    if (_e33 >= 0i) {
        {
            let _e37 = vTexCoord_1;
            let _e38 = vTexIndex_1;
            let _e42 = vec3<f32>(_e37.x, _e37.y, f32(_e38));
            let _e47 = textureSampleLevel(uTextures, uTextures_s, _e42.xy, i32(_e42.z), 0f);
            texColor = _e47;
            let _e48 = texColor;
            let _e49 = vColor_1;
            fragColor = (_e48 * _e49);
        }
    } else {
        {
            let _e51 = vColor_1;
            fragColor = _e51;
        }
    }
    let _e52 = fragColor;
    if (_e52.w < 0.004f) {
        {
            discard;
        }
    } else {
        return;
    }
}

@fragment 
fn main(@location(0) vTexCoord: vec2<f32>, @location(1) vColor: vec4<f32>, @location(2) @interpolate(flat) vTexIndex: i32, @location(3) @interpolate(flat) vClip: vec4<f32>, @builtin(position) gl_FragCoord: vec4<f32>) -> FragmentOutput {
    vTexCoord_1 = vTexCoord;
    vColor_1 = vColor;
    vTexIndex_1 = vTexIndex;
    vClip_1 = vClip;
    gl_FragCoord_1 = gl_FragCoord;
    main_1();
    let _e27 = fragColor;
    return FragmentOutput(_e27);
}
