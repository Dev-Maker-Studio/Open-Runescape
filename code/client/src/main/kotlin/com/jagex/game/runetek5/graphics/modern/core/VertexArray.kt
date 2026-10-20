package com.jagex.game.runetek5.graphics.modern.core

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FLOAT
import org.lwjgl.opengl.GL
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_BYTE

class VertexArray {
    val id: Int = GLWrapper.genVertexArrays()
    private val enabledAttributes = mutableSetOf<Int>()

    fun bind() {
        GLWrapper.bindVertexArray(id)
    }

    fun unbind() {
        GLWrapper.bindVertexArray(0)
    }

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

    fun disableAttribute(index: Int) {
        GLWrapper.disableVertexAttribArray(index)
        enabledAttributes.remove(index)
    }

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

object VertexFormats {
    fun setupModelFormat(vao: VertexArray, stride: Int = 36) {
        vao.setAttribute(VertexArray.Companion.Attributes.POSITION, 3, FLOAT, false, stride, 0)
        vao.setAttribute(VertexArray.Companion.Attributes.NORMAL, 3, FLOAT, false, stride, 12)
        vao.setAttribute(VertexArray.Companion.Attributes.TEXCOORD, 2, FLOAT, false, stride, 24)
        vao.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, UNSIGNED_BYTE, true, stride, 32)
    }

    fun setupSimpleFormat(vao: VertexArray, stride: Int = 24) {
        vao.setAttribute(VertexArray.Companion.Attributes.POSITION, 3, FLOAT, false, stride, 0)
        vao.setAttribute(VertexArray.Companion.Attributes.TEXCOORD, 2, FLOAT, false, stride, 12)
        vao.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, UNSIGNED_BYTE, true, stride, 20)
    }

    fun setupGroundFormat(vao: VertexArray, stride: Int = 28) {
        vao.setAttribute(VertexArray.Companion.Attributes.POSITION, 3, FLOAT, false, stride, 0)
        vao.setAttribute(VertexArray.Companion.Attributes.NORMAL, 3, FLOAT, false, stride, 12)
        vao.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, UNSIGNED_BYTE, true, stride, 24)
    }

    fun setupUIFormat(vao: VertexArray, stride: Int = 20) {
        vao.setAttribute(VertexArray.Companion.Attributes.POSITION, 2, FLOAT, false, stride, 0)
        vao.setAttribute(VertexArray.Companion.Attributes.TEXCOORD, 2, FLOAT, false, stride, 8)
        vao.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, UNSIGNED_BYTE, true, stride, 16)
    }
}
