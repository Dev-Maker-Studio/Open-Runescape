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

    @JvmField var centerX: Int = 0
    @JvmField var centerY: Int = 0
    @JvmField var zoom: Int = 1365
    @JvmField var maxIterations: Int = 20

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val output = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            for (i in 0 until TextureRaster.currentWidth) {
                val offsetX = (TextureRaster.horizontalTable!![i] shl 12) / zoom + centerX
                val offsetY = (TextureRaster.verticalTable!![i_1] shl 12) / zoom + centerY
                val startX = offsetX
                val startY = offsetY
                var realPart = offsetX
                var imagPart = offsetY
                var realSquared = offsetX * offsetX shr 12
                var imagSquared = offsetY * offsetY shr 12

                var iterations = 0
                while (realSquared + imagSquared < 16384 && iterations < maxIterations) {
                    imagPart = (realPart * imagPart shr 12) * 2 + startY
                    realPart = startX + (realSquared - imagSquared)
                    realSquared = realPart * realPart shr 12
                    imagSquared = imagPart * imagPart shr 12
                    iterations++
                }

                output[i] = if (iterations < maxIterations - 1) (iterations shl 12) / maxIterations else 0
            }
        }

        return output
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> zoom = buffer.readUShort()
            1 -> maxIterations = buffer.readUShort()
            2 -> centerX = buffer.readUShort()
            3 -> centerY = buffer.readUShort()
        }
    }
}
