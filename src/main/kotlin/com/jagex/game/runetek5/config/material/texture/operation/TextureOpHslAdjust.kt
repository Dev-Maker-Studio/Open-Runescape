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

    override fun getColorOutput(i_1: Int): Array<IntArray> {
        val ints_3 = colorImageCache.getPalette(i_1)
        if (colorImageCache.dirty) {
            val ints_4 = getChildColorOutput(0, i_1)
            val ints_5 = ints_4[0]
            val ints_6 = ints_4[1]
            val ints_7 = ints_4[2]
            val ints_8 = ints_3[0]
            val ints_9 = ints_3[1]
            val ints_10 = ints_3[2]

            for (i_11 in 0 until TextureRaster.currentWidth) {
                rgbToHsl(ints_5[i_11], ints_6[i_11], ints_7[i_11])
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
                ints_8[i_11] = outputRed
                ints_9[i_11] = outputGreen
                ints_10[i_11] = outputBlue
            }
        }

        return ints_3
    }

    fun rgbToHsl(i_1: Int, i_2: Int, i_3: Int) {
        var i_5 = max(i_1, i_2)
        i_5 = max(i_3, i_5)
        var i_6 = min(i_1, i_2)
        i_6 = min(i_3, i_6)
        val i_7 = i_5 - i_6
        lightness = (i_6 + i_5) / 2

        if (lightness > 0 && lightness < 4096) {
            saturation = (i_7 shl 12) / (if (lightness <= 2048) lightness * 2 else 8192 - lightness * 2)
        } else {
            saturation = 0
        }

        if (i_7 > 0) {
            val i_8 = (i_5 - i_1 shl 12) / i_7
            val i_9 = (i_5 - i_2 shl 12) / i_7
            val i_10 = (i_5 - i_3 shl 12) / i_7

            hue = when {
                i_5 == i_1 -> if (i_6 == i_2) i_10 + 20480 else 4096 - i_9
                i_5 == i_2 -> if (i_3 == i_6) i_8 + 4096 else 12288 - i_10
                else -> if (i_6 == i_1) i_9 + 12288 else 20480 - i_8
            }

            hue /= 6
        } else {
            hue = 0
        }
    }

    fun hslToRgb(i_1: Int, i_2: Int, i_3: Int) {
        val i_5 = if (i_3 <= 2048) i_3 * (i_2 + 4096) shr 12 else i_3 + i_2 - (i_3 * i_2 shr 12)

        if (i_5 > 0) {
            val adjustedHue = i_1 * 6
            val i_6 = i_3 + i_3 - i_5
            val i_7 = (i_5 - i_6 shl 12) / i_5
            val i_8 = adjustedHue shr 12
            val i_9 = adjustedHue - (i_8 shl 12)
            var i_10 = i_7 * i_5 shr 12
            i_10 = i_9 * i_10 shr 12
            val i_11 = i_6 + i_10
            val i_12 = i_5 - i_10

            when (i_8) {
                0 -> {
                    outputRed = i_5
                    outputGreen = i_11
                    outputBlue = i_6
                }
                1 -> {
                    outputRed = i_12
                    outputGreen = i_5
                    outputBlue = i_6
                }
                2 -> {
                    outputRed = i_6
                    outputGreen = i_5
                    outputBlue = i_11
                }
                3 -> {
                    outputRed = i_6
                    outputGreen = i_12
                    outputBlue = i_5
                }
                4 -> {
                    outputRed = i_11
                    outputGreen = i_6
                    outputBlue = i_5
                }
                5 -> {
                    outputRed = i_5
                    outputGreen = i_6
                    outputBlue = i_12
                }
            }
        } else {
            outputBlue = i_3 * -680551435
            outputRed = i_3
            outputGreen = i_3
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
