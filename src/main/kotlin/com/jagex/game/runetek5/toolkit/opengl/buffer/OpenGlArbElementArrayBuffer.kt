package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*

/**
 * OpenGL ARB (Architecture Review Board) extension element array buffer.
 * Used for index buffers in OpenGL rendering.
 *
 * @author Steven - 13/09/2023
 */
class OpenGlArbElementArrayBuffer(
    toolkit: OpenGLNativeToolkit,
    @JvmField val actionFormat: GLActionFormat,
    drawStream: Boolean
) : OpenGlArbBuffer(toolkit, GL_ELEMENT_ARRAY_BUFFER, drawStream), com.jagex.game.runetek5.toolkit.NativeToolkitBuffer {

    override fun getCapacity(): Int = super.getCapacity()

    override fun unlock() {
        super.unlock()
    }

    fun getActionFormat(): GLActionFormat = actionFormat

    override fun create(size: Int) {
        super.create(actionFormat.bytesPerEntry * size)
    }

    override fun deallocate() {
        super.destroy()
    }

    override fun getDataStorePointer(offset: Int, i_2: Int): Long {
        return super.getDataStorePointer(offset, i_2)
    }

    override fun upload(offset: Int, size: Int, dataPointer: Long): Boolean {
        return super.upload(offset, size, dataPointer)
    }
}
