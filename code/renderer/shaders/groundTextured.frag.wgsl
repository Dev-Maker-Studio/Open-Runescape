diagnostic(off, derivative_uniformity);

struct P {
    uViewProjection: mat4x4<f32>,
    uClipPlane: vec4<f32>,
    uTextureScale: f32,
    uTextureScroll: vec2<f32>,
    uHasNormals_u: u32,
    uSunDirection: vec3<f32>,
    uSunColor: vec3<f32>,
    uAmbient: f32,
    uContrast: f32,
    uSunBrightness: f32,
    uContrastPos: f32,
    uContrastNeg: f32,
    uRawColorDebug: i32,
    uFogStart: f32,
    uFogEnd: f32,
    uFogEnabled_u: u32,
    uPointLightCount: i32,
    uPointLightPos: array<vec4<f32>, 128>,
    uPointLightColor: array<vec3<f32>, 128>,
    uWaterMode: i32,
    uStillFog: vec4<f32>,
    uLapNoise: array<vec4<f32>, 64>,
    uLapTurbulence: f32,
    uLapAmbient: vec3<f32>,
    uWaterTime: f32,
    uRippleFog: vec2<f32>,
    uHasWaterNoise_u: u32,
    uWaterColour: vec3<f32>,
    uDepthFog_u: u32,
    uInvViewProjection: mat4x4<f32>,
    uViewport: vec4<f32>,
    uFramebufferSize: vec2<f32>,
    uGroundShadowEnabled_u: u32,
    uGroundShadowScale: f32,
    uHasTexture_u: u32,
    uFogColor: vec4<f32>,
    uCombineMode: i32,
    uAlphaFromVertex_u: u32,
}

