#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aTexCoord;
layout(location = 3) in vec4 aColor;

uniform mat4 uModelView;
uniform mat4 uProjection;
uniform mat4 uTextureMatrix;
uniform mat4 uInverseViewRotation;

uniform vec4 uNoise[64];

uniform float uTurbulence;
uniform vec3 uAmbientLight;
uniform float uTime;
uniform bool uUse3DTexture;

out vec4 vColor;
out vec2 vTexCoord0;
out vec3 vTexCoord1;
out float vFogCoord;

void main() {
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);

    vFogCoord = viewPos.z;

    vec4 worldPos = uInverseViewRotation * viewPos;

    vec2 noiseInput;
    noiseInput.x = worldPos.x + worldPos.z;
    noiseInput.y = worldPos.z - worldPos.x;
    noiseInput = fract(noiseInput * 0.0001220703125) * 64.0;

    int indexX = clamp(int(noiseInput.x), 0, 63);
    int indexY = clamp(int(noiseInput.y), 0, 63);
    vec2 noiseOffset;
    noiseOffset.x = uNoise[indexX].x;
    noiseOffset.y = uNoise[indexY].y;

    noiseOffset *= uTurbulence;

    vec4 tc4 = vec4(aTexCoord, 1.0);
    vec2 texCoord;
    texCoord.x = dot(uTextureMatrix[0], tc4);
    texCoord.y = dot(uTextureMatrix[1], tc4);

    vTexCoord0 = texCoord + noiseOffset;

    vTexCoord1.xy = texCoord * 0.125;
    if (uUse3DTexture) {
        vTexCoord1.z = uTime;
    } else {
        vTexCoord1.z = 0.0;
    }

    vColor = vec4(aColor.rgb * uAmbientLight, aColor.a);

    gl_Position = uProjection * viewPos;
}
