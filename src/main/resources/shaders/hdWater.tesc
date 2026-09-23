#version 400 core

layout(vertices = 3) out;

in vec3 tcWorld[];
in float tcEdge[];
in float tcDepth[];

out vec3 teWorld[];
out float teEdge[];
out float teDepth[];

uniform mat4 uViewProjection;
uniform vec4 uViewport;
uniform float uTessPixels;

vec2 toScreen(vec3 world) {
    vec4 clip = uViewProjection * vec4(world, 1.0);
    return clip.xy / max(clip.w, 1.0) * 0.5 * uViewport.zw;
}

float level(vec3 a, vec3 b) {
    vec4 ca = uViewProjection * vec4(a, 1.0);
    vec4 cb = uViewProjection * vec4(b, 1.0);
    if (ca.w <= 0.0 && cb.w <= 0.0) return 1.0;
    return clamp(distance(toScreen(a), toScreen(b)) / uTessPixels, 1.0, 16.0);
}

void main() {
    teWorld[gl_InvocationID] = tcWorld[gl_InvocationID];
    teEdge[gl_InvocationID] = tcEdge[gl_InvocationID];
    teDepth[gl_InvocationID] = tcDepth[gl_InvocationID];
    if (gl_InvocationID == 0) {
        float e0 = level(tcWorld[1], tcWorld[2]);
        float e1 = level(tcWorld[2], tcWorld[0]);
        float e2 = level(tcWorld[0], tcWorld[1]);
        gl_TessLevelOuter[0] = e0;
        gl_TessLevelOuter[1] = e1;
        gl_TessLevelOuter[2] = e2;
        gl_TessLevelInner[0] = max(e0, max(e1, e2));
    }
}
