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
class TextureOpBinary : TextureOperation(1, true) {

    @JvmField var lowerThreshold: Int = 0
    @JvmField var upperThreshold: Int = 4096

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            val childPixels = getChildMonochromeOutput(0, y)

            for (x in 0 until TextureRaster.currentWidth) {
                val value = childPixels[x]
                output[x] = if (value >= this.lowerThreshold && value <= this.upperThreshold) 4096 else 0
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> this.lowerThreshold = buffer.readUShort()
            1 -> this.upperThreshold = buffer.readUShort()
        }
    }
}
