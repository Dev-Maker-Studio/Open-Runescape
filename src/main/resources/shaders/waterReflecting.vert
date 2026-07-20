#version 330 core

// Reflecting Water Vertex Shader
// Converted from the fixed-function pipeline in ReflectingTextureEffect.kt.
//
// The reflecting texture effect uses cube map environment mapping to create
// reflective water surfaces. The legacy pipeline used:
//   - GL_REFLECTION_MAP texture generation mode for auto-computing
//     reflection vectors from the eye direction and surface normal
//   - A cube map texture bound to texture unit 1
//   - GL_INTERPOLATE combine mode: mix(cubeMapColor, baseColor, vertexAlpha)
//
// In GLSL 330 core, we manually compute:
//   1. The eye-to-vertex direction in view space
//   2. The reflection vector from the eye direction and the surface normal
//   3. Transform the reflection vector by the view rotation matrix
//      (to align with the cube map's world-space orientation)
//
// This effect does NOT use any ARB vertex program. The legacy pipeline
// used standard fixed-function vertex processing with GL_REFLECTION_MAP
// texture coordinate generation. We combine the vertex transform with
// the reflection vector computation in a single vertex shader.

// Vertex attributes
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

// Matrix uniforms
uniform mat4 uModelViewProjection;  // combined MVP for clip space
uniform mat4 uModelView;            // for view-space position and normal
uniform mat3 uNormalMatrix;         // inverse-transpose of upper 3x3 of modelview

// View rotation matrix for transforming reflection vectors into cube map space.
// This is the view rotation matrix with translation zeroed (method6534):
// copies the 3x3 rotation part, zeros out translation (col 3) and
// the w-column (row 3 elements [3],[7],[11] = 0, [15] = 1).
// We only need the 3x3 rotation portion for the reflection vector.
uniform mat3 uViewRotation;

// Outputs to fragment shader
out vec4 vColor;
out vec2 vTexCoord0;        // base texture coordinates
out vec3 vReflectDir;       // reflection vector for cube map sampling
out float vFogCoord;        // view-space Z for fog

void main() {
    // ------------------------------------------------------------------
    // Step 1: Transform position to clip space
    // ------------------------------------------------------------------
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    // ------------------------------------------------------------------
    // Step 2: View-space position for reflection calculation
    // ------------------------------------------------------------------
    vec4 viewPos4 = uModelView * vec4(aPosition, 1.0);
    vec3 viewPos = viewPos4.xyz;

    // Fog coordinate from view-space Z
    vFogCoord = viewPos.z;

    // ------------------------------------------------------------------
    // Step 3: Pass through base texture coordinates and vertex color
    // The base texture (unit 0) uses standard texture coordinates.
    // Vertex color includes alpha which controls reflection blend.
    // ------------------------------------------------------------------
    vTexCoord0 = aTexCoord;
    vColor = aColor;

    // ------------------------------------------------------------------
    // Step 4: Compute reflection vector for cube map sampling
    //
    // GL_REFLECTION_MAP computes the reflection of the eye-to-vertex
    // vector about the surface normal. In the fixed-function pipeline:
    //   eyeDir = normalize(vertexPosition_viewSpace)
    //   R = reflect(eyeDir, normal_viewSpace)
    //   texCoord = R (used to sample cube map)
    //
    // The reflection vector is then transformed by the texture matrix
    // (which was set to the view rotation matrix with translation zeroed)
    // to align it with the cube map's world-space coordinate system.
    //
    // reflect(I, N) = I - 2 * dot(N, I) * N
    // where I = incident (eye) direction, N = surface normal
    // ------------------------------------------------------------------
    vec3 viewNormal = normalize(uNormalMatrix * aNormal);
    vec3 eyeDir = normalize(viewPos);
    vec3 reflectVec = reflect(eyeDir, viewNormal);

    // Transform reflection vector from view space to world space
    // using the view rotation matrix (matching the legacy texture matrix setup)
    vReflectDir = uViewRotation * reflectVec;
}
