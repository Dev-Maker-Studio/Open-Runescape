#version 330 core

// Inputs from vertex shader
in vec2 vTexCoord;
in vec4 vColor;
in float vFogFactor;

// Uniforms
uniform sampler2D uTexture;
uniform bool uTextureEnabled;
uniform float uAlphaThreshold;

// Output
out vec4 fragColor;

void main() {
    vec4 color;

    if (uTextureEnabled) {
        // Sample texture and modulate with vertex color
        vec4 texColor = texture(uTexture, vTexCoord);
        color = texColor * vColor;
    } else {
        // Use vertex color only
        color = vColor;
    }

    // Alpha test (discard fully transparent fragments)
    if (color.a <= uAlphaThreshold) {
        discard;
    }

    // No fog blending - fog is already baked into colors if needed
    fragColor = color;
}
