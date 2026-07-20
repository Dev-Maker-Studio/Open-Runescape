#version 330 core

// Water Surface Fragment Shader
// Replaces the fixed-function texture combiner setup from StillWaterEffect:
//
// Texture unit 0: the ground/water base texture (or a fallback when no texture)
// Texture unit 1: a 2-pixel edge-blend texture (pixel 0 = 0x00, pixel 1 = 0xFF)
//                 with GL_INTERPOLATE combiner and the water tile color as env color.
//
// The legacy combiner chain for texture unit 1 was:
//   RGB: GL_INTERPOLATE
//     Src0 = GL_CONSTANT (water tile color)  operand = GL_SRC_COLOR
//     Src1 = (previous stage result)          [implicit]
//     Src2 = GL_TEXTURE (edge blend texture)  operand = GL_SRC_ALPHA
//   Alpha: GL_REPLACE
//     Src0 = GL_PREVIOUS
//
// This means: finalRGB = mix(previousRGB, waterColor, edgeBlendAlpha)
// where edgeBlendAlpha comes from the blend texture sampled by vBlendFactor.
//
// When no base texture is bound (loadCombiner with null texture):
//   Src0 = GL_PREVIOUS (uses vertex color as both source and previous)
//   The base color is just the vertex color without texture modulation.
//
// Fog is applied via the legacy GL_LINEAR fog using vFogCoord as the distance.

// Inputs from vertex shader
in vec4 vColor;
in vec2 vTexCoord0;
in vec4 vBlendFactor;   // .xyz = max(primaryFog, edgeFactor), .w = 1.0
in float vFogCoord;     // view-space Z

// Texture samplers
uniform sampler2D uTexture;        // base texture (texture unit 0)
uniform sampler2D uEdgeTexture;    // 2-pixel edge blend texture (texture unit 1)

// Water parameters
uniform vec3 uWaterColor;          // water tile color (RGB, normalized 0-1)
uniform bool uHasBaseTexture;      // whether a base texture is bound

// Fog parameters (emulating legacy GL_LINEAR fog)
uniform float uFogStart;
uniform float uFogEnd;
uniform vec4 uFogColor;
uniform bool uFogEnabled;

// Output
out vec4 fragColor;

void main() {
    // ------------------------------------------------------------------
    // Step 1: Base color from texture unit 0
    // When a texture is bound: baseColor = texture * vertexColor
    // When no texture: baseColor = vertexColor (the legacy combiner uses
    //   GL_PREVIOUS which defaults to primary color when no texture is set)
    // ------------------------------------------------------------------
    vec4 baseColor;
    if (uHasBaseTexture) {
        vec4 texColor = texture(uTexture, vTexCoord0);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    // ------------------------------------------------------------------
    // Step 2: Edge blending (texture unit 1 combiner emulation)
    //
    // The 2-pixel edge texture is sampled using the blend factor from the
    // vertex shader. The blend factor encodes how close we are to the
    // water's edge (0 = fully blended with water color, 1 = no blending).
    //
    // Legacy combiner: GL_INTERPOLATE
    //   result.rgb = mix(previous.rgb, constant.rgb, texture.a)
    //   result.a = previous.a  (GL_REPLACE from GL_PREVIOUS)
    //
    // The edge texture is a 2-pixel ramp: pixel 0 = alpha 0, pixel 1 = alpha 255.
    // Sampling it at vBlendFactor.x gives us the interpolation factor.
    //
    // However, the blend factor from the vertex shader already IS the
    // interpolation factor (clamped [0,1]). The 2-pixel texture was just
    // a hardware trick to get a linear ramp. We can use the factor directly.
    // ------------------------------------------------------------------
    float edgeBlend = vBlendFactor.x;

    // mix(waterColor, baseColor, edgeBlend):
    //   edgeBlend = 0 -> pure water color (edge of water)
    //   edgeBlend = 1 -> pure base color (center of water)
    //
    // Note: The legacy GL_INTERPOLATE was:
    //   result = Src0 * Src2 + (1 - Src2) * Src1
    //   = constant * texture.a + (1 - texture.a) * previous
    // Which is: mix(previous, constant, factor) = previous*(1-f) + constant*f
    // So when edgeBlend is HIGH, we get MORE of the base color (previous).
    // When edgeBlend is LOW, we get MORE of the water color (constant).
    //
    // Reversing to match: result = mix(waterColor, baseColor.rgb, 1.0 - edgeBlend)
    // would give: edgeBlend=0 -> baseColor, edgeBlend=1 -> waterColor
    // But from the ARB: edgeBlend = max(primaryFog, edgeFactor)
    // where primaryFog goes high when FAR from fog start and edgeFactor
    // encodes depth. So high values = more fog/water color.
    //
    // Legacy combiner order:
    //   Src0 = GL_CONSTANT (waterColor), operand = SRC_COLOR
    //   Src2 = GL_TEXTURE (edge tex alpha = blend factor)
    //   Src1 = previous (baseColor) [implicit second source in INTERPOLATE]
    //   result = Src0 * Src2_alpha + Src1 * (1 - Src2_alpha)
    //          = waterColor * edgeBlend + baseColor * (1 - edgeBlend)
    vec3 blendedColor = mix(baseColor.rgb, uWaterColor, edgeBlend);

    // Preserve the original alpha from the base color
    fragColor = vec4(blendedColor, baseColor.a);

    // ------------------------------------------------------------------
    // Step 3: Fog (emulating legacy GL_LINEAR fog)
    // Legacy used GL_FOG_COORDINATE with linear fog:
    //   fogFactor = (fogEnd - fogCoord) / (fogEnd - fogStart)
    //   finalColor = mix(fogColor, shadedColor, fogFactor)
    // ------------------------------------------------------------------
    if (uFogEnabled) {
        float fogDist = abs(vFogCoord);
        float fogFactor = clamp((uFogEnd - fogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
        fragColor.rgb = mix(uFogColor.rgb, fragColor.rgb, fogFactor);
    }
}
