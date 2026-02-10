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

    @JvmField var redMultiplier: Int = 4096
    @JvmField var greenMultiplier: Int = 4096
    @JvmField var blueMultiplier: Int = 4096

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
                    outputRed[i] = red * redMultiplier shr 12
                    outputGreen[i] = green * greenMultiplier shr 12
                    outputBlue[i] = blue * blueMultiplier shr 12
                } else {
                    outputRed[i] = redMultiplier
                    outputGreen[i] = greenMultiplier
                    outputBlue[i] = blueMultiplier
                }
            }
        }

        return palette
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> redMultiplier = buffer.readUShort()
            1 -> greenMultiplier = buffer.readUShort()
            2 -> blueMultiplier = buffer.readUShort()
        }
    }
}
