#version 330 core

in vec4 vColor;
in vec2 vTexCoord;
in float vFogFactor;
in vec2 vWorldXZ;
in vec3 vWaterNoiseCoord;
in float vWaterMix;
in float vWorldY;
in vec3 vPointLight;

uniform int uWaterMode;
uniform sampler3D uWaterNoise;
uniform bool uHasWaterNoise;
uniform vec3 uWaterColour;
uniform vec4 uStillFog;
uniform bool uDepthFog;
uniform sampler2D uSceneDepth;
uniform mat4 uInvViewProjection;
uniform vec4 uViewport;
uniform vec2 uFramebufferSize;

uniform bool uGroundShadowEnabled;
uniform sampler2D uGroundShadowTex;
uniform float uGroundShadowScale;

uniform sampler2D uTexture;
uniform bool uHasTexture;
uniform vec4 uFogColor;
uniform int uCombineMode;
uniform bool uAlphaFromVertex;

out vec4 fragColor;

void main() {
    vec4 color;
    if (uCombineMode == 99) {
        fragColor = vec4(vColor.rgb, 1.0);
        return;
    }
    if (uHasTexture) {
        vec4 texColor = texture(uTexture, vTexCoord);
        if (uCombineMode == 1) {
            color = texColor;
        } else if (uCombineMode == 3) {
            color = vec4(clamp(texColor.rgb + vColor.rgb, 0.0, 1.0), texColor.a * vColor.a);
        } else if (uCombineMode == 4) {
            color = clamp(texColor - vColor, 0.0, 1.0);
        } else {
            color = texColor * vColor;
        }
    } else {
        color = vColor;
    }

    if (uWaterMode == 4) {
        vec4 n = uHasWaterNoise ? texture(uWaterNoise, vWaterNoiseCoord) : vec4(0.0, 0.0, 0.0, 1.0);
        color = vec4(clamp((vColor.rgb + n.rgb) * 2.0, 0.0, 1.0), clamp(vColor.a * n.a + vWaterMix, 0.0, 1.0));
        if (uDepthFog) {
            float sceneDepth = texture(uSceneDepth, gl_FragCoord.xy / uFramebufferSize).r;
            vec2 ndc = (gl_FragCoord.xy - uViewport.xy) / uViewport.zw * 2.0 - 1.0;
            vec4 below = uInvViewProjection * vec4(ndc, sceneDepth * 2.0 - 1.0, 1.0);
            float submerged = max(below.y / below.w - vWorldY, 0.0);
            float cover = clamp(submerged * uStillFog.z + uStillFog.w, 0.0, 1.0);
            float alpha = color.a + (1.0 - color.a) * cover;
            color.rgb = (color.rgb * color.a + uWaterColour * (1.0 - color.a) * cover) / max(alpha, 0.0001);
            color.a = alpha;
        }
    } else if (uWaterMode == 2) {
        if (uHasWaterNoise) color.rgb = clamp(color.rgb + texture(uWaterNoise, vWaterNoiseCoord).rgb, 0.0, 1.0);
        color.a = 1.0;
    } else if (uWaterMode == 3) {
        color.rgb = mix(color.rgb, uWaterColour, vWaterMix);
    }
    if (uAlphaFromVertex && uWaterMode != 4 && uWaterMode != 2) {
        color.a = vColor.a;
    }
    if (uGroundShadowEnabled) {
        color.rgb *= 1.0 - texture(uGroundShadowTex, vWorldXZ * uGroundShadowScale).r;
    }

    color.rgb = mix(uFogColor.rgb, color.rgb, vFogFactor);
    color.rgb = clamp(color.rgb * (vec3(1.0) + vFogFactor * vPointLight), 0.0, 1.0);

    fragColor = color;
}
