#version 330 core
#define MAX_POINT_LIGHTS 128

in vec3 vWorld;
in float vEdge;

uniform sampler2D uSceneColor;
uniform sampler2D uSceneDepth;
uniform sampler3D uWaveNoise;
uniform sampler3D uFoamNoise;
uniform samplerCube uEnvMap;
uniform sampler2D uShadowTex;
uniform sampler2D uReflection;
uniform bool uUsePlanar;
uniform float uPlanarDistort;
uniform bool uHasWaveNoise;
uniform bool uHasFoamNoise;
uniform bool uHasEnvMap;
uniform bool uShadowEnabled;

uniform mat4 uViewProjection;
uniform mat4 uInvViewProjection;
uniform vec4 uViewport;
uniform vec2 uFramebufferSize;

uniform vec3 uSunDirection;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform vec3 uSkyColour;
uniform vec3 uWaterColour;
uniform float uAbsorbDistance;
uniform float uScatter;
uniform float uRefraction;
uniform float uWaveScale;
uniform float uWaveStrength;
uniform float uTime;
uniform float uSlowTime;
uniform vec2 uFlow;
uniform float uShadowScale;
uniform int uReflectionSteps;
uniform bool uFogEnabled;
uniform float uFogStart;
uniform float uFogEnd;

uniform int uPointLightCount;
uniform vec4 uPointLightPos[MAX_POINT_LIGHTS];
uniform vec3 uPointLightColor[MAX_POINT_LIGHTS];

out vec4 fragColor;

const vec3 UP = vec3(0.0, -1.0, 0.0);
const vec3 OCEAN = vec3(0.16, 0.5, 0.78);
const float REFLECTIVITY = 0.2;

vec3 unproject(vec2 fragXY, float depth) {
    vec2 ndc = (fragXY - uViewport.xy) / uViewport.zw * 2.0 - 1.0;
    vec4 world = uInvViewProjection * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return world.xyz / world.w;
}

bool project(vec3 world, out vec3 screen) {
    vec4 clip = uViewProjection * vec4(world, 1.0);
    if (clip.w <= 0.0) return false;
    vec3 ndc = clip.xyz / clip.w;
    screen = vec3((ndc.xy * 0.5 + 0.5) * uViewport.zw + uViewport.xy, ndc.z * 0.5 + 0.5);
    return all(greaterThanEqual(screen.xy, uViewport.xy)) && all(lessThanEqual(screen.xy, uViewport.xy + uViewport.zw));
}

float sceneDepth(vec2 fragXY) {
    return texture(uSceneDepth, fragXY / uFramebufferSize).r;
}

float thicknessAt(vec2 fragXY, vec3 surface, vec3 ray) {
    float depth = sceneDepth(fragXY);
    if (depth >= 1.0) return 1.0e6;
    return dot(unproject(fragXY, depth) - surface, ray);
}

float octave(vec2 xz, float scale, float time, vec2 shift) {
    return texture(uWaveNoise, vec3(xz / scale + shift, time)).r;
}

float waveHeight(vec2 xz, float detail) {
    vec2 p = xz + uFlow;
    return octave(p, uWaveScale, uTime, vec2(0.0))
        + 0.5 * octave(p, uWaveScale * 0.4, fract(uTime * 2.0 + 0.5), vec2(0.31, 0.67))
        + detail * 0.3 * octave(p, uWaveScale * 0.1, fract(uTime * 3.0 + 0.25), vec2(0.73, 0.19));
}

vec3 waterNormal(vec2 xz, float distance) {
    if (!uHasWaveNoise) return UP;
    float detail = 1.0 - smoothstep(1500.0, 6000.0, distance);
    float strength = uWaveStrength / (1.0 + distance / 15000.0);
    float e = uWaveScale / 256.0;
    float h = waveHeight(xz, detail);
    float dx = (waveHeight(xz + vec2(e, 0.0), detail) - h) / e * uWaveScale * 0.05;
    float dz = (waveHeight(xz + vec2(0.0, e), detail) - h) / e * uWaveScale * 0.05;
    return normalize(vec3(-dx * strength, -1.0, -dz * strength));
}

float caustic(vec2 xz) {
    if (!uHasWaveNoise) return 0.0;
    float a = octave(xz + uFlow, uWaveScale * 0.5, uTime, vec2(0.13, 0.41));
    float b = octave(xz - uFlow, uWaveScale * 0.4, fract(uTime * 2.0 + 0.33), vec2(0.57, 0.08));
    float ridge = 1.0 - abs(a - b) * 3.0;
    return pow(clamp(ridge, 0.0, 1.0), 3.0);
}

