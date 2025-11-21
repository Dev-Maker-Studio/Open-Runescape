package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 * Texture operation that generates Mandelbrot fractal patterns.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpMandelbrot : TextureOperation(0, true) {

    @JvmField var anInt9741: Int = 0
    @JvmField var anInt9740: Int = 0
    @JvmField var anInt9742: Int = 1365
    @JvmField var anInt9743: Int = 20

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val output = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            for (i in 0 until TextureRaster.currentWidth) {
                val offsetX = (TextureRaster.horizontalTable!![i] shl 12) / anInt9742 + anInt9741
                val offsetY = (TextureRaster.verticalTable!![i_1] shl 12) / anInt9742 + anInt9740
                val startX = offsetX
                val startY = offsetY
                var realPart = offsetX
                var imagPart = offsetY
                var realSquared = offsetX * offsetX shr 12
                var imagSquared = offsetY * offsetY shr 12

                var iterations = 0
                while (realSquared + imagSquared < 16384 && iterations < anInt9743) {
                    imagPart = (realPart * imagPart shr 12) * 2 + startY
                    realPart = startX + (realSquared - imagSquared)
                    realSquared = realPart * realPart shr 12
                    imagSquared = imagPart * imagPart shr 12
                    iterations++
                }

                output[i] = if (iterations < anInt9743 - 1) (iterations shl 12) / anInt9743 else 0
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> anInt9742 = buffer.readUShort()
            1 -> anInt9743 = buffer.readUShort()
            2 -> anInt9741 = buffer.readUShort()
            3 -> anInt9740 = buffer.readUShort()
        }
    }
}
