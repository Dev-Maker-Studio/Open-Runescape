package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.GL_ARRAY_BUFFER
import jaclib.memory.Source

/**
 * OpenGL ARB array buffer implementation for vertex data storage.
 *
 * @author Steven - 13/09/2023
 */
class OpenGlArbArrayBuffer(
    nativeToolkit: OpenGLNativeToolkit,
    drawStream: Boolean
) : OpenGlArbBuffer(nativeToolkit, GL_ARRAY_BUFFER, drawStream), NativeStream {

    @JvmField var stride: Byte = 0

    override fun getCapacity(): Int {
        return super.getCapacity()
    }

    override fun createFromSource(size: Int, stride: Int, source: Source): Boolean {
        this.stride = stride.toByte()
        createFromSource(size, source)
        return true
    }

    override fun create(size: Int, stride: Int): Boolean {
        this.stride = stride.toByte()
        create(size)
        return true
    }

    override fun unlock() {
        super.unlock()
    }

    fun getStride(): Int {
        return stride.toInt()
    }

    override fun getDataStorePointer(offset: Int, unused: Int): Long {
        return super.getDataStorePointer(offset, unused)
    }

    override fun deallocate() {
        super.destroy()
    }

    override fun upload(offset: Int, size: Int, dataPointer: Long): Boolean {
        return super.upload(offset, size, dataPointer)
    }
}
