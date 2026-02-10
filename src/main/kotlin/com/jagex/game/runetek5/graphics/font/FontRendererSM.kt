package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM
import kotlin.math.min

/**
 * The default font renderer for safe mode, doesn't support alpha
 * @author Brainpower
 */
class FontRendererSM(
    override val toolkit: SafeModeToolkit,
    fontMetrics: FontMetrics,
    arr_3: Array<SpriteType>,
    ints_4: IntArray,
    ints_5: IntArray
) : AbstractFontRenderer(toolkit, fontMetrics) {

    @JvmField
    val charWidths: IntArray = ints_4

    @JvmField
    val charHeights: IntArray = ints_5

    @JvmField
    val aByteArrayArray8967: Array<ByteArray?>

    @JvmField
    val anIntArray8972: IntArray

    @JvmField
    val anIntArray8973: IntArray

    @JvmField
    val anIntArray8970: IntArray?

    init {
        aByteArrayArray8967 = Array(arr_3.size) { null }
        anIntArray8972 = IntArray(arr_3.size)
        anIntArray8973 = IntArray(arr_3.size)
        for (i_6 in arr_3.indices) {
            aByteArrayArray8967[i_6] = arr_3[i_6].pixels
            anIntArray8972[i_6] = arr_3[i_6].minY
            anIntArray8973[i_6] = arr_3[i_6].minX
        }
        anIntArray8970 = arr_3[0].pallete
    }

    override fun drawChar(ch: Int, x: Int, y: Int, color: Int, useAlpha: Boolean) {
        var drawY = y
        var drawX = x
        if (toolkit.colourBuffer != null) {
            drawX += anIntArray8973[ch]
            drawY += anIntArray8972[ch]
            var charWidth = charWidths[ch]
            var charHeight = charHeights[ch]
            val viewportWidth = toolkit.viewportWidth
            var pixelIndex = drawX + drawY * viewportWidth
            var rowPadding = viewportWidth - charWidth
            var i_11 = 0
            var rowOffset = 0
            var rowCount: Int
            if (drawY < toolkit.clipTop) {
                rowCount = toolkit.clipTop - drawY
                charHeight -= rowCount
                drawY = toolkit.clipTop
                rowOffset += rowCount * charWidth
                pixelIndex += rowCount * viewportWidth
            }
            if (drawY + charHeight > toolkit.clipBottom) {
                charHeight -= drawY + charHeight - toolkit.clipBottom
            }
            if (drawX < toolkit.clipLeft) {
                rowCount = toolkit.clipLeft - drawX
                charWidth -= rowCount
                drawX = toolkit.clipLeft
                rowOffset += rowCount
                pixelIndex += rowCount
                i_11 += rowCount
                rowPadding += rowCount
            }
            if (charWidth + drawX > toolkit.clipRight) {
                rowCount = charWidth + drawX - toolkit.clipRight
                charWidth -= rowCount
                i_11 += rowCount
                rowPadding += rowCount
            }
            if (charWidth > 0 && charHeight > 0) {
                if (useAlpha) {
                    method14325(aByteArrayArray8967[ch]!!, toolkit.colourBuffer!!, color, rowOffset, pixelIndex, charWidth, charHeight, rowPadding, i_11)
                } else {
                    method14334(aByteArrayArray8967[ch]!!, toolkit.colourBuffer!!, anIntArray8970!!, rowOffset, pixelIndex, charWidth, charHeight, rowPadding, i_11)
                }
            }
        }
    }

    fun method14325(bytes_1: ByteArray, pixels: IntArray, color: Int, x: Int, y: Int, width: Int, height: Int, scanWidth: Int, padding: Int) {
        var x = x
        var y = y
        val dw = -(width shr 2)
        val width = -(width and 0x3)
        for (dy in -height until 0) {
            for (dx in dw until 0) {
                if (bytes_1[x++] != 0.toByte()) {
                    pixels[y++] = color
                } else {
                    ++y
                }
                if (bytes_1[x++] != 0.toByte()) {
                    pixels[y++] = color
                } else {
                    ++y
                }
                if (bytes_1[x++] != 0.toByte()) {
                    pixels[y++] = color
                } else {
                    ++y
                }
                if (bytes_1[x++] != 0.toByte()) {
                    pixels[y++] = color
                } else {
                    ++y
                }
            }
            for (dx in width until 0) {
                if (bytes_1[x++] != 0.toByte()) {
                    pixels[y++] = color
                } else {
                    ++y
                }
            }
            y += scanWidth
            x += padding
        }
    }

    override fun drawChar(var_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean, dimension: ComponentSurface, i_7: Int, i_8: Int) {
        var i_31 = i_3
        var i_21 = i_2
        if (toolkit.colourBuffer != null) {
            if (dimension == null) {
                drawChar(var_1, i_21, i_31, i_4, bool5)
            } else {
                i_21 += anIntArray8973[var_1]
                i_31 += anIntArray8972[var_1]
                var i_9 = charWidths[var_1]
                var i_10 = charHeights[var_1]
                val i_11 = toolkit.viewportWidth
                var i_12 = i_21 + i_31 * i_11
                var i_13 = i_11 - i_9
                var i_14 = 0
                var i_15 = 0
                var i_16: Int
                if (i_31 < toolkit.clipTop) {
                    i_16 = toolkit.clipTop - i_31
                    i_10 -= i_16
                    i_31 = toolkit.clipTop
                    i_15 += i_9 * i_16
                    i_12 += i_16 * i_11
                }
                if (i_31 + i_10 > toolkit.clipBottom) {
                    i_10 -= i_31 + i_10 - toolkit.clipBottom
                }
                if (i_21 < toolkit.clipLeft) {
                    i_16 = toolkit.clipLeft - i_21
                    i_9 -= i_16
                    i_21 = toolkit.clipLeft
                    i_15 += i_16
                    i_12 += i_16
                    i_14 += i_16
                    i_13 += i_16
                }
                if (i_9 + i_21 > toolkit.clipRight) {
                    i_16 = i_9 + i_21 - toolkit.clipRight
                    i_9 -= i_16
                    i_14 += i_16
                    i_13 += i_16
                }
                if (i_9 > 0 && i_10 > 0) {
                    if (bool5) {
                        method14327(aByteArrayArray8967[var_1]!!, toolkit.colourBuffer!!, i_4, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, charWidths[var_1], dimension, i_7, i_8)
                    } else {
                        method14326(aByteArrayArray8967[var_1]!!, toolkit.colourBuffer!!, anIntArray8970!!, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, charWidths[var_1], dimension, i_7, i_8)
                    }
                }
            }
        }
    }

    fun method14326(bytes_1: ByteArray, ints_2: IntArray, ints_3: IntArray, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, i_10: Int, i_11: Int, i_12: Int, i_13: Int, dimension: ComponentSurface, i_15: Int, i_16: Int) {
        val packetsdecoder17 = dimension as ComponentDimensionSM
        val ints_18 = packetsdecoder17.anIntArray9077
        val ints_19 = packetsdecoder17.anIntArray9078
        val i_20 = i_11 - toolkit.clipLeft
        var i_21 = i_12
        var i_5 = i_5
        var i_6 = i_6
        if (i_16 > i_12) {
            i_21 = i_16
            i_6 += (i_16 - i_12) * toolkit.viewportWidth
            i_5 += i_13 * (i_16 - i_12)
        }
        val i_22 = min(ints_18.size + i_16, i_8 + i_12)
        for (i_24 in i_21 until i_22) {
            var i_25 = i_15 + ints_18[i_24 - i_16]
            var i_26 = ints_19[i_24 - i_16]
            var i_27 = i_7
            var i_28: Int
            if (i_20 > i_25) {
                i_28 = i_20 - i_25
                if (i_28 >= i_26) {
                    i_5 += i_10 + i_7
                    i_6 += i_7 + i_9
                    continue
                }
                i_26 -= i_28
            } else {
                i_28 = i_25 - i_20
                if (i_28 >= i_7) {
                    i_5 += i_10 + i_7
                    i_6 += i_7 + i_9
                    continue
                }
                i_5 += i_28
                i_27 = i_7 - i_28
                i_6 += i_28
            }
            i_28 = 0
            if (i_27 < i_26) {
                i_26 = i_27
            } else {
                i_28 = i_27 - i_26
            }
            for (i_29 in -i_26 until 0) {
                val b_30: Byte
                if (bytes_1[i_5++].also { b_30 = it } != 0.toByte()) {
                    ints_2[i_6++] = ints_3[b_30.toInt() and 0xff]
                } else {
                    ++i_6
                }
            }
            i_5 += i_10 + i_28
            i_6 += i_28 + i_9
        }
    }

    fun method14327(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, i_10: Int, i_11: Int, i_12: Int, class45513: ComponentSurface, i_14: Int, i_15: Int) {
        val packetsdecoder16 = class45513 as ComponentDimensionSM
        val ints_17 = packetsdecoder16.anIntArray9077
        val ints_18 = packetsdecoder16.anIntArray9078
        val i_19 = i_10 - toolkit.clipLeft
        var i_20 = i_11
        var i_4 = i_4
        var i_5 = i_5
        if (i_15 > i_11) {
            i_20 = i_15
            i_5 += toolkit.viewportWidth * (i_15 - i_11)
            i_4 += (i_15 - i_11) * i_12
        }
        val i_21 = min(ints_17.size + i_15, i_11 + i_7)
        for (i_22 in i_20 until i_21) {
            val i_23 = ints_17[i_22 - i_15] + i_14
            var i_24 = ints_18[i_22 - i_15]
            var i_25 = i_6
            var i_26: Int
            if (i_19 > i_23) {
                i_26 = i_19 - i_23
                if (i_26 >= i_24) {
                    i_4 += i_9 + i_6
                    i_5 += i_8 + i_6
                    continue
                }
                i_24 -= i_26
            } else {
                i_26 = i_23 - i_19
                if (i_26 >= i_6) {
                    i_4 += i_9 + i_6
                    i_5 += i_8 + i_6
                    continue
                }
                i_4 += i_26
                i_25 = i_6 - i_26
                i_5 += i_26
            }
            i_26 = 0
            if (i_25 < i_24) {
                i_24 = i_25
            } else {
                i_26 = i_25 - i_24
            }
            for (i_27 in -i_24 until 0) {
                if (bytes_1[i_4++] != 0.toByte()) {
                    ints_2[i_5++] = i_3
                } else {
                    ++i_5
                }
            }
            i_4 += i_26 + i_4
            i_5 += i_26 + i_8
        }
    }

    fun method14334(bytes_1: ByteArray, ints_2: IntArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int) {
        var i_4 = i_4
        var i_5 = i_5
        val i_10 = -(i_6 shr 2)
        val i_6 = -(i_6 and 0x3)
        for (i_12 in -i_7 until 0) {
            var i_13: Int
            var b_14: Byte
            i_13 = i_10
            while (i_13 < 0) {
                if (bytes_1[i_4++].also { b_14 = it } != 0.toByte()) {
                    ints_2[i_5++] = ints_3[b_14.toInt() and 0xff]
                } else {
                    ++i_5
                }
                if (bytes_1[i_4++].also { b_14 = it } != 0.toByte()) {
                    ints_2[i_5++] = ints_3[b_14.toInt() and 0xff]
                } else {
                    ++i_5
                }
                if (bytes_1[i_4++].also { b_14 = it } != 0.toByte()) {
                    ints_2[i_5++] = ints_3[b_14.toInt() and 0xff]
                } else {
                    ++i_5
                }
                if (bytes_1[i_4++].also { b_14 = it } != 0.toByte()) {
                    ints_2[i_5++] = ints_3[b_14.toInt() and 0xff]
                } else {
                    ++i_5
                }
                i_13++
            }
            i_13 = i_6
            while (i_13 < 0) {
                if (bytes_1[i_4++].also { b_14 = it } != 0.toByte()) {
                    ints_2[i_5++] = ints_3[b_14.toInt() and 0xff]
                } else {
                    ++i_5
                }
                i_13++
            }
            i_5 += i_8
            i_4 += i_9
        }
    }
}
