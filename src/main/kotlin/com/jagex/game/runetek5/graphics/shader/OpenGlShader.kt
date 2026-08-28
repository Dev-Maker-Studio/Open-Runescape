package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import jaggl.OpenGL

class OpenGlShader(
    @JvmField val activeToolkit: OpenGlToolkit,
    @JvmField val shaderHandle: Int
) {

    @Suppress("DEPRECATION", "removal")
    @Throws(Throwable::class)
    protected fun finalize() {
        this.activeToolkit.requestShaderProgramCleanup(this.shaderHandle.toLong())
    }

    companion object {
        @JvmField val FETCHED_SHADER_PARAMETERS = IntArray(2)

        @JvmStatic
        fun createShader(toolkit: OpenGlToolkit, shaderType: Int, source: String): OpenGlShader? {
            val shaderHandle = OpenGL.glCreateShader(shaderType)
            OpenGL.glShaderSource(shaderHandle, source)
            OpenGL.glCompileShader(shaderHandle)
            OpenGL.glGetShaderiv(shaderHandle, 35713, FETCHED_SHADER_PARAMETERS, 0)
            if (FETCHED_SHADER_PARAMETERS[0] == 0) {
                OpenGL.glGetShaderiv(shaderHandle, 35716, FETCHED_SHADER_PARAMETERS, 1)
                if (FETCHED_SHADER_PARAMETERS[1] > 1) {
                    val shaderInfoLog = ByteArray(FETCHED_SHADER_PARAMETERS[1])
                    OpenGL.glGetShaderInfoLog(shaderHandle, FETCHED_SHADER_PARAMETERS[1], FETCHED_SHADER_PARAMETERS, 0, shaderInfoLog, 0)
                    println(String(shaderInfoLog))
                }
                if (FETCHED_SHADER_PARAMETERS[0] == 0) {
                    OpenGL.glDeleteShader(shaderHandle)
                    return null
                }
            }
            return OpenGlShader(toolkit, shaderHandle)
        }
    }
}
