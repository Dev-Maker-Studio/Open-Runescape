diagnostic(off, derivative_uniformity);

struct P {
    uViewProjection: mat4x4<f32>,
    uHasWaveNoise_u: u32,
    uWaveScale: f32,
    uTime: f32,
    uFlow: vec2<f32>,
    uWaveAmplitude: f32,
    uHasWaterDepth_u: u32,
    uUsePlanar_u: u32,
    uPlanarDistort: f32,
    uHasFoamNoise_u: u32,
    uHasEnvMap_u: u32,
    uShadowEnabled_u: u32,
    uInvViewProjection: mat4x4<f32>,
    uViewport: vec4<f32>,
    uFramebufferSize: vec2<f32>,
    uSunDirection: vec3<f32>,
    uSunColor: vec3<f32>,
    uAmbient: f32,
    uSkyColour: vec3<f32>,
    uWaterColour: vec3<f32>,
    uAbsorbDistance: f32,
    uScatter: f32,
    uRefraction: f32,
    uWaveStrength: f32,
    uSlowTime: f32,
    uShadowScale: f32,
    uReflectionSteps: i32,
    uFogEnabled_u: u32,
    uFogStart: f32,
    uFogEnd: f32,
    uPointLightCount: i32,
    uPointLightPos: array<vec4<f32>, 128>,
    uPointLightColor: array<vec3<f32>, 128>,
}

struct FragmentOutput {
    @location(0) fragColor: vec4<f32>,
}

const UP: vec3<f32> = vec3<f32>(0f, -1f, 0f);
const OCEAN: vec3<f32> = vec3<f32>(0.16f, 0.5f, 0.78f);
const REFLECTIVITY: f32 = 0.2f;

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uWaveNoise: texture_3d<f32>;
@group(1) @binding(1) 
var uWaveNoise_s: sampler;
@group(1) @binding(2) 
var uSceneColor: texture_2d<f32>;
@group(1) @binding(3) 
var uSceneColor_s: sampler;
@group(1) @binding(4) 
var uSceneDepth: texture_2d<f32>;
@group(1) @binding(5) 
var uSceneDepth_s: sampler;
@group(1) @binding(6) 
var uFoamNoise: texture_3d<f32>;
@group(1) @binding(7) 
var uFoamNoise_s: sampler;
@group(1) @binding(8) 
var uEnvMap: texture_cube<f32>;
@group(1) @binding(9) 
var uEnvMap_s: sampler;
@group(1) @binding(10) 
var uShadowTex: texture_2d<f32>;
@group(1) @binding(11) 
var uShadowTex_s: sampler;
@group(1) @binding(12) 
var uReflection: texture_2d<f32>;
@group(1) @binding(13) 
var uReflection_s: sampler;
var<private> vWorld_1: vec3<f32>;
var<private> vEdge_1: f32;
var<private> fragColor: vec4<f32>;
var<private> gl_FragCoord_1: vec4<f32>;

fn unproject(fragXY: vec2<f32>, depth: f32) -> vec3<f32> {
    var fragXY_1: vec2<f32>;
    var depth_1: f32;
    var ndc: vec2<f32>;
    var world: vec4<f32>;

    fragXY_1 = fragXY;
    depth_1 = depth;
    let _e90 = fragXY_1;
    let _e91 = global.uViewport;
    let _e94 = global.uViewport;
    ndc = ((((_e90 - _e91.xy) / _e94.zw) * 2f) - vec2(1f));
    let _e103 = global.uInvViewProjection;
    let _e104 = ndc;
    let _e105 = depth_1;
    world = (_e103 * vec4<f32>(_e104.x, _e104.y, ((_e105 * 2f) - 1f), 1f));
    let _e116 = world;
    let _e118 = world;
    return (_e116.xyz / vec3(_e118.w));
}

fn project(world_1: vec3<f32>, screen: ptr<function, vec3<f32>>) -> bool {
    var world_2: vec3<f32>;
    var clip: vec4<f32>;
    var ndc_1: vec3<f32>;

    world_2 = world_1;
    let _e89 = global.uViewProjection;
    let _e90 = world_2;
    clip = (_e89 * vec4<f32>(_e90.x, _e90.y, _e90.z, 1f));
    let _e98 = clip;
    if (_e98.w <= 0f) {
        return false;
    }
    let _e103 = clip;
    let _e105 = clip;
    ndc_1 = (_e103.xyz / vec3(_e105.w));
    let _e110 = ndc_1;
    let _e117 = global.uViewport;
    let _e120 = global.uViewport;
    let _e122 = ((((_e110.xy * 0.5f) + vec2(0.5f)) * _e117.zw) + _e120.xy);
    let _e123 = ndc_1;
    (*screen) = vec3<f32>(_e122.x, _e122.y, ((_e123.z * 0.5f) + 0.5f));
    let _e132 = (*screen);
    let _e134 = global.uViewport;
    let _e138 = (*screen);
    let _e140 = global.uViewport;
    let _e142 = global.uViewport;
    return (all((_e132.xy >= _e134.xy)) && all((_e138.xy <= (_e140.xy + _e142.zw))));
}

