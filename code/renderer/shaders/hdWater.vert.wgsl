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

struct VertexOutput {
    @location(0) vWorld: vec3<f32>,
    @location(1) vEdge: f32,
    @builtin(position) gl_Position: vec4<f32>,
}

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
var<private> aPosition_1: vec3<f32>;
var<private> aColor_1: vec4<f32>;
var<private> aTexCoord_1: vec3<f32>;
var<private> vWorld: vec3<f32>;
var<private> vEdge: f32;
var<private> gl_Position: vec4<f32>;

fn octave(xz: vec2<f32>, scale: f32, time: f32, shift: vec2<f32>) -> f32 {
    var xz_1: vec2<f32>;
    var scale_1: f32;
    var time_1: f32;
    var shift_1: vec2<f32>;

    xz_1 = xz;
    scale_1 = scale;
    time_1 = time;
    shift_1 = shift;
    let _e93 = xz_1;
    let _e94 = scale_1;
    let _e97 = shift_1;
    let _e98 = ((_e93 / vec2(_e94)) + _e97);
    let _e99 = time_1;
    let _e104 = textureSampleLevel(uWaveNoise, uWaveNoise_s, vec3<f32>(_e98.x, _e98.y, _e99), 0f);
    return _e104.x;
}

fn main_1() {
    var world: vec3<f32>;
    var edge: f32;
    var depth: f32;
    var p: vec2<f32>;
    var h: f32;
    var local: f32;
    var shore: f32;
    var distanceFade: f32;

    let _e85 = aPosition_1;
    world = _e85;
    let _e87 = aColor_1;
    edge = _e87.w;
    let _e90 = aTexCoord_1;
    depth = _e90.z;
    let _e93 = global.uHasWaveNoise_u;
    if (_e93 != 0u) {
        {
            let _e96 = world;
            let _e98 = global.uFlow;
            p = (_e96.xz + _e98);
            let _e101 = p;
            let _e102 = global.uWaveScale;
            let _e103 = global.uTime;
            let _e106 = octave(_e101, _e102, _e103, vec2(0f));
            let _e108 = p;
            let _e109 = global.uWaveScale;
            let _e112 = global.uTime;
            let _e121 = octave(_e108, (_e109 * 0.4f), fract(((_e112 * 2f) + 0.5f)), vec2<f32>(0.31f, 0.67f));
            h = ((_e106 + (0.5f * _e121)) - 0.75f);
            let _e127 = global.uHasWaterDepth_u;
            if (_e127 != 0u) {
                let _e130 = depth;
                local = clamp((_e130 / 32f), 0f, 1f);
            } else {
                local = 1f;
            }
            let _e138 = local;
            shore = _e138;
            let _e143 = global.uViewProjection;
            let _e144 = world;
            distanceFade = (1f - smoothstep(6000f, 14000f, (_e143 * vec4<f32>(_e144.x, _e144.y, _e144.z, 1f)).w));
            let _e156 = world;
            let _e158 = h;
            let _e159 = global.uWaveAmplitude;
            let _e161 = shore;
            let _e163 = edge;
            let _e170 = distanceFade;
            world.y = (_e156.y - ((((_e158 * _e159) * _e161) * clamp((_e163 * 2f), 0f, 1f)) * _e170));
        }
    }
    let _e173 = world;
    vWorld = _e173;
    let _e174 = edge;
    vEdge = _e174;
    let _e176 = global.uViewProjection;
    let _e177 = world;
    gl_Position = (_e176 * vec4<f32>(_e177.x, _e177.y, _e177.z, 1f));
    return;
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aColor: vec4<f32>, @location(2) aTexCoord: vec3<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aColor_1 = aColor;
    aTexCoord_1 = aTexCoord;
    main_1();
    let _e111 = vWorld;
    let _e113 = vEdge;
    let _e115 = gl_Position;
    return VertexOutput(_e111, _e113, _e115);
}
