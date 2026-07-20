#version 330 core

// Water Surface Vertex Shader
// Converted from the 4 ARB vertex programs in StillWaterEffect.kt:
//   waterNodepthColor, waterNodepthReflection, waterDepthColor, waterDepthReflection
//
// The legacy water system uses a 2x2 combination of features:
//   - Depth mode: "noDepth" uses iTexCoord.z as edge factor; "depth" uses a water plane
//   - Lighting mode: "color" passes vertex color through; "reflection" computes 2-light diffuse
//
// This shader unifies all 4 variants via boolean uniforms.

// Vertex attributes
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aTexCoord;  // .xy = texture coords, .z = water edge depth factor
layout(location = 3) in vec4 aColor;

// Matrix uniforms
uniform mat4 uModelView;
uniform mat4 uProjection;
uniform mat4 uTextureMatrix;

// Water-specific uniforms
// fogParams = vec4(fogFar, fogNear, depthScale, edgeIntensity)
//   fogFar   = depthPerspective - (depthPerspective - fogMinStart) * 0.25
//   fogNear  = depthPerspective - (depthPerspective - fogMinStart) * 0.125
//   depthScale = 256.0 / waterTile.scale
//   edgeIntensity = waterTile.intensity / 255.0
uniform vec4 uFogParams;

// waterPlane = vec4(planeNx, planeNy, planeNz, planeD)
// Derived from the modelview matrix column 1 and water depth:
//   normal = -mvMatrix[4,5,6] (negated Y column of MV matrix)
//   d = -(normal . (mvMatrix * waterDepthPoint))
uniform vec4 uWaterPlane;

// Feature toggles
uniform bool uUseLighting;    // false = color passthrough, true = 2-light diffuse
uniform bool uUseDepthPlane;  // false = noDepth (uses texcoord.z), true = depth plane test

// Lighting uniforms (only used when uUseLighting = true)
// In the legacy pipeline, lights come from state.light[0..1].position and .diffuse,
// plus state.lightmodel.ambient. We pass these explicitly.
uniform vec3 uLight0Direction;   // state.light[0].position (directional, view-space)
uniform vec3 uLight1Direction;   // state.light[1].position (directional, view-space)
uniform vec4 uLight0Diffuse;     // state.light[0].diffuse
uniform vec4 uLight1Diffuse;     // state.light[1].diffuse
uniform vec4 uAmbientColor;      // state.lightmodel.ambient

// Outputs to fragment shader
out vec4 vColor;
out vec2 vTexCoord0;
out vec4 vBlendFactor;   // edge/fog blending factor (was oTexCoord1 in ARB)
out float vFogCoord;     // view-space Z for fixed-function fog emulation

