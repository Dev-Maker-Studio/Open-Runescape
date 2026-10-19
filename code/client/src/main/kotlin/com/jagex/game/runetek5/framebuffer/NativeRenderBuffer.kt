package com.jagex.game.runetek5.framebuffer

interface NativeRenderBuffer : FrameBuffer, RenderBuffer {
    fun getFrameRenderBuffer(colorAttachmentSlot: Int)
}
