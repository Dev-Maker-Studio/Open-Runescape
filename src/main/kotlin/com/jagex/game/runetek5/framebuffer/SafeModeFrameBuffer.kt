package com.jagex.game.runetek5.framebuffer

class SafeModeFrameBuffer(
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val pixels: IntArray
) : FrameBuffer {

    override fun getUseWidth(): Int {
        return width
    }

    override fun getUseHeight(): Int {
        return height
    }

    override fun deallocate() {
        // NOOP
    }
}
