#version 330 core
#define MAX_POINT_LIGHTS 128

// Vertex attributes - must match the legacy VBO layout from GroundGL.resetLight():
// - Position: vec3 (12 bytes) at offset 0 (GL_FLOAT, 3 components)
// - Color: RGBA (4 bytes) at offset 12 (GL_UNSIGNED_BYTE, 4 components)
// - TexCoord: vec2 or vec3 (8 or 12 bytes) at offset 16 (GL_FLOAT, 2 or 3 components)
// - Normal: vec3 (12 bytes) at offset 24 or 28 (GL_FLOAT, 3 components) - optional for HD_LIGHT
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec4 aColor;
layout(location = 2) in vec3 aTexCoord;
layout(location = 3) in vec3 aNormal;

// Uniforms
uniform mat4 uViewProjection;
uniform float uTextureScale;  // 1.0 / textureScale for texture coordinate scaling
uniform vec2 uTextureScroll;
uniform bool uHasNormals;     // Whether HD lighting normals are present

// Lighting uniforms (used when uHasNormals is true).
// HD_LIGHT grounds bake sunDotProduct = 1.0 into the per-material vertex colors
// (GroundGL.resetLight, GroundGL.kt:457-460) and rely on runtime lighting instead.
// This replicates the client's own sun formula (same one GroundGL bakes for SD):
//   intensity = sunBrightness + d * (d > 0 ? sunColorIntensity : antiSunColorIntensity)
// where d = dot(sunDirection, normal), both unnormalized, exactly as in
// GroundGL.kt:458-459. uSunDirection here is the RAW toolkit sunDirection.
uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform float uContrast;
uniform float uSunBrightness;
uniform float uContrastPos;   // sunColorIntensity
uniform float uContrastNeg;   // antiSunColorIntensity
uniform int uRawColorDebug;   // 1 = bypass lighting/lights/shadow (debug, -Drender.groundcolor=raw)

// Fog uniforms
uniform float uFogStart;
uniform float uFogEnd;
uniform bool uFogEnabled;

// Outputs to fragment shader
// Point lights, evaluated natively in the modern pipeline. The legacy renderer paints
// these as fixed-function MULTIPLY light meshes over the ground; that path must never
// run here, so the same contribution is computed in-shader instead.
uniform int uPointLightCount;
uniform vec4 uPointLightPos[MAX_POINT_LIGHTS];   // xyz world, w = 1/size2d^2
uniform vec3 uPointLightColor[MAX_POINT_LIGHTS]; // colour * transparency/255

uniform int uWaterMode;
uniform vec4 uStillFog;
uniform vec2 uLapNoise[64];
uniform float uLapTurbulence;
uniform vec3 uLapAmbient;
uniform float uWaterTime;
uniform vec2 uRippleFog;

out vec4 vColor;
out vec2 vTexCoord;
out float vFogFactor;
out vec2 vWorldXZ;   // for the per-fragment ground shadow overlay (see frag)
out vec3 vWaterNoiseCoord;
out float vWaterMix;
out vec3 vPointLight;

void main() {
    vec3 position = aPosition;
    gl_Position = uViewProjection * vec4(position, 1.0);
    float viewZ = gl_Position.w;
    float waterDepth = aTexCoord.z;
    vWaterMix = 0.0;
    vWaterNoiseCoord = vec3(0.0);
    vPointLight = vec3(0.0);
    if (uWaterMode == 3) {
        float blend = (viewZ - uStillFog.x) * 0.001953125;
        float tint = waterDepth * uStillFog.z + uStillFog.w;
        float pull = clamp((viewZ - uStillFog.y) * 0.00390625, 0.0, 1.0) * waterDepth;
        blend = clamp(blend * tint, 0.0, 1.0);
        tint = clamp(tint, 0.0, 1.0);
        position.y -= pull;
        gl_Position = uViewProjection * vec4(position, 1.0);
        vWaterMix = clamp((max(blend, tint) - 0.25) * 2.0, 0.0, 1.0);
    } else if (uWaterMode == 4) {
        vWaterNoiseCoord = vec3(aTexCoord.xy * uTextureScale * 0.25, uWaterTime);
        if (uRippleFog.y > 0.0) vWaterMix = clamp((viewZ - uRippleFog.x) / uRippleFog.y, 0.0, 1.0);
    }

    vec3 color = aColor.rgb;
    vWorldXZ = position.xz;
    vTexCoord = aTexCoord.xy * uTextureScale + uTextureScroll;
    vFogFactor = uFogEnabled ? clamp((uFogEnd - viewZ) / max(uFogEnd - uFogStart, 1e-4), 0.0, 1.0) : 1.0;
    if (uWaterMode == 2) {
        vec2 n = fract(vec2(position.x + position.z, position.z - position.x) * 0.0001220703125) * 64.0;
        vec2 offset = vec2(uLapNoise[int(n.x)].x, uLapNoise[int(n.y)].y) * uLapTurbulence;
        vec2 base = aTexCoord.xy * uTextureScale;
        vTexCoord = base + offset;
        vWaterNoiseCoord = vec3(base * 0.125, uWaterTime);
        vColor = vec4(clamp(aColor.rgb * uLapAmbient, 0.0, 1.0), aColor.a);
        return;
    }
    if (uRawColorDebug == 1) {
        vColor = vec4(color, aColor.a);
        return;
    }
    if (uHasNormals) {
        float d = dot(uSunDirection, aNormal);
        float intensity = uSunBrightness + d * (d > 0.0 ? uContrastPos : uContrastNeg);
        // Multiply by the SUN COLOUR as well as the intensity. Fixed-function ground
        // lighting uses GL_LIGHT0 diffuse = sunColorIntensity * sunColor and
        // GL_LIGHT_MODEL_AMBIENT = sunBrightness * sunColor, so the tint is part of
        // the lit result; writeVertexColor bakes only a SCALAR colourMultiplier, never
        // the tint. Omitting it left textured terrain untinted and ~2x too bright
        // (measured blue/red 0.66 here vs 0.32 in legacy, exactly the 0.35/0.71 the
        // sun colour applies). ground.vert already did this for untextured terrain.
        color = clamp(color * uSunColor * intensity, 0.0, 1.0);
    }
    vec3 n = uHasNormals ? normalize(aNormal) : vec3(0.0, 1.0, 0.0);
    for (int i = 0; i < uPointLightCount; ++i) {
        vec3 toLight = uPointLightPos[i].xyz - aPosition;
        float d2 = dot(toLight, toLight);
        float intensity = max(1.0 - d2 * uPointLightPos[i].w, 0.0);
        if (intensity <= 0.0) continue;
        float ndotl = max(dot(n, normalize(toLight)), 0.0);
        vPointLight += uPointLightColor[i] * min(2.0 * ndotl * intensity, 1.0);
    }
    vColor = vec4(clamp(color, 0.0, 1.0), aColor.a);
}
