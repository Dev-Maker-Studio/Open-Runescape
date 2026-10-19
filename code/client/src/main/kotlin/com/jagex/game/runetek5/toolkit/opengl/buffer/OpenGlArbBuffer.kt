package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.buffer.NativeBuffer
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GL_STATIC_DRAW
import com.jagex.game.runetek5.toolkit.opengl.GL_STREAM_DRAW
import com.jagex.game.runetek5.toolkit.opengl.GL_WRITE_ONLY
import jaclib.memory.Source
import jaclib.memory.heap.NativeHeapBuffer
import jaggl.OpenGL

abstract class OpenGlArbBuffer protected constructor(
    @JvmField val nativeToolkit: OpenGLNativeToolkit,
    @JvmField val bufferTarget: Int,
    @JvmField val drawStream: Boolean
) : NativeBuffer {

    @JvmField var activeHandle = -1
    @JvmField var nativeheapBuffer: NativeHeapBuffer? = null
    @JvmField var capacity = 0
    @JvmField var initialCapacity = 0

    override fun getCapacity(): Int {
        return capacity
    }

    fun bind() {
        if (nativeToolkit.GLARBVertexBufferObject) {
            OpenGL.glBindBufferARB(bufferTarget, activeHandle)
        }
    }

    fun initialize() {
        if (activeHandle < 0) {
            if (nativeToolkit.GLARBVertexBufferObject) {
                OpenGL.glGenBuffersARB(1, HANDLE_BUFFER, 0)
                activeHandle = HANDLE_BUFFER[0]
                OpenGL.glBindBufferARB(bufferTarget, activeHandle)
            } else {
                activeHandle = 0
            }
        }
    }

    override fun getDataStorePointer(offset: Int, unused: Int): Long {
        OpenGL.glBindBufferARB(bufferTarget, activeHandle)
        return OpenGL.glMapBufferARB(bufferTarget, GL_WRITE_ONLY) + offset
    }

    fun getBufferAddress(): Long {
        return if (activeHandle == 0) nativeheapBuffer!!.getBufferAddress() else 0L
    }

    override fun upload(offset: Int, size: Int, dataPointer: Long): Boolean {
        initialize()
        if (activeHandle > 0) {
            OpenGL.glBindBufferARB(bufferTarget, activeHandle)
            OpenGL.glBufferSubDataARBa(bufferTarget, offset, size, dataPointer)
        } else {
            nativeheapBuffer!!.nativeHeap.copy(nativeheapBuffer!!.getBufferAddress() + offset, dataPointer, size)
        }

        return true
    }

    protected fun destroy() {
        if (activeHandle > 0) {
            nativeToolkit.requestTextureBufferDeletion(activeHandle, capacity)
            activeHandle = -1
        }
    }

    override fun unlock() {
        OpenGL.glUnmapBufferARB(bufferTarget)
    }

    protected fun createFromSource(size: Int, source: Source): Boolean {
        if (size > initialCapacity) {
            initialize()
            if (activeHandle > 0) {
                OpenGL.glBindBufferARB(bufferTarget, activeHandle)
                OpenGL.glBufferDataARBa(bufferTarget, size, source.getBufferAddress(), if (drawStream) GL_STREAM_DRAW else GL_STATIC_DRAW)
                nativeToolkit.textureBuffersSizeOccupied += size - capacity
            } else {
                nativeheapBuffer = nativeToolkit.generateNativeHeapBuffer(size, false)
                nativeheapBuffer!!.copy(source)
            }

            initialCapacity = size
        } else if (activeHandle > 0) {
            OpenGL.glBindBufferARB(bufferTarget, activeHandle)
            OpenGL.glBufferSubDataARBa(bufferTarget, 0, capacity, source.getBufferAddress())
            nativeToolkit.textureBuffersSizeOccupied += size - capacity
        } else {
            nativeheapBuffer = nativeToolkit.generateNativeHeapBuffer(size, false)
            nativeheapBuffer!!.copy(source)
        }

        capacity = size
        return true
    }

    protected open fun create(size: Int) {
        if (size > initialCapacity) {
            initialize()
            if (activeHandle > 0) {
                OpenGL.glBindBufferARB(bufferTarget, activeHandle)
                OpenGL.glBufferDataARBub(bufferTarget, size, null, 0, if (drawStream) GL_STREAM_DRAW else GL_STATIC_DRAW)
                nativeToolkit.textureBuffersSizeOccupied += size - initialCapacity
            } else {
                nativeheapBuffer = nativeToolkit.generateNativeHeapBuffer(size, false)
            }

            initialCapacity = size
        }

        capacity = size
    }

    companion object {
        @JvmField val HANDLE_BUFFER = IntArray(1)
    }
}
