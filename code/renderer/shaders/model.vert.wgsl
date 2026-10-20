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
    uPreLitColors_u: u32,
    uTextureEnabled_u: u32,
    uFogColor: vec4<f32>,
    uAlphaThreshold: f32,
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

    let _e40 = global.uModelViewProjection;
    let _e41 = aPosition_1;
    gl_Position = (_e40 * vec4<f32>(_e41.x, _e41.y, _e41.z, 1f));
    let _e48 = global.uModelView;
    let _e49 = aPosition_1;
    viewPos = (_e48 * vec4<f32>(_e49.x, _e49.y, _e49.z, 1f));
    let _e57 = viewPos;
    vViewPos = _e57.xyz;
    let _e59 = aTexCoord_1;
    vTexCoord = _e59;
    let _e60 = global.uNormalMatrix;
    let _e61 = aNormal_1;
    vNormal = normalize((_e60 * _e61));
    let _e64 = global.uPreLitColors_u;
    if (_e64 != 0u) {
        {
            let _e67 = aColor_1;
            vColor = _e67;
        }
    } else {
        {
            let _e68 = vNormal;
            let _e69 = global.uSunDirection;
            nDotL = dot(_e68, _e69);
            let _e72 = global.uAmbient;
            let _e73 = nDotL;
            let _e76 = global.uContrast;
            lightIntensity = (_e72 + (max(_e73, 0f) * _e76));
            let _e80 = aColor_1;
            let _e82 = global.uSunColor;
            let _e84 = lightIntensity;
            let _e85 = ((_e80.xyz * _e82) * _e84);
            let _e86 = aColor_1;
            vColor = vec4<f32>(_e85.x, _e85.y, _e85.z, _e86.w);
        }
    }
    let _e92 = global.uFogEnabled_u;
    if (_e92 != 0u) {
        {
            let _e95 = viewPos;
            fogDistance = length(_e95.xyz);
            let _e99 = global.uFogEnd;
            let _e100 = fogDistance;
            let _e102 = global.uFogEnd;
            let _e103 = global.uFogStart;
            vFogFactor = clamp(((_e99 - _e100) / (_e102 - _e103)), 0f, 1f);
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
    let _e59 = vTexCoord;
    let _e61 = vColor;
    let _e63 = vNormal;
    let _e65 = vViewPos;
    let _e67 = vFogFactor;
    let _e69 = gl_Position;
    return VertexOutput(_e59, _e61, _e63, _e65, _e67, _e69);
}
