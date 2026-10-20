#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uModelViewProjection;
uniform mat4 uModelView;
uniform mat3 uNormalMatrix;

uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform float uContrast;
uniform bool uPreLitColors;

uniform float uFogStart;
uniform float uFogEnd;
uniform bool uFogEnabled;

out vec2 vTexCoord;
out vec4 vColor;
out vec3 vNormal;
out vec3 vViewPos;
out float vFogFactor;

void main() {
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    vec4 viewPos = uModelView * vec4(aPosition, 1.0);
    vViewPos = viewPos.xyz;

    vTexCoord = aTexCoord;
    vNormal = normalize(uNormalMatrix * aNormal);

    if (uPreLitColors) {
        vColor = aColor;
    } else {
        float nDotL = dot(vNormal, uSunDirection);
        float lightIntensity = uAmbient + max(nDotL, 0.0) * uContrast;
        vColor = vec4(aColor.rgb * uSunColor * lightIntensity, aColor.a);
    }

    if (uFogEnabled) {
        float fogDistance = length(viewPos.xyz);
        vFogFactor = clamp((uFogEnd - fogDistance) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }
}
