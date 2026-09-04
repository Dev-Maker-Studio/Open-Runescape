package com.jagex.game.runetek5.graphics.modern.core

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FLOAT
import org.lwjgl.opengl.GL
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_BYTE

/**
 * Vertex Array Object (VAO) wrapper for modern OpenGL rendering.
 *
 * VAOs store the vertex attribute configuration, eliminating the need
 * to re-specify vertex pointers for each draw call.
 *
 * Typical usage:
 * ```kotlin
 * val vao = VertexArray()
 * vao.bind()
 * vertexBuffer.bind()
 *
 * // Configure attributes (stored in VAO)
 * vao.setAttribute(0, 3, FLOAT, false, 32, 0)   // position
 * vao.setAttribute(1, 3, FLOAT, false, 32, 12)  // normal
 * vao.setAttribute(2, 2, FLOAT, false, 32, 24)  // texcoord
 *
 * indexBuffer.bind()
 * vao.unbind()
 *
 * // During rendering:
 * vao.bind()
 * glDrawElements(...)
 * ```
 */
class VertexArray {
    val id: Int = GLWrapper.genVertexArrays()
    private val enabledAttributes = mutableSetOf<Int>()

    /**
     * Binds this VAO.
     */
    fun bind() {
        GLWrapper.bindVertexArray(id)
    }

    /**
     * Unbinds any VAO.
     */
    fun unbind() {
        GLWrapper.bindVertexArray(0)
    }

    /**
     * Configures a vertex attribute.
     *
     * @param index Attribute location (matches shader layout location)
     * @param size Number of components (1-4)
     * @param type Data type (GL_FLOAT, GL_UNSIGNED_BYTE, etc.)
     * @param normalized Whether to normalize integer values to [0,1] or [-1,1]
     * @param stride Byte offset between consecutive vertex attributes
     * @param offset Byte offset of first component in the buffer
     */
    fun setAttribute(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        offset: Long
    ) {
        GLWrapper.vertexAttribPointer(index, size, type, normalized, stride, offset)
        GLWrapper.enableVertexAttribArray(index)
        enabledAttributes.add(index)
    }

    /**
     * Configures an integer vertex attribute (for use with gl_VertexID-like semantics).
     *
     * @param index Attribute location
     * @param size Number of components (1-4)
     * @param type Data type (GL_INT, GL_UNSIGNED_INT, etc.)
     * @param stride Byte offset between consecutive attributes
     * @param offset Byte offset of first component
     */
    fun setIntegerAttribute(
        index: Int,
        size: Int,
        type: Int,
        stride: Int,
        offset: Long
    ) {
        GLWrapper.vertexAttribIPointer(index, size, type, stride, offset)
        GLWrapper.enableVertexAttribArray(index)
        enabledAttributes.add(index)
    }

    /**
     * Disables a vertex attribute.
     */
    fun disableAttribute(index: Int) {
        GLWrapper.disableVertexAttribArray(index)
        enabledAttributes.remove(index)
    }

    /**
     * Releases GPU resources.
     * Safe to call from any thread - will silently skip if no GL context is available.
     */
    fun destroy() {
        val hasContext = try {
            GL.getCapabilities() != null
        } catch (e: IllegalStateException) {
            false
        }
        if (hasContext) {
            GLWrapper.deleteVertexArrays(id)
        }
    }

    companion object {
        /**
         * Standard vertex attribute locations used in shaders.
         */
        object Attributes {
            const val POSITION = 0
            const val NORMAL = 1
            const val TEXCOORD = 2
            const val COLOR = 3
            const val TANGENT = 4
            const val BONE_IDS = 5
            const val BONE_WEIGHTS = 6
        }
    }
}

/**
 * Helper class for setting up common vertex formats.
 */
object VertexFormats {
    /**
     * Standard model vertex format matching RuneTek5 ModelGL layout.
     *
     * Layout (36 bytes per vertex):
     * - position: vec3 (12 bytes) at offset 0
     * - normal: vec3 (12 bytes) at offset 12
     * - texcoord: vec2 (8 bytes) at offset 24
     * - color: vec4 ubyte (4 bytes) at offset 32
     */
    fun setupModelFormat(vao: VertexArray, stride: Int = 36) {
        vao.setAttribute(VertexArray.Companion.Attributes.POSITION, 3, FLOAT, false, stride, 0)
        vao.setAttribute(VertexArray.Companion.Attributes.NORMAL, 3, FLOAT, false, stride, 12)
        vao.setAttribute(VertexArray.Companion.Attributes.TEXCOORD, 2, FLOAT, false, stride, 24)
        vao.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, UNSIGNED_BYTE, true, stride, 32)
    }

    /**
     * Simplified format without normals.
     *
     * Layout (24 bytes per vertex):
     * - position: vec3 (12 bytes) at offset 0
     * - texcoord: vec2 (8 bytes) at offset 12
     * - color: vec4 ubyte (4 bytes) at offset 20
     */
    fun setupSimpleFormat(vao: VertexArray, stride: Int = 24) {
        vao.setAttribute(VertexArray.Companion.Attributes.POSITION, 3, FLOAT, false, stride, 0)
        vao.setAttribute(VertexArray.Companion.Attributes.TEXCOORD, 2, FLOAT, false, stride, 12)
        vao.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, UNSIGNED_BYTE, true, stride, 20)
    }

    /**
     * Ground/terrain vertex format.
     *
     * Layout (28 bytes per vertex):
     * - position: vec3 (12 bytes) at offset 0
     * - normal: vec3 (12 bytes) at offset 12
     * - color: vec4 ubyte (4 bytes) at offset 24
     */
    fun setupGroundFormat(vao: VertexArray, stride: Int = 28) {
        vao.setAttribute(VertexArray.Companion.Attributes.POSITION, 3, FLOAT, false, stride, 0)
        vao.setAttribute(VertexArray.Companion.Attributes.NORMAL, 3, FLOAT, false, stride, 12)
        vao.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, UNSIGNED_BYTE, true, stride, 24)
    }

    /**
     * UI/2D sprite format.
     *
     * Layout (20 bytes per vertex):
     * - position: vec2 (8 bytes) at offset 0
     * - texcoord: vec2 (8 bytes) at offset 8
     * - color: vec4 ubyte (4 bytes) at offset 16
     */
    fun setupUIFormat(vao: VertexArray, stride: Int = 20) {
        vao.setAttribute(VertexArray.Companion.Attributes.POSITION, 2, FLOAT, false, stride, 0)
        vao.setAttribute(VertexArray.Companion.Attributes.TEXCOORD, 2, FLOAT, false, stride, 8)
        vao.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, UNSIGNED_BYTE, true, stride, 16)
    }
}
