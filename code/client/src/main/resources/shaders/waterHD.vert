#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uModelView;
uniform mat4 uProjection;
uniform mat4 uTextureMatrix;
uniform mat3 uNormalMatrix;

uniform float uTime;
uniform float uScale;

uniform float uFogStart;
uniform float uFogEnd;

out vec3 vWorldViewNormal;
out float vWaterDepth;
out vec3 vTexCoord;
out float vFogFactor;

void main() {
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);

    vWaterDepth = aTexCoord.z;

    vWorldViewNormal.x = dot(uNormalMatrix[0], viewPos.xyz);
    vWorldViewNormal.y = dot(uNormalMatrix[1], viewPos.xyz);
    vWorldViewNormal.z = dot(uNormalMatrix[2], viewPos.xyz);

    vec4 tc4 = vec4(aTexCoord, 1.0);
    vTexCoord.x = dot(uTextureMatrix[0], tc4) * uScale;
    vTexCoord.y = dot(uTextureMatrix[1], tc4) * uScale;
    vTexCoord.z = uTime;

    float fogScale = 1.0 / (uFogEnd - uFogStart);
    vFogFactor = clamp((viewPos.z - uFogStart) * fogScale, 0.0, 1.0);

    gl_Position = uProjection * viewPos;
}
