#version 330 core

// Shadow depth pass fragment shader.
// For opaque geometry, the depth buffer is written automatically by the fixed-function
// depth test -- this shader only needs to handle alpha-tested geometry (foliage, fences)
// that should not cast shadows where the texture is transparent.

in vec2 vTexCoord;

// When true, sample the texture and discard transparent fragments
uniform bool uAlphaTestEnabled;
uniform sampler2D uTexture;
uniform float uAlphaThreshold;

void main() {
    if (uAlphaTestEnabled) {
        float alpha = texture(uTexture, vTexCoord).a;
        if (alpha <= uAlphaThreshold) {
            discard;
        }
    }

    // Depth is written automatically by the depth buffer.
    // No color output needed (no color attachment on shadow FBO).
}
