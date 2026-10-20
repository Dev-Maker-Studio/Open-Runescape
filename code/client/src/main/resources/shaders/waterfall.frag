#version 330 core

in vec2 vTexCoord;
in vec3 vWaterfallCoord;
in vec4 vColor;
in vec3 vViewPos;
in float vFogFactor;

uniform sampler2D uTexture;
uniform sampler3D uTexture3D;
uniform sampler2D uTexture2D;
uniform bool uTextureEnabled;
uniform bool uUse3DTexture;
uniform bool uHasWaterfallTexture;
uniform vec4 uFogColor;
uniform float uAlphaThreshold;

out vec4 fragColor;

void main() {
    vec4 baseColor;
    if (uTextureEnabled) {
        vec4 texColor = texture(uTexture, vTexCoord);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    if (uHasWaterfallTexture) {
        vec4 waterfallColor;
        if (uUse3DTexture) {
            waterfallColor = texture(uTexture3D, vWaterfallCoord);
        } else {
            waterfallColor = texture(uTexture2D, vWaterfallCoord.xy);
        }
        baseColor.rgb *= waterfallColor.rgb;
        baseColor.a *= waterfallColor.a;
    }

    if (baseColor.a <= uAlphaThreshold) {
        discard;
    }

    fragColor = mix(uFogColor, baseColor, vFogFactor);
    fragColor.a = baseColor.a;
}
