package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM
import kotlin.math.min

class TransparentFontRendererSM(
    hardwarerenderer1: SafeModeToolkit,
    fontmetrics2: FontMetrics,
    arr_3: Array<SpriteType>,
    ints_4: IntArray,
    ints_5: IntArray
) : AbstractFontRenderer(hardwarerenderer1, fontmetrics2) {

    @JvmField
    var safeModeToolkit: SafeModeToolkit = hardwarerenderer1

    @JvmField
    val glyphWidths: IntArray = ints_4

    @JvmField
    val glyphHeights: IntArray = ints_5

    @JvmField
    val glyphAlphaData: Array<ByteArray?>

    @JvmField
    val glyphOffsetY: IntArray

    @JvmField
    val glyphOffsetX: IntArray

    init {
        glyphAlphaData = arrayOfNulls(arr_3.size)
        glyphOffsetY = IntArray(arr_3.size)
        glyphOffsetX = IntArray(arr_3.size)

        for (i_6 in arr_3.indices) {
            val class917 = arr_3[i_6]
            if (class917.alpha != null) {
                glyphAlphaData[i_6] = class917.alpha
            } else {
                val bytes_8 = class917.pixels
                val bytes_9 = ByteArray(bytes_8.size)
                glyphAlphaData[i_6] = bytes_9

                for (i_10 in bytes_8.indices) {
                    bytes_9[i_10] = if (bytes_8[i_10].toInt() == 0) 0 else -1
                }
            }

            glyphOffsetY[i_6] = class917.minY
            glyphOffsetX[i_6] = class917.minX
        }
    }

    override fun drawChar(var_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean) {
        val i_31 = i_3
        var i_21 = i_2
        if (safeModeToolkit.colourBuffer != null) {
            i_21 += glyphOffsetX[var_1]
            var i_31_mod = i_31 + glyphOffsetY[var_1]
            var i_6 = glyphWidths[var_1]
            var i_7 = glyphHeights[var_1]
            val i_8 = safeModeToolkit.viewportWidth
            var i_9 = i_21 + i_31_mod * i_8
            var i_10 = i_8 - i_6
            var i_11 = 0
            var i_12 = 0
            var i_13: Int
            if (i_31_mod < safeModeToolkit.clipTop) {
                i_13 = safeModeToolkit.clipTop - i_31_mod
                i_7 -= i_13
                i_31_mod = safeModeToolkit.clipTop
                i_12 += i_6 * i_13
                i_9 += i_8 * i_13
            }

            if (i_31_mod + i_7 > safeModeToolkit.clipBottom) {
                i_7 -= i_31_mod + i_7 - safeModeToolkit.clipBottom
            }

            if (i_21 < safeModeToolkit.clipLeft) {
                i_13 = safeModeToolkit.clipLeft - i_21
                i_6 -= i_13
                i_21 = safeModeToolkit.clipLeft
                i_12 += i_13
                i_9 += i_13
                i_11 += i_13
                i_10 += i_13
            }

            if (i_6 + i_21 > safeModeToolkit.clipRight) {
                i_13 = i_6 + i_21 - safeModeToolkit.clipRight
                i_6 -= i_13
                i_11 += i_13
                i_10 += i_13
            }

            if (i_6 > 0 && i_7 > 0) {
                blendGlyphPixels(glyphAlphaData[var_1]!!, safeModeToolkit.colourBuffer!!, i_4, i_12, i_9, i_6, i_7, i_10, i_11)
            }
        }
    }

    fun blendGlyphPixels(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int) {
        var i_4_mod = i_4
        var i_5_mod = i_5
        for (i_10 in -i_7 until 0) {
            for (i_11 in -i_6 until 0) {
                var i_12 = bytes_1[i_4_mod++].toInt() and 0xff
                if (i_12 != 0) {
                    val i_13 = (i_12 * (i_3 and 0xff00) and 0xff0000) + ((i_3 and 0xff00ff) * i_12 and -16711936) shr 8
                    i_12 = 256 - i_12
                    val i_14 = ints_2[i_5_mod]
                    ints_2[i_5_mod++] = i_13 + ((i_12 * (i_14 and 0xff00) and 0xff0000) + ((i_14 and 0xff00ff) * i_12 and -16711936) shr 8)
                } else {
                    ++i_5_mod
                }
            }

            i_5_mod += i_8
            i_4_mod += i_9
        }
    }

    fun blendGlyphPixelsMasked(
        bytes_1: ByteArray,
        ints_2: IntArray,
        i_3: Int,
        i_4: Int,
        i_5: Int,
        i_6: Int,
        i_7: Int,
        i_8: Int,
        i_9: Int,
        i_10: Int,
        i_11: Int,
        i_12: Int,
        class45513: ComponentSurface,
        i_14: Int,
        i_15: Int
    ) {
        val packetsdecoder16 = class45513 as ComponentDimensionSM
        val ints_17 = packetsdecoder16.rowStartX
        val ints_18 = packetsdecoder16.rowWidth
        val i_19 = i_10 - safeModeToolkit.clipLeft
        var i_20 = i_11
        if (i_15 > i_11) {
            i_20 = i_15
            var i_5_mod = i_5 + safeModeToolkit.viewportWidth * (i_15 - i_11)
            var i_4_mod = i_4 + (i_15 - i_11) * i_12
        }

        val i_21 = min(ints_17.size + i_15, i_11 + i_7)
        var i_4_mod = i_4
        var i_5_mod = i_5

        for (i_22 in i_20 until i_21) {
            val i_23 = ints_17[i_22 - i_15] + i_14
            val i_24_orig = ints_18[i_22 - i_15]
            var i_25 = i_6
            val i_26: Int
            var i_24 = i_24_orig
            if (i_19 > i_23) {
                i_26 = i_19 - i_23
                if (i_26 >= i_24) {
                    i_4_mod += i_9 + i_6
                    i_5_mod += i_8 + i_6
                    continue
                }

                i_24 -= i_26
            } else {
                i_26 = i_23 - i_19
                if (i_26 >= i_6) {
                    i_4_mod += i_9 + i_6
                    i_5_mod += i_8 + i_6
                    continue
                }

                i_4_mod += i_26
                i_25 = i_6 - i_26
                i_5_mod += i_26
            }

            var i_26_mod = 0
            if (i_25 < i_24) {
                i_24 = i_25
            } else {
                i_26_mod = i_25 - i_24
            }

            for (i_27 in -i_24 until 0) {
                var i_28 = bytes_1[i_4_mod++].toInt() and 0xff
                if (i_28 != 0) {
                    val i_29 = (i_28 * (i_3 and 0xff00) and 0xff0000) + ((i_3 and 0xff00ff) * i_28 and -16711936) shr 8
                    i_28 = 256 - i_28
                    val i_30 = ints_2[i_5_mod]
                    ints_2[i_5_mod++] = (((i_30 and 0xff00ff) * i_28 and -16711936) + (i_28 * (i_30 and 0xff00) and 0xff0000) shr 8) + i_29
                } else {
                    ++i_5_mod
                }
            }

            i_4_mod = i_26_mod + i_4_mod + i_9
            i_5_mod += i_26_mod + i_8
        }
    }

    override fun drawChar(
        var_1: Int,
        i_2: Int,
        i_3: Int,
        i_4: Int,
        bool5: Boolean,
        dimension: ComponentSurface,
        i_7: Int,
        i_8: Int
    ) {
        val i_31 = i_3
        var i_21 = i_2
        if (safeModeToolkit.colourBuffer != null) {
            if (dimension == null) {
                drawChar(var_1, i_21, i_31, i_4, bool5)
            } else {
                i_21 += glyphOffsetX[var_1]
                var i_31_mod = i_31 + glyphOffsetY[var_1]
                var i_9 = glyphWidths[var_1]
                var i_10 = glyphHeights[var_1]
                val i_11 = safeModeToolkit.viewportWidth
                var i_12 = i_21 + i_31_mod * i_11
                var i_13 = i_11 - i_9
                var i_14 = 0
                var i_15 = 0
                var i_16: Int
                if (i_31_mod < safeModeToolkit.clipTop) {
                    i_16 = safeModeToolkit.clipTop - i_31_mod
                    i_10 -= i_16
                    i_31_mod = safeModeToolkit.clipTop
                    i_15 += i_16 * i_9
                    i_12 += i_16 * i_11
                }

                if (i_31_mod + i_10 > safeModeToolkit.clipBottom) {
                    i_10 -= i_31_mod + i_10 - safeModeToolkit.clipBottom
                }

                if (i_21 < safeModeToolkit.clipLeft) {
                    i_16 = safeModeToolkit.clipLeft - i_21
                    i_9 -= i_16
                    i_21 = safeModeToolkit.clipLeft
                    i_15 += i_16
                    i_12 += i_16
                    i_14 += i_16
                    i_13 += i_16
                }

                if (i_9 + i_21 > safeModeToolkit.clipRight) {
                    i_16 = i_9 + i_21 - safeModeToolkit.clipRight
                    i_9 -= i_16
                    i_14 += i_16
                    i_13 += i_16
                }

                if (i_9 > 0 && i_10 > 0) {
                    blendGlyphPixelsMasked(
                        glyphAlphaData[var_1]!!,
                        safeModeToolkit.colourBuffer!!,
                        i_4,
                        i_15,
                        i_12,
                        i_9,
                        i_10,
                        i_13,
                        i_14,
                        i_21,
                        i_31_mod,
                        glyphWidths[var_1],
                        dimension,
                        i_7,
                        i_8
                    )
                }
            }
        }
    }
}
