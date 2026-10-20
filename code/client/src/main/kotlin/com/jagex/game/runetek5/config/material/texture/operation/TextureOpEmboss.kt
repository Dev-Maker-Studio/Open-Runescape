package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TextureOpEmboss : TextureOperation(1, true) {

    @JvmField var embossDepth = 4096
    @JvmField var lightAzimuth = 3216
    @JvmField var lightElevation = 3216
    @JvmField val lightDirection = IntArray(3)

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val scaledDepth = embossDepth * TextureRaster.scaledWidth shr 12
            val prevRow = getChildMonochromeOutput(0, y - 1 and TextureRaster.columnEnd)
            val currentRow = getChildMonochromeOutput(0, y)
            val nextRow = getChildMonochromeOutput(0, y + 1 and TextureRaster.columnEnd)

            for (x in 0 until TextureRaster.currentWidth) {
                val gradientY = (nextRow[x] - prevRow[x]) * scaledDepth shr 12
                val gradientX = scaledDepth * (currentRow[x - 1 and TextureRaster.rowEnd] - currentRow[x + 1 and TextureRaster.rowEnd]) shr 12
                var absGradX = gradientX shr 4
                var absGradY = gradientY shr 4
                if (absGradX < 0) {
                    absGradX = -absGradX
                }

                if (absGradX > 255) {
                    absGradX = 255
                }

                if (absGradY < 0) {
                    absGradY = -absGradY
                }

                if (absGradY > 255) {
                    absGradY = 255
                }

                val normalScale = normalizeTable[absGradX + (absGradY * (absGradY + 1) shr 1)].toInt() and 0xff
                val normalX = gradientX * normalScale shr 8
                val normalY = gradientY * normalScale shr 8
                val normalZ = normalScale * 4096 shr 8
                val dotX = normalX * lightDirection[0] shr 12
                val dotY = lightDirection[1] * normalY shr 12
                val dotZ = normalZ * lightDirection[2] shr 12
                output[x] = dotZ + dotY + dotX
            }
        }

        return output
    }

    override fun postDecode() {
        computeLightDirection()
    }

    fun computeLightDirection() {
        val cosElevation = cos(lightElevation / 4096.0)
        lightDirection[0] = (sin(lightAzimuth / 4096.0) * cosElevation * 4096.0).toInt()
        lightDirection[1] = (cos(lightAzimuth / 4096.0) * cosElevation * 4096.0).toInt()
        lightDirection[2] = (sin(lightElevation / 4096.0) * 4096.0).toInt()
        val dotXX = lightDirection[0] * lightDirection[0] shr 12
        val dotYY = lightDirection[1] * lightDirection[1] shr 12
        val dotZZ = lightDirection[2] * lightDirection[2] shr 12
        val magnitude = (sqrt((dotXX + dotYY + dotZZ shr 12).toDouble()) * 4096.0).toInt()
        if (magnitude != 0) {
            lightDirection[0] = (lightDirection[0] shl 12) / magnitude
            lightDirection[1] = (lightDirection[1] shl 12) / magnitude
            lightDirection[2] = (lightDirection[2] shl 12) / magnitude
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> embossDepth = buffer.readUShort()
            1 -> lightAzimuth = buffer.readUShort()
            2 -> lightElevation = buffer.readUShort()
        }
    }

    companion object {
        @JvmField val normalizeTable: ByteArray

        init {
            var idx = 0
            normalizeTable = ByteArray(32896)

            for (row in 0..255) {
                for (col in 0..row) {
                    normalizeTable[idx++] = (255.0 / sqrt((col * col + row * row + 65535) / 65535.0)).toInt().toByte()
                }
            }
        }
    }
}
