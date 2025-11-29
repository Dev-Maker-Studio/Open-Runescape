package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 * Texture operation that applies curve interpolation.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpCurve : TextureOperation(1, true) {

    @JvmField var anIntArrayArray9800: Array<IntArray>? = null
    @JvmField var anIntArray9798: IntArray? = null
    @JvmField var anIntArray9796: IntArray? = null
    @JvmField var anInt9799: Int = 0
    @JvmField val aShortArray9801: ShortArray = ShortArray(257)

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val ints_4 = getChildMonochromeOutput(0, i_1)

            for (i_5 in 0 until TextureRaster.currentWidth) {
                var i_6 = ints_4[i_5] shr 4
                if (i_6 < 0) {
                    i_6 = 0
                }

                if (i_6 > 256) {
                    i_6 = 256
                }

                ints_3[i_5] = aShortArray9801[i_6].toInt()
            }
        }

        return ints_3
    }

    fun method15257(i_1: Int): IntArray {
        return when {
            i_1 < 0 -> anIntArray9798!!
            i_1 >= anIntArrayArray9800!!.size -> anIntArray9796!!
            else -> anIntArrayArray9800!![i_1]
        }
    }

    override fun postDecode() {
        if (anIntArrayArray9800 == null) {
            anIntArrayArray9800 = arrayOf(intArrayOf(0, 0), intArrayOf(4096, 4096))
        }

        if (anIntArrayArray9800!!.size < 2) {
            throw RuntimeException()
        } else {
            if (anInt9799 == 2) {
                method15259()
            }

            TextureRaster.caculateTable()
            method15258()
        }
    }

    fun method15258() {
        var i_2: Int
        var i_3: Int
        var i_4: Int
        var ints_5: IntArray
        var ints_6: IntArray
        var i_7: Int
        var i_8: Int
        var i_9: Int
        var i_10: Int

        when (anInt9799) {
            1 -> {
                for (i_2_loop in 0..256) {
                    i_3 = i_2_loop shl 4

                    i_4 = 1
                    while (i_4 < anIntArrayArray9800!!.size - 1 && anIntArrayArray9800!![i_4][0] <= i_3) {
                        i_4++
                    }

                    ints_5 = anIntArrayArray9800!![i_4 - 1]
                    ints_6 = anIntArrayArray9800!![i_4]
                    i_7 = (i_3 - ints_5[0] shl 12) / (ints_6[0] - ints_5[0])
                    i_8 = 4096 - TextureRaster.PALETTE_COS!![i_7 shr 5 and 0xff] shr 1
                    i_9 = 4096 - i_8
                    i_10 = i_8 * ints_6[1] + i_9 * ints_5[1] shr 12

                    if (i_10 <= -32768) {
                        i_10 = -32767
                    }

                    if (i_10 >= 32768) {
                        i_10 = 32767
                    }

                    aShortArray9801[i_2_loop] = i_10.toShort()
                }
            }
            2 -> {
                for (i_2_loop in 0..256) {
                    i_3 = i_2_loop shl 4

                    i_4 = 1
                    while (i_4 < anIntArrayArray9800!!.size - 1 && anIntArrayArray9800!![i_4][0] <= i_3) {
                        i_4++
                    }

                    ints_5 = anIntArrayArray9800!![i_4 - 1]
                    ints_6 = anIntArrayArray9800!![i_4]
                    i_7 = method15257(i_4 - 2)[1]
                    i_8 = ints_5[1]
                    i_9 = ints_6[1]
                    i_10 = method15257(i_4 + 1)[1]
                    val i_11 = (i_3 - ints_5[0] shl 12) / (ints_6[0] - ints_5[0])
                    val i_12 = i_11 * i_11 shr 12
                    val i_13 = i_8 + (i_10 - i_9 - i_7)
                    val i_14 = i_7 - i_8 - i_13
                    val i_15 = i_9 - i_7
                    val i_17 = i_12 * (i_13 * i_11 shr 12) shr 12
                    val i_18 = i_14 * i_12 shr 12
                    val i_19 = i_11 * i_15 shr 12
                    var i_20 = i_17 + i_18 + i_19 + i_8

                    if (i_20 <= -32768) {
                        i_20 = -32767
                    }

                    if (i_20 >= 32768) {
                        i_20 = 32767
                    }

                    aShortArray9801[i_2_loop] = i_20.toShort()
                }
            }
            else -> {
                for (i_2_loop in 0..256) {
                    i_3 = i_2_loop shl 4

                    i_4 = 1
                    while (i_4 < anIntArrayArray9800!!.size - 1 && anIntArrayArray9800!![i_4][0] <= i_3) {
                        i_4++
                    }

                    ints_5 = anIntArrayArray9800!![i_4 - 1]
                    ints_6 = anIntArrayArray9800!![i_4]
                    i_7 = (i_3 - ints_5[0] shl 12) / (ints_6[0] - ints_5[0])
                    i_8 = 4096 - i_7
                    i_9 = i_7 * ints_6[1] + i_8 * ints_5[1] shr 12

                    if (i_9 <= -32768) {
                        i_9 = -32767
                    }

                    if (i_9 >= 32768) {
                        i_9 = 32767
                    }

                    aShortArray9801[i_2_loop] = i_9.toShort()
                }
            }
        }
    }

    fun method15259() {
        val ints_2 = anIntArrayArray9800!![0]
        val ints_3 = anIntArrayArray9800!![1]
        val ints_4 = anIntArrayArray9800!![anIntArrayArray9800!!.size - 2]
        val ints_5 = anIntArrayArray9800!![anIntArrayArray9800!!.size - 1]
        anIntArray9798 = intArrayOf(
            ints_2[0] - ints_3[0] + ints_2[0],
            ints_2[1] - ints_3[1] + ints_2[1]
        )
        anIntArray9796 = intArrayOf(
            ints_4[0] - ints_5[0] + ints_4[0],
            ints_4[1] - ints_5[1] + ints_4[1]
        )
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            anInt9799 = buffer.readUByte()
            anIntArrayArray9800 = Array(buffer.readUByte()) { IntArray(2) }

            for (i_4 in anIntArrayArray9800!!.indices) {
                anIntArrayArray9800!![i_4][0] = buffer.readUShort()
                anIntArrayArray9800!![i_4][1] = buffer.readUShort()
            }
        }
    }
}
