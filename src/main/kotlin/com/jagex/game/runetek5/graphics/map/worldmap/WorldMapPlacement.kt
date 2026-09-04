package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.Node

/**
 * Represents the placement and coordinate mapping of a world map element.
 * Maps between world map coordinates and actual game coordinates.
 */
class WorldMapPlacement(
    /** The plane/level this placement is on */
    @JvmField val plane: Int,

    /** Starting X coordinate in game world */
    @JvmField val startX: Int,

    /** Starting Y coordinate in game world */
    @JvmField val startY: Int,

    /** Ending X coordinate in game world */
    @JvmField val endX: Int,

    /** Ending Y coordinate in game world */
    @JvmField val endY: Int,

    /** Minimum X coordinate in world map space */
    @JvmField val mapMinX: Int,

    /** Minimum Y coordinate in world map space */
    @JvmField val mapMinY: Int,

    /** Maximum X coordinate in world map space */
    @JvmField val mapMaxX: Int,

    /** Maximum Y coordinate in world map space */
    @JvmField val mapMaxY: Int
) : Node<WorldMapPlacement>() {

    /**
     * Checks if the coordinates are within this placement's game coordinate bounds on the specified plane
     */
    fun withinPlaneBounds(plane: Int, x: Int, y: Int): Boolean {
        return this.plane == plane && x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY
    }

    /**
     * Checks if the given world map coordinates are within this placement's bounds
     * @param mapX World map X coordinate
     * @param mapY World map Y coordinate
     * @return true if coordinates are within world map bounds
     */
    fun withinMapBounds(mapX: Int, mapY: Int): Boolean {
        return mapX >= this.mapMinX && mapX <= this.mapMaxX && mapY >= this.mapMinY && mapY <= this.mapMaxY
    }

    /**
     * Converts world map coordinates to game coordinates
     * @param mapX World map X coordinate
     * @param mapY World map Y coordinate
     * @param position Output array [plane, gameX, gameY]
     */
    fun mapToGameCoords(mapX: Int, mapY: Int, position: IntArray) {
        position[0] = this.plane
        position[1] = this.startX - this.mapMinX + mapX
        position[2] = this.startY - this.mapMinY + mapY
    }

    /**
     * Converts game coordinates to world map coordinates
     * @param gameX Game world X coordinate
     * @param gameY Game world Y coordinate
     * @param position Output array [0, worldMapX, worldMapY]
     */
    fun cache(gameX: Int, gameY: Int, position: IntArray) {
        position[0] = 0
        position[1] = this.mapMinX - this.startX + gameX
        position[2] = this.mapMinY - this.startY + gameY
    }

    /**
     * Checks if coordinates are within game coordinate bounds (any plane)
     */
    fun withinBounds(x: Int, y: Int): Boolean {
        return x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY
    }
}
