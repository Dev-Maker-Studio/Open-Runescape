package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.GL_ELEMENT_ARRAY_BUFFER

/**
 *
 * @author Steven - 13/09/2023
 *
 */
internal class GlVboElementsArrayBuffer(
    toolkit: OpenGlToolkit,
    data: ByteArray,
    bufferSize: Int,
    streamDraw: Boolean
) : AbstractArbBufferGL(toolkit, GL_ELEMENT_ARRAY_BUFFER, data, bufferSize, streamDraw), ElementsBufferGL {

    override fun getId(): Int {
        return super.id
    }

    override fun getBufferAddress(): Long {
        return 0L
    }

    override fun putElements(data: ByteArray, size: Int) {
        put(data, size)
    }

    override fun setAsActiveBuffer() {
        super.toolkit.setElementsBuffer(this)
    }
}
