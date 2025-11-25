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

    @JvmField var anInt9593 = 10
    @JvmField var anInt9594 = 2048
    @JvmField var anInt9595: Int = 0
    @JvmField var anIntArray9596: IntArray? = null
    @JvmField var anIntArray9592: IntArray? = null

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val i_4 = TextureRaster.verticalTable!![i_1]
            var i_6: Int
            if (anInt9595 != 0) {
                for (i_5 in 0 until TextureRaster.currentWidth) {
                    i_6 = 0
                    var s_7: Short = 0
                    val i_8 = TextureRaster.horizontalTable!![i_5]
                    when (anInt9595) {
                        1 -> i_6 = i_8
                        2 -> i_6 = (i_8 - (4096 - i_4) shr 1) + 2048
                        3 -> i_6 = (i_8 - i_4 shr 1) + 2048
                    }

                    for (i_9 in 0 until anInt9593) {
                        if (i_6 >= anIntArray9592!![i_9] && i_6 < anIntArray9592!![i_9 + 1]) {
                            if (i_6 < anIntArray9596!![i_9]) {
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
                while (i_6 < anInt9593) {
                    if (i_4 >= anIntArray9592!![i_6] && i_4 < anIntArray9592!![i_6 + 1]) {
                        if (i_4 < anIntArray9596!![i_6]) {
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
        method14842()
    }

    fun method14842() {
        var i_2 = 0
        anIntArray9596 = IntArray(anInt9593 + 1)
        anIntArray9592 = IntArray(anInt9593 + 1)
        val i_3 = 4096 / anInt9593
        val i_4 = i_3 * anInt9594 shr 12

        for (i_5 in 0 until anInt9593) {
            anIntArray9592!![i_5] = i_2
            anIntArray9596!![i_5] = i_4 + i_2
            i_2 += i_3
        }

        anIntArray9592!![anInt9593] = 4096
        anIntArray9596!![anInt9593] = anIntArray9596!![0] + 4096
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> anInt9593 = rsbytebuffer2.readUByte()
            1 -> anInt9594 = rsbytebuffer2.readUShort()
            2 -> anInt9595 = rsbytebuffer2.readUByte()
        }
    }
}
