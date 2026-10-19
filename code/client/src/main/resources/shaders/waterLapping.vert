#version 330 core

// Lapping Water Vertex Shader
// Converted from the ARB vertex program in LappingWaterEffect.kt.
//
// The lapping water effect adds Perlin noise-based texture coordinate
// perturbation to create wave-like motion on water surfaces. It also
// supports a secondary animated water texture (either 3D texture with
// time-based Z coordinate, or 2D texture array fallback).
//
// Key differences from the base water shader (StillWaterEffect):
//   - Uses ARB_position_invariant (fixed-function clip transform),
//     so we replicate the standard MVP transform here
//   - Transforms view-space position back to world space to compute
//     noise coordinates from world XZ position
//   - Applies noise perturbation to base texture coordinates
//   - Generates secondary texture coordinates for animated water overlay
//   - Simpler lighting: vertex color * ambient (no multi-light diffuse)
//
// The legacy ARB program used the inverse view rotation matrix loaded
// into GL_TEXTURE1's matrix to invert view space back to world space.

// Vertex attributes
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;      // unused in this effect, but declared for layout compat
layout(location = 2) in vec3 aTexCoord;     // .xy = texture coords, .z = water edge factor
layout(location = 3) in vec4 aColor;

// Matrix uniforms
uniform mat4 uModelView;
uniform mat4 uProjection;
uniform mat4 uTextureMatrix;        // texture unit 0 matrix (rotation/scale for water UVs)
uniform mat4 uInverseViewRotation;  // inverse of the view rotation (for world-space recovery)

// Noise lookup table: 64 vec2 entries packed as vec4 uniforms
// Each entry contains (noiseX, noiseY, 0, 0).
// The legacy pipeline used program.local[0..63] with 2 floats per slot.
// These are updated per-frame from a Perlin noise table based on animation time.
uniform vec4 uNoise[64];

// Effect parameters
uniform float uTurbulence;      // noise amplitude: 0.025 (default), 0.05 (type 2), 0.1 (type 3)
uniform vec3 uAmbientLight;     // sun brightness * sun color RGB
uniform float uTime;            // 3D texture Z coordinate for animation (0..1 over 4000ms cycle)
uniform bool uUse3DTexture;     // whether to use 3D texture (time in .z) or 2D fallback (z=0)

// Fog uniforms (for legacy linear fog emulation)
// Not used in vertex shader fog calculation for lapping water --
// the legacy lapping water uses ARB_position_invariant which relies
// on fixed-function fog. We pass view-space Z for the fragment shader.

// Outputs to fragment shader
out vec4 vColor;
out vec2 vTexCoord0;        // noise-perturbed base texture coordinates
out vec3 vTexCoord1;        // animated overlay texture coordinates (.z = time for 3D tex)
out float vFogCoord;        // view-space Z for fog