fn sceneDepth(fragXY_2: vec2<f32>) -> f32 {
    var fragXY_3: vec2<f32>;

    fragXY_3 = fragXY_2;
    let _e88 = fragXY_3;
    let _e90 = global.uFramebufferSize;
    let _e92 = fragXY_3;
    let _e96 = global.uFramebufferSize;
    let _e98 = textureSample(uSceneDepth, uSceneDepth_s, (vec2<f32>(_e88.x, (_e90.y - _e92.y)) / _e96));
    return _e98.x;
}

fn thicknessAt(fragXY_4: vec2<f32>, surface: vec3<f32>, ray: vec3<f32>) -> f32 {
    var fragXY_5: vec2<f32>;
    var surface_1: vec3<f32>;
    var ray_1: vec3<f32>;
    var depth_2: f32;

    fragXY_5 = fragXY_4;
    surface_1 = surface;
    ray_1 = ray;
    let _e92 = fragXY_5;
    let _e93 = sceneDepth(_e92);
    depth_2 = _e93;
    let _e95 = depth_2;
    if (_e95 >= 1f) {
        return 1000000f;
    }
    let _e99 = fragXY_5;
    let _e100 = depth_2;
    let _e101 = unproject(_e99, _e100);
    let _e102 = surface_1;
    let _e104 = ray_1;
    return dot((_e101 - _e102), _e104);
}

fn octave(xz: vec2<f32>, scale: f32, time: f32, shift: vec2<f32>) -> f32 {
    var xz_1: vec2<f32>;
    var scale_1: f32;
    var time_1: f32;
    var shift_1: vec2<f32>;

    xz_1 = xz;
    scale_1 = scale;
    time_1 = time;
    shift_1 = shift;
    let _e94 = xz_1;
    let _e95 = scale_1;
    let _e98 = shift_1;
    let _e99 = ((_e94 / vec2(_e95)) + _e98);
    let _e100 = time_1;
    let _e105 = textureSampleLevel(uWaveNoise, uWaveNoise_s, vec3<f32>(_e99.x, _e99.y, _e100), 0f);
    return _e105.x;
}

fn waveHeight(xz_2: vec2<f32>, detail: f32) -> f32 {
    var xz_3: vec2<f32>;
    var detail_1: f32;
    var p: vec2<f32>;

    xz_3 = xz_2;
    detail_1 = detail;
    let _e90 = xz_3;
    let _e91 = global.uFlow;
    p = (_e90 + _e91);
    let _e94 = p;
    let _e95 = global.uWaveScale;
    let _e96 = global.uTime;
    let _e99 = octave(_e94, _e95, _e96, vec2(0f));
    let _e101 = p;
    let _e102 = global.uWaveScale;
    let _e105 = global.uTime;
    let _e114 = octave(_e101, (_e102 * 0.4f), fract(((_e105 * 2f) + 0.5f)), vec2<f32>(0.31f, 0.67f));
    let _e117 = detail_1;
    let _e120 = p;
    let _e121 = global.uWaveScale;
    let _e124 = global.uTime;
    let _e133 = octave(_e120, (_e121 * 0.1f), fract(((_e124 * 3f) + 0.25f)), vec2<f32>(0.73f, 0.19f));
    return ((_e99 + (0.5f * _e114)) + ((_e117 * 0.3f) * _e133));
}

