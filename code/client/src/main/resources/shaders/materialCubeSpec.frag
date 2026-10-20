#version 330 core

in vec2 vTexCoord;
in vec4 vColor;
in vec3 vNormal;
in vec3 vViewPos;
in float vFogFactor;

uniform sampler2D uTexture;
uniform bool uTextureEnabled;
uniform vec4 uFogColor;
uniform float uAlphaThreshold;

uniform int uSpecularLevel;
uniform bool uEffectEnabled;
uniform bool uLimitedTextureUnits;

out vec4 fragColor;

const float COS_22_5 = 0.92388;
const float SIN_22_5 = 0.38268;

void main() {
    vec4 baseColor;
    if (uTextureEnabled) {
        vec4 texColor = texture(uTexture, vTexCoord);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    if (baseColor.a <= uAlphaThreshold) {
        discard;
    }

    if (uEffectEnabled) {
        vec3 cubeDir = normalize(vNormal);

        vec3 rotatedDir;
        rotatedDir.x = cubeDir.x;
        rotatedDir.y = cubeDir.y * COS_22_5 - cubeDir.z * SIN_22_5;
        rotatedDir.z = cubeDir.y * SIN_22_5 + cubeDir.z * COS_22_5;

        float specularBase = max(0.0, max(abs(rotatedDir.x), max(abs(rotatedDir.y), abs(rotatedDir.z))));

        float specExponent;
        float scale = uLimitedTextureUnits ? 48.0 / 255.0 : 127.0 / 255.0;

        if (uSpecularLevel == 1) {
            specExponent = 96.0;
        } else if (uSpecularLevel == 2) {
            specExponent = 36.0;
        } else {
            specExponent = 12.0;
        }

        float specular = pow(specularBase, specExponent) * scale;

        if (uLimitedTextureUnits) {
            baseColor.rgb += vec3(specular);
        } else {
            float specSharp = pow(specularBase, 96.0) * scale;
            float specMedium = pow(specularBase, 36.0) * scale;
            baseColor.rgb += vec3(specSharp + specMedium);
        }
    }

    fragColor = mix(uFogColor, baseColor, vFogFactor);
    fragColor.a = baseColor.a;
}
