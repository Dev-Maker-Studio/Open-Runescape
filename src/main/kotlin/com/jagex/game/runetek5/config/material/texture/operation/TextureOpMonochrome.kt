package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpMonochrome : TextureOperation(1, true) {

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = super.monochromeImageCache.getPalette(i_1)
        if (super.monochromeImageCache.dirty) {
            val ints_4 = getChildColorOutput(0, i_1)
            val ints_5 = ints_4[0]
            val ints_6 = ints_4[1]
            val ints_7 = ints_4[2]
            for (i_8 in 0 until TextureRaster.currentWidth) {
                ints_3[i_8] = (ints_7[i_8] + ints_6[i_8] + ints_5[i_8]) / 3
            }
        }
        return ints_3
    }
}
