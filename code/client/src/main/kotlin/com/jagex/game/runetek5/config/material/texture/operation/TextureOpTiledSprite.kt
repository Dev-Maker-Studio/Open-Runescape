package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpTiledSprite : TextureOpSprite() {

    override fun getColorOutput(y: Int): Array<IntArray> {
        val palette = super.colorImageCache.getPalette(y)
        if (super.colorImageCache.dirty && ensureSpriteLoaded()) {
            val outRed = palette[0]
            val outGreen = palette[1]
            val outBlue = palette[2]
            val srcRow = y % super.spriteHeight * super.spriteHeight

            for (x in 0 until TextureRaster.currentWidth) {
                val rgb = super.spritePixels!![x % super.spriteWidth + srcRow]
                outBlue[x] = (rgb and 0xff) shl 4
                outGreen[x] = (rgb and 0xff00) shr 4
                outRed[x] = (rgb and 0xff0000) shr 12
            }
        }

        return palette
    }
}
