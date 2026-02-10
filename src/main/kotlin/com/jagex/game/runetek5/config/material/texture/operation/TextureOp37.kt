package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOp37 : TextureOperation(0, true) {

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

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> pattern1OffsetX = rsbytebuffer2.readUShort()
            1 -> pattern1OffsetY = rsbytebuffer2.readUShort()
            2 -> pattern2OffsetX = rsbytebuffer2.readUShort()
            3 -> pattern2OffsetY = rsbytebuffer2.readUShort()
            4 -> waveFrequency = rsbytebuffer2.readUShort()
            5 -> waveAmplitude = rsbytebuffer2.readUShort()
            6 -> waveDecay = rsbytebuffer2.readUShort()
        }
    }

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val i_4 = TextureRaster.verticalTable!![i_1] - 2048

            for (i_5 in 0 until TextureRaster.currentWidth) {
                val i_6 = TextureRaster.horizontalTable!![i_5] - 2048
                var i_7 = i_6 + pattern1OffsetX
                i_7 = if (i_7 < -2048) i_7 + 4096 else i_7
                i_7 = if (i_7 > 2048) i_7 - 4096 else i_7
                var i_8 = i_4 + pattern1OffsetY
                i_8 = if (i_8 < -2048) i_8 + 4096 else i_8
                i_8 = if (i_8 > 2048) i_8 - 4096 else i_8
                var i_9 = i_6 + pattern2OffsetX
                i_9 = if (i_9 < -2048) i_9 + 4096 else i_9
                i_9 = if (i_9 > 2048) i_9 - 4096 else i_9
                var i_10 = i_4 + pattern2OffsetY
                i_10 = if (i_10 < -2048) i_10 + 4096 else i_10
                i_10 = if (i_10 > 2048) i_10 - 4096 else i_10
                ints_3[i_5] = if (!testDiagonalWave(i_7, i_8) && !testAntiDiagonalWave(i_9, i_10)) 0 else 4096
            }
        }

        return ints_3
    }

    fun testAntiDiagonalWave(i_1: Int, i_2: Int): Boolean {
        val i_4 = (i_2 + i_1) * waveFrequency shr 12
        var i_5 = TextureRaster.PALETTE_COS!![i_4 * 255 shr 12 and 0xff]
        i_5 = (i_5 shl 12) / waveFrequency
        i_5 = (i_5 shl 12) / waveDecay
        i_5 = i_5 * waveAmplitude shr 12
        return i_2 - i_1 < i_5 && i_2 - i_1 > -i_5
    }

    fun testDiagonalWave(i_1: Int, i_2: Int): Boolean {
        val i_4 = (i_2 - i_1) * waveFrequency shr 12
        var i_5 = TextureRaster.PALETTE_COS!![i_4 * 255 shr 12 and 0xff]
        i_5 = (i_5 shl 12) / waveFrequency
        i_5 = (i_5 shl 12) / waveDecay
        i_5 = i_5 * waveAmplitude shr 12
        return i_2 + i_1 < i_5 && i_2 + i_1 > -i_5
    }
}
