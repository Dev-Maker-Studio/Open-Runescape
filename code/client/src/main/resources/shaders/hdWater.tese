#version 400 core

layout(triangles, fractional_odd_spacing, ccw) in;

in vec3 teWorld[];
in float teEdge[];
in float teDepth[];

out vec3 vWorld;
out float vEdge;

uniform mat4 uViewProjection;
uniform sampler3D uWaveNoise;
uniform bool uHasWaveNoise;
uniform float uWaveScale;
uniform float uTime;
uniform vec2 uFlow;
uniform float uWaveAmplitude;
uniform bool uHasWaterDepth;

float octave(vec2 xz, float scale, float time, vec2 shift) {
    return texture(uWaveNoise, vec3(xz / scale + shift, time)).r;
}

void main() {
    vec3 b = gl_TessCoord;
    vec3 world = teWorld[0] * b.x + teWorld[1] * b.y + teWorld[2] * b.z;
    float edge = teEdge[0] * b.x + teEdge[1] * b.y + teEdge[2] * b.z;
    float depth = teDepth[0] * b.x + teDepth[1] * b.y + teDepth[2] * b.z;

    if (uHasWaveNoise) {
        vec2 p = world.xz + uFlow;
        float h = octave(p, uWaveScale, uTime, vec2(0.0))
            + 0.5 * octave(p, uWaveScale * 0.4, fract(uTime * 2.0 + 0.5), vec2(0.31, 0.67)) - 0.75;
        float shore = uHasWaterDepth ? clamp(depth / 32.0, 0.0, 1.0) : 1.0;
        float distanceFade = 1.0 - smoothstep(6000.0, 14000.0, (uViewProjection * vec4(world, 1.0)).w);
        world.y -= h * uWaveAmplitude * shore * clamp(edge * 2.0, 0.0, 1.0) * distanceFade;
    }

    vWorld = world;
    vEdge = edge;
    gl_Position = uViewProjection * vec4(world, 1.0);
}
