#version 330 core

// Inputs from vertex shader
in vec4 vColor;
in vec2 vTexCoord;
in float vFogFactor;
in vec2 vWorldXZ;
in vec3 vWaterNoiseCoord;
in float vWaterMix;

uniform int uWaterMode;
uniform sampler3D uWaterNoise;
uniform bool uHasWaterNoise;
uniform vec3 uWaterColour;

// Native ground shadow overlay. One R8 texel per shadow cell over the whole ground,
// holding what legacy's ShadowTileGL.regenerateTexture puts in its per-chunk alpha
// textures: 68 where the shadow map is non-zero, neighbourCount*17 on the fringe.
// Legacy draws that texture over the terrain as a REPLACE + alpha-blended quad, i.e.
// dst *= 1 - texel, with fog applied to the overlay too; fog(ground*(1-a)) is the
// same number, so applying it here before fog is exact. LINEAR filtering supplies
// the same soft edge legacy's linear-filtered chunk textures do.
uniform bool uGroundShadowEnabled;
uniform sampler2D uGroundShadowTex;
uniform float uGroundShadowScale;   // 1 / (worldUnitsPerTexel * mapWidth)

// Uniforms
uniform sampler2D uTexture;
uniform bool uHasTexture;
uniform vec4 uFogColor;
// Legacy fixed-function texture combine mode from MaterialType.combineMode,
// applied by OpenGlToolkit.loadTextureCombineMode(mode) (OpenGlToolkit.kt:1935):
//   0 = GL_MODULATE/GL_MODULATE, 1 = GL_REPLACE/GL_REPLACE,
//   2 = GL_INTERPOLATE/GL_INTERPOLATE, 3 = GL_ADD rgb + GL_MODULATE alpha,
//   4 = GL_SUBTRACT/GL_SUBTRACT
// The vertex color is the per-material baked light/shadow color from
// GroundMaterialGL.writeVertexColor (arrayBufferPointer VBO), so MODULATE
// reproduces legacy's dark modulated terrain. No GL_RGB_SCALE is applied for
// ground in legacy (only RipplingWaterEffect uses 2x), so no scale here.
uniform int uCombineMode;
// Effects 1 and 7 (CubeMapSpecularEffect, ReflectingTextureEffect) source unit 0's
// alpha from GL_PRIMARY_COLOR, so their mostly transparent textures never punch
// through the terrain; every other effect keeps the combiner's alpha.
uniform bool uAlphaFromVertex;

// Output
out vec4 fragColor;

void main() {
    vec4 color;
    // Debug (-Drender.groundcolor=true): output the per-material baked vertex
    // colour alone. If the terrain goes black here, the colour VBO is not reaching
    // the shader and MODULATE is a no-op, which is exactly what full-brightness
    // ground looks like.
    if (uCombineMode == 99) {
        fragColor = vec4(vColor.rgb, 1.0);
        return;
    }
    if (uHasTexture) {
        vec4 texColor = texture(uTexture, vTexCoord);
        if (uCombineMode == 1) {
            // GL_REPLACE / GL_REPLACE
            color = texColor;
        } else if (uCombineMode == 3) {
            // GL_ADD rgb, GL_MODULATE alpha
            color = vec4(clamp(texColor.rgb + vColor.rgb, 0.0, 1.0), texColor.a * vColor.a);
        } else if (uCombineMode == 4) {
            // GL_SUBTRACT / GL_SUBTRACT (Arg0 - Arg1 = texture - primary)
            color = clamp(texColor - vColor, 0.0, 1.0);
        } else {
            // GL_MODULATE / GL_MODULATE (mode 0; mode 2 INTERPOLATE approximated
            // as MODULATE — its GL_CONSTANT arg is not plumbed through yet)
            color = texColor * vColor;
        }
    } else {
        // No texture: use vertex color directly
        color = vColor;
    }

    if (uWaterMode == 4) {
        vec4 n = uHasWaterNoise ? texture(uWaterNoise, vWaterNoiseCoord) : vec4(0.0, 0.0, 0.0, 1.0);
        color = vec4(clamp((vColor.rgb + n.rgb) * 2.0, 0.0, 1.0), clamp(vColor.a * n.a + vWaterMix, 0.0, 1.0));
    } else if (uWaterMode == 2) {
        if (uHasWaterNoise) color.rgb = clamp(color.rgb + texture(uWaterNoise, vWaterNoiseCoord).rgb, 0.0, 1.0);
        color.a = 1.0;
    } else if (uWaterMode == 3) {
        color.rgb = mix(color.rgb, uWaterColour, vWaterMix);
    }
    if (uAlphaFromVertex && uWaterMode != 4 && uWaterMode != 2) {
        color.a = vColor.a;
    }
    if (uGroundShadowEnabled) {
        color.rgb *= 1.0 - texture(uGroundShadowTex, vWorldXZ * uGroundShadowScale).r;
    }

    // Apply fog
    color.rgb = mix(uFogColor.rgb, color.rgb, vFogFactor);

    fragColor = color;
}
