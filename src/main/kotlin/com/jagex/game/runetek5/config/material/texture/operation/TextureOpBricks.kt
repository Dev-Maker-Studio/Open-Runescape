package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils
import java.util.Random

/**
 * Texture operation that generates brick patterns.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpBricks : TextureOperation(0, true) {

    @JvmField var anInt9982: Int = 4
    @JvmField var anInt9994: Int = 8
    @JvmField var anInt9986: Int = 0
    @JvmField var anInt9987: Int = 1024
    @JvmField var anInt9984: Int = 1024
    @JvmField var anInt9989: Int = 409
    @JvmField var anInt9990: Int = 204
    @JvmField var anInt9991: Int = 81
    @JvmField var anInt9993: Int = 0
    @JvmField var anInt9988: Int = 0
    @JvmField var anInt9995: Int = 0
    @JvmField var anIntArray9996: IntArray? = null
    @JvmField var anIntArrayArray9979: Array<IntArray>? = null
    @JvmField var anIntArrayArray9992: Array<IntArray>? = null

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            var i_4 = 0

            var i_5 = TextureRaster.verticalTable!![i_1] + anInt9986
            while (i_5 < 0) {
                i_5 += 4096
            }

            while (i_5 > 4096) {
                i_5 -= 4096
            }

            while (i_4 < anInt9994 && i_5 >= anIntArray9996!![i_4]) {
                ++i_4
            }

            val i_6 = i_4 - 1
            val bool7 = (i_4 and 0x1) == 0
            val i_8 = anIntArray9996!![i_4]
            val i_9 = anIntArray9996!![i_4 - 1]

            if (i_5 > i_9 + anInt9993 && i_5 < i_8 - anInt9993) {
                for (i_10 in 0 until TextureRaster.currentWidth) {
                    var i_11 = 0
                    val i_12 = if (bool7) anInt9987 else -anInt9987

                    var i_13 = TextureRaster.horizontalTable!![i_10] + (i_12 * anInt9988 shr 12)
                    while (i_13 < 0) {
                        i_13 += 4096
                    }

                    while (i_13 > 4096) {
                        i_13 -= 4096
                    }

                    while (i_11 < anInt9982 && i_13 >= anIntArrayArray9979!![i_6][i_11]) {
                        ++i_11
                    }

                    val i_14 = i_11 - 1
                    val i_15 = anIntArrayArray9979!![i_6][i_14]
                    val i_16 = anIntArrayArray9979!![i_6][i_11]

                    if (i_13 > i_15 + anInt9993 && i_13 < i_16 - anInt9993) {
                        ints_3[i_10] = anIntArrayArray9992!![i_6][i_14]
                    } else {
                        ints_3[i_10] = 0
                    }
                }
            } else {
                ArrayUtils.setVariables(ints_3, 0, TextureRaster.currentWidth, 0)
            }
        }

        return ints_3
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> anInt9982 = buffer.readUByte()
            1 -> anInt9994 = buffer.readUByte()
            2 -> anInt9989 = buffer.readUShort()
            3 -> anInt9990 = buffer.readUShort()
            4 -> anInt9987 = buffer.readUShort()
            5 -> anInt9986 = buffer.readUShort()
            6 -> anInt9991 = buffer.readUShort()
            7 -> anInt9984 = buffer.readUShort()
        }
    }

    fun method15422() {
        val random2 = Random(anInt9994.toLong())
        anInt9993 = anInt9991 / 2
        anInt9988 = 4096 / anInt9982
        anInt9995 = 4096 / anInt9994
        val i_3 = anInt9988 / 2
        val i_4 = anInt9995 / 2
        anIntArray9996 = IntArray(anInt9994 + 1)
        anIntArrayArray9979 = Array(anInt9994) { IntArray(anInt9982 + 1) }
        anIntArrayArray9992 = Array(anInt9994) { IntArray(anInt9982) }
        anIntArray9996!![0] = 0

        for (i_5 in 0 until anInt9994) {
            var i_6: Int
            var i_7: Int
            if (i_5 > 0) {
                i_6 = anInt9995
                i_7 = (StaticMethods.method7931(random2, 4096) - 2048) * anInt9990 shr 12
                val adjusted = i_6 + (i_7 * i_4 shr 12)
                anIntArray9996!![i_5] = adjusted + anIntArray9996!![i_5 - 1]
            }

            anIntArrayArray9979!![i_5][0] = 0

            for (i_6_2 in 0 until anInt9982) {
                if (i_6_2 > 0) {
                    i_7 = anInt9988
                    val i_8 = (StaticMethods.method7931(random2, 4096) - 2048) * anInt9989 shr 12
                    val adjusted = i_7 + (i_8 * i_3 shr 12)
                    anIntArrayArray9979!![i_5][i_6_2] = anIntArrayArray9979!![i_5][i_6_2 - 1] + adjusted
                }

                anIntArrayArray9992!![i_5][i_6_2] = if (anInt9984 > 0) {
                    4096 - StaticMethods.method7931(random2, anInt9984)
                } else {
                    4096
                }
            }

            anIntArrayArray9979!![i_5][anInt9982] = 4096
        }

        anIntArray9996!![anInt9994] = 4096
    }

    override fun postDecode() {
        method15422()
    }
}
