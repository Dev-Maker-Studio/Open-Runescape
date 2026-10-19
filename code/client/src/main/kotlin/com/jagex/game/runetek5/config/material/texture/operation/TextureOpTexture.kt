package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.config.material.texture.TextureTypeList

/**
 * Texture operation that samples from a material texture.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpTexture : TextureOperation(0, false) {

    @JvmField var texturePixels: IntArray? = null
    @JvmField var textureHeight: Int = 0
    @JvmField var textureWidth: Int = 0
    @JvmField var materialId: Int = -1

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val adjustedY = (if (textureHeight != TextureRaster.currentHeight) textureHeight * y / TextureRaster.currentHeight else y) * textureWidth
            val red = output[0]
            val green = output[1]
            val blue = output[2]
            if (textureWidth == TextureRaster.currentWidth) {
                var offset = adjustedY
                for (x in 0 until TextureRaster.currentWidth) {
                    val pixel = texturePixels!![offset++]
                    blue[x] = (pixel and 0xff) shl 4
                    green[x] = (pixel and 0xff00) shr 4
                    red[x] = (pixel and 0xff0000) shr 12
                }
            } else {
                for (x in 0 until TextureRaster.currentWidth) {
                    val adjustedX = x * textureWidth / TextureRaster.currentWidth
                    val pixel = texturePixels!![adjustedY + adjustedX]
                    blue[x] = (pixel and 0xff) shl 4
                    green[x] = (pixel and 0xff00) shr 4
                    red[x] = (pixel and 0xff0000) shr 12
                }
            }
        }
        return output
    }

    override fun createImageCache(width: Int, height: Int) {
        super.createImageCache(width, height)
        if (materialId >= 0 && TextureTypeList.materialProvider != null) {
            val size = if (TextureTypeList.materialProvider!!.getMaterial(materialId).isSmall) 64 else 128
            texturePixels = TextureTypeList.materialProvider!!.getPixels(materialId, 1.0f, size, size, false)
            textureHeight = size
            textureWidth = size
        }
    }

    override fun clearImageCache() {
        super.clearImageCache()
        texturePixels = null
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            materialId = buffer.readUShort()
        }
    }

    override fun getMaterialId(): Int {
        return materialId
    }
}
