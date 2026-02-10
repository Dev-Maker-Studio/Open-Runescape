package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpIrregularBricks : TextureOperation(0, true) {

    var randomSeed: Int = 0
    var minBrickWidth: Int = 1024
    var maxBrickWidth: Int = 2048
    var minBrickHeight: Int = 409
    var maxBrickHeight: Int = 819
    var offsetVariation: Int = 1024
    var cornerMode: Int = 0
    var heightVariationMultiplier: Int = 1024
    var brightnessVariation: Int = 1024
    var cornerRadius: Int = 0

    fun drawBrick(random1: Random, ints_2: Array<IntArray?>, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        val i_8 = if (brightnessVariation > 0) 4096 - StaticMethods.boundedRandom(random1, brightnessVariation) else 4096
        val i_9 = heightVariationMultiplier * cornerRadius shr 12
        val i_10 = cornerRadius - if (i_9 > 0) StaticMethods.boundedRandom(random1, i_9) else 0
        var i_3Var = i_3
        if (i_3Var >= TextureRaster.currentWidth) {
            i_3Var -= TextureRaster.currentWidth
        }
        var i_11: Int
        var i_12: Int
        if (i_10 > 0) {
            if (i_6 > 0 && i_5 > 0) {
                i_11 = i_5 / 2
                i_12 = i_6 / 2
                val i_13 = min(i_11, i_10)
                val i_14 = min(i_12, i_10)
                val i_15 = i_3Var + i_13
                val i_16 = i_5 - i_13 * 2
                for (i_17 in 0 until i_6) {
                    val ints_18 = ints_2[i_17 + i_4]!!
                    var i_19: Int
                    var i_20: Int
                    var i_21: Int
                    if (i_17 < i_14) {
                        i_19 = i_8 * i_17 / i_14
                        if (cornerMode == 0) {
                            i_20 = 0
                            while (i_20 < i_13) {
                                i_21 = i_8 * i_20 / i_13
                                ints_18[(i_3Var + i_20) and TextureRaster.rowEnd] = i_21 * i_19 shr 12
                                ints_18[(i_3Var + i_5 - i_20 - 1) and TextureRaster.rowEnd] = i_21 * i_19 shr 12
                                i_20++
                            }
                        } else {
                            i_20 = 0
                            while (i_20 < i_13) {
                                i_21 = i_20 * i_8 / i_13
                                ints_18[(i_3Var + i_20) and TextureRaster.rowEnd] = min(i_21, i_19)
                                ints_18[(i_3Var + i_5 - i_20 - 1) and TextureRaster.rowEnd] = min(i_21, i_19)
                                i_20++
                            }
                        }
                        if (i_16 + i_15 > TextureRaster.currentWidth) {
                            i_20 = TextureRaster.currentWidth - i_15
                            ArrayUtils.setVariables(ints_18, i_15, i_20, i_19)
                            ArrayUtils.setVariables(ints_18, 0, i_16 - i_20, i_19)
                        } else {
                            ArrayUtils.setVariables(ints_18, i_15, i_16, i_19)
                        }
                    } else {
                        i_19 = i_6 - i_17 - 1
                        if (i_19 < i_14) {
                            i_20 = i_8 * i_19 / i_14
                            var i_22: Int
                            if (cornerMode == 0) {
                                i_21 = 0
                                while (i_21 < i_13) {
                                    i_22 = i_21 * i_8 / i_13
                                    ints_18[(i_3Var + i_21) and TextureRaster.rowEnd] = i_20 * i_22 shr 12
                                    ints_18[(i_3Var + i_5 - i_21 - 1) and TextureRaster.rowEnd] = i_20 * i_22 shr 12
                                    i_21++
                                }
                            } else {
                                i_21 = 0
                                while (i_21 < i_13) {
                                    i_22 = i_21 * i_8 / i_13
                                    ints_18[(i_3Var + i_21) and TextureRaster.rowEnd] = min(i_22, i_20)
                                    ints_18[(i_3Var + i_5 - i_21 - 1) and TextureRaster.rowEnd] = min(i_22, i_20)
                                    i_21++
                                }
                            }
                            if (i_16 + i_15 > TextureRaster.currentWidth) {
                                i_21 = TextureRaster.currentWidth - i_15
                                ArrayUtils.setVariables(ints_18, i_15, i_21, i_20)
                                ArrayUtils.setVariables(ints_18, 0, i_16 - i_21, i_20)
                            } else {
                                ArrayUtils.setVariables(ints_18, i_15, i_16, i_20)
                            }
                        } else {
                            i_20 = 0
                            while (i_20 < i_13) {
                                ints_18[(i_3Var + i_20) and TextureRaster.rowEnd] = i_20 * i_8 / i_13
                                ints_18[(i_3Var + i_5 - i_20 - 1) and TextureRaster.rowEnd] = i_20 * i_8 / i_13
                                i_20++
                            }
                            if (i_15 + i_16 > TextureRaster.currentWidth) {
                                i_20 = TextureRaster.currentWidth - i_15
                                ArrayUtils.setVariables(ints_18, i_15, i_20, i_8)
                                ArrayUtils.setVariables(ints_18, 0, i_16 - i_20, i_8)
                            } else {
                                ArrayUtils.setVariables(ints_18, i_15, i_16, i_8)
                            }
                        }
                    }
                }
            }
        } else if (i_3Var + i_5 > TextureRaster.currentWidth) {
            i_11 = TextureRaster.currentWidth - i_3Var
            i_12 = 0
            while (i_12 < i_6) {
                val ints_23 = ints_2[i_12 + i_4]!!
                ArrayUtils.setVariables(ints_23, i_3Var, i_11, i_8)
                ArrayUtils.setVariables(ints_23, 0, i_5 - i_11, i_8)
                i_12++
            }
        } else {
            i_11 = 0
            while (i_11 < i_6) {
                ArrayUtils.setVariables(ints_2[i_11 + i_4]!!, i_3Var, i_5, i_8)
                i_11++
            }
        }
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> randomSeed = rsbytebuffer2.readUByte()
            1 -> minBrickWidth = rsbytebuffer2.readUShort()
            2 -> maxBrickWidth = rsbytebuffer2.readUShort()
            3 -> minBrickHeight = rsbytebuffer2.readUShort()
            4 -> maxBrickHeight = rsbytebuffer2.readUShort()
            5 -> offsetVariation = rsbytebuffer2.readUShort()
            6 -> cornerMode = rsbytebuffer2.readUByte()
            7 -> heightVariationMultiplier = rsbytebuffer2.readUShort()
            8 -> brightnessVariation = rsbytebuffer2.readUShort()
        }
    }

    override fun postDecode() {
    }

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = super.monochromeImageCache.getPalette(i_1)
        if (super.monochromeImageCache.dirty) {
            val ints_4 = super.monochromeImageCache.method5722()
            var i_5 = 0
            var i_6 = 0
            var i_7 = 0
            var i_8 = 0
            var i_9 = 0
            var bool10 = true
            var bool11 = true
            var i_12 = 0
            var i_13 = 0
            val i_14 = minBrickWidth * TextureRaster.currentWidth shr 12
            val i_15 = maxBrickWidth * TextureRaster.currentWidth shr 12
            val i_16 = minBrickHeight * TextureRaster.currentHeight shr 12
            val i_17 = maxBrickHeight * TextureRaster.currentHeight shr 12
            if (i_17 <= 1) {
                return ints_4[i_1]!!
            } else {
                cornerRadius = TextureRaster.currentWidth / 8 * offsetVariation shr 12
                val i_18 = TextureRaster.currentWidth / i_14 + 1
                val ints_19 = Array(i_18) { IntArray(3) }
                var ints_20 = Array(i_18) { IntArray(3) }
                val random21 = Random(randomSeed.toLong())
                while (true) {
                    var i_22 = i_14 + StaticMethods.boundedRandom(random21, i_15 - i_14)
                    var i_23 = i_16 + StaticMethods.boundedRandom(random21, i_17 - i_16)
                    var i_24 = i_22 + i_8
                    if (i_24 > TextureRaster.currentWidth) {
                        i_22 = TextureRaster.currentWidth - i_8
                        i_24 = TextureRaster.currentWidth
                    }
                    var i_25: Int
                    var i_28: Int
                    if (bool11) {
                        i_25 = 0
                    } else {
                        var i_26 = i_9
                        val ints_27 = ints_20[i_9]
                        i_28 = 0
                        var i_29 = i_24 + i_5
                        if (i_29 < 0) {
                            i_29 += TextureRaster.currentWidth
                        }
                        if (i_29 > TextureRaster.currentWidth) {
                            i_29 -= TextureRaster.currentWidth
                        }
                        while (true) {
                            val ints_30 = ints_20[i_26]
                            if (i_29 >= ints_30[0] && i_29 <= ints_30[1]) {
                                i_25 = ints_27[2]
                                if (i_26 != i_9) {
                                    var i_38 = i_8 + i_5
                                    if (i_38 < 0) {
                                        i_38 += TextureRaster.currentWidth
                                    }
                                    if (i_38 > TextureRaster.currentWidth) {
                                        i_38 -= TextureRaster.currentWidth
                                    }
                                    var i_31: Int
                                    var ints_32: IntArray
                                    i_31 = 1
                                    while (i_31 <= i_28) {
                                        ints_32 = ints_20[(i_31 + i_9) % i_12]
                                        i_25 = max(i_25, ints_32[2])
                                        i_31++
                                    }
                                    i_31 = 0
                                    while (i_31 <= i_28) {
                                        ints_32 = ints_20[(i_31 + i_9) % i_12]
                                        val i_33 = ints_32[2]
                                        if (i_33 != i_25) {
                                            val i_34 = ints_32[0]
                                            val i_35 = ints_32[1]
                                            val i_36: Int
                                            val i_37: Int
                                            if (i_38 < i_29) {
                                                i_36 = max(i_38, i_34)
                                                i_37 = min(i_29, i_35)
                                            } else if (i_34 == 0) {
                                                i_36 = 0
                                                i_37 = min(i_29, i_35)
                                            } else {
                                                i_36 = max(i_38, i_34)
                                                i_37 = TextureRaster.currentWidth
                                            }
                                            drawBrick(random21, ints_4, i_36 + i_7, i_33, i_37 - i_36, i_25 - i_33)
                                        }
                                        i_31++
                                    }
                                }
                                i_9 = i_26
                                break
                            }
                            ++i_26
                            if (i_26 >= i_12) {
                                i_26 = 0
                            }
                            ++i_28
                        }
                    }
                    if (i_25 + i_23 > TextureRaster.currentHeight) {
                        i_23 = TextureRaster.currentHeight - i_25
                    } else {
                        bool10 = false
                    }
                    val ints_39: IntArray
                    if (i_24 == TextureRaster.currentWidth) {
                        drawBrick(random21, ints_4, i_6 + i_8, i_25, i_22, i_23)
                        if (bool10) {
                            return ints_3
                        }
                        bool10 = true
                        ints_39 = ints_19[i_13++]
                        ints_39[0] = i_8
                        ints_39[1] = i_24
                        ints_39[2] = i_23 + i_25
                        val ints_41 = ints_20
                        ints_20 = ints_19
                        val ints_19New = ints_41
                        i_12 = i_13
                        i_13 = 0
                        i_7 = i_6
                        i_6 = StaticMethods.boundedRandom(random21, TextureRaster.currentWidth)
                        i_5 = i_6 - i_7
                        i_8 = 0
                        i_28 = i_5
                        if (i_5 < 0) {
                            i_28 = i_5 + TextureRaster.currentWidth
                        }
                        if (i_28 > TextureRaster.currentWidth) {
                            i_28 -= TextureRaster.currentWidth
                        }
                        i_9 = 0
                        while (true) {
                            val ints_40 = ints_20[i_9]
                            if (i_28 >= ints_40[0] && i_28 <= ints_40[1]) {
                                bool11 = false
                                break
                            }
                            ++i_9
                            if (i_9 >= i_12) {
                                i_9 = 0
                            }
                        }
                    } else {
                        ints_39 = ints_19[i_13++]
                        ints_39[0] = i_8
                        ints_39[1] = i_24
                        ints_39[2] = i_23 + i_25
                        drawBrick(random21, ints_4, i_6 + i_8, i_25, i_22, i_23)
                        i_8 = i_24
                    }
                }
            }
        } else {
            return ints_3
        }
    }

    companion object {
        @JvmField
        var backgroundColor: Int = 0
    }
}
