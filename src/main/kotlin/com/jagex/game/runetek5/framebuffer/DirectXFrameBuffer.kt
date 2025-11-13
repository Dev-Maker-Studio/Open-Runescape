package com.jagex.game.runetek5.framebuffer

interface DirectXFrameBuffer : FrameBuffer {
    fun destroy()
    fun getSurfaceLevel(): Long
    override fun deallocate()
}
