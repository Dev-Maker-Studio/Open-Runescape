package com.jagex.game.runetek5.graphics.modern.core

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.COMPILE_STATUS
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FRAGMENT_SHADER
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.LINK_STATUS
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.VERTEX_SHADER
import org.lwjgl.opengl.GL
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class ShaderProgram private constructor(
    val programId: Int,
    private val vertexShaderId: Int,
    private val fragmentShaderId: Int
) {
    private val uniformLocations = mutableMapOf<String, Int>()
    private var isBound = false

    private val matrix3Buffer = FloatArray(9)
    private val matrix4Buffer = FloatArray(16)

    companion object {
        @JvmStatic
        var groundPassProgramId: Int = -1

        @JvmStatic
        var releaseGroundPass: Runnable? = null

        fun create(vertexSource: String, fragmentSource: String): ShaderProgram {
            val vertexShaderId = compileShader(VERTEX_SHADER, vertexSource)
            val fragmentShaderId = try {
                compileShader(FRAGMENT_SHADER, fragmentSource)
            } catch (e: ShaderCompilationException) {
                GLWrapper.deleteShader(vertexShaderId)
                throw e
            }

            val programId = GLWrapper.createProgram()
            GLWrapper.attachShader(programId, vertexShaderId)
            GLWrapper.attachShader(programId, fragmentShaderId)
            GLWrapper.linkProgram(programId)

            if (GLWrapper.getProgramiv(programId, LINK_STATUS) == 0) {
                val log = GLWrapper.getProgramInfoLog(programId)
                GLWrapper.deleteShader(vertexShaderId)
                GLWrapper.deleteShader(fragmentShaderId)
                GLWrapper.deleteProgram(programId)
                throw ShaderCompilationException("Failed to link shader program: $log")
            }

            return ShaderProgram(programId, vertexShaderId, fragmentShaderId).also { com.jagex.game.runetek5.graphics.modern.dump.FrameDump.source(programId, vertexSource, fragmentSource) }
        }

        fun createFromResources(vertexResourcePath: String, fragmentResourcePath: String): ShaderProgram {
            val vertexSource = loadResource(vertexResourcePath)
            val fragmentSource = loadResource(fragmentResourcePath)
            return create(vertexSource, fragmentSource).also { com.jagex.game.runetek5.graphics.modern.dump.FrameDump.name(it.programId, vertexResourcePath.substringAfterLast('/').substringBeforeLast('.')) }
        }

        fun createFromSource(vertexSource: String, fragmentSource: String): ShaderProgram {
            return create(vertexSource, fragmentSource)
        }

        private fun compileShader(type: Int, source: String): Int {
            val shaderId = GLWrapper.createShader(type)
            GLWrapper.shaderSource(shaderId, source)
            GLWrapper.compileShader(shaderId)

            if (GLWrapper.getShaderiv(shaderId, COMPILE_STATUS) == 0) {
                val log = GLWrapper.getShaderInfoLog(shaderId)
                val typeName = if (type == VERTEX_SHADER) "vertex" else "fragment"
                GLWrapper.deleteShader(shaderId)
                throw ShaderCompilationException("Failed to compile $typeName shader: $log")
            }

            return shaderId
        }

        private fun loadResource(path: String): String {
            return ShaderProgram::class.java.getResourceAsStream(path)?.bufferedReader()?.readText()
                ?: throw ShaderCompilationException("Could not load shader resource: $path")
        }
    }

    fun getId(): Int = programId

    fun bind() {
        if (programId != groundPassProgramId) releaseGroundPass?.run()

        GLWrapper.useProgram(programId)
        isBound = true
    }

    fun unbind() {
        if (programId != groundPassProgramId) releaseGroundPass?.run()
        GLWrapper.useProgram(0)
        isBound = false
    }

    fun getArrayUniformLocation(name: String): Int {
        val bare = getUniformLocation(name)
        return if (bare >= 0) bare else getUniformLocation("$name[0]")
    }

    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            GLWrapper.getUniformLocation(programId, name)
        }
    }

    fun getAttribLocation(name: String): Int {
        return GLWrapper.getAttribLocation(programId, name)
    }

    fun setUniform(name: String, value: Int) {
        GLWrapper.uniform1i(getUniformLocation(name), value)
    }

    fun setUniform(name: String, value: Float) {
        GLWrapper.uniform1f(getUniformLocation(name), value)
    }

    fun setUniform(name: String, value: Boolean) {
        GLWrapper.uniform1i(getUniformLocation(name), if (value) 1 else 0)
    }

    fun setUniform(name: String, value: Vector2f) {
        GLWrapper.uniform2f(getUniformLocation(name), value.x, value.y)
    }

    fun setUniform(name: String, x: Float, y: Float) {
        GLWrapper.uniform2f(getUniformLocation(name), x, y)
    }

    fun setUniform(name: String, value: Vector3f) {
        GLWrapper.uniform3f(getUniformLocation(name), value.x, value.y, value.z)
    }

    fun setUniform(name: String, x: Float, y: Float, z: Float) {
        GLWrapper.uniform3f(getUniformLocation(name), x, y, z)
    }

    fun setUniform(name: String, value: Vector4f) {
        GLWrapper.uniform4f(getUniformLocation(name), value.x, value.y, value.z, value.w)
    }

    fun setUniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        GLWrapper.uniform4f(getUniformLocation(name), x, y, z, w)
    }

    fun setUniform(name: String, value: Matrix3f) {
        value.get(matrix3Buffer)
        GLWrapper.uniformMatrix3fv(getUniformLocation(name), false, matrix3Buffer)
    }

    fun setUniform(name: String, value: Matrix4f) {
        value.get(matrix4Buffer)
        GLWrapper.uniformMatrix4fv(getUniformLocation(name), false, matrix4Buffer)
    }

    fun setUniformArray(name: String, values: FloatArray) {
        when (values.size) {
            3 -> GLWrapper.uniform3fv(getUniformLocation(name), values)
            4 -> GLWrapper.uniform4fv(getUniformLocation(name), values)
            else -> throw IllegalArgumentException("Unsupported array size: ${values.size}")
        }
    }

    private var intUniformCache = IntArray(0)
    private var intUniformValid = BooleanArray(0)
    private var floatUniformCache = FloatArray(0)
    private var floatUniformValid = BooleanArray(0)

    fun setUniform(location: Int, value: Int) {
        if (location < 0) return
        if (location >= intUniformCache.size) {
            val n = maxOf(64, location + 1, intUniformCache.size * 2)
            intUniformCache = intUniformCache.copyOf(n)
            intUniformValid = intUniformValid.copyOf(n)
        }
        if (intUniformValid[location] && intUniformCache[location] == value) return
        intUniformCache[location] = value
        intUniformValid[location] = true
        GLWrapper.uniform1i(location, value)
    }

    fun setUniform(location: Int, value: Float) {
        if (location < 0) return
        if (location >= floatUniformCache.size) {
            val n = maxOf(64, location + 1, floatUniformCache.size * 2)
            floatUniformCache = floatUniformCache.copyOf(n)
            floatUniformValid = floatUniformValid.copyOf(n)
        }
        if (floatUniformValid[location] && floatUniformCache[location] == value) return
        floatUniformCache[location] = value
        floatUniformValid[location] = true
        GLWrapper.uniform1f(location, value)
    }

    fun setUniform(location: Int, x: Float, y: Float) {
        GLWrapper.uniform2f(location, x, y)
    }

    fun setUniform(location: Int, x: Float, y: Float, z: Float) {
        GLWrapper.uniform3f(location, x, y, z)
    }

    fun setUniform(location: Int, x: Float, y: Float, z: Float, w: Float) {
        GLWrapper.uniform4f(location, x, y, z, w)
    }

    fun setUniformMatrix4(location: Int, value: Matrix4f) {
        value.get(matrix4Buffer)
        GLWrapper.uniformMatrix4fv(location, false, matrix4Buffer)
    }

    fun destroy() {
        val hasContext = try {
            GL.getCapabilities() != null
        } catch (e: IllegalStateException) {
            false
        }

        if (hasContext) {
            unbind()
            GLWrapper.detachShader(programId, vertexShaderId)
            GLWrapper.detachShader(programId, fragmentShaderId)
            GLWrapper.deleteShader(vertexShaderId)
            GLWrapper.deleteShader(fragmentShaderId)
            GLWrapper.deleteProgram(programId)
        }

        uniformLocations.clear()
    }
}

class ShaderCompilationException(message: String) : RuntimeException(message)
