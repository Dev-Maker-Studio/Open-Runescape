package com.jagex.game.runetek5.toolkit.directx

import com.jagex.game.runetek5.framebuffer.RenderBuffer

interface DirectXFrameBufferObject : RenderBuffer {
    fun destroy()
    fun getSurfaceLevel(): Long
    override fun deallocate()
}
