package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.utils.ArrayUtils

class TextureOpVerticalGradient : TextureOperation(0, true) {

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            ArrayUtils.setVariables(output, 0, TextureRaster.currentWidth, TextureRaster.verticalTable!![y])
        }
        return output
    }
}
