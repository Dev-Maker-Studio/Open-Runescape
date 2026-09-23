#version 330 core

// Vertex attributes
// Particle vertex format (24 bytes per vertex):
//   position: vec3 (12 bytes) at offset 0    - world-space billboard corner
//   texcoord: vec2 (8 bytes) at offset 12    - UV coordinates
//   color: vec4 ubyte (4 bytes) at offset 20 - RGBA particle color
layout(location = 0) in vec3 aPosition;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

// Uniforms
uniform mat4 uViewProjection;  // Combined view-projection matrix
uniform vec2 uTextureScroll;
uniform vec3 uAmbient;

// Fog uniforms
uniform bool uFogEnabled;
uniform float uFogStart;
uniform float uFogEnd;

// Outputs to fragment shader
out vec2 vTexCoord;
out vec4 vColor;
out float vFogFactor;

void main() {
    // Billboard corners are pre-computed on the CPU in world space,
    // so we only need to apply the view-projection transform.
    gl_Position = uViewProjection * vec4(aPosition, 1.0);

    vTexCoord = aTexCoord + uTextureScroll;
    vColor = vec4(clamp(aColor.rgb * uAmbient, 0.0, 1.0), aColor.a);

    // Calculate fog factor based on clip-space depth
    if (uFogEnabled) {
        // Use clip-space w as a distance approximation (matches legacy linear fog)
        float dist = gl_Position.w;
        vFogFactor = clamp((uFogEnd - dist) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }
}
