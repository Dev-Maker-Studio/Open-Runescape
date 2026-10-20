#version 330 core

in vec2 vTexCoord;
in vec4 vColor;
in float vFogFactor;

uniform sampler2D uTexture;
uniform bool uTextureEnabled;
uniform int uCombineMode;

uniform vec4 uFogColor;

out vec4 fragColor;

void main() {
    vec4 color;

    if (uTextureEnabled) {
        vec4 texColor = texture(uTexture, vTexCoord);
        if (uCombineMode == 1) {
            color = texColor;
        } else if (uCombineMode == 3) {
            color = vec4(clamp(texColor.rgb + vColor.rgb, 0.0, 1.0), texColor.a * vColor.a);
        } else if (uCombineMode == 4) {
            color = clamp(texColor - vColor, 0.0, 1.0);
        } else {
            color = texColor * vColor;
        }
    } else {
        color = vColor;
    }

    if (color.a < 0.004) {
        discard;
    }

    fragColor = mix(uFogColor, color, vFogFactor);
    fragColor.a = color.a;
}
