struct P {
    uProjection: mat4x4<f32>,
}

struct VertexOutput {
    @location(0) vTexCoord: vec2<f32>,
    @location(1) vColor: vec4<f32>,
    @location(2) @interpolate(flat) vTexIndex: i32,
    @location(3) @interpolate(flat) vClip: vec4<f32>,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uTextures: texture_2d_array<f32>;
@group(1) @binding(1) 
var uTextures_s: sampler;
var<private> aPosition_1: vec2<f32>;
var<private> aTexCoord_1: vec2<f32>;
var<private> aColor_1: vec4<f32>;
var<private> aTexIndex_1: f32;
var<private> aClip_1: vec4<f32>;
var<private> vTexCoord: vec2<f32>;
var<private> vColor: vec4<f32>;
var<private> vTexIndex: i32;
var<private> vClip: vec4<f32>;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    let _e14 = global.uProjection;
    let _e15 = aPosition_1;
    gl_Position = (_e14 * vec4<f32>(_e15.x, _e15.y, 0f, 1f));
    let _e22 = aTexCoord_1;
    vTexCoord = _e22;
    let _e23 = aClip_1;
    vClip = _e23;
    let _e24 = aColor_1;
    vColor = _e24;
    let _e25 = aTexIndex_1;
    vTexIndex = i32(floor((_e25 + 0.5f)));
    return;
}

@vertex 
fn main(@location(0) aPosition: vec2<f32>, @location(2) aTexCoord: vec2<f32>, @location(3) aColor: vec4<f32>, @location(4) aTexIndex: f32, @location(5) aClip: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    aTexIndex_1 = aTexIndex;
    aClip_1 = aClip;
    main_1();
    let _e35 = vTexCoord;
    let _e37 = vColor;
    let _e39 = vTexIndex;
    let _e41 = vClip;
    let _e43 = gl_Position;
    return VertexOutput(_e35, _e37, _e39, _e41, _e43);
}
