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

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = super.monochromeImageCache.getPalette(y)
        if (super.monochromeImageCache.dirty) {
            val prevRow = getChildMonochromeOutput(0, y - 1 and TextureRaster.columnEnd)
            val currentRow = getChildMonochromeOutput(0, y)
            val nextRow = getChildMonochromeOutput(0, y + 1 and TextureRaster.columnEnd)

            for (x in 0 until TextureRaster.currentWidth) {
                val gradientY = (nextRow[x] - prevRow[x]) * this.strength
                val gradientX = (currentRow[x + 1 and TextureRaster.rowEnd] - currentRow[x - 1 and TextureRaster.rowEnd]) * this.strength
                val scaledGradX = gradientX shr 12
                val scaledGradY = gradientY shr 12
                val gradXSq = scaledGradX * scaledGradX shr 12
                val gradYSq = scaledGradY * scaledGradY shr 12
                val normalLength = (sqrt((gradYSq + gradXSq + 4096) / 4096.0f.toDouble()) * 4096.0).toInt()
                val invNormal = if (normalLength != 0) 16777216 / normalLength else 0
                output[x] = 4096 - invNormal
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            this.strength = buffer.readUShort()
        }
    }
}
