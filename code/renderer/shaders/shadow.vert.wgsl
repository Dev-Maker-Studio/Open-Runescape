struct P {
    uLightViewProjection: mat4x4<f32>,
    uModelMatrix: mat4x4<f32>,
    uAlphaTestEnabled_u: u32,
    uAlphaThreshold: f32,
}

struct VertexOutput {
    @location(0) vTexCoord: vec2<f32>,
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
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var worldPos: vec4<f32>;

    let _e15 = global.uModelMatrix;
    let _e16 = aPosition_1;
    worldPos = (_e15 * vec4<f32>(_e16.x, _e16.y, _e16.z, 1f));
    let _e25 = global.uLightViewProjection;
    let _e26 = worldPos;
    gl_Position = (_e25 * _e26);
    let _e28 = aTexCoord_1;
    vTexCoord = _e28;
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
    let _e33 = gl_Position;
    return VertexOutput(_e31, _e33);
}
