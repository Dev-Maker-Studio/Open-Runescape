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
    @JvmField val anInt7701: Int,

    /** Minimum Y coordinate in world map space */
    @JvmField val anInt7703: Int,

    /** Maximum X coordinate in world map space */
    @JvmField val anInt7704: Int,

    /** Maximum Y coordinate in world map space */
    @JvmField val anInt7705: Int
) : Node<WorldMapPlacement>() {

    /**
     * Checks if the coordinates are within this placement's game coordinate bounds on the specified plane
     */
    fun withinPlaneBounds(plane: Int, x: Int, y: Int): Boolean {
        return this.plane == plane && x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY
    }

    /**
     * Checks if the given world map coordinates are within this placement's bounds
     * @param i_1 World map X coordinate
     * @param i_2 World map Y coordinate
     * @return true if coordinates are within world map bounds
     */
    fun method12409(i_1: Int, i_2: Int): Boolean {
        return i_1 >= this.anInt7701 && i_1 <= this.anInt7704 && i_2 >= this.anInt7703 && i_2 <= this.anInt7705
    }

    /**
     * Converts world map coordinates to game coordinates
     * @param i_1 World map X coordinate
     * @param i_2 World map Y coordinate
     * @param position Output array [plane, gameX, gameY]
     */
    fun method12410(i_1: Int, i_2: Int, position: IntArray) {
        position[0] = this.plane
        position[1] = this.startX - this.anInt7701 + i_1
        position[2] = this.startY - this.anInt7703 + i_2
    }

    /**
     * Converts game coordinates to world map coordinates
     * @param offsetX Game world X offset
     * @param offsetY Game world Y offset
     * @param position Output array [0, worldMapX, worldMapY]
     */
    fun cache(offsetX: Int, offsetY: Int, position: IntArray) {
        position[0] = 0
        position[1] = this.anInt7701 - this.startX + offsetX
        position[2] = this.anInt7703 - this.startY + offsetY
    }

    /**
     * Checks if coordinates are within game coordinate bounds (any plane)
     */
    fun withinBounds(x: Int, y: Int): Boolean {
        return x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY
    }
}
