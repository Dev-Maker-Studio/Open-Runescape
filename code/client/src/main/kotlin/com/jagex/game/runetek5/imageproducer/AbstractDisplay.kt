package com.jagex.game.runetek5.imageproducer

abstract class AbstractDisplay {
    abstract fun getWidth(): Int

    abstract fun getHeight(): Int

    abstract fun cleanup()

    abstract fun setDepthStencilSurface(): Boolean

    abstract fun reload(): Boolean
}
