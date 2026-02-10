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

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val kernelHeight = radiusY + radiusY + 1
            val verticalScale = 65536 / kernelHeight
            val kernelWidth = radiusX + radiusX + 1
            val horizontalScale = 65536 / kernelWidth
            val rowBuffers = arrayOfNulls<IntArray>(kernelHeight)
            var row: Int
            row = y - radiusY
            while (row <= radiusY + y) {
                val childRow = getChildMonochromeOutput(0, row and TextureRaster.columnEnd)
                val blurredRow = IntArray(TextureRaster.currentWidth)
                var runningSum = 0
                var col: Int
                col = -radiusX
                while (col <= radiusX) {
                    runningSum += childRow[col and TextureRaster.rowEnd]
                    col++
                }
                col = 0
                while (col < TextureRaster.currentWidth) {
                    blurredRow[col] = runningSum * horizontalScale shr 16
                    runningSum -= childRow[col - radiusX and TextureRaster.rowEnd]
                    runningSum += childRow[col + radiusX and TextureRaster.rowEnd]
                    ++col
                }
                rowBuffers[row + radiusY - y] = blurredRow
                row++
            }
            row = 0
            while (row < TextureRaster.currentWidth) {
                var verticalSum = 0
                for (k in 0 until kernelHeight) {
                    verticalSum += rowBuffers[k]!![row]
                }
                output[row] = verticalSum * verticalScale shr 16
                row++
            }
        }
        return output
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val palette = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val kernelHeight = radiusY + radiusY + 1
            val verticalScale = 65536 / kernelHeight
            val kernelWidth = radiusX + radiusX + 1
            val horizontalScale = 65536 / kernelWidth
            val rowBuffers = arrayOfNulls<Array<IntArray>>(kernelHeight)
            val outRed: Int
            val outGreen: Int
            val outBlue: Int
            var row = y - radiusY
            while (row <= radiusY + y) {
                val childColors = getChildColorOutput(0, row and TextureRaster.columnEnd)
                val blurredRow = Array(3) { IntArray(TextureRaster.currentWidth) }
                var sumRed = 0
                var sumGreen = 0
                var sumBlue = 0
                val childRed = childColors[0]
                val childGreen = childColors[1]
                val childBlue = childColors[2]
                var col = -radiusX
                while (col <= radiusX) {
                    val wrappedCol = col and TextureRaster.rowEnd
                    sumRed += childRed[wrappedCol]
                    sumGreen += childGreen[wrappedCol]
                    sumBlue += childBlue[wrappedCol]
                    col++
                }
                val blurRed = blurredRow[0]
                val blurGreen = blurredRow[1]
                val blurBlue = blurredRow[2]
                var idx: Int
                var x = 0
                while (x < TextureRaster.currentWidth) {
                    blurRed[x] = horizontalScale * sumRed shr 16
                    blurGreen[x] = horizontalScale * sumGreen shr 16
                    blurBlue[x] = sumBlue * horizontalScale shr 16
                    idx = x - radiusX and TextureRaster.rowEnd
                    sumRed -= childRed[idx]
                    sumGreen -= childGreen[idx]
                    sumBlue -= childBlue[idx]
                    ++x
                    idx = x + radiusX and TextureRaster.rowEnd
                    sumRed += childRed[idx]
                    sumGreen += childGreen[idx]
                    sumBlue += childBlue[idx]
                }
                rowBuffers[row + radiusY - y] = blurredRow
                row++
            }
            val outRedPixels = palette[0]
            val outGreenPixels = palette[1]
            val outBluePixels = palette[2]
            var x = 0
            while (x < TextureRaster.currentWidth) {
                var accRed = 0
                var accGreen = 0
                var accBlue = 0
                for (k in 0 until kernelHeight) {
                    val bufferedRow = rowBuffers[k]!!
                    accRed += bufferedRow[0][x]
                    accGreen += bufferedRow[1][x]
                    accBlue += bufferedRow[2][x]
                }
                outRedPixels[x] = accRed * verticalScale shr 16
                outGreenPixels[x] = verticalScale * accGreen shr 16
                outBluePixels[x] = verticalScale * accBlue shr 16
                x++
            }
        }
        return palette
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> radiusX = buffer.readUByte()
            1 -> radiusY = buffer.readUByte()
            2 -> monochrome = buffer.readUByte() == 1
        }
    }
}
