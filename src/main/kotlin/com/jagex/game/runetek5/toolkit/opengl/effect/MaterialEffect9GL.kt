package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.graphics.shader.OpenGLShaderManager
import com.jagex.game.runetek5.graphics.shader.OpenGlShader
import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GL_FRAGMENT_SHADER
import com.jagex.game.runetek5.toolkit.opengl.GL_VERTEX_SHADER
import jaggl.OpenGL

class MaterialEffect9GL(
    toolkit: OpenGlToolkit,
    @JvmField val textures: TextureContainerGL
) : MaterialEffectGL(toolkit) {

    @JvmField var loaded = false
    @JvmField var initialized = false
    @JvmField var shaders: OpenGLShaderManager? = null

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
                        "gl_TexCoord[0].z = time;\n" +
                        "gl_TexCoord[0].w = 1.0;\n" +
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
                        "uniform vec2 waveIntensity;\n" +
                        "uniform float waveExponent;\n" +
                        "uniform float breakWaterDepth;\n" +
                        "uniform float breakWaterOffset;\n" +
                        "uniform sampler3D normalSampler;\n" +
                        "uniform samplerCube envMapSampler;\n" +
                        "void main() {\n" +
                        "vec4 wnNormal = texture3D(normalSampler, gl_TexCoord[0].xyz).rbga;\n" +
                        "wnNormal.xyz = 2.0*wnNormal.xyz-1.0;\n" +
                        "vec3 wnVector = normalize(wvVertex);\n" +
                        "vec3 wnReflection = reflect(wnVector, wnNormal.xyz);\n" +
                        "vec3 envColour = textureCube(envMapSampler, wnReflection).rgb;\n" +
                        "vec4 specularColour = sunColour*pow(clamp(-dot(sunDir, wnReflection), 0.0, 1.0), sunExponent);\n" +
                        "float shoreFactor = clamp(waterDepth/breakWaterDepth-breakWaterOffset*wnNormal.w, 0.0, 1.0);\n" +
                        "float waveFactor = pow(1.0-shoreFactor, waveExponent)-0.5;\n" +
                        "waveFactor = -4.0*waveFactor*waveFactor+1.0;\n" +
                        "float ndote = dot(wnVector, wnNormal.xyz);\n" +
                        "float fresnel = pow(1.0-abs(ndote), 1.0);\n" +
                        "vec4 surfaceColour = mix(vec4(envColour, fresnel*shoreFactor), (waveIntensity.x*wnNormal.wwww)+waveIntensity.y, waveFactor)+specularColour*shoreFactor;\n" +
                        "gl_FragColor = vec4(mix(surfaceColour.rgb, gl_Fog.color.rgb, gl_FogFragCoord), surfaceColour.a);\n}\n"
            )
            shaders = OpenGLShaderManager.addShaders(glToolkit, arrayOf(vertexShader!!, fragmentShader!!))
            initialized = shaders != null
        }
    }

    override fun isInitialized(): Boolean {
        return initialized
    }

    override fun disable() {
        if (loaded) {
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(null)
            glToolkit.activateTexture(0)
            glToolkit.setActiveTexture(null)
            OpenGL.glUseProgram(0)
            loaded = false
        }
    }

    override fun loadCombineMode() {
        // NOOP
    }

    override fun loadCombiner(texture: OpenGlTexture?, combineMode: Int) {
        if (!loaded) {
            glToolkit.setActiveTexture(texture)
            glToolkit.loadTextureCombineMode(combineMode)
        }
    }

    override fun applyEffectParams(param1: Int, param2: Int) {
        if (loaded) {
            val delta = 1 shl (param1 and 0x3)
            val scale = (1 shl (param1 shr 3 and 0x7)) / 32.0f
            val breakWaterDepth = param2 and 0xffff
            val breakWaterOffset = (param2 shr 16 and 0x3) / 8.0f
            val intensityHigh = (param2 shr 19 and 0xf) / 16.0f
            val intensityLow = (param2 shr 23 and 0xf) / 16.0f
            val waveExponent = (param2 shr 27 and 0xf).toFloat()
            val handle = shaders!!.programHandle
            OpenGL.glUniform1f(
                OpenGL.glGetUniformLocation(handle, "time"),
                (glToolkit.textureAnimatingProgression * delta % 40000) / 40000.0f
            )
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "scale"), scale)
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "breakWaterDepth"), breakWaterDepth.toFloat())
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "breakWaterOffset"), breakWaterOffset)
            OpenGL.glUniform2f(OpenGL.glGetUniformLocation(handle, "waveIntensity"), intensityLow, intensityHigh)
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "waveExponent"), waveExponent)
        }
    }

    override fun toggle(enabled: Boolean) {
        //System.out.println("Enabled material effect "+this.getClass().getSimpleName());
        if (Loader.MATERIAL_EFFECTS_DISABLED[9]) return
        val cubMapTexture = glToolkit.getCubeMap()
        if (initialized && cubMapTexture != null) {
            val brightness = 1.0f + (1.0f - Math.abs(glToolkit.sunDirection[1])) * 2.0f
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(cubMapTexture)
            glToolkit.activateTexture(0)
            glToolkit.setActiveTexture(textures.hdrTexture3d)
            val handle = shaders!!.programHandle
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
                glToolkit.sunColorRed * brightness,
                glToolkit.sunColorGreen * brightness,
                glToolkit.sunColorBlue * brightness,
                1.0f
            )
            OpenGL.glUniform1f(
                OpenGL.glGetUniformLocation(handle, "sunExponent"),
                64.0f + Math.abs(glToolkit.sunDirection[1]) * 928.0f
            )
            loaded = true
        }
    }
}
