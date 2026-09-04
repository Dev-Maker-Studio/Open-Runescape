#version 330 core

// Inputs from vertex shader
in vec4 vColor;
in vec3 vNormal;
in vec3 vViewPos;
in float vFogFactor;
in vec4 vShadowCoord;

// Uniforms
uniform vec4 uFogColor;
uniform bool uShadowEnabled;
uniform sampler2D uShadowMap;
uniform float uShadowBias;
uniform float uShadowIntensity;
uniform vec2 uShadowMapSize;  // Shadow map dimensions (width, height) for texel size

// Output
out vec4 fragColor;

// Percentage Closer Filtering (PCF) shadow calculation.
// Samples a 3x3 grid around the shadow coordinate to produce soft shadow edges.
// The kernel averages 9 depth comparisons, weighting the result between
// full shadow (uShadowIntensity) and full light (1.0).
float calculateShadow() {
    if (!uShadowEnabled) {
        return 1.0;
    }

    // Perspective divide (identity for orthographic, but correct for both)
    vec3 projCoords = vShadowCoord.xyz / vShadowCoord.w;

    // Transform from NDC [-1,1] to texture coordinates [0,1]
    projCoords = projCoords * 0.5 + 0.5;

    // Fragments outside the shadow map receive no shadow
    if (projCoords.x < 0.0 || projCoords.x > 1.0 ||
        projCoords.y < 0.0 || projCoords.y > 1.0 ||
        projCoords.z > 1.0) {
        return 1.0;
    }

    float currentDepth = projCoords.z;

    // Slope-scaled bias: surfaces facing away from the light need more bias
    // to avoid shadow acne. The dot product between normal and light direction
    // (encoded in the shadow matrix) provides the slope factor.
    // A minimum bias of uShadowBias prevents acne on flat surfaces,
    // while the maximum prevents excessive peter-panning.
    vec3 normal = normalize(vNormal);
    float slopeFactor = 1.0 - abs(dot(normal, vec3(0.0, 1.0, 0.0)));
    float bias = uShadowBias + uShadowBias * 2.0 * slopeFactor;

    // Texel size for sampling neighbors
    vec2 texelSize = 1.0 / uShadowMapSize;

    // 3x3 PCF kernel: sample 9 positions around the shadow coordinate
    float shadowSum = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            float closestDepth = texture(uShadowMap, projCoords.xy + offset).r;
            // Compare: if current depth (with bias) exceeds the stored depth, this sample is in shadow
            shadowSum += (currentDepth - bias > closestDepth) ? 1.0 : 0.0;
        }
    }

    // Average the 9 samples: 0.0 = fully lit, 1.0 = fully shadowed
    float shadowFactor = shadowSum / 9.0;

    // Interpolate between full light (1.0) and shadow darkness (uShadowIntensity)
    // uShadowIntensity controls how dark the shadows are (0.0 = pitch black, 1.0 = no shadow)
    return mix(1.0, uShadowIntensity, shadowFactor);
}

void main() {
    vec4 color = vColor;

    // Apply shadow
    float shadow = calculateShadow();
    color.rgb *= shadow;

    // Apply fog
    fragColor = mix(uFogColor, color, vFogFactor);
    fragColor.a = color.a;
}
