package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.ArrayUtils

class EllipseRasterizer(
    i_1: Int,
    i_2: Int,
    i_3: Int,
    i_4: Int,
    i_5: Int,
    i_6: Int,
    i_7: Int
) : TextureShapeRasterizer(i_5, i_6, i_7) {

    val centerX: Int = i_1
    val centerY: Int = i_2
    val radiusX: Int = i_3
    val radiusY: Int = i_4

    override fun drawStroked(i_1: Int, i_2: Int) {
    }

    override fun drawFilled(i_1: Int, i_2: Int) {
        val i_4 = centerX * i_1 shr 12
        val i_5 = radiusX * i_1 shr 12
        val i_6 = i_2 * centerY shr 12
        val i_7 = i_2 * radiusY shr 12
        ArrayUtils.drawFilledEllipse(i_4, i_6, i_5, i_7, fillColor)
    }

    override fun drawFilledAndStroked(i_1: Int, i_2: Int) {
        val i_4 = centerX * i_1 shr 12
        val i_5 = radiusX * i_1 shr 12
        val i_6 = i_2 * centerY shr 12
        val i_7 = i_2 * radiusY shr 12
        drawEllipse(i_4, i_6, i_5, i_7, fillColor, strokeColor, strokeWidth)
    }

    companion object {
        @JvmStatic
        fun decode(rsbytebuffer0: Packet): EllipseRasterizer {
            return EllipseRasterizer(
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
        fun drawClippedEllipse(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
            var i_8 = 0
            var i_9 = i_3
            var i_10 = 0
            val i_11 = i_2 - i_6
            val i_12 = i_3 - i_6
            val i_13 = i_2 * i_2
            val i_14 = i_3 * i_3
            val i_15 = i_11 * i_11
            val i_16 = i_12 * i_12
            val i_17 = i_14 shl 1
            val i_18 = i_13 shl 1
            val i_19 = i_16 shl 1
            val i_20 = i_15 shl 1
            val i_21 = i_3 shl 1
            val i_22 = i_12 shl 1
            var i_23 = i_17 + (1 - i_21) * i_13
            var i_24 = i_14 - i_18 * (i_21 - 1)
            var i_25 = i_19 + (1 - i_22) * i_15
            var i_26 = i_16 - i_20 * (i_22 - 1)
            val i_27 = i_13 shl 2
            val i_28 = i_14 shl 2
            val i_29 = i_15 shl 2
            val i_30 = i_16 shl 2
            var i_31 = i_17 * 3
            var i_32 = i_18 * (i_21 - 3)
            var i_33 = i_19 * 3
            var i_34 = i_20 * (i_22 - 3)
            var i_35 = i_28
            var i_36 = (i_3 - 1) * i_27
            var i_37 = i_30
            var i_38 = i_29 * (i_12 - 1)
            var i_40: Int
            var i_41: Int
            var i_42: Int
            var i_43: Int
            if (i_1 >= TextureOpRasterizer.clipMinY && i_1 <= TextureOpRasterizer.clipMaxY) {
                val ints_39 = TextureShapeRasterizer.pixelBuffer!![i_1]
                i_40 = ArrayUtils.clamp(i_0 - i_2, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                i_41 = ArrayUtils.clamp(i_0 + i_2, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                i_42 = ArrayUtils.clamp(i_0 - i_11, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                i_43 = ArrayUtils.clamp(i_0 + i_11, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                ArrayUtils.setArrayValuesTo(ints_39, i_40, i_42, i_5)
                ArrayUtils.setArrayValuesTo(ints_39, i_42, i_43, i_4)
                ArrayUtils.setArrayValuesTo(ints_39, i_43, i_41, i_5)
            }

            while (i_9 > 0) {
                val bool47 = i_9 <= i_12
                if (bool47) {
                    if (i_25 < 0) {
                        while (i_25 < 0) {
                            i_25 += i_33
                            i_26 += i_37
                            i_33 += i_30
                            i_37 += i_30
                            ++i_10
                        }
                    }

                    if (i_26 < 0) {
                        i_25 += i_33
                        i_26 += i_37
                        i_33 += i_30
                        i_37 += i_30
                        ++i_10
                    }

                    i_25 += -i_38
                    i_26 += -i_34
                    i_34 -= i_29
                    i_38 -= i_29
                }

                if (i_23 < 0) {
                    while (i_23 < 0) {
                        i_23 += i_31
                        i_24 += i_35
                        i_31 += i_28
                        i_35 += i_28
                        ++i_8
                    }
                }

                if (i_24 < 0) {
                    i_23 += i_31
                    i_24 += i_35
                    i_31 += i_28
                    i_35 += i_28
                    ++i_8
                }

                i_23 += -i_36
                i_24 += -i_32
                i_32 -= i_27
                i_36 -= i_27
                --i_9
                i_40 = i_1 - i_9
                i_41 = i_9 + i_1
                if (i_41 >= TextureOpRasterizer.clipMinY && i_40 <= TextureOpRasterizer.clipMaxY) {
                    i_42 = ArrayUtils.clamp(i_0 + i_8, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                    i_43 = ArrayUtils.clamp(i_0 - i_8, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                    if (bool47) {
                        val i_44 = ArrayUtils.clamp(i_0 + i_10, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                        val i_45 = ArrayUtils.clamp(i_0 - i_10, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                        var ints_46: IntArray
                        if (i_40 >= TextureOpRasterizer.clipMinY) {
                            ints_46 = TextureShapeRasterizer.pixelBuffer!![i_40]
                            ArrayUtils.setArrayValuesTo(ints_46, i_43, i_45, i_5)
                            ArrayUtils.setArrayValuesTo(ints_46, i_45, i_44, i_4)
                            ArrayUtils.setArrayValuesTo(ints_46, i_44, i_42, i_5)
                        }

                        if (i_41 <= TextureOpRasterizer.clipMaxY) {
                            ints_46 = TextureShapeRasterizer.pixelBuffer!![i_41]
                            ArrayUtils.setArrayValuesTo(ints_46, i_43, i_45, i_5)
                            ArrayUtils.setArrayValuesTo(ints_46, i_45, i_44, i_4)
                            ArrayUtils.setArrayValuesTo(ints_46, i_44, i_42, i_5)
                        }
                    } else {
                        if (i_40 >= TextureOpRasterizer.clipMinY) {
                            ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_40], i_43, i_42, i_5)
                        }

                        if (i_41 <= TextureOpRasterizer.clipMaxY) {
                            ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_41], i_43, i_42, i_5)
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun drawUnclippedEllipse(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
            var i_8 = 0
            var i_9 = i_3
            var i_10 = 0
            val i_11 = i_2 - i_6
            val i_12 = i_3 - i_6
            val i_13 = i_2 * i_2
            val i_14 = i_3 * i_3
            val i_15 = i_11 * i_11
            val i_16 = i_12 * i_12
            val i_17 = i_14 shl 1
            val i_18 = i_13 shl 1
            val i_19 = i_16 shl 1
            val i_20 = i_15 shl 1
            val i_21 = i_3 shl 1
            val i_22 = i_12 shl 1
            var i_23 = i_17 + (1 - i_21) * i_13
            var i_24 = i_14 - i_18 * (i_21 - 1)
            var i_25 = i_19 + (1 - i_22) * i_15
            var i_26 = i_16 - i_20 * (i_22 - 1)
            val i_27 = i_13 shl 2
            val i_28 = i_14 shl 2
            val i_29 = i_15 shl 2
            val i_30 = i_16 shl 2
            var i_31 = i_17 * 3
            var i_32 = i_18 * (i_21 - 3)
            var i_33 = i_19 * 3
            var i_34 = i_20 * (i_22 - 3)
            var i_35 = i_28
            var i_36 = (i_3 - 1) * i_27
            var i_37 = i_30
            var i_38 = i_29 * (i_12 - 1)
            val ints_39 = TextureShapeRasterizer.pixelBuffer!![i_1]
            ArrayUtils.setArrayValuesTo(ints_39, i_0 - i_2, i_0 - i_11, i_5)
            ArrayUtils.setArrayValuesTo(ints_39, i_0 - i_11, i_0 + i_11, i_4)
            ArrayUtils.setArrayValuesTo(ints_39, i_0 + i_11, i_0 + i_2, i_5)

            while (i_9 > 0) {
                val bool40 = i_9 <= i_12
                if (bool40) {
                    if (i_25 < 0) {
                        while (i_25 < 0) {
                            i_25 += i_33
                            i_26 += i_37
                            i_33 += i_30
                            i_37 += i_30
                            ++i_10
                        }
                    }

                    if (i_26 < 0) {
                        i_25 += i_33
                        i_26 += i_37
                        i_33 += i_30
                        i_37 += i_30
                        ++i_10
                    }

                    i_25 += -i_38
                    i_26 += -i_34
                    i_34 -= i_29
                    i_38 -= i_29
                }

                if (i_23 < 0) {
                    while (i_23 < 0) {
                        i_23 += i_31
                        i_24 += i_35
                        i_31 += i_28
                        i_35 += i_28
                        ++i_8
                    }
                }

                if (i_24 < 0) {
                    i_23 += i_31
                    i_24 += i_35
                    i_31 += i_28
                    i_35 += i_28
                    ++i_8
                }

                i_23 += -i_36
                i_24 += -i_32
                i_32 -= i_27
                i_36 -= i_27
                --i_9
                val i_41 = i_1 - i_9
                val i_42 = i_9 + i_1
                val i_43 = i_0 + i_8
                val i_44 = i_0 - i_8
                if (bool40) {
                    val i_45 = i_0 + i_10
                    val i_46 = i_0 - i_10
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_41], i_44, i_46, i_5)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_41], i_46, i_45, i_4)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_41], i_45, i_43, i_5)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_42], i_44, i_46, i_5)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_42], i_46, i_45, i_4)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_42], i_45, i_43, i_5)
                } else {
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_41], i_44, i_43, i_5)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_42], i_44, i_43, i_5)
                }
            }
        }

        @JvmStatic
        fun drawCircle(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
            if (i_0 - i_2 >= TextureOpRasterizer.clipMinX && i_0 + i_2 <= TextureOpRasterizer.clipMaxX && i_1 - i_2 >= TextureOpRasterizer.clipMinY && i_2 + i_1 <= TextureOpRasterizer.clipMaxY) {
                ArrayUtils.drawUnclippedFilledAndStrokedCircle(i_0, i_1, i_2, i_3, i_4, i_5)
            } else {
                ArrayUtils.drawClippedFilledAndStrokedCircle(i_0, i_1, i_2, i_3, i_4, i_5)
            }
        }

        @JvmStatic
        fun drawEllipse(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
            if (i_3 == i_2) {
                drawCircle(i_0, i_1, i_2, i_4, i_5, i_6)
            } else if (i_0 - i_2 >= TextureOpRasterizer.clipMinX && i_0 + i_2 <= TextureOpRasterizer.clipMaxX && i_1 - i_3 >= TextureOpRasterizer.clipMinY && i_3 + i_1 <= TextureOpRasterizer.clipMaxY) {
                drawUnclippedEllipse(i_0, i_1, i_2, i_3, i_4, i_5, i_6)
            } else {
                drawClippedEllipse(i_0, i_1, i_2, i_3, i_4, i_5, i_6)
            }
        }
    }
}
