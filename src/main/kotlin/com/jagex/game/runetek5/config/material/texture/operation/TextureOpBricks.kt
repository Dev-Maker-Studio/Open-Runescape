package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils
import java.util.Random

/**
 * Texture operation that generates brick patterns.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
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

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            var i_4 = 0

            var i_5 = TextureRaster.verticalTable!![i_1] + verticalOffset
            while (i_5 < 0) {
                i_5 += 4096
            }

            while (i_5 > 4096) {
                i_5 -= 4096
            }

            while (i_4 < rowCount && i_5 >= rowBoundaries!![i_4]) {
                ++i_4
            }

            val i_6 = i_4 - 1
            val bool7 = (i_4 and 0x1) == 0
            val i_8 = rowBoundaries!![i_4]
            val i_9 = rowBoundaries!![i_4 - 1]

            if (i_5 > i_9 + halfMortarThickness && i_5 < i_8 - halfMortarThickness) {
                for (i_10 in 0 until TextureRaster.currentWidth) {
                    var i_11 = 0
                    val i_12 = if (bool7) staggerAmount else -staggerAmount

                    var i_13 = TextureRaster.horizontalTable!![i_10] + (i_12 * columnWidth shr 12)
                    while (i_13 < 0) {
                        i_13 += 4096
                    }

                    while (i_13 > 4096) {
                        i_13 -= 4096
                    }

                    while (i_11 < columnsPerRow && i_13 >= columnBoundaries!![i_6][i_11]) {
                        ++i_11
                    }

                    val i_14 = i_11 - 1
                    val i_15 = columnBoundaries!![i_6][i_14]
                    val i_16 = columnBoundaries!![i_6][i_11]

                    if (i_13 > i_15 + halfMortarThickness && i_13 < i_16 - halfMortarThickness) {
                        ints_3[i_10] = brickBrightness!![i_6][i_14]
                    } else {
                        ints_3[i_10] = 0
                    }
                }
            } else {
                ArrayUtils.setVariables(ints_3, 0, TextureRaster.currentWidth, 0)
            }
        }

        return ints_3
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
        val random2 = Random(rowCount.toLong())
        halfMortarThickness = mortarThickness / 2
        columnWidth = 4096 / columnsPerRow
        rowHeight = 4096 / rowCount
        val i_3 = columnWidth / 2
        val i_4 = rowHeight / 2
        rowBoundaries = IntArray(rowCount + 1)
        columnBoundaries = Array(rowCount) { IntArray(columnsPerRow + 1) }
        brickBrightness = Array(rowCount) { IntArray(columnsPerRow) }
        rowBoundaries!![0] = 0

        for (i_5 in 0 until rowCount) {
            var i_6: Int
            var i_7: Int
            if (i_5 > 0) {
                i_6 = rowHeight
                i_7 = (StaticMethods.boundedRandom(random2, 4096) - 2048) * rowHeightVariation shr 12
                val adjusted = i_6 + (i_7 * i_4 shr 12)
                rowBoundaries!![i_5] = adjusted + rowBoundaries!![i_5 - 1]
            }

            columnBoundaries!![i_5][0] = 0

            for (i_6_2 in 0 until columnsPerRow) {
                if (i_6_2 > 0) {
                    i_7 = columnWidth
                    val i_8 = (StaticMethods.boundedRandom(random2, 4096) - 2048) * columnWidthVariation shr 12
                    val adjusted = i_7 + (i_8 * i_3 shr 12)
                    columnBoundaries!![i_5][i_6_2] = columnBoundaries!![i_5][i_6_2 - 1] + adjusted
                }

                brickBrightness!![i_5][i_6_2] = if (brightnessVariation > 0) {
                    4096 - StaticMethods.boundedRandom(random2, brightnessVariation)
                } else {
                    4096
                }
            }

            columnBoundaries!![i_5][columnsPerRow] = 4096
        }

        rowBoundaries!![rowCount] = 4096
    }

    override fun postDecode() {
        generateBrickLayout()
    }
}
