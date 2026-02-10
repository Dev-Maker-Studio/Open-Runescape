package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpTiledSprite : TextureOpSprite() {

    override fun getColorOutput(i_1: Int): Array<IntArray> {
        val ints_3 = super.colorImageCache.getPalette(i_1)
        if (super.colorImageCache.dirty && ensureSpriteLoaded()) {
            val ints_4 = ints_3[0]
            val ints_5 = ints_3[1]
            val ints_6 = ints_3[2]
            val i_7 = i_1 % super.spriteHeight * super.spriteHeight

            for (i_8 in 0 until TextureRaster.currentWidth) {
                val i_9 = super.spritePixels!![i_8 % super.spriteWidth + i_7]
                ints_6[i_8] = (i_9 and 0xff) shl 4
                ints_5[i_8] = (i_9 and 0xff00) shr 4
                ints_4[i_8] = (i_9 and 0xff0000) shr 12
            }
        }

        return ints_3
    }
}
