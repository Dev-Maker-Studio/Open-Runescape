diagnostic(off, derivative_uniformity);

struct P {
    uModelView: mat4x4<f32>,
    uProjection: mat4x4<f32>,
    uTextureMatrix: mat4x4<f32>,
    uNormalMatrix: mat3x3<f32>,
    uTime: f32,
    uScale: f32,
    uFogStart: f32,
    uFogEnd: f32,
    uSunDir: vec3<f32>,
    uSunColour: vec4<f32>,
    uSunExponent: f32,
    uBreakWaterDepth: f32,
    uBreakWaterOffset: f32,
    uUseWaveEffect_u: u32,
    uWaveIntensity: vec2<f32>,
    uWaveExponent: f32,
    uFogColor: vec4<f32>,
}

struct FragmentOutput {
    @location(0) fragColor: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uNormalSampler: texture_3d<f32>;
@group(1) @binding(1) 
var uNormalSampler_s: sampler;
@group(1) @binding(2) 
var uEnvMapSampler: texture_cube<f32>;
@group(1) @binding(3) 
var uEnvMapSampler_s: sampler;
var<private> vWorldViewNormal_1: vec3<f32>;
var<private> vWaterDepth_1: f32;
var<private> vTexCoord_1: vec3<f32>;
var<private> vFogFactor_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var rawNormal: vec4<f32>;
    var wnNormal: vec4<f32>;
    var wnVector: vec3<f32>;
    var wnReflection: vec3<f32>;
    var envColour: vec3<f32>;
    var specularColour: vec4<f32>;
    var shoreFactor: f32;
    var ndote: f32;
    var surfaceColour: vec4<f32>;
    var waveFactor: f32;
    var fresnel: f32;
    var fresnel_1: f32;

    let _e43 = vTexCoord_1;
    let _e44 = textureSample(uNormalSampler, uNormalSampler_s, _e43);
    rawNormal = _e44;
    let _e48 = rawNormal;
    wnNormal.x = _e48.x;
    let _e51 = rawNormal;
    wnNormal.y = _e51.z;
    let _e54 = rawNormal;
    wnNormal.z = _e54.y;
    let _e57 = rawNormal;
    wnNormal.w = _e57.w;
    let _e59 = wnNormal;
    let _e62 = wnNormal;
    let _e67 = ((2f * _e62.xyz) - vec3(1f));
    wnNormal.x = _e67.x;
    wnNormal.y = _e67.y;
    wnNormal.z = _e67.z;
    let _e74 = vWorldViewNormal_1;
    wnVector = normalize(_e74);
    let _e77 = wnVector;
    let _e78 = wnNormal;
    wnReflection = reflect(_e77, _e78.xyz);
    let _e82 = wnReflection;
    let _e83 = textureSample(uEnvMapSampler, uEnvMapSampler_s, _e82);
    envColour = _e83.xyz;
    let _e86 = global.uSunColour;
    let _e87 = global.uSunDir;
    let _e88 = wnReflection;
    let _e94 = global.uSunExponent;
    specularColour = (_e86 * pow(clamp(-(dot(_e87, _e88)), 0f, 1f), _e94));
    let _e98 = vWaterDepth_1;
    let _e99 = global.uBreakWaterDepth;
    let _e101 = global.uBreakWaterOffset;
    let _e102 = wnNormal;
    shoreFactor = clamp(((_e98 / _e99) - (_e101 * _e102.w)), 0f, 1f);
    let _e110 = wnVector;
    let _e111 = wnNormal;
    ndote = dot(_e110, _e111.xyz);
    let _e116 = global.uUseWaveEffect_u;
    if (_e116 != 0u) {
        {
            let _e120 = shoreFactor;
            let _e122 = global.uWaveExponent;
            waveFactor = (pow((1f - _e120), _e122) - 0.5f);
            let _e129 = waveFactor;
            let _e131 = waveFactor;
            waveFactor = (((-4f * _e129) * _e131) + 1f);
            let _e136 = ndote;
            fresnel = (1f - abs(_e136));
            let _e140 = envColour;
            let _e141 = fresnel;
            let _e142 = shoreFactor;
            let _e148 = global.uWaveIntensity;
            let _e150 = wnNormal;
            let _e153 = global.uWaveIntensity;
            let _e157 = waveFactor;
            let _e160 = specularColour;
            let _e161 = shoreFactor;
            surfaceColour = (mix(vec4<f32>(_e140.x, _e140.y, _e140.z, (_e141 * _e142)), ((_e148.x * _e150.wwww) + vec4(_e153.y)), vec4(_e157)) + (_e160 * _e161));
        }
    } else {
        {
            let _e165 = ndote;
            fresnel_1 = pow((1f - abs(_e165)), 2f);
            let _e171 = envColour;
            let _e172 = fresnel_1;
            let _e173 = shoreFactor;
            let _e179 = specularColour;
            let _e180 = shoreFactor;
            surfaceColour = (vec4<f32>(_e171.x, _e171.y, _e171.z, (_e172 * _e173)) + (_e179 * _e180));
        }
    }
    let _e183 = surfaceColour;
    let _e185 = global.uFogColor;
    let _e187 = vFogFactor_1;
    let _e189 = mix(_e183.xyz, _e185.xyz, vec3(_e187));
    let _e190 = surfaceColour;
    fragColor = vec4<f32>(_e189.x, _e189.y, _e189.z, _e190.w);
    return;
}

@fragment 
fn main(@location(0) vWorldViewNormal: vec3<f32>, @location(1) vWaterDepth: f32, @location(2) vTexCoord: vec3<f32>, @location(3) vFogFactor: f32) -> FragmentOutput {
    vWorldViewNormal_1 = vWorldViewNormal;
    vWaterDepth_1 = vWaterDepth;
    vTexCoord_1 = vTexCoord;
    vFogFactor_1 = vFogFactor;
    main_1();
    let _e61 = fragColor;
    return FragmentOutput(_e61);
}
