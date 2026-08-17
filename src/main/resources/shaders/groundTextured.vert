#version 330 core
#define MAX_POINT_LIGHTS 128

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
// Point lights, evaluated natively in the modern pipeline. The legacy renderer paints
// these as fixed-function MULTIPLY light meshes over the ground; that path must never
// run here, so the same contribution is computed in-shader instead.
uniform int uPointLightCount;
uniform vec4 uPointLightPos[MAX_POINT_LIGHTS];   // xyz world, w = 1/size2d^2
uniform vec3 uPointLightColor[MAX_POINT_LIGHTS]; // colour * transparency/255

// Native ground shadows: one intensity texel per shadow cell over the whole ground,
// the same ShadowMapperGL data legacy composites with a fixed-function overlay.
uniform bool uGroundShadowEnabled;
uniform sampler2D uGroundShadowTex;
uniform float uGroundShadowScale;   // 1 / (worldUnitsPerTexel * mapWidth)

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
    // Point lights (see ground.vert). Uses the tile normal when HD normals are
    // present, otherwise straight up -- terrain is essentially flat per tile.
    vec3 n = uHasNormals ? aNormal : vec3(0.0, 1.0, 0.0);
    for (int i = 0; i < uPointLightCount; ++i) {
        vec3 toLight = uPointLightPos[i].xyz - aPosition;
        float d2 = dot(toLight, toLight);
        float att = 1.0 / max(uPointLightPos[i].w * d2, 1e-6);
        float ndotl = max(dot(n, normalize(toLight)), 0.0);
        color += aColor.rgb * uPointLightColor[i] * (att * ndotl);
    }
    color = clamp(color, 0.0, 1.0);

    if (uGroundShadowEnabled) {
        // Legacy builds its overlay texture as 68 wherever shadowMapperData is
        // non-zero, and neighbourCount*17 on the boundary (ShadowTileGL.
        // regenerateTexture) -- so the stored magnitude is really a boolean and full
        // shadow is 68/255. Saturating any non-zero texel reproduces that, and the
        // texture's LINEAR filter supplies the soft edge the neighbour count encoded.
        float raw = texture(uGroundShadowTex, aPosition.xz * uGroundShadowScale).r * 255.0;
            color *= (1.0 - min(raw, 1.0) * (68.0 / 255.0));
    }
    vColor = vec4(color, aColor.a);
    vTexCoord = aTexCoord * uTextureScale;
    vFogFactor = 1.0;
}
