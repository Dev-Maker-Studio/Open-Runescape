package com.jagex.game.runetek5.buffer

import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.GL_FRAMEBUFFER
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.GL_RENDERBUFFER
import jaggl.OpenGL

/**
 * OpenGL render buffer implementation.
 */
class OpenGlRenderBuffer : CacheableNode, NativeRenderBuffer {

    @JvmField var handle: Int
    @JvmField val activeToolkit: OpenGlToolkit
    @JvmField val width: Int
    @JvmField val height: Int
    @JvmField val format: GLFormat
    @JvmField val actionformat: GLActionFormat
    @JvmField val anInt10284: Int

    constructor(
        openglToolkit: OpenGlToolkit,
        glformat: GLFormat,
        glactionformat: GLActionFormat,
        width: Int,
        height: Int,
        sample: Int
    ) {
        this.activeToolkit = openglToolkit
        this.width = width
        this.height = height
        this.format = glformat
        this.actionformat = glactionformat
        OpenGL.glGenRenderbuffersEXT(1, BUFFERS, 0)
        this.handle = BUFFERS[0]
        OpenGL.glBindRenderbufferEXT(GL_RENDERBUFFER, this.handle)
        OpenGL.glRenderbufferStorageMultisampleEXT(
            GL_RENDERBUFFER, sample,
            OpenGlToolkit.getInternalTextureFormat(this.format, this.actionformat),
            this.width, this.height
        )
        this.anInt10284 = this.format.bytesPerPixel * this.actionformat.bytesPerEntry * this.width * this.height
    }

    constructor(
        openglToolkit: OpenGlToolkit,
        glformat: GLFormat,
        glactionformat: GLActionFormat,
        width: Int,
        height: Int
    ) {
        this.activeToolkit = openglToolkit
        this.width = width
        this.height = height
        this.format = glformat
        this.actionformat = glactionformat
        OpenGL.glGenRenderbuffersEXT(1, BUFFERS, 0)
        this.handle = BUFFERS[0]
        OpenGL.glBindRenderbufferEXT(GL_RENDERBUFFER, this.handle)
        OpenGL.glRenderbufferStorageEXT(
            GL_RENDERBUFFER,
            OpenGlToolkit.getInternalTextureFormat(this.format, this.actionformat),
            this.width, this.height
        )
        this.anInt10284 = this.format.bytesPerPixel * this.actionformat.bytesPerEntry * this.width * this.height
    }

    override fun getUseHeight(): Int {
        return height
    }

    override fun getUseWidth(): Int {
        return width
    }

    override fun deallocate() {
        if (handle > 0) {
            val activeHandle = intArrayOf(handle)
            OpenGL.glDeleteRenderbuffersEXT(1, activeHandle, 0)
            handle = 0
        }
    }

    fun dispose() {
        if (handle > 0) {
            activeToolkit.requestRenderBufferCleanup(handle, anInt10284)
            handle = 0
        }
    }

    @Suppress("removal", "DEPRECATION")
    @Deprecated("Deprecated in Java")
    protected fun finalize() {
        dispose()
    }

    override fun getFrameRenderBuffer(attachment: Int) {
        OpenGL.glFramebufferRenderbufferEXT(GL_FRAMEBUFFER, attachment, GL_RENDERBUFFER, handle)
    }

    companion object {
        @JvmField val BUFFERS = IntArray(1)
    }
}
