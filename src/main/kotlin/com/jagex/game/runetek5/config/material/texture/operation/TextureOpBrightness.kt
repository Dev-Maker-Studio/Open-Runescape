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

    @JvmField var anInt9907: Int = 4096
    @JvmField var anInt9908: Int = 4096
    @JvmField var anInt9909: Int = 4096
    @JvmField var anInt9903: Int = 409
    @JvmField val anIntArray9911: IntArray = IntArray(3)

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
                var redDelta = red - anIntArray9911[0]
                if (redDelta < 0) {
                    redDelta = -redDelta
                }

                if (redDelta > anInt9903) {
                    outputRed[x] = red
                    outputGreen[x] = childGreen[x]
                    outputBlue[x] = childBlue[x]
                } else {
                    val green = childGreen[x]
                    var greenDelta = green - anIntArray9911[1]
                    if (greenDelta < 0) {
                        greenDelta = -greenDelta
                    }

                    if (greenDelta > anInt9903) {
                        outputRed[x] = red
                        outputGreen[x] = green
                        outputBlue[x] = childBlue[x]
                    } else {
                        val blue = childBlue[x]
                        var blueDelta = blue - anIntArray9911[2]
                        if (blueDelta < 0) {
                            blueDelta = -blueDelta
                        }

                        if (blueDelta > anInt9903) {
                            outputRed[x] = red
                            outputGreen[x] = green
                            outputBlue[x] = blue
                        } else {
                            outputRed[x] = red * anInt9909 shr 12
                            outputGreen[x] = green * anInt9908 shr 12
                            outputBlue[x] = blue * anInt9907 shr 12
                        }
                    }
                }
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> anInt9903 = buffer.readUShort()
            1 -> anInt9907 = buffer.readUShort()
            2 -> anInt9908 = buffer.readUShort()
            3 -> anInt9909 = buffer.readUShort()
            4 -> {
                val color = buffer.readUMedium()
                anIntArray9911[0] = (color and 0xff0000) shl 4
                anIntArray9911[1] = (color and 0xff00) shr 4
                anIntArray9911[2] = (color and 0xff) shr 12
            }
        }
    }
}
