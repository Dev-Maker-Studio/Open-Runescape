diagnostic(off, derivative_uniformity);

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

struct FragmentOutput {
    @location(0) fragColor: vec4<f32>,
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
var<private> vColor_1: vec4<f32>;
var<private> vNormal_1: vec3<f32>;
var<private> vViewPos_1: vec3<f32>;
var<private> vFogFactor_1: f32;
var<private> vShadowCoord_1: vec4<f32>;
var<private> fragColor: vec4<f32>;

fn calculateShadow() -> f32 {
    var projCoords: vec3<f32>;
    var currentDepth: f32;
    var normal: vec3<f32>;
    var slopeFactor: f32;
    var bias: f32;
    var texelSize: vec2<f32>;
    var shadowSum: f32 = 0f;
    var x: i32 = -1i;
    var y: i32;
    var offset: vec2<f32>;
    var closestDepth: f32;
    var local: f32;
    var shadowFactor: f32;

    let _e52 = global.uShadowEnabled_u;
    if !((_e52 != 0u)) {
        {
            return 1f;
        }
    }
    let _e57 = vShadowCoord_1;
    let _e59 = vShadowCoord_1;
    projCoords = (_e57.xyz / vec3(_e59.w));
    let _e64 = projCoords;
    projCoords = ((_e64 * 0.5f) + vec3(0.5f));
    let _e70 = projCoords;
    let _e74 = projCoords;
    let _e79 = projCoords;
    let _e84 = projCoords;
    let _e89 = projCoords;
    if (((((_e70.x < 0f) || (_e74.x > 1f)) || (_e79.y < 0f)) || (_e84.y > 1f)) || (_e89.z > 1f)) {
        {
            return 1f;
        }
    }
    let _e95 = projCoords;
    currentDepth = _e95.z;
    let _e98 = vNormal_1;
    normal = normalize(_e98);
    let _e102 = normal;
    slopeFactor = (1f - abs(dot(_e102, vec3<f32>(0f, 1f, 0f))));
    let _e111 = global.uShadowBias;
    let _e112 = global.uShadowBias;
    let _e115 = slopeFactor;
    bias = (_e111 + ((_e112 * 2f) * _e115));
    let _e120 = global.uShadowMapSize;
    texelSize = (vec2(1f) / _e120);
    loop {
        let _e129 = x;
        if !((_e129 <= 1i)) {
            break;
        }
        {
            y = -1i;
            loop {
                let _e139 = y;
                if !((_e139 <= 1i)) {
                    break;
                }
                {
                    let _e146 = x;
                    let _e148 = y;
                    let _e151 = texelSize;
                    offset = (vec2<f32>(f32(_e146), f32(_e148)) * _e151);
                    let _e154 = projCoords;
                    let _e156 = offset;
                    let _e159 = textureSampleLevel(uShadowMap, uShadowMap_s, (_e154.xy + _e156), 0f);
                    closestDepth = _e159.x;
                    let _e162 = shadowSum;
                    let _e163 = currentDepth;
                    let _e164 = bias;
                    let _e166 = closestDepth;
                    if ((_e163 - _e164) > _e166) {
                        local = 1f;
                    } else {
                        local = 0f;
                    }
                    let _e171 = local;
                    shadowSum = (_e162 + _e171);
                }
                continuing {
                    let _e143 = y;
                    y = (_e143 + 1i);
                }
            }
        }
        continuing {
            let _e133 = x;
            x = (_e133 + 1i);
        }
    }
    let _e173 = shadowSum;
    shadowFactor = (_e173 / 9f);
    let _e178 = global.uShadowIntensity;
    let _e179 = shadowFactor;
    return mix(1f, _e178, _e179);
}

fn main_1() {
    var color: vec4<f32>;
    var shadow: f32;

    let _e52 = vColor_1;
    color = _e52;
    let _e54 = calculateShadow();
    shadow = _e54;
    let _e56 = color;
    let _e58 = color;
    let _e60 = shadow;
    let _e61 = (_e58.xyz * _e60);
    color.x = _e61.x;
    color.y = _e61.y;
    color.z = _e61.z;
    let _e68 = global.uFogColor;
    let _e69 = color;
    let _e70 = vFogFactor_1;
    fragColor = mix(_e68, _e69, vec4(_e70));
    let _e74 = color;
    fragColor.w = _e74.w;
    return;
}

@fragment 
fn main(@location(0) vColor: vec4<f32>, @location(1) vNormal: vec3<f32>, @location(2) vViewPos: vec3<f32>, @location(3) vFogFactor: f32, @location(4) vShadowCoord: vec4<f32>) -> FragmentOutput {
    vColor_1 = vColor;
    vNormal_1 = vNormal;
    vViewPos_1 = vViewPos;
    vFogFactor_1 = vFogFactor;
    vShadowCoord_1 = vShadowCoord;
    main_1();
    let _e73 = fragColor;
    return FragmentOutput(_e73);
}
