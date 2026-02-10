package com.jagex.game.runetek5.config.material.texture.operation

import java.util.Random

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
class TextureOpLineNoise : TextureOperation(0, true) {

    @JvmField var randomSeed = 0
    @JvmField var lineCount = 2000
    @JvmField var lineLength = 16
    @JvmField var angleOffset = 0
    @JvmField var angleSpread = 4096

    override fun postDecode() {
        TextureRaster.caculateTable()
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> randomSeed = buffer.readUByte()
            1 -> lineCount = buffer.readUShort()
            2 -> lineLength = buffer.readUByte()
            3 -> angleOffset = buffer.readUShort()
            4 -> angleSpread = buffer.readUShort()
        }
    }

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val halfSpread = angleSpread shr 1
            val allPaletteData = monochromeImageCache.getAllPaletteData()
            val rng = Random(randomSeed.toLong())

            for (line in 0 until lineCount) {
                var angle = if (angleSpread > 0) angleOffset + (StaticMethods.boundedRandom(rng, angleSpread) - halfSpread) else angleOffset
                angle = angle shr 4 and 0xff
                val startX = StaticMethods.boundedRandom(rng, TextureRaster.currentWidth)
                val startY = StaticMethods.boundedRandom(rng, TextureRaster.currentHeight)
                val endX = startX + (TextureRaster.PALETTE_COS!![angle] * lineLength shr 12)
                val endY = startY + (TextureRaster.PALETTE_SIN!![angle] * lineLength shr 12)
                val dy = endY - startY
                val dx = endX - startX
                if (dx != 0 || dy != 0) {
                    var absDy = if (dy < 0) -dy else dy
                    var absDx = if (dx < 0) -dx else dx

                    val isSteep = absDy > absDx
                    var x1 = startX
                    var y1 = startY
                    var x2 = endX
                    var y2 = endY
                    if (isSteep) {
                        x1 = startY
                        y1 = startX
                        x2 = endY
                        y2 = endX
                    }

                    if (x1 > x2) {
                        val tempX = x1
                        val tempY = y1
                        x1 = x2
                        x2 = tempX
                        y1 = y2
                        y2 = tempY
                    }

                    var currentY = y1
                    val deltaX = x2 - x1
                    var deltaY = y2 - y1
                    var error = -deltaX / 2
                    val brightnessStep = 2048 / deltaX
                    val baseBrightness = 1024 - (StaticMethods.boundedRandom(rng, 4096) shr 2)
                    val yStep = if (y1 < y2) 1 else -1
                    if (deltaY < 0) {
                        deltaY = -deltaY
                    }

                    for (px in x1 until x2) {
                        val brightness = baseBrightness + brightnessStep * (px - x1) + 1024
                        val wrappedX = px and TextureRaster.rowEnd
                        val wrappedY = currentY and TextureRaster.columnEnd
                        if (isSteep) {
                            allPaletteData[wrappedY]!![wrappedX] = brightness
                        } else {
                            allPaletteData[wrappedX]!![wrappedY] = brightness
                        }

                        error += deltaY
                        if (error > 0) {
                            error -= deltaX
                            currentY += yStep
                        }
                    }
                }
            }
        }

        return output
    }
}
