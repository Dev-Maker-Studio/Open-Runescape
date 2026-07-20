#version 330 core

// Inputs from vertex shader
in vec4 vColor;
in vec2 vTexCoord;
in float vFogFactor;

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

// Output
out vec4 fragColor;

void main() {
    vec4 color;
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

    // Apply fog
    color.rgb = mix(uFogColor.rgb, color.rgb, vFogFactor);

    fragColor = color;
}