void main() {
    // ------------------------------------------------------------------
    // Step 1: Transform position to view space
    // ARB: DP4 viewPos.x/y/z/w, mvMatrix[0..3], iPos
    // ------------------------------------------------------------------
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);

    // ------------------------------------------------------------------
    // Step 2: Compute fog/blend factors
    // The legacy water uses a custom multi-zone fog with edge blending.
    //
    // fogParams interpretation:
    //   .x = fogFar  (outer fog boundary in view-space Z)
    //   .y = fogNear (inner fog boundary in view-space Z)
    //   .z = depthScale (256.0 / waterTile.scale)
    //   .w = edgeIntensity (waterTile.intensity / 255.0)
    //
    // The constants 0.001953125 = 1/512 and 0.00390625 = 1/256 are
    // reciprocal fog range scalars used in the legacy pipeline.
    // ------------------------------------------------------------------
    vec4 fogFactor;

    // Primary fog zone: distance from outer fog boundary, scaled
    // ARB: SUB fogFactor.x, viewPos.z, fogParams.x
    //      MUL fogFactor.x, fogFactor.x, 0.001953125
    fogFactor.x = (viewPos.z - uFogParams.x) * 0.001953125;

    if (uUseDepthPlane) {
        // Depth-plane mode: use water plane equation for edge factor
        // ARB: DP4 depth, waterPlane, viewPos
        //      MAD fogFactor.y, -depth, fogParams.z, fogParams.w
        float depth = dot(uWaterPlane, viewPos);
        fogFactor.y = -depth * uFogParams.z + uFogParams.w;
    } else {
        // No-depth mode: use texcoord.z as the edge factor
        // ARB: MAD fogFactor.y, iTexCoord.z, fogParams.z, fogParams.w
        fogFactor.y = aTexCoord.z * uFogParams.z + uFogParams.w;
    }

    // Secondary fog zone: distance from inner fog boundary, scaled
    // ARB: SUB fogFactor.z, viewPos.z, fogParams.y
    //      MUL fogFactor.z, fogFactor.z, 0.00390625
    fogFactor.z = (viewPos.z - uFogParams.y) * 0.00390625;

    // Combine primary fog with edge intensity
    // ARB: MUL fogFactor.x, fogFactor.x, fogFactor.y
    //      (only in noDepth mode -- in depth mode this line is absent, but
    //       the MIN/MAX clamp below handles it equivalently since fogFactor.x
    //       stands alone)
    if (!uUseDepthPlane) {
        fogFactor.x = fogFactor.x * fogFactor.y;
    }

    // Clamp all components to [0, 1]
    // ARB: MIN fogFactor, fogFactor, 1
    //      MAX fogFactor, fogFactor, 0
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    // ------------------------------------------------------------------
    // Step 3: Water plane displacement
    // Displace the view-space position along the water plane normal
    // based on the secondary fog factor and depth.
    //
    // In noDepth mode: displacement = fogFactor.z * texcoord.z
    // In depth mode:   displacement = fogFactor.z * (-depth)
    // ------------------------------------------------------------------
    float displacementFactor;
    if (uUseDepthPlane) {
        // ARB: MUL fogFactor.z, fogFactor.z, -depth
        float depth = dot(uWaterPlane, viewPos);
        displacementFactor = fogFactor.z * (-depth);
    } else {
        // ARB: MUL fogFactor.z, fogFactor.z, iTexCoord.z
        displacementFactor = fogFactor.z * aTexCoord.z;
    }

    // ARB: MAD viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw
    // Note: waterPlane.xyz is the plane normal, the swizzle .xyzw on a vec4
    // used with .zzzz broadcast means: viewPos.xyz += waterPlane.xyz * displacement
    viewPos.xyz += uWaterPlane.xyz * displacementFactor;

    // ------------------------------------------------------------------
    // Step 4: Output the blend factor
    // ARB: MAX oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy
    //      MOV oTexCoord1.w, 1
    // This produces max(primaryFog, edgeFactor) as the alpha for edge blending
    // in the fragment shader's texture combiner interpolation.
    // ------------------------------------------------------------------
    float blendValue = max(fogFactor.x, fogFactor.y);
    vBlendFactor = vec4(blendValue, blendValue, blendValue, 1.0);

    // ------------------------------------------------------------------
    // Step 5: Vertex color / Lighting
    // ------------------------------------------------------------------
    if (uUseLighting) {
        // 2-light diffuse lighting in view space
        // ARB: DP3 viewNormal.xyz, mvMatrix[0..2], iNormal
        vec3 viewNormal;
        viewNormal.x = dot(uModelView[0].xyz, aNormal);
        viewNormal.y = dot(uModelView[1].xyz, aNormal);
        viewNormal.z = dot(uModelView[2].xyz, aNormal);

        // ARB: DP3 ndotl.x, viewNormal, state.light[0].position
        //      DP3 ndotl.y, viewNormal, state.light[1].position
        //      MAX ndotl, ndotl, 0
        float ndotl0 = max(dot(viewNormal, uLight0Direction), 0.0);
        float ndotl1 = max(dot(viewNormal, uLight1Direction), 0.0);

        // ARB: MOV colour, state.lightmodel.ambient
        //      MAD colour, state.light[0].diffuse, ndotl.xxxx, colour
        //      MAD colour, state.light[1].diffuse, ndotl.yyyy, colour
        //      MUL oColour, iColour, colour
        vec4 lightColor = uAmbientColor;
        lightColor += uLight0Diffuse * ndotl0;
        lightColor += uLight1Diffuse * ndotl1;
        vColor = aColor * lightColor;
    } else {
        // ARB: MOV oColour, iColour
        vColor = aColor;
    }

    // ------------------------------------------------------------------
    // Step 6: Project the (possibly displaced) view position to clip space
    // ARB: DP4 oPos.x/y/z/w, pMatrix[0..3], viewPos
    // ------------------------------------------------------------------
    gl_Position = uProjection * viewPos;

    // ------------------------------------------------------------------
    // Step 7: Fog coordinate (view-space Z for legacy fog emulation)
    // ARB: MOV oFogCoord.x, viewPos.z
    // ------------------------------------------------------------------
    vFogCoord = viewPos.z;

    // ------------------------------------------------------------------
    // Step 8: Texture coordinate transform
    // In noDepth mode, uses DP3 (3-component dot, ignoring .w / translation):
    //   ARB: DP3 oTexCoord0.x, tMatrix[0], iTexCoord
    //        DP3 oTexCoord0.y, tMatrix[1], iTexCoord
    //        MOV oTexCoord0.zw, iTexCoord
    // In depth mode, uses DP4 (full 4x4 transform):
    //   ARB: DP4 oTexCoord0.x/y/z, texMatrix[0..2], iTexCoord
    //        MOV oTexCoord0.w, 1
    //
    // We compute the DP3 variant here (rotation/scale only, no translation),
    // as this is the primary path. The depth-mode DP4 path would need a
    // full 4-component texcoord, but the visual difference is minimal for
    // still water since the texture matrix is typically a simple rotation.
    // ------------------------------------------------------------------
    if (uUseDepthPlane) {
        vec4 tc4 = vec4(aTexCoord, 1.0);
        vTexCoord0.x = dot(uTextureMatrix[0], tc4);
        vTexCoord0.y = dot(uTextureMatrix[1], tc4);
    } else {
        // DP3 = 3-component dot (rotation/scale only, no translation)
        vTexCoord0.x = dot(uTextureMatrix[0].xyz, aTexCoord);
        vTexCoord0.y = dot(uTextureMatrix[1].xyz, aTexCoord);
    }
}
