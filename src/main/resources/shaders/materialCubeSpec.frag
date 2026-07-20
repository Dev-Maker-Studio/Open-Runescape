#version 330 core

// Cube Map Specular Fragment Shader
// Replaces MaterialEffect1GL (effect index 1)
//
// Legacy behavior in MaterialEffect1GL:
//   - GL_NORMAL_MAP texgen generates cube map coords from surface normal
//   - Cube map contains pre-computed specular power falloff at 3 exponents:
//     cubeMapTextures[0]: pow(n, 96) * scale  (sharp specular)
//     cubeMapTextures[1]: pow(n, 36) * scale  (medium specular)
//     cubeMapTextures[2]: pow(n, 12) * scale  (broad specular)
//   - Texture combiner: GL_ADD adds specular to base, then GL_REPLACE alpha
//   - When maxTextureUnits >= 3: uses 3-stage combiner chain
//   - When maxTextureUnits < 3: uses simpler single-stage add
//
// Modern approach:
//   - Compute reflection vector from normal and view direction
//   - Apply the specular rotation (22.5 deg around X)
//   - Compute specular highlights using the same power curves as the cube map
//   - Add specular to base color, matching the legacy combiner output
//
// The specular highlight level is controlled by uSpecularLevel (1..3):
//   1 -> pow(96) sharp highlights
//   2 -> pow(36) medium highlights
//   3 -> pow(12) broad highlights

in vec2 vTexCoord;
in vec4 vColor;
in vec3 vNormal;
in vec3 vViewPos;
in float vFogFactor;

uniform sampler2D uTexture;
uniform bool uTextureEnabled;
uniform vec4 uFogColor;
uniform float uAlphaThreshold;

// Specular effect uniforms
uniform int uSpecularLevel;      // 1-3, selects power curve (from effectParam1)
uniform bool uEffectEnabled;     // Whether the cube map specular effect is active
uniform bool uLimitedTextureUnits; // true when < 3 texture units (aBool9025)

out vec4 fragColor;

// Pre-computed rotation matrix for 22.5 degrees around X
// The legacy code does: glRotatef(22.5f, 1.0f, 0.0f, 0.0f)
// cos(22.5) = 0.92388, sin(22.5) = 0.38268
const float COS_22_5 = 0.92388;
const float SIN_22_5 = 0.38268;

void main() {
    // Base color from texture and vertex color
    vec4 baseColor;
    if (uTextureEnabled) {
        vec4 texColor = texture(uTexture, vTexCoord);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    if (baseColor.a <= uAlphaThreshold) {
        discard;
    }

    if (uEffectEnabled) {
        // Compute the normal-mapped cube lookup direction
        // Legacy used GL_NORMAL_MAP texgen which uses the eye-space normal as the cube map direction
        vec3 cubeDir = normalize(vNormal);

        // Apply the 22.5-degree X rotation (matching glRotatef in legacy)
        vec3 rotatedDir;
        rotatedDir.x = cubeDir.x;
        rotatedDir.y = cubeDir.y * COS_22_5 - cubeDir.z * SIN_22_5;
        rotatedDir.z = cubeDir.y * SIN_22_5 + cubeDir.z * COS_22_5;

        // Compute the dominant axis component for specular falloff
        // The legacy cube map was computed per-face using the dominant axis distance:
        //   f_13 = -f_9, f_9, f_10, -f_10, -f_11, f_11 for the 6 cube faces
        // The specular value is pow(f_13, exponent) * scale
        // In practice, this produces a view-dependent highlight based on the reflected normal
        float specularBase = max(0.0, max(abs(rotatedDir.x), max(abs(rotatedDir.y), abs(rotatedDir.z))));

        // Select specular exponent based on level
        float specExponent;
        float scale = uLimitedTextureUnits ? 48.0 / 255.0 : 127.0 / 255.0;

        if (uSpecularLevel == 1) {
            specExponent = 96.0;
        } else if (uSpecularLevel == 2) {
            specExponent = 36.0;
        } else {
            specExponent = 12.0;
        }

        float specular = pow(specularBase, specExponent) * scale;

        if (uLimitedTextureUnits) {
            // Simple mode: ADD specular, use texture alpha for transparency
            // Legacy: GL_ADD for RGB, GL_REPLACE for alpha (from GL_TEXTURE alpha)
            baseColor.rgb += vec3(specular);
        } else {
            // Full mode: Multi-stage combiner emulation
            // Stage 1 (tex unit 1): GL_REPLACE RGB = cube map, GL_MODULATE alpha = previous * cube
            // Stage 2 (tex unit 2): GL_ADD RGB = previous + cube_medium, GL_REPLACE alpha = previous
            // Net effect: base + specular_sharp + specular_medium
            float specSharp = pow(specularBase, 96.0) * scale;
            float specMedium = pow(specularBase, 36.0) * scale;
            baseColor.rgb += vec3(specSharp + specMedium);
        }
    }

    // Apply fog
    fragColor = mix(uFogColor, baseColor, vFogFactor);
    fragColor.a = baseColor.a;
}
