#version 330 core

in vec4 vColor;
in vec2 vTexCoord0;
in vec4 vBlendFactor;
in float vFogCoord;

uniform sampler2D uTexture;
uniform sampler2D uEdgeTexture;

uniform vec3 uWaterColor;
uniform bool uHasBaseTexture;

uniform float uFogStart;
uniform float uFogEnd;
uniform vec4 uFogColor;
uniform bool uFogEnabled;

out vec4 fragColor;

void main() {
    vec4 baseColor;
    if (uHasBaseTexture) {
        vec4 texColor = texture(uTexture, vTexCoord0);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    float edgeBlend = vBlendFactor.x;

    vec3 blendedColor = mix(baseColor.rgb, uWaterColor, edgeBlend);

    fragColor = vec4(blendedColor, baseColor.a);

    if (uFogEnabled) {
        float fogDist = abs(vFogCoord);
        float fogFactor = clamp((uFogEnd - fogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
        fragColor.rgb = mix(uFogColor.rgb, fragColor.rgb, fogFactor);
    }
}
