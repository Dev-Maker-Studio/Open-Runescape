package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils

class RectangleRasterizer(
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    fill: Int,
    stroke: Int,
    strokeW: Int
) : TextureShapeRasterizer(fill, stroke, strokeW) {

    @JvmField val left: Int = x1
    @JvmField val top: Int = y1
    @JvmField val right: Int = x2
    @JvmField val bottom: Int = y2

    override fun drawStroked(width: Int, height: Int) {
        val scaledLeft = left * width shr 12
        val scaledRight = right * width shr 12
        val scaledTop = height * top shr 12
        val scaledBottom = height * bottom shr 12
        drawStrokedRectangle(scaledLeft, scaledRight, scaledTop, scaledBottom, strokeColor, strokeWidth)
    }

    override fun drawFilled(width: Int, height: Int) {
        val scaledLeft = left * width shr 12
        val scaledRight = right * width shr 12
        val scaledTop = height * top shr 12
        val scaledBottom = height * bottom shr 12
        StaticMethods.drawFilledRectangleToPixelBuffer(scaledLeft, scaledRight, scaledTop, scaledBottom, fillColor)
    }

    override fun drawFilledAndStroked(width: Int, height: Int) {
        val scaledLeft = left * width shr 12
        val scaledRight = right * width shr 12
        val scaledTop = height * top shr 12
        val scaledBottom = height * bottom shr 12
        StaticMethods.drawStrokedAndFilledRectangle(scaledLeft, scaledRight, scaledTop, scaledBottom, fillColor, strokeColor, strokeWidth)
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): RectangleRasterizer {
            return RectangleRasterizer(
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readUMedium(),
                buffer.readUMedium(),
                buffer.readUByte()
            )
        }

        @JvmStatic
        fun drawUnclippedStrokedRectangle(leftEdge: Int, rightEdge: Int, topEdge: Int, bottomEdge: Int, color: Int, thickness: Int) {
            val topBorder = thickness + topEdge
            val bottomBorder = bottomEdge - thickness
            var row: Int
            row = topEdge
            while (row < topBorder) {
                ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![row], leftEdge, rightEdge, color)
                row++
            }
            row = bottomEdge
            while (row > bottomBorder) {
                ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![row], leftEdge, rightEdge, color)
                --row
            }
            row = thickness + leftEdge
            val rightBorder = rightEdge - thickness
            for (y in topBorder..bottomBorder) {
                val rowPixels = TextureShapeRasterizer.pixelBuffer!![y]
                ArrayUtils.setArrayValuesTo(rowPixels, leftEdge, row, color)
                ArrayUtils.setArrayValuesTo(rowPixels, rightBorder, rightEdge, color)
            }
        }

        @JvmStatic
        fun drawClippedStrokedRectangle(leftEdge: Int, rightEdge: Int, topEdge: Int, bottomEdge: Int, color: Int) {
            var clampedLeft = leftEdge
            var clampedRight = rightEdge
            var clampedTop = topEdge
            var clampedBottom = bottomEdge
            if (clampedTop <= TextureOpRasterizer.clipMaxY && clampedBottom >= TextureOpRasterizer.clipMinY) {
                val leftVisible: Boolean
                if (clampedLeft < TextureOpRasterizer.clipMinX) {
                    clampedLeft = TextureOpRasterizer.clipMinX
                    leftVisible = false
                } else if (clampedLeft > TextureOpRasterizer.clipMaxX) {
                    clampedLeft = TextureOpRasterizer.clipMaxX
                    leftVisible = false
                } else {
                    leftVisible = true
                }
                val rightVisible: Boolean
                if (clampedRight < TextureOpRasterizer.clipMinX) {
                    clampedRight = TextureOpRasterizer.clipMinX
                    rightVisible = false
                } else if (clampedRight > TextureOpRasterizer.clipMaxX) {
                    clampedRight = TextureOpRasterizer.clipMaxX
                    rightVisible = false
                } else {
                    rightVisible = true
                }
                if (clampedTop >= TextureOpRasterizer.clipMinY) {
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![clampedTop++], clampedLeft, clampedRight, color)
                } else {
                    clampedTop = TextureOpRasterizer.clipMinY
                }
                if (clampedBottom <= TextureOpRasterizer.clipMaxY) {
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![clampedBottom--], clampedLeft, clampedRight, color)
                } else {
                    clampedBottom = TextureOpRasterizer.clipMaxY
                }
                var y: Int
                if (leftVisible && rightVisible) {
                    y = clampedTop
                    while (y <= clampedBottom) {
                        val rowPixels = TextureShapeRasterizer.pixelBuffer!![y]
                        rowPixels[clampedLeft] = color
                        rowPixels[clampedRight] = color
                        y++
                    }
                } else if (leftVisible) {
                    y = clampedTop
                    while (y <= clampedBottom) {
                        TextureShapeRasterizer.pixelBuffer!![y][clampedLeft] = color
                        y++
                    }
                } else if (rightVisible) {
                    y = clampedTop
                    while (y <= clampedBottom) {
                        TextureShapeRasterizer.pixelBuffer!![y][clampedRight] = color
                        y++
                    }
                }
            }
        }

        @JvmStatic
        fun drawStrokedRectangle(leftEdge: Int, rightEdge: Int, topEdge: Int, bottomEdge: Int, color: Int, thickness: Int) {
            if (leftEdge >= TextureOpRasterizer.clipMinX && rightEdge <= TextureOpRasterizer.clipMaxX && topEdge >= TextureOpRasterizer.clipMinY && bottomEdge <= TextureOpRasterizer.clipMaxY) {
                if (thickness == 1) {
                    StaticMethods.drawStrokedRectangleToPixelBuffer(leftEdge, rightEdge, topEdge, bottomEdge, color)
                } else {
                    drawUnclippedStrokedRectangle(leftEdge, rightEdge, topEdge, bottomEdge, color, thickness)
                }
            } else if (thickness == 1) {
                drawClippedStrokedRectangle(leftEdge, rightEdge, topEdge, bottomEdge, color)
            } else {
                ArrayUtils.drawClippedStrokedRectangle(leftEdge, rightEdge, topEdge, bottomEdge, color, thickness)
            }
        }
    }
}
