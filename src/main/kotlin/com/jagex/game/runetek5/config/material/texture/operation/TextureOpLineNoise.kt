package com.jagex.game.runetek5.config.material.texture.operation

import java.util.Random

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.staticmethods.StaticMethods

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpLineNoise : TextureOperation(0, true) {

    @JvmField var randomSeed = 0
    @JvmField var lineCount = 2000
    @JvmField var lineLength = 16
    @JvmField var angleOffset = 0
    @JvmField var angleSpread = 4096

    override fun postDecode() {
        TextureRaster.caculateTable()
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> randomSeed = rsbytebuffer2.readUByte()
            1 -> lineCount = rsbytebuffer2.readUShort()
            2 -> lineLength = rsbytebuffer2.readUByte()
            3 -> angleOffset = rsbytebuffer2.readUShort()
            4 -> angleSpread = rsbytebuffer2.readUShort()
        }
    }

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val i_4 = angleSpread shr 1
            val ints_5 = monochromeImageCache.method5722()
            val random6 = Random(randomSeed.toLong())

            for (i_7 in 0 until lineCount) {
                var i_8 = if (angleSpread > 0) angleOffset + (StaticMethods.boundedRandom(random6, angleSpread) - i_4) else angleOffset
                i_8 = i_8 shr 4 and 0xff
                val i_9 = StaticMethods.boundedRandom(random6, TextureRaster.currentWidth)
                val i_10 = StaticMethods.boundedRandom(random6, TextureRaster.currentHeight)
                val i_11 = i_9 + (TextureRaster.PALETTE_COS!![i_8] * lineLength shr 12)
                val i_12 = i_10 + (TextureRaster.PALETTE_SIN!![i_8] * lineLength shr 12)
                val i_13 = i_12 - i_10
                val i_14 = i_11 - i_9
                if (i_14 != 0 || i_13 != 0) {
                    var i_13_abs = if (i_13 < 0) -i_13 else i_13
                    var i_14_abs = if (i_14 < 0) -i_14 else i_14

                    val bool15 = i_13_abs > i_14_abs
                    var i_9_swap = i_9
                    var i_10_swap = i_10
                    var i_11_swap = i_11
                    var i_12_swap = i_12
                    if (bool15) {
                        i_9_swap = i_10
                        i_10_swap = i_9
                        i_11_swap = i_12
                        i_12_swap = i_11
                    }

                    if (i_9_swap > i_11_swap) {
                        val temp1 = i_9_swap
                        val temp2 = i_10_swap
                        i_9_swap = i_11_swap
                        i_11_swap = temp1
                        i_10_swap = i_12_swap
                        i_12_swap = temp2
                    }

                    var i_16 = i_10_swap
                    val i_17 = i_11_swap - i_9_swap
                    var i_18 = i_12_swap - i_10_swap
                    var i_19 = -i_17 / 2
                    val i_20 = 2048 / i_17
                    val i_21 = 1024 - (StaticMethods.boundedRandom(random6, 4096) shr 2)
                    val i_22 = if (i_10_swap < i_12_swap) 1 else -1
                    if (i_18 < 0) {
                        i_18 = -i_18
                    }

                    for (i_23 in i_9_swap until i_11_swap) {
                        val i_24 = i_21 + i_20 * (i_23 - i_9_swap) + 1024
                        val i_25 = i_23 and TextureRaster.rowEnd
                        val i_26 = i_16 and TextureRaster.columnEnd
                        if (bool15) {
                            ints_5[i_26]!![i_25] = i_24
                        } else {
                            ints_5[i_25]!![i_26] = i_24
                        }

                        i_19 += i_18
                        if (i_19 > 0) {
                            i_19 -= i_17
                            i_16 += i_22
                        }
                    }
                }
            }
        }

        return ints_3
    }
}
