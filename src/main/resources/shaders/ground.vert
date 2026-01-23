#version 330 core

// Vertex attributes (matching VertexFormats.setupGroundFormat)
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
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

// Shadow uniforms (for terrain shadow mapping)
uniform mat4 uShadowMatrix;
uniform bool uShadowEnabled;

// Outputs to fragment shader
out vec4 vColor;
out vec3 vNormal;
out vec3 vViewPos;
out float vFogFactor;
out vec4 vShadowCoord;

void main() {
    // Transform position to clip space
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    // Transform position to view space (for fog calculation)
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);
    vViewPos = viewPos.xyz;

    // Transform normal to view space
    vNormal = normalize(uNormalMatrix * aNormal);

    // Calculate vertex lighting (Gouraud shading)
    float nDotL = dot(vNormal, uSunDirection);
    float lightIntensity = uAmbient + max(nDotL, 0.0) * uContrast;

    // Apply lighting to vertex color
    vColor = vec4(aColor.rgb * uSunColor * lightIntensity, aColor.a);

    // Calculate fog factor
    if (uFogEnabled) {
        float fogDistance = length(viewPos.xyz);
        vFogFactor = clamp((uFogEnd - fogDistance) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }

    // Calculate shadow coordinates
    if (uShadowEnabled) {
        vShadowCoord = uShadowMatrix * vec4(aPosition, 1.0);
    } else {
        vShadowCoord = vec4(0.0);
    }
}
