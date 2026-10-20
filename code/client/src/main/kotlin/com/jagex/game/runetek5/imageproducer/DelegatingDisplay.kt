package com.jagex.game.runetek5.imageproducer

abstract class DelegatingDisplay : AbstractDisplay() {
    abstract fun draw(): Int

    abstract fun draw(x: Int, y: Int): Int

    abstract fun resize(width: Int, height: Int)
}
