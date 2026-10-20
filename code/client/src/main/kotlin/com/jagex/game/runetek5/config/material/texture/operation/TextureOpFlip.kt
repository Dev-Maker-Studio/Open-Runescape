package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.utils.ArrayUtils

class TextureOpFlip : TextureOperation(1, false) {

    @JvmField var mirrorHorizontally: Boolean = true
    @JvmField var mirrorVertically: Boolean = true

    override fun getMonochromeOutput(index: Int): IntArray {
        val palette = monochromeImageCache.getPalette(index)
        if (monochromeImageCache.dirty) {
            val mirroredPalette = getChildMonochromeOutput(0, if (mirrorVertically) TextureRaster.columnEnd - index else index)
            if (mirrorHorizontally) {
                for (i in 0 until TextureRaster.currentWidth) {
                    palette[i] = mirroredPalette[TextureRaster.rowEnd - i]
                }
            } else {
                ArrayUtils.arrayCopy(mirroredPalette, 0, palette, 0, TextureRaster.currentWidth)
            }
        }
        return palette
    }

    override fun getColorOutput(index: Int): Array<IntArray> {
        val palette = colorImageCache.getPalette(index)
        if (colorImageCache.dirty) {
            val mirroredPalette = getChildColorOutput(0, if (mirrorVertically) TextureRaster.columnEnd - index else index)
            val mirroredRed = mirroredPalette[0]
            val mirroredGreen = mirroredPalette[1]
            val mirroredBlue = mirroredPalette[2]
            val paletteRed = palette[0]
            val paletteGreen = palette[1]
            val paletteBlue = palette[2]

            if (mirrorHorizontally) {
                for (i in 0 until TextureRaster.currentWidth) {
                    paletteRed[i] = mirroredRed[TextureRaster.rowEnd - i]
                    paletteGreen[i] = mirroredGreen[TextureRaster.rowEnd - i]
                    paletteBlue[i] = mirroredBlue[TextureRaster.rowEnd - i]
                }
            } else {
                for (i in 0 until TextureRaster.currentWidth) {
                    paletteRed[i] = mirroredRed[i]
                    paletteGreen[i] = mirroredGreen[i]
                    paletteBlue[i] = mirroredBlue[i]
                }
            }
        }
        return palette
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> mirrorHorizontally = buffer.readUByte() == 1
            1 -> mirrorVertically = buffer.readUByte() == 1
            2 -> monochrome = buffer.readUByte() == 1
        }
    }
}