vec3 skyGradient(vec3 direction) {
    float up = clamp(dot(direction, UP), 0.0, 1.0);
    vec3 zenith = mix(uSkyColour, vec3(0.28, 0.45, 0.72), 0.55);
    return mix(uSkyColour * 1.05, zenith, pow(up, 0.6));
}

vec3 traceReflection(vec3 origin, vec3 direction, out float weight) {
    weight = 0.0;
    vec3 position = origin;
    float stepLength = 48.0;
    for (int i = 0; i < uReflectionSteps; i++) {
        vec3 previous = position;
        position += direction * stepLength;
        stepLength *= 1.15;
        vec3 screen;
        if (!project(position, screen)) break;
        float depth = sceneDepth(screen.xy);
        if (depth >= 1.0 || screen.z <= depth) continue;
        vec3 surface = unproject(screen.xy, depth);
        if (distance(surface, position) > stepLength * 3.0) continue;
        vec3 near = previous;
        vec3 far = position;
        for (int j = 0; j < 6; j++) {
            vec3 middle = (near + far) * 0.5;
            vec3 s;
            if (project(middle, s) && s.z > sceneDepth(s.xy)) far = middle; else near = middle;
        }
        vec3 hit;
        if (!project(far, hit)) break;
        vec2 border = min(hit.xy - uViewport.xy, uViewport.xy + uViewport.zw - hit.xy) / (uViewport.zw * 0.08);
        weight = clamp(min(border.x, border.y), 0.0, 1.0) * (1.0 - float(i) / float(uReflectionSteps));
        return texture(uSceneColor, hit.xy / uFramebufferSize).rgb;
    }
    return vec3(0.0);
}

