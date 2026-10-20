#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uModelViewProjection;
uniform mat4 uModelView;
uniform mat3 uNormalMatrix;

uniform mat3 uViewRotation;

out vec4 vColor;
out vec2 vTexCoord0;
out vec3 vReflectDir;
out float vFogCoord;

void main() {
    gl_Position = uModelViewProjection * vec4(aPosition, 1.0);

    vec4 viewPos4 = uModelView * vec4(aPosition, 1.0);
    vec3 viewPos = viewPos4.xyz;

    vFogCoord = viewPos.z;

    vTexCoord0 = aTexCoord;
    vColor = aColor;

    vec3 viewNormal = normalize(uNormalMatrix * aNormal);
    vec3 eyeDir = normalize(viewPos);
    vec3 reflectVec = reflect(eyeDir, viewNormal);

    vReflectDir = uViewRotation * reflectVec;
}
