package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.*
import jaclib.memory.Buffer
import jaggl.OpenGL

/**
 * Abstract base class for OpenGL ARB buffer objects.
 */
internal abstract class AbstractArbBufferGL : AutoCloseable {

    @JvmField var deallocated: Boolean = false
    @JvmField val toolkit: OpenGlToolkit
    @JvmField val bufferTarget: Int
    @JvmField var bufferSize: Int
    @JvmField val streamDraw: Boolean
    @JvmField val id: Int

    constructor(toolkit: OpenGlToolkit, buffer: Buffer, bufferSize: Int) {
        this.toolkit = toolkit
        this.bufferTarget = GL_ARRAY_BUFFER
        this.bufferSize = bufferSize
        this.streamDraw = false
        OpenGL.glGenBuffersARB(1, HANDLE, 0)
        this.id = HANDLE[0]
        setAsActiveBuffer()
        OpenGL.glBufferDataARBa(GL_ARRAY_BUFFER, bufferSize, buffer.getBufferAddress(), GL_STATIC_DRAW)
        this.toolkit.vboBuffersSize += this.bufferSize
    }

    constructor(toolkit: OpenGlToolkit, bufferTarget: Int, data: ByteArray, bufferSize: Int, streamDraw: Boolean) {
        this.toolkit = toolkit
        this.bufferTarget = bufferTarget
        this.bufferSize = bufferSize
        this.streamDraw = streamDraw
        OpenGL.glGenBuffersARB(1, HANDLE, 0)
        this.id = HANDLE[0]
        setAsActiveBuffer()
        OpenGL.glBufferDataARBub(
            bufferTarget,
            this.bufferSize,
            data,
            0,
            if (this.streamDraw) GL_STREAM_DRAW else GL_STATIC_DRAW
        )
        this.toolkit.vboBuffersSize += this.bufferSize
    }

    override fun close() {
        if (!deallocated) {
            toolkit.requestArbBufferCleanup(id, bufferSize)
            deallocated = true
        }
    }

    /**
     * GL buffer handles are released through the toolkit's deallocation queue, which is
     * drained on the GL thread. Nothing in the render path calls [close] for buffers owned
     * by models that drop out of the model caches, so reclaim the handle when the wrapper
     * is collected. [OpenGlToolkit.requestArbBufferCleanup] is synchronized and only
     * enqueues, making it safe to call from the finalizer thread.
     */
    protected fun finalize() {
        close()
    }

    abstract fun setAsActiveBuffer()

    open fun put(data: ByteArray, size: Int) {
        setAsActiveBuffer()
        if (size > bufferSize) {
            OpenGL.glBufferDataARBub(
                bufferTarget,
                size,
                data,
                0,
                if (streamDraw) GL_STREAM_DRAW else GL_STATIC_DRAW
            )
            toolkit.vboBuffersSize += size - bufferSize
            bufferSize = size
        } else {
            if (streamDraw) {
                // Orphan the buffer store before overwriting it. Dynamic (STREAM_DRAW)
                // buffers are shared toolkit-wide (the seven model copy slots and other
                // per-frame geometry), so the GPU may still be reading the previous
                // contents when the next entity rewrites them. Re-specifying the store
                // with a NULL data pointer lets the driver hand back fresh memory
                // instead of stalling on an implicit synchronization.
                OpenGL.glBufferDataARBa(bufferTarget, bufferSize, 0L, GL_STREAM_DRAW)
            }
            OpenGL.glBufferSubDataARBub(bufferTarget, 0, size, data, 0)
        }
    }

    companion object {
        @JvmField val HANDLE = IntArray(1)
    }
}
