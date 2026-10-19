package com.jagex.game.runetek5.client

import java.awt.Canvas
import java.awt.Component
import java.awt.Graphics

class GameCanvas(
    @JvmField val compont: Component
) : Canvas() {
    override fun update(graphics: Graphics) {
        compont.update(graphics)
    }

    override fun paint(graphics: Graphics) {
        compont.paint(graphics)
    }
}
