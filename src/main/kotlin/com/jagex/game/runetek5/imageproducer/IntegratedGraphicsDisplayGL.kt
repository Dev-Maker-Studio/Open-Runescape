package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import jaggl.OpenGL

/**
 * @author Brainpower - 22/04/2022
 */
class IntegratedGraphicsDisplayGL(
    @JvmField val toolkit: OpenGlToolkit
) : DirectDisplay(), AutoCloseable {

    @JvmField var handle: Int
    @JvmField var occupiedSlots: Int = 0
    @JvmField var renderBufferFlags: Int = 0
    @JvmField var height: Int = 0
    @JvmField var width: Int = 0
    @JvmField val colorBuffers: Array<NativeRenderBuffer?> = arrayOfNulls(4)
    @JvmField var depthBuffer: NativeRenderBuffer? = null

    init {
        if (!toolkit.intelGpu) {
            throw IllegalStateException("")
        }
        val buffer = IntArray(1)
        OpenGL.glGenFramebuffersEXT(1, buffer, 0)
        handle = buffer[0]
    }

    override fun reload(): Boolean {
        OpenGL.glBindFramebufferEXT(GL_FRAMEBUFFER, handle)
        for (slot in 0 until 4) {
            if ((occupiedSlots and (1 shl slot)) != 0) {
                createColorBuffer(slot)
            }
        }
        if ((occupiedSlots and 0x10) != 0) {
            createDepthBuffer()
        }
        occupiedSlots = 0
        toolkit.method13578()
        return true
    }

    override fun setFramebuffer(slot: Int, nativeHandle: FrameBuffer?) {
        val bitFlag = 1 shl slot
        val renderBuffer = nativeHandle as NativeRenderBuffer?
        if (nativeHandle == null) {
            renderBufferFlags = renderBufferFlags and bitFlag.inv()
            colorBuffers[slot] = null
            if (renderBufferFlags == 0) {
                height = 0
                width = 0
            }
        } else {
            if (bitFlag != (renderBufferFlags or bitFlag)) {
                if (width != renderBuffer!!.getUseWidth() || height != renderBuffer.getUseHeight()) {
                    throw RuntimeException()
                }
            } else {
                height = renderBuffer!!.getUseHeight()
                width = renderBuffer.getUseWidth()
                if (this === toolkit.getDisplay()) {
                    toolkit.method13639()
                }
            }

            renderBufferFlags = renderBufferFlags or bitFlag
            colorBuffers[slot] = renderBuffer
        }

        if (this === toolkit.getDisplay()) {
            createColorBuffer(slot)
        } else {
            occupiedSlots = occupiedSlots or bitFlag
        }
    }

    fun createDepthBuffer() {
        if (depthBuffer == null) {
            OpenGL.glFramebufferRenderbufferEXT(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0)
        } else {
            depthBuffer!!.getFrameRenderBuffer(GL_DEPTH_ATTACHMENT)
        }
    }

    fun createColorBuffer(colorAttachmentSlot: Int) {
        val colorBuffer = colorBuffers[colorAttachmentSlot]
        if (colorBuffer == null) {
            OpenGL.glFramebufferRenderbufferEXT(GL_FRAMEBUFFER, colorAttachmentSlot + GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0)
        } else {
            colorBuffer.getFrameRenderBuffer(colorAttachmentSlot + GL_COLOR_ATTACHMENT0)
        }
    }

    override fun isFramebufferComplete(): Boolean {
        val status = OpenGL.glCheckFramebufferStatusEXT(GL_FRAMEBUFFER)
        return status == GL_FRAMEBUFFER_COMPLETE
    }

    fun bindFrameBuffer(i_3: Int, i_4: Int, bool8: Boolean) {
        val height = height
        val displayHeight = toolkit.getDisplay().getHeight()
        var flags = 0
        if (bool8) {
            flags = flags or 0x100
        }

        flags = flags or 0x4000

        OpenGL.glBindFramebufferEXT(GL_READ_FRAMEBUFFER, handle)
        OpenGL.glBlitFramebufferEXT(0, height - i_4, i_3, height, 0, displayHeight - i_4, i_3, displayHeight, flags, GL_NEAREST)
        OpenGL.glBindFramebufferEXT(GL_READ_FRAMEBUFFER, 0)
    }

    override fun setDepthStencilSurface(): Boolean {
        OpenGL.glBindFramebufferEXT(GL_FRAMEBUFFER, 0)
        return true
    }

    override fun cleanup() {
        if (handle != 0) {
            toolkit.requestFrameBufferCleanup(handle)
            handle = 0
        }
    }

    override fun getHeight(): Int {
        return height
    }

    override fun setRenderBuffer(renderBuffer: RenderBuffer?) {
        val nativeRenderBuffer = renderBuffer as NativeRenderBuffer?
        if (renderBuffer == null) {
            renderBufferFlags = renderBufferFlags and -17
            depthBuffer = null
            if (renderBufferFlags == 0) {
                height = 0
                width = 0
            }
        } else {
            if ((renderBufferFlags or 0x10) != 16) {
                if (width != nativeRenderBuffer!!.getUseWidth() || height != nativeRenderBuffer.getUseHeight()) {
                    throw RuntimeException()
                }
            } else {
                height = nativeRenderBuffer!!.getUseHeight()
                width = nativeRenderBuffer.getUseWidth()
                if (this === toolkit.getDisplay()) {
                    toolkit.method13639()
                }
            }

            renderBufferFlags = renderBufferFlags or 0x10
            depthBuffer = nativeRenderBuffer
        }

        if (this === toolkit.getDisplay()) {
            createDepthBuffer()
        } else {
            occupiedSlots = occupiedSlots or 0x10
        }
    }

    override fun getWidth(): Int {
        return width
    }

    fun drawBuffer(colorAttachmentSlot: Int) {
        OpenGL.glDrawBuffer(colorAttachmentSlot + GL_COLOR_ATTACHMENT0)
    }

    override fun close() {
        cleanup()
    }
}
