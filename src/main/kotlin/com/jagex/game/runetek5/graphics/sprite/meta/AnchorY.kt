package com.jagex.game.runetek5.graphics.sprite.meta

import com.jagex.game.runetek5.client.GameShell
import kotlin.math.max

enum class AnchorY {
    TOP,
    CENTER,
    BOTTOM;

    fun calculateDrawY(scaleHeight: Int, gameHeight: Int): Int {
        val maxHeight = max(GameShell.screenCanvasHeight * -969250379, gameHeight)
        return when (this) {
            TOP -> 0
            BOTTOM -> maxHeight - scaleHeight
            CENTER -> (maxHeight - scaleHeight) / 2
        }
    }
}
