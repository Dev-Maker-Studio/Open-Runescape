package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.framebuffer.SafeModeFrameBuffer
import com.jagex.game.runetek5.framebuffer.SafeModeRenderBuffer
import com.jagex.game.runetek5.toolkit.SafeModeToolkit

class ImageProducerSM1(
    @JvmField val safemodeToolkit: SafeModeToolkit
) : DirectDisplay() {

    @JvmField var frameBuffer: SafeModeFrameBuffer? = null
    @JvmField var width: Int = 0
    @JvmField var height: Int = 0
    @JvmField var safemodeRenderBuffer: SafeModeRenderBuffer? = null

    override fun getWidth(): Int {
        return width
    }

    override fun setFramebuffer(i_1: Int, handle: FrameBuffer?) {
        if (i_1 != 0) {
            throw RuntimeException()
        }
        val frameBuffer = handle as SafeModeFrameBuffer?
        if (safemodeRenderBuffer != null && frameBuffer != null
            && (safemodeRenderBuffer!!.width != frameBuffer.width
                    || safemodeRenderBuffer!!.height != frameBuffer.height)
        ) {
            throw RuntimeException()
        }
        this.frameBuffer = frameBuffer
        if (frameBuffer != null) {
            width = frameBuffer.width
            height = frameBuffer.height
        } else if (safemodeRenderBuffer == null) {
            width = 0
            height = 0
        }

        if (this == safemodeToolkit.getDisplay()) {
            reload()
        }
    }

    override fun setRenderBuffer(renderBuffer: RenderBuffer?) {
        val safemodeRenderBuffer = renderBuffer as SafeModeRenderBuffer?
        if (frameBuffer != null && safemodeRenderBuffer != null &&
            (frameBuffer!!.width != safemodeRenderBuffer.width || frameBuffer!!.height != safemodeRenderBuffer.height)
        ) {
            throw RuntimeException()
        }
        this.safemodeRenderBuffer = safemodeRenderBuffer
        if (safemodeRenderBuffer != null) {
            width = safemodeRenderBuffer.width
            height = safemodeRenderBuffer.height
        } else if (frameBuffer == null) {
            width = 0
            height = 0
        }

        if (this == safemodeToolkit.getDisplay()) {
            reload()
        }
    }

    override fun isFramebufferComplete(): Boolean {
        return true
    }

    override fun getHeight(): Int {
        return height
    }

    override fun setDepthStencilSurface(): Boolean {
        return true
    }

    override fun reload(): Boolean {
        safemodeToolkit.bind(
            width, height, if (frameBuffer == null) null else frameBuffer!!.pixels,
            if (safemodeRenderBuffer == null) null else safemodeRenderBuffer!!.depthBuffer
        )
        return true
    }

    override fun cleanup() {
    }
}