fn waterNormal(xz_4: vec2<f32>, distance_: f32) -> vec3<f32> {
    var xz_5: vec2<f32>;
    var distance_1: f32;
    var detail_2: f32;
    var strength: f32;
    var e: f32;
    var h: f32;
    var dx: f32;
    var dz: f32;

    xz_5 = xz_4;
    distance_1 = distance_;
    let _e90 = global.uHasWaveNoise_u;
    if !((_e90 != 0u)) {
        return UP;
    }
    let _e97 = distance_1;
    detail_2 = (1f - smoothstep(1500f, 6000f, _e97));
    let _e101 = global.uWaveStrength;
    let _e103 = distance_1;
    strength = (_e101 / (1f + (_e103 / 15000f)));
    let _e109 = global.uWaveScale;
    e = (_e109 / 256f);
    let _e113 = xz_5;
    let _e114 = detail_2;
    let _e115 = waveHeight(_e113, _e114);
    h = _e115;
    let _e117 = xz_5;
    let _e118 = e;
    let _e122 = detail_2;
    let _e123 = waveHeight((_e117 + vec2<f32>(_e118, 0f)), _e122);
    let _e124 = h;
    let _e126 = e;
    let _e128 = global.uWaveScale;
    dx = ((((_e123 - _e124) / _e126) * _e128) * 0.05f);
    let _e133 = xz_5;
    let _e135 = e;
    let _e138 = detail_2;
    let _e139 = waveHeight((_e133 + vec2<f32>(0f, _e135)), _e138);
    let _e140 = h;
    let _e142 = e;
    let _e144 = global.uWaveScale;
    dz = ((((_e139 - _e140) / _e142) * _e144) * 0.05f);
    let _e149 = dx;
    let _e151 = strength;
    let _e155 = dz;
    let _e157 = strength;
    return normalize(vec3<f32>((-(_e149) * _e151), -1f, (-(_e155) * _e157)));
}

fn caustic(xz_6: vec2<f32>) -> f32 {
    var xz_7: vec2<f32>;
    var a: f32;
    var b: f32;
    var ridge: f32;

    xz_7 = xz_6;
    let _e88 = global.uHasWaveNoise_u;
    if !((_e88 != 0u)) {
        return 0f;
    }
    let _e93 = xz_7;
    let _e94 = global.uFlow;
    let _e96 = global.uWaveScale;
    let _e99 = global.uTime;
    let _e103 = octave((_e93 + _e94), (_e96 * 0.5f), _e99, vec2<f32>(0.13f, 0.41f));
    a = _e103;
    let _e105 = xz_7;
    let _e106 = global.uFlow;
    let _e108 = global.uWaveScale;
    let _e111 = global.uTime;
    let _e120 = octave((_e105 - _e106), (_e108 * 0.4f), fract(((_e111 * 2f) + 0.33f)), vec2<f32>(0.57f, 0.08f));
    b = _e120;
    let _e123 = a;
    let _e124 = b;
    ridge = (1f - (abs((_e123 - _e124)) * 3f));
    let _e131 = ridge;
    return pow(clamp(_e131, 0f, 1f), 3f);
}

fn skyGradient(direction: vec3<f32>) -> vec3<f32> {
    var direction_1: vec3<f32>;
    var up: f32;
    var zenith: vec3<f32>;

    direction_1 = direction;
    let _e88 = direction_1;
    up = clamp(dot(_e88, UP), 0f, 1f);
    let _e94 = global.uSkyColour;
    zenith = mix(_e94, vec3<f32>(0.28f, 0.45f, 0.72f), vec3(0.55f));
    let _e103 = global.uSkyColour;
    let _e106 = zenith;
    let _e107 = up;
    return mix((_e103 * 1.05f), _e106, vec3(pow(_e107, 0.6f)));
}

