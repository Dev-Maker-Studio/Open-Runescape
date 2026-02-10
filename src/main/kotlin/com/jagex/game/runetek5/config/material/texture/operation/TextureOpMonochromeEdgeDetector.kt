package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import kotlin.math.sqrt

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpMonochromeEdgeDetector : TextureOperation(1, true) {

    @JvmField var strength: Int = 4096

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = super.monochromeImageCache.getPalette(i_1)
        if (super.monochromeImageCache.dirty) {
            val ints_4 = getChildMonochromeOutput(0, i_1 - 1 and TextureRaster.columnEnd)
            val ints_5 = getChildMonochromeOutput(0, i_1)
            val ints_6 = getChildMonochromeOutput(0, i_1 + 1 and TextureRaster.columnEnd)

            for (i_7 in 0 until TextureRaster.currentWidth) {
                val i_8 = (ints_6[i_7] - ints_4[i_7]) * this.strength
                val i_9 = (ints_5[i_7 + 1 and TextureRaster.rowEnd] - ints_5[i_7 - 1 and TextureRaster.rowEnd]) * this.strength
                val i_10 = i_9 shr 12
                val i_11 = i_8 shr 12
                val i_12 = i_10 * i_10 shr 12
                val i_13 = i_11 * i_11 shr 12
                val i_14 = (sqrt((i_13 + i_12 + 4096) / 4096.0f.toDouble()) * 4096.0).toInt()
                val i_15 = if (i_14 != 0) 16777216 / i_14 else 0
                ints_3[i_7] = 4096 - i_15
            }
        }

        return ints_3
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        if (i_1 == 0) {
            this.strength = rsbytebuffer2.readUShort()
        }
    }
}
