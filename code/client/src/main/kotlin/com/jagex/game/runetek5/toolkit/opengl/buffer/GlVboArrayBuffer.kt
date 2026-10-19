package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.buffer.ArrayBufferGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.*
import jaclib.memory.Buffer

/**
 * OpenGL Vertex Buffer Object (VBO) array buffer implementation.
 *
 * @author Steven - 13/09/2023
 */
internal class GlVboArrayBuffer : AbstractArbBufferGL, ArrayBufferGL {

    private var _stride: Int

    constructor(
        toolkit: OpenGlToolkit,
        stride: Int,
        data: ByteArray,
        bufferSize: Int,
        streamDraw: Boolean
    ) : super(toolkit, GL_ARRAY_BUFFER, data, bufferSize, streamDraw) {
        this._stride = stride
    }

    constructor(
        toolkit: OpenGlToolkit,
        stride: Int,
        buffer: Buffer,
        bufferSize: Int
    ) : super(toolkit, buffer, bufferSize) {
        this._stride = stride
    }

    override fun putArray(stride: Int, data: ByteArray, size: Int) {
        put(data, size)
        this._stride = stride
    }

    override fun getStride(): Int = _stride

    override fun getBufferAddress(): Long = 0L

    override fun setAsActiveBuffer() {
        toolkit.setArrayBuffer(this)
    }

    override fun getId(): Int = id
}
