package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.Node

class WorldMapPlacement(
    @JvmField val plane: Int,
    @JvmField val startX: Int,
    @JvmField val startY: Int,
    @JvmField val endX: Int,
    @JvmField val endY: Int,
    @JvmField val anInt7701: Int,
    @JvmField val anInt7703: Int,
    @JvmField val anInt7704: Int,
    @JvmField val anInt7705: Int
) : Node<WorldMapPlacement>() {

    fun withinPlaneBounds(plane: Int, x: Int, y: Int): Boolean {
        return this.plane == plane && x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY
    }

    fun method12409(i_1: Int, i_2: Int): Boolean {
        return i_1 >= this.anInt7701 && i_1 <= this.anInt7704 && i_2 >= this.anInt7703 && i_2 <= this.anInt7705
    }

    fun method12410(i_1: Int, i_2: Int, position: IntArray) {
        position[0] = this.plane
        position[1] = this.startX - this.anInt7701 + i_1
        position[2] = this.startY - this.anInt7703 + i_2
    }

    fun cache(offsetX: Int, offsetY: Int, position: IntArray) {
        position[0] = 0
        position[1] = this.anInt7701 - this.startX + offsetX
        position[2] = this.anInt7703 - this.startY + offsetY
    }

    fun withinBounds(x: Int, y: Int): Boolean {
        return x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY
    }
}
