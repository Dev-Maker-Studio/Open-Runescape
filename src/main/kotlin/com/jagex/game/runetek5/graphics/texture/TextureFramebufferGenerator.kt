package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer

interface TextureFramebufferGenerator : FrameBuffer, RenderBuffer {
    fun generateTextureFramebuffer(attachmentPoint: Int)
}