void main() {
    vec3 eye = unproject(gl_FragCoord.xy, 0.0);
    vec3 ray = normalize(vWorld - eye);
    float viewDistance = distance(vWorld, eye);
    vec3 n = waterNormal(vWorld.xz, viewDistance);
    vec3 toEye = -ray;

    float shadow = uShadowEnabled ? texture(uShadowTex, vWorld.xz * uShadowScale).r : 0.0;
    float sunLit = 1.0 - shadow;
    float sunHeight = clamp(dot(uSunDirection, UP), 0.0, 1.0);
    vec3 sunLight = uSunColor * (0.35 + 0.65 * sunHeight) * sunLit;
    vec3 ambientLight = uSkyColour * 0.35 + uSunColor * uAmbient * 0.4;

    float thickness = max(thicknessAt(gl_FragCoord.xy, vWorld, ray), 0.0);
    float shore = smoothstep(0.0, 64.0, thickness);
    vec2 refracted = gl_FragCoord.xy + n.xz * uRefraction * clamp(thickness / 256.0, 0.0, 1.0);
    float refractedThickness = thicknessAt(refracted, vWorld, ray);
    if (refractedThickness < 0.0) {
        refracted = gl_FragCoord.xy;
        refractedThickness = thickness;
    }
    refractedThickness = max(refractedThickness, 0.0);

    vec3 behind = texture(uSceneColor, refracted / uFramebufferSize).rgb;
    float bedDepth = sceneDepth(refracted);
    if (bedDepth < 1.0) {
        vec3 bed = unproject(refracted, bedDepth);
        float below = max(bed.y - vWorld.y, 0.0);
        float light = caustic(bed.xz + uSunDirection.xz * below) * exp(-below / uAbsorbDistance) * smoothstep(16.0, 96.0, below);
        behind += behind * sunLight * light * 0.45;
    }

    vec3 extinction = vec3(1.6, 1.05, 0.8) / uAbsorbDistance;
    vec3 transmittance = exp(-extinction * refractedThickness);
    float peak = max(max(uWaterColour.r, uWaterColour.g), max(uWaterColour.b, 0.05));
    float low = min(min(uWaterColour.r, uWaterColour.g), uWaterColour.b);
    float saturation = clamp((peak - low) / peak, 0.0, 1.0);
    vec3 scatterColour = mix(OCEAN, uWaterColour / peak, saturation) * uScatter;
    float deep = 1.0 - exp(-refractedThickness / (uAbsorbDistance * 2.0));
    scatterColour *= mix(1.0, 0.42, deep);
    float waveLight = 0.7 + 0.6 * max(dot(n, uSunDirection), 0.0);
    float patches = uHasWaveNoise ? 0.8 + 0.4 * octave(vWorld.xz, uWaveScale * 4.0, uSlowTime, vec2(0.21, 0.83)) : 1.0;
    vec3 inScatter = scatterColour * (ambientLight + sunLight * 0.8) * waveLight * patches;

    float shafts = 0.0;
    float reach = min(refractedThickness, uAbsorbDistance * 2.0);
    for (int i = 1; i <= 3; i++) {
        vec3 p = vWorld + ray * reach * (float(i) / 4.0);
        float below = max(p.y - vWorld.y, 0.0);
        shafts += caustic(p.xz + uSunDirection.xz * below) * exp(-below / uAbsorbDistance);
    }
    inScatter += scatterColour * sunLight * shafts * 0.25;

    vec3 lightSpecular = vec3(0.0);
    for (int i = 0; i < uPointLightCount; i++) {
        vec3 toLight = uPointLightPos[i].xyz - vWorld;
        float attenuation = 1.0 / max(uPointLightPos[i].w * dot(toLight, toLight), 1.0e-6);
        vec3 l = normalize(toLight);
        inScatter += scatterColour * uPointLightColor[i] * attenuation * 0.5;
        vec3 h = normalize(l + toEye);
        lightSpecular += uPointLightColor[i] * attenuation * pow(max(dot(n, h), 0.0), 96.0);
    }

    vec3 body = behind * transmittance + inScatter * (vec3(1.0) - transmittance);

    vec3 reflected = reflect(ray, n);
    vec3 sky = uHasEnvMap ? texture(uEnvMap, vec3(reflected.x, -reflected.y, reflected.z)).rgb : skyGradient(reflected);
    vec3 reflection;
    if (uUsePlanar) {
        vec4 planar = texture(uReflection, gl_FragCoord.xy / uFramebufferSize + n.xz * uPlanarDistort);
        reflection = mix(sky, planar.rgb * 0.85, clamp(planar.a * 4.0, 0.0, 1.0) * shore);
    } else {
        float traced = 0.0;
        vec3 scene = uReflectionSteps > 0 ? traceReflection(vWorld, reflected, traced) : vec3(0.0);
        reflection = mix(sky, scene * 0.75, traced * 0.7 * shore);
    }
    float fresnel = mix(REFLECTIVITY, 1.0, pow(1.0 - clamp(dot(toEye, n), 0.0, 1.0), 5.0)) * mix(0.35, 1.0, shore);

    vec3 halfway = normalize(uSunDirection + toEye);
    float highlight = max(dot(n, halfway), 0.0);
    float glancing = 0.02 + 0.98 * pow(1.0 - clamp(dot(toEye, halfway), 0.0, 1.0), 5.0);
    vec3 specular = uSunColor * sunLit * glancing * (pow(highlight, 512.0) * 1.5 + pow(highlight, 4096.0) * 4.0);

    vec3 colour = mix(body, reflection, fresnel) + specular + lightSpecular;

    if (uHasWaveNoise && uHasFoamNoise) {
        float crest = smoothstep(1.25, 1.55, waveHeight(vWorld.xz, 1.0));
        float spray = texture(uFoamNoise, vec3(vWorld.xz / (uWaveScale * 0.125), uTime)).r;
        float whitecap = crest * smoothstep(0.4, 0.8, spray) * deep * (1.0 - smoothstep(4000.0, 12000.0, viewDistance));
        colour = mix(colour, vec3(0.9) * (ambientLight + sunLight * 0.6), whitecap * 0.35);
    }

    if (uHasFoamNoise) {
        float band = 1.0 - smoothstep(4.0, 72.0, thickness);
        float breakup = texture(uFoamNoise, vec3((vWorld.xz + uFlow) / (uWaveScale * 0.25), uTime)).r;
        float lines = 0.5 + 0.5 * sin(thickness * 0.12 + uTime * 6.2831853 * 2.0 + breakup * 6.0);
        float foam = band * smoothstep(0.7, 0.95, lines) * smoothstep(0.35, 0.75, breakup);
        colour = mix(colour, vec3(0.85) * (ambientLight + sunLight * 0.5), foam * 0.25);
    }

    if (uFogEnabled) {
        float fog = clamp((1.0 / gl_FragCoord.w - uFogStart) / max(uFogEnd - uFogStart, 1.0), 0.0, 1.0);
        colour = mix(colour, uSkyColour, fog);
    }

    fragColor = vec4(colour, vEdge * smoothstep(0.0, 24.0, thickness));
}
