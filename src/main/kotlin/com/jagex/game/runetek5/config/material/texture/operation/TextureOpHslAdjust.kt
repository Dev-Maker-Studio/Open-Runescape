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

    @JvmField var anInt9922: Int = 0
    @JvmField var anInt9925: Int = 0
    @JvmField var anInt9926: Int = 0
    @JvmField var anInt9918: Int = 0
    @JvmField var anInt9919: Int = 0
    @JvmField var anInt9923: Int = 0
    @JvmField var anInt9924: Int = 0
    @JvmField var anInt9921: Int = 0
    @JvmField var anInt9927: Int = 0

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
                method15400(ints_5[i_11], ints_6[i_11], ints_7[i_11])
                anInt9922 += anInt9918
                anInt9925 += anInt9919

                anInt9926 += anInt9923
                while (anInt9922 < 0) {
                    anInt9922 += 4096
                }

                while (anInt9922 > 4096) {
                    anInt9922 -= 4096
                }

                if (anInt9925 < 0) {
                    anInt9925 = 0
                }

                if (anInt9925 > 4096) {
                    anInt9925 = 4096
                }

                if (anInt9926 < 0) {
                    anInt9926 = 0
                }

                if (anInt9926 > 4096) {
                    anInt9926 = 4096
                }

                method15401(anInt9922, anInt9925, anInt9926)
                ints_8[i_11] = anInt9927
                ints_9[i_11] = anInt9921
                ints_10[i_11] = anInt9924
            }
        }

        return ints_3
    }

    fun method15400(i_1: Int, i_2: Int, i_3: Int) {
        var i_5 = max(i_1, i_2)
        i_5 = max(i_3, i_5)
        var i_6 = min(i_1, i_2)
        i_6 = min(i_3, i_6)
        val i_7 = i_5 - i_6
        anInt9926 = (i_6 + i_5) / 2

        if (anInt9926 > 0 && anInt9926 < 4096) {
            anInt9925 = (i_7 shl 12) / (if (anInt9926 <= 2048) anInt9926 * 2 else 8192 - anInt9926 * 2)
        } else {
            anInt9925 = 0
        }

        if (i_7 > 0) {
            val i_8 = (i_5 - i_1 shl 12) / i_7
            val i_9 = (i_5 - i_2 shl 12) / i_7
            val i_10 = (i_5 - i_3 shl 12) / i_7

            anInt9922 = when {
                i_5 == i_1 -> if (i_6 == i_2) i_10 + 20480 else 4096 - i_9
                i_5 == i_2 -> if (i_3 == i_6) i_8 + 4096 else 12288 - i_10
                else -> if (i_6 == i_1) i_9 + 12288 else 20480 - i_8
            }

            anInt9922 /= 6
        } else {
            anInt9922 = 0
        }
    }

    fun method15401(i_1: Int, i_2: Int, i_3: Int) {
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
                    anInt9927 = i_5
                    anInt9921 = i_11
                    anInt9924 = i_6
                }
                1 -> {
                    anInt9927 = i_12
                    anInt9921 = i_5
                    anInt9924 = i_6
                }
                2 -> {
                    anInt9927 = i_6
                    anInt9921 = i_5
                    anInt9924 = i_11
                }
                3 -> {
                    anInt9927 = i_6
                    anInt9921 = i_12
                    anInt9924 = i_5
                }
                4 -> {
                    anInt9927 = i_11
                    anInt9921 = i_6
                    anInt9924 = i_5
                }
                5 -> {
                    anInt9927 = i_5
                    anInt9921 = i_6
                    anInt9924 = i_12
                }
            }
        } else {
            anInt9924 = i_3 * -680551435
            anInt9927 = i_3
            anInt9921 = i_3
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> anInt9918 = buffer.readShort()
            1 -> anInt9919 = (buffer.readByte().toInt() shl 12) / 100
            2 -> anInt9923 = (buffer.readByte().toInt() shl 12) / 100
        }
    }
}
