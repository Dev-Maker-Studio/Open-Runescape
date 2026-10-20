package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpMonochrome : TextureOperation(1, true) {

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            val childColors = getChildColorOutput(0, y)
            val red = childColors[0]
            val green = childColors[1]
            val blue = childColors[2]
            for (x in 0 until TextureRaster.currentWidth) {
                output[x] = (blue[x] + green[x] + red[x]) / 3
            }
        }
        return output
    }
}
