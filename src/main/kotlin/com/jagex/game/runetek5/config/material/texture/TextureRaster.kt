package com.jagex.game.runetek5.config.material.texture

import kotlin.math.cos
import kotlin.math.sin

class TextureRaster {
    init {
        throw Error()
    }

    companion object {
        @JvmField var PALETTE_SIN: IntArray? = null
        @JvmField var PALETTE_COS: IntArray? = null
        @JvmField var horizontalTable: IntArray? = null
        @JvmField var rowEnd: Int = 0
        @JvmField var currentWidth: Int = 0
        @JvmField var scaledWidth: Int = 0
        @JvmField var verticalTable: IntArray? = null
        @JvmField var columnEnd: Int = 0
        @JvmField var currentHeight: Int = 0

        @JvmStatic
        fun caculateTable() {
            if (PALETTE_SIN == null || PALETTE_COS == null) {
                PALETTE_SIN = IntArray(256)
                PALETTE_COS = IntArray(256)
                for (i_1 in 0 until 256) {
                    val d_2 = 6.283185307179586 * (i_1 / 255.0)
                    PALETTE_SIN!![i_1] = (sin(d_2) * 4096.0).toInt()
                    PALETTE_COS!![i_1] = (cos(d_2) * 4096.0).toInt()
                }
            }
        }

        @JvmStatic
        fun setDimensions(width: Int, height: Int) {
            var idx: Int
            if (width != currentWidth) {
                horizontalTable = IntArray(width)
                for (idx in 0 until width) {
                    horizontalTable!![idx] = (idx shl 12) / width
                }
                rowEnd = width - 1
                currentWidth = width
                scaledWidth = width * 32
            }
            if (currentHeight != height) {
                if (currentWidth != height) {
                    verticalTable = IntArray(height)
                    for (idx in 0 until height) {
                        verticalTable!![idx] = (idx shl 12) / height
                    }
                } else {
                    verticalTable = horizontalTable
                }
                columnEnd = height - 1
                currentHeight = height
            }
        }
    }
}