fn traceReflection(origin: vec3<f32>, direction_2: vec3<f32>, weight: ptr<function, f32>) -> vec3<f32> {
    var origin_1: vec3<f32>;
    var direction_3: vec3<f32>;
    var position: vec3<f32>;
    var stepLength: f32 = 48f;
    var i: i32 = 0i;
    var previous: vec3<f32>;
    var screen_1: vec3<f32>;
    var depth_3: f32;
    var surface_2: vec3<f32>;
    var near: vec3<f32>;
    var far: vec3<f32>;
    var j: i32;
    var middle: vec3<f32>;
    var s: vec3<f32>;
    var hit: vec3<f32>;
    var border: vec2<f32>;

    origin_1 = origin;
    direction_3 = direction_2;
    (*weight) = 0f;
    let _e92 = origin_1;
    position = _e92;
    loop {
        let _e98 = i;
        let _e99 = global.uReflectionSteps;
        if !((_e98 < _e99)) {
            break;
        }
        {
            let _e105 = position;
            previous = _e105;
            let _e107 = position;
            let _e108 = direction_3;
            let _e109 = stepLength;
            position = (_e107 + (_e108 * _e109));
            let _e112 = stepLength;
            stepLength = (_e112 * 1.15f);
            let _e116 = position;
            let _e119 = project(_e116, (&screen_1));
            if !(_e119) {
                break;
            }
            let _e121 = screen_1;
            let _e123 = sceneDepth(_e121.xy);
            depth_3 = _e123;
            let _e125 = depth_3;
            let _e128 = screen_1;
            let _e130 = depth_3;
            if ((_e125 >= 1f) || (_e128.z <= _e130)) {
                continue;
            }
            let _e133 = screen_1;
            let _e135 = depth_3;
            let _e136 = unproject(_e133.xy, _e135);
            surface_2 = _e136;
            let _e138 = surface_2;
            let _e139 = position;
            let _e141 = stepLength;
            if (distance(_e138, _e139) > (_e141 * 3f)) {
                continue;
            }
            let _e145 = previous;
            near = _e145;
            let _e147 = position;
            far = _e147;
            j = 0i;
            loop {
                let _e151 = j;
                if !((_e151 < 6i)) {
                    break;
                }
                {
                    let _e158 = near;
                    let _e159 = far;
                    middle = ((_e158 + _e159) * 0.5f);
                    let _e165 = middle;
                    let _e168 = project(_e165, (&s));
                    let _e169 = s;
                    let _e171 = s;
                    let _e173 = sceneDepth(_e171.xy);
                    if (_e168 && (_e169.z > _e173)) {
                        let _e176 = middle;
                        far = _e176;
                    } else {
                        let _e177 = middle;
                        near = _e177;
                    }
                }
                continuing {
                    let _e155 = j;
                    j = (_e155 + 1i);
                }
            }
            let _e179 = far;
            let _e182 = project(_e179, (&hit));
            if !(_e182) {
                break;
            }
            let _e184 = hit;
            let _e186 = global.uViewport;
            let _e189 = global.uViewport;
            let _e191 = global.uViewport;
            let _e194 = hit;
            let _e198 = global.uViewport;
            border = (min((_e184.xy - _e186.xy), ((_e189.xy + _e191.zw) - _e194.xy)) / (_e198.zw * 0.08f));
            let _e204 = border;
            let _e206 = border;
            let _e213 = i;
            let _e215 = global.uReflectionSteps;
            (*weight) = (clamp(min(_e204.x, _e206.y), 0f, 1f) * (1f - (f32(_e213) / f32(_e215))));
            let _e220 = hit;
            let _e223 = global.uFramebufferSize;
            let _e225 = hit;
            let _e230 = global.uFramebufferSize;
            let _e232 = textureSample(uSceneColor, uSceneColor_s, (vec2<f32>(_e220.x, (_e223.y - _e225.y)) / _e230));
            return _e232.xyz;
        }
        continuing {
            let _e102 = i;
            i = (_e102 + 1i);
        }
    }
    return vec3(0f);
}

