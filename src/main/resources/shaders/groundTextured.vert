#version 330 core

// Vertex attributes - must match the legacy VBO layout from GroundGL.resetLight():
// - Position: vec3 (12 bytes) at offset 0 (GL_FLOAT, 3 components)
// - Color: RGBA (4 bytes) at offset 12 (GL_UNSIGNED_BYTE, 4 components)
// - TexCoord: vec2 or vec3 (8 or 12 bytes) at offset 16 (GL_FLOAT, 2 or 3 components)
// - Normal: vec3 (12 bytes) at offset 24 or 28 (GL_FLOAT, 3 components) - optional for HD_LIGHT
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec4 aColor;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec3 aNormal;

// Uniforms
uniform mat4 uViewProjection;
uniform float uTextureScale;  // 1.0 / textureScale for texture coordinate scaling
uniform bool uHasNormals;     // Whether HD lighting normals are present

// Lighting uniforms (used when uHasNormals is true).
// HD_LIGHT grounds bake sunDotProduct = 1.0 into the per-material vertex colors
// (GroundGL.resetLight, GroundGL.kt:457-460) and rely on runtime lighting instead.
// This replicates the client's own sun formula (same one GroundGL bakes for SD):
//   intensity = sunBrightness + d * (d > 0 ? sunColorIntensity : antiSunColorIntensity)
// where d = dot(sunDirection, normal), both unnormalized, exactly as in
// GroundGL.kt:458-459. uSunDirection here is the RAW toolkit sunDirection.
uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform float uContrast;
uniform float uSunBrightness;
uniform float uContrastPos;   // sunColorIntensity
uniform float uContrastNeg;   // antiSunColorIntensity

// Fog uniforms
uniform float uFogStart;
uniform float uFogEnd;
uniform bool uFogEnabled;

// Outputs to fragment shader
out vec4 vColor;
out vec2 vTexCoord;
out float vFogFactor;

void main() {
    // Apply the VP transform
    gl_Position = uViewProjection * vec4(aPosition, 1.0);

    vec3 color = aColor.rgb;
    if (uHasNormals) {
        float d = dot(uSunDirection, aNormal);
        float intensity = uSunBrightness + d * (d > 0.0 ? uContrastPos : uContrastNeg);
        // Multiply by the SUN COLOUR as well as the intensity. Fixed-function ground
        // lighting uses GL_LIGHT0 diffuse = sunColorIntensity * sunColor and
        // GL_LIGHT_MODEL_AMBIENT = sunBrightness * sunColor, so the tint is part of
        // the lit result; writeVertexColor bakes only a SCALAR colourMultiplier, never
        // the tint. Omitting it left textured terrain untinted and ~2x too bright
        // (measured blue/red 0.66 here vs 0.32 in legacy, exactly the 0.35/0.71 the
        // sun colour applies). ground.vert already did this for untextured terrain.
        color = clamp(color * uSunColor * intensity, 0.0, 1.0);
    }
    vColor = vec4(color, aColor.a);
    vTexCoord = aTexCoord * uTextureScale;
    vFogFactor = 1.0;
}
