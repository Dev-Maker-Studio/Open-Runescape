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

struct VertexOutput {
    @location(0) vColor: vec4<f32>,
    @location(1) vTexCoord: vec2<f32>,
    @location(2) vFogFactor: f32,
    @location(3) vWorldXZ: vec2<f32>,
    @location(4) vWaterNoiseCoord: vec3<f32>,
    @location(5) vWaterMix: f32,
    @location(6) vWorldY: f32,
    @location(7) vPointLight: vec3<f32>,
    @location(8) vClipDistance: f32,
    @builtin(position) gl_Position: vec4<f32>,
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
var<private> aPosition_1: vec3<f32>;
var<private> aColor_1: vec4<f32>;
var<private> aTexCoord_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> vColor: vec4<f32>;
var<private> vTexCoord: vec2<f32>;
var<private> vFogFactor: f32;
var<private> vWorldXZ: vec2<f32>;
var<private> vWaterNoiseCoord: vec3<f32>;
var<private> vWaterMix: f32;
var<private> vWorldY: f32;
var<private> vPointLight: vec3<f32>;
var<private> vClipDistance: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var position: vec3<f32>;
    var viewZ: f32;
    var waterDepth: f32;
    var blend: f32;
    var tint: f32;
    var pull: f32;
    var color: vec3<f32>;
    var local: f32;
    var n: vec2<f32>;
    var offset: vec2<f32>;
    var base: vec2<f32>;
    var d: f32;
    var local_1: f32;
    var intensity: f32;
    var local_2: vec3<f32>;
    var n_1: vec3<f32>;
    var i: i32 = 0i;
    var toLight: vec3<f32>;
    var d2_: f32;
    var intensity_1: f32;
    var ndotl: f32;

