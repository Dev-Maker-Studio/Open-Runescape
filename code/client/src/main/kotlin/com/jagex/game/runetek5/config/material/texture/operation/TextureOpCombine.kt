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
class TextureOpCombine : TextureOperation(2, false) {

    @JvmField
    var blendMode: Int = 6

    override fun getMonochromeOutput(y: Int): IntArray {
        val outputPixels = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            val inputPixelsA = getChildMonochromeOutput(0, y)
            val inputPixelsB = getChildMonochromeOutput(1, y)
            var x: Int
            var valueB: Int
            var valueA: Int
            when (this.blendMode) {
                1 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        outputPixels[x] = inputPixelsB[x] + inputPixelsA[x]
                        x++
                    }
                    return outputPixels
                }
                2 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        outputPixels[x] = inputPixelsA[x] - inputPixelsB[x]
                        x++
                    }
                    return outputPixels
                }
                3 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        outputPixels[x] = inputPixelsA[x] * inputPixelsB[x] shr 12
                        x++
                    }
                    return outputPixels
                }
                4 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        valueB = inputPixelsB[x]
                        outputPixels[x] = if (valueB != 0) (inputPixelsA[x] shl 12) / valueB else 4096
                        x++
                    }
                    return outputPixels
                }
                5 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        outputPixels[x] = 4096 - ((4096 - inputPixelsA[x]) * (4096 - inputPixelsB[x]) shr 12)
                        x++
                    }
                    return outputPixels
                }
                6 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        valueB = inputPixelsB[x]
                        outputPixels[x] = if (valueB < 2048) valueB * inputPixelsA[x] shr 11 else 4096 - ((4096 - inputPixelsA[x]) * (4096 - valueB) shr 11)
                        x++
                    }
                    return outputPixels
                }
                7 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        valueA = inputPixelsA[x]
                        outputPixels[x] = if (valueA == 4096) 4096 else (inputPixelsB[x] shl 12) / (4096 - valueA)
                        x++
                    }
                    return outputPixels
                }
                8 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        valueA = inputPixelsA[x]
                        outputPixels[x] = if (valueA == 0) 0 else 4096 - (4096 - inputPixelsB[x] shl 12) / valueA
                        x++
                    }
                    return outputPixels
                }
                9 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        valueA = inputPixelsA[x]
                        valueB = inputPixelsB[x]
                        outputPixels[x] = Math.min(valueA, valueB)
                        x++
                    }
                    return outputPixels
                }
                10 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        valueA = inputPixelsA[x]
                        valueB = inputPixelsB[x]
                        outputPixels[x] = Math.max(valueA, valueB)
                        x++
                    }
                    return outputPixels
                }
                11 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        valueA = inputPixelsA[x]
                        valueB = inputPixelsB[x]
                        outputPixels[x] = if (valueA > valueB) valueA - valueB else valueB - valueA
                        x++
                    }
                    return outputPixels
                }
                12 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        valueA = inputPixelsA[x]
                        valueB = inputPixelsB[x]
                        outputPixels[x] = valueB + valueA - (valueA * valueB shr 11)
                        x++
                    }
                }
            }
        }
        return outputPixels
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val palette = super.colorImageCache.getPalette(y)
        if (super.colorImageCache.dirty) {
            val childColorsA = getChildColorOutput(0, y)
            val childColorsB = getChildColorOutput(1, y)
            val outRed = palette[0]
            val outGreen = palette[1]
            val outBlue = palette[2]
            val inRedA = childColorsA[0]
            val inGreenA = childColorsA[1]
            val inBlueA = childColorsA[2]
            val inRedB = childColorsB[0]
            val inGreenB = childColorsB[1]
            val inBlueB = childColorsB[2]
            var x: Int
            var redB: Int
            var greenB: Int
            var blueB: Int
            var redA: Int
            var greenA: Int
            var blueA: Int
            when (this.blendMode) {
                1 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        outRed[x] = inRedA[x] + inRedB[x]
                        outGreen[x] = inGreenA[x] + inGreenB[x]
                        outBlue[x] = inBlueB[x] + inBlueA[x]
                        x++
                    }
                    return palette
                }
                2 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        outRed[x] = inRedA[x] - inRedB[x]
                        outGreen[x] = inGreenA[x] - inGreenB[x]
                        outBlue[x] = inBlueA[x] - inBlueB[x]
                        x++
                    }
                    return palette
                }
                3 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        outRed[x] = inRedB[x] * inRedA[x] shr 12
                        outGreen[x] = inGreenA[x] * inGreenB[x] shr 12
                        outBlue[x] = inBlueA[x] * inBlueB[x] shr 12
                        x++
                    }
                    return palette
                }
                4 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        redB = inRedB[x]
                        greenB = inGreenB[x]
                        blueB = inBlueB[x]
                        outRed[x] = if (redB != 0) (inRedA[x] shl 12) / redB else 4096
                        outGreen[x] = if (greenB != 0) (inGreenA[x] shl 12) / greenB else 4096
                        outBlue[x] = if (blueB != 0) (inBlueA[x] shl 12) / blueB else 4096
                        x++
                    }
                    return palette
                }
                5 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        outRed[x] = 4096 - ((4096 - inRedB[x]) * (4096 - inRedA[x]) shr 12)
                        outGreen[x] = 4096 - ((4096 - inGreenA[x]) * (4096 - inGreenB[x]) shr 12)
                        outBlue[x] = 4096 - ((4096 - inBlueA[x]) * (4096 - inBlueB[x]) shr 12)
                        x++
                    }
                    return palette
                }
                6 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        redB = inRedB[x]
                        greenB = inGreenB[x]
                        blueB = inBlueB[x]
                        outRed[x] = if (redB < 2048) redB * inRedA[x] shr 11 else 4096 - ((4096 - inRedA[x]) * (4096 - redB) shr 11)
                        outGreen[x] = if (greenB < 2048) greenB * inGreenA[x] shr 11 else 4096 - ((4096 - inGreenA[x]) * (4096 - greenB) shr 11)
                        outBlue[x] = if (blueB < 2048) blueB * inBlueA[x] shr 11 else 4096 - ((4096 - inBlueA[x]) * (4096 - blueB) shr 11)
                        x++
                    }
                    return palette
                }
                7 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        redA = inRedA[x]
                        greenA = inGreenA[x]
                        blueA = inBlueA[x]
                        outRed[x] = if (redA == 4096) 4096 else (inRedB[x] shl 12) / (4096 - redA)
                        outGreen[x] = if (greenA == 4096) 4096 else (inGreenB[x] shl 12) / (4096 - greenA)
                        outBlue[x] = if (blueA == 4096) 4096 else (inBlueB[x] shl 12) / (4096 - blueA)
                        x++
                    }
                    return palette
                }
                8 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        redA = inRedA[x]
                        greenA = inGreenA[x]
                        blueA = inBlueA[x]
                        outRed[x] = if (redA == 0) 0 else 4096 - (4096 - inRedB[x] shl 12) / redA
                        outGreen[x] = if (greenA == 0) 0 else 4096 - (4096 - inGreenB[x] shl 12) / greenA
                        outBlue[x] = if (blueA == 0) 0 else 4096 - (4096 - inBlueB[x] shl 12) / blueA
                        x++
                    }
                    return palette
                }
                9 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        redA = inRedA[x]
                        greenA = inGreenA[x]
                        blueA = inBlueA[x]
                        redB = inRedB[x]
                        greenB = inGreenB[x]
                        blueB = inBlueB[x]
                        outRed[x] = Math.min(redA, redB)
                        outGreen[x] = Math.min(greenA, greenB)
                        outBlue[x] = Math.min(blueA, blueB)
                        x++
                    }
                    return palette
                }
                10 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        redA = inRedA[x]
                        greenA = inGreenA[x]
                        blueA = inBlueA[x]
                        redB = inRedB[x]
                        greenB = inGreenB[x]
                        blueB = inBlueB[x]
                        outRed[x] = Math.max(redA, redB)
                        outGreen[x] = Math.max(greenA, greenB)
                        outBlue[x] = Math.max(blueA, blueB)
                        x++
                    }
                    return palette
                }
                11 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        redA = inRedA[x]
                        greenA = inGreenA[x]
                        blueA = inBlueA[x]
                        redB = inRedB[x]
                        greenB = inGreenB[x]
                        blueB = inBlueB[x]
                        outRed[x] = if (redA > redB) redA - redB else redB - redA
                        outGreen[x] = if (greenA > greenB) greenA - greenB else greenB - greenA
                        outBlue[x] = if (blueA > blueB) blueA - blueB else blueB - blueA
                        x++
                    }
                    return palette
                }
                12 -> {
                    x = 0
                    while (x < TextureRaster.currentWidth) {
                        redA = inRedA[x]
                        greenA = inGreenA[x]
                        blueA = inBlueA[x]
                        redB = inRedB[x]
                        greenB = inGreenB[x]
                        blueB = inBlueB[x]
                        outRed[x] = redA + redB - (redB * redA shr 11)
                        outGreen[x] = greenB + greenA - (greenB * greenA shr 11)
                        outBlue[x] = blueB + blueA - (blueB * blueA shr 11)
                        x++
                    }
                }
            }
        }
        return palette
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> this.blendMode = buffer.readUByte()
            1 -> super.monochrome = buffer.readUByte() == 1
        }
    }
}
