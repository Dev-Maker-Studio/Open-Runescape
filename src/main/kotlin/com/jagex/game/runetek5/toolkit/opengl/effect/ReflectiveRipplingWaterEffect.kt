package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.graphics.shader.OpenGLShaderManager
import com.jagex.game.runetek5.graphics.shader.OpenGlShader
import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.GL_FRAGMENT_SHADER
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.GL_VERTEX_SHADER

import jaggl.OpenGL

/**
 *
 * @author Steven - 19/07/2023
 * Handles display of floor water tiles
 * Shows water with waves that reflects light
 * Tested by adding this.material_effects[4] = this.material_effects[8];
 * at the bottom of the constructor of
 * {@link com.jagex.game.runetek5.toolkit.opengl.EffectManagerGL}
 *
 */
class ReflectiveRipplingWaterEffect(
    openglToolkit: OpenGlToolkit,
    @JvmField val textures: TextureContainerGL
) : MaterialEffectGL(openglToolkit) {

    @JvmField var active = false
    @JvmField var initialized = false
    @JvmField var shaderManager: OpenGLShaderManager? = null

    init {
        if (textures.hdrTexture3d != null && glToolkit.arbVertexShader && glToolkit.arbFragmentShader) {
            val vertexShader = OpenGlShader.createShader(
                glToolkit, GL_VERTEX_SHADER,
                "uniform float time;\n" +
                        "uniform float scale;\n" +
                        "varying vec3 wvVertex;\n" +
                        "varying float waterDepth;\n" +
                        "void main() {\n" +
                        "waterDepth = gl_MultiTexCoord0.z;\n" +
                        "vec4 ecVertex = gl_ModelViewMatrix*gl_Vertex;\n" +
                        "wvVertex.x = dot(gl_NormalMatrix[0], ecVertex.xyz);\n" +
                        "wvVertex.y = dot(gl_NormalMatrix[1], ecVertex.xyz);\n" +
                        "wvVertex.z = dot(gl_NormalMatrix[2], ecVertex.xyz);\n" +
                        "gl_TexCoord[0].x = dot(gl_TextureMatrix[0][0], gl_MultiTexCoord0)*scale;\n" +
                        "gl_TexCoord[0].y = dot(gl_TextureMatrix[0][1], gl_MultiTexCoord0)*scale;\n" +
                        "gl_TexCoord[0].z = time;\ngl_TexCoord[0].w = 1.0;\n" +
                        "gl_FogFragCoord = clamp((ecVertex.z-gl_Fog.start)*gl_Fog.scale, 0.0, 1.0);\n" +
                        "gl_Position = ftransform();\n}\n"
            )
            val fragmentShader = OpenGlShader.createShader(
                glToolkit, GL_FRAGMENT_SHADER,
                "varying vec3 wvVertex;\n" +
                        "varying float waterDepth;\n" +
                        "uniform vec3 sunDir;\n" +
                        "uniform vec4 sunColour;\n" +
                        "uniform float sunExponent;\n" +
                        "uniform float breakWaterDepth;\n" +
                        "uniform float breakWaterOffset;\n" +
                        "uniform sampler3D normalSampler;\n" +
                        "uniform samplerCube envMapSampler;\n" +
                        "void main() {\nvec4 wnNormal = texture3D(normalSampler, gl_TexCoord[0].xyz).rbga;\n" +
                        "wnNormal.xyz = 2.0*wnNormal.xyz-1.0;\n" +
                        "vec3 wnVector = normalize(wvVertex);\n" +
                        "vec3 wnReflection = reflect(wnVector, wnNormal.xyz);\n" +
                        "vec3 envColour = textureCube(envMapSampler, wnReflection).rgb;\n" +
                        "vec4 specularColour = sunColour*pow(clamp(-dot(sunDir, wnReflection), 0.0, 1.0), sunExponent);\n" +
                        "float shoreFactor = clamp(waterDepth/breakWaterDepth-breakWaterOffset*wnNormal.w, 0.0, 1.0);\n" +
                        "float ndote = dot(wnVector, wnNormal.xyz);\n" +
                        "float fresnel = pow(1.0-abs(ndote), 2.0);\n" +
                        "vec4 surfaceColour = vec4(envColour, fresnel*shoreFactor)+specularColour*shoreFactor;\n" +
                        "gl_FragColor = vec4(mix(surfaceColour.rgb, gl_Fog.color.rgb, gl_FogFragCoord), surfaceColour.a);\n}\n"
            )
            shaderManager = OpenGLShaderManager.addShaders(glToolkit, arrayOf(vertexShader!!, fragmentShader!!))
            initialized = shaderManager != null
        }
    }

    override fun toggle(enabled: Boolean) {
        println("Enabled material effect " + this.javaClass.simpleName)
        if (Loader.MATERIAL_EFFECTS_DISABLED[8]) return
        val texture = glToolkit.getCubeMap()
        if (initialized && texture != null) {
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(texture)
            glToolkit.activateTexture(0)
            glToolkit.setActiveTexture(textures.hdrTexture3d)
            val handle = shaderManager!!.programHandle
            OpenGL.glUseProgram(handle)
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "normalSampler"), 0)
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "envMapSampler"), 1)
            OpenGL.glUniform3f(
                OpenGL.glGetUniformLocation(handle, "sunDir"),
                -glToolkit.sunDirection[0],
                -glToolkit.sunDirection[1],
                -glToolkit.sunDirection[2]
            )
            OpenGL.glUniform4f(
                OpenGL.glGetUniformLocation(handle, "sunColour"),
                glToolkit.sunColorRed,
                glToolkit.sunColorGreen,
                glToolkit.sunColorBlue,
                1.0f
            )
            OpenGL.glUniform1f(
                OpenGL.glGetUniformLocation(handle, "sunExponent"),
                96.0f + Math.abs(glToolkit.sunDirection[1]) * 928.0f
            )
            active = true
        }
    }

    override fun isInitialized(): Boolean {
        return initialized
    }

    override fun disable() {
        if (active) {
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(null)
            glToolkit.activateTexture(0)
            glToolkit.setActiveTexture(null)
            OpenGL.glUseProgram(0)
            active = false
        }
    }

    override fun loadCombineMode() {
    }

    override fun loadCombiner(texture: OpenGlTexture?, combineMode: Int) {
        if (!active) {
            glToolkit.setActiveTexture(texture)
            glToolkit.loadTextureCombineMode(combineMode)
        }
    }

    override fun applyEffectParams(effectParam1: Int, effectParam2: Int) {
        if (active) {
            val delta = 1 shl (effectParam1 and 0x3)
            val waterScale = (1 shl (effectParam1 shr 3 and 0x7)) / 32.0f
            val breakWaterDepth = effectParam2 and 0xffff
            val breakWaterOffset = (effectParam2 shr 16 and 0x3) / 8.0f
            val handle = shaderManager!!.programHandle
            OpenGL.glUniform1f(
                OpenGL.glGetUniformLocation(handle, "time"),
                (glToolkit.textureAnimatingProgression * delta % 40000) / 40000.0f
            )
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "scale"), waterScale)
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "breakWaterDepth"), breakWaterDepth.toFloat())
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "breakWaterOffset"), breakWaterOffset)
        }
    }
}