fn main_1() {
    var eye: vec3<f32>;
    var ray_2: vec3<f32>;
    var viewDistance: f32;
    var n: vec3<f32>;
    var toEye: vec3<f32>;
    var local: f32;
    var shadow: f32;
    var sunLit: f32;
    var sunHeight: f32;
    var sunLight: vec3<f32>;
    var ambientLight: vec3<f32>;
    var thickness: f32;
    var shore: f32;
    var refracted: vec2<f32>;
    var refractedThickness: f32;
    var behind: vec3<f32>;
    var bedDepth: f32;
    var bed: vec3<f32>;
    var below: f32;
    var light: f32;
    var extinction: vec3<f32>;
    var transmittance: vec3<f32>;
    var peak: f32;
    var low: f32;
    var saturation: f32;
    var scatterColour: vec3<f32>;
    var deep: f32;
    var waveLight: f32;
    var local_1: f32;
    var patches: f32;
    var inScatter: vec3<f32>;
    var shafts: f32 = 0f;
    var reach: f32;
    var i_1: i32 = 1i;
    var p_1: vec3<f32>;
    var below_1: f32;
    var lightSpecular: vec3<f32> = vec3(0f);
    var i_2: i32 = 0i;
    var toLight: vec3<f32>;
    var attenuation: f32;
    var l: vec3<f32>;
    var h_1: vec3<f32>;
    var body: vec3<f32>;
    var reflected: vec3<f32>;
    var local_2: vec3<f32>;
    var sky: vec3<f32>;
    var reflection: vec3<f32>;
    var planar: vec4<f32>;
    var traced: f32 = 0f;
    var local_3: vec3<f32>;
    var scene: vec3<f32>;
    var fresnel: f32;
    var halfway: vec3<f32>;
    var highlight: f32;
    var glancing: f32;
    var specular: vec3<f32>;
    var colour: vec3<f32>;
    var crest: f32;
    var spray: f32;
    var whitecap: f32;
    var band: f32;
    var breakup: f32;
    var lines: f32;
    var foam: f32;
    var fog: f32;

    let _e87 = gl_FragCoord_1;
    let _e89 = global.uFramebufferSize;
    let _e91 = gl_FragCoord_1;
    let _e96 = unproject(vec2<f32>(_e87.x, (_e89.y - _e91.y)), 0f);
    eye = _e96;
    let _e98 = vWorld_1;
    let _e99 = eye;
    ray_2 = normalize((_e98 - _e99));
    let _e103 = vWorld_1;
    let _e104 = eye;
    viewDistance = distance(_e103, _e104);
    let _e107 = vWorld_1;
    let _e109 = viewDistance;
    let _e110 = waterNormal(_e107.xz, _e109);
    n = _e110;
    let _e112 = ray_2;
    toEye = -(_e112);
    let _e115 = global.uShadowEnabled_u;
    if (_e115 != 0u) {
        let _e118 = vWorld_1;
        let _e120 = global.uShadowScale;
        let _e122 = textureSample(uShadowTex, uShadowTex_s, (_e118.xz * _e120));
        local = _e122.x;
    } else {
        local = 0f;
    }
    let _e126 = local;
    shadow = _e126;
    let _e129 = shadow;
    sunLit = (1f - _e129);
    let _e132 = global.uSunDirection;
    sunHeight = clamp(dot(_e132, UP), 0f, 1f);
    let _e138 = global.uSunColor;
    let _e141 = sunHeight;
    let _e145 = sunLit;
    sunLight = ((_e138 * (0.35f + (0.65f * _e141))) * _e145);
    let _e148 = global.uSkyColour;
    let _e151 = global.uSunColor;
    let _e152 = global.uAmbient;
    ambientLight = ((_e148 * 0.35f) + ((_e151 * _e152) * 0.4f));
    let _e158 = gl_FragCoord_1;
    let _e160 = global.uFramebufferSize;
    let _e162 = gl_FragCoord_1;
    let _e166 = vWorld_1;
    let _e167 = ray_2;
    let _e168 = thicknessAt(vec2<f32>(_e158.x, (_e160.y - _e162.y)), _e166, _e167);
    thickness = max(_e168, 0f);
    let _e174 = thickness;
    shore = smoothstep(0f, 64f, _e174);
    let _e177 = gl_FragCoord_1;
    let _e179 = global.uFramebufferSize;
    let _e181 = gl_FragCoord_1;
    let _e185 = n;
    let _e187 = global.uRefraction;
    let _e189 = thickness;
    refracted = (vec2<f32>(_e177.x, (_e179.y - _e181.y)) + ((_e185.xz * _e187) * clamp((_e189 / 256f), 0f, 1f)));
    let _e198 = refracted;
    let _e199 = vWorld_1;
    let _e200 = ray_2;
    let _e201 = thicknessAt(_e198, _e199, _e200);
    refractedThickness = _e201;
    let _e203 = refractedThickness;
    if (_e203 < 0f) {
        {
            let _e206 = gl_FragCoord_1;
            let _e208 = global.uFramebufferSize;
            let _e210 = gl_FragCoord_1;
            refracted = vec2<f32>(_e206.x, (_e208.y - _e210.y));
            let _e214 = thickness;
            refractedThickness = _e214;
        }
    }
    let _e215 = refractedThickness;
    refractedThickness = max(_e215, 0f);
    let _e218 = refracted;
    let _e220 = global.uFramebufferSize;
    let _e222 = refracted;
    let _e226 = global.uFramebufferSize;
    let _e228 = textureSample(uSceneColor, uSceneColor_s, (vec2<f32>(_e218.x, (_e220.y - _e222.y)) / _e226));
    behind = _e228.xyz;
    let _e231 = refracted;
    let _e232 = sceneDepth(_e231);
    bedDepth = _e232;
    let _e234 = bedDepth;
    if (_e234 < 1f) {
        {
            let _e237 = refracted;
            let _e238 = bedDepth;
            let _e239 = unproject(_e237, _e238);
            bed = _e239;
            let _e241 = bed;
            let _e243 = vWorld_1;
            below = max((_e241.y - _e243.y), 0f);
            let _e249 = bed;
            let _e251 = global.uSunDirection;
            let _e253 = below;
            let _e256 = caustic((_e249.xz + (_e251.xz * _e253)));
            let _e257 = below;
            let _e259 = global.uAbsorbDistance;
            let _e265 = below;
            light = ((_e256 * exp((-(_e257) / _e259))) * smoothstep(16f, 96f, _e265));
            let _e269 = behind;
            let _e270 = behind;
            let _e271 = sunLight;
            let _e273 = light;
            behind = (_e269 + (((_e270 * _e271) * _e273) * 0.45f));
        }
    }
    let _e282 = global.uAbsorbDistance;
    extinction = (vec3<f32>(1.6f, 1.05f, 0.8f) / vec3(_e282));
    let _e286 = extinction;
    let _e288 = refractedThickness;
    transmittance = exp((-(_e286) * _e288));
    let _e292 = global.uWaterColour;
    let _e294 = global.uWaterColour;
    let _e297 = global.uWaterColour;
    peak = max(max(_e292.x, _e294.y), max(_e297.z, 0.05f));
    let _e303 = global.uWaterColour;
    let _e305 = global.uWaterColour;
    let _e308 = global.uWaterColour;
    low = min(min(_e303.x, _e305.y), _e308.z);
    let _e312 = peak;
    let _e313 = low;
    let _e315 = peak;
    saturation = clamp(((_e312 - _e313) / _e315), 0f, 1f);
    let _e321 = global.uWaterColour;
    let _e322 = peak;
    let _e325 = saturation;
    let _e328 = global.uScatter;
    scatterColour = (mix(OCEAN, (_e321 / vec3(_e322)), vec3(_e325)) * _e328);
    let _e332 = refractedThickness;
    let _e334 = global.uAbsorbDistance;
    deep = (1f - exp((-(_e332) / (_e334 * 2f))));
    let _e341 = scatterColour;
    let _e344 = deep;
    scatterColour = (_e341 * mix(1f, 0.42f, _e344));
    let _e349 = n;
    let _e350 = global.uSunDirection;
    waveLight = (0.7f + (0.6f * max(dot(_e349, _e350), 0f)));
    let _e357 = global.uHasWaveNoise_u;
    if (_e357 != 0u) {
        let _e362 = vWorld_1;
        let _e364 = global.uWaveScale;
        let _e367 = global.uSlowTime;
        let _e371 = octave(_e362.xz, (_e364 * 4f), _e367, vec2<f32>(0.21f, 0.83f));
        local_1 = (0.8f + (0.4f * _e371));
    } else {
        local_1 = 1f;
    }
    let _e376 = local_1;
    patches = _e376;
    let _e378 = scatterColour;
    let _e379 = ambientLight;
    let _e380 = sunLight;
    let _e385 = waveLight;
    let _e387 = patches;
    inScatter = (((_e378 * (_e379 + (_e380 * 0.8f))) * _e385) * _e387);
    let _e392 = refractedThickness;
    let _e393 = global.uAbsorbDistance;
    reach = min(_e392, (_e393 * 2f));
    loop {
        let _e400 = i_1;
        if !((_e400 <= 3i)) {
            break;
        }
        {
            let _e407 = vWorld_1;
            let _e408 = ray_2;
            let _e409 = reach;
            let _e411 = i_1;
            p_1 = (_e407 + ((_e408 * _e409) * (f32(_e411) / 4f)));
            let _e418 = p_1;
            let _e420 = vWorld_1;
            below_1 = max((_e418.y - _e420.y), 0f);
            let _e426 = shafts;
            let _e427 = p_1;
            let _e429 = global.uSunDirection;
            let _e431 = below_1;
            let _e434 = caustic((_e427.xz + (_e429.xz * _e431)));
            let _e435 = below_1;
            let _e437 = global.uAbsorbDistance;
            shafts = (_e426 + (_e434 * exp((-(_e435) / _e437))));
        }
        continuing {
            let _e404 = i_1;
            i_1 = (_e404 + 1i);
        }
    }
    let _e442 = inScatter;
    let _e443 = scatterColour;
    let _e444 = sunLight;
    let _e446 = shafts;
    inScatter = (_e442 + (((_e443 * _e444) * _e446) * 0.25f));
    loop {
        let _e456 = i_2;
        let _e457 = global.uPointLightCount;
        if !((_e456 < _e457)) {
            break;
        }
        {
            let _e463 = i_2;
            let _e465 = global.uPointLightPos[_e463];
            let _e467 = vWorld_1;
            toLight = (_e465.xyz - _e467);
            let _e471 = i_2;
            let _e473 = global.uPointLightPos[_e471];
            let _e475 = toLight;
            let _e476 = toLight;
            attenuation = (1f / max((_e473.w * dot(_e475, _e476)), 0.000001f));
            let _e483 = toLight;
            l = normalize(_e483);
            let _e486 = inScatter;
            let _e487 = scatterColour;
            let _e488 = i_2;
            let _e490 = global.uPointLightColor[_e488];
            let _e492 = attenuation;
            inScatter = (_e486 + (((_e487 * _e490) * _e492) * 0.5f));
            let _e497 = l;
            let _e498 = toEye;
            h_1 = normalize((_e497 + _e498));
            let _e502 = lightSpecular;
            let _e503 = i_2;
            let _e505 = global.uPointLightColor[_e503];
            let _e506 = attenuation;
            let _e508 = n;
            let _e509 = h_1;
            lightSpecular = (_e502 + ((_e505 * _e506) * pow(max(dot(_e508, _e509), 0f), 96f)));
        }
        continuing {
            let _e460 = i_2;
            i_2 = (_e460 + 1i);
        }
    }
    let _e517 = behind;
    let _e518 = transmittance;
    let _e520 = inScatter;
    let _e523 = transmittance;
    body = ((_e517 * _e518) + (_e520 * (vec3(1f) - _e523)));
    let _e528 = ray_2;
    let _e529 = n;
    reflected = reflect(_e528, _e529);
    let _e532 = global.uHasEnvMap_u;
    if (_e532 != 0u) {
        let _e535 = reflected;
        let _e537 = reflected;
        let _e540 = reflected;
        let _e543 = textureSample(uEnvMap, uEnvMap_s, vec3<f32>(_e535.x, -(_e537.y), _e540.z));
        local_2 = _e543.xyz;
    } else {
        let _e545 = reflected;
        let _e546 = skyGradient(_e545);
        local_2 = _e546;
    }
    let _e548 = local_2;
    sky = _e548;
    let _e551 = global.uUsePlanar_u;
    if (_e551 != 0u) {
        {
            let _e554 = gl_FragCoord_1;
            let _e556 = global.uFramebufferSize;
            let _e558 = gl_FragCoord_1;
            let _e563 = global.uFramebufferSize;
            let _e565 = gl_FragCoord_1;
            let _e567 = global.uFramebufferSize;
            let _e569 = gl_FragCoord_1;
            let _e576 = global.uFramebufferSize;
            let _e578 = n;
            let _e580 = global.uPlanarDistort;
            let _e583 = textureSample(uReflection, uReflection_s, ((vec2<f32>(vec2<f32>(_e554.x, (_e556.y - _e558.y)).x, (_e563.y - vec2<f32>(_e565.x, (_e567.y - _e569.y)).y)) / _e576) + (_e578.xz * _e580)));
            planar = _e583;
            let _e585 = sky;
            let _e586 = planar;
            let _e590 = planar;
            let _e597 = shore;
            reflection = mix(_e585, (_e586.xyz * 0.85f), vec3((clamp((_e590.w * 4f), 0f, 1f) * _e597)));
        }
    } else {
        {
            let _e603 = global.uReflectionSteps;
            if (_e603 > 0i) {
                let _e606 = vWorld_1;
                let _e607 = reflected;
                let _e610 = traceReflection(_e606, _e607, (&traced));
                local_3 = _e610;
            } else {
                local_3 = vec3(0f);
            }
            let _e614 = local_3;
            scene = _e614;
            let _e616 = sky;
            let _e617 = scene;
            let _e620 = traced;
            let _e623 = shore;
            reflection = mix(_e616, (_e617 * 0.75f), vec3(((_e620 * 0.7f) * _e623)));
        }
    }
    let _e629 = toEye;
    let _e630 = n;
    let _e641 = shore;
    fresnel = (mix(REFLECTIVITY, 1f, pow((1f - clamp(dot(_e629, _e630), 0f, 1f)), 5f)) * mix(0.35f, 1f, _e641));
    let _e645 = global.uSunDirection;
    let _e646 = toEye;
    halfway = normalize((_e645 + _e646));
    let _e650 = n;
    let _e651 = halfway;
    highlight = max(dot(_e650, _e651), 0f);
    let _e659 = toEye;
    let _e660 = halfway;
    glancing = (0.02f + (0.98f * pow((1f - clamp(dot(_e659, _e660), 0f, 1f)), 5f)));
    let _e671 = global.uSunColor;
    let _e672 = sunLit;
    let _e674 = glancing;
    let _e676 = highlight;
    let _e681 = highlight;
    specular = (((_e671 * _e672) * _e674) * ((pow(_e676, 512f) * 1.5f) + (pow(_e681, 4096f) * 4f)));
    let _e689 = body;
    let _e690 = reflection;
    let _e691 = fresnel;
    let _e694 = specular;
    let _e696 = lightSpecular;
    colour = ((mix(_e689, _e690, vec3(_e691)) + _e694) + _e696);
    let _e699 = global.uHasWaveNoise_u;
    let _e702 = global.uHasFoamNoise_u;
    if ((_e699 != 0u) && (_e702 != 0u)) {
        {
            let _e708 = vWorld_1;
            let _e711 = waveHeight(_e708.xz, 1f);
            crest = smoothstep(1.25f, 1.55f, _e711);
            let _e714 = vWorld_1;
            let _e716 = global.uWaveScale;
            let _e720 = (_e714.xz / vec2((_e716 * 0.125f)));
            let _e721 = global.uTime;
            let _e725 = textureSample(uFoamNoise, uFoamNoise_s, vec3<f32>(_e720.x, _e720.y, _e721));
            spray = _e725.x;
            let _e728 = crest;
            let _e731 = spray;
            let _e734 = deep;
            let _e739 = viewDistance;
            whitecap = (((_e728 * smoothstep(0.4f, 0.8f, _e731)) * _e734) * (1f - smoothstep(4000f, 12000f, _e739)));
            let _e744 = colour;
            let _e747 = ambientLight;
            let _e748 = sunLight;
            let _e753 = whitecap;
            colour = mix(_e744, (vec3(0.9f) * (_e747 + (_e748 * 0.6f))), vec3((_e753 * 0.35f)));
        }
    }
    let _e758 = global.uHasFoamNoise_u;
    if (_e758 != 0u) {
        {
            let _e764 = thickness;
            band = (1f - smoothstep(4f, 72f, _e764));
            let _e768 = vWorld_1;
            let _e770 = global.uFlow;
            let _e772 = global.uWaveScale;
            let _e776 = ((_e768.xz + _e770) / vec2((_e772 * 0.25f)));
            let _e777 = global.uTime;
            let _e781 = textureSample(uFoamNoise, uFoamNoise_s, vec3<f32>(_e776.x, _e776.y, _e777));
            breakup = _e781.x;
            let _e786 = thickness;
            let _e789 = global.uTime;
            let _e795 = breakup;
            lines = (0.5f + (0.5f * sin((((_e786 * 0.12f) + ((_e789 * 6.2831855f) * 2f)) + (_e795 * 6f)))));
            let _e803 = band;
            let _e806 = lines;
            let _e811 = breakup;
            foam = ((_e803 * smoothstep(0.7f, 0.95f, _e806)) * smoothstep(0.35f, 0.75f, _e811));
            let _e815 = colour;
            let _e818 = ambientLight;
            let _e819 = sunLight;
            let _e824 = foam;
            colour = mix(_e815, (vec3(0.85f) * (_e818 + (_e819 * 0.5f))), vec3((_e824 * 0.25f)));
        }
    }
    let _e829 = global.uFogEnabled_u;
    if (_e829 != 0u) {
        {
            let _e833 = gl_FragCoord_1;
            let _e836 = global.uFogStart;
            let _e838 = global.uFogEnd;
            let _e839 = global.uFogStart;
            fog = clamp((((1f / _e833.w) - _e836) / max((_e838 - _e839), 1f)), 0f, 1f);
            let _e848 = colour;
            let _e849 = global.uSkyColour;
            let _e850 = fog;
            colour = mix(_e848, _e849, vec3(_e850));
        }
    }
    let _e853 = colour;
    let _e854 = vEdge_1;
    let _e857 = thickness;
    fragColor = vec4<f32>(_e853.x, _e853.y, _e853.z, (_e854 * smoothstep(0f, 24f, _e857)));
    return;
}

@fragment 
fn main(@location(0) vWorld: vec3<f32>, @location(1) vEdge: f32, @builtin(position) gl_FragCoord: vec4<f32>) -> FragmentOutput {
    vWorld_1 = vWorld;
    vEdge_1 = vEdge;
    gl_FragCoord_1 = gl_FragCoord;
    main_1();
    let _e113 = fragColor;
    return FragmentOutput(_e113);
}
