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

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            val verticalPos = TextureRaster.verticalTable!![y]

            for (x in 0 until TextureRaster.currentWidth) {
                output[x] = hashNoise(TextureRaster.horizontalTable!![x], verticalPos) % 4096
            }
        }

        return output
    }

    fun hashNoise(px: Int, py: Int): Int {
        var hash = py * 57 + px
        hash = hash xor (hash shl 1)
        return 4096 - ((hash * hash * 15731 + 789221) * hash + 1376312589 and 0x7fffffff) / 262144
    }
}