void main() {
    // ------------------------------------------------------------------
    // Step 1: Transform position to view space
    // ARB: DP4 viewPos.x/y/z/w, mvMatrix[0..3], iPos
    // ------------------------------------------------------------------
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);

    // ------------------------------------------------------------------
    // Step 2: Fog coordinate from view-space Z
    // ARB: MOV oFogCoord.x, viewPos.z
    // ------------------------------------------------------------------
    vFogCoord = viewPos.z;

    // ------------------------------------------------------------------
    // Step 3: Transform view-space position to world space
    // The legacy pipeline loaded the inverse view rotation matrix into
    // GL_TEXTURE1's matrix. We use the explicit uInverseViewRotation.
    // ARB: DP4 worldPos.x/y/z/w, ivMatrix[0..3], viewPos
    // ------------------------------------------------------------------
    vec4 worldPos = uInverseViewRotation * viewPos;

    // ------------------------------------------------------------------
    // Step 4: Compute noise lookup indices from world-space position
    //
    // The noise lookup uses world-space X+Z and Z-X coordinates:
    //   noiseIndexX = fract((worldX + worldZ) * (1/8192)) * 64
    //   noiseIndexY = fract((worldZ - worldX) * (1/8192)) * 64
    //
    // ARB: ADD noise.x, worldPos.x, worldPos.z
    //      SUB noise.y, worldPos.z, worldPos.x
    //      MUL noise, noise, 0.0001220703125  (= 1/8192)
    //      FRC noise, noise
    //      MUL noise, noise, 64
    // ------------------------------------------------------------------
    vec2 noiseInput;
    noiseInput.x = worldPos.x + worldPos.z;
    noiseInput.y = worldPos.z - worldPos.x;
    noiseInput = fract(noiseInput * 0.0001220703125) * 64.0;

    // ------------------------------------------------------------------
    // Step 5: Lookup noise values from the table
    //
    // ARB: ARL noiseAddr.x, noise.x
    //      MOV noise.x, fNoise[noiseAddr.x].x
    //      ARL noiseAddr.x, noise.y
    //      MOV noise.y, fNoise[noiseAddr.x].y
    //
    // The ARL instruction floors the float to get an integer index.
    // We clamp to [0, 63] to avoid out-of-bounds access.
    // ------------------------------------------------------------------
    int indexX = clamp(int(noiseInput.x), 0, 63);
    int indexY = clamp(int(noiseInput.y), 0, 63);
    vec2 noiseOffset;
    noiseOffset.x = uNoise[indexX].x;
    noiseOffset.y = uNoise[indexY].y;

    // ------------------------------------------------------------------
    // Step 6: Apply turbulence scaling to noise offsets
    // ARB: MUL noise, noise, turbulence.x
    // ------------------------------------------------------------------
    noiseOffset *= uTurbulence;

    // ------------------------------------------------------------------
    // Step 7: Compute base texture coordinates via texture matrix
    // ARB: DP4 texCoord.x, texMatrix[0], iTexCoord
    //      DP4 texCoord.y, texMatrix[1], iTexCoord
    // ------------------------------------------------------------------
    vec4 tc4 = vec4(aTexCoord, 1.0);
    vec2 texCoord;
    texCoord.x = dot(uTextureMatrix[0], tc4);
    texCoord.y = dot(uTextureMatrix[1], tc4);

    // ------------------------------------------------------------------
    // Step 8: Add noise perturbation to produce final texture coordinates
    // ARB: ADD oTexCoord0.xy, texCoord, noise
    //      MOV oTexCoord0.z, 0
    //      MOV oTexCoord0.w, 1
    // ------------------------------------------------------------------
    vTexCoord0 = texCoord + noiseOffset;

    // ------------------------------------------------------------------
    // Step 9: Compute animated overlay texture coordinates
    // ARB: MUL oTexCoord1.xy, texCoord, 0.125
    //      MOV oTexCoord1.zw, time.xxxw   (time.x in z, 1.0 in w)
    //
    // The base texture coords are scaled down by 1/8 for the overlay.
    // The .z component holds the animation time for 3D texture lookup.
    // ------------------------------------------------------------------
    vTexCoord1.xy = texCoord * 0.125;
    if (uUse3DTexture) {
        vTexCoord1.z = uTime;
    } else {
        vTexCoord1.z = 0.0;
    }

    // ------------------------------------------------------------------
    // Step 10: Apply lighting (vertex color * ambient)
    // ARB: MUL oColour.xyz, iColour, lightAmbient
    //      MOV oColour.w, iColour.w
    // ------------------------------------------------------------------
    vColor = vec4(aColor.rgb * uAmbientLight, aColor.a);

    // ------------------------------------------------------------------
    // Step 11: Clip-space transform
    // The legacy ARB program used ARB_position_invariant, meaning the
    // fixed-function pipeline handled the projection. We replicate this
    // with the standard projection * viewPos transform.
    // ------------------------------------------------------------------
    gl_Position = uProjection * viewPos;
}
