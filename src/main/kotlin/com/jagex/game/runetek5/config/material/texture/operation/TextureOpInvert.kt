package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 * Texture operation that inverts RGB color values.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpInvert : TextureOperation(1, false) {

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
                outputRed[i] = 4096 - inputRed[i]
                outputGreen[i] = 4096 - inputGreen[i]
                outputBlue[i] = 4096 - inputBlue[i]
            }
        }

        return palette
    }

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val output = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val input = getChildMonochromeOutput(0, i_1)

            for (i in 0 until TextureRaster.currentWidth) {
                output[i] = 4096 - input[i]
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            monochrome = buffer.readUByte() == 1
        }
    }
}
