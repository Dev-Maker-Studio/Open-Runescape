package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.cache.node.Node

class SafeModeTexture2D(
    @JvmField val materialId: Int,
    @JvmField val totalColorCount: Int,
    argbColor: IntArray,
    shouldBlur: Boolean
) : Node<SafeModeTexture2D>() {

    @JvmField var cycle: Int = 0
    @JvmField var isAnimated: Boolean = false
    @JvmField var colorArgb: IntArray = argbColor

    init {
        if (shouldBlur) {
            val a = IntArray(totalColorCount)
            val r = IntArray(totalColorCount)
            val g = IntArray(totalColorCount)
            val b = IntArray(totalColorCount)

            if (colorsArgb == null || colorsArgb!!.size != colorArgb.size) {
                colorsArgb = IntArray(colorArgb.size)
            }

            val startCount = totalColorCount
            val endCount = totalColorCount
            val start = startCount - 1
            val end = endCount - 1
            val initialColorIndex = startCount * endCount
            var primaryColorIndex = startCount
            var secondaryColorIndex = startCount

            for (pass in 2 downTo 0) {
                for (idx in start downTo 0) {
                    val secondary = colorArgb[--primaryColorIndex]
                    a[idx] += (secondary shr 24) and 0xff
                    r[idx] += (secondary shr 16) and 0xff
                    g[idx] += (secondary shr 8) and 0xff
                    b[idx] += secondary and 0xff
                }

                if (primaryColorIndex == 0) {
                    primaryColorIndex = initialColorIndex
                }
            }

            var startColorIndex = initialColorIndex

            for (idx in end downTo 0) {
                var secondary = 1
                var primary = 1
                var sumGreen = 0
                var sumBlue = 0
                var sumRed = 0
                var sumAlpha = 0

                for (pass in 2 downTo 0) {
                    sumAlpha += a[--primary]
                    sumRed += r[primary]
                    sumGreen += g[primary]
                    sumBlue += b[primary]
                    if (primary == 0) {
                        primary = startCount
                    }
                }

                for (rowIdx in start downTo 0) {
                    --primary
                    --secondary
                    val calcedA = sumAlpha / 9
                    val calcedR = sumRed / 9
                    val calcedG = sumGreen / 9
                    val calcedB = sumBlue / 9
                    --startColorIndex
                    colorsArgb!![startColorIndex] = (calcedA shl 24) or (calcedR shl 16) or (calcedG shl 8) or calcedB
                    sumAlpha += a[primary] - a[secondary]
                    sumRed += r[primary] - r[secondary]
                    sumBlue += b[primary] - b[secondary]
                    sumGreen += g[primary] - g[secondary]
                    if (primary == 0) {
                        primary = startCount
                    }

                    if (secondary == 0) {
                        secondary = startCount
                    }
                }

                for (rowIdx in start downTo 0) {
                    val calcedA = colorArgb[--primaryColorIndex]
                    val calcedR = colorArgb[--secondaryColorIndex]
                    a[rowIdx] += ((calcedA shr 24) and 0xff) - ((calcedR shr 24) and 0xff)
                    r[rowIdx] += ((calcedA shr 16) and 0xff) - ((calcedR shr 16) and 0xff)
                    g[rowIdx] += ((calcedA shr 8) and 0xff) - ((calcedR shr 8) and 0xff)
                    b[rowIdx] += (calcedA and 0xff) - (calcedR and 0xff)
                }

                if (primaryColorIndex == 0) {
                    primaryColorIndex = initialColorIndex
                }

                if (secondaryColorIndex == 0) {
                    secondaryColorIndex = initialColorIndex
                }
            }

            val tempColorArgb = colorArgb
            colorArgb = colorsArgb!!
            colorsArgb = tempColorArgb
        }
    }

    fun animate(offsetX: Int, offsetY: Int) {
        if (offsetX != 0 || offsetY != 0) {
            if (colorsArgb == null || colorsArgb!!.size != colorArgb.size) {
                colorsArgb = IntArray(colorArgb.size)
            }

            val colorCount = colorArgb.size
            val mask = totalColorCount - 1
            val rowOffset = offsetY * totalColorCount
            val lastIndex = colorCount - 1

            var rowStart = 0
            while (rowStart < colorCount) {
                val sourceRowStart = (rowStart + rowOffset) and lastIndex

                for (pos in 0 until totalColorCount) {
                    val dst = rowStart + pos
                    val src = sourceRowStart + ((pos + offsetX) and mask)
                    colorsArgb!![dst] = colorArgb[src]
                }

                rowStart += totalColorCount
            }

            val tempColorRgb = colorArgb
            colorArgb = colorsArgb!!
            colorsArgb = tempColorRgb
        }
    }

    fun getArgbColor(): IntArray = colorArgb

    companion object {
        @JvmStatic
        var colorsArgb: IntArray? = null
    }
}
