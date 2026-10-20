struct P {
    uModelView: mat4x4<f32>,
    uProjection: mat4x4<f32>,
    uTextureMatrix: mat4x4<f32>,
    uFogParams: vec4<f32>,
    uWaterPlane: vec4<f32>,
    uUseLighting_u: u32,
    uUseDepthPlane_u: u32,
    uLight0Direction: vec3<f32>,
    uLight1Direction: vec3<f32>,
    uLight0Diffuse: vec4<f32>,
    uLight1Diffuse: vec4<f32>,
    uAmbientColor: vec4<f32>,
    uWaterColor: vec3<f32>,
    uHasBaseTexture_u: u32,
    uFogStart: f32,
    uFogEnd: f32,
    uFogColor: vec4<f32>,
    uFogEnabled_u: u32,
}

struct VertexOutput {
    @location(0) vColor: vec4<f32>,
    @location(1) vTexCoord0_: vec2<f32>,
    @location(2) vBlendFactor: vec4<f32>,
    @location(3) vFogCoord: f32,
    @builtin(position) gl_Position: vec4<f32>,
}

@group(0) @binding(0) 
var<uniform> global: P;
@group(1) @binding(0) 
var uTexture: texture_2d<f32>;
@group(1) @binding(1) 
var uTexture_s: sampler;
@group(1) @binding(2) 
var uEdgeTexture: texture_2d<f32>;
@group(1) @binding(3) 
var uEdgeTexture_s: sampler;
var<private> aPosition_1: vec3<f32>;
var<private> aNormal_1: vec3<f32>;
var<private> aTexCoord_1: vec3<f32>;
var<private> aColor_1: vec4<f32>;
var<private> vColor: vec4<f32>;
var<private> vTexCoord0_: vec2<f32>;
var<private> vBlendFactor: vec4<f32>;
var<private> vFogCoord: f32;
var<private> gl_Position: vec4<f32>;

