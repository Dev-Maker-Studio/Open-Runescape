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
                    let _e210 = global.uViewport;
                    let _e213 = global.uViewport;
                    ndc = ((((_e208.xy - _e210.xy) / _e213.zw) * 2f) - vec2(1f));
                    let _e222 = global.uInvViewProjection;
                    let _e223 = ndc;
                    let _e224 = sceneDepth;
                    below = (_e222 * vec4<f32>(_e223.x, _e223.y, ((_e224 * 2f) - 1f), 1f));
                    let _e235 = below;
                    let _e237 = below;
                    let _e240 = vWorldY_1;
                    submerged = max(((_e235.y / _e237.w) - _e240), 0f);
                    let _e245 = submerged;
                    let _e246 = global.uStillFog;
                    let _e249 = global.uStillFog;
                    cover = clamp(((_e245 * _e246.z) + _e249.w), 0f, 1f);
                    let _e256 = color;
                    let _e259 = color;
                    let _e262 = cover;
                    alpha = (_e256.w + ((1f - _e259.w) * _e262));
                    let _e266 = color;
                    let _e268 = color;
                    let _e270 = color;
                    let _e273 = global.uWaterColour;
                    let _e275 = color;
                    let _e279 = cover;
                    let _e282 = alpha;
                    let _e286 = (((_e268.xyz * _e270.w) + ((_e273 * (1f - _e275.w)) * _e279)) / vec3(max(_e282, 0.0001f)));
                    color.x = _e286.x;
                    color.y = _e286.y;
                    color.z = _e286.z;
                    let _e294 = alpha;
                    color.w = _e294;
                }
            }
        }
    } else {
        let _e295 = global.uWaterMode;
        if (_e295 == 2i) {
            {
                let _e298 = global.uHasWaterNoise_u;
                if (_e298 != 0u) {
                    let _e301 = color;
                    let _e303 = color;
                    let _e305 = vWaterNoiseCoord_1;
                    let _e306 = textureSample(uWaterNoise, uWaterNoise_s, _e305);
                    let _e313 = clamp((_e303.xyz + _e306.xyz), vec3(0f), vec3(1f));
                    color.x = _e313.x;
                    color.y = _e313.y;
                    color.z = _e313.z;
                }
                color.w = 1f;
            }
        } else {
            let _e322 = global.uWaterMode;
            if (_e322 == 3i) {
                {
                    let _e325 = color;
                    let _e327 = color;
                    let _e329 = global.uWaterColour;
                    let _e330 = vWaterMix_1;
                    let _e332 = mix(_e327.xyz, _e329, vec3(_e330));
                    color.x = _e332.x;
                    color.y = _e332.y;
                    color.z = _e332.z;
                }
            }
        }
    }
    let _e339 = global.uAlphaFromVertex_u;
    let _e342 = global.uWaterMode;
    let _e346 = global.uWaterMode;
    if (((_e339 != 0u) && (_e342 != 4i)) && (_e346 != 2i)) {
        {
            let _e351 = vColor_1;
            color.w = _e351.w;
        }
    }
    let _e353 = global.uGroundShadowEnabled_u;
    if (_e353 != 0u) {
        {
            let _e356 = color;
            let _e358 = color;
            let _e361 = vWorldXZ_1;
            let _e362 = global.uGroundShadowScale;
            let _e364 = textureSample(uGroundShadowTex, uGroundShadowTex_s, (_e361 * _e362));
            let _e367 = (_e358.xyz * (1f - _e364.x));
            color.x = _e367.x;
            color.y = _e367.y;
            color.z = _e367.z;
        }
    }
    let _e374 = color;
    let _e376 = global.uFogColor;
    let _e378 = color;
    let _e380 = vFogFactor_1;
    let _e382 = mix(_e376.xyz, _e378.xyz, vec3(_e380));
    color.x = _e382.x;
    color.y = _e382.y;
    color.z = _e382.z;
    let _e389 = color;
    let _e391 = color;
    let _e395 = vFogFactor_1;
    let _e396 = vPointLight_1;
    let _e404 = clamp((_e391.xyz * (vec3(1f) + (_e395 * _e396))), vec3(0f), vec3(1f));
    color.x = _e404.x;
    color.y = _e404.y;
    color.z = _e404.z;
    let _e411 = color;
    fragColor = _e411;
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
