package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import kotlin.math.PI
import kotlin.math.sqrt

class TextureOpWaveform : TextureOperation(0, true) {

    @JvmField var waveType: Int = 0
    @JvmField var waveShape: Int = 0
    @JvmField var frequency: Int = 1

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val verticalValue = TextureRaster.verticalTable!![y]
            val verticalOffset = (verticalValue - 2048) shr 1

            for (x in 0 until TextureRaster.currentWidth) {
                val horizontalValue = TextureRaster.horizontalTable!![x]
                val horizontalOffset = (horizontalValue - 2048) shr 1
                var waveValue: Int

                if (waveType == 0) {
                    waveValue = (horizontalValue - verticalValue) * frequency
                } else {
                    val distanceSquared = (verticalOffset * verticalOffset + horizontalOffset * horizontalOffset) shr 12
                    var distance = (sqrt((distanceSquared / 4096.0f).toDouble()) * 4096.0).toInt()
                    waveValue = (distance * frequency * PI).toInt()
                }

                waveValue -= waveValue and -4096

                when (waveShape) {
                    0 -> waveValue = (TextureRaster.PALETTE_SIN!![waveValue shr 4 and 0xff] + 4096) shr 1
                    2 -> {
                        waveValue -= 2048
                        if (waveValue < 0) {
                            waveValue = -waveValue
                        }
                        waveValue = (2048 - waveValue) shl 1
                    }
                }

                output[x] = waveValue
            }
        }
        return output
    }

    override fun postDecode() {
        TextureRaster.caculateTable()
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> waveType = buffer.readUByte()
            1 -> waveShape = buffer.readUByte()
            3 -> frequency = buffer.readUByte()
        }
    }
}
