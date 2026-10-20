package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils
import java.util.Random

class TextureOpBricks : TextureOperation(0, true) {

    @JvmField var columnsPerRow: Int = 4
    @JvmField var rowCount: Int = 8
    @JvmField var verticalOffset: Int = 0
    @JvmField var staggerAmount: Int = 1024
    @JvmField var brightnessVariation: Int = 1024
    @JvmField var columnWidthVariation: Int = 409
    @JvmField var rowHeightVariation: Int = 204
    @JvmField var mortarThickness: Int = 81
    @JvmField var halfMortarThickness: Int = 0
    @JvmField var columnWidth: Int = 0
    @JvmField var rowHeight: Int = 0
    @JvmField var rowBoundaries: IntArray? = null
    @JvmField var columnBoundaries: Array<IntArray>? = null
    @JvmField var brickBrightness: Array<IntArray>? = null

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            var rowIdx = 0

            var verticalPos = TextureRaster.verticalTable!![y] + verticalOffset
            while (verticalPos < 0) {
                verticalPos += 4096
            }

            while (verticalPos > 4096) {
                verticalPos -= 4096
            }

            while (rowIdx < rowCount && verticalPos >= rowBoundaries!![rowIdx]) {
                ++rowIdx
            }

            val brickRow = rowIdx - 1
            val isEvenRow = (rowIdx and 0x1) == 0
            val rowTop = rowBoundaries!![rowIdx]
            val rowBottom = rowBoundaries!![rowIdx - 1]

            if (verticalPos > rowBottom + halfMortarThickness && verticalPos < rowTop - halfMortarThickness) {
                for (x in 0 until TextureRaster.currentWidth) {
                    var colIdx = 0
                    val stagger = if (isEvenRow) staggerAmount else -staggerAmount

                    var horizontalPos = TextureRaster.horizontalTable!![x] + (stagger * columnWidth shr 12)
                    while (horizontalPos < 0) {
                        horizontalPos += 4096
                    }

                    while (horizontalPos > 4096) {
                        horizontalPos -= 4096
                    }

                    while (colIdx < columnsPerRow && horizontalPos >= columnBoundaries!![brickRow][colIdx]) {
                        ++colIdx
                    }

                    val brickCol = colIdx - 1
                    val colLeft = columnBoundaries!![brickRow][brickCol]
                    val colRight = columnBoundaries!![brickRow][colIdx]

                    if (horizontalPos > colLeft + halfMortarThickness && horizontalPos < colRight - halfMortarThickness) {
                        output[x] = brickBrightness!![brickRow][brickCol]
                    } else {
                        output[x] = 0
                    }
                }
            } else {
                ArrayUtils.setVariables(output, 0, TextureRaster.currentWidth, 0)
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> columnsPerRow = buffer.readUByte()
            1 -> rowCount = buffer.readUByte()
            2 -> columnWidthVariation = buffer.readUShort()
            3 -> rowHeightVariation = buffer.readUShort()
            4 -> staggerAmount = buffer.readUShort()
            5 -> verticalOffset = buffer.readUShort()
            6 -> mortarThickness = buffer.readUShort()
            7 -> brightnessVariation = buffer.readUShort()
        }
    }

    fun generateBrickLayout() {
        val rng = Random(rowCount.toLong())
        halfMortarThickness = mortarThickness / 2
        columnWidth = 4096 / columnsPerRow
        rowHeight = 4096 / rowCount
        val halfColumnWidth = columnWidth / 2
        val halfRowHeight = rowHeight / 2
        rowBoundaries = IntArray(rowCount + 1)
        columnBoundaries = Array(rowCount) { IntArray(columnsPerRow + 1) }
        brickBrightness = Array(rowCount) { IntArray(columnsPerRow) }
        rowBoundaries!![0] = 0

        for (row in 0 until rowCount) {
            var baseHeight: Int
            var variation: Int
            if (row > 0) {
                baseHeight = rowHeight
                variation = (StaticMethods.boundedRandom(rng, 4096) - 2048) * rowHeightVariation shr 12
                val adjusted = baseHeight + (variation * halfRowHeight shr 12)
                rowBoundaries!![row] = adjusted + rowBoundaries!![row - 1]
            }

            columnBoundaries!![row][0] = 0

            for (col in 0 until columnsPerRow) {
                if (col > 0) {
                    variation = columnWidth
                    val widthVariation = (StaticMethods.boundedRandom(rng, 4096) - 2048) * columnWidthVariation shr 12
                    val adjusted = variation + (widthVariation * halfColumnWidth shr 12)
                    columnBoundaries!![row][col] = columnBoundaries!![row][col - 1] + adjusted
                }

                brickBrightness!![row][col] = if (brightnessVariation > 0) {
                    4096 - StaticMethods.boundedRandom(rng, brightnessVariation)
                } else {
                    4096
                }
            }

            columnBoundaries!![row][columnsPerRow] = 4096
        }

        rowBoundaries!![rowCount] = 4096
    }

    override fun postDecode() {
        generateBrickLayout()
    }
}
