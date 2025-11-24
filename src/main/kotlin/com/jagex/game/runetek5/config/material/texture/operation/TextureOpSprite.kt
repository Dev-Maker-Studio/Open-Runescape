package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.config.material.texture.TextureTypeList
import com.jagex.game.runetek5.graphics.sprite.SpriteType

/**
 * Texture operation that renders a sprite as a texture.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
open class TextureOpSprite : TextureOperation(0, false) {

    @JvmField var anIntArray9916: IntArray? = null
    @JvmField var anInt9914: Int = 0
    @JvmField var anInt9915: Int = 0
    @JvmField var anInt9912: Int = -1

    override fun clearImageCache() {
        super.clearImageCache()
        anIntArray9916 = null
    }

    fun method15398(): Boolean {
        if (anIntArray9916 != null) {
            return true
        } else if (anInt9912 >= 0) {
            val sprite = if (TextureTypeList.TEXTURE_GROUP_ID >= 0) {
                SpriteType.listSingle(TextureTypeList.textureSpriteResource, TextureTypeList.TEXTURE_GROUP_ID, anInt9912)
            } else {
                SpriteType.listSingle(TextureTypeList.textureSpriteResource, anInt9912)
            }
            assert(sprite != null)
            sprite!!.method1523()
            anIntArray9916 = sprite.getPixels()
            anInt9914 = sprite.width
            anInt9915 = sprite.height
            return true
        } else {
            return false
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            anInt9912 = buffer.readUShort()
        }
    }

    override fun getTextureId(): Int {
        return anInt9912
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val output = colorImageCache.getPalette(y)
        if (colorImageCache.dirty && method15398()) {
            val outputRed = output[0]
            val outputGreen = output[1]
            val outputBlue = output[2]
            val rowOffset = (if (anInt9915 != TextureRaster.currentHeight) anInt9915 * y / TextureRaster.currentHeight else y) * anInt9914
            if (anInt9914 == TextureRaster.currentWidth) {
                var offset = rowOffset
                for (x in 0 until TextureRaster.currentWidth) {
                    val pixel = anIntArray9916!![offset++]
                    outputBlue[x] = (pixel and 0xff) shl 4
                    outputGreen[x] = (pixel and 0xff00) shr 4
                    outputRed[x] = (pixel and 0xff0000) shr 12
                }
            } else {
                for (x in 0 until TextureRaster.currentWidth) {
                    val spriteX = x * anInt9914 / TextureRaster.currentWidth
                    val pixel = anIntArray9916!![rowOffset + spriteX]
                    outputBlue[x] = (pixel and 0xff) shl 4
                    outputGreen[x] = (pixel and 0xff00) shr 4
                    outputRed[x] = (pixel and 0xff0000) shr 12
                }
            }
        }

        return output
    }
}
