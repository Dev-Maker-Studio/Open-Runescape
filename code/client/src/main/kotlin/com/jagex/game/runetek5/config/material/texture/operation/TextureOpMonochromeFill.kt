package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.utils.ArrayUtils

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpMonochromeFill : TextureOperation(0, true) {

    @JvmField var fillValue: Int = 4096

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            ArrayUtils.setVariables(output, 0, TextureRaster.currentWidth, this.fillValue)
        }
        return output
    }

    override fun decodeOpcode(opcode: Int, stream: Packet) {
        when (opcode) {
            0 -> this.fillValue = (stream.readUByte() shl 12) / 255
        }
    }
}
