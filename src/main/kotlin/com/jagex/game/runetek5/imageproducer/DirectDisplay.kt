package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer

abstract class DirectDisplay : AbstractDisplay() {
    abstract fun setFramebuffer(slot: Int, handle: FrameBuffer?)

    abstract fun isFramebufferComplete(): Boolean

    abstract fun setRenderBuffer(renderBuffer: RenderBuffer?)
}
