package com.jagex.game.runetek5.utils

import com.jagex.game.runetek5.config.flo.FloType
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import kotlin.math.pow

/**
 *
 * @author Brainpower - 28/01/2023
 * Reference: https://www.rune-server.ee/runescape-development/rs2-client/tools/589900-rs2-hsb-color-picker.html
 *
 */
object ColorUtil {

    @JvmStatic
    var hsvToRgbTable: IntArray? = null

    @JvmStatic
    var hslToRgbUsers: Int = 0

    @JvmStatic
    var hsvToRgbUsers: Int = 0

    @JvmStatic
    var hslToRgb: IntArray? = null

    @JvmStatic
    var unsignedHslToRgb: IntArray? = null

    @JvmStatic
    fun getOverlayColorHsl(drawingarea: AbstractToolkit, overlay: FloType): Int {
        if (overlay.minimapColorHsl != -1) {
            return overlay.minimapColorHsl
        }
        if (overlay.texture != -1) {
            val material = drawingarea.materialProvider!!.getMaterial(overlay.texture)
            if (!material.detailsOnly) {
                return material.colorHsl.toInt()
            }
        }
        return overlay.tileColorHsl
    }

    @JvmStatic
    fun blend(colorA: Int, colorB: Int, i_2: Int): Int {
        if (colorA == colorB) {
            return colorA
        } else {
            val i_4 = 128 - i_2
            val i_5 = i_4 * (colorA and 0x7f) + (colorB and 0x7f) * i_2 shr 7
            val i_6 = i_4 * (colorA and 0x380) + i_2 * (colorB and 0x380) shr 7
            val i_7 = (colorB and 0xfc00) * i_2 + i_4 * (colorA and 0xfc00) shr 7
            return i_7 and 0xfc00 or (i_6 and 0x380) or (i_5 and 0x7f)
        }
    }

    @JvmStatic
    fun hsl16to24(hue: Int, saturation: Int, lightness: Int): Int {
        var saturation = saturation
        if (lightness > 243) {
            saturation = saturation shr 4
        } else if (lightness > 217) {
            saturation = saturation shr 3
        } else if (lightness > 192) {
            saturation = saturation shr 2
        } else if (lightness > 179) {
            saturation = saturation shr 1
        }
        return ((hue and 0xff) shr 2 shl 10) + (lightness shr 1) + (saturation shr 5 shl 7)
    }

    @JvmStatic
    fun calculateHsvToRgbTable() {
        if (hsvToRgbTable == null) {
            hsvToRgbTable = IntArray(65536)
            val d_1 = 0.7 + (Math.random() * 0.03 - 0.015)
            var i_3 = 0
            for (i_4 in 0 until 512) {
                val f_5 = 360.0f * ((i_4 shr 3) / 64.0f + 0.0078125f)
                val f_6 = (i_4 and 0x7) / 8.0f + 0.0625f
                for (i_7 in 0 until 128) {
                    val f_8 = i_7 / 128.0f
                    var f_9 = 0.0f
                    var f_10 = 0.0f
                    var f_11 = 0.0f
                    val f_12 = f_5 / 60.0f
                    val i_13 = f_12.toInt()
                    val i_14 = i_13 % 6
                    val f_15 = f_12 - i_13
                    val f_16 = f_8 * (1.0f - f_6)
                    val f_17 = f_8 * (1.0f - f_6 * f_15)
                    val f_18 = (1.0f - f_6 * (1.0f - f_15)) * f_8
                    when (i_14) {
                        0 -> {
                            f_9 = f_8
                            f_10 = f_18
                            f_11 = f_16
                        }
                        1 -> {
                            f_9 = f_17
                            f_10 = f_8
                            f_11 = f_16
                        }
                        2 -> {
                            f_9 = f_16
                            f_10 = f_8
                            f_11 = f_18
                        }
                        3 -> {
                            f_9 = f_16
                            f_10 = f_17
                            f_11 = f_8
                        }
                        4 -> {
                            f_9 = f_18
                            f_10 = f_16
                            f_11 = f_8
                        }
                        5 -> {
                            f_9 = f_8
                            f_10 = f_16
                            f_11 = f_17
                        }
                    }
                    f_9 = f_9.toDouble().pow(d_1).toFloat()
                    f_10 = f_10.toDouble().pow(d_1).toFloat()
                    f_11 = f_11.toDouble().pow(d_1).toFloat()
                    val i_19 = (f_9 * 256.0f).toInt()
                    val i_20 = (256.0f * f_10).toInt()
                    val i_21 = (256.0f * f_11).toInt()
                    val i_22 = i_21 + (i_20 shl 8) + (i_19 shl 16) + 0xffffff.inv()
                    hsvToRgbTable!![i_3++] = i_22
                }
            }
        }
    }

