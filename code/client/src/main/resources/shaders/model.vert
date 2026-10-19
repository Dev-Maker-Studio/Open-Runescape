#version 330 core

// Vertex attributes (matching VertexArray.Attributes)
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

// Uniforms
uniform mat4 uModelViewProjection;
uniform mat4 uModelView;
uniform mat3 uNormalMatrix;

// Lighting uniforms
uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform float uContrast;

// Fog uniforms
uniform float uFogStart;
uniform float uFogEnd;
uniform bool uFogEnabled;

// Pre-lit mode: when true, skip lighting calculation (colors are already lit)
uniform bool uPreLitColors;

// Outputs to fragment shader
out vec2 vTexCoord;
out vec4 vColor;
out vec3 vNormal;
out vec3 vViewPos;
out float vFogFactor;

void main() {
    // Transform position to clip space
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    // Transform position to view space (for fog calculation)
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);
    vViewPos = viewPos.xyz;

    // Pass through texture coordinates
    vTexCoord = aTexCoord;

    // Transform normal to view space
    vNormal = normalize(uNormalMatrix * aNormal);

    // Calculate vertex lighting (Gouraud shading for compatibility with original)
    if (uPreLitColors) {
        // Colors are pre-lit (baked lighting from legacy path), pass through directly
        vColor = aColor;
    } else {
        float nDotL = dot(vNormal, uSunDirection);
        float lightIntensity = uAmbient + max(nDotL, 0.0) * uContrast;
        // Apply lighting to vertex color
        vColor = vec4(aColor.rgb * uSunColor * lightIntensity, aColor.a);
    }

    // Calculate fog factor
    if (uFogEnabled) {
        float fogDistance = length(viewPos.xyz);
        vFogFactor = clamp((uFogEnd - fogDistance) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }
}
