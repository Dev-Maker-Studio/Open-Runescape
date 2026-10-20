#version 330 core

in vec3 vWorldViewNormal;
in float vWaterDepth;
in vec3 vTexCoord;
in float vFogFactor;

uniform sampler3D uNormalSampler;
uniform samplerCube uEnvMapSampler;

uniform vec3 uSunDir;
uniform vec4 uSunColour;
uniform float uSunExponent;

uniform float uBreakWaterDepth;
uniform float uBreakWaterOffset;

uniform bool uUseWaveEffect;
uniform vec2 uWaveIntensity;
uniform float uWaveExponent;

uniform vec4 uFogColor;

out vec4 fragColor;

void main() {
    vec4 rawNormal = texture(uNormalSampler, vTexCoord);
    vec4 wnNormal;
    wnNormal.x = rawNormal.r;
    wnNormal.y = rawNormal.b;
    wnNormal.z = rawNormal.g;
    wnNormal.w = rawNormal.a;
    wnNormal.xyz = 2.0 * wnNormal.xyz - 1.0;

    vec3 wnVector = normalize(vWorldViewNormal);
    vec3 wnReflection = reflect(wnVector, wnNormal.xyz);

    vec3 envColour = texture(uEnvMapSampler, wnReflection).rgb;

    vec4 specularColour = uSunColour * pow(clamp(-dot(uSunDir, wnReflection), 0.0, 1.0), uSunExponent);

    float shoreFactor = clamp(vWaterDepth / uBreakWaterDepth - uBreakWaterOffset * wnNormal.w, 0.0, 1.0);

    float ndote = dot(wnVector, wnNormal.xyz);
    vec4 surfaceColour;

    if (uUseWaveEffect) {
        float waveFactor = pow(1.0 - shoreFactor, uWaveExponent) - 0.5;
        waveFactor = -4.0 * waveFactor * waveFactor + 1.0;

        float fresnel = 1.0 - abs(ndote);

        surfaceColour = mix(
            vec4(envColour, fresnel * shoreFactor),
            uWaveIntensity.x * wnNormal.wwww + uWaveIntensity.y,
            waveFactor
        ) + specularColour * shoreFactor;
    } else {
        float fresnel = pow(1.0 - abs(ndote), 2.0);

        surfaceColour = vec4(envColour, fresnel * shoreFactor) + specularColour * shoreFactor;
    }

    fragColor = vec4(mix(surfaceColour.rgb, uFogColor.rgb, vFogFactor), surfaceColour.a);
}
