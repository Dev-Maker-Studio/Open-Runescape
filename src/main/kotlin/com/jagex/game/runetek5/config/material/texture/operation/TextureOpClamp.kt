package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import kotlin.math.min

/**
 * Texture operation that clamps pixel values to a range.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpClamp : TextureOperation(1, false) {

    @JvmField var minValue: Int = 0
    @JvmField var maxValue: Int = 4096

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val input = getChildColorOutput(0, y)
            val inputRed = input[0]
            val inputGreen = input[1]
            val inputBlue = input[2]
            val outputRed = output[0]
            val outputGreen = output[1]
            val outputBlue = output[2]

            for (x in 0 until TextureRaster.currentWidth) {
                val red = inputRed[x]
                val green = inputGreen[x]
                val blue = inputBlue[x]
                outputRed[x] = if (red < minValue) minValue else min(red, maxValue)
                outputGreen[x] = if (green < minValue) minValue else min(green, maxValue)
                outputBlue[x] = if (blue < minValue) minValue else min(blue, maxValue)
            }
        }
        return output
    }

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val input = getChildMonochromeOutput(0, y)

            for (x in 0 until TextureRaster.currentWidth) {
                val value = input[x]
                output[x] = if (value < minValue) minValue else min(value, maxValue)
            }
        }
        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> minValue = buffer.readUShort()
            1 -> maxValue = buffer.readUShort()
            2 -> monochrome = buffer.readUByte() == 1
        }
    }
}
