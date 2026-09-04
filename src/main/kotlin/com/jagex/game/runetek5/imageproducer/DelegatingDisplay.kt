package com.jagex.game.runetek5.imageproducer

/**
 *
 * @author Brainpower - 22/04/2022
 *
 */
abstract class DelegatingDisplay : AbstractDisplay() {
    abstract fun draw(): Int

    abstract fun draw(x: Int, y: Int): Int

    abstract fun resize(width: Int, height: Int)
}
