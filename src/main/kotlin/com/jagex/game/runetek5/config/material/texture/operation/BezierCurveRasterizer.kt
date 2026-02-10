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
    i_9: Int,
    i_10: Int
) : TextureShapeRasterizer(-1, i_9, i_10) {

    override fun drawFilled(var1: Int, var2: Int) {
    }

    override fun drawFilledAndStroked(var1: Int, var2: Int) {
    }

    override fun drawStroked(var1: Int, var2: Int) {
        val i_4 = controlX0 * var1 shr 12
        val i_5 = var2 * controlY0 shr 12
        val i_6 = controlX1 * var1 shr 12
        val i_7 = var2 * controlY1 shr 12
        val i_8 = controlX2 * var1 shr 12
        val i_9 = var2 * controlY2 shr 12
        val i_10 = controlX3 * var1 shr 12
        val i_11 = var2 * controlY3 shr 12
        drawBezierCurve(i_4, i_5, i_6, i_7, i_8, i_9, i_10, i_11, strokeColor)
    }

    companion object {
        @JvmStatic
        fun drawBresenhamLine(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
            var i_01 = i_0
            var i_21 = i_2
            var i_15 = i_1
            var i_31 = i_3
            val i_6 = i_31 - i_15
            val i_7 = i_21 - i_01

            if (i_7 == 0) {
                if (i_6 != 0) {
                    TextureShapeRasterizer.drawVerticalLine(i_01, i_15, i_31, i_4)
                }
            } else if (i_6 == 0) {
                ArrayUtils.drawHorizontalLine(i_01, i_21, i_15, i_4)
            } else {
                var i_6_adj = if (i_6 < 0) -i_6 else i_6
                var i_7_adj = if (i_7 < 0) -i_7 else i_7
                val bool8 = i_6_adj > i_7_adj

                var i_9: Int
                var i_10: Int
                if (bool8) {
                    i_9 = i_01
                    i_10 = i_21
                    i_01 = i_15
                    i_15 = i_9
                    i_21 = i_31
                    i_31 = i_10
                }

                if (i_01 > i_21) {
                    i_9 = i_01
                    i_10 = i_15
                    i_01 = i_21
                    i_21 = i_9
                    i_15 = i_31
                    i_31 = i_10
                }

                i_9 = i_15
                i_10 = i_21 - i_01
                var i_11 = i_31 - i_15
                var i_12 = -(i_10 shr 1)
                val i_13 = if (i_15 < i_31) 1 else -1

                if (i_11 < 0) {
                    i_11 = -i_11
                }

                if (bool8) {
                    for (i_14 in i_01..i_21) {
                        TextureShapeRasterizer.pixelBuffer!![i_14][i_9] = i_4
                        i_12 += i_11
                        if (i_12 > 0) {
                            i_9 += i_13
                            i_12 -= i_10
                        }
                    }
                } else {
                    for (i_14 in i_01..i_21) {
                        TextureShapeRasterizer.pixelBuffer!![i_9][i_14] = i_4
                        i_12 += i_11
                        if (i_12 > 0) {
                            i_9 += i_13
                            i_12 -= i_10
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun drawUnclippedBezier(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int) {
            if (i_0 == i_2 && i_3 == i_1 && i_4 == i_6 && i_5 == i_7) {
                drawBresenhamLine(i_0, i_1, i_6, i_7, i_8)
            } else {
                var i_10 = i_0
                var i_11 = i_1
                val i_12 = i_0 * 3
                val i_13 = i_1 * 3
                val i_14 = i_2 * 3
                val i_15 = i_3 * 3
                val i_16 = i_4 * 3
                val i_17 = i_5 * 3
                val i_18 = i_6 - i_16 + i_14 - i_0
                val i_19 = i_15 + (i_7 - i_17) - i_1
                val i_20 = i_12 + (i_16 - i_14 - i_14)
                val i_21 = i_13 + (i_17 - i_15 - i_15)
                val i_22 = i_14 - i_12
                val i_23 = i_15 - i_13

                var i_24 = 128
                while (i_24 <= 4096) {
                    val i_25 = i_24 * i_24 shr 12
                    val i_26 = i_25 * i_24 shr 12
                    val i_27 = i_18 * i_26
                    val i_28 = i_26 * i_19
                    val i_29 = i_20 * i_25
                    val i_30 = i_25 * i_21
                    val i_31 = i_24 * i_22
                    val i_32 = i_24 * i_23
                    val i_33 = i_0 + (i_29 + i_27 + i_31 shr 12)
                    val i_34 = (i_28 + i_30 + i_32 shr 12) + i_1
                    drawBresenhamLine(i_10, i_11, i_33, i_34, i_8)
                    i_10 = i_33
                    i_11 = i_34
                    i_24 += 128
                }
            }
        }

        @JvmStatic
        fun drawClippedBezier(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int) {
            if (i_0 == i_2 && i_3 == i_1 && i_4 == i_6 && i_5 == i_7) {
                TextureShapeRasterizer.drawLine(i_0, i_1, i_6, i_7, i_8)
            } else {
                var i_10 = i_0
                var i_11 = i_1
                val i_12 = i_0 * 3
                val i_13 = i_1 * 3
                val i_14 = i_2 * 3
                val i_15 = i_3 * 3
                val i_16 = i_4 * 3
                val i_17 = i_5 * 3
                val i_18 = i_6 - i_16 + i_14 - i_0
                val i_19 = i_15 + (i_7 - i_17) - i_1
                val i_20 = i_12 + (i_16 - i_14 - i_14)
                val i_21 = i_13 + (i_17 - i_15 - i_15)
                val i_22 = i_14 - i_12
                val i_23 = i_15 - i_13

                var i_24 = 128
                while (i_24 <= 4096) {
                    val i_25 = i_24 * i_24 shr 12
                    val i_26 = i_24 * i_25 shr 12
                    val i_27 = i_18 * i_26
                    val i_28 = i_19 * i_26
                    val i_29 = i_25 * i_20
                    val i_30 = i_21 * i_25
                    val i_31 = i_22 * i_24
                    val i_32 = i_24 * i_23
                    val i_33 = i_0 + (i_29 + i_27 + i_31 shr 12)
                    val i_34 = (i_30 + i_28 + i_32 shr 12) + i_1
                    TextureShapeRasterizer.drawLine(i_10, i_11, i_33, i_34, i_8)
                    i_10 = i_33
                    i_11 = i_34
                    i_24 += 128
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
        fun drawBezierCurve(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int) {
            if (i_0 >= TextureOpRasterizer.clipMinX && i_0 <= TextureOpRasterizer.clipMaxX &&
                i_2 >= TextureOpRasterizer.clipMinX && i_2 <= TextureOpRasterizer.clipMaxX &&
                i_4 >= TextureOpRasterizer.clipMinX && i_4 <= TextureOpRasterizer.clipMaxX &&
                i_6 >= TextureOpRasterizer.clipMinX && i_6 <= TextureOpRasterizer.clipMaxX &&
                i_1 >= TextureOpRasterizer.clipMinY && i_1 <= TextureOpRasterizer.clipMaxY &&
                i_3 >= TextureOpRasterizer.clipMinY && i_3 <= TextureOpRasterizer.clipMaxY &&
                i_5 >= TextureOpRasterizer.clipMinY && i_5 <= TextureOpRasterizer.clipMaxY &&
                i_7 >= TextureOpRasterizer.clipMinY && i_7 <= TextureOpRasterizer.clipMaxY
            ) {
                drawUnclippedBezier(i_0, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_8)
            } else {
                drawClippedBezier(i_0, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_8)
            }
        }
    }
}
