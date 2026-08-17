#version 330 core
#define MAX_POINT_LIGHTS 128

// Vertex attributes (matching VertexFormats.setupGroundFormat)
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 3) in vec4 aColor;

// Uniforms
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

// Shadow uniforms (for terrain shadow mapping)
uniform mat4 uShadowMatrix;
uniform bool uShadowEnabled;

// Outputs to fragment shader
// Point lights, evaluated natively in the modern pipeline. The legacy renderer paints
// these as fixed-function MULTIPLY light meshes over the ground; that path must never
// run here, so the same contribution is computed in-shader instead.
uniform int uPointLightCount;
uniform vec4 uPointLightPos[MAX_POINT_LIGHTS];   // xyz world, w = 1/size2d^2
uniform vec3 uPointLightColor[MAX_POINT_LIGHTS]; // colour * transparency/255

// Native ground shadows: one intensity texel per shadow cell over the whole ground,
// the same ShadowMapperGL data legacy composites with a fixed-function overlay.
uniform bool uGroundShadowEnabled;
uniform sampler2D uGroundShadowTex;
uniform float uGroundShadowScale;   // 1 / (worldUnitsPerTexel * mapWidth)

out vec4 vColor;
out vec3 vNormal;
out vec3 vViewPos;
out float vFogFactor;
out vec4 vShadowCoord;

void main() {
    // Transform position to clip space
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    // Transform position to view space (for fog calculation)
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);
    vViewPos = viewPos.xyz;

    // Transform normal to view space
    vNormal = normalize(uNormalMatrix * aNormal);

    // Calculate vertex lighting (Gouraud shading)
    float nDotL = dot(vNormal, uSunDirection);
    float lightIntensity = uAmbient + max(nDotL, 0.0) * uContrast;

    // Apply lighting to vertex color
    vec3 litRGB = aColor.rgb * uSunColor * lightIntensity;

    // Point lights: att = 1/(kq*d^2), matching the legacy GL_QUADRATIC_ATTENUATION
    // of 1/size2d^2 with zero constant/linear terms.
    for (int i = 0; i < uPointLightCount; ++i) {
        vec3 toLight = uPointLightPos[i].xyz - aPosition;
        float d2 = dot(toLight, toLight);
        float att = 1.0 / max(uPointLightPos[i].w * d2, 1e-6);
        float ndotl = max(dot(aNormal, normalize(toLight)), 0.0);
        litRGB += aColor.rgb * uPointLightColor[i] * (att * ndotl);
    }

    if (uGroundShadowEnabled) {
        // Legacy builds its overlay texture as 68 wherever shadowMapperData is
        // non-zero, and neighbourCount*17 on the boundary (ShadowTileGL.
        // regenerateTexture) -- so the stored magnitude is really a boolean and full
        // shadow is 68/255. Saturating any non-zero texel reproduces that, and the
        // texture's LINEAR filter supplies the soft edge the neighbour count encoded.
        float raw = texture(uGroundShadowTex, aPosition.xz * uGroundShadowScale).r * 255.0;
        litRGB *= (1.0 - min(raw, 1.0) * (68.0 / 255.0));
    }
    vColor = vec4(clamp(litRGB, 0.0, 1.0), aColor.a);

    // Calculate fog factor
    if (uFogEnabled) {
        float fogDistance = length(viewPos.xyz);
        vFogFactor = clamp((uFogEnd - fogDistance) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }

    // Calculate shadow coordinates
    if (uShadowEnabled) {
        vShadowCoord = uShadowMatrix * vec4(aPosition, 1.0);
    } else {
        vShadowCoord = vec4(0.0);
    }
}
