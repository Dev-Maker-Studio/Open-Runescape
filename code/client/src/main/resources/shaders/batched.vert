#version 330 core

// Vertex attributes (matching VertexArray.Attributes)
// These vertices are PRE-TRANSFORMED to NDC space (after MVP and perspective divide)
layout(location = 0) in vec3 aPosition;  // Already in NDC: [-1,1] x [-1,1] x [0,1]
layout(location = 1) in vec3 aNormal;    // Not used but kept for format compatibility
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;     // Pre-lit colors (lighting already applied)

// Outputs to fragment shader
out vec2 vTexCoord;
out vec4 vColor;
out float vFogFactor;

void main() {
    // Position is already in NDC, just pass through
    // w=1 since we've already done perspective divide on CPU
    gl_Position = vec4(aPosition, 1.0);

    // Pass through texture coordinates and pre-lit color
    vTexCoord = aTexCoord;
    vColor = aColor;

    // Fog is pre-computed on CPU, disable here (could pass fog factor as vertex attribute if needed)
    vFogFactor = 1.0;
}
