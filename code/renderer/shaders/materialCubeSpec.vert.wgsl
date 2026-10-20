struct P {
    uModelViewProjection: mat4x4<f32>,
    uModelView: mat4x4<f32>,
    uNormalMatrix: mat3x3<f32>,
    uSunDirection: vec3<f32>,
    uSunColor: vec3<f32>,
    uAmbient: f32,
    uContrast: f32,
    uPreLitColors_u: u32,
    uFogStart: f32,
    uFogEnd: f32,
    uFogEnabled_u: u32,
    uTextureEnabled_u: u32,
    uFogColor: vec4<f32>,
    uAlphaThreshold: f32,
    uSpecularLevel: i32,
    uEffectEnabled_u: u32,
    uLimitedTextureUnits_u: u32,
}

struct VertexOutput {
    @location(0) vTexCoord: vec2<f32>,
    @location(1) vColor: vec4<f32>,
    @location(2) vNormal: vec3<f32>,
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
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aTexCoord_1: vec2<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vTexCoord: vec2<f32>;
var<private> vColor: vec4<f32>;
var<private> vNormal: vec3<f32>;
var<private> vViewPos: vec3<f32>;
var<private> vFogFactor: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var viewPos: vec4<f32>;
    var nDotL: f32;
    var lightIntensity: f32;
    var fogDistance: f32;

    let _e46 = global.uModelViewProjection;
    let _e47 = aPosition_1;
    gl_Position = (_e46 * vec4<f32>(_e47.x, _e47.y, _e47.z, 1f));
    let _e54 = global.uModelView;
    let _e55 = aPosition_1;
    viewPos = (_e54 * vec4<f32>(_e55.x, _e55.y, _e55.z, 1f));
    let _e63 = viewPos;
    vViewPos = _e63.xyz;
    let _e65 = aTexCoord_1;
    vTexCoord = _e65;
    let _e66 = global.uNormalMatrix;
    let _e67 = aNormal_1;
    vNormal = normalize((_e66 * _e67));
    let _e70 = global.uPreLitColors_u;
    if (_e70 != 0u) {
        {
            let _e73 = aColor_1;
            vColor = _e73;
        }
    } else {
        {
            let _e74 = vNormal;
            let _e75 = global.uSunDirection;
            nDotL = dot(_e74, _e75);
            let _e78 = global.uAmbient;
            let _e79 = nDotL;
            let _e82 = global.uContrast;
            lightIntensity = (_e78 + (max(_e79, 0f) * _e82));
            let _e86 = aColor_1;
            let _e88 = global.uSunColor;
            let _e90 = lightIntensity;
            let _e91 = ((_e86.xyz * _e88) * _e90);
            let _e92 = aColor_1;
            vColor = vec4<f32>(_e91.x, _e91.y, _e91.z, _e92.w);
        }
    }
    let _e98 = global.uFogEnabled_u;
    if (_e98 != 0u) {
        {
            let _e101 = viewPos;
            fogDistance = length(_e101.xyz);
            let _e105 = global.uFogEnd;
            let _e106 = fogDistance;
            let _e108 = global.uFogEnd;
            let _e109 = global.uFogStart;
            vFogFactor = clamp(((_e105 - _e106) / (_e108 - _e109)), 0f, 1f);
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
    let _e65 = vTexCoord;
    let _e67 = vColor;
    let _e69 = vNormal;
    let _e71 = vViewPos;
    let _e73 = vFogFactor;
    let _e75 = gl_Position;
    return VertexOutput(_e65, _e67, _e69, _e71, _e73, _e75);
}
