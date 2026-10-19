#version 400 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec4 aColor;
layout(location = 2) in vec3 aTexCoord;

out vec3 tcWorld;
out float tcEdge;
out float tcDepth;

void main() {
    tcWorld = aPosition;
    tcEdge = aColor.a;
    tcDepth = aTexCoord.z;
}
