#version 330 core

// HD Water Fragment Shader
// Replaces both ReflectiveRipplingWaterEffect (effect 8) and BreakingWaterEffect (effect 9)
//
// Both effects share a very similar fragment shader:
//   1. Sample 3D normal map texture, decode from [0,1] to [-1,1], swizzle .rbga
//   2. Compute view direction from wvVertex (world-view normal)
//   3. Reflect view direction about the sampled normal
//   4. Sample environment cube map using the reflection vector
//   5. Compute specular highlight from sun reflection
//   6. Compute shore factor from water depth
//   7. Blend reflection + specular + optional wave effects
//   8. Apply fog
//
// The key difference between effect 8 and effect 9:
//   Effect 8 (ReflectiveRipplingWaterEffect):
//     - Fresnel: pow(1 - abs(ndote), 2.0) -- stronger edge reflection
//     - No wave intensity/exponent uniforms
//     - surfaceColour = vec4(envColour, fresnel*shoreFactor) + specular*shoreFactor
//
//   Effect 9 (BreakingWaterEffect):
//     - Fresnel: pow(1 - abs(ndote), 1.0) = 1 - abs(ndote) -- linear falloff
//     - Wave intensity/exponent uniforms for breaking wave foam
//     - surfaceColour = mix(reflection, waveColor, waveFactor) + specular*shoreFactor
//
// We unify both into a single shader with uUseWaveEffect toggle.

in vec3 vWorldViewNormal;   // view-space vector for eye direction computation
in float vWaterDepth;       // shore proximity from texcoord.z
in vec3 vTexCoord;          // .xy = animated UV, .z = time for 3D texture
in float vFogFactor;        // pre-computed linear fog factor (0 = clear, 1 = fogged)

// Texture samplers
uniform sampler3D uNormalSampler;     // 3D normal map (animated water normals)
uniform samplerCube uEnvMapSampler;   // environment cube map for reflections

// Sun/specular uniforms
uniform vec3 uSunDir;               // negated sun direction (-sunDirection[0/1/2])
uniform vec4 uSunColour;            // sun color * brightness
uniform float uSunExponent;         // specular power (64 + abs(sunY) * 928)

// Shore/wave uniforms
uniform float uBreakWaterDepth;     // depth at which wave breaking starts
uniform float uBreakWaterOffset;    // offset for wave breaking normal perturbation

// Wave effect uniforms (only used by effect 9)
uniform bool uUseWaveEffect;        // true for effect 9, false for effect 8
uniform vec2 uWaveIntensity;        // (intensityLow, intensityHigh) for wave foam
uniform float uWaveExponent;        // power curve for wave breaking shape

// Fog
uniform vec4 uFogColor;

// Output
out vec4 fragColor;

void main() {
    // ------------------------------------------------------------------
    // Step 1: Sample the 3D normal map
    // Legacy: wnNormal = texture3D(normalSampler, gl_TexCoord[0].xyz).rbga
    // The .rbga swizzle reorders the channels: r -> x, b -> y, g -> z, a -> w
    // Then decode from [0,1] to [-1,1]: xyz = 2*xyz - 1
    // ------------------------------------------------------------------
    vec4 rawNormal = texture(uNormalSampler, vTexCoord);
    vec4 wnNormal;
    wnNormal.x = rawNormal.r;
    wnNormal.y = rawNormal.b;
    wnNormal.z = rawNormal.g;
    wnNormal.w = rawNormal.a;
    wnNormal.xyz = 2.0 * wnNormal.xyz - 1.0;

    // ------------------------------------------------------------------
    // Step 2: Compute view direction and reflection vector
    // Legacy: wnVector = normalize(wvVertex)
    //         wnReflection = reflect(wnVector, wnNormal.xyz)
    // ------------------------------------------------------------------
    vec3 wnVector = normalize(vWorldViewNormal);
    vec3 wnReflection = reflect(wnVector, wnNormal.xyz);

    // ------------------------------------------------------------------
    // Step 3: Sample environment cube map
    // Legacy: envColour = textureCube(envMapSampler, wnReflection).rgb
    // ------------------------------------------------------------------
    vec3 envColour = texture(uEnvMapSampler, wnReflection).rgb;

    // ------------------------------------------------------------------
    // Step 4: Compute specular highlight
    // Legacy: specularColour = sunColour * pow(clamp(-dot(sunDir, wnReflection), 0, 1), sunExponent)
    // ------------------------------------------------------------------
    vec4 specularColour = uSunColour * pow(clamp(-dot(uSunDir, wnReflection), 0.0, 1.0), uSunExponent);

    // ------------------------------------------------------------------
    // Step 5: Shore/depth factor
    // Legacy: shoreFactor = clamp(waterDepth/breakWaterDepth - breakWaterOffset*wnNormal.w, 0, 1)
    // ------------------------------------------------------------------
    float shoreFactor = clamp(vWaterDepth / uBreakWaterDepth - uBreakWaterOffset * wnNormal.w, 0.0, 1.0);

    // ------------------------------------------------------------------
    // Step 6: Fresnel and surface color computation
    // ------------------------------------------------------------------
    float ndote = dot(wnVector, wnNormal.xyz);
    vec4 surfaceColour;

    if (uUseWaveEffect) {
        // Effect 9 (BreakingWaterEffect): wave breaking + linear fresnel
        float waveFactor = pow(1.0 - shoreFactor, uWaveExponent) - 0.5;
        waveFactor = -4.0 * waveFactor * waveFactor + 1.0;

        float fresnel = 1.0 - abs(ndote);  // pow(..., 1.0)

        surfaceColour = mix(
            vec4(envColour, fresnel * shoreFactor),
            uWaveIntensity.x * wnNormal.wwww + uWaveIntensity.y,
            waveFactor
        ) + specularColour * shoreFactor;
    } else {
        // Effect 8 (ReflectiveRipplingWaterEffect): quadratic fresnel, no waves
        float fresnel = pow(1.0 - abs(ndote), 2.0);

        surfaceColour = vec4(envColour, fresnel * shoreFactor) + specularColour * shoreFactor;
    }

    // ------------------------------------------------------------------
    // Step 7: Apply fog
    // Legacy: gl_FragColor = vec4(mix(surfaceColour.rgb, gl_Fog.color.rgb, gl_FogFragCoord), surfaceColour.a)
    // Note: vFogFactor is 0 = clear, 1 = fully fogged (opposite of standard convention)
    // The legacy shader mixes toward fog color as fogFactor increases.
    // ------------------------------------------------------------------
    fragColor = vec4(mix(surfaceColour.rgb, uFogColor.rgb, vFogFactor), surfaceColour.a);
}
