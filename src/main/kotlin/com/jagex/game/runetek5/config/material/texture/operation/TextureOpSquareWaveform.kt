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

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val i_4 = TextureRaster.verticalTable!![i_1]
            var i_6: Int
            if (waveAxis != 0) {
                for (i_5 in 0 until TextureRaster.currentWidth) {
                    i_6 = 0
                    var s_7: Short = 0
                    val i_8 = TextureRaster.horizontalTable!![i_5]
                    when (waveAxis) {
                        1 -> i_6 = i_8
                        2 -> i_6 = (i_8 - (4096 - i_4) shr 1) + 2048
                        3 -> i_6 = (i_8 - i_4 shr 1) + 2048
                    }

                    for (i_9 in 0 until stepCount) {
                        if (i_6 >= stepStartBoundaries!![i_9] && i_6 < stepStartBoundaries!![i_9 + 1]) {
                            if (i_6 < pulseEndBoundaries!![i_9]) {
                                s_7 = 4096
                            }
                            break
                        }
                    }

                    ints_3[i_5] = s_7.toInt()
                }
            } else {
                var s_10: Short = 0

                i_6 = 0
                while (i_6 < stepCount) {
                    if (i_4 >= stepStartBoundaries!![i_6] && i_4 < stepStartBoundaries!![i_6 + 1]) {
                        if (i_4 < pulseEndBoundaries!![i_6]) {
                            s_10 = 4096
                        }
                        break
                    }
                    i_6++
                }

                ArrayUtils.setVariables(ints_3, 0, TextureRaster.currentWidth, s_10.toInt())
            }
        }

        return ints_3
    }

    override fun postDecode() {
        computeStepBoundaries()
    }

    fun computeStepBoundaries() {
        var i_2 = 0
        pulseEndBoundaries = IntArray(stepCount + 1)
        stepStartBoundaries = IntArray(stepCount + 1)
        val i_3 = 4096 / stepCount
        val i_4 = i_3 * dutyCycle shr 12

        for (i_5 in 0 until stepCount) {
            stepStartBoundaries!![i_5] = i_2
            pulseEndBoundaries!![i_5] = i_4 + i_2
            i_2 += i_3
        }

        stepStartBoundaries!![stepCount] = 4096
        pulseEndBoundaries!![stepCount] = pulseEndBoundaries!![0] + 4096
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> stepCount = rsbytebuffer2.readUByte()
            1 -> dutyCycle = rsbytebuffer2.readUShort()
            2 -> waveAxis = rsbytebuffer2.readUByte()
        }
    }
}
