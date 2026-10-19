package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 * Texture operation for polar coordinate distortion effects.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpPolarDistortion : TextureOperation(3, false) {

    @JvmField var distortionStrength: Int = 32768

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val angleData = getChildMonochromeOutput(1, y)
            val radiusData = getChildMonochromeOutput(2, y)

            for (x in 0 until TextureRaster.currentWidth) {
                val angle = angleData[x] shr 4 and 0xff
                val radius = distortionStrength * radiusData[x] shr 12
                val offsetX = TextureRaster.PALETTE_COS!![angle] * radius shr 12
                val offsetY = TextureRaster.PALETTE_SIN!![angle] * radius shr 12
                val sampledX = (x + (offsetX shr 12)) and TextureRaster.rowEnd
                val sampledY = ((offsetY shr 12) + y) and TextureRaster.columnEnd
                val sourceRow = getChildMonochromeOutput(0, sampledY)
                output[x] = sourceRow[sampledX]
            }
        }
        return output
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val angleData = getChildMonochromeOutput(1, y)
            val radiusData = getChildMonochromeOutput(2, y)
            val redOutput = output[0]
            val greenOutput = output[1]
            val blueOutput = output[2]

            for (x in 0 until TextureRaster.currentWidth) {
                val angle = angleData[x] * 255 shr 12 and 0xff
                val radius = distortionStrength * radiusData[x] shr 12
                val offsetX = TextureRaster.PALETTE_COS!![angle] * radius shr 12
                val offsetY = TextureRaster.PALETTE_SIN!![angle] * radius shr 12
                val sampledX = (x + (offsetX shr 12)) and TextureRaster.rowEnd
                val sampledY = ((offsetY shr 12) + y) and TextureRaster.columnEnd
                val sourceRow = getChildColorOutput(0, sampledY)
                redOutput[x] = sourceRow[0][sampledX]
                greenOutput[x] = sourceRow[1][sampledX]
                blueOutput[x] = sourceRow[2][sampledX]
            }
        }
        return output
    }

    override fun postDecode() {
        TextureRaster.caculateTable()
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> distortionStrength = buffer.readUShort() shl 4
            1 -> monochrome = buffer.readUByte() == 1
        }
    }
}