    @JvmStatic
    fun calculateHslToRgbTable() {
        if (hslToRgb == null) {
            hslToRgb = IntArray(65536)
            unsignedHslToRgb = IntArray(65536)
            val d_1 = 0.7 + (Math.random() * 0.03 - 0.015)
            for (i_3 in 0 until 65536) {
                val d_4 = (i_3 shr 10 and 0x3f) / 64.0 + 0.0078125
                val d_6 = 0.0625 + (i_3 shr 7 and 0x7) / 8.0
                val d_8 = (i_3 and 0x7f) / 128.0
                var d_10 = d_8
                var d_12 = d_8
                var d_14 = d_8
                if (d_6 != 0.0) {
                    val d_16: Double
                    if (d_8 < 0.5) {
                        d_16 = d_8 * (d_6 + 1.0)
                    } else {
                        d_16 = d_6 + d_8 - d_8 * d_6
                    }
                    val d_18 = d_8 * 2.0 - d_16
                    var d_20 = 0.3333333333333333 + d_4
                    if (d_20 > 1.0) {
                        --d_20
                    }
                    var d_24 = d_4 - 0.3333333333333333
                    if (d_24 < 0.0) {
                        ++d_24
                    }
                    if (d_20 * 6.0 < 1.0) {
                        d_10 = d_18 + (d_16 - d_18) * 6.0 * d_20
                    } else if (2.0 * d_20 < 1.0) {
                        d_10 = d_16
                    } else if (3.0 * d_20 < 2.0) {
                        d_10 = d_18 + 6.0 * (0.6666666666666666 - d_20) * (d_16 - d_18)
                    } else {
                        d_10 = d_18
                    }
                    if (6.0 * d_4 < 1.0) {
                        d_12 = d_18 + 6.0 * (d_16 - d_18) * d_4
                    } else if (d_4 * 2.0 < 1.0) {
                        d_12 = d_16
                    } else if (3.0 * d_4 < 2.0) {
                        d_12 = 6.0 * (0.6666666666666666 - d_4) * (d_16 - d_18) + d_18
                    } else {
                        d_12 = d_18
                    }
                    if (d_24 * 6.0 < 1.0) {
                        d_14 = d_24 * 6.0 * (d_16 - d_18) + d_18
                    } else if (d_24 * 2.0 < 1.0) {
                        d_14 = d_16
                    } else if (d_24 * 3.0 < 2.0) {
                        d_14 = 6.0 * (0.6666666666666666 - d_24) * (d_16 - d_18) + d_18
                    } else {
                        d_14 = d_18
                    }
                }
                d_10 = d_10.pow(d_1)
                d_12 = d_12.pow(d_1)
                d_14 = d_14.pow(d_1)
                val i_26 = (d_10 * 256.0).toInt()
                val i_17 = (d_12 * 256.0).toInt()
                val i_27 = (256.0 * d_14).toInt()
                val i_19 = i_27 + (i_17 shl 8) + (i_26 shl 16)
                hslToRgb!![i_3] = i_19 and 0xffffff
                val i_28 = i_26 + (i_17 shl 8) + (i_27 shl 16)
                unsignedHslToRgb!![i_3] = i_28
            }
        }
    }

    @JvmStatic
    fun initializeTables(hsvColorTable: Boolean, hslColorTable: Boolean) {
        if (hsvColorTable) {
            hsvToRgbUsers++
            calculateHsvToRgbTable()
        }
        if (hslColorTable) {
            hslToRgbUsers++
            calculateHslToRgbTable()
        }
    }

    @JvmStatic
    fun blendMaterialColor(materialColor: Int, i_1: Int): Int {
        val i_3 = i_1 ushr 24
        val i_4 = 255 - i_3
        val i_1_result = (i_3 * (i_1 and 0xff00ff) and -16711936 or (i_3 * (i_1 and 0xff00) and 0xff0000)) ushr 8
        return ((i_4 * (materialColor and 0xff00ff) and -16711936 or ((materialColor and 0xff00) * i_4 and 0xff0000)) ushr 8) + i_1_result
    }

    @JvmStatic
    fun mixColorsWithTransparency(a: Int, b: Int, alpha: Float): Int {
        return blendColorsWithAlpha(a, b, alpha.toInt())
    }

    @JvmStatic
    fun blendColorsWithAlpha(color: Int, colorB: Int, alpha: Int): Int {
        val transparency = 255 - alpha
        val colorB_result = ((colorB and 0xff00ff) * alpha and -16711936 or ((colorB and 0xff00) * alpha and 0xff0000)) ushr 8
        return ((transparency * (color and 0xff00ff) and -16711936 or ((color and 0xff00) * transparency and 0xff0000)) ushr 8) + colorB_result
    }

    @JvmStatic
    fun resetTables(hsvToRgb: Boolean, hslToRgb: Boolean) {
        if (hsvToRgb) {
            if (--hsvToRgbUsers == 0) {
                hsvToRgbTable = null
            }
        }
        if (hslToRgb) {
            if (--hslToRgbUsers == 0) {
                ColorUtil.hslToRgb = null
            }
        }
    }

