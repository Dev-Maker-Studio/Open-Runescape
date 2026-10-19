#version 330 core

// Shadow depth pass vertex shader.
// Transforms vertices into light-space clip coordinates for depth map generation.
// Used by ShadowPass to render scene depth from the sun's perspective.

// Vertex attributes - must match the formats used by both model and ground renderers.
// Only position is needed for depth; other attributes are declared for VAO compatibility.
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;     // unused in depth pass
layout(location = 2) in vec2 aTexCoord;   // passed through for alpha-tested shadows
layout(location = 3) in vec4 aColor;      // unused in depth pass

// Light-space view-projection matrix (orthographic projection * light view)
uniform mat4 uLightViewProjection;

// Model matrix for per-object transforms (identity for ground tiles)
uniform mat4 uModelMatrix;

// Texture coordinate output for alpha-tested shadow casters (foliage, fences, etc.)
out vec2 vTexCoord;

void main() {
    vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
    gl_Position = uLightViewProjection * worldPos;

    // Pass texture coordinates for alpha test in fragment shader
    vTexCoord = aTexCoord;
}
