#version 330 core

in vec2 vTexCoord;
in vec4 vColor;
in vec3 vViewPos;
in float vFogFactor;
in float vFogAlpha;

uniform sampler2D uTexture;
uniform sampler3D uTexture3D;
uniform bool uTextureEnabled;
uniform bool uUse3DTexture;
uniform float uAnimTime;
uniform vec4 uFogColor;
uniform float uAlphaThreshold;

out vec4 fragColor;

void main() {
    vec4 baseColor = vColor;

    if (uTextureEnabled) {
        vec4 texColor;
        if (uUse3DTexture) {
            texColor = texture(uTexture3D, vec3(vTexCoord, uAnimTime));
        } else {
            texColor = texture(uTexture, vTexCoord);
        }

        baseColor.rgb = clamp((baseColor.rgb + texColor.rgb) * 2.0, 0.0, 1.0);
        baseColor.a *= texColor.a;
    }

    baseColor.a = clamp(baseColor.a + vFogAlpha, 0.0, 1.0);

    if (baseColor.a <= uAlphaThreshold) {
        discard;
    }

    fragColor = mix(uFogColor, baseColor, vFogFactor);
    fragColor.a = baseColor.a;
}