    let _e97 = aPosition_1;
    position = _e97;
    let _e100 = global.uViewProjection;
    let _e101 = position;
    gl_Position = (_e100 * vec4<f32>(_e101.x, _e101.y, _e101.z, 1f));
    let _e108 = position;
    let _e114 = global.uClipPlane;
    vClipDistance = dot(vec4<f32>(_e108.x, _e108.y, _e108.z, 1f), _e114);
    let _e116 = gl_Position;
    viewZ = _e116.w;
    let _e119 = aTexCoord_1;
    waterDepth = _e119.z;
    vWaterMix = 0f;
    vWaterNoiseCoord = vec3(0f);
    vPointLight = vec3(0f);
    let _e127 = global.uWaterMode;
    if (_e127 == 3i) {
        {
            let _e130 = viewZ;
            let _e131 = global.uStillFog;
            blend = ((_e130 - _e131.x) * 0.001953125f);
            let _e137 = waterDepth;
            let _e138 = global.uStillFog;
            let _e141 = global.uStillFog;
            tint = ((_e137 * _e138.z) + _e141.w);
            let _e145 = viewZ;
            let _e146 = global.uStillFog;
            let _e154 = waterDepth;
            pull = (clamp(((_e145 - _e146.y) * 0.00390625f), 0f, 1f) * _e154);
            let _e157 = blend;
            let _e158 = tint;
            blend = clamp((_e157 * _e158), 0f, 1f);
            let _e163 = tint;
            tint = clamp(_e163, 0f, 1f);
            let _e168 = position;
            let _e170 = pull;
            position.y = (_e168.y - _e170);
            let _e172 = global.uViewProjection;
            let _e173 = position;
            gl_Position = (_e172 * vec4<f32>(_e173.x, _e173.y, _e173.z, 1f));
            let _e180 = position;
            let _e186 = global.uClipPlane;
            vClipDistance = dot(vec4<f32>(_e180.x, _e180.y, _e180.z, 1f), _e186);
            let _e188 = blend;
            let _e189 = tint;
            vWaterMix = clamp(((max(_e188, _e189) - 0.25f) * 2f), 0f, 1f);
        }
    } else {
        let _e198 = global.uWaterMode;
        if (_e198 == 4i) {
            {
                let _e201 = aTexCoord_1;
                let _e203 = global.uTextureScale;
                let _e206 = ((_e201.xy * _e203) * 0.25f);
                let _e207 = global.uWaterTime;
                vWaterNoiseCoord = vec3<f32>(_e206.x, _e206.y, _e207);
                let _e211 = global.uRippleFog;
                if (_e211.y > 0f) {
                    let _e215 = viewZ;
                    let _e216 = global.uRippleFog;
                    let _e219 = global.uRippleFog;
                    vWaterMix = clamp(((_e215 - _e216.x) / _e219.y), 0f, 1f);
                }
            }
        }
    }
    let _e225 = aColor_1;
    color = _e225.xyz;
    let _e228 = position;
    vWorldXZ = _e228.xz;
    let _e230 = position;
    vWorldY = _e230.y;
    let _e232 = aTexCoord_1;
    let _e234 = global.uTextureScale;
    let _e236 = global.uTextureScroll;
    vTexCoord = ((_e232.xy * _e234) + _e236);
    let _e238 = global.uFogEnabled_u;
    if (_e238 != 0u) {
        let _e241 = global.uFogEnd;
        let _e242 = viewZ;
        let _e244 = global.uFogEnd;
        let _e245 = global.uFogStart;
        local = clamp(((_e241 - _e242) / max((_e244 - _e245), 0.0001f)), 0f, 1f);
    } else {
        local = 1f;
    }
    let _e255 = local;
    vFogFactor = _e255;
    let _e256 = global.uWaterMode;
    if (_e256 == 2i) {
        {
            let _e259 = position;
            let _e261 = position;
            let _e264 = position;
            let _e266 = position;
            n = (fract((vec2<f32>((_e259.x + _e261.z), (_e264.z - _e266.x)) * 0.00012207031f)) * 64f);
            let _e276 = n;
            let _e280 = global.uLapNoise[i32(_e276.x)];
            let _e282 = n;
            let _e286 = global.uLapNoise[i32(_e282.y)];
            let _e289 = global.uLapTurbulence;
            offset = (vec2<f32>(_e280.x, _e286.y) * _e289);
            let _e292 = aTexCoord_1;
            let _e294 = global.uTextureScale;
            let _e296 = global.uTextureScroll;
            base = ((_e292.xy * _e294) + _e296);
            let _e299 = base;
            let _e300 = offset;
            vTexCoord = (_e299 + _e300);
            let _e302 = base;
            let _e304 = (_e302 * 0.125f);
            let _e305 = global.uWaterTime;
            vWaterNoiseCoord = vec3<f32>(_e304.x, _e304.y, _e305);
            let _e309 = aColor_1;
            let _e311 = global.uLapAmbient;
            let _e317 = clamp((_e309.xyz * _e311), vec3(0f), vec3(1f));
            let _e318 = aColor_1;
            vColor = vec4<f32>(_e317.x, _e317.y, _e317.z, _e318.w);
            return;
        }
    }
    let _e324 = global.uRawColorDebug;
    if (_e324 == 1i) {
        {
            let _e327 = color;
            let _e328 = aColor_1;
            vColor = vec4<f32>(_e327.x, _e327.y, _e327.z, _e328.w);
            return;
        }
    }
    let _e334 = global.uHasNormals_u;
    if (_e334 != 0u) {
        {
            let _e337 = global.uSunDirection;
            let _e338 = aNormal_1;
            d = dot(_e337, _e338);
            let _e341 = global.uSunBrightness;
            let _e342 = d;
            let _e343 = d;
            if (_e343 > 0f) {
                let _e346 = global.uContrastPos;
                local_1 = _e346;
            } else {
                let _e347 = global.uContrastNeg;
                local_1 = _e347;
            }
            let _e349 = local_1;
            intensity = (_e341 + (_e342 * _e349));
            let _e353 = color;
            let _e354 = global.uSunColor;
            let _e356 = intensity;
            color = clamp(((_e353 * _e354) * _e356), vec3(0f), vec3(1f));
        }
    }
    let _e363 = global.uHasNormals_u;
    if (_e363 != 0u) {
        let _e366 = aNormal_1;
        local_2 = normalize(_e366);
    } else {
        local_2 = vec3<f32>(0f, 1f, 0f);
    }
    let _e373 = local_2;
    n_1 = _e373;
    loop {
        let _e377 = i;
        let _e378 = global.uPointLightCount;
        if !((_e377 < _e378)) {
            break;
        }
        {
            let _e384 = i;
            let _e386 = global.uPointLightPos[_e384];
            let _e388 = aPosition_1;
            toLight = (_e386.xyz - _e388);
            let _e391 = toLight;
            let _e392 = toLight;
            d2_ = dot(_e391, _e392);
            let _e396 = d2_;
            let _e397 = i;
            let _e399 = global.uPointLightPos[_e397];
            intensity_1 = max((1f - (_e396 * _e399.w)), 0f);
            let _e406 = intensity_1;
            if (_e406 <= 0f) {
                continue;
            }
            let _e409 = n_1;
            let _e410 = toLight;
            ndotl = max(dot(_e409, normalize(_e410)), 0f);
            let _e416 = vPointLight;
            let _e417 = i;
            let _e419 = global.uPointLightColor[_e417];
            let _e421 = ndotl;
            let _e423 = intensity_1;
            vPointLight = (_e416 + (_e419 * (min(((2f * _e421) * _e423), 1f) / 255f)));
        }
        continuing {
            let _e381 = i;
            i = (_e381 + 1i);
        }
    }
    let _e431 = color;
    let _e436 = clamp(_e431, vec3(0f), vec3(1f));
    let _e437 = aColor_1;
    vColor = vec4<f32>(_e436.x, _e436.y, _e436.z, _e437.w);
    return;
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aColor: vec4<f32>, @location(2) aTexCoord: vec3<f32>, @location(3) aNormal: vec3<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aColor_1 = aColor;
    aTexCoord_1 = aTexCoord;
    aNormal_1 = aNormal;
    main_1();
    let _e127 = vColor;
    let _e129 = vTexCoord;
    let _e131 = vFogFactor;
    let _e133 = vWorldXZ;
    let _e135 = vWaterNoiseCoord;
    let _e137 = vWaterMix;
    let _e139 = vWorldY;
    let _e141 = vPointLight;
    let _e143 = vClipDistance;
    let _e145 = gl_Position;
    return VertexOutput(_e127, _e129, _e131, _e133, _e135, _e137, _e139, _e141, _e143, _e145);
}
