package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.NativeToolkitBuffer
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.*
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
) : OpenGlArbBuffer(toolkit, GL_ELEMENT_ARRAY_BUFFER, drawStream), NativeToolkitBuffer {

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

    override fun getDataStorePointer(offset: Int, unused: Int): Long {
        return super.getDataStorePointer(offset, unused)
    }

    override fun upload(offset: Int, size: Int, dataPointer: Long): Boolean {
        return super.upload(offset, size, dataPointer)
    }
}
