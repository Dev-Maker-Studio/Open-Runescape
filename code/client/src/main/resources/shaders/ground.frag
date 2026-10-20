#version 330 core

in vec4 vColor;
in vec3 vNormal;
in vec3 vViewPos;
in float vFogFactor;
in vec4 vShadowCoord;

uniform vec4 uFogColor;
uniform bool uShadowEnabled;
uniform sampler2D uShadowMap;
uniform float uShadowBias;
uniform float uShadowIntensity;
uniform vec2 uShadowMapSize;

out vec4 fragColor;

float calculateShadow() {
    if (!uShadowEnabled) {
        return 1.0;
    }

    vec3 projCoords = vShadowCoord.xyz / vShadowCoord.w;

    projCoords = projCoords * 0.5 + 0.5;

    if (projCoords.x < 0.0 || projCoords.x > 1.0 ||
        projCoords.y < 0.0 || projCoords.y > 1.0 ||
        projCoords.z > 1.0) {
        return 1.0;
    }

    float currentDepth = projCoords.z;

    vec3 normal = normalize(vNormal);
    float slopeFactor = 1.0 - abs(dot(normal, vec3(0.0, 1.0, 0.0)));
    float bias = uShadowBias + uShadowBias * 2.0 * slopeFactor;

    vec2 texelSize = 1.0 / uShadowMapSize;

    float shadowSum = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            float closestDepth = texture(uShadowMap, projCoords.xy + offset).r;
            shadowSum += (currentDepth - bias > closestDepth) ? 1.0 : 0.0;
        }
    }

    float shadowFactor = shadowSum / 9.0;

    return mix(1.0, uShadowIntensity, shadowFactor);
}

void main() {
    vec4 color = vColor;

    float shadow = calculateShadow();
    color.rgb *= shadow;

    fragColor = mix(uFogColor, color, vFogFactor);
    fragColor.a = color.a;
}
