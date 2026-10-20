#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

out vec2 vTexCoord;
out vec4 vColor;
out float vFogFactor;

void main() {
    gl_Position = vec4(aPosition, 1.0);

    vTexCoord = aTexCoord;
    vColor = aColor;

    vFogFactor = 1.0;
}
