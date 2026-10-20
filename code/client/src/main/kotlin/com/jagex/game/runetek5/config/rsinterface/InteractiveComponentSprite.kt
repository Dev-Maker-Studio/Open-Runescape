package com.jagex.game.runetek5.config.rsinterface

class InteractiveComponentSprite(
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val rowWidth: IntArray,
    @JvmField val rowStartX: IntArray,
    @JvmField val surface: ComponentSurface,
    @JvmField val spriteId: Int
) {

    fun withinBounds(x: Int, y: Int): Boolean {
        if (y >= 0 && y < this.rowStartX.size) {
            val startX = this.rowStartX[y]
            return x >= startX && x <= startX + this.rowWidth[y]
        }
        return false
    }
}
