#version 330 core

in vec4 vColor;
in vec2 vTexCoord0;
in vec3 vReflectDir;
in float vFogCoord;

uniform sampler2D uBaseTexture;
uniform samplerCube uCubeMap;

uniform bool uHasBaseTexture;
uniform bool uReflectionEnabled;

uniform float uFogStart;
uniform float uFogEnd;
uniform vec4 uFogColor;
uniform bool uFogEnabled;

out vec4 fragColor;

void main() {
    vec4 baseColor;
    if (uHasBaseTexture) {
        vec4 texColor = texture(uBaseTexture, vTexCoord0);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    if (uReflectionEnabled) {
        vec4 cubeMapColor = texture(uCubeMap, vReflectDir);
        float blendFactor = baseColor.a;
        fragColor.rgb = mix(baseColor.rgb, cubeMapColor.rgb, blendFactor);
        fragColor.a = vColor.a;
    } else {
        fragColor = baseColor;
    }

    if (uFogEnabled) {
        float fogDist = abs(vFogCoord);
        float fogFactor = clamp((uFogEnd - fogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
        fragColor.rgb = mix(uFogColor.rgb, fragColor.rgb, fogFactor);
    }
}
