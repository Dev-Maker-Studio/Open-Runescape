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

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = super.monochromeImageCache.getPalette(i_1)
        if (super.monochromeImageCache.dirty) {
            ArrayUtils.setVariables(ints_3, 0, TextureRaster.currentWidth, this.fillValue)
        }
        return ints_3
    }

    override fun decodeOpcode(opcode: Int, stream: Packet) {
        when (opcode) {
            0 -> this.fillValue = (stream.readUByte() shl 12) / 255
        }
    }
}
