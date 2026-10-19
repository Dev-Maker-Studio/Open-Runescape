#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec4 aColor;

uniform mat4 uViewProjection;

out vec3 vWorld;
out float vEdge;

void main() {
    vWorld = aPosition;
    vEdge = aColor.a;
    gl_Position = uViewProjection * vec4(aPosition, 1.0);
}
