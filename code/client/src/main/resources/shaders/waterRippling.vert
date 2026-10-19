#version 330 core

// Rippling Water Vertex Shader
// Replaces RipplingWaterEffect (effect index 4)
//
// Legacy behavior:
//   - Uses GL_EYE_LINEAR texgen for S and T on texture unit 1 (fog-based alpha ramp)
//   - Uses GL_OBJECT_LINEAR texgen for R and Q on texture unit 0 (3D texture animation)
//   - Texture combiner chain: GL_ADD for RGB, GL_MODULATE for alpha on unit 0
//     and GL_REPLACE/GL_ADD on unit 1
//   - glScalef(0.25, 0.25, 1.0) on texture matrix for water tile scaling
//   - GL_RGB_SCALE = 2.0 for double-brightness on base texture
//
// Modern approach:
//   - Compute eye-linear texture coordinate in vertex shader
//   - Pass 3D texture coordinate with animated time component
//   - Apply scaling and brightness in fragment shader

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

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

// Effect-specific uniforms
uniform vec4 uEyePlaneS;      // eye-space plane for fog-based alpha ramp
uniform float uTextureScale;  // 0.25 (from glScalef in legacy)

// Fog alpha ramp: density-based alpha for secondary texture
uniform float uFogDensity;    // fog density for 1D alpha lookup
uniform float uFogPosition;   // fog start position

out vec2 vTexCoord;
out vec4 vColor;
out vec3 vViewPos;
out float vFogFactor;
out float vFogAlpha;       // fog-based alpha ramp for overlay texture

void main() {
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    vec4 viewPos = uModelView * vec4(aPosition, 1.0);
    vViewPos = viewPos.xyz;

    // Apply texture scaling (legacy glScalef(0.25, 0.25, 1.0))
    vTexCoord = aTexCoord * uTextureScale;

    // Normal for lighting
    vec3 normal = normalize(uNormalMatrix * aNormal);

    // Vertex lighting
    float nDotL = dot(normal, uSunDirection);
    float lightIntensity = uAmbient + max(nDotL, 0.0) * uContrast;
    vColor = vec4(aColor.rgb * uSunColor * lightIntensity, aColor.a);

    // Standard fog
    if (uFogEnabled) {
        float fogDistance = length(viewPos.xyz);
        vFogFactor = clamp((uFogEnd - fogDistance) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }

    // Eye-linear texture coordinate for fog-based alpha ramp
    // Legacy: GL_EYE_LINEAR with plane = (0, 0, density, fogStart*density + 0.25)
    // This computes: S = dot(eyePlane, viewPos) = viewPos.z * density + offset
    vFogAlpha = clamp(dot(uEyePlaneS, viewPos), 0.0, 1.0);
}
