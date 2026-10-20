struct P {
    uModelView: mat4x4<f32>,
    uProjection: mat4x4<f32>,
    uTextureMatrix: mat4x4<f32>,
    uInverseViewRotation: mat4x4<f32>,
    uNoise: array<vec4<f32>, 64>,
    uTurbulence: f32,
    uAmbientLight: vec3<f32>,
    uTime: f32,
    uUse3DTexture_u: u32,
    uHasOverlay_u: u32,
    uHasBaseTexture_u: u32,
    uOverlayConstColor: vec4<f32>,
    uFogStart: f32,
    uFogEnd: f32,
    uFogColor: vec4<f32>,
    uFogEnabled_u: u32,
}

struct VertexOutput {
    @location(0) vColor: vec4<f32>,
    @location(1) vTexCoord0_: vec2<f32>,
    @location(2) vTexCoord1_: vec3<f32>,
    @location(3) vFogCoord: f32,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uBaseTexture: texture_2d<f32>;
@group(1) @binding(1) 
var uBaseTexture_s: sampler;
@group(1) @binding(2) 
var uOverlayTexture3D: texture_3d<f32>;
@group(1) @binding(3) 
var uOverlayTexture3D_s: sampler;
@group(1) @binding(4) 
var uOverlayTexture2D: texture_2d<f32>;
@group(1) @binding(5) 
var uOverlayTexture2D_s: sampler;
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aTexCoord_1: vec3<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vColor: vec4<f32>;
var<private> vTexCoord0_: vec2<f32>;
var<private> vTexCoord1_: vec3<f32>;
var<private> vFogCoord: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var viewPos: vec4<f32>;
    var worldPos: vec4<f32>;
    var noiseInput: vec2<f32>;
    var indexX: i32;
    var indexY: i32;
    var noiseOffset: vec2<f32>;
    var tc4_: vec4<f32>;
    var texCoord: vec2<f32>;

    let _e46 = global.uModelView;
    let _e47 = aPosition_1;
    viewPos = (_e46 * vec4<f32>(_e47.x, _e47.y, _e47.z, 1f));
    let _e55 = viewPos;
    vFogCoord = _e55.z;
    let _e57 = global.uInverseViewRotation;
    let _e58 = viewPos;
    worldPos = (_e57 * _e58);
    let _e63 = worldPos;
    let _e65 = worldPos;
    noiseInput.x = (_e63.x + _e65.z);
    let _e69 = worldPos;
    let _e71 = worldPos;
    noiseInput.y = (_e69.z - _e71.x);
    let _e74 = noiseInput;
    noiseInput = (fract((_e74 * 0.00012207031f)) * 64f);
    let _e80 = noiseInput;
    indexX = clamp(i32(_e80.x), 0i, 63i);
    let _e87 = noiseInput;
    indexY = clamp(i32(_e87.y), 0i, 63i);
    let _e96 = indexX;
    let _e98 = global.uNoise[_e96];
    noiseOffset.x = _e98.x;
    let _e101 = indexY;
    let _e103 = global.uNoise[_e101];
    noiseOffset.y = _e103.y;
    let _e105 = noiseOffset;
    let _e106 = global.uTurbulence;
    noiseOffset = (_e105 * _e106);
    let _e108 = aTexCoord_1;
    tc4_ = vec4<f32>(_e108.x, _e108.y, _e108.z, 1f);
    let _e119 = global.uTextureMatrix[0];
    let _e120 = tc4_;
    texCoord.x = dot(_e119, _e120);
    let _e125 = global.uTextureMatrix[1];
    let _e126 = tc4_;
    texCoord.y = dot(_e125, _e126);
    let _e128 = texCoord;
    let _e129 = noiseOffset;
    vTexCoord0_ = (_e128 + _e129);
    let _e131 = vTexCoord1_;
    let _e133 = texCoord;
    let _e135 = (_e133 * 0.125f);
    vTexCoord1_.x = _e135.x;
    vTexCoord1_.y = _e135.y;
    let _e140 = global.uUse3DTexture_u;
    if (_e140 != 0u) {
        {
            let _e144 = global.uTime;
            vTexCoord1_.z = _e144;
        }
    } else {
        {
            vTexCoord1_.z = 0f;
        }
    }
    let _e147 = aColor_1;
    let _e149 = global.uAmbientLight;
    let _e150 = (_e147.xyz * _e149);
    let _e151 = aColor_1;
    vColor = vec4<f32>(_e150.x, _e150.y, _e150.z, _e151.w);
    let _e158 = global.uProjection;
    let _e159 = viewPos;
    gl_Position = (_e158 * _e159);
    return;
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aNormal: vec3<f32>, @location(2) aTexCoord: vec3<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aNormal_1 = aNormal;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    main_1();
    let _e69 = vColor;
    let _e71 = vTexCoord0_;
    let _e73 = vTexCoord1_;
    let _e75 = vFogCoord;
    let _e77 = gl_Position;
    return VertexOutput(_e69, _e71, _e73, _e75, _e77);
}
