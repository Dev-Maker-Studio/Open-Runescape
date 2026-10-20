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
    uTextureScale: f32,
    uFogDensity: f32,
    uFogPosition: f32,
    uTextureEnabled_u: u32,
    uUse3DTexture_u: u32,
    uAnimTime: f32,
    uFogColor: vec4<f32>,
    uAlphaThreshold: f32,
}

struct VertexOutput {
    @location(0) vTexCoord: vec2<f32>,
    @location(1) vColor: vec4<f32>,
    @location(2) vViewPos: vec3<f32>,
    @location(3) vFogFactor: f32,
    @location(4) vFogAlpha: f32,
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
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aTexCoord_1: vec2<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vTexCoord: vec2<f32>;
var<private> vColor: vec4<f32>;
var<private> vViewPos: vec3<f32>;
var<private> vFogFactor: f32;
var<private> vFogAlpha: f32;
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
    let _e72 = global.uTextureScale;
    vTexCoord = (_e71 * _e72);
    let _e74 = global.uNormalMatrix;
    let _e75 = aNormal_1;
    normal = normalize((_e74 * _e75));
    let _e79 = normal;
    let _e80 = global.uSunDirection;
    nDotL = dot(_e79, _e80);
    let _e83 = global.uAmbient;
    let _e84 = nDotL;
    let _e87 = global.uContrast;
    lightIntensity = (_e83 + (max(_e84, 0f) * _e87));
    let _e91 = aColor_1;
    let _e93 = global.uSunColor;
    let _e95 = lightIntensity;
    let _e96 = ((_e91.xyz * _e93) * _e95);
    let _e97 = aColor_1;
    vColor = vec4<f32>(_e96.x, _e96.y, _e96.z, _e97.w);
    let _e103 = global.uFogEnabled_u;
    if (_e103 != 0u) {
        {
            let _e106 = viewPos;
            fogDistance = length(_e106.xyz);
            let _e110 = global.uFogEnd;
            let _e111 = fogDistance;
            let _e113 = global.uFogEnd;
            let _e114 = global.uFogStart;
            vFogFactor = clamp(((_e110 - _e111) / (_e113 - _e114)), 0f, 1f);
        }
    } else {
        {
            vFogFactor = 1f;
        }
    }
    let _e121 = global.uEyePlaneS;
    let _e122 = viewPos;
    vFogAlpha = clamp(dot(_e121, _e122), 0f, 1f);
    return;
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aNormal: vec3<f32>, @location(2) aTexCoord: vec2<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aNormal_1 = aNormal;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    main_1();
    let _e73 = vTexCoord;
    let _e75 = vColor;
    let _e77 = vViewPos;
    let _e79 = vFogFactor;
    let _e81 = vFogAlpha;
    let _e83 = gl_Position;
    return VertexOutput(_e73, _e75, _e77, _e79, _e81, _e83);
}
