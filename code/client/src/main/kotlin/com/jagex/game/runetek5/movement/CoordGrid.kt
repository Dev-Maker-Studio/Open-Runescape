package com.jagex.game.runetek5.movement

class CoordGrid {

    @JvmField val plane: Int

    @JvmField var x: Int = 0

    @JvmField var y: Int = 0

    constructor(bitpacked: Int) {
        if (bitpacked == -1) {
            plane = -1
        } else {
            plane = bitpacked shr 28 and 0x3
            x = bitpacked shr 14 and 0x3fff
            y = bitpacked and 0x3fff
        }
    }

    constructor() {
        plane = -1
    }

    constructor(plane: Int, x: Int, y: Int) {
        this.plane = plane
        this.x = x
        this.y = y
    }

    override fun toString(): String {
        return "$plane,${x shr 6},${y shr 6},${x and 0x3f},${y and 0x3f}"
    }
}
