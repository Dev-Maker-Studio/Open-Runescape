package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.framebuffer.FrameBuffer

interface RectangleTextureSampler : NativeTextureSampler {
    fun getFramebuffer(): FrameBuffer
}
