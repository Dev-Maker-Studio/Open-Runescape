#version 330 core

// Reflecting Water Fragment Shader
// Replaces the fixed-function texture combiner setup from ReflectingTextureEffect:
//
// Texture unit 0: base ground/water texture
//   - Standard GL_MODULATE combine mode (or GL_REPLACE when no texture)
//   - loadCombineMode(): GL_MODULATE, GL_REPLACE
//
// Texture unit 1: cube map reflection (when enabled)
//   Legacy setup from toggle() init list (callList 0):
//     loadTextureCombineMode(GL_INTERPOLATE, GL_REPLACE)
//     setTexture(2, GL_PREVIOUS, GL_SRC_ALPHA)
//     setTextureTransparency(0, GL_PRIMARY_COLOR)
//
//   RGB combine = GL_INTERPOLATE:
//     Src0 = GL_TEXTURE (cube map)
//     Src1 = GL_PREVIOUS (base color from unit 0)
//     Src2 = GL_PREVIOUS, GL_SRC_ALPHA (blend factor = previous stage alpha)
//     Result: mix(base.rgb, cubeMap.rgb, previous.a)
//
//   Alpha combine = GL_REPLACE:
//     Src0 = GL_PRIMARY_COLOR (vertex color alpha)
//     Result: alpha = vertexColor.a
//
//   When reflection is unavailable (cubeMap == null):
//     setTextureTransparency(0, GL_PREVIOUS) makes alpha = previous.a
//
//   loadCombineMode() for unit 0: GL_MODULATE, GL_REPLACE
//     RGB = texture * vertexColor, Alpha = texture.a
//
// Final blend summary:
//   blendFactor = baseColor.a (texture.a * vertexColor.a from GL_MODULATE)
//   finalRGB = mix(baseColor.rgb, cubeMapColor.rgb, blendFactor)
//   finalAlpha = vertexColor.a (from GL_PRIMARY_COLOR)

// Inputs from vertex shader
in vec4 vColor;
in vec2 vTexCoord0;        // base texture coordinates
in vec3 vReflectDir;       // reflection vector for cube map sampling
in float vFogCoord;        // view-space Z for fog

// Texture samplers
uniform sampler2D uBaseTexture;       // texture unit 0: base texture
uniform samplerCube uCubeMap;         // texture unit 1: environment cube map

// Effect parameters
uniform bool uHasBaseTexture;     // whether a base texture is bound
uniform bool uReflectionEnabled;  // whether cube map reflection is active

// Fog parameters (emulating legacy GL_LINEAR fog)
uniform float uFogStart;
uniform float uFogEnd;
uniform vec4 uFogColor;
uniform bool uFogEnabled;

// Output
out vec4 fragColor;

void main() {
    // ------------------------------------------------------------------
    // Step 1: Base color from texture unit 0
    //
    // loadCombineMode() was GL_MODULATE, GL_REPLACE:
    //   RGB = texture * vertexColor (GL_MODULATE)
    //   Alpha = texture.a (GL_REPLACE from texture source)
    //
    // loadCombiner() with null texture switches to:
    //   Src0 = GL_PREVIOUS (vertex color passthrough)
    //   So without a texture: base = vertexColor
    // ------------------------------------------------------------------
    vec4 baseColor;
    if (uHasBaseTexture) {
        vec4 texColor = texture(uBaseTexture, vTexCoord0);
        baseColor = texColor * vColor;
    } else {
        baseColor = vColor;
    }

    // ------------------------------------------------------------------
    // Step 2: Cube map reflection blend (if enabled)
    //
    // GL_INTERPOLATE on texture unit 1:
    //   Src0 = cube map texture  (reflection color)
    //   Src1 = previous          (base color from unit 0)
    //   Src2 = GL_PREVIOUS, GL_SRC_ALPHA  (base/previous alpha = blend factor)
    //
    //   result.rgb = cubeMap.rgb * Src2_alpha + base.rgb * (1 - Src2_alpha)
    //              = mix(base.rgb, cubeMap.rgb, previousAlpha)
    //
    // The alpha for blending comes from the previous stage's alpha (base texture
    // alpha * vertex alpha from GL_MODULATE). Higher alpha = more reflection.
    //
    // Alpha output: GL_REPLACE from GL_PRIMARY_COLOR = vertex color alpha
    // ------------------------------------------------------------------
    if (uReflectionEnabled) {
        vec4 cubeMapColor = texture(uCubeMap, vReflectDir);
        float blendFactor = baseColor.a;
        fragColor.rgb = mix(baseColor.rgb, cubeMapColor.rgb, blendFactor);
        fragColor.a = vColor.a;  // GL_REPLACE from GL_PRIMARY_COLOR
    } else {
        fragColor = baseColor;
    }

    // ------------------------------------------------------------------
    // Step 3: Fog (emulating legacy GL_LINEAR fog)
    // ------------------------------------------------------------------
    if (uFogEnabled) {
        float fogDist = abs(vFogCoord);
        float fogFactor = clamp((uFogEnd - fogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
        fragColor.rgb = mix(uFogColor.rgb, fragColor.rgb, fogFactor);
    }
}
