package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.utils.ArrayUtils

/**
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
class TextureOpSquareWaveform : TextureOperation(0, true) {

    @JvmField var stepCount = 10
    @JvmField var dutyCycle = 2048
    @JvmField var waveAxis: Int = 0
    @JvmField var pulseEndBoundaries: IntArray? = null
    @JvmField var stepStartBoundaries: IntArray? = null

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val verticalPos = TextureRaster.verticalTable!![y]
            var coord: Int
            if (waveAxis != 0) {
                for (x in 0 until TextureRaster.currentWidth) {
                    coord = 0
                    var value: Short = 0
                    val horizontalPos = TextureRaster.horizontalTable!![x]
                    when (waveAxis) {
                        1 -> coord = horizontalPos
                        2 -> coord = (horizontalPos - (4096 - verticalPos) shr 1) + 2048
                        3 -> coord = (horizontalPos - verticalPos shr 1) + 2048
                    }

                    for (step in 0 until stepCount) {
                        if (coord >= stepStartBoundaries!![step] && coord < stepStartBoundaries!![step + 1]) {
                            if (coord < pulseEndBoundaries!![step]) {
                                value = 4096
                            }
                            break
                        }
                    }

                    output[x] = value.toInt()
                }
            } else {
                var value: Short = 0

                coord = 0
                while (coord < stepCount) {
                    if (verticalPos >= stepStartBoundaries!![coord] && verticalPos < stepStartBoundaries!![coord + 1]) {
                        if (verticalPos < pulseEndBoundaries!![coord]) {
                            value = 4096
                        }
                        break
                    }
                    coord++
                }

                ArrayUtils.setVariables(output, 0, TextureRaster.currentWidth, value.toInt())
            }
        }

        return output
    }

    override fun postDecode() {
        computeStepBoundaries()
    }

    fun computeStepBoundaries() {
        var position = 0
        pulseEndBoundaries = IntArray(stepCount + 1)
        stepStartBoundaries = IntArray(stepCount + 1)
        val stepSize = 4096 / stepCount
        val pulseWidth = stepSize * dutyCycle shr 12

        for (step in 0 until stepCount) {
            stepStartBoundaries!![step] = position
            pulseEndBoundaries!![step] = pulseWidth + position
            position += stepSize
        }

        stepStartBoundaries!![stepCount] = 4096
        pulseEndBoundaries!![stepCount] = pulseEndBoundaries!![0] + 4096
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> stepCount = buffer.readUByte()
            1 -> dutyCycle = buffer.readUShort()
            2 -> waveAxis = buffer.readUByte()
        }
    }
}
