package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 * Texture operation that colorizes grayscale textures with RGB values.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpColorize : TextureOperation(1, false) {

    @JvmField var anInt9971: Int = 4096
    @JvmField var anInt9970: Int = 4096
    @JvmField var anInt9968: Int = 4096

    override fun getColorOutput(i_1: Int): Array<IntArray> {
        val palette = colorImageCache.getPalette(i_1)
        if (colorImageCache.dirty) {
            val childOutput = getChildColorOutput(0, i_1)
            val inputRed = childOutput[0]
            val inputGreen = childOutput[1]
            val inputBlue = childOutput[2]
            val outputRed = palette[0]
            val outputGreen = palette[1]
            val outputBlue = palette[2]

            for (i in 0 until TextureRaster.currentWidth) {
                val red = inputRed[i]
                val green = inputGreen[i]
                val blue = inputBlue[i]
                if (red == green && green == blue) {
                    outputRed[i] = red * anInt9971 shr 12
                    outputGreen[i] = green * anInt9970 shr 12
                    outputBlue[i] = blue * anInt9968 shr 12
                } else {
                    outputRed[i] = anInt9971
                    outputGreen[i] = anInt9970
                    outputBlue[i] = anInt9968
                }
            }
        }

        return palette
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> anInt9971 = buffer.readUShort()
            1 -> anInt9970 = buffer.readUShort()
            2 -> anInt9968 = buffer.readUShort()
        }
    }
}
