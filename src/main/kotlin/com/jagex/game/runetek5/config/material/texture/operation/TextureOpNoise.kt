package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpNoise : TextureOperation(0, true) {

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = super.monochromeImageCache.getPalette(i_1)
        if (super.monochromeImageCache.dirty) {
            val i_4 = TextureRaster.verticalTable!![i_1]

            for (i_5 in 0 until TextureRaster.currentWidth) {
                ints_3[i_5] = hashNoise(TextureRaster.horizontalTable!![i_5], i_4) % 4096
            }
        }

        return ints_3
    }

    fun hashNoise(i_1: Int, i_2: Int): Int {
        var i_4 = i_2 * 57 + i_1
        i_4 = i_4 xor (i_4 shl 1)
        return 4096 - ((i_4 * i_4 * 15731 + 789221) * i_4 + 1376312589 and 0x7fffffff) / 262144
    }
}
