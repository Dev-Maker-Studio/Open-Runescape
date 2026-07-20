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

/**
 * Wrapper for OpenGL shader programs.
 *
 * Handles compilation, linking, and uniform management for vertex and fragment shaders.
 * Supports caching uniform locations for efficient access.
 */
class ShaderProgram private constructor(
    val programId: Int,
    private val vertexShaderId: Int,
    private val fragmentShaderId: Int
) {
    private val uniformLocations = mutableMapOf<String, Int>()
    private var isBound = false

    // Temporary arrays for matrix uploads (avoid allocation per frame)
    private val matrix3Buffer = FloatArray(9)
    private val matrix4Buffer = FloatArray(16)

    companion object {
        /**
         * Creates a shader program from vertex and fragment shader source code.
         *
         * @param vertexSource GLSL source code for vertex shader
         * @param fragmentSource GLSL source code for fragment shader
         * @return Compiled and linked ShaderProgram
         * @throws ShaderCompilationException if compilation or linking fails
         */
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

            return ShaderProgram(programId, vertexShaderId, fragmentShaderId)
        }

        /**
         * Creates a shader program from resource files.
         *
         * @param vertexResourcePath Path to vertex shader resource (e.g., "/shaders/model.vert")
         * @param fragmentResourcePath Path to fragment shader resource (e.g., "/shaders/model.frag")
         * @return Compiled and linked ShaderProgram
         */
        fun createFromResources(vertexResourcePath: String, fragmentResourcePath: String): ShaderProgram {
            val vertexSource = loadResource(vertexResourcePath)
            val fragmentSource = loadResource(fragmentResourcePath)
            return create(vertexSource, fragmentSource)
        }

        /**
         * Creates a shader program from source strings.
         * Alias for create() for API consistency.
         */
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

    /**
     * Gets the OpenGL program ID.
     */
    fun getId(): Int = programId

    /**
     * Binds this shader program for use in rendering.
     */
    fun bind() {
        if (!isBound) {
            GLWrapper.useProgram(programId)
            isBound = true
        }
    }

    /**
     * Unbinds this shader program.
     */
    fun unbind() {
        if (isBound) {
            GLWrapper.useProgram(0)
            isBound = false
        }
    }

    /**
     * Gets the location of a uniform variable, caching the result.
     */
    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            GLWrapper.getUniformLocation(programId, name)
        }
    }

    /**
     * Gets the location of an attribute variable.
     */
    fun getAttribLocation(name: String): Int {
        return GLWrapper.getAttribLocation(programId, name)
    }

    // ===== Uniform Setters =====

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

    // ===== Direct Location Setters (for hot paths) =====

    fun setUniform(location: Int, value: Int) {
        GLWrapper.uniform1i(location, value)
    }

    fun setUniform(location: Int, value: Float) {
        GLWrapper.uniform1f(location, value)
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

    /**
     * Releases GPU resources associated with this shader program.
     * Safe to call from any thread - will silently skip if no GL context is available.
     */
    fun destroy() {
        // Check if GL context is available on this thread before making GL calls
        // This prevents LWJGL from aborting the JVM with a native error
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
        // If no context, resources will be freed when the GL context is destroyed

        uniformLocations.clear()
    }
}

/**
 * Exception thrown when shader compilation or linking fails.
 */
class ShaderCompilationException(message: String) : RuntimeException(message)