struct FragmentOutput {
    @location(0) fragColor: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uWaterNoise: texture_3d<f32>;
@group(1) @binding(1) 
var uWaterNoise_s: sampler;
@group(1) @binding(2) 
var uSceneDepth: texture_2d<f32>;
@group(1) @binding(3) 
var uSceneDepth_s: sampler;
@group(1) @binding(4) 
var uGroundShadowTex: texture_2d<f32>;
@group(1) @binding(5) 
var uGroundShadowTex_s: sampler;
@group(1) @binding(6) 
var uTexture: texture_2d<f32>;
@group(1) @binding(7) 
var uTexture_s: sampler;
var<private> vColor_1: vec4<f32>;
var<private> vTexCoord_1: vec2<f32>;
var<private> vFogFactor_1: f32;
var<private> vWorldXZ_1: vec2<f32>;
var<private> vWaterNoiseCoord_1: vec3<f32>;
var<private> vWaterMix_1: f32;
var<private> vWorldY_1: f32;
var<private> vPointLight_1: vec3<f32>;
var<private> fragColor: vec4<f32>;
var<private> vClipDistance_1: f32;
var<private> gl_FragCoord_1: vec4<f32>;

fn main_1() {
    var color: vec4<f32>;
    var texColor: vec4<f32>;
    var local: vec4<f32>;
    var n: vec4<f32>;
    var sceneDepth: f32;
    var ndc: vec2<f32>;
    var below: vec4<f32>;
    var submerged: f32;
    var cover: f32;
    var alpha: f32;

    let _e94 = vClipDistance_1;
    if (_e94 < 0f) {
        discard;
    }
    let _e98 = global.uCombineMode;
    if (_e98 == 99i) {
        {
            let _e101 = vColor_1;
            let _e102 = _e101.xyz;
            fragColor = vec4<f32>(_e102.x, _e102.y, _e102.z, 1f);
            return;
        }
    }
    let _e108 = global.uHasTexture_u;
    if (_e108 != 0u) {
        {
            let _e111 = vTexCoord_1;
            let _e112 = textureSample(uTexture, uTexture_s, _e111);
            texColor = _e112;
            let _e114 = global.uCombineMode;
            if (_e114 == 1i) {
                {
                    let _e117 = texColor;
                    color = _e117;
                }
            } else {
                let _e118 = global.uCombineMode;
                if (_e118 == 3i) {
                    {
                        let _e121 = texColor;
                        let _e123 = vColor_1;
                        let _e130 = clamp((_e121.xyz + _e123.xyz), vec3(0f), vec3(1f));
                        let _e131 = texColor;
                        let _e133 = vColor_1;
                        color = vec4<f32>(_e130.x, _e130.y, _e130.z, (_e131.w * _e133.w));
                    }
                } else {
                    let _e140 = global.uCombineMode;
                    if (_e140 == 4i) {
                        {
                            let _e143 = texColor;
                            let _e144 = vColor_1;
                            color = clamp((_e143 - _e144), vec4(0f), vec4(1f));
                        }
                    } else {
                        {
                            let _e151 = texColor;
                            let _e152 = vColor_1;
                            color = (_e151 * _e152);
                        }
                    }
                }
            }
        }
    } else {
        {
            let _e154 = vColor_1;
            color = _e154;
        }
    }
    let _e155 = global.uWaterMode;
    if (_e155 == 4i) {
        {
            let _e158 = global.uHasWaterNoise_u;
            if (_e158 != 0u) {
                let _e161 = vWaterNoiseCoord_1;
                let _e162 = textureSample(uWaterNoise, uWaterNoise_s, _e161);
                local = _e162;
            } else {
                local = vec4<f32>(0f, 0f, 0f, 1f);
            }
            let _e169 = local;
            n = _e169;
            let _e171 = vColor_1;
            let _e173 = n;
            let _e182 = clamp(((_e171.xyz + _e173.xyz) * 2f), vec3(0f), vec3(1f));
            let _e183 = vColor_1;
            let _e185 = n;
            let _e188 = vWaterMix_1;
            color = vec4<f32>(_e182.x, _e182.y, _e182.z, clamp(((_e183.w * _e185.w) + _e188), 0f, 1f));
            let _e197 = global.uDepthFog_u;
            if (_e197 != 0u) {
                {
                    let _e201 = gl_FragCoord_1;
                    let _e203 = global.uFramebufferSize;
                    let _e205 = textureSample(uSceneDepth, uSceneDepth_s, (_e201.xy / _e203));
                    sceneDepth = _e205.x;
                    let _e208 = gl_FragCoord_1;
                    let _e210 = global.uFramebufferSize;
                    let _e212 = gl_FragCoord_1;
                    let _e216 = global.uViewport;
                    let _e219 = global.uViewport;
                    ndc = ((((vec2<f32>(_e208.x, (_e210.y - _e212.y)) - _e216.xy) / _e219.zw) * 2f) - vec2(1f));
                    let _e228 = global.uInvViewProjection;
                    let _e229 = ndc;
                    let _e230 = sceneDepth;
                    below = (_e228 * vec4<f32>(_e229.x, _e229.y, ((_e230 * 2f) - 1f), 1f));
                    let _e241 = below;
                    let _e243 = below;
                    let _e246 = vWorldY_1;
                    submerged = max(((_e241.y / _e243.w) - _e246), 0f);
                    let _e251 = submerged;
                    let _e252 = global.uStillFog;
                    let _e255 = global.uStillFog;
                    cover = clamp(((_e251 * _e252.z) + _e255.w), 0f, 1f);
                    let _e262 = color;
                    let _e265 = color;
                    let _e268 = cover;
                    alpha = (_e262.w + ((1f - _e265.w) * _e268));
                    let _e272 = color;
                    let _e274 = color;
                    let _e276 = color;
                    let _e279 = global.uWaterColour;
                    let _e281 = color;
                    let _e285 = cover;
                    let _e288 = alpha;
                    let _e292 = (((_e274.xyz * _e276.w) + ((_e279 * (1f - _e281.w)) * _e285)) / vec3(max(_e288, 0.0001f)));
                    color.x = _e292.x;
                    color.y = _e292.y;
                    color.z = _e292.z;
                    let _e300 = alpha;
                    color.w = _e300;
                }
            }
        }
    } else {
        let _e301 = global.uWaterMode;
        if (_e301 == 2i) {
            {
                let _e304 = global.uHasWaterNoise_u;
                if (_e304 != 0u) {
                    let _e307 = color;
                    let _e309 = color;
                    let _e311 = vWaterNoiseCoord_1;
                    let _e312 = textureSample(uWaterNoise, uWaterNoise_s, _e311);
                    let _e319 = clamp((_e309.xyz + _e312.xyz), vec3(0f), vec3(1f));
                    color.x = _e319.x;
                    color.y = _e319.y;
                    color.z = _e319.z;
                }
                color.w = 1f;
            }
        } else {
            let _e328 = global.uWaterMode;
            if (_e328 == 3i) {
                {
                    let _e331 = color;
                    let _e333 = color;
                    let _e335 = global.uWaterColour;
                    let _e336 = vWaterMix_1;
                    let _e338 = mix(_e333.xyz, _e335, vec3(_e336));
                    color.x = _e338.x;
                    color.y = _e338.y;
                    color.z = _e338.z;
                }
            }
        }
    }
    let _e345 = global.uAlphaFromVertex_u;
    let _e348 = global.uWaterMode;
    let _e352 = global.uWaterMode;
    if (((_e345 != 0u) && (_e348 != 4i)) && (_e352 != 2i)) {
        {
            let _e357 = vColor_1;
            color.w = _e357.w;
        }
    }
    let _e359 = global.uGroundShadowEnabled_u;
    if (_e359 != 0u) {
        {
            let _e362 = color;
            let _e364 = color;
            let _e367 = vWorldXZ_1;
            let _e368 = global.uGroundShadowScale;
            let _e370 = textureSample(uGroundShadowTex, uGroundShadowTex_s, (_e367 * _e368));
            let _e373 = (_e364.xyz * (1f - _e370.x));
            color.x = _e373.x;
            color.y = _e373.y;
            color.z = _e373.z;
        }
    }
    let _e380 = color;
    let _e382 = global.uFogColor;
    let _e384 = color;
    let _e386 = vFogFactor_1;
    let _e388 = mix(_e382.xyz, _e384.xyz, vec3(_e386));
    color.x = _e388.x;
    color.y = _e388.y;
    color.z = _e388.z;
    let _e395 = color;
    let _e397 = color;
    let _e401 = vFogFactor_1;
    let _e402 = vPointLight_1;
    let _e410 = clamp((_e397.xyz * (vec3(1f) + (_e401 * _e402))), vec3(0f), vec3(1f));
    color.x = _e410.x;
    color.y = _e410.y;
    color.z = _e410.z;
    let _e417 = color;
    fragColor = _e417;
    return;
}

@fragment 
fn main(@location(0) vColor: vec4<f32>, @location(1) vTexCoord: vec2<f32>, @location(2) vFogFactor: f32, @location(3) vWorldXZ: vec2<f32>, @location(4) vWaterNoiseCoord: vec3<f32>, @location(5) vWaterMix: f32, @location(6) vWorldY: f32, @location(7) vPointLight: vec3<f32>, @location(8) vClipDistance: f32, @builtin(position) gl_FragCoord: vec4<f32>) -> FragmentOutput {
    vColor_1 = vColor;
    vTexCoord_1 = vTexCoord;
    vFogFactor_1 = vFogFactor;
    vWorldXZ_1 = vWorldXZ;
    vWaterNoiseCoord_1 = vWaterNoiseCoord;
    vWaterMix_1 = vWaterMix;
    vWorldY_1 = vWorldY;
    vPointLight_1 = vPointLight;
    vClipDistance_1 = vClipDistance;
    gl_FragCoord_1 = gl_FragCoord;
    main_1();
    let _e133 = fragColor;
    return FragmentOutput(_e133);
}
