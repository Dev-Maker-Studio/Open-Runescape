package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpIrregularBricks : TextureOperation(0, true) {

    var randomSeed: Int = 0
    var minBrickWidth: Int = 1024
    var maxBrickWidth: Int = 2048
    var minBrickHeight: Int = 409
    var maxBrickHeight: Int = 819
    var offsetVariation: Int = 1024
    var cornerMode: Int = 0
    var heightVariationMultiplier: Int = 1024
    var brightnessVariation: Int = 1024
    var cornerRadius: Int = 0

    fun drawBrick(rng: Random, pixelRows: Array<IntArray?>, startX: Int, startY: Int, brickWidth: Int, brickHeight: Int) {
        val brightness = if (brightnessVariation > 0) 4096 - StaticMethods.boundedRandom(rng, brightnessVariation) else 4096
        val cornerVariation = heightVariationMultiplier * cornerRadius shr 12
        val effectiveCornerRadius = cornerRadius - if (cornerVariation > 0) StaticMethods.boundedRandom(rng, cornerVariation) else 0
        var wrappedStartX = startX
        if (wrappedStartX >= TextureRaster.currentWidth) {
            wrappedStartX -= TextureRaster.currentWidth
        }
        var halfWidth: Int
        var halfHeight: Int
        if (effectiveCornerRadius > 0) {
            if (brickHeight > 0 && brickWidth > 0) {
                halfWidth = brickWidth / 2
                halfHeight = brickHeight / 2
                val cornerW = min(halfWidth, effectiveCornerRadius)
                val cornerH = min(halfHeight, effectiveCornerRadius)
                val innerStartX = wrappedStartX + cornerW
                val innerWidth = brickWidth - cornerW * 2
                for (row in 0 until brickHeight) {
                    val rowPixels = pixelRows[row + startY]!!
                    var fadeValue: Int
                    var colIdx: Int
                    var cornerBrightness: Int
                    if (row < cornerH) {
                        fadeValue = brightness * row / cornerH
                        if (cornerMode == 0) {
                            colIdx = 0
                            while (colIdx < cornerW) {
                                cornerBrightness = brightness * colIdx / cornerW
                                rowPixels[(wrappedStartX + colIdx) and TextureRaster.rowEnd] = cornerBrightness * fadeValue shr 12
                                rowPixels[(wrappedStartX + brickWidth - colIdx - 1) and TextureRaster.rowEnd] = cornerBrightness * fadeValue shr 12
                                colIdx++
                            }
                        } else {
                            colIdx = 0
                            while (colIdx < cornerW) {
                                cornerBrightness = colIdx * brightness / cornerW
                                rowPixels[(wrappedStartX + colIdx) and TextureRaster.rowEnd] = min(cornerBrightness, fadeValue)
                                rowPixels[(wrappedStartX + brickWidth - colIdx - 1) and TextureRaster.rowEnd] = min(cornerBrightness, fadeValue)
                                colIdx++
                            }
                        }
                        if (innerWidth + innerStartX > TextureRaster.currentWidth) {
                            colIdx = TextureRaster.currentWidth - innerStartX
                            ArrayUtils.setVariables(rowPixels, innerStartX, colIdx, fadeValue)
                            ArrayUtils.setVariables(rowPixels, 0, innerWidth - colIdx, fadeValue)
                        } else {
                            ArrayUtils.setVariables(rowPixels, innerStartX, innerWidth, fadeValue)
                        }
                    } else {
                        fadeValue = brickHeight - row - 1
                        if (fadeValue < cornerH) {
                            colIdx = brightness * fadeValue / cornerH
                            var cornerCol: Int
                            if (cornerMode == 0) {
                                cornerBrightness = 0
                                while (cornerBrightness < cornerW) {
                                    cornerCol = cornerBrightness * brightness / cornerW
                                    rowPixels[(wrappedStartX + cornerBrightness) and TextureRaster.rowEnd] = colIdx * cornerCol shr 12
                                    rowPixels[(wrappedStartX + brickWidth - cornerBrightness - 1) and TextureRaster.rowEnd] = colIdx * cornerCol shr 12
                                    cornerBrightness++
                                }
                            } else {
                                cornerBrightness = 0
                                while (cornerBrightness < cornerW) {
                                    cornerCol = cornerBrightness * brightness / cornerW
                                    rowPixels[(wrappedStartX + cornerBrightness) and TextureRaster.rowEnd] = min(cornerCol, colIdx)
                                    rowPixels[(wrappedStartX + brickWidth - cornerBrightness - 1) and TextureRaster.rowEnd] = min(cornerCol, colIdx)
                                    cornerBrightness++
                                }
                            }
                            if (innerWidth + innerStartX > TextureRaster.currentWidth) {
                                cornerBrightness = TextureRaster.currentWidth - innerStartX
                                ArrayUtils.setVariables(rowPixels, innerStartX, cornerBrightness, colIdx)
                                ArrayUtils.setVariables(rowPixels, 0, innerWidth - cornerBrightness, colIdx)
                            } else {
                                ArrayUtils.setVariables(rowPixels, innerStartX, innerWidth, colIdx)
                            }
                        } else {
                            colIdx = 0
                            while (colIdx < cornerW) {
                                rowPixels[(wrappedStartX + colIdx) and TextureRaster.rowEnd] = colIdx * brightness / cornerW
                                rowPixels[(wrappedStartX + brickWidth - colIdx - 1) and TextureRaster.rowEnd] = colIdx * brightness / cornerW
                                colIdx++
                            }
                            if (innerStartX + innerWidth > TextureRaster.currentWidth) {
                                colIdx = TextureRaster.currentWidth - innerStartX
                                ArrayUtils.setVariables(rowPixels, innerStartX, colIdx, brightness)
                                ArrayUtils.setVariables(rowPixels, 0, innerWidth - colIdx, brightness)
                            } else {
                                ArrayUtils.setVariables(rowPixels, innerStartX, innerWidth, brightness)
                            }
                        }
                    }
                }
            }
        } else if (wrappedStartX + brickWidth > TextureRaster.currentWidth) {
            halfWidth = TextureRaster.currentWidth - wrappedStartX
            halfHeight = 0
            while (halfHeight < brickHeight) {
                val rowPixels = pixelRows[halfHeight + startY]!!
                ArrayUtils.setVariables(rowPixels, wrappedStartX, halfWidth, brightness)
                ArrayUtils.setVariables(rowPixels, 0, brickWidth - halfWidth, brightness)
                halfHeight++
            }
        } else {
            halfWidth = 0
            while (halfWidth < brickHeight) {
                ArrayUtils.setVariables(pixelRows[halfWidth + startY]!!, wrappedStartX, brickWidth, brightness)
                halfWidth++
            }
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> randomSeed = buffer.readUByte()
            1 -> minBrickWidth = buffer.readUShort()
            2 -> maxBrickWidth = buffer.readUShort()
            3 -> minBrickHeight = buffer.readUShort()
            4 -> maxBrickHeight = buffer.readUShort()
            5 -> offsetVariation = buffer.readUShort()
            6 -> cornerMode = buffer.readUByte()
            7 -> heightVariationMultiplier = buffer.readUShort()
            8 -> brightnessVariation = buffer.readUShort()
        }
    }

    override fun postDecode() {
    }

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            val allPaletteData = super.monochromeImageCache.getAllPaletteData()
            var prevRowOffset = 0
            var currentRowOffset = 0
            var prevPrevRowOffset = 0
            var brickEndX = 0
            var prevRowSlotIdx = 0
            var isFirstRow = true
            var isFirstColumn = true
            var prevRowSlotCount = 0
            var currentSlotCount = 0
            val minWidthPixels = minBrickWidth * TextureRaster.currentWidth shr 12
            val maxWidthPixels = maxBrickWidth * TextureRaster.currentWidth shr 12
            val minHeightPixels = minBrickHeight * TextureRaster.currentHeight shr 12
            val maxHeightPixels = maxBrickHeight * TextureRaster.currentHeight shr 12
            if (maxHeightPixels <= 1) {
                return allPaletteData[y]!!
            } else {
                cornerRadius = TextureRaster.currentWidth / 8 * offsetVariation shr 12
                val maxSlotsPerRow = TextureRaster.currentWidth / minWidthPixels + 1
                val currentRowSlots = Array(maxSlotsPerRow) { IntArray(3) }
                var prevRowSlots = Array(maxSlotsPerRow) { IntArray(3) }
                val rng = Random(randomSeed.toLong())
                while (true) {
                    var brickWidth = minWidthPixels + StaticMethods.boundedRandom(rng, maxWidthPixels - minWidthPixels)
                    var brickHeight = minHeightPixels + StaticMethods.boundedRandom(rng, maxHeightPixels - minHeightPixels)
                    var nextEndX = brickWidth + brickEndX
                    if (nextEndX > TextureRaster.currentWidth) {
                        brickWidth = TextureRaster.currentWidth - brickEndX
                        nextEndX = TextureRaster.currentWidth
                    }
                    var brickStartY: Int
                    var searchCount: Int
                    if (isFirstColumn) {
                        brickStartY = 0
                    } else {
                        var slotIdx = prevRowSlotIdx
                        val prevSlot = prevRowSlots[prevRowSlotIdx]
                        searchCount = 0
                        var wrappedEndX = nextEndX + prevRowOffset
                        if (wrappedEndX < 0) {
                            wrappedEndX += TextureRaster.currentWidth
                        }
                        if (wrappedEndX > TextureRaster.currentWidth) {
                            wrappedEndX -= TextureRaster.currentWidth
                        }
                        while (true) {
                            val slot = prevRowSlots[slotIdx]
                            if (wrappedEndX >= slot[0] && wrappedEndX <= slot[1]) {
                                brickStartY = prevSlot[2]
                                if (slotIdx != prevRowSlotIdx) {
                                    var wrappedStartX = brickEndX + prevRowOffset
                                    if (wrappedStartX < 0) {
                                        wrappedStartX += TextureRaster.currentWidth
                                    }
                                    if (wrappedStartX > TextureRaster.currentWidth) {
                                        wrappedStartX -= TextureRaster.currentWidth
                                    }
                                    var gapIdx: Int
                                    var gapSlot: IntArray
                                    gapIdx = 1
                                    while (gapIdx <= searchCount) {
                                        gapSlot = prevRowSlots[(gapIdx + prevRowSlotIdx) % prevRowSlotCount]
                                        brickStartY = max(brickStartY, gapSlot[2])
                                        gapIdx++
                                    }
                                    gapIdx = 0
                                    while (gapIdx <= searchCount) {
                                        gapSlot = prevRowSlots[(gapIdx + prevRowSlotIdx) % prevRowSlotCount]
                                        val gapTop = gapSlot[2]
                                        if (gapTop != brickStartY) {
                                            val gapLeft = gapSlot[0]
                                            val gapRight = gapSlot[1]
                                            val fillLeft: Int
                                            val fillRight: Int
                                            if (wrappedStartX < wrappedEndX) {
                                                fillLeft = max(wrappedStartX, gapLeft)
                                                fillRight = min(wrappedEndX, gapRight)
                                            } else if (gapLeft == 0) {
                                                fillLeft = 0
                                                fillRight = min(wrappedEndX, gapRight)
                                            } else {
                                                fillLeft = max(wrappedStartX, gapLeft)
                                                fillRight = TextureRaster.currentWidth
                                            }
                                            drawBrick(rng, allPaletteData, fillLeft + prevPrevRowOffset, gapTop, fillRight - fillLeft, brickStartY - gapTop)
                                        }
                                        gapIdx++
                                    }
                                }
                                prevRowSlotIdx = slotIdx
                                break
                            }
                            ++slotIdx
                            if (slotIdx >= prevRowSlotCount) {
                                slotIdx = 0
                            }
                            ++searchCount
                        }
                    }
                    if (brickStartY + brickHeight > TextureRaster.currentHeight) {
                        brickHeight = TextureRaster.currentHeight - brickStartY
                    } else {
                        isFirstRow = false
                    }
                    val newSlot: IntArray
                    if (nextEndX == TextureRaster.currentWidth) {
                        drawBrick(rng, allPaletteData, currentRowOffset + brickEndX, brickStartY, brickWidth, brickHeight)
                        if (isFirstRow) {
                            return output
                        }
                        isFirstRow = true
                        newSlot = currentRowSlots[currentSlotCount++]
                        newSlot[0] = brickEndX
                        newSlot[1] = nextEndX
                        newSlot[2] = brickHeight + brickStartY
                        val swapSlots = prevRowSlots
                        prevRowSlots = currentRowSlots
                        val newCurrentSlots = swapSlots
                        prevRowSlotCount = currentSlotCount
                        currentSlotCount = 0
                        prevPrevRowOffset = currentRowOffset
                        currentRowOffset = StaticMethods.boundedRandom(rng, TextureRaster.currentWidth)
                        prevRowOffset = currentRowOffset - prevPrevRowOffset
                        brickEndX = 0
                        searchCount = prevRowOffset
                        if (prevRowOffset < 0) {
                            searchCount = prevRowOffset + TextureRaster.currentWidth
                        }
                        if (searchCount > TextureRaster.currentWidth) {
                            searchCount -= TextureRaster.currentWidth
                        }
                        prevRowSlotIdx = 0
                        while (true) {
                            val searchSlot = prevRowSlots[prevRowSlotIdx]
                            if (searchCount >= searchSlot[0] && searchCount <= searchSlot[1]) {
                                isFirstColumn = false
                                break
                            }
                            ++prevRowSlotIdx
                            if (prevRowSlotIdx >= prevRowSlotCount) {
                                prevRowSlotIdx = 0
                            }
                        }
                    } else {
                        newSlot = currentRowSlots[currentSlotCount++]
                        newSlot[0] = brickEndX
                        newSlot[1] = nextEndX
                        newSlot[2] = brickHeight + brickStartY
                        drawBrick(rng, allPaletteData, currentRowOffset + brickEndX, brickStartY, brickWidth, brickHeight)
                        brickEndX = nextEndX
                    }
                }
            }
        } else {
            return output
        }
    }

    companion object {
        @JvmField
        var backgroundColor: Int = 0
    }
}
