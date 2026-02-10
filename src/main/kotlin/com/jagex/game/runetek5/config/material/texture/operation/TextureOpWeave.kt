package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import kotlin.math.abs

/**
 * Texture operation that generates weave/fabric patterns.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpWeave : TextureOperation(0, true) {

    @JvmField var threadWidth: Int = 585

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> threadWidth = buffer.readUShort()
        }
    }

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val output = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val verticalValue = TextureRaster.verticalTable!![i_1]
            for (i in 0 until TextureRaster.currentWidth) {
                val horizontalValue = TextureRaster.horizontalTable!![i]
                val result: Int
                if (horizontalValue > threadWidth && horizontalValue < 4096 - threadWidth &&
                    verticalValue > 2048 - threadWidth && verticalValue < threadWidth + 2048) {
                    result = abs(2048 - horizontalValue)
                    val scaled = result shl 12
                    output[i] = 4096 - scaled / (2048 - threadWidth)
                } else if (horizontalValue > 2048 - threadWidth && horizontalValue < threadWidth + 2048) {
                    result = abs(verticalValue - 2048) - threadWidth
                    val scaled = result shl 12
                    output[i] = scaled / (2048 - threadWidth)
                } else if (verticalValue >= threadWidth && verticalValue <= 4096 - threadWidth) {
                    if (horizontalValue >= threadWidth && horizontalValue <= 4096 - threadWidth) {
                        output[i] = 0
                    } else {
                        result = abs(2048 - verticalValue)
                        val scaled = result shl 12
                        output[i] = 4096 - scaled / (2048 - threadWidth)
                    }
                } else {
                    result = abs(horizontalValue - 2048) - threadWidth
                    val scaled = result shl 12
                    output[i] = scaled / (2048 - threadWidth)
                }
            }
        }
        return output
    }
}
