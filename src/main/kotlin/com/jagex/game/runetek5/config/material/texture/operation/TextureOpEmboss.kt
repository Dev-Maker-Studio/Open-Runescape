package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpEmboss : TextureOperation(1, true) {

    @JvmField var embossDepth = 4096
    @JvmField var lightAzimuth = 3216
    @JvmField var lightElevation = 3216
    @JvmField val lightDirection = IntArray(3)

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val i_4 = embossDepth * TextureRaster.anInt3678 shr 12
            val ints_5 = getChildMonochromeOutput(0, i_1 - 1 and TextureRaster.columnEnd)
            val ints_6 = getChildMonochromeOutput(0, i_1)
            val ints_7 = getChildMonochromeOutput(0, i_1 + 1 and TextureRaster.columnEnd)

            for (i_8 in 0 until TextureRaster.currentWidth) {
                val i_9 = (ints_7[i_8] - ints_5[i_8]) * i_4 shr 12
                val i_10 = i_4 * (ints_6[i_8 - 1 and TextureRaster.rowEnd] - ints_6[i_8 + 1 and TextureRaster.rowEnd]) shr 12
                var i_11 = i_10 shr 4
                var i_12 = i_9 shr 4
                if (i_11 < 0) {
                    i_11 = -i_11
                }

                if (i_11 > 255) {
                    i_11 = 255
                }

                if (i_12 < 0) {
                    i_12 = -i_12
                }

                if (i_12 > 255) {
                    i_12 = 255
                }

                val i_13 = normalizeTable[i_11 + (i_12 * (i_12 + 1) shr 1)].toInt() and 0xff
                val i_14 = i_10 * i_13 shr 8
                val i_15 = i_9 * i_13 shr 8
                val i_16 = i_13 * 4096 shr 8
                val i_14_final = i_14 * lightDirection[0] shr 12
                val i_15_final = lightDirection[1] * i_15 shr 12
                val i_16_final = i_16 * lightDirection[2] shr 12
                ints_3[i_8] = i_16_final + i_15_final + i_14_final
            }
        }

        return ints_3
    }

    override fun postDecode() {
        computeLightDirection()
    }

    fun computeLightDirection() {
        val d_2 = cos(lightElevation / 4096.0)
        lightDirection[0] = (sin(lightAzimuth / 4096.0) * d_2 * 4096.0).toInt()
        lightDirection[1] = (cos(lightAzimuth / 4096.0) * d_2 * 4096.0).toInt()
        lightDirection[2] = (sin(lightElevation / 4096.0) * 4096.0).toInt()
        val i_4 = lightDirection[0] * lightDirection[0] shr 12
        val i_5 = lightDirection[1] * lightDirection[1] shr 12
        val i_6 = lightDirection[2] * lightDirection[2] shr 12
        val i_7 = (sqrt((i_4 + i_5 + i_6 shr 12).toDouble()) * 4096.0).toInt()
        if (i_7 != 0) {
            lightDirection[0] = (lightDirection[0] shl 12) / i_7
            lightDirection[1] = (lightDirection[1] shl 12) / i_7
            lightDirection[2] = (lightDirection[2] shl 12) / i_7
        }
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> embossDepth = rsbytebuffer2.readUShort()
            1 -> lightAzimuth = rsbytebuffer2.readUShort()
            2 -> lightElevation = rsbytebuffer2.readUShort()
        }
    }

    companion object {
        @JvmField val normalizeTable: ByteArray

        init {
            var i_0 = 0
            normalizeTable = ByteArray(32896)

            for (i_1 in 0..255) {
                for (i_2 in 0..i_1) {
                    normalizeTable[i_0++] = (255.0 / sqrt((i_2 * i_2 + i_1 * i_1 + 65535) / 65535.0)).toInt().toByte()
                }
            }
        }
    }
}
