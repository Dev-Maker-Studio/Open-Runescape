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
    uShadowMatrix: mat4x4<f32>,
    uShadowEnabled_u: u32,
    uPointLightCount: i32,
    uPointLightPos: array<vec4<f32>, 128>,
    uPointLightColor: array<vec3<f32>, 128>,
    uGroundShadowEnabled_u: u32,
    uGroundShadowScale: f32,
    uFogColor: vec4<f32>,
    uShadowBias: f32,
    uShadowIntensity: f32,
    uShadowMapSize: vec2<f32>,
}

struct VertexOutput {
    @location(0) vColor: vec4<f32>,
    @location(1) vNormal: vec3<f32>,
    @location(2) vViewPos: vec3<f32>,
    @location(3) vFogFactor: f32,
    @location(4) vShadowCoord: vec4<f32>,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uGroundShadowTex: texture_2d<f32>;
@group(1) @binding(1) 
var uGroundShadowTex_s: sampler;
@group(1) @binding(2) 
var uShadowMap: texture_2d<f32>;
@group(1) @binding(3) 
var uShadowMap_s: sampler;
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vColor: vec4<f32>;
var<private> vNormal: vec3<f32>;
var<private> vViewPos: vec3<f32>;
var<private> vFogFactor: f32;
var<private> vShadowCoord: vec4<f32>;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var viewPos: vec4<f32>;
    var nDotL: f32;
    var lightIntensity: f32;
    var litRGB: vec3<f32>;
    var i: i32 = 0i;
    var toLight: vec3<f32>;
    var d2_: f32;
    var att: f32;
    var ndotl: f32;
    var raw: f32;
    var fogDistance: f32;

    let _e55 = global.uModelViewProjection;
    let _e56 = aPosition_1;
    gl_Position = (_e55 * vec4<f32>(_e56.x, _e56.y, _e56.z, 1f));
    let _e63 = global.uModelView;
    let _e64 = aPosition_1;
    viewPos = (_e63 * vec4<f32>(_e64.x, _e64.y, _e64.z, 1f));
    let _e72 = viewPos;
    vViewPos = _e72.xyz;
    let _e74 = global.uNormalMatrix;
    let _e75 = aNormal_1;
    vNormal = normalize((_e74 * _e75));
    let _e78 = vNormal;
    let _e79 = global.uSunDirection;
    nDotL = dot(_e78, _e79);
    let _e82 = global.uAmbient;
    let _e83 = nDotL;
    let _e86 = global.uContrast;
    lightIntensity = (_e82 + (max(_e83, 0f) * _e86));
    let _e90 = aColor_1;
    let _e92 = global.uSunColor;
    let _e94 = lightIntensity;
    litRGB = ((_e90.xyz * _e92) * _e94);
    loop {
        let _e99 = i;
        let _e100 = global.uPointLightCount;
        if !((_e99 < _e100)) {
            break;
        }
        {
            let _e106 = i;
            let _e108 = global.uPointLightPos[_e106];
            let _e110 = aPosition_1;
            toLight = (_e108.xyz - _e110);
            let _e113 = toLight;
            let _e114 = toLight;
            d2_ = dot(_e113, _e114);
            let _e118 = i;
            let _e120 = global.uPointLightPos[_e118];
            let _e122 = d2_;
            att = (1f / max((_e120.w * _e122), 0.000001f));
            let _e128 = aNormal_1;
            let _e129 = toLight;
            ndotl = max(dot(_e128, normalize(_e129)), 0f);
            let _e135 = litRGB;
            let _e136 = aColor_1;
            let _e138 = i;
            let _e140 = global.uPointLightColor[_e138];
            let _e142 = att;
            let _e143 = ndotl;
            litRGB = (_e135 + ((_e136.xyz * _e140) * (_e142 * _e143)));
        }
        continuing {
            let _e103 = i;
            i = (_e103 + 1i);
        }
    }
    let _e147 = global.uGroundShadowEnabled_u;
    if (_e147 != 0u) {
        {
            let _e150 = aPosition_1;
            let _e152 = global.uGroundShadowScale;
            let _e155 = textureSampleLevel(uGroundShadowTex, uGroundShadowTex_s, (_e150.xz * _e152), 0f);
            raw = (_e155.x * 255f);
            let _e160 = litRGB;
            let _e162 = raw;
            litRGB = (_e160 * (1f - (min(_e162, 1f) * 0.26666668f)));
        }
    }
    let _e171 = litRGB;
    let _e176 = clamp(_e171, vec3(0f), vec3(1f));
    let _e177 = aColor_1;
    vColor = vec4<f32>(_e176.x, _e176.y, _e176.z, _e177.w);
    let _e183 = global.uFogEnabled_u;
    if (_e183 != 0u) {
        {
            let _e186 = viewPos;
            fogDistance = length(_e186.xyz);
            let _e190 = global.uFogEnd;
            let _e191 = fogDistance;
            let _e193 = global.uFogEnd;
            let _e194 = global.uFogStart;
            vFogFactor = clamp(((_e190 - _e191) / (_e193 - _e194)), 0f, 1f);
        }
    } else {
        {
            vFogFactor = 1f;
        }
    }
    let _e201 = global.uShadowEnabled_u;
    if (_e201 != 0u) {
        {
            let _e204 = global.uShadowMatrix;
            let _e205 = aPosition_1;
            vShadowCoord = (_e204 * vec4<f32>(_e205.x, _e205.y, _e205.z, 1f));
            return;
        }
    } else {
        {
            vShadowCoord = vec4(0f);
            return;
        }
    }
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aNormal: vec3<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aNormal_1 = aNormal;
    aColor_1 = aColor;
    main_1();
    let _e73 = vColor;
    let _e75 = vNormal;
    let _e77 = vViewPos;
    let _e79 = vFogFactor;
    let _e81 = vShadowCoord;
    let _e83 = gl_Position;
    return VertexOutput(_e73, _e75, _e77, _e79, _e81, _e83);
}
