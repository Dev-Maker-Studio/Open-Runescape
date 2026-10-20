#version 330 core
#define MAX_POINT_LIGHTS 128

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec4 aColor;
layout(location = 2) in vec3 aTexCoord;
layout(location = 3) in vec3 aNormal;

uniform mat4 uViewProjection;
uniform vec4 uClipPlane;
uniform float uTextureScale;
uniform vec2 uTextureScroll;
uniform bool uHasNormals;

uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform float uContrast;
uniform float uSunBrightness;
uniform float uContrastPos;
uniform float uContrastNeg;
uniform int uRawColorDebug;

uniform float uFogStart;
uniform float uFogEnd;
uniform bool uFogEnabled;

uniform int uPointLightCount;
uniform vec4 uPointLightPos[MAX_POINT_LIGHTS];
uniform vec3 uPointLightColor[MAX_POINT_LIGHTS];

uniform int uWaterMode;
uniform vec4 uStillFog;
uniform vec2 uLapNoise[64];
uniform float uLapTurbulence;
uniform vec3 uLapAmbient;
uniform float uWaterTime;
uniform vec2 uRippleFog;

out vec4 vColor;
out vec2 vTexCoord;
out float vFogFactor;
out vec2 vWorldXZ;
out vec3 vWaterNoiseCoord;
out float vWaterMix;
out float vWorldY;
out vec3 vPointLight;

void main() {
    vec3 position = aPosition;
    gl_Position = uViewProjection * vec4(position, 1.0);
    gl_ClipDistance[0] = dot(vec4(position, 1.0), uClipPlane);
    float viewZ = gl_Position.w;
    float waterDepth = aTexCoord.z;
    vWaterMix = 0.0;
    vWaterNoiseCoord = vec3(0.0);
    vPointLight = vec3(0.0);
    if (uWaterMode == 3) {
        float blend = (viewZ - uStillFog.x) * 0.001953125;
        float tint = waterDepth * uStillFog.z + uStillFog.w;
        float pull = clamp((viewZ - uStillFog.y) * 0.00390625, 0.0, 1.0) * waterDepth;
        blend = clamp(blend * tint, 0.0, 1.0);
        tint = clamp(tint, 0.0, 1.0);
        position.y -= pull;
        gl_Position = uViewProjection * vec4(position, 1.0);
        gl_ClipDistance[0] = dot(vec4(position, 1.0), uClipPlane);
        vWaterMix = clamp((max(blend, tint) - 0.25) * 2.0, 0.0, 1.0);
    } else if (uWaterMode == 4) {
        vWaterNoiseCoord = vec3(aTexCoord.xy * uTextureScale * 0.25, uWaterTime);
        if (uRippleFog.y > 0.0) vWaterMix = clamp((viewZ - uRippleFog.x) / uRippleFog.y, 0.0, 1.0);
    }

    vec3 color = aColor.rgb;
    vWorldXZ = position.xz;
    vWorldY = position.y;
    vTexCoord = aTexCoord.xy * uTextureScale + uTextureScroll;
    vFogFactor = uFogEnabled ? clamp((uFogEnd - viewZ) / max(uFogEnd - uFogStart, 1e-4), 0.0, 1.0) : 1.0;
    if (uWaterMode == 2) {
        vec2 n = fract(vec2(position.x + position.z, position.z - position.x) * 0.0001220703125) * 64.0;
        vec2 offset = vec2(uLapNoise[int(n.x)].x, uLapNoise[int(n.y)].y) * uLapTurbulence;
        vec2 base = aTexCoord.xy * uTextureScale + uTextureScroll;
        vTexCoord = base + offset;
        vWaterNoiseCoord = vec3(base * 0.125, uWaterTime);
        vColor = vec4(clamp(aColor.rgb * uLapAmbient, 0.0, 1.0), aColor.a);
        return;
    }
    if (uRawColorDebug == 1) {
        vColor = vec4(color, aColor.a);
        return;
    }
    if (uHasNormals) {
        float d = dot(uSunDirection, aNormal);
        float intensity = uSunBrightness + d * (d > 0.0 ? uContrastPos : uContrastNeg);
        color = clamp(color * uSunColor * intensity, 0.0, 1.0);
    }
    vec3 n = uHasNormals ? normalize(aNormal) : vec3(0.0, 1.0, 0.0);
    for (int i = 0; i < uPointLightCount; ++i) {
        vec3 toLight = uPointLightPos[i].xyz - aPosition;
        float d2 = dot(toLight, toLight);
        float intensity = max(1.0 - d2 * uPointLightPos[i].w, 0.0);
        if (intensity <= 0.0) continue;
        float ndotl = max(dot(n, normalize(toLight)), 0.0);
        vPointLight += uPointLightColor[i] * (min(2.0 * ndotl * intensity, 1.0) / 255.0);
    }
    vColor = vec4(clamp(color, 0.0, 1.0), aColor.a);
}
