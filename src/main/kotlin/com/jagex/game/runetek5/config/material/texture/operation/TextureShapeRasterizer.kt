package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.utils.ArrayUtils

abstract class TextureShapeRasterizer(
    @JvmField val fillColor: Int,
    @JvmField val strokeColor: Int,
    @JvmField val strokeWidth: Int
) {

    abstract fun drawFilled(var1: Int, var2: Int)

    abstract fun drawFilledAndStroked(var1: Int, var2: Int)

    abstract fun drawStroked(var1: Int, var2: Int)

    companion object {
        @JvmField
        var pixelBuffer: Array<IntArray>? = null

        @JvmStatic
        fun drawVerticalLine(x: Int, y1: Int, y2: Int, color: Int) {
            var y: Int
            if (y1 > y2) {
                y = y2
                while (y < y1) {
                    pixelBuffer!![y][x] = color
                    y++
                }
            } else {
                y = y1
                while (y < y2) {
                    pixelBuffer!![y][x] = color
                    y++
                }
            }
        }

        @JvmStatic
        fun drawClippedVerticalLine(x: Int, y1: Int, y2: Int, color: Int) {
            if (x >= TextureOpRasterizer.clipMinX && x <= TextureOpRasterizer.clipMaxX) {
                val clampedY1 = ArrayUtils.method4890(y1, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
                val clampedY2 = ArrayUtils.method4890(y2, TextureOpRasterizer.clipMinY, TextureOpRasterizer.clipMaxY)
                drawVerticalLine(x, clampedY1, clampedY2, color)
            }
        }

        @JvmStatic
        fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
            val deltaX = x2 - x1
            val deltaY = y2 - y1
            if (deltaX == 0) {
                if (deltaY != 0) {
                    drawClippedVerticalLine(x1, y1, y2, color)
                }
            } else if (deltaY == 0) {
                ArrayUtils.method11250(x1, x2, y1, color)
            } else {
                val slope = (deltaY shl 12) / deltaX
                val intercept = y1 - (x1 * slope shr 12)
                val startX: Int
                val startY: Int
                if (x1 < TextureOpRasterizer.clipMinX) {
                    startY = (slope * TextureOpRasterizer.clipMinX shr 12) + intercept
                    startX = TextureOpRasterizer.clipMinX
                } else if (x1 > TextureOpRasterizer.clipMaxX) {
                    startY = (slope * TextureOpRasterizer.clipMaxX shr 12) + intercept
                    startX = TextureOpRasterizer.clipMaxX
                } else {
                    startY = y1
                    startX = x1
                }
                val endX: Int
                val endY: Int
                if (x2 < TextureOpRasterizer.clipMinX) {
                    endY = (slope * TextureOpRasterizer.clipMinX shr 12) + intercept
                    endX = TextureOpRasterizer.clipMinX
                } else if (x2 > TextureOpRasterizer.clipMaxX) {
                    endY = (slope * TextureOpRasterizer.clipMaxX shr 12) + intercept
                    endX = TextureOpRasterizer.clipMaxX
                } else {
                    endY = y2
                    endX = x2
                }
                var clampedStartY = startY
                var clampedStartX = startX
                if (clampedStartY < TextureOpRasterizer.clipMinY) {
                    clampedStartY = TextureOpRasterizer.clipMinY
                    clampedStartX = (TextureOpRasterizer.clipMinY - intercept shl 12) / slope
                } else if (clampedStartY > TextureOpRasterizer.clipMaxY) {
                    clampedStartY = TextureOpRasterizer.clipMaxY
                    clampedStartX = (TextureOpRasterizer.clipMaxY - intercept shl 12) / slope
                }
                var clampedEndY = endY
                var clampedEndX = endX
                if (clampedEndY < TextureOpRasterizer.clipMinY) {
                    clampedEndY = TextureOpRasterizer.clipMinY
                    clampedEndX = (TextureOpRasterizer.clipMinY - intercept shl 12) / slope
                } else if (clampedEndY > TextureOpRasterizer.clipMaxY) {
                    clampedEndY = TextureOpRasterizer.clipMaxY
                    clampedEndX = (TextureOpRasterizer.clipMaxY - intercept shl 12) / slope
                }
                BezierCurveRasterizer.drawBresenhamLine(clampedStartX, clampedStartY, clampedEndX, clampedEndY, color)
            }
        }
    }
}
