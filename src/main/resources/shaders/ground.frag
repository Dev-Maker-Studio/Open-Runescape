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

// Output
out vec4 fragColor;

float calculateShadow() {
    if (!uShadowEnabled) {
        return 1.0;
    }

    // Perspective divide
    vec3 projCoords = vShadowCoord.xyz / vShadowCoord.w;

    // Transform to [0,1] range
    projCoords = projCoords * 0.5 + 0.5;

    // Check if outside shadow map bounds
    if (projCoords.x < 0.0 || projCoords.x > 1.0 ||
        projCoords.y < 0.0 || projCoords.y > 1.0 ||
        projCoords.z > 1.0) {
        return 1.0;
    }

    // Get closest depth from shadow map
    float closestDepth = texture(uShadowMap, projCoords.xy).r;

    // Current fragment depth
    float currentDepth = projCoords.z;

    // Shadow comparison with bias
    float shadow = currentDepth - uShadowBias > closestDepth ? uShadowIntensity : 1.0;

    return shadow;
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