fn main_1() {
    var viewPos: vec4<f32>;
    var fogFactor: vec4<f32>;
    var depth: f32;
    var displacementFactor: f32;
    var depth_1: f32;
    var blendValue: f32;
    var viewNormal: vec3<f32>;
    var ndotl0_: f32;
    var ndotl1_: f32;
    var lightColor: vec4<f32>;
    var tc4_: vec4<f32>;

    let _e48 = global.uModelView;
    let _e49 = aPosition_1;
    viewPos = (_e48 * vec4<f32>(_e49.x, _e49.y, _e49.z, 1f));
    let _e59 = viewPos;
    let _e61 = global.uFogParams;
    fogFactor.x = ((_e59.z - _e61.x) * 0.001953125f);
    let _e66 = global.uUseDepthPlane_u;
    if (_e66 != 0u) {
        {
            let _e69 = global.uWaterPlane;
            let _e70 = viewPos;
            depth = dot(_e69, _e70);
            let _e74 = depth;
            let _e76 = global.uFogParams;
            let _e79 = global.uFogParams;
            fogFactor.y = ((-(_e74) * _e76.z) + _e79.w);
        }
    } else {
        {
            let _e83 = aTexCoord_1;
            let _e85 = global.uFogParams;
            let _e88 = global.uFogParams;
            fogFactor.y = ((_e83.z * _e85.z) + _e88.w);
        }
    }
    let _e92 = viewPos;
    let _e94 = global.uFogParams;
    fogFactor.z = ((_e92.z - _e94.y) * 0.00390625f);
    let _e99 = global.uUseDepthPlane_u;
    if !((_e99 != 0u)) {
        {
            let _e104 = fogFactor;
            let _e106 = fogFactor;
            fogFactor.x = (_e104.x * _e106.y);
        }
    }
    let _e109 = fogFactor;
    fogFactor = clamp(_e109, vec4(0f), vec4(1f));
    let _e116 = global.uUseDepthPlane_u;
    if (_e116 != 0u) {
        {
            let _e119 = global.uWaterPlane;
            let _e120 = viewPos;
            depth_1 = dot(_e119, _e120);
            let _e123 = fogFactor;
            let _e125 = depth_1;
            displacementFactor = (_e123.z * -(_e125));
        }
    } else {
        {
            let _e128 = fogFactor;
            let _e130 = aTexCoord_1;
            displacementFactor = (_e128.z * _e130.z);
        }
    }
    let _e133 = viewPos;
    let _e135 = viewPos;
    let _e137 = global.uWaterPlane;
    let _e139 = displacementFactor;
    let _e141 = (_e135.xyz + (_e137.xyz * _e139));
    viewPos.x = _e141.x;
    viewPos.y = _e141.y;
    viewPos.z = _e141.z;
    let _e148 = fogFactor;
    let _e150 = fogFactor;
    blendValue = max(_e148.x, _e150.y);
    let _e154 = blendValue;
    let _e155 = blendValue;
    let _e156 = blendValue;
    vBlendFactor = vec4<f32>(_e154, _e155, _e156, 1f);
    let _e159 = global.uUseLighting_u;
    if (_e159 != 0u) {
        {
            let _e166 = global.uModelView[0];
            let _e168 = aNormal_1;
            viewNormal.x = dot(_e166.xyz, _e168);
            let _e173 = global.uModelView[1];
            let _e175 = aNormal_1;
            viewNormal.y = dot(_e173.xyz, _e175);
            let _e180 = global.uModelView[2];
            let _e182 = aNormal_1;
            viewNormal.z = dot(_e180.xyz, _e182);
            let _e184 = viewNormal;
            let _e185 = global.uLight0Direction;
            ndotl0_ = max(dot(_e184, _e185), 0f);
            let _e190 = viewNormal;
            let _e191 = global.uLight1Direction;
            ndotl1_ = max(dot(_e190, _e191), 0f);
            let _e196 = global.uAmbientColor;
            lightColor = _e196;
            let _e198 = lightColor;
            let _e199 = global.uLight0Diffuse;
            let _e200 = ndotl0_;
            lightColor = (_e198 + (_e199 * _e200));
            let _e203 = lightColor;
            let _e204 = global.uLight1Diffuse;
            let _e205 = ndotl1_;
            lightColor = (_e203 + (_e204 * _e205));
            let _e208 = aColor_1;
            let _e209 = lightColor;
            vColor = (_e208 * _e209);
        }
    } else {
        {
            let _e211 = aColor_1;
            vColor = _e211;
        }
    }
    let _e213 = global.uProjection;
    let _e214 = viewPos;
    gl_Position = (_e213 * _e214);
    let _e216 = viewPos;
    vFogCoord = _e216.z;
    let _e218 = global.uUseDepthPlane_u;
    if (_e218 != 0u) {
        {
            let _e221 = aTexCoord_1;
            tc4_ = vec4<f32>(_e221.x, _e221.y, _e221.z, 1f);
            let _e231 = global.uTextureMatrix[0];
            let _e232 = tc4_;
            vTexCoord0_.x = dot(_e231, _e232);
            let _e237 = global.uTextureMatrix[1];
            let _e238 = tc4_;
            vTexCoord0_.y = dot(_e237, _e238);
            return;
        }
    } else {
        {
            let _e243 = global.uTextureMatrix[0];
            let _e245 = aTexCoord_1;
            vTexCoord0_.x = dot(_e243.xyz, _e245);
            let _e250 = global.uTextureMatrix[1];
            let _e252 = aTexCoord_1;
            vTexCoord0_.y = dot(_e250.xyz, _e252);
            return;
        }
    }
}

@vertex 
fn main(@location(0) aPosition: vec3<f32>, @location(1) aNormal: vec3<f32>, @location(2) aTexCoord: vec3<f32>, @location(3) aColor: vec4<f32>) -> VertexOutput {
    aPosition_1 = aPosition;
    aNormal_1 = aNormal;
    aTexCoord_1 = aTexCoord;
    aColor_1 = aColor;
    main_1();
    let _e69 = vColor;
    let _e71 = vTexCoord0_;
    let _e73 = vBlendFactor;
    let _e75 = vFogCoord;
    let _e77 = gl_Position;
    return VertexOutput(_e69, _e71, _e73, _e75, _e77);
}
