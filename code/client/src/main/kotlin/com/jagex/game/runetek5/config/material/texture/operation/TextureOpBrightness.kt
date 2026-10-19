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
class TextureOpBrightness : TextureOperation(1, false) {

    @JvmField var blueBrightness: Int = 4096
    @JvmField var greenBrightness: Int = 4096
    @JvmField var redBrightness: Int = 4096
    @JvmField var colorTolerance: Int = 409
    @JvmField val targetColor: IntArray = IntArray(3)

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val childOutput = getChildColorOutput(0, y)
            val childRed = childOutput[0]
            val childGreen = childOutput[1]
            val childBlue = childOutput[2]
            val outputRed = output[0]
            val outputGreen = output[1]
            val outputBlue = output[2]

            for (x in 0 until TextureRaster.currentWidth) {
                val red = childRed[x]
                var redDelta = red - targetColor[0]
                if (redDelta < 0) {
                    redDelta = -redDelta
                }

                if (redDelta > colorTolerance) {
                    outputRed[x] = red
                    outputGreen[x] = childGreen[x]
                    outputBlue[x] = childBlue[x]
                } else {
                    val green = childGreen[x]
                    var greenDelta = green - targetColor[1]
                    if (greenDelta < 0) {
                        greenDelta = -greenDelta
                    }

                    if (greenDelta > colorTolerance) {
                        outputRed[x] = red
                        outputGreen[x] = green
                        outputBlue[x] = childBlue[x]
                    } else {
                        val blue = childBlue[x]
                        var blueDelta = blue - targetColor[2]
                        if (blueDelta < 0) {
                            blueDelta = -blueDelta
                        }

                        if (blueDelta > colorTolerance) {
                            outputRed[x] = red
                            outputGreen[x] = green
                            outputBlue[x] = blue
                        } else {
                            outputRed[x] = red * redBrightness shr 12
                            outputGreen[x] = green * greenBrightness shr 12
                            outputBlue[x] = blue * blueBrightness shr 12
                        }
                    }
                }
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> colorTolerance = buffer.readUShort()
            1 -> blueBrightness = buffer.readUShort()
            2 -> greenBrightness = buffer.readUShort()
            3 -> redBrightness = buffer.readUShort()
            4 -> {
                val color = buffer.readUMedium()
                targetColor[0] = (color and 0xff0000) shl 4
                targetColor[1] = (color and 0xff00) shr 4
                targetColor[2] = (color and 0xff) shr 12
            }
        }
    }
}
