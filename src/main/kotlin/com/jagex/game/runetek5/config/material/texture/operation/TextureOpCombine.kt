package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpCombine : TextureOperation(2, false) {

    @JvmField
    var anInt9895: Int = 6

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = super.monochromeImageCache.getPalette(i_1)
        if (super.monochromeImageCache.dirty) {
            val ints_4 = getChildMonochromeOutput(0, i_1)
            val ints_5 = getChildMonochromeOutput(1, i_1)
            var i_6: Int
            var i_7: Int
            var i_8: Int
            when (this.anInt9895) {
                1 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        ints_3[i_6] = ints_5[i_6] + ints_4[i_6]
                        i_6++
                    }
                    return ints_3
                }
                2 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        ints_3[i_6] = ints_4[i_6] - ints_5[i_6]
                        i_6++
                    }
                    return ints_3
                }
                3 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        ints_3[i_6] = ints_4[i_6] * ints_5[i_6] shr 12
                        i_6++
                    }
                    return ints_3
                }
                4 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        i_7 = ints_5[i_6]
                        ints_3[i_6] = if (i_7 != 0) (ints_4[i_6] shl 12) / i_7 else 4096
                        i_6++
                    }
                    return ints_3
                }
                5 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        ints_3[i_6] = 4096 - ((4096 - ints_4[i_6]) * (4096 - ints_5[i_6]) shr 12)
                        i_6++
                    }
                    return ints_3
                }
                6 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        i_7 = ints_5[i_6]
                        ints_3[i_6] = if (i_7 < 2048) i_7 * ints_4[i_6] shr 11 else 4096 - ((4096 - ints_4[i_6]) * (4096 - i_7) shr 11)
                        i_6++
                    }
                    return ints_3
                }
                7 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        i_7 = ints_4[i_6]
                        ints_3[i_6] = if (i_7 == 4096) 4096 else (ints_5[i_6] shl 12) / (4096 - i_7)
                        i_6++
                    }
                    return ints_3
                }
                8 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        i_7 = ints_4[i_6]
                        ints_3[i_6] = if (i_7 == 0) 0 else 4096 - (4096 - ints_5[i_6] shl 12) / i_7
                        i_6++
                    }
                    return ints_3
                }
                9 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        i_7 = ints_4[i_6]
                        i_8 = ints_5[i_6]
                        ints_3[i_6] = Math.min(i_7, i_8)
                        i_6++
                    }
                    return ints_3
                }
                10 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        i_7 = ints_4[i_6]
                        i_8 = ints_5[i_6]
                        ints_3[i_6] = Math.max(i_7, i_8)
                        i_6++
                    }
                    return ints_3
                }
                11 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        i_7 = ints_4[i_6]
                        i_8 = ints_5[i_6]
                        ints_3[i_6] = if (i_7 > i_8) i_7 - i_8 else i_8 - i_7
                        i_6++
                    }
                    return ints_3
                }
                12 -> {
                    i_6 = 0
                    while (i_6 < TextureRaster.currentWidth) {
                        i_7 = ints_4[i_6]
                        i_8 = ints_5[i_6]
                        ints_3[i_6] = i_8 + i_7 - (i_7 * i_8 shr 11)
                        i_6++
                    }
                }
            }
        }
        return ints_3
    }

    override fun getColorOutput(i_1: Int): Array<IntArray> {
        val ints_3 = super.colorImageCache.getPalette(i_1)
        if (super.colorImageCache.dirty) {
            val ints_4 = getChildColorOutput(0, i_1)
            val ints_5 = getChildColorOutput(1, i_1)
            val ints_6 = ints_3[0]
            val ints_7 = ints_3[1]
            val ints_8 = ints_3[2]
            val ints_9 = ints_4[0]
            val ints_10 = ints_4[1]
            val ints_11 = ints_4[2]
            val ints_12 = ints_5[0]
            val ints_13 = ints_5[1]
            val ints_14 = ints_5[2]
            var i_15: Int
            var i_16: Int
            var i_17: Int
            var i_18: Int
            var i_19: Int
            var i_20: Int
            var i_21: Int
            when (this.anInt9895) {
                1 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        ints_6[i_15] = ints_9[i_15] + ints_12[i_15]
                        ints_7[i_15] = ints_10[i_15] + ints_13[i_15]
                        ints_8[i_15] = ints_14[i_15] + ints_11[i_15]
                        i_15++
                    }
                    return ints_3
                }
                2 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        ints_6[i_15] = ints_9[i_15] - ints_12[i_15]
                        ints_7[i_15] = ints_10[i_15] - ints_13[i_15]
                        ints_8[i_15] = ints_11[i_15] - ints_14[i_15]
                        i_15++
                    }
                    return ints_3
                }
                3 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        ints_6[i_15] = ints_12[i_15] * ints_9[i_15] shr 12
                        ints_7[i_15] = ints_10[i_15] * ints_13[i_15] shr 12
                        ints_8[i_15] = ints_11[i_15] * ints_14[i_15] shr 12
                        i_15++
                    }
                    return ints_3
                }
                4 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        i_16 = ints_12[i_15]
                        i_17 = ints_13[i_15]
                        i_18 = ints_14[i_15]
                        ints_6[i_15] = if (i_16 != 0) (ints_9[i_15] shl 12) / i_16 else 4096
                        ints_7[i_15] = if (i_17 != 0) (ints_10[i_15] shl 12) / i_17 else 4096
                        ints_8[i_15] = if (i_18 != 0) (ints_11[i_15] shl 12) / i_18 else 4096
                        i_15++
                    }
                    return ints_3
                }
                5 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        ints_6[i_15] = 4096 - ((4096 - ints_12[i_15]) * (4096 - ints_9[i_15]) shr 12)
                        ints_7[i_15] = 4096 - ((4096 - ints_10[i_15]) * (4096 - ints_13[i_15]) shr 12)
                        ints_8[i_15] = 4096 - ((4096 - ints_11[i_15]) * (4096 - ints_14[i_15]) shr 12)
                        i_15++
                    }
                    return ints_3
                }
                6 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        i_16 = ints_12[i_15]
                        i_17 = ints_13[i_15]
                        i_18 = ints_14[i_15]
                        ints_6[i_15] = if (i_16 < 2048) i_16 * ints_9[i_15] shr 11 else 4096 - ((4096 - ints_9[i_15]) * (4096 - i_16) shr 11)
                        ints_7[i_15] = if (i_17 < 2048) i_17 * ints_10[i_15] shr 11 else 4096 - ((4096 - ints_10[i_15]) * (4096 - i_17) shr 11)
                        ints_8[i_15] = if (i_18 < 2048) i_18 * ints_11[i_15] shr 11 else 4096 - ((4096 - ints_11[i_15]) * (4096 - i_18) shr 11)
                        i_15++
                    }
                    return ints_3
                }
                7 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        i_16 = ints_9[i_15]
                        i_17 = ints_10[i_15]
                        i_18 = ints_11[i_15]
                        ints_6[i_15] = if (i_16 == 4096) 4096 else (ints_12[i_15] shl 12) / (4096 - i_16)
                        ints_7[i_15] = if (i_17 == 4096) 4096 else (ints_13[i_15] shl 12) / (4096 - i_17)
                        ints_8[i_15] = if (i_18 == 4096) 4096 else (ints_14[i_15] shl 12) / (4096 - i_18)
                        i_15++
                    }
                    return ints_3
                }
                8 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        i_16 = ints_9[i_15]
                        i_17 = ints_10[i_15]
                        i_18 = ints_11[i_15]
                        ints_6[i_15] = if (i_16 == 0) 0 else 4096 - (4096 - ints_12[i_15] shl 12) / i_16
                        ints_7[i_15] = if (i_17 == 0) 0 else 4096 - (4096 - ints_13[i_15] shl 12) / i_17
                        ints_8[i_15] = if (i_18 == 0) 0 else 4096 - (4096 - ints_14[i_15] shl 12) / i_18
                        i_15++
                    }
                    return ints_3
                }
                9 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        i_16 = ints_9[i_15]
                        i_17 = ints_10[i_15]
                        i_18 = ints_11[i_15]
                        i_19 = ints_12[i_15]
                        i_20 = ints_13[i_15]
                        i_21 = ints_14[i_15]
                        ints_6[i_15] = Math.min(i_16, i_19)
                        ints_7[i_15] = Math.min(i_17, i_20)
                        ints_8[i_15] = Math.min(i_18, i_21)
                        i_15++
                    }
                    return ints_3
                }
                10 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        i_16 = ints_9[i_15]
                        i_17 = ints_10[i_15]
                        i_18 = ints_11[i_15]
                        i_19 = ints_12[i_15]
                        i_20 = ints_13[i_15]
                        i_21 = ints_14[i_15]
                        ints_6[i_15] = Math.max(i_16, i_19)
                        ints_7[i_15] = Math.max(i_17, i_20)
                        ints_8[i_15] = Math.max(i_18, i_21)
                        i_15++
                    }
                    return ints_3
                }
                11 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        i_16 = ints_9[i_15]
                        i_17 = ints_10[i_15]
                        i_18 = ints_11[i_15]
                        i_19 = ints_12[i_15]
                        i_20 = ints_13[i_15]
                        i_21 = ints_14[i_15]
                        ints_6[i_15] = if (i_16 > i_19) i_16 - i_19 else i_19 - i_16
                        ints_7[i_15] = if (i_17 > i_20) i_17 - i_20 else i_20 - i_17
                        ints_8[i_15] = if (i_18 > i_21) i_18 - i_21 else i_21 - i_18
                        i_15++
                    }
                    return ints_3
                }
                12 -> {
                    i_15 = 0
                    while (i_15 < TextureRaster.currentWidth) {
                        i_16 = ints_9[i_15]
                        i_17 = ints_10[i_15]
                        i_18 = ints_11[i_15]
                        i_19 = ints_12[i_15]
                        i_20 = ints_13[i_15]
                        i_21 = ints_14[i_15]
                        ints_6[i_15] = i_16 + i_19 - (i_19 * i_16 shr 11)
                        ints_7[i_15] = i_20 + i_17 - (i_20 * i_17 shr 11)
                        ints_8[i_15] = i_21 + i_18 - (i_21 * i_18 shr 11)
                        i_15++
                    }
                }
            }
        }
        return ints_3
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> this.anInt9895 = rsbytebuffer2.readUByte()
            1 -> super.monochrome = rsbytebuffer2.readUByte() == 1
        }
    }
}
