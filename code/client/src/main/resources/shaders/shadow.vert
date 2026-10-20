#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uLightViewProjection;

uniform mat4 uModelMatrix;

out vec2 vTexCoord;

void main() {
    vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
    gl_Position = uLightViewProjection * worldPos;

    vTexCoord = aTexCoord;
}
