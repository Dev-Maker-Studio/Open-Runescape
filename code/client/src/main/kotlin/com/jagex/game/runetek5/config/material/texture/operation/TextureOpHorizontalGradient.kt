package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpHorizontalGradient : TextureOperation(0, true) {
    override fun getMonochromeOutput(y: Int): IntArray {
        return TextureRaster.horizontalTable!!
    }
}
