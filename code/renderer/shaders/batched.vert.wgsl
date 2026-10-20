struct P {
    uTextureEnabled_u: u32,
    uAlphaThreshold: f32,
}

struct VertexOutput {
    @location(0) vTexCoord: vec2<f32>,
    @location(1) vColor: vec4<f32>,
    @location(2) vFogFactor: f32,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uTexture: texture_2d<f32>;
@group(1) @binding(1) 
var uTexture_s: sampler;
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aTexCoord_1: vec2<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vTexCoord: vec2<f32>;
var<private> vColor: vec4<f32>;
var<private> vFogFactor: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    let _e14 = aPosition_1;
    gl_Position = vec4<f32>(_e14.x, _e14.y, _e14.z, 1f);
    let _e20 = aTexCoord_1;
    vTexCoord = _e20;
    let _e21 = aColor_1;
    vColor = _e21;
    vFogFactor = 1f;
    return;
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aNormal: vec3<f32>, @location(2) aTexCoord: vec2<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aNormal_1 = aNormal;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    main_1();
    let _e31 = vTexCoord;
    let _e33 = vColor;
    let _e35 = vFogFactor;
    let _e37 = gl_Position;
    return VertexOutput(_e31, _e33, _e35, _e37);
}
