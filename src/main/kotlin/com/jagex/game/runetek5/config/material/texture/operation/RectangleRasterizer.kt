package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils

class RectangleRasterizer(
    i_1: Int,
    i_2: Int,
    i_3: Int,
    i_4: Int,
    i_5: Int,
    i_6: Int,
    i_7: Int
) : TextureShapeRasterizer(i_5, i_6, i_7) {

    @JvmField val left: Int = i_1
    @JvmField val top: Int = i_2
    @JvmField val right: Int = i_3
    @JvmField val bottom: Int = i_4

    override fun drawStroked(i_1: Int, i_2: Int) {
        val i_4 = left * i_1 shr 12
        val i_5 = right * i_1 shr 12
        val i_6 = i_2 * top shr 12
        val i_7 = i_2 * bottom shr 12
        drawStrokedRectangle(i_4, i_5, i_6, i_7, strokeColor, strokeWidth)
    }

    override fun drawFilled(i_1: Int, i_2: Int) {
        val i_4 = left * i_1 shr 12
        val i_5 = right * i_1 shr 12
        val i_6 = i_2 * top shr 12
        val i_7 = i_2 * bottom shr 12
        StaticMethods.drawFilledRectangleToPixelBuffer(i_4, i_5, i_6, i_7, fillColor)
    }

    override fun drawFilledAndStroked(i_1: Int, i_2: Int) {
        val i_4 = left * i_1 shr 12
        val i_5 = right * i_1 shr 12
        val i_6 = i_2 * top shr 12
        val i_7 = i_2 * bottom shr 12
        StaticMethods.drawStrokedAndFilledRectangle(i_4, i_5, i_6, i_7, fillColor, strokeColor, strokeWidth)
    }

    companion object {
        @JvmStatic
        fun decode(rsbytebuffer0: Packet): RectangleRasterizer {
            return RectangleRasterizer(
                rsbytebuffer0.readShort(),
                rsbytebuffer0.readShort(),
                rsbytebuffer0.readShort(),
                rsbytebuffer0.readShort(),
                rsbytebuffer0.readUMedium(),
                rsbytebuffer0.readUMedium(),
                rsbytebuffer0.readUByte()
            )
        }

        @JvmStatic
        fun drawUnclippedStrokedRectangle(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
            val i_7 = i_5 + i_2
            val i_8 = i_3 - i_5
            var i_9: Int
            i_9 = i_2
            while (i_9 < i_7) {
                ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_9], i_0, i_1, i_4)
                i_9++
            }
            i_9 = i_3
            while (i_9 > i_8) {
                ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_9], i_0, i_1, i_4)
                --i_9
            }
            i_9 = i_5 + i_0
            val i_10 = i_1 - i_5
            for (i_11 in i_7..i_8) {
                val ints_12 = TextureShapeRasterizer.pixelBuffer!![i_11]
                ArrayUtils.setArrayValuesTo(ints_12, i_0, i_9, i_4)
                ArrayUtils.setArrayValuesTo(ints_12, i_10, i_1, i_4)
            }
        }

        @JvmStatic
        fun drawClippedStrokedRectangle(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
            var i_01 = i_0
            var i_11 = i_1
            var i_21 = i_2
            var i_31 = i_3
            if (i_21 <= TextureOpRasterizer.clipMaxY && i_31 >= TextureOpRasterizer.clipMinY) {
                val bool6: Boolean
                if (i_01 < TextureOpRasterizer.clipMinX) {
                    i_01 = TextureOpRasterizer.clipMinX
                    bool6 = false
                } else if (i_01 > TextureOpRasterizer.clipMaxX) {
                    i_01 = TextureOpRasterizer.clipMaxX
                    bool6 = false
                } else {
                    bool6 = true
                }
                val bool7: Boolean
                if (i_11 < TextureOpRasterizer.clipMinX) {
                    i_11 = TextureOpRasterizer.clipMinX
                    bool7 = false
                } else if (i_11 > TextureOpRasterizer.clipMaxX) {
                    i_11 = TextureOpRasterizer.clipMaxX
                    bool7 = false
                } else {
                    bool7 = true
                }
                if (i_21 >= TextureOpRasterizer.clipMinY) {
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_21++], i_01, i_11, i_4)
                } else {
                    i_21 = TextureOpRasterizer.clipMinY
                }
                if (i_31 <= TextureOpRasterizer.clipMaxY) {
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_31--], i_01, i_11, i_4)
                } else {
                    i_31 = TextureOpRasterizer.clipMaxY
                }
                var i_8: Int
                if (bool6 && bool7) {
                    i_8 = i_21
                    while (i_8 <= i_31) {
                        val ints_9 = TextureShapeRasterizer.pixelBuffer!![i_8]
                        ints_9[i_01] = i_4
                        ints_9[i_11] = i_4
                        i_8++
                    }
                } else if (bool6) {
                    i_8 = i_21
                    while (i_8 <= i_31) {
                        TextureShapeRasterizer.pixelBuffer!![i_8][i_01] = i_4
                        i_8++
                    }
                } else if (bool7) {
                    i_8 = i_21
                    while (i_8 <= i_31) {
                        TextureShapeRasterizer.pixelBuffer!![i_8][i_11] = i_4
                        i_8++
                    }
                }
            }
        }

        @JvmStatic
        fun drawStrokedRectangle(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
            if (i_0 >= TextureOpRasterizer.clipMinX && i_1 <= TextureOpRasterizer.clipMaxX && i_2 >= TextureOpRasterizer.clipMinY && i_3 <= TextureOpRasterizer.clipMaxY) {
                if (i_5 == 1) {
                    StaticMethods.drawStrokedRectangleToPixelBuffer(i_0, i_1, i_2, i_3, i_4)
                } else {
                    drawUnclippedStrokedRectangle(i_0, i_1, i_2, i_3, i_4, i_5)
                }
            } else if (i_5 == 1) {
                drawClippedStrokedRectangle(i_0, i_1, i_2, i_3, i_4)
            } else {
                ArrayUtils.drawClippedStrokedRectangle(i_0, i_1, i_2, i_3, i_4, i_5)
            }
        }
    }
}
