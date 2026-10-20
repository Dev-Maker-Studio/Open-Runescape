package com.jagex.game.runetek5.framebuffer

class SafeModeRenderBuffer(
    @JvmField val width: Int,
    @JvmField val height: Int
) : RenderBuffer {

    @JvmField
    val depthBuffer: FloatArray = FloatArray(width * height)

    override fun getUseWidth(): Int {
        return width
    }

    override fun getUseHeight(): Int {
        return height
    }

    override fun deallocate() {
    }
}
