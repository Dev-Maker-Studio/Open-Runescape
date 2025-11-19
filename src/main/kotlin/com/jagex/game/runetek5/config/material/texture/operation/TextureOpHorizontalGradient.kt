package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpHorizontalGradient : TextureOperation(0, true) {
    override fun getMonochromeOutput(i_1: Int): IntArray {
        return TextureRaster.horizontalTable!!
    }
}
