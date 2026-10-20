#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uModelView;
uniform mat4 uProjection;
uniform mat4 uTextureMatrix;

uniform vec4 uFogParams;

uniform vec4 uWaterPlane;

uniform bool uUseLighting;
uniform bool uUseDepthPlane;

uniform vec3 uLight0Direction;
uniform vec3 uLight1Direction;
uniform vec4 uLight0Diffuse;
uniform vec4 uLight1Diffuse;
uniform vec4 uAmbientColor;

out vec4 vColor;
out vec2 vTexCoord0;
out vec4 vBlendFactor;
out float vFogCoord;

void main() {
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);

    vec4 fogFactor;

    fogFactor.x = (viewPos.z - uFogParams.x) * 0.001953125;

    if (uUseDepthPlane) {
        float depth = dot(uWaterPlane, viewPos);
        fogFactor.y = -depth * uFogParams.z + uFogParams.w;
    } else {
        fogFactor.y = aTexCoord.z * uFogParams.z + uFogParams.w;
    }

    fogFactor.z = (viewPos.z - uFogParams.y) * 0.00390625;

    if (!uUseDepthPlane) {
        fogFactor.x = fogFactor.x * fogFactor.y;
    }

    fogFactor = clamp(fogFactor, 0.0, 1.0);

    float displacementFactor;
    if (uUseDepthPlane) {
        float depth = dot(uWaterPlane, viewPos);
        displacementFactor = fogFactor.z * (-depth);
    } else {
        displacementFactor = fogFactor.z * aTexCoord.z;
    }

    viewPos.xyz += uWaterPlane.xyz * displacementFactor;

    float blendValue = max(fogFactor.x, fogFactor.y);
    vBlendFactor = vec4(blendValue, blendValue, blendValue, 1.0);

    if (uUseLighting) {
        vec3 viewNormal;
        viewNormal.x = dot(uModelView[0].xyz, aNormal);
        viewNormal.y = dot(uModelView[1].xyz, aNormal);
        viewNormal.z = dot(uModelView[2].xyz, aNormal);

        float ndotl0 = max(dot(viewNormal, uLight0Direction), 0.0);
        float ndotl1 = max(dot(viewNormal, uLight1Direction), 0.0);

        vec4 lightColor = uAmbientColor;
        lightColor += uLight0Diffuse * ndotl0;
        lightColor += uLight1Diffuse * ndotl1;
        vColor = aColor * lightColor;
    } else {
        vColor = aColor;
    }

    gl_Position = uProjection * viewPos;

    vFogCoord = viewPos.z;

    if (uUseDepthPlane) {
        vec4 tc4 = vec4(aTexCoord, 1.0);
        vTexCoord0.x = dot(uTextureMatrix[0], tc4);
        vTexCoord0.y = dot(uTextureMatrix[1], tc4);
    } else {
        vTexCoord0.x = dot(uTextureMatrix[0].xyz, aTexCoord);
        vTexCoord0.y = dot(uTextureMatrix[1].xyz, aTexCoord);
    }
}
