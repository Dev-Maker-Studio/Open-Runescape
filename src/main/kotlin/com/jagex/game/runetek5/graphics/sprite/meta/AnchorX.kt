package com.jagex.game.runetek5.graphics.sprite.meta

import com.jagex.game.runetek5.client.GameShell
import kotlin.math.max

enum class AnchorX {
    LEFT,
    CENTER,
    RIGHT;

    fun calculateDrawX(itemWidth: Int, gameWidth: Int): Int {
        val maxWidth = max(GameShell.screenCanvasWidth, gameWidth)
        return when (this) {
            LEFT -> 0
            RIGHT -> maxWidth - itemWidth
            CENTER -> (maxWidth - itemWidth) / 2
        }
    }
}
