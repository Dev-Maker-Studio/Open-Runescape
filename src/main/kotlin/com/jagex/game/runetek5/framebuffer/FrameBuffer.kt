package com.jagex.game.runetek5.framebuffer

interface FrameBuffer : BoundSurface {
    override fun deallocate()
}
