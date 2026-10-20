package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.config.material.texture.TextureTypeList
import com.jagex.game.runetek5.graphics.sprite.SpriteType

open class TextureOpSprite : TextureOperation(0, false) {

    @JvmField var spritePixels: IntArray? = null
    @JvmField var spriteWidth: Int = 0
    @JvmField var spriteHeight: Int = 0
    @JvmField var spriteId: Int = -1

    override fun clearImageCache() {
        super.clearImageCache()
        spritePixels = null
    }

    fun ensureSpriteLoaded(): Boolean {
        if (spritePixels != null) {
            return true
        } else if (spriteId >= 0) {
            val sprite = if (TextureTypeList.TEXTURE_GROUP_ID >= 0) {
                SpriteType.listSingle(TextureTypeList.textureSpriteResource, TextureTypeList.TEXTURE_GROUP_ID, spriteId)
            } else {
                SpriteType.listSingle(TextureTypeList.textureSpriteResource, spriteId)
            }
            assert(sprite != null)
            sprite!!.expandToFullSize()
            spritePixels = sprite.getPixels()
            spriteWidth = sprite.width
            spriteHeight = sprite.height
            return true
        } else {
            return false
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            spriteId = buffer.readUShort()
        }
    }

    override fun getTextureId(): Int {
        return spriteId
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty && ensureSpriteLoaded()) {
            val outputRed = output[0]
            val outputGreen = output[1]
            val outputBlue = output[2]
            val rowOffset = (if (spriteHeight != TextureRaster.currentHeight) spriteHeight * y / TextureRaster.currentHeight else y) * spriteWidth
            if (spriteWidth == TextureRaster.currentWidth) {
                var offset = rowOffset
                for (x in 0 until TextureRaster.currentWidth) {
                    val pixel = spritePixels!![offset++]
                    outputBlue[x] = (pixel and 0xff) shl 4
                    outputGreen[x] = (pixel and 0xff00) shr 4
                    outputRed[x] = (pixel and 0xff0000) shr 12
                }
            } else {
                for (x in 0 until TextureRaster.currentWidth) {
                    val spriteX = x * spriteWidth / TextureRaster.currentWidth
                    val pixel = spritePixels!![rowOffset + spriteX]
                    outputBlue[x] = (pixel and 0xff) shl 4
                    outputGreen[x] = (pixel and 0xff00) shr 4
                    outputRed[x] = (pixel and 0xff0000) shr 12
                }
            }
        }

        return output
    }
}
