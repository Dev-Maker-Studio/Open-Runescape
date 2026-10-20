package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.Node

class WorldMapPlacement(
    @JvmField val plane: Int,

    @JvmField val startX: Int,

    @JvmField val startY: Int,

    @JvmField val endX: Int,

    @JvmField val endY: Int,

    @JvmField val mapMinX: Int,

    @JvmField val mapMinY: Int,

    @JvmField val mapMaxX: Int,

    @JvmField val mapMaxY: Int
) : Node<WorldMapPlacement>() {

    fun withinPlaneBounds(plane: Int, x: Int, y: Int): Boolean {
        return this.plane == plane && x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY
    }

    fun withinMapBounds(mapX: Int, mapY: Int): Boolean {
        return mapX >= this.mapMinX && mapX <= this.mapMaxX && mapY >= this.mapMinY && mapY <= this.mapMaxY
    }

    fun mapToGameCoords(mapX: Int, mapY: Int, position: IntArray) {
        position[0] = this.plane
        position[1] = this.startX - this.mapMinX + mapX
        position[2] = this.startY - this.mapMinY + mapY
    }

    fun cache(gameX: Int, gameY: Int, position: IntArray) {
        position[0] = 0
        position[1] = this.mapMinX - this.startX + gameX
        position[2] = this.mapMinY - this.startY + gameY
    }

    fun withinBounds(x: Int, y: Int): Boolean {
        return x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY
    }
}
