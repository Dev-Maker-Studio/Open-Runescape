package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

/**
 * Manages OpenGL shader programs by linking multiple shaders together.
 */
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

        /**
         * Links multiple OpenGL shaders into a shader program
         * @param toolkit The OpenGL toolkit
         * @param openglShaders Array of shaders to link
         * @return The shader manager, or null if linking failed
         */
        @JvmStatic
        fun addShaders(toolkit: OpenGlToolkit, openglShaders: Array<OpenGlShader>): OpenGLShaderManager? {
            // Validate all shaders are compiled
            for (shader in openglShaders) {
                if (shader == null || shader.shaderHandle <= 0) {
                    return null
                }
            }

            // Create program and attach shaders
            val programHandle = OpenGL.glCreateProgram()
            for (shader in openglShaders) {
                OpenGL.glAttachShader(programHandle, shader.shaderHandle)
            }

            // Link program
            OpenGL.glLinkProgram(programHandle)
            OpenGL.glGetProgramiv(programHandle, GL_LINK_STATUS, BUFFER, 0)

            if (BUFFER[0] == 0) {
                // Get error info log
                OpenGL.glGetProgramiv(programHandle, GL_INFO_LOG_LENGTH, BUFFER, 1)
                if (BUFFER[1] > 1) {
                    val infoLogBuffer = ByteArray(BUFFER[1])
                    OpenGL.glGetProgramInfoLog(programHandle, BUFFER[1], BUFFER, 0, infoLogBuffer, 0)
                }

                // Cleanup on failure
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
