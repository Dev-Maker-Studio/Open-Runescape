package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.ArrayUtils

/**
 * Texture operation bezier curve rendering class.
 */
class BezierCurveRasterizer(
    @JvmField val controlX0: Int,
    @JvmField val controlY0: Int,
    @JvmField val controlX1: Int,
    @JvmField val controlY1: Int,
    @JvmField val controlX2: Int,
    @JvmField val controlY2: Int,
    @JvmField val controlX3: Int,
    @JvmField val controlY3: Int,
    strokeColorParam: Int,
    strokeWidthParam: Int
) : TextureShapeRasterizer(-1, strokeColorParam, strokeWidthParam) {

    override fun drawFilled(width: Int, height: Int) {
    }

    override fun drawFilledAndStroked(width: Int, height: Int) {
    }

    override fun drawStroked(width: Int, height: Int) {
        val scaledX0 = controlX0 * width shr 12
        val scaledY0 = height * controlY0 shr 12
        val scaledX1 = controlX1 * width shr 12
        val scaledY1 = height * controlY1 shr 12
        val scaledX2 = controlX2 * width shr 12
        val scaledY2 = height * controlY2 shr 12
        val scaledX3 = controlX3 * width shr 12
        val scaledY3 = height * controlY3 shr 12
        drawBezierCurve(scaledX0, scaledY0, scaledX1, scaledY1, scaledX2, scaledY2, scaledX3, scaledY3, strokeColor)
    }

    companion object {
        @JvmStatic
        fun drawBresenhamLine(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
            var startX = x1
            var endX = x2
            var startY = y1
            var endY = y2
            val dy = endY - startY
            val dx = endX - startX

            if (dx == 0) {
                if (dy != 0) {
                    TextureShapeRasterizer.drawVerticalLine(startX, startY, endY, color)
                }
            } else if (dy == 0) {
                ArrayUtils.drawHorizontalLine(startX, endX, startY, color)
            } else {
                var absDy = if (dy < 0) -dy else dy
                var absDx = if (dx < 0) -dx else dx
                val isSteep = absDy > absDx

                var temp1: Int
                var temp2: Int
                if (isSteep) {
                    temp1 = startX
                    temp2 = endX
                    startX = startY
                    startY = temp1
                    endX = endY
                    endY = temp2
                }

                if (startX > endX) {
                    temp1 = startX
                    temp2 = startY
                    startX = endX
                    endX = temp1
                    startY = endY
                    endY = temp2
                }

                temp1 = startY
                temp2 = endX - startX
                var deltaY = endY - startY
                var error = -(temp2 shr 1)
                val yStep = if (startY < endY) 1 else -1

                if (deltaY < 0) {
                    deltaY = -deltaY
                }

                if (isSteep) {
                    for (i in startX..endX) {
                        TextureShapeRasterizer.pixelBuffer!![i][temp1] = color
                        error += deltaY
                        if (error > 0) {
                            temp1 += yStep
                            error -= temp2
                        }
                    }
                } else {
                    for (i in startX..endX) {
                        TextureShapeRasterizer.pixelBuffer!![temp1][i] = color
                        error += deltaY
                        if (error > 0) {
                            temp1 += yStep
                            error -= temp2
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun drawUnclippedBezier(p0x: Int, p0y: Int, p1x: Int, p1y: Int, p2x: Int, p2y: Int, p3x: Int, p3y: Int, color: Int) {
            if (p0x == p1x && p1y == p0y && p2x == p3x && p2y == p3y) {
                drawBresenhamLine(p0x, p0y, p3x, p3y, color)
            } else {
                var prevX = p0x
                var prevY = p0y
                val p0x3 = p0x * 3
                val p0y3 = p0y * 3
                val p1x3 = p1x * 3
                val p1y3 = p1y * 3
                val p2x3 = p2x * 3
                val p2y3 = p2y * 3
                val coeffAx = p3x - p2x3 + p1x3 - p0x
                val coeffAy = p1y3 + (p3y - p2y3) - p0y
                val coeffBx = p0x3 + (p2x3 - p1x3 - p1x3)
                val coeffBy = p0y3 + (p2y3 - p1y3 - p1y3)
                val coeffCx = p1x3 - p0x3
                val coeffCy = p1y3 - p0y3

                var t = 128
                while (t <= 4096) {
                    val tSquared = t * t shr 12
                    val tCubed = tSquared * t shr 12
                    val axTerm = coeffAx * tCubed
                    val ayTerm = tCubed * coeffAy
                    val bxTerm = coeffBx * tSquared
                    val byTerm = tSquared * coeffBy
                    val cxTerm = t * coeffCx
                    val cyTerm = t * coeffCy
                    val curX = p0x + (bxTerm + axTerm + cxTerm shr 12)
                    val curY = (ayTerm + byTerm + cyTerm shr 12) + p0y
                    drawBresenhamLine(prevX, prevY, curX, curY, color)
                    prevX = curX
                    prevY = curY
                    t += 128
                }
            }
        }

        @JvmStatic
        fun drawClippedBezier(p0x: Int, p0y: Int, p1x: Int, p1y: Int, p2x: Int, p2y: Int, p3x: Int, p3y: Int, color: Int) {
            if (p0x == p1x && p1y == p0y && p2x == p3x && p2y == p3y) {
                TextureShapeRasterizer.drawLine(p0x, p0y, p3x, p3y, color)
            } else {
                var prevX = p0x
                var prevY = p0y
                val p0x3 = p0x * 3
                val p0y3 = p0y * 3
                val p1x3 = p1x * 3
                val p1y3 = p1y * 3
                val p2x3 = p2x * 3
                val p2y3 = p2y * 3
                val coeffAx = p3x - p2x3 + p1x3 - p0x
                val coeffAy = p1y3 + (p3y - p2y3) - p0y
                val coeffBx = p0x3 + (p2x3 - p1x3 - p1x3)
                val coeffBy = p0y3 + (p2y3 - p1y3 - p1y3)
                val coeffCx = p1x3 - p0x3
                val coeffCy = p1y3 - p0y3

                var t = 128
                while (t <= 4096) {
                    val tSquared = t * t shr 12
                    val tCubed = t * tSquared shr 12
                    val axTerm = coeffAx * tCubed
                    val ayTerm = coeffAy * tCubed
                    val bxTerm = tSquared * coeffBx
                    val byTerm = coeffBy * tSquared
                    val cxTerm = coeffCx * t
                    val cyTerm = t * coeffCy
                    val curX = p0x + (bxTerm + axTerm + cxTerm shr 12)
                    val curY = (byTerm + ayTerm + cyTerm shr 12) + p0y
                    TextureShapeRasterizer.drawLine(prevX, prevY, curX, curY, color)
                    prevX = curX
                    prevY = curY
                    t += 128
                }
            }
        }

        @JvmStatic
        fun decode(buffer: Packet): BezierCurveRasterizer {
            return BezierCurveRasterizer(
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readUMedium(),
                buffer.readUByte()
            )
        }

        @JvmStatic
        fun drawBezierCurve(p0x: Int, p0y: Int, p1x: Int, p1y: Int, p2x: Int, p2y: Int, p3x: Int, p3y: Int, color: Int) {
            if (p0x >= TextureOpRasterizer.clipMinX && p0x <= TextureOpRasterizer.clipMaxX &&
                p1x >= TextureOpRasterizer.clipMinX && p1x <= TextureOpRasterizer.clipMaxX &&
                p2x >= TextureOpRasterizer.clipMinX && p2x <= TextureOpRasterizer.clipMaxX &&
                p3x >= TextureOpRasterizer.clipMinX && p3x <= TextureOpRasterizer.clipMaxX &&
                p0y >= TextureOpRasterizer.clipMinY && p0y <= TextureOpRasterizer.clipMaxY &&
                p1y >= TextureOpRasterizer.clipMinY && p1y <= TextureOpRasterizer.clipMaxY &&
                p2y >= TextureOpRasterizer.clipMinY && p2y <= TextureOpRasterizer.clipMaxY &&
                p3y >= TextureOpRasterizer.clipMinY && p3y <= TextureOpRasterizer.clipMaxY
            ) {
                drawUnclippedBezier(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y, color)
            } else {
                drawClippedBezier(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y, color)
            }
        }
    }
}