    @JvmStatic
    fun rgbToHsl24(i_0: Int): Int {
        val d_2 = (i_0 shr 16 and 0xff) / 256.0
        val d_4 = (i_0 shr 8 and 0xff) / 256.0
        val d_6 = (i_0 and 0xff) / 256.0
        var d_8 = d_2
        if (d_4 < d_2) {
            d_8 = d_4
        }
        if (d_6 < d_8) {
            d_8 = d_6
        }
        var d_10 = d_2
        if (d_4 > d_2) {
            d_10 = d_4
        }
        if (d_6 > d_10) {
            d_10 = d_6
        }
        var d_12 = 0.0
        var d_14 = 0.0
        val d_16 = (d_10 + d_8) / 2.0
        if (d_10 != d_8) {
            if (d_16 < 0.5) {
                d_14 = (d_10 - d_8) / (d_8 + d_10)
            }
            if (d_16 >= 0.5) {
                d_14 = (d_10 - d_8) / (2.0 - d_10 - d_8)
            }
            if (d_2 == d_10) {
                d_12 = (d_4 - d_6) / (d_10 - d_8)
            } else if (d_4 == d_10) {
                d_12 = 2.0 + (d_6 - d_2) / (d_10 - d_8)
            } else if (d_10 == d_6) {
                d_12 = 4.0 + (d_2 - d_4) / (d_10 - d_8)
            }
        }
        d_12 /= 6.0
        val i_18 = (256.0 * d_12).toInt()
        var i_19 = (d_14 * 256.0).toInt()
        var i_20 = (d_16 * 256.0).toInt()
        if (i_19 < 0) {
            i_19 = 0
        } else if (i_19 > 255) {
            i_19 = 255
        }
        if (i_20 < 0) {
            i_20 = 0
        } else if (i_20 > 255) {
            i_20 = 255
        }
        if (i_20 > 243) {
            i_19 = i_19 shr 4
        } else if (i_20 > 217) {
            i_19 = i_19 shr 3
        } else if (i_20 > 192) {
            i_19 = i_19 shr 2
        } else if (i_20 > 179) {
            i_19 = i_19 shr 1
        }
        return ((i_18 and 0xff) shr 2 shl 10) + (i_20 shr 1) + (i_19 shr 5 shl 7)
    }

    @JvmStatic
    fun adjustHsv(i_0: Int): Short {
        val i_2 = i_0 shr 10 and 0x3f
        val i_3 = i_0 shr 3 and 0x70
        val i_4 = i_0 and 0x7f
        val i_3_result = if (i_4 <= 64) i_4 * i_3 shr 7 else i_3 * (127 - i_4) shr 7
        val i_5 = i_3_result + i_4
        val i_6: Int
        if (i_5 != 0) {
            i_6 = (i_3_result shl 8) / i_5
        } else {
            i_6 = i_3_result shl 1
        }

        return (i_2 shl 10 or (i_6 shr 4 shl 7) or i_5).toShort()
    }

    @JvmStatic
    fun repackHsl(hsl: Int, light: Int): Int {
        var Light = light
        Light = (hsl and 0x7f) * Light shr 7
        if (Light < 2) {
            Light = 2
        } else if (Light > 126) {
            Light = 126
        }

        return (hsl and 0xff80) + Light
    }

    @JvmStatic
    fun brighten(color: Int, multiplier: Int): Int {
        var red = (color and 0xff0000) * multiplier shr 23
        if (red < 2) {
            red = 2
        } else if (red > 253) {
            red = 253
        }

        var green = (color and 0xff00) * multiplier shr 15
        if (green < 2) {
            green = 2
        } else if (green > 253) {
            green = 253
        }

        var blue = (color and 0xff) * multiplier shr 7
        if (blue < 2) {
            blue = 2
        } else if (blue > 253) {
            blue = 253
        }

        return red shl 16 or (green shl 8) or blue
    }

    @JvmStatic
    fun method14677(i_0: Int, i_1: Int): Int {
        var i_11 = i_1
        if (i_0 == -1) {
            return 12345678
        } else {
            i_11 = (i_0 and 0x7f) * i_11 shr 7
            if (i_11 < 2) {
                i_11 = 2
            } else if (i_11 > 126) {
                i_11 = 126
            }

            return (i_0 and 0xff80) + i_11
        }
    }

    @JvmStatic
    fun method4162(colorHsl: Int, lightness: Int): Int {
        var lightness = lightness
        if (colorHsl == -2) {
            return 12345678
        } else if (colorHsl == -1) {
            if (lightness < 2) {
                lightness = 2
            } else if (lightness > 126) {
                lightness = 126
            }
            return lightness
        } else {
            lightness = (colorHsl and 0x7f) * lightness shr 7
            if (lightness < 2) {
                lightness = 2
            } else if (lightness > 126) {
                lightness = 126
            }
            return (colorHsl and 0xff80) + lightness
        }
    }
}
