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

struct FragmentOutput {
    @location(0) fragColor: vec4<f32>,
}

const COS_22_5_: f32 = 0.92388f;
const SIN_22_5_: f32 = 0.38268f;

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uTexture: texture_2d<f32>;
@group(1) @binding(1) 
var uTexture_s: sampler;
var<private> vTexCoord_1: vec2<f32>;
var<private> vColor_1: vec4<f32>;
var<private> vNormal_1: vec3<f32>;
var<private> vViewPos_1: vec3<f32>;
var<private> vFogFactor_1: f32;
var<private> fragColor: vec4<f32>;

fn main_1() {
    var baseColor: vec4<f32>;
    var texColor: vec4<f32>;
    var cubeDir: vec3<f32>;
    var rotatedDir: vec3<f32>;
    var specularBase: f32;
    var specExponent: f32;
    var local: f32;
    var scale: f32;
    var specular: f32;
    var specSharp: f32;
    var specMedium: f32;

    let _e45 = global.uTextureEnabled_u;
    if (_e45 != 0u) {
        {
            let _e48 = vTexCoord_1;
            let _e49 = textureSample(uTexture, uTexture_s, _e48);
            texColor = _e49;
            let _e51 = texColor;
            let _e52 = vColor_1;
            baseColor = (_e51 * _e52);
        }
    } else {
        {
            let _e54 = vColor_1;
            baseColor = _e54;
        }
    }
    let _e55 = baseColor;
    let _e57 = global.uAlphaThreshold;
    if (_e55.w <= _e57) {
        {
            discard;
        }
    }
    let _e59 = global.uEffectEnabled_u;
    if (_e59 != 0u) {
        {
            let _e62 = vNormal_1;
            cubeDir = normalize(_e62);
            let _e67 = cubeDir;
            rotatedDir.x = _e67.x;
            let _e70 = cubeDir;
            let _e73 = cubeDir;
            rotatedDir.y = ((_e70.y * COS_22_5_) - (_e73.z * SIN_22_5_));
            let _e78 = cubeDir;
            let _e81 = cubeDir;
            rotatedDir.z = ((_e78.y * SIN_22_5_) + (_e81.z * COS_22_5_));
            let _e86 = rotatedDir;
            let _e89 = rotatedDir;
            let _e92 = rotatedDir;
            specularBase = max(0f, max(abs(_e86.x), max(abs(_e89.y), abs(_e92.z))));
            let _e100 = global.uLimitedTextureUnits_u;
            if (_e100 != 0u) {
                local = 0.1882353f;
            } else {
                local = 0.49803922f;
            }
            let _e110 = local;
            scale = _e110;
            let _e112 = global.uSpecularLevel;
            if (_e112 == 1i) {
                {
                    specExponent = 96f;
                }
            } else {
                let _e116 = global.uSpecularLevel;
                if (_e116 == 2i) {
                    {
                        specExponent = 36f;
                    }
                } else {
                    {
                        specExponent = 12f;
                    }
                }
            }
            let _e121 = specularBase;
            let _e122 = specExponent;
            let _e124 = scale;
            specular = (pow(_e121, _e122) * _e124);
            let _e127 = global.uLimitedTextureUnits_u;
            if (_e127 != 0u) {
                {
                    let _e130 = baseColor;
                    let _e132 = baseColor;
                    let _e134 = specular;
                    let _e136 = (_e132.xyz + vec3(_e134));
                    baseColor.x = _e136.x;
                    baseColor.y = _e136.y;
                    baseColor.z = _e136.z;
                }
            } else {
                {
                    let _e143 = specularBase;
                    let _e146 = scale;
                    specSharp = (pow(_e143, 96f) * _e146);
                    let _e149 = specularBase;
                    let _e152 = scale;
                    specMedium = (pow(_e149, 36f) * _e152);
                    let _e155 = baseColor;
                    let _e157 = baseColor;
                    let _e159 = specSharp;
                    let _e160 = specMedium;
                    let _e163 = (_e157.xyz + vec3((_e159 + _e160)));
                    baseColor.x = _e163.x;
                    baseColor.y = _e163.y;
                    baseColor.z = _e163.z;
                }
            }
        }
    }
    let _e170 = global.uFogColor;
    let _e171 = baseColor;
    let _e172 = vFogFactor_1;
    fragColor = mix(_e170, _e171, vec4(_e172));
    let _e176 = baseColor;
    fragColor.w = _e176.w;
    return;
}

@fragment 
fn main(@location(0) vTexCoord: vec2<f32>, @location(1) vColor: vec4<f32>, @location(2) vNormal: vec3<f32>, @location(3) vViewPos: vec3<f32>, @location(4) vFogFactor: f32) -> FragmentOutput {
    vTexCoord_1 = vTexCoord;
    vColor_1 = vColor;
    vNormal_1 = vNormal;
    vViewPos_1 = vViewPos;
    vFogFactor_1 = vFogFactor;
    main_1();
    let _e65 = fragColor;
    return FragmentOutput(_e65);
}
