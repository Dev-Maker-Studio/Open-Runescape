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

    @JvmField var anInt9805: Int = 0
    @JvmField var anInt9803: Int = 4096

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = super.monochromeImageCache.getPalette(i_1)
        if (super.monochromeImageCache.dirty) {
            val ints_4 = getChildMonochromeOutput(0, i_1)

            for (i_5 in 0 until TextureRaster.currentWidth) {
                val i_6 = ints_4[i_5]
                ints_3[i_5] = if (i_6 >= this.anInt9805 && i_6 <= this.anInt9803) 4096 else 0
            }
        }

        return ints_3
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> this.anInt9805 = rsbytebuffer2.readUShort()
            1 -> this.anInt9803 = rsbytebuffer2.readUShort()
        }
    }
}
