#version 330 core

// HD Water Vertex Shader
// Replaces both ReflectiveRipplingWaterEffect (effect 8) and BreakingWaterEffect (effect 9)
//
// Both legacy effects use the same vertex shader structure:
//   - gl_ModelViewMatrix * gl_Vertex for view-space position
//   - gl_NormalMatrix * ecVertex.xyz for world-view-space normal recovery
//   - gl_TextureMatrix[0] * gl_MultiTexCoord0 for animated UV coordinates
//   - gl_Fog for linear fog
//   - ftransform() for clip-space position
//
// Uniforms 'time' and 'scale' control the 3D texture animation.
// waterDepth from gl_MultiTexCoord0.z provides the shore proximity factor.
//
// This unified shader handles both effect 8 and effect 9 with a single program.

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aTexCoord;  // .xy = tex coords, .z = water depth
layout(location = 3) in vec4 aColor;

uniform mat4 uModelView;
uniform mat4 uProjection;
uniform mat4 uTextureMatrix;     // texture[0] transform for UV animation
uniform mat3 uNormalMatrix;      // gl_NormalMatrix equivalent

// Animation uniforms
uniform float uTime;             // normalized time for 3D texture Z coordinate
uniform float uScale;            // UV scale factor

// Fog uniforms
uniform float uFogStart;
uniform float uFogEnd;

out vec3 vWorldViewNormal;   // view-space normal (for reflection/specular)
out float vWaterDepth;       // shore proximity factor from texcoord.z
out vec3 vTexCoord;          // .xy = animated UV * scale, .z = time
out float vFogFactor;        // linear fog factor

void main() {
    // Transform to view space
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);

    // Water depth from texture coordinate Z
    // Legacy: waterDepth = gl_MultiTexCoord0.z
    vWaterDepth = aTexCoord.z;

    // Compute world-view-space normal for reflection calculations
    // Legacy: wvVertex.x/y/z = dot(gl_NormalMatrix[0/1/2], ecVertex.xyz)
    // gl_NormalMatrix is the inverse-transpose of upper-left 3x3 of ModelView.
    // But in the legacy shader, it was used to transform the VIEW-SPACE position
    // (not the object-space normal) to recover a world-oriented direction.
    // This gives us a view-to-world-ish direction for the eye vector.
    vWorldViewNormal.x = dot(uNormalMatrix[0], viewPos.xyz);
    vWorldViewNormal.y = dot(uNormalMatrix[1], viewPos.xyz);
    vWorldViewNormal.z = dot(uNormalMatrix[2], viewPos.xyz);

    // Compute animated texture coordinates
    // Legacy: gl_TexCoord[0].x = dot(gl_TextureMatrix[0][0], gl_MultiTexCoord0) * scale
    //         gl_TexCoord[0].y = dot(gl_TextureMatrix[0][1], gl_MultiTexCoord0) * scale
    //         gl_TexCoord[0].z = time
    vec4 tc4 = vec4(aTexCoord, 1.0);
    vTexCoord.x = dot(uTextureMatrix[0], tc4) * uScale;
    vTexCoord.y = dot(uTextureMatrix[1], tc4) * uScale;
    vTexCoord.z = uTime;

    // Linear fog
    // Legacy: gl_FogFragCoord = clamp((ecVertex.z - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0)
    // gl_Fog.scale = 1.0 / (gl_Fog.end - gl_Fog.start)
    float fogScale = 1.0 / (uFogEnd - uFogStart);
    vFogFactor = clamp((viewPos.z - uFogStart) * fogScale, 0.0, 1.0);

    // Clip-space position
    // Legacy: gl_Position = ftransform() = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex
    gl_Position = uProjection * viewPos;
}
