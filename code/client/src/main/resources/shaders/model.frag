#version 330 core

in vec2 vTexCoord;
in vec4 vColor;
in vec3 vNormal;
in vec3 vViewPos;
in float vFogFactor;

uniform sampler2D uTexture;
uniform bool uTextureEnabled;
uniform vec4 uFogColor;
uniform float uAlphaThreshold;

out vec4 fragColor;

void main() {
    vec4 color;

    if (uTextureEnabled) {
        vec4 texColor = texture(uTexture, vTexCoord);
        color = texColor * vColor;
    } else {
        color = vColor;
    }

    if (color.a <= uAlphaThreshold) {
        discard;
    }

    fragColor = mix(uFogColor, color, vFogFactor);
    fragColor.a = color.a;
}
