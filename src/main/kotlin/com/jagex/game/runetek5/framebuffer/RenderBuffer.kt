package com.jagex.game.runetek5.framebuffer

interface RenderBuffer : BoundSurface {
    override fun deallocate()
}
