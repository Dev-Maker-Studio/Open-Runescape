#version 330 core

// Rippling Water Fragment Shader
// Replaces RipplingWaterEffect (effect index 4)
//
// Legacy texture combiner setup:
//   Unit 0: GL_ADD + GL_MODULATE (RGB_SCALE = 2.0)
//     - Adds water ripple texture to vertex color (2x brightness)
//
//   Unit 1: GL_REPLACE + GL_ADD
//     - Uses 1D fog ramp texture for distance-based fade
//     - When fog > 0: adds fog-based alpha ramp
//
// Modern approach:
//   - Sample water texture (2D or 3D with animation)
//   - Add to vertex color with 2x brightness scale
//   - Apply fog-based alpha ramp
//   - Apply standard fog

in vec2 vTexCoord;
in vec4 vColor;
in vec3 vViewPos;
in float vFogFactor;
in float vFogAlpha;

uniform sampler2D uTexture;
uniform sampler3D uTexture3D;
uniform bool uTextureEnabled;
uniform bool uUse3DTexture;
uniform float uAnimTime;        // 3D texture R coordinate (animation time)
uniform vec4 uFogColor;
uniform float uAlphaThreshold;

out vec4 fragColor;

void main() {
    vec4 baseColor = vColor;

    if (uTextureEnabled) {
        vec4 texColor;
        if (uUse3DTexture) {
            texColor = texture(uTexture3D, vec3(vTexCoord, uAnimTime));
        } else {
            texColor = texture(uTexture, vTexCoord);
        }

        // GL_ADD with GL_RGB_SCALE = 2.0: (base + texture) * 2
        baseColor.rgb = clamp((baseColor.rgb + texColor.rgb) * 2.0, 0.0, 1.0);
        // GL_MODULATE for alpha: base.a * texture.a
        baseColor.a *= texColor.a;
    }

    // Apply fog-based alpha ramp from eye-linear texture coordinate
    // Legacy used a 1D texture (2 pixels: 0, 255) sampled at the eye-linear coord
    // In modern path, we use the computed coordinate directly as the alpha factor
    baseColor.a = clamp(baseColor.a + vFogAlpha, 0.0, 1.0);

    if (baseColor.a <= uAlphaThreshold) {
        discard;
    }

    // Apply fog
    fragColor = mix(uFogColor, baseColor, vFogFactor);
    fragColor.a = baseColor.a;
}
