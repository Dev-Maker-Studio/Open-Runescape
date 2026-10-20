#version 330 core

in vec4 vColor;
in vec2 vTexCoord0;
in vec3 vTexCoord1;
in float vFogCoord;

uniform sampler2D uBaseTexture;
uniform sampler3D uOverlayTexture3D;
uniform sampler2D uOverlayTexture2D;

uniform bool uHasOverlay;
uniform bool uUse3DTexture;
uniform bool uHasBaseTexture;
uniform vec4 uOverlayConstColor;

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

    if (uHasOverlay) {
        vec4 overlayColor;
        if (uUse3DTexture) {
            overlayColor = texture(uOverlayTexture3D, vTexCoord1);
        } else {
            overlayColor = texture(uOverlayTexture2D, vTexCoord1.xy);
        }

        baseColor.rgb = clamp(baseColor.rgb + overlayColor.rgb, 0.0, 1.0);

        baseColor.a = uOverlayConstColor.a;
    }

    fragColor = baseColor;

    if (uFogEnabled) {
        float fogDist = abs(vFogCoord);
        float fogFactor = clamp((uFogEnd - fogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
        fragColor.rgb = mix(uFogColor.rgb, fragColor.rgb, fogFactor);
    }
}
