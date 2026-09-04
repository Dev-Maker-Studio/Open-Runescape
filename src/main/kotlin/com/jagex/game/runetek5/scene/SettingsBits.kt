package com.jagex.game.runetek5.scene

class SettingsBits(x: Int, y: Int) {
    @JvmField val tileFunctionMasks: Array<Array<ByteArray>> = Array(4) { Array(x) { ByteArray(y) } }

    fun reset() {
        for (z in tileFunctionMasks.indices) {
            for (x in tileFunctionMasks[0].indices) {
                for (y in tileFunctionMasks[0][0].indices) {
                    tileFunctionMasks[z][x][y] = 0
                }
            }
        }
    }

    fun areRoofsHidden(x: Int, y: Int): Boolean {
        return x >= 0 && y >= 0 && x < tileFunctionMasks[1].size && y < tileFunctionMasks[1][x].size &&
               (tileFunctionMasks[1][x][y].toInt() and 0x2) != 0
    }

    fun isVisible(collisionPlane: Int, z: Int, x: Int, y: Int): Boolean {
        return (tileFunctionMasks[0][x][y].toInt() and 0x2) != 0 ||
               ((tileFunctionMasks[z][x][y].toInt() and 0x10) == 0 && getCollisionPlane(z, x, y) == collisionPlane)
    }

    fun getCollisionPlane(z: Int, x: Int, y: Int): Int {
        return if ((tileFunctionMasks[z][x][y].toInt() and 0x8) != 0) {
            0
        } else if (z > 0 && (tileFunctionMasks[1][x][y].toInt() and 0x2) != 0) {
            z - 1
        } else {
            z
        }
    }
}
