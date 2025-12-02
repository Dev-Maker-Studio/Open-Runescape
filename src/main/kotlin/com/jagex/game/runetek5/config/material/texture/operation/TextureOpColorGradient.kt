package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpColorGradient : TextureOperation(1, false) {
    var anIntArrayArray9822: Array<IntArray>? = null
    val anIntArray9823 = IntArray(257)

    override fun getColorOutput(i_1: Int): Array<IntArray> {
        val ints_3 = colorImageCache.getPalette(i_1)
        if (colorImageCache.dirty) {
            val ints_4 = getChildMonochromeOutput(0, i_1)
            val ints_5 = ints_3[0]
            val ints_6 = ints_3[1]
            val ints_7 = ints_3[2]

            for (i_8 in 0 until TextureRaster.currentWidth) {
                var i_9 = ints_4[i_8] shr 4
                if (i_9 < 0) {
                    i_9 = 0
                }

                if (i_9 > 256) {
                    i_9 = 256
                }

                i_9 = this.anIntArray9823[i_9]
                ints_5[i_8] = (i_9 and 0xff0000) shr 12
                ints_6[i_8] = (i_9 and 0xff00) shr 4
                ints_7[i_8] = (i_9 and 0xff) shl 4
            }
        }

        return ints_3
    }

    fun method15268() {
        val i_2 = this.anIntArrayArray9822?.size ?: 0
        if (i_2 > 0) {
            for (i_3 in 0..256) {
                var i_4 = 0
                val i_5 = i_3 shl 4

                var i_6 = 0
                while (i_6 < i_2 && i_5 >= this.anIntArrayArray9822!![i_6][0]) {
                    ++i_4
                    i_6++
                }

                val i_7: Int
                val i_8: Int
                val ints_9: IntArray
                if (i_4 < i_2) {
                    ints_9 = this.anIntArrayArray9822!![i_4]
                    if (i_4 > 0) {
                        val ints_10 = this.anIntArrayArray9822!![i_4 - 1]
                        val i_11 = (i_5 - ints_10[0] shl 12) / (ints_9[0] - ints_10[0])
                        val i_12 = 4096 - i_11
                        i_6 = i_11 * ints_9[1] + i_12 * ints_10[1] shr 12
                        i_7 = i_11 * ints_9[2] + i_12 * ints_10[2] shr 12
                        i_8 = i_11 * ints_9[3] + i_12 * ints_10[3] shr 12
                    } else {
                        i_6 = ints_9[1]
                        i_7 = ints_9[2]
                        i_8 = ints_9[3]
                    }
                } else {
                    ints_9 = this.anIntArrayArray9822!![i_2 - 1]
                    i_6 = ints_9[1]
                    i_7 = ints_9[2]
                    i_8 = ints_9[3]
                }

                var red = i_6 shr 4
                var green = i_7 shr 4
                var blue = i_8 shr 4

                if (red < 0) {
                    red = 0
                } else if (red > 255) {
                    red = 255
                }

                if (green < 0) {
                    green = 0
                } else if (green > 255) {
                    green = 255
                }

                if (blue < 0) {
                    blue = 0
                } else if (blue > 255) {
                    blue = 255
                }

                this.anIntArray9823[i_3] = red shl 16 or (green shl 8) or blue
            }
        }
    }

    fun method15269(i_1: Int) {
        if (i_1 != 0) {
            when (i_1) {
                1 -> {
                    this.anIntArrayArray9822 = Array(2) { IntArray(4) }
                    this.anIntArrayArray9822!![0][0] = 0
                    this.anIntArrayArray9822!![0][1] = 0
                    this.anIntArrayArray9822!![0][2] = 0
                    this.anIntArrayArray9822!![0][3] = 0
                    this.anIntArrayArray9822!![1][0] = 4096
                    this.anIntArrayArray9822!![1][1] = 4096
                    this.anIntArrayArray9822!![1][2] = 4096
                    this.anIntArrayArray9822!![1][3] = 4096
                }
                2 -> {
                    this.anIntArrayArray9822 = Array(8) { IntArray(4) }
                    this.anIntArrayArray9822!![0] = intArrayOf(0, 2650, 2602, 2361)
                    this.anIntArrayArray9822!![1] = intArrayOf(2867, 2313, 1799, 1558)
                    this.anIntArrayArray9822!![2] = intArrayOf(3072, 2618, 1734, 1413)
                    this.anIntArrayArray9822!![3] = intArrayOf(3276, 2296, 1220, 947)
                    this.anIntArrayArray9822!![4] = intArrayOf(3481, 2072, 963, 722)
                    this.anIntArrayArray9822!![5] = intArrayOf(3686, 2730, 2152, 1766)
                    this.anIntArrayArray9822!![6] = intArrayOf(3891, 2232, 1060, 915)
                    this.anIntArrayArray9822!![7] = intArrayOf(4096, 1686, 1413, 1140)
                }
                3 -> {
                    this.anIntArrayArray9822 = Array(7) { IntArray(4) }
                    this.anIntArrayArray9822!![0] = intArrayOf(0, 0, 0, 4096)
                    this.anIntArrayArray9822!![1] = intArrayOf(663, 0, 4096, 4096)
                    this.anIntArrayArray9822!![2] = intArrayOf(1363, 0, 4096, 0)
                    this.anIntArrayArray9822!![3] = intArrayOf(2048, 4096, 4096, 0)
                    this.anIntArrayArray9822!![4] = intArrayOf(2727, 4096, 0, 0)
                    this.anIntArrayArray9822!![5] = intArrayOf(3411, 4096, 0, 4096)
                    this.anIntArrayArray9822!![6] = intArrayOf(4096, 0, 0, 4096)
                }
                4 -> {
                    this.anIntArrayArray9822 = Array(6) { IntArray(4) }
                    this.anIntArrayArray9822!![0] = intArrayOf(0, 0, 0, 0)
                    this.anIntArrayArray9822!![1] = intArrayOf(1843, 0, 0, 1493)
                    this.anIntArrayArray9822!![2] = intArrayOf(2457, 0, 0, 2939)
                    this.anIntArrayArray9822!![3] = intArrayOf(2781, 0, 1124, 3565)
                    this.anIntArrayArray9822!![4] = intArrayOf(3481, 546, 3084, 4031)
                    this.anIntArrayArray9822!![5] = intArrayOf(4096, 4096, 4096, 4096)
                }
                5 -> {
                    this.anIntArrayArray9822 = Array(16) { IntArray(4) }
                    this.anIntArrayArray9822!![0] = intArrayOf(0, 80, 192, 321)
                    this.anIntArrayArray9822!![1] = intArrayOf(155, 321, 449, 562)
                    this.anIntArrayArray9822!![2] = intArrayOf(389, 578, 690, 803)
                    this.anIntArrayArray9822!![3] = intArrayOf(671, 947, 995, 1140)
                    this.anIntArrayArray9822!![4] = intArrayOf(897, 1285, 1397, 1509)
                    this.anIntArrayArray9822!![5] = intArrayOf(1175, 1525, 1429, 1413)
                    this.anIntArrayArray9822!![6] = intArrayOf(1368, 1734, 1461, 1333)
                    this.anIntArrayArray9822!![7] = intArrayOf(1507, 1413, 1525, 1702)
                    this.anIntArrayArray9822!![8] = intArrayOf(1736, 1108, 1590, 2056)
                    this.anIntArrayArray9822!![9] = intArrayOf(2088, 1766, 2056, 2666)
                    this.anIntArrayArray9822!![10] = intArrayOf(2355, 2409, 2586, 3276)
                    this.anIntArrayArray9822!![11] = intArrayOf(2691, 3116, 3148, 3228)
                    this.anIntArrayArray9822!![12] = intArrayOf(3031, 3806, 3710, 3196)
                    this.anIntArrayArray9822!![13] = intArrayOf(3522, 3437, 3421, 3019)
                    this.anIntArrayArray9822!![14] = intArrayOf(3727, 3116, 3148, 3228)
                    this.anIntArrayArray9822!![15] = intArrayOf(4096, 2377, 2505, 2746)
                }
                6 -> {
                    this.anIntArrayArray9822 = Array(4) { IntArray(4) }
                    this.anIntArrayArray9822!![0] = intArrayOf(2048, 0, 4096, 0)
                    this.anIntArrayArray9822!![1] = intArrayOf(2867, 4096, 4096, 0)
                    this.anIntArrayArray9822!![2] = intArrayOf(3276, 4096, 4096, 0)
                    this.anIntArrayArray9822!![3] = intArrayOf(4096, 4096, 0, 0)
                }
                else -> throw RuntimeException()
            }
        }
    }

    override fun postDecode() {
        if (this.anIntArrayArray9822 == null) {
            method15269(1)
        }

        method15268()
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        if (i_1 == 0) {
            val i_4 = rsbytebuffer2.readUByte()
            if (i_4 == 0) {
                this.anIntArrayArray9822 = Array(rsbytebuffer2.readUByte()) { IntArray(4) }

                for (i_5 in this.anIntArrayArray9822!!.indices) {
                    this.anIntArrayArray9822!![i_5][0] = rsbytebuffer2.readUShort()
                    this.anIntArrayArray9822!![i_5][1] = rsbytebuffer2.readUByte() shl 4
                    this.anIntArrayArray9822!![i_5][2] = rsbytebuffer2.readUByte() shl 4
                    this.anIntArrayArray9822!![i_5][3] = rsbytebuffer2.readUByte() shl 4
                }
            } else {
                method15269(i_4)
            }
        }
    }
}
