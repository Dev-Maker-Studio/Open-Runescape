#version 330 core

// Waterfall Vertex Shader
// Replaces WaterfallEffect (effect index 5)
//
// Legacy behavior:
//   - GL_EYE_LINEAR texgen for S and T on texture unit 1
//     (eye-space plane parameters determine scroll direction and speed)
//   - GL_OBJECT_LINEAR texgen for R on texture unit 1 (3D texture Z animation)
//   - The plane parameters encode:
//     S: position along X or Z axis (controls horizontal scroll scale)
//     T: position along Y axis (controls vertical scroll, animated over time)
//     R: animated time value for 3D texture Z coordinate
//
// The waterfall effect creates a scrolling texture that flows downward,
// with the scroll direction determined by the orientation flags (flipped or not).

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uModelViewProjection;
uniform mat4 uModelView;
uniform mat3 uNormalMatrix;

// Lighting uniforms
uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform float uContrast;

// Fog uniforms
uniform float uFogStart;
uniform float uFogEnd;
uniform bool uFogEnabled;

// Waterfall-specific uniforms
// These replicate the GL_EYE_LINEAR plane parameters:
//   uEyePlaneS: (position, 0, 0, 0) or (0, 0, position, 0) depending on orientation
//   uEyePlaneT: (0, position, 0, animatedOffset) for vertical scroll with animation
uniform vec4 uEyePlaneS;
uniform vec4 uEyePlaneT;
uniform float uAnimTimeR;     // 3D texture R coordinate for animation
uniform bool uUse3DTexture;

out vec2 vTexCoord;          // base texture coordinates
out vec3 vWaterfallCoord;    // generated texcoords for waterfall texture (.z = 3D anim)
out vec4 vColor;
out vec3 vViewPos;
out float vFogFactor;

void main() {
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    vec4 viewPos = uModelView * vec4(aPosition, 1.0);
    vViewPos = viewPos.xyz;

    vTexCoord = aTexCoord;

    // Compute eye-linear texture coordinates for the waterfall
    // Legacy: GL_EYE_LINEAR computes S = dot(eyePlane, viewPos) for each coordinate
    vWaterfallCoord.x = dot(uEyePlaneS, viewPos);
    vWaterfallCoord.y = dot(uEyePlaneT, viewPos);
    if (uUse3DTexture) {
        vWaterfallCoord.z = uAnimTimeR;
    } else {
        vWaterfallCoord.z = 0.0;
    }

    // Normal and lighting
    vec3 normal = normalize(uNormalMatrix * aNormal);
    float nDotL = dot(normal, uSunDirection);
    float lightIntensity = uAmbient + max(nDotL, 0.0) * uContrast;
    vColor = vec4(aColor.rgb * uSunColor * lightIntensity, aColor.a);

    // Fog
    if (uFogEnabled) {
        float fogDistance = length(viewPos.xyz);
        vFogFactor = clamp((uFogEnd - fogDistance) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }
}
