package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpDiagonalWaves : TextureOperation(0, true) {

    @JvmField var pattern1OffsetX: Int = 2048
    @JvmField var pattern1OffsetY: Int = 0
    @JvmField var pattern2OffsetX: Int = 0
    @JvmField var pattern2OffsetY: Int = 2048
    @JvmField var waveFrequency: Int = 12288
    @JvmField var waveAmplitude: Int = 4096
    @JvmField var waveDecay: Int = 8192

    override fun postDecode() {
        TextureRaster.caculateTable()
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> pattern1OffsetX = buffer.readUShort()
            1 -> pattern1OffsetY = buffer.readUShort()
            2 -> pattern2OffsetX = buffer.readUShort()
            3 -> pattern2OffsetY = buffer.readUShort()
            4 -> waveFrequency = buffer.readUShort()
            5 -> waveAmplitude = buffer.readUShort()
            6 -> waveDecay = buffer.readUShort()
        }
    }

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val verticalPos = TextureRaster.verticalTable!![y] - 2048

            for (x in 0 until TextureRaster.currentWidth) {
                val horizontalPos = TextureRaster.horizontalTable!![x] - 2048
                var offsetX1 = horizontalPos + pattern1OffsetX
                offsetX1 = if (offsetX1 < -2048) offsetX1 + 4096 else offsetX1
                offsetX1 = if (offsetX1 > 2048) offsetX1 - 4096 else offsetX1
                var offsetY1 = verticalPos + pattern1OffsetY
                offsetY1 = if (offsetY1 < -2048) offsetY1 + 4096 else offsetY1
                offsetY1 = if (offsetY1 > 2048) offsetY1 - 4096 else offsetY1
                var offsetX2 = horizontalPos + pattern2OffsetX
                offsetX2 = if (offsetX2 < -2048) offsetX2 + 4096 else offsetX2
                offsetX2 = if (offsetX2 > 2048) offsetX2 - 4096 else offsetX2
                var offsetY2 = verticalPos + pattern2OffsetY
                offsetY2 = if (offsetY2 < -2048) offsetY2 + 4096 else offsetY2
                offsetY2 = if (offsetY2 > 2048) offsetY2 - 4096 else offsetY2
                output[x] = if (!testDiagonalWave(offsetX1, offsetY1) && !testAntiDiagonalWave(offsetX2, offsetY2)) 0 else 4096
            }
        }

        return output
    }

    fun testAntiDiagonalWave(px: Int, py: Int): Boolean {
        val phase = (py + px) * waveFrequency shr 12
        var envelope = TextureRaster.PALETTE_COS!![phase * 255 shr 12 and 0xff]
        envelope = (envelope shl 12) / waveFrequency
        envelope = (envelope shl 12) / waveDecay
        envelope = envelope * waveAmplitude shr 12
        return py - px < envelope && py - px > -envelope
    }

    fun testDiagonalWave(px: Int, py: Int): Boolean {
        val phase = (py - px) * waveFrequency shr 12
        var envelope = TextureRaster.PALETTE_COS!![phase * 255 shr 12 and 0xff]
        envelope = (envelope shl 12) / waveFrequency
        envelope = (envelope shl 12) / waveDecay
        envelope = envelope * waveAmplitude shr 12
        return py + px < envelope && py + px > -envelope
    }
}
