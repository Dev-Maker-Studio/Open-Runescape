package com.jagex.game.runetek5.utils

import com.jagex.game.runetek5.config.flo.FloType
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import kotlin.math.pow

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
    fun blend(colorA: Int, colorB: Int, blendFactor: Int): Int {
        if (colorA == colorB) {
            return colorA
        } else {
            val inverseFactor = 128 - blendFactor
            val blendedLightness = inverseFactor * (colorA and 0x7f) + (colorB and 0x7f) * blendFactor shr 7
            val blendedSaturation = inverseFactor * (colorA and 0x380) + blendFactor * (colorB and 0x380) shr 7
            val blendedHue = (colorB and 0xfc00) * blendFactor + inverseFactor * (colorA and 0xfc00) shr 7
            return blendedHue and 0xfc00 or (blendedSaturation and 0x380) or (blendedLightness and 0x7f)
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
            val gammaCorrection = 0.7 + (Math.random() * 0.03 - 0.015)
            var tableIndex = 0
            for (hueSatIndex in 0 until 512) {
                val hueAngle = 360.0f * ((hueSatIndex shr 3) / 64.0f + 0.0078125f)
                val saturation = (hueSatIndex and 0x7) / 8.0f + 0.0625f
                for (valueIndex in 0 until 128) {
                    val value = valueIndex / 128.0f
                    var red = 0.0f
                    var green = 0.0f
                    var blue = 0.0f
                    val hueSegment = hueAngle / 60.0f
                    val hueSegmentInt = hueSegment.toInt()
                    val hueRegion = hueSegmentInt % 6
                    val fractionalHue = hueSegment - hueSegmentInt
                    val minComponent = value * (1.0f - saturation)
                    val fallingComponent = value * (1.0f - saturation * fractionalHue)
                    val risingComponent = (1.0f - saturation * (1.0f - fractionalHue)) * value
                    when (hueRegion) {
                        0 -> {
                            red = value
                            green = risingComponent
                            blue = minComponent
                        }
                        1 -> {
                            red = fallingComponent
                            green = value
                            blue = minComponent
                        }
                        2 -> {
                            red = minComponent
                            green = value
                            blue = risingComponent
                        }
                        3 -> {
                            red = minComponent
                            green = fallingComponent
                            blue = value
                        }
                        4 -> {
                            red = risingComponent
                            green = minComponent
                            blue = value
                        }
                        5 -> {
                            red = value
                            green = minComponent
                            blue = fallingComponent
                        }
                    }
                    red = red.toDouble().pow(gammaCorrection).toFloat()
                    green = green.toDouble().pow(gammaCorrection).toFloat()
                    blue = blue.toDouble().pow(gammaCorrection).toFloat()
                    val redInt = (red * 256.0f).toInt()
                    val greenInt = (256.0f * green).toInt()
                    val blueInt = (256.0f * blue).toInt()
                    val rgb = blueInt + (greenInt shl 8) + (redInt shl 16) + 0xffffff.inv()
                    hsvToRgbTable!![tableIndex++] = rgb
                }
            }
        }
    }

    @JvmStatic
    fun calculateHslToRgbTable() {
        if (hslToRgb == null) {
            hslToRgb = IntArray(65536)
            unsignedHslToRgb = IntArray(65536)
            val gammaCorrection = 0.7 + (Math.random() * 0.03 - 0.015)
            for (hslIndex in 0 until 65536) {
                val hue = (hslIndex shr 10 and 0x3f) / 64.0 + 0.0078125
                val saturation = 0.0625 + (hslIndex shr 7 and 0x7) / 8.0
                val lightness = (hslIndex and 0x7f) / 128.0
                var red = lightness
                var green = lightness
                var blue = lightness
                if (saturation != 0.0) {
                    val chroma: Double
                    if (lightness < 0.5) {
                        chroma = lightness * (saturation + 1.0)
                    } else {
                        chroma = saturation + lightness - lightness * saturation
                    }
                    val minComponent = lightness * 2.0 - chroma
                    var hueRed = 0.3333333333333333 + hue
                    if (hueRed > 1.0) {
                        --hueRed
                    }
                    var hueBlue = hue - 0.3333333333333333
                    if (hueBlue < 0.0) {
                        ++hueBlue
                    }
                    if (hueRed * 6.0 < 1.0) {
                        red = minComponent + (chroma - minComponent) * 6.0 * hueRed
                    } else if (2.0 * hueRed < 1.0) {
                        red = chroma
                    } else if (3.0 * hueRed < 2.0) {
                        red = minComponent + 6.0 * (0.6666666666666666 - hueRed) * (chroma - minComponent)
                    } else {
                        red = minComponent
                    }
                    if (6.0 * hue < 1.0) {
                        green = minComponent + 6.0 * (chroma - minComponent) * hue
                    } else if (hue * 2.0 < 1.0) {
                        green = chroma
                    } else if (3.0 * hue < 2.0) {
                        green = 6.0 * (0.6666666666666666 - hue) * (chroma - minComponent) + minComponent
                    } else {
                        green = minComponent
                    }
                    if (hueBlue * 6.0 < 1.0) {
                        blue = hueBlue * 6.0 * (chroma - minComponent) + minComponent
                    } else if (hueBlue * 2.0 < 1.0) {
                        blue = chroma
                    } else if (hueBlue * 3.0 < 2.0) {
                        blue = 6.0 * (0.6666666666666666 - hueBlue) * (chroma - minComponent) + minComponent
                    } else {
                        blue = minComponent
                    }
                }
                red = red.pow(gammaCorrection)
                green = green.pow(gammaCorrection)
                blue = blue.pow(gammaCorrection)
                val redInt = (red * 256.0).toInt()
                val greenInt = (green * 256.0).toInt()
                val blueInt = (256.0 * blue).toInt()
                val rgbBgr = blueInt + (greenInt shl 8) + (redInt shl 16)
                hslToRgb!![hslIndex] = rgbBgr and 0xffffff
                val rgbNormal = redInt + (greenInt shl 8) + (blueInt shl 16)
                unsignedHslToRgb!![hslIndex] = rgbNormal
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
    fun blendMaterialColor(materialColor: Int, overlayColor: Int): Int {
        val overlayAlpha = overlayColor ushr 24
        val materialAlpha = 255 - overlayAlpha
        val overlayResult = (overlayAlpha * (overlayColor and 0xff00ff) and -16711936 or (overlayAlpha * (overlayColor and 0xff00) and 0xff0000)) ushr 8
        return ((materialAlpha * (materialColor and 0xff00ff) and -16711936 or ((materialColor and 0xff00) * materialAlpha and 0xff0000)) ushr 8) + overlayResult
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
    fun rgbToHsl24(rgb: Int): Int {
        val red = (rgb shr 16 and 0xff) / 256.0
        val green = (rgb shr 8 and 0xff) / 256.0
        val blue = (rgb and 0xff) / 256.0
        var minChannel = red
        if (green < red) {
            minChannel = green
        }
        if (blue < minChannel) {
            minChannel = blue
        }
        var maxChannel = red
        if (green > red) {
            maxChannel = green
        }
        if (blue > maxChannel) {
            maxChannel = blue
        }
        var hue = 0.0
        var saturation = 0.0
        val lightness = (maxChannel + minChannel) / 2.0
        if (maxChannel != minChannel) {
            if (lightness < 0.5) {
                saturation = (maxChannel - minChannel) / (minChannel + maxChannel)
            }
            if (lightness >= 0.5) {
                saturation = (maxChannel - minChannel) / (2.0 - maxChannel - minChannel)
            }
            if (red == maxChannel) {
                hue = (green - blue) / (maxChannel - minChannel)
            } else if (green == maxChannel) {
                hue = 2.0 + (blue - red) / (maxChannel - minChannel)
            } else if (maxChannel == blue) {
                hue = 4.0 + (red - green) / (maxChannel - minChannel)
            }
        }
        hue /= 6.0
        val hueInt = (256.0 * hue).toInt()
        var saturationInt = (saturation * 256.0).toInt()
        var lightnessInt = (lightness * 256.0).toInt()
        if (saturationInt < 0) {
            saturationInt = 0
        } else if (saturationInt > 255) {
            saturationInt = 255
        }
        if (lightnessInt < 0) {
            lightnessInt = 0
        } else if (lightnessInt > 255) {
            lightnessInt = 255
        }
        if (lightnessInt > 243) {
            saturationInt = saturationInt shr 4
        } else if (lightnessInt > 217) {
            saturationInt = saturationInt shr 3
        } else if (lightnessInt > 192) {
            saturationInt = saturationInt shr 2
        } else if (lightnessInt > 179) {
            saturationInt = saturationInt shr 1
        }
        return ((hueInt and 0xff) shr 2 shl 10) + (lightnessInt shr 1) + (saturationInt shr 5 shl 7)
    }

    @JvmStatic
    fun adjustHsv(hsvPacked: Int): Short {
        val hue = hsvPacked shr 10 and 0x3f
        val saturation = hsvPacked shr 3 and 0x70
        val value = hsvPacked and 0x7f
        val chromaComponent = if (value <= 64) value * saturation shr 7 else saturation * (127 - value) shr 7
        val brightness = chromaComponent + value
        val newSaturation: Int
        if (brightness != 0) {
            newSaturation = (chromaComponent shl 8) / brightness
        } else {
            newSaturation = chromaComponent shl 1
        }

        return (hue shl 10 or (newSaturation shr 4 shl 7) or brightness).toShort()
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
    fun repackHslSafe(hsl: Int, light: Int): Int {
        var adjustedLight = light
        if (hsl == -1) {
            return 12345678
        } else {
            adjustedLight = (hsl and 0x7f) * adjustedLight shr 7
            if (adjustedLight < 2) {
                adjustedLight = 2
            } else if (adjustedLight > 126) {
                adjustedLight = 126
            }

            return (hsl and 0xff80) + adjustedLight
        }
    }

    @JvmStatic
    fun repackHslWithSentinel(colorHsl: Int, lightness: Int): Int {
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
