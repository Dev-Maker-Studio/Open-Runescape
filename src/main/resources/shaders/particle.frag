#version 330 core

// Inputs from vertex shader
in vec2 vTexCoord;
in vec4 vColor;
in float vFogFactor;

// Uniforms
uniform sampler2D uTexture;
uniform bool uTextureEnabled;

// Fog
uniform vec4 uFogColor;

// Output
out vec4 fragColor;

void main() {
    vec4 color;

    if (uTextureEnabled) {
        // Sample texture and modulate with vertex color
        vec4 texColor = texture(uTexture, vTexCoord);
        color = texColor * vColor;
    } else {
        // Untextured particle - use vertex color directly
        color = vColor;
    }

    // Discard fully transparent fragments
    if (color.a < 0.004) {
        discard;
    }

    // Apply fog
    fragColor = mix(uFogColor, color, vFogFactor);
    fragColor.a = color.a;  // Preserve original alpha through fog
}
