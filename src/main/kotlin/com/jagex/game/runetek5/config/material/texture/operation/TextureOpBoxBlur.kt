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
class TextureOpBoxBlur : TextureOperation(1, false) {

    @JvmField var radiusX = 1
    @JvmField var radiusY = 1

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val i_4 = radiusY + radiusY + 1
            val i_5 = 65536 / i_4
            val i_6 = radiusX + radiusX + 1
            val i_7 = 65536 / i_6
            val ints_8 = arrayOfNulls<IntArray>(i_4)
            var i_9: Int
            i_9 = i_1 - radiusY
            while (i_9 <= radiusY + i_1) {
                val ints_14 = getChildMonochromeOutput(0, i_9 and TextureRaster.columnEnd)
                val ints_15 = IntArray(TextureRaster.currentWidth)
                var i_12 = 0
                var i_13: Int
                i_13 = -radiusX
                while (i_13 <= radiusX) {
                    i_12 += ints_14[i_13 and TextureRaster.rowEnd]
                    i_13++
                }
                i_13 = 0
                while (i_13 < TextureRaster.currentWidth) {
                    ints_15[i_13] = i_12 * i_7 shr 16
                    i_12 -= ints_14[i_13 - radiusX and TextureRaster.rowEnd]
                    i_12 += ints_14[i_13 + radiusX and TextureRaster.rowEnd]
                    ++i_13
                }
                ints_8[i_9 + radiusY - i_1] = ints_15
                i_9++
            }
            i_9 = 0
            while (i_9 < TextureRaster.currentWidth) {
                var i_10 = 0
                for (i_11 in 0 until i_4) {
                    i_10 += ints_8[i_11]!![i_9]
                }
                ints_3[i_9] = i_10 * i_5 shr 16
                i_9++
            }
        }
        return ints_3
    }

    override fun getColorOutput(i_1: Int): Array<IntArray> {
        val ints_3 = colorImageCache.getPalette(i_1)
        if (colorImageCache.dirty) {
            val i_4 = radiusY + radiusY + 1
            val i_5 = 65536 / i_4
            val i_6 = radiusX + radiusX + 1
            val i_7 = 65536 / i_6
            val ints_8 = arrayOfNulls<Array<IntArray>>(i_4)
            val i_12: Int
            val i_13: Int
            val i_14: Int
            var i_9 = i_1 - radiusY
            while (i_9 <= radiusY + i_1) {
                val ints_10 = getChildColorOutput(0, i_9 and TextureRaster.columnEnd)
                val ints_11 = Array(3) { IntArray(TextureRaster.currentWidth) }
                var i_12_2 = 0
                var i_13_2 = 0
                var i_14_2 = 0
                val ints_24 = ints_10[0]
                val ints_25 = ints_10[1]
                val ints_17 = ints_10[2]
                var i_18 = -radiusX
                while (i_18 <= radiusX) {
                    val i_27 = i_18 and TextureRaster.rowEnd
                    i_12_2 += ints_24[i_27]
                    i_13_2 += ints_25[i_27]
                    i_14_2 += ints_17[i_27]
                    i_18++
                }
                val ints_26 = ints_11[0]
                val ints_19 = ints_11[1]
                val ints_20 = ints_11[2]
                var i_22: Int
                var i_21 = 0
                while (i_21 < TextureRaster.currentWidth) {
                    ints_26[i_21] = i_7 * i_12_2 shr 16
                    ints_19[i_21] = i_7 * i_13_2 shr 16
                    ints_20[i_21] = i_14_2 * i_7 shr 16
                    i_22 = i_21 - radiusX and TextureRaster.rowEnd
                    i_12_2 -= ints_24[i_22]
                    i_13_2 -= ints_25[i_22]
                    i_14_2 -= ints_17[i_22]
                    ++i_21
                    i_22 = i_21 + radiusX and TextureRaster.rowEnd
                    i_12_2 += ints_24[i_22]
                    i_13_2 += ints_25[i_22]
                    i_14_2 += ints_17[i_22]
                }
                ints_8[i_9 + radiusY - i_1] = ints_11
                i_9++
            }
            val ints_23 = ints_3[0]
            val ints_28 = ints_3[1]
            val ints_29 = ints_3[2]
            var i_12_3 = 0
            while (i_12_3 < TextureRaster.currentWidth) {
                var i_13_3 = 0
                var i_14_3 = 0
                var i_15 = 0
                for (i_16 in 0 until i_4) {
                    val ints_30 = ints_8[i_16]!!
                    i_13_3 += ints_30[0][i_12_3]
                    i_14_3 += ints_30[1][i_12_3]
                    i_15 += ints_30[2][i_12_3]
                }
                ints_23[i_12_3] = i_13_3 * i_5 shr 16
                ints_28[i_12_3] = i_5 * i_14_3 shr 16
                ints_29[i_12_3] = i_5 * i_15 shr 16
                i_12_3++
            }
        }
        return ints_3
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> radiusX = rsbytebuffer2.readUByte()
            1 -> radiusY = rsbytebuffer2.readUByte()
            2 -> monochrome = rsbytebuffer2.readUByte() == 1
        }
    }
}
