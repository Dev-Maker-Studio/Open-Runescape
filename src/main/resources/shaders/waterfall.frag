#version 330 core

// Waterfall Fragment Shader
// Replaces WaterfallEffect (effect index 5)
//
// The waterfall uses a scrolling texture that overlays the base geometry.
// The texture scrolls vertically (downward) with animation, and can use
// either a 2D or 3D texture for the waterfall pattern.
//
// Legacy texture combiner was straightforward: the waterfall texture on
// unit 1 modulates with the base color from unit 0.

in vec2 vTexCoord;
in vec3 vWaterfallCoord;
in vec4 vColor;
in vec3 vViewPos;
in float vFogFactor;

uniform sampler2D uTexture;       // base texture (unit 0)
uniform sampler3D uTexture3D;     // waterfall 3D texture (unit 1)
uniform sampler2D uTexture2D;     // waterfall 2D texture (fallback)
uniform bool uTextureEnabled;
uniform bool uUse3DTexture;
uniform bool uHasWaterfallTexture;
uniform vec4 uFogColor;
uniform float uAlphaThreshold;

out vec4 fragColor;

void main() {
    // Base color from vertex color and optional base texture
    vec4 baseColor;
    if (uTextureEnabled) {
        vec4 texColor = texture(uTexture, vTexCoord);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    // Apply waterfall overlay texture
    if (uHasWaterfallTexture) {
        vec4 waterfallColor;
        if (uUse3DTexture) {
            waterfallColor = texture(uTexture3D, vWaterfallCoord);
        } else {
            waterfallColor = texture(uTexture2D, vWaterfallCoord.xy);
        }
        // Modulate base with waterfall texture
        baseColor.rgb *= waterfallColor.rgb;
        baseColor.a *= waterfallColor.a;
    }

    if (baseColor.a <= uAlphaThreshold) {
        discard;
    }

    // Apply fog
    fragColor = mix(uFogColor, baseColor, vFogFactor);
    fragColor.a = baseColor.a;
}
