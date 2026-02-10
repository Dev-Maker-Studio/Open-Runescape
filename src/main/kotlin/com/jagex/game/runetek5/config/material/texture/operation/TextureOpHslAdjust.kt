package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import kotlin.math.max
import kotlin.math.min

/**
 * Texture operation that adjusts HSL (Hue, Saturation, Lightness) values.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpHslAdjust : TextureOperation(1, false) {

    @JvmField var hue: Int = 0
    @JvmField var saturation: Int = 0
    @JvmField var lightness: Int = 0
    @JvmField var hueAdjust: Int = 0
    @JvmField var saturationAdjust: Int = 0
    @JvmField var lightnessAdjust: Int = 0
    @JvmField var outputBlue: Int = 0
    @JvmField var outputGreen: Int = 0
    @JvmField var outputRed: Int = 0

    override fun getColorOutput(y: Int): Array<IntArray> {
        val palette = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val childColors = getChildColorOutput(0, y)
            val inRed = childColors[0]
            val inGreen = childColors[1]
            val inBlue = childColors[2]
            val outRedPixels = palette[0]
            val outGreenPixels = palette[1]
            val outBluePixels = palette[2]

            for (x in 0 until TextureRaster.currentWidth) {
                rgbToHsl(inRed[x], inGreen[x], inBlue[x])
                hue += hueAdjust
                saturation += saturationAdjust

                lightness += lightnessAdjust
                while (hue < 0) {
                    hue += 4096
                }

                while (hue > 4096) {
                    hue -= 4096
                }

                if (saturation < 0) {
                    saturation = 0
                }

                if (saturation > 4096) {
                    saturation = 4096
                }

                if (lightness < 0) {
                    lightness = 0
                }

                if (lightness > 4096) {
                    lightness = 4096
                }

                hslToRgb(hue, saturation, lightness)
                outRedPixels[x] = outputRed
                outGreenPixels[x] = outputGreen
                outBluePixels[x] = outputBlue
            }
        }

        return palette
    }

    fun rgbToHsl(red: Int, green: Int, blue: Int) {
        var maxChannel = max(red, green)
        maxChannel = max(blue, maxChannel)
        var minChannel = min(red, green)
        minChannel = min(blue, minChannel)
        val chroma = maxChannel - minChannel
        lightness = (minChannel + maxChannel) / 2

        if (lightness > 0 && lightness < 4096) {
            saturation = (chroma shl 12) / (if (lightness <= 2048) lightness * 2 else 8192 - lightness * 2)
        } else {
            saturation = 0
        }

        if (chroma > 0) {
            val distRed = (maxChannel - red shl 12) / chroma
            val distGreen = (maxChannel - green shl 12) / chroma
            val distBlue = (maxChannel - blue shl 12) / chroma

            hue = when {
                maxChannel == red -> if (minChannel == green) distBlue + 20480 else 4096 - distGreen
                maxChannel == green -> if (blue == minChannel) distRed + 4096 else 12288 - distBlue
                else -> if (minChannel == red) distGreen + 12288 else 20480 - distRed
            }

            hue /= 6
        } else {
            hue = 0
        }
    }

    fun hslToRgb(h: Int, s: Int, l: Int) {
        val chroma = if (l <= 2048) l * (s + 4096) shr 12 else l + s - (l * s shr 12)

        if (chroma > 0) {
            val adjustedHue = h * 6
            val minComponent = l + l - chroma
            val satRange = (chroma - minComponent shl 12) / chroma
            val hueRegion = adjustedHue shr 12
            val hueFraction = adjustedHue - (hueRegion shl 12)
            var hueOffset = satRange * chroma shr 12
            hueOffset = hueFraction * hueOffset shr 12
            val ascending = minComponent + hueOffset
            val descending = chroma - hueOffset

            when (hueRegion) {
                0 -> {
                    outputRed = chroma
                    outputGreen = ascending
                    outputBlue = minComponent
                }
                1 -> {
                    outputRed = descending
                    outputGreen = chroma
                    outputBlue = minComponent
                }
                2 -> {
                    outputRed = minComponent
                    outputGreen = chroma
                    outputBlue = ascending
                }
                3 -> {
                    outputRed = minComponent
                    outputGreen = descending
                    outputBlue = chroma
                }
                4 -> {
                    outputRed = ascending
                    outputGreen = minComponent
                    outputBlue = chroma
                }
                5 -> {
                    outputRed = chroma
                    outputGreen = minComponent
                    outputBlue = descending
                }
            }
        } else {
            outputBlue = l * -680551435
            outputRed = l
            outputGreen = l
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> hueAdjust = buffer.readShort()
            1 -> saturationAdjust = (buffer.readByte().toInt() shl 12) / 100
            2 -> lightnessAdjust = (buffer.readByte().toInt() shl 12) / 100
        }
    }
}
