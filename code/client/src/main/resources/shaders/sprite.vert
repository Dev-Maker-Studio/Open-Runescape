#version 330 core

layout(location = 0) in vec2 aPosition;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;
layout(location = 4) in float aTexIndex;
layout(location = 5) in vec4 aClip;

uniform mat4 uProjection;

out vec2 vTexCoord;
out vec4 vColor;
flat out int vTexIndex;
flat out vec4 vClip;

void main() {
    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
    vTexCoord = aTexCoord;
    vClip = aClip;
    vColor = aColor;
    vTexIndex = int(floor(aTexIndex + 0.5));
}
