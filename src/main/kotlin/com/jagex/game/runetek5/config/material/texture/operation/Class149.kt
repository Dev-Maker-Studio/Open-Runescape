package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.utils.ArrayUtils

abstract class Class149(
    @JvmField val anInt1743: Int,
    @JvmField val anInt1741: Int,
    @JvmField val anInt1742: Int
) {

    abstract fun method2556(var1: Int, var2: Int)

    abstract fun method2557(var1: Int, var2: Int)

    abstract fun method2561(var1: Int, var2: Int)

    companion object {
        @JvmField
        var anIntArrayArray7072: Array<IntArray>? = null

        @JvmStatic
        fun method13411(x: Int, y1: Int, y2: Int, color: Int) {
            var y: Int
            if (y1 > y2) {
                y = y2
                while (y < y1) {
                    anIntArrayArray7072!![y][x] = color
                    y++
                }
            } else {
                y = y1
                while (y < y2) {
                    anIntArrayArray7072!![y][x] = color
                    y++
                }
            }
        }

        @JvmStatic
        fun method12746(x: Int, y1: Int, y2: Int, color: Int) {
            if (x >= TextureOpRasterizer.anInt7071 && x <= TextureOpRasterizer.anInt7069) {
                val clampedY1 = ArrayUtils.method4890(y1, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
                val clampedY2 = ArrayUtils.method4890(y2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068)
                method13411(x, clampedY1, clampedY2, color)
            }
        }

        @JvmStatic
        fun method6159(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
            val deltaX = x2 - x1
            val deltaY = y2 - y1
            if (deltaX == 0) {
                if (deltaY != 0) {
                    method12746(x1, y1, y2, color)
                }
            } else if (deltaY == 0) {
                ArrayUtils.method11250(x1, x2, y1, color)
            } else {
                val slope = (deltaY shl 12) / deltaX
                val intercept = y1 - (x1 * slope shr 12)
                val startX: Int
                val startY: Int
                if (x1 < TextureOpRasterizer.anInt7071) {
                    startY = (slope * TextureOpRasterizer.anInt7071 shr 12) + intercept
                    startX = TextureOpRasterizer.anInt7071
                } else if (x1 > TextureOpRasterizer.anInt7069) {
                    startY = (slope * TextureOpRasterizer.anInt7069 shr 12) + intercept
                    startX = TextureOpRasterizer.anInt7069
                } else {
                    startY = y1
                    startX = x1
                }
                val endX: Int
                val endY: Int
                if (x2 < TextureOpRasterizer.anInt7071) {
                    endY = (slope * TextureOpRasterizer.anInt7071 shr 12) + intercept
                    endX = TextureOpRasterizer.anInt7071
                } else if (x2 > TextureOpRasterizer.anInt7069) {
                    endY = (slope * TextureOpRasterizer.anInt7069 shr 12) + intercept
                    endX = TextureOpRasterizer.anInt7069
                } else {
                    endY = y2
                    endX = x2
                }
                var clampedStartY = startY
                var clampedStartX = startX
                if (clampedStartY < TextureOpRasterizer.anInt7070) {
                    clampedStartY = TextureOpRasterizer.anInt7070
                    clampedStartX = (TextureOpRasterizer.anInt7070 - intercept shl 12) / slope
                } else if (clampedStartY > TextureOpRasterizer.anInt7068) {
                    clampedStartY = TextureOpRasterizer.anInt7068
                    clampedStartX = (TextureOpRasterizer.anInt7068 - intercept shl 12) / slope
                }
                var clampedEndY = endY
                var clampedEndX = endX
                if (clampedEndY < TextureOpRasterizer.anInt7070) {
                    clampedEndY = TextureOpRasterizer.anInt7070
                    clampedEndX = (TextureOpRasterizer.anInt7070 - intercept shl 12) / slope
                } else if (clampedEndY > TextureOpRasterizer.anInt7068) {
                    clampedEndY = TextureOpRasterizer.anInt7068
                    clampedEndX = (TextureOpRasterizer.anInt7068 - intercept shl 12) / slope
                }
                Class149_Sub2.method11220(clampedStartX, clampedStartY, clampedEndX, clampedEndY, color)
            }
        }
    }
}
