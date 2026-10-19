package com.jagex.game.runetek5.movement

/**
 * [J]
 * < Fully refactored >
 * @author Brainpower - 28/05/2020
 * Represents a 3 dimensional world view grid
 */
class CoordGrid {

    /**
     * The floor (z) represented
     */
    @JvmField val plane: Int

    /**
     * The position in the width (x)
     */
    @JvmField var x: Int = 0

    /**
     * The position in length (y)
     */
    @JvmField var y: Int = 0

    /**
     * Creates a coordgrid from a bitpacked position
     */
    constructor(bitpacked: Int) {
        if (bitpacked == -1) {
            plane = -1
        } else {
            plane = bitpacked shr 28 and 0x3
            x = bitpacked shr 14 and 0x3fff
            y = bitpacked and 0x3fff
        }
    }

    /**
     * Creates a coordgrid that needs manipulation before use
     */
    constructor() {
        plane = -1
    }

    /**
     * Create a coordgrid directly
     */
    constructor(plane: Int, x: Int, y: Int) {
        this.plane = plane
        this.x = x
        this.y = y
    }

    /**
     * Plane, ?, ?, base_x, base_y
     */
    override fun toString(): String {
        return "$plane,${x shr 6},${y shr 6},${x and 0x3f},${y and 0x3f}"
    }
}
