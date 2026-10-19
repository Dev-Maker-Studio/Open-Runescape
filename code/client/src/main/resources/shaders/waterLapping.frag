#version 330 core

// Lapping Water Fragment Shader
// Replaces the fixed-function texture combiner setup from LappingWaterEffect:
//
// Texture unit 0: base water texture sampled with noise-perturbed UVs
//   - Standard GL_MODULATE: texture * vertex color (handled in vertex shader)
//
// Texture unit 1: animated water overlay (3D or 2D texture)
//   - Combine mode: GL_ADD (adds overlay RGB to the base)
//   - Alpha: GL_REPLACE from GL_CONSTANT (replaces alpha with constant value)
//   - The constant alpha comes from the water tile color's alpha,
//     set via glTexEnvColor with the inverted tile color (0xffffff.inv())
//
// The GL_ADD combine mode means:
//   result.rgb = base.rgb + overlay.rgb
//   result.a = constantAlpha (from the environment color)
//
// When no overlay texture is bound (param1 & 0x80 != 0):
//   The overlay texture unit is unbound, so only the base texture * vertex color is used.

// Inputs from vertex shader
in vec4 vColor;
in vec2 vTexCoord0;        // noise-perturbed base water texture coordinates
in vec3 vTexCoord1;        // animated overlay: .xy = UV, .z = 3D texture time
in float vFogCoord;        // view-space Z for fog

// Texture samplers
uniform sampler2D uBaseTexture;       // texture unit 0: base water texture
uniform sampler3D uOverlayTexture3D;  // texture unit 1: 3D animated water texture
uniform sampler2D uOverlayTexture2D;  // texture unit 1: 2D animated water texture (fallback)

// Effect parameters
uniform bool uHasOverlay;         // whether the overlay texture is active
uniform bool uUse3DTexture;       // whether to sample 3D or 2D overlay
uniform bool uHasBaseTexture;     // whether the base texture is bound
uniform vec4 uOverlayConstColor;  // GL_CONSTANT env color for overlay alpha (from tile color)

// Fog parameters (emulating legacy GL_LINEAR fog)
uniform float uFogStart;
uniform float uFogEnd;
uniform vec4 uFogColor;
uniform bool uFogEnabled;

// Output
out vec4 fragColor;

void main() {
    // ------------------------------------------------------------------
    // Step 1: Sample base texture
    // When a base texture is bound: baseColor = texture * vertexColor
    // When not bound: baseColor = vertexColor
    // The vertex shader already applied ambient lighting to vColor.
    // ------------------------------------------------------------------
    vec4 baseColor;
    if (uHasBaseTexture) {
        vec4 texColor = texture(uBaseTexture, vTexCoord0);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    // ------------------------------------------------------------------
    // Step 2: Apply animated overlay texture (if active)
    //
    // Legacy combine mode was GL_ADD:
    //   result.rgb = previousStage.rgb + overlayTexture.rgb
    //
    // Legacy alpha mode was GL_REPLACE from GL_CONSTANT:
    //   result.a = constantColor.a
    //
    // The constant color was set via setTextureEnvColor(0xffffff.inv())
    // which produces RGBA = (0, 0, 0, 0) in the legacy pipeline
    // (the inverted white gives all zeros). However, the alpha from
    // GL_CONSTANT + GL_SRC_ALPHA uses the env color alpha.
    // The setTextureTransparency(0, GL_CONSTANT) sets the alpha source.
    //
    // In practice, the overlay adds its color to the base, and the
    // alpha comes from the constant environment color.
    // ------------------------------------------------------------------
    if (uHasOverlay) {
        vec4 overlayColor;
        if (uUse3DTexture) {
            overlayColor = texture(uOverlayTexture3D, vTexCoord1);
        } else {
            overlayColor = texture(uOverlayTexture2D, vTexCoord1.xy);
        }

        // GL_ADD: result.rgb = base.rgb + overlay.rgb, clamped to [0,1]
        baseColor.rgb = clamp(baseColor.rgb + overlayColor.rgb, 0.0, 1.0);

        // Alpha from GL_CONSTANT source (overlay constant color alpha)
        baseColor.a = uOverlayConstColor.a;
    }

    fragColor = baseColor;

    // ------------------------------------------------------------------
    // Step 3: Fog (emulating legacy GL_LINEAR fog)
    // Legacy used GL_FOG_COORDINATE with linear fog.
    // ------------------------------------------------------------------
    if (uFogEnabled) {
        float fogDist = abs(vFogCoord);
        float fogFactor = clamp((uFogEnd - fogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
        fragColor.rgb = mix(uFogColor.rgb, fragColor.rgb, fogFactor);
    }
}
