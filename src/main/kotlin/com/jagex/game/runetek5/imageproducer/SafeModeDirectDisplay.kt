package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.framebuffer.SafeModeFrameBuffer
import com.jagex.game.runetek5.framebuffer.SafeModeRenderBuffer
import com.jagex.game.runetek5.toolkit.SafeModeToolkit

class SafeModeDirectDisplay(
    @JvmField val safemodeToolkit: SafeModeToolkit
) : DirectDisplay() {

    @JvmField var frameBuffer: SafeModeFrameBuffer? = null
    @JvmField var width: Int = 0
    @JvmField var height: Int = 0
    @JvmField var safemodeRenderBuffer: SafeModeRenderBuffer? = null

    override fun getWidth(): Int {
        return width
    }

    override fun setFramebuffer(slot: Int, handle: FrameBuffer?) {
        if (slot != 0) {
            throw RuntimeException()
        }
        val newFrameBuffer = handle as SafeModeFrameBuffer?
        if (safemodeRenderBuffer != null && newFrameBuffer != null
            && (safemodeRenderBuffer!!.width != newFrameBuffer.width
                    || safemodeRenderBuffer!!.height != newFrameBuffer.height)
        ) {
            throw RuntimeException()
        }
        this.frameBuffer = newFrameBuffer
        if (newFrameBuffer != null) {
            width = newFrameBuffer.width
            height = newFrameBuffer.height
        } else if (safemodeRenderBuffer == null) {
            width = 0
            height = 0
        }

        if (this == safemodeToolkit.getDisplay()) {
            reload()
        }
    }

    override fun setRenderBuffer(renderBuffer: RenderBuffer?) {
        val newRenderBuffer = renderBuffer as SafeModeRenderBuffer?
        if (frameBuffer != null && newRenderBuffer != null &&
            (frameBuffer!!.width != newRenderBuffer.width || frameBuffer!!.height != newRenderBuffer.height)
        ) {
            throw RuntimeException()
        }
        this.safemodeRenderBuffer = newRenderBuffer
        if (newRenderBuffer != null) {
            width = newRenderBuffer.width
            height = newRenderBuffer.height
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
