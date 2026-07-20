#version 330 core

// Vertex attributes (matching ModernSpriteRenderer multi-texture format)
layout(location = 0) in vec2 aPosition;  // Screen-space position
layout(location = 2) in vec2 aTexCoord;  // Texture coordinates
layout(location = 3) in vec4 aColor;     // Vertex color (RGBA, normalized ubyte)
layout(location = 4) in float aTexIndex; // Texture slot index (0-7, or -1 for untextured)

// Uniforms
uniform mat4 uProjection;  // Orthographic projection matrix

// Outputs to fragment shader
out vec2 vTexCoord;
out vec4 vColor;
flat out int vTexIndex;

void main() {
    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
    vTexCoord = aTexCoord;
    vColor = aColor;
    // Round to nearest int. Must use floor(): GLSL int() truncates TOWARD ZERO, so
    // the untextured marker -1.0 became int(-0.5) = 0 — every solid FILL quad then
    // sampled texture slot 0 at UV (0,0) and was tinted by an arbitrary texel
    // (fills rendered darker and hue-shifted toward whatever sprite was bound).
    // floor(-0.5) = -1 keeps untextured quads on the pure-vertex-color path.
    vTexIndex = int(floor(aTexIndex + 0.5));
}
