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

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            rasterizeShapes(monochromeImageCache.getAllPaletteData())
        }
        return output
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val palette = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val width = TextureRaster.currentWidth
            val height = TextureRaster.currentHeight
            val pixelBuffer = Array(height) { IntArray(width) }
            val allPalettes = colorImageCache.getAllPalettes()
            @Suppress("UNCHECKED_CAST")
            rasterizeShapes(pixelBuffer as Array<IntArray?>)
            for (row in 0 until TextureRaster.currentHeight) {
                val rowPixels = pixelBuffer[row]
                val paletteRow = allPalettes[row]
                val redChannel = paletteRow[0]
                val greenChannel = paletteRow[1]
                val blueChannel = paletteRow[2]
                for (x in 0 until TextureRaster.currentWidth) {
                    val rgb = rowPixels[x]
                    blueChannel[x] = (rgb and 0xff) shl 4
                    greenChannel[x] = (rgb and 0xff00) shr 4
                    redChannel[x] = (rgb and 0xff0000) shr 12
                }
            }
        }
        return palette
    }

    fun rasterizeShapes(pixelData: Array<IntArray?>) {
        val width = TextureRaster.currentWidth
        val height = TextureRaster.currentHeight
        @Suppress("UNCHECKED_CAST")
        StaticMethods.setPixelBuffer(pixelData as Array<IntArray>?)
        setClipBounds(TextureRaster.rowEnd, TextureRaster.columnEnd)
        if (shapes != null) {
            for (i in shapes!!.indices) {
                val shape = shapes!![i] ?: continue
                val fill = shape.fillColor
                val stroke = shape.strokeColor
                if (fill >= 0) {
                    if (stroke >= 0) {
                        shape.drawFilledAndStroked(width, height)
                    } else {
                        shape.drawFilled(width, height)
                    }
                } else if (stroke >= 0) {
                    shape.drawStroked(width, height)
                }
            }
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> {
                val count = buffer.readUByte()
                shapes = arrayOfNulls<TextureShapeRasterizer>(count)
                for (i in 0 until count) {
                    val shapeType = buffer.readUByte()
                    shapes!![i] = when (shapeType) {
                        0 -> LineRasterizer.decode(buffer)
                        1 -> BezierCurveRasterizer.decode(buffer)
                        2 -> RectangleRasterizer.decode(buffer)
                        3 -> EllipseRasterizer.decode(buffer)
                        else -> null
                    }
                }
            }
            1 -> monochrome = buffer.readUByte() == 1
        }
    }

    companion object {
        @JvmField var clipMinX: Int = 0
        @JvmField var clipMaxX = 100
        @JvmField var clipMinY: Int = 0
        @JvmField var clipMaxY = 100

        @JvmStatic
        fun setClipBounds(maxX: Int, maxY: Int) {
            clipMinX = 0
            clipMaxX = maxX
            clipMinY = 0
            clipMaxY = maxY
        }
    }
}
