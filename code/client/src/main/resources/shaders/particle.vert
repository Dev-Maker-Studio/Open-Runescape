#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uViewProjection;
uniform vec2 uTextureScroll;
uniform vec3 uAmbient;

uniform bool uFogEnabled;
uniform float uFogStart;
uniform float uFogEnd;

out vec2 vTexCoord;
out vec4 vColor;
out float vFogFactor;

void main() {
    gl_Position = uViewProjection * vec4(aPosition, 1.0);

    vTexCoord = aTexCoord + uTextureScroll;
    vColor = vec4(clamp(aColor.rgb * uAmbient, 0.0, 1.0), aColor.a);

    if (uFogEnabled) {
        float dist = gl_Position.w;
        vFogFactor = clamp((uFogEnd - dist) / (uFogEnd - uFogStart), 0.0, 1.0);
    } else {
        vFogFactor = 1.0;
    }
}
