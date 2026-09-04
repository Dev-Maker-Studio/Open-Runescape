package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 * Texture operation that fills with a solid color.
 * Used to create uniform color textures.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpColorFill : TextureOperation(0, false) {

    /** Red component (scaled) */
    @JvmField var redComponent: Int = 0

    /** Green component (scaled) */
    @JvmField var greenComponent: Int = 0

    /** Blue component (scaled) */
    @JvmField var blueComponent: Int = 0

    init {
        setColor(0)
    }

    /**
     * Sets the fill color from a packed RGB value
     * @param packedColor RGB color value (0xRRGGBB)
     */
    fun setColor(packedColor: Int) {
        blueComponent = (packedColor and 0xff) shl 4
        greenComponent = (packedColor and 0xff00) shr 4
        redComponent = (packedColor and 0xff0000) shr 12
    }

    override fun decodeOpcode(opcode: Int, packet: Packet) {
        when (opcode) {
            0 -> setColor(packet.readUMedium())
        }
    }

    override fun getColorOutput(y: Int): Array<IntArray> {
        val palette = colorImageCache.getPalette(y)

        if (colorImageCache.dirty) {
            val reds = palette[0]
            val greens = palette[1]
            val blues = palette[2]

            // Fill entire row with the solid color
            for (x in 0 until TextureRaster.currentWidth) {
                reds[x] = redComponent
                greens[x] = greenComponent
                blues[x] = blueComponent
            }
        }

        return palette
    }
}
