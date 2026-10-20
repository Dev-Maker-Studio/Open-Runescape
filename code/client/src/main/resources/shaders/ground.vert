#version 330 core
#define MAX_POINT_LIGHTS 128

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 3) in vec4 aColor;

uniform mat4 uModelViewProjection;
uniform mat4 uModelView;
uniform mat3 uNormalMatrix;

uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform float uContrast;

uniform float uFogStart;
uniform float uFogEnd;
uniform bool uFogEnabled;

uniform mat4 uShadowMatrix;
uniform bool uShadowEnabled;

uniform int uPointLightCount;
uniform vec4 uPointLightPos[MAX_POINT_LIGHTS];
uniform vec3 uPointLightColor[MAX_POINT_LIGHTS];

uniform bool uGroundShadowEnabled;
uniform sampler2D uGroundShadowTex;
uniform float uGroundShadowScale;

out vec4 vColor;
out vec3 vNormal;
out vec3 vViewPos;
out float vFogFactor;
out vec4 vShadowCoord;

void main() {
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    vec4 viewPos = uModelView * vec4(aPosition, 1.0);
    vViewPos = viewPos.xyz;

    vNormal = normalize(uNormalMatrix * aNormal);

    float nDotL = dot(vNormal, uSunDirection);
    float lightIntensity = uAmbient + max(nDotL, 0.0) * uContrast;

    vec3 litRGB = aColor.rgb * uSunColor * lightIntensity;

    for (int i = 0; i < uPointLightCount; ++i) {
        vec3 toLight = uPointLightPos[i].xyz - aPosition;
        float d2 = dot(toLight, toLight);
        float att = 1.0 / max(uPointLightPos[i].w * d2, 1e-6);
        float ndotl = max(dot(aNormal, normalize(toLight)), 0.0);
        litRGB += aColor.rgb * uPointLightColor[i] * (att * ndotl);
    }

    if (uGroundShadowEnabled) {
        float raw = texture(uGroundShadowTex, aPosition.xz * uGroundShadowScale).r * 255.0;
        litRGB *= (1.0 - min(raw, 1.0) * (68.0 / 255.0));
    }
    vColor = vec4(clamp(litRGB, 0.0, 1.0), aColor.a);

    if (uFogEnabled) {
        float fogDistance = length(viewPos.xyz);
        vFogFactor = clamp((uFogEnd - fogDistance) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }

    if (uShadowEnabled) {
        vShadowCoord = uShadowMatrix * vec4(aPosition, 1.0);
    } else {
        vShadowCoord = vec4(0.0);
    }
}
