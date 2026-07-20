package com.jagex.game.runetek5.graphics.modern.util

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

/**
 * OpenGL wrapper providing a clean interface over LWJGL3 GL33 functions.
 * This abstraction allows for easier testing and potential WebGL porting.
 *
 * All methods use the OpenGL 3.3 Core Profile, which is:
 * - Compatible with OpenGL ES 3.0 (basis for WebGL 2.0)
 * - Cross-platform (Windows, Linux, macOS)
 * - Modern programmable pipeline only (no fixed-function)
 */
object GLWrapper {

    // ===== Shader Functions =====

    fun createShader(type: Int): Int = glCreateShader(type)

    fun shaderSource(shader: Int, source: String) = glShaderSource(shader, source)

    fun compileShader(shader: Int) = glCompileShader(shader)

    fun getShaderiv(shader: Int, pname: Int): Int {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocInt(1)
            glGetShaderiv(shader, pname, buffer)
            return buffer.get(0)
        }
    }

    fun getShaderInfoLog(shader: Int): String = glGetShaderInfoLog(shader)

    fun deleteShader(shader: Int) = glDeleteShader(shader)

    fun createProgram(): Int = glCreateProgram()

    fun attachShader(program: Int, shader: Int) = glAttachShader(program, shader)

    fun detachShader(program: Int, shader: Int) = glDetachShader(program, shader)

    fun linkProgram(program: Int) = glLinkProgram(program)

    fun getProgramiv(program: Int, pname: Int): Int {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocInt(1)
            glGetProgramiv(program, pname, buffer)
            return buffer.get(0)
        }
    }

    fun getProgramInfoLog(program: Int): String = glGetProgramInfoLog(program)

    fun useProgram(program: Int) = glUseProgram(program)

    fun deleteProgram(program: Int) = glDeleteProgram(program)

    fun getUniformLocation(program: Int, name: String): Int = glGetUniformLocation(program, name)

    fun getAttribLocation(program: Int, name: String): Int = glGetAttribLocation(program, name)

    // ===== Uniform Functions =====

    fun uniform1i(location: Int, value: Int) = glUniform1i(location, value)

    fun uniform1f(location: Int, value: Float) = glUniform1f(location, value)

    fun uniform2f(location: Int, x: Float, y: Float) = glUniform2f(location, x, y)

    fun uniform3f(location: Int, x: Float, y: Float, z: Float) = glUniform3f(location, x, y, z)

    fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) = glUniform4f(location, x, y, z, w)

    fun uniform3fv(location: Int, values: FloatArray) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.floats(*values)
            glUniform3fv(location, buffer)
        }
    }

    fun uniform4fv(location: Int, values: FloatArray) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.floats(*values)
            glUniform4fv(location, buffer)
        }
    }

    fun uniformMatrix3fv(location: Int, transpose: Boolean, values: FloatArray) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.floats(*values)
            glUniformMatrix3fv(location, transpose, buffer)
        }
    }

    fun uniformMatrix4fv(location: Int, transpose: Boolean, values: FloatArray) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.floats(*values)
            glUniformMatrix4fv(location, transpose, buffer)
        }
    }

    // ===== Buffer Functions =====

    fun genBuffers(): Int = glGenBuffers()

    fun genBuffers(count: Int): IntArray {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocInt(count)
            glGenBuffers(buffer)
            val result = IntArray(count)
            buffer.get(result)
            return result
        }
    }

    fun bindBuffer(target: Int, buffer: Int) = glBindBuffer(target, buffer)

    fun bufferData(target: Int, data: FloatBuffer, usage: Int) = glBufferData(target, data, usage)

    fun bufferData(target: Int, data: IntBuffer, usage: Int) = glBufferData(target, data, usage)

    fun bufferData(target: Int, data: ByteBuffer, usage: Int) = glBufferData(target, data, usage)

    fun bufferData(target: Int, data: ShortBuffer, usage: Int) = glBufferData(target, data, usage)

    fun bufferData(target: Int, size: Long, usage: Int) = glBufferData(target, size, usage)

    fun bufferSubData(target: Int, offset: Long, data: FloatBuffer) = glBufferSubData(target, offset, data)

    fun bufferSubData(target: Int, offset: Long, data: ByteBuffer) = glBufferSubData(target, offset, data)

    fun bufferSubData(target: Int, offset: Long, data: ShortBuffer) = glBufferSubData(target, offset, data)

    fun bufferSubData(target: Int, offset: Long, data: IntBuffer) = glBufferSubData(target, offset, data)

    fun deleteBuffers(buffer: Int) = glDeleteBuffers(buffer)

    fun deleteBuffers(buffers: IntArray) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.ints(*buffers)
            glDeleteBuffers(buffer)
        }
    }

    // ===== Vertex Array Functions =====

    fun genVertexArrays(): Int = glGenVertexArrays()

    fun bindVertexArray(array: Int) = glBindVertexArray(array)

    fun deleteVertexArrays(array: Int) = glDeleteVertexArrays(array)

    fun enableVertexAttribArray(index: Int) = glEnableVertexAttribArray(index)

    fun disableVertexAttribArray(index: Int) = glDisableVertexAttribArray(index)

    fun vertexAttribPointer(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        offset: Long
    ) = glVertexAttribPointer(index, size, type, normalized, stride, offset)

    fun vertexAttribIPointer(
        index: Int,
        size: Int,
        type: Int,
        stride: Int,
        offset: Long
    ) = glVertexAttribIPointer(index, size, type, stride, offset)

    // ===== Texture Functions =====

    fun genTextures(): Int = glGenTextures()

    fun bindTexture(target: Int, texture: Int) = glBindTexture(target, texture)

    fun deleteTextures(texture: Int) = glDeleteTextures(texture)

    fun activeTexture(unit: Int) = glActiveTexture(unit)

    fun texParameteri(target: Int, pname: Int, param: Int) = glTexParameteri(target, pname, param)

    fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        type: Int,
        data: ByteBuffer?
    ) = glTexImage2D(target, level, internalFormat, width, height, border, format, type, data)

    fun texSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        data: ByteBuffer
    ) = glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, data)

    fun generateMipmap(target: Int) = glGenerateMipmap(target)

    // ===== Framebuffer Functions =====

    fun genFramebuffers(): Int = glGenFramebuffers()

    fun bindFramebuffer(target: Int, framebuffer: Int) = glBindFramebuffer(target, framebuffer)

    fun deleteFramebuffers(framebuffer: Int) = glDeleteFramebuffers(framebuffer)

    fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) =
        glFramebufferTexture2D(target, attachment, textarget, texture, level)

    fun checkFramebufferStatus(target: Int): Int = glCheckFramebufferStatus(target)

    fun genRenderbuffers(): Int = glGenRenderbuffers()

    fun bindRenderbuffer(target: Int, renderbuffer: Int) = glBindRenderbuffer(target, renderbuffer)

    fun renderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int) =
        glRenderbufferStorage(target, internalFormat, width, height)

    fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) =
        glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)

    fun deleteRenderbuffers(renderbuffer: Int) = glDeleteRenderbuffers(renderbuffer)

    fun renderbufferStorageMultisample(target: Int, samples: Int, internalFormat: Int, width: Int, height: Int) =
        glRenderbufferStorageMultisample(target, samples, internalFormat, width, height)

    fun blitFramebuffer(
        srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int,
        dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int,
        mask: Int, filter: Int
    ) = glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)

    fun drawBuffers(buffers: IntArray) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.ints(*buffers)
            glDrawBuffers(buffer)
        }
    }

    // ===== Draw Functions =====

    fun drawArrays(mode: Int, first: Int, count: Int) = glDrawArrays(mode, first, count)

    fun drawElements(mode: Int, count: Int, type: Int, offset: Long) = glDrawElements(mode, count, type, offset)

    fun drawElementsInstanced(mode: Int, count: Int, type: Int, offset: Long, instanceCount: Int) =
        glDrawElementsInstanced(mode, count, type, offset, instanceCount)

    fun vertexAttribDivisor(index: Int, divisor: Int) = glVertexAttribDivisor(index, divisor)

    // ===== State Functions =====

    fun enable(cap: Int) = glEnable(cap)

    fun disable(cap: Int) = glDisable(cap)

    fun blendFunc(sfactor: Int, dfactor: Int) = glBlendFunc(sfactor, dfactor)

    fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) =
        glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)

    fun depthFunc(func: Int) = glDepthFunc(func)

    fun depthMask(flag: Boolean) = glDepthMask(flag)

    fun cullFace(mode: Int) = glCullFace(mode)

    fun frontFace(mode: Int) = glFrontFace(mode)

    fun viewport(x: Int, y: Int, width: Int, height: Int) = glViewport(x, y, width, height)

    fun scissor(x: Int, y: Int, width: Int, height: Int) = glScissor(x, y, width, height)

    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) =
        glColorMask(red, green, blue, alpha)

    fun clear(mask: Int) = glClear(mask)

    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) =
        glClearColor(red, green, blue, alpha)

    fun clearDepth(depth: Double) = glClearDepth(depth)

    fun polygonMode(face: Int, mode: Int) = glPolygonMode(face, mode)

    fun lineWidth(width: Float) = glLineWidth(width)

    fun pointSize(size: Float) = glPointSize(size)

    // ===== Query Functions =====

    fun getError(): Int = glGetError()

    fun getString(name: Int): String? = glGetString(name)

    fun getInteger(pname: Int): Int {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocInt(1)
            glGetIntegerv(pname, buffer)
            return buffer.get(0)
        }
    }

    fun getFloat(pname: Int): Float {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(1)
            glGetFloatv(pname, buffer)
            return buffer.get(0)
        }
    }

    // ===== Constants =====
    object Constants {
        // Shader types
        const val VERTEX_SHADER = GL_VERTEX_SHADER
        const val FRAGMENT_SHADER = GL_FRAGMENT_SHADER

        // Shader status
        const val COMPILE_STATUS = GL_COMPILE_STATUS
        const val LINK_STATUS = GL_LINK_STATUS

        // Buffer targets
        const val ARRAY_BUFFER = GL_ARRAY_BUFFER
        const val ELEMENT_ARRAY_BUFFER = GL_ELEMENT_ARRAY_BUFFER
        const val UNIFORM_BUFFER = GL_UNIFORM_BUFFER

        // Buffer usage
        const val STATIC_DRAW = GL_STATIC_DRAW
        const val DYNAMIC_DRAW = GL_DYNAMIC_DRAW
        const val STREAM_DRAW = GL_STREAM_DRAW

        // Data types
        const val FLOAT = GL_FLOAT
        const val INT = GL_INT
        const val UNSIGNED_INT = GL_UNSIGNED_INT
        const val UNSIGNED_BYTE = GL_UNSIGNED_BYTE
        const val UNSIGNED_SHORT = GL_UNSIGNED_SHORT
        const val BYTE = GL_BYTE
        const val SHORT = GL_SHORT

        // Primitive types
        const val POINTS = GL_POINTS
        const val LINES = GL_LINES
        const val LINE_STRIP = GL_LINE_STRIP
        const val LINE_LOOP = GL_LINE_LOOP
        const val TRIANGLES = GL_TRIANGLES
        const val TRIANGLE_STRIP = GL_TRIANGLE_STRIP
        const val TRIANGLE_FAN = GL_TRIANGLE_FAN

        // Texture targets
        const val TEXTURE_2D = GL_TEXTURE_2D
        const val TEXTURE_3D = GL_TEXTURE_3D
        const val TEXTURE_CUBE_MAP = GL_TEXTURE_CUBE_MAP

        // Texture parameters
        const val TEXTURE_MIN_FILTER = GL_TEXTURE_MIN_FILTER
        const val TEXTURE_MAG_FILTER = GL_TEXTURE_MAG_FILTER
        const val TEXTURE_WRAP_S = GL_TEXTURE_WRAP_S
        const val TEXTURE_WRAP_T = GL_TEXTURE_WRAP_T
        const val LINEAR = GL_LINEAR
        const val NEAREST = GL_NEAREST
        const val LINEAR_MIPMAP_LINEAR = GL_LINEAR_MIPMAP_LINEAR
        const val CLAMP_TO_EDGE = GL_CLAMP_TO_EDGE
        const val REPEAT = GL_REPEAT

        // Texture formats
        const val RGBA = GL_RGBA
        const val RGB = GL_RGB
        const val RGBA8 = GL_RGBA8
        const val RGB8 = GL_RGB8

        // Texture units
        const val TEXTURE0 = GL_TEXTURE0

        // Framebuffer
        const val FRAMEBUFFER = GL_FRAMEBUFFER
        const val READ_FRAMEBUFFER = GL_READ_FRAMEBUFFER
        const val DRAW_FRAMEBUFFER = GL_DRAW_FRAMEBUFFER
        const val RENDERBUFFER = GL_RENDERBUFFER
        const val COLOR_ATTACHMENT0 = GL_COLOR_ATTACHMENT0
        const val DEPTH_ATTACHMENT = GL_DEPTH_ATTACHMENT
        const val DEPTH_STENCIL_ATTACHMENT = GL_DEPTH_STENCIL_ATTACHMENT
        const val FRAMEBUFFER_COMPLETE = GL_FRAMEBUFFER_COMPLETE
        const val DEPTH24_STENCIL8 = GL_DEPTH24_STENCIL8
        const val DEPTH_COMPONENT = GL_DEPTH_COMPONENT
        const val DEPTH_COMPONENT24 = GL_DEPTH_COMPONENT24

        // Capabilities
        const val DEPTH_TEST = GL_DEPTH_TEST
        const val BLEND = GL_BLEND
        const val CULL_FACE = GL_CULL_FACE
        const val SCISSOR_TEST = GL_SCISSOR_TEST
        const val MULTISAMPLE = GL_MULTISAMPLE

        // Blend factors
        const val SRC_ALPHA = GL_SRC_ALPHA
        const val ONE_MINUS_SRC_ALPHA = GL_ONE_MINUS_SRC_ALPHA
        const val ONE = GL_ONE
        const val ZERO = GL_ZERO
        const val DST_COLOR = GL_DST_COLOR

        // Depth functions
        const val LESS = GL_LESS
        const val LEQUAL = GL_LEQUAL
        const val GREATER = GL_GREATER
        const val ALWAYS = GL_ALWAYS

        // Cull face
        const val BACK = GL_BACK
        const val FRONT = GL_FRONT
        const val FRONT_AND_BACK = GL_FRONT_AND_BACK

        // Front face
        const val CCW = GL_CCW
        const val CW = GL_CW

        // Clear bits
        const val COLOR_BUFFER_BIT = GL_COLOR_BUFFER_BIT
        const val DEPTH_BUFFER_BIT = GL_DEPTH_BUFFER_BIT
        const val STENCIL_BUFFER_BIT = GL_STENCIL_BUFFER_BIT

        // Polygon mode
        const val FILL = GL_FILL
        const val LINE = GL_LINE
        const val POINT = GL_POINT

        // Query
        const val VENDOR = GL_VENDOR
        const val RENDERER = GL_RENDERER
        const val VERSION = GL_VERSION
        const val SHADING_LANGUAGE_VERSION = GL_SHADING_LANGUAGE_VERSION
        const val MAX_TEXTURE_SIZE = GL_MAX_TEXTURE_SIZE
        const val MAX_TEXTURE_IMAGE_UNITS = GL_MAX_TEXTURE_IMAGE_UNITS
    }
}
