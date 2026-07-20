package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GL_ELEMENT_ARRAY_BUFFER
import com.jagex.game.runetek5.toolkit.opengl.GL_STREAM_DRAW
import jaggl.OpenGL

/**
 * VBO-backed element (index) buffer for index lists that are rebuilt whenever the camera
 * moves (ground rendering). Unlike [GlElementsArrayBuffer] (client-memory indices drawn
 * straight from a native heap pointer), the indices live in a GL element array buffer so
 * glDrawElements sources them from GPU-visible memory.
 *
 * - The buffer object is created lazily on first [putElements] so instances can be
 *   constructed off the GL thread (map building); uploads/draws happen on the GL thread.
 * - Each upload re-specifies the buffer store with GL_STREAM_DRAW (driver-side orphan),
 *   avoiding stalls on indices still referenced by in-flight draws.
 * - [getBufferAddress] returns 0 so the toolkit's drawElements() treats the pointer as an
 *   offset into the bound element buffer (see OpenGlToolkit.setElementsBuffer, which binds
 *   [getId] when VBOs are supported).
 *
 * Only use when the toolkit reports vertexbufferSupported; otherwise fall back to
 * [GlElementsArrayBuffer].
 */
class GlStreamElementsArrayBuffer(
    @JvmField val toolkit: OpenGlToolkit
) : ElementsBufferGL {

    @JvmField var id: Int = 0
    @JvmField var bufferSize: Int = 0

    override fun getId(): Int {
        return id
    }

    override fun getBufferAddress(): Long {
        return 0L
    }

    override fun putElements(data: ByteArray, size: Int) {
        if (id == 0) {
            OpenGL.glGenBuffersARB(1, HANDLE, 0)
            id = HANDLE[0]
        }
        toolkit.setElementsBuffer(this)
        OpenGL.glBufferDataARBub(GL_ELEMENT_ARRAY_BUFFER, size, data, 0, GL_STREAM_DRAW)
        toolkit.vboBuffersSize += size - bufferSize
        bufferSize = size
    }

    @Suppress("removal", "DEPRECATION")
    @Deprecated("Deprecated in Java")
    protected fun finalize() {
        if (id != 0) {
            // Queued for glDeleteBuffersARB on the GL thread (see OpenGlToolkit.animateTextures).
            toolkit.requestArbBufferCleanup(id, bufferSize)
            id = 0
        }
    }

    companion object {
        private val HANDLE = IntArray(1)
    }
}
