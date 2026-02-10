package com.jagex.game.runetek5.config.material.texture.operation

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
class TextureOpRasterizer : TextureOperation(0, true) {

    @JvmField var shapes: Array<TextureShapeRasterizer?>? = null

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            rasterizeShapes(monochromeImageCache.method5722())
        }
        return ints_3
    }

    override fun getColorOutput(i_1: Int): Array<IntArray> {
        val ints_3 = colorImageCache.getPalette(i_1)
        if (colorImageCache.dirty) {
            val i_4 = TextureRaster.currentWidth
            val i_5 = TextureRaster.currentHeight
            val ints_6 = Array(i_5) { IntArray(i_4) }
            val ints_7 = colorImageCache.method5464()
            @Suppress("UNCHECKED_CAST")
            rasterizeShapes(ints_6 as Array<IntArray?>)
            for (i_8 in 0 until TextureRaster.currentHeight) {
                val ints_9 = ints_6[i_8]
                val ints_10 = ints_7[i_8]
                val ints_11 = ints_10[0]
                val ints_12 = ints_10[1]
                val ints_13 = ints_10[2]
                for (i_14 in 0 until TextureRaster.currentWidth) {
                    val i_15 = ints_9[i_14]
                    ints_13[i_14] = (i_15 and 0xff) shl 4
                    ints_12[i_14] = (i_15 and 0xff00) shr 4
                    ints_11[i_14] = (i_15 and 0xff0000) shr 12
                }
            }
        }
        return ints_3
    }

    fun rasterizeShapes(ints_1: Array<IntArray?>) {
        val i_3 = TextureRaster.currentWidth
        val i_4 = TextureRaster.currentHeight
        @Suppress("UNCHECKED_CAST")
        StaticMethods.method3936(ints_1 as Array<IntArray>?)
        setClipBounds(TextureRaster.rowEnd, TextureRaster.columnEnd)
        if (shapes != null) {
            for (i_5 in shapes!!.indices) {
                val class1496 = shapes!![i_5] ?: continue
                val i_7 = class1496.fillColor
                val i_8 = class1496.strokeColor
                if (i_7 >= 0) {
                    if (i_8 >= 0) {
                        class1496.drawFilledAndStroked(i_3, i_4)
                    } else {
                        class1496.drawFilled(i_3, i_4)
                    }
                } else if (i_8 >= 0) {
                    class1496.drawStroked(i_3, i_4)
                }
            }
        }
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> {
                val count = rsbytebuffer2.readUByte()
                shapes = arrayOfNulls<TextureShapeRasterizer>(count)
                for (i_4 in 0 until count) {
                    val i_5 = rsbytebuffer2.readUByte()
                    shapes!![i_4] = when (i_5) {
                        0 -> LineRasterizer.decode(rsbytebuffer2)
                        1 -> BezierCurveRasterizer.decode(rsbytebuffer2)
                        2 -> RectangleRasterizer.decode(rsbytebuffer2)
                        3 -> EllipseRasterizer.decode(rsbytebuffer2)
                        else -> null
                    }
                }
            }
            1 -> monochrome = rsbytebuffer2.readUByte() == 1
        }
    }

    companion object {
        @JvmField var clipMinX: Int = 0
        @JvmField var clipMaxX = 100
        @JvmField var clipMinY: Int = 0
        @JvmField var clipMaxY = 100

        @JvmStatic
        fun setClipBounds(i_2: Int, i_3: Int) {
            clipMinX = 0
            clipMaxX = i_2
            clipMinY = 0
            clipMaxY = i_3
        }
    }
}
