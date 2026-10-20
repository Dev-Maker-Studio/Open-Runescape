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

struct VertexOutput {
    @location(0) vTexCoord: vec2<f32>,
    @location(1) vWaterfallCoord: vec3<f32>,
    @location(2) vColor: vec4<f32>,
    @location(3) vViewPos: vec3<f32>,
    @location(4) vFogFactor: f32,
    @builtin(position) gl_Position: vec4<f32>,
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
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aTexCoord_1: vec2<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vTexCoord: vec2<f32>;
var<private> vWaterfallCoord: vec3<f32>;
var<private> vColor: vec4<f32>;
var<private> vViewPos: vec3<f32>;
var<private> vFogFactor: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var viewPos: vec4<f32>;
    var normal: vec3<f32>;
    var nDotL: f32;
    var lightIntensity: f32;
    var fogDistance: f32;

    let _e52 = global.uModelViewProjection;
    let _e53 = aPosition_1;
    gl_Position = (_e52 * vec4<f32>(_e53.x, _e53.y, _e53.z, 1f));
    let _e60 = global.uModelView;
    let _e61 = aPosition_1;
    viewPos = (_e60 * vec4<f32>(_e61.x, _e61.y, _e61.z, 1f));
    let _e69 = viewPos;
    vViewPos = _e69.xyz;
    let _e71 = aTexCoord_1;
    vTexCoord = _e71;
    let _e73 = global.uEyePlaneS;
    let _e74 = viewPos;
    vWaterfallCoord.x = dot(_e73, _e74);
    let _e77 = global.uEyePlaneT;
    let _e78 = viewPos;
    vWaterfallCoord.y = dot(_e77, _e78);
    let _e80 = global.uUse3DTexture_u;
    if (_e80 != 0u) {
        {
            let _e84 = global.uAnimTimeR;
            vWaterfallCoord.z = _e84;
        }
    } else {
        {
            vWaterfallCoord.z = 0f;
        }
    }
    let _e87 = global.uNormalMatrix;
    let _e88 = aNormal_1;
    normal = normalize((_e87 * _e88));
    let _e92 = normal;
    let _e93 = global.uSunDirection;
    nDotL = dot(_e92, _e93);
    let _e96 = global.uAmbient;
    let _e97 = nDotL;
    let _e100 = global.uContrast;
    lightIntensity = (_e96 + (max(_e97, 0f) * _e100));
    let _e104 = aColor_1;
    let _e106 = global.uSunColor;
    let _e108 = lightIntensity;
    let _e109 = ((_e104.xyz * _e106) * _e108);
    let _e110 = aColor_1;
    vColor = vec4<f32>(_e109.x, _e109.y, _e109.z, _e110.w);
    let _e116 = global.uFogEnabled_u;
    if (_e116 != 0u) {
        {
            let _e119 = viewPos;
            fogDistance = length(_e119.xyz);
            let _e123 = global.uFogEnd;
            let _e124 = fogDistance;
            let _e126 = global.uFogEnd;
            let _e127 = global.uFogStart;
            vFogFactor = clamp(((_e123 - _e124) / (_e126 - _e127)), 0f, 1f);
            return;
        }
    } else {
        {
            vFogFactor = 1f;
            return;
        }
    }
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aNormal: vec3<f32>, @location(2) aTexCoord: vec2<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aNormal_1 = aNormal;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    main_1();
    let _e75 = vTexCoord;
    let _e77 = vWaterfallCoord;
    let _e79 = vColor;
    let _e81 = vViewPos;
    let _e83 = vFogFactor;
    let _e85 = gl_Position;
    return VertexOutput(_e75, _e77, _e79, _e81, _e83, _e85);
}
