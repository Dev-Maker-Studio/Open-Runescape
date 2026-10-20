package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

class OpenGLShaderManager(
    @JvmField val activeToolkit: OpenGlToolkit,
    @JvmField val programHandle: Int
) {

    @Suppress("removal", "DEPRECATION")
    protected fun finalize() {
        activeToolkit.requestShaderProgramCleanup(programHandle.toLong())
    }

    companion object {
        @JvmField val BUFFER = IntArray(2)

        @JvmStatic
        fun addShaders(toolkit: OpenGlToolkit, openglShaders: Array<OpenGlShader>): OpenGLShaderManager? {
            for (shader in openglShaders) {
                if (shader == null || shader.shaderHandle <= 0) {
                    return null
                }
            }

            val programHandle = OpenGL.glCreateProgram()
            for (shader in openglShaders) {
                OpenGL.glAttachShader(programHandle, shader.shaderHandle)
            }

            OpenGL.glLinkProgram(programHandle)
            OpenGL.glGetProgramiv(programHandle, GL_LINK_STATUS, BUFFER, 0)

            if (BUFFER[0] == 0) {
                OpenGL.glGetProgramiv(programHandle, GL_INFO_LOG_LENGTH, BUFFER, 1)
                if (BUFFER[1] > 1) {
                    val infoLogBuffer = ByteArray(BUFFER[1])
                    OpenGL.glGetProgramInfoLog(programHandle, BUFFER[1], BUFFER, 0, infoLogBuffer, 0)
                }

                if (BUFFER[0] == 0) {
                    for (shader in openglShaders) {
                        OpenGL.glDetachShader(programHandle, shader.shaderHandle)
                    }
                    OpenGL.glDeleteProgram(programHandle)
                    return null
                }
            }

            return OpenGLShaderManager(toolkit, programHandle)
        }
    }
}
