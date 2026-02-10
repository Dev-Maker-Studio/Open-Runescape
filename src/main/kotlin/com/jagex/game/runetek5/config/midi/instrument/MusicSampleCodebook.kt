package com.jagex.game.runetek5.config.midi.instrument

import com.jagex.game.runetek5.utils.BufferUtils
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.pow

/**
 * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
 * @author Brainpower
 *
 */
class MusicSampleCodebook {

    @JvmField val dimensions: Int
    @JvmField val entries: Int
    @JvmField val lengths: IntArray
    @JvmField var mults: IntArray? = null
    @JvmField var multiplicands: Array<FloatArray>? = null
    @JvmField var huffmanTree: IntArray? = null

    init {
        MusicSample.getBits(24)
        dimensions = MusicSample.getBits(16)
        entries = MusicSample.getBits(24)
        lengths = IntArray(entries)
        val ordered = MusicSample.getBit() != 0
        var lookupType: Int
        if (ordered) {
            lookupType = 0
            var currentLength = MusicSample.getBits(5) + 1
            while (lookupType < entries) {
                val limit = MusicSample.getBits(BufferUtils.ilog(entries - lookupType))
                for (i_5 in 0 until limit) {
                    lengths[lookupType++] = currentLength
                }
                currentLength++
            }
        } else {
            val present = MusicSample.getBit() != 0
            for (j in 0 until entries) {
                if (present && MusicSample.getBit() == 0) {
                    lengths[j] = 0
                } else {
                    lengths[j] = MusicSample.getBits(5) + 1
                }
            }
        }

        computeAcceleratedHuffman()

        lookupType = MusicSample.getBits(4)
        if (lookupType > 0) {
            val minimumValue = MusicSample.float32Unpack(MusicSample.getBits(32))
            val deltaValue = MusicSample.float32Unpack(MusicSample.getBits(32))
            val valueBits = MusicSample.getBits(4) + 1
            val sequenceP = MusicSample.getBit() != 0
            val lookupValues: Int
            if (lookupType == 1) {
                lookupValues = lookup1Values(entries, dimensions)
            } else {
                lookupValues = entries * dimensions
            }

            mults = IntArray(lookupValues)
            for (j in 0 until lookupValues) {
                mults!![j] = MusicSample.getBits(valueBits)
            }

            multiplicands = Array(entries) { FloatArray(dimensions) }
            if (lookupType == 1) {
                for (i in 0 until entries) {
                    var last = 0.0f
                    var div = 1

                    for (j in 0 until dimensions) {
                        val off = i / div % lookupValues
                        val `val` = mults!![off] * deltaValue + minimumValue + last
                        multiplicands!![i][j] = `val`
                        if (sequenceP) {
                            last = `val`
                        }
                        div *= lookupValues
                    }
                }
            } else {
                for (i in 0 until entries) {
                    var last = 0.0f
                    var off = i * dimensions

                    for (j in 0 until dimensions) {
                        val `val` = mults!![off] * deltaValue + minimumValue + last
                        multiplicands!![i][j] = `val`
                        if (sequenceP) {
                            last = `val`
                        }
                        off++
                    }
                }
            }
        }

    }

    fun decodeVector(): FloatArray {
        return multiplicands!![decode()]
    }

    //buildhuffmantree
    fun computeAcceleratedHuffman() {
        val ints_1 = IntArray(entries)
        val ints_2 = IntArray(33)

        var i_3: Int
        var i_4: Int
        var i_5: Int
        var i_6: Int
        var i_7: Int
        var i_8: Int
        var i_9: Int
        for (i_3 in 0 until entries) {
            i_4 = lengths[i_3]
            if (i_4 != 0) {
                i_5 = 1 shl 32 - i_4
                i_6 = ints_2[i_4]
                ints_1[i_3] = i_6
                if ((i_6 and i_5) != 0) {
                    i_7 = ints_2[i_4 - 1]
                } else {
                    i_7 = i_6 or i_5

                    i_8 = i_4 - 1
                    while (i_8 >= 1) {
                        i_9 = ints_2[i_8]
                        if (i_9 != i_6) {
                            break
                        }

                        val i_12 = 1 shl 32 - i_8
                        if ((i_9 and i_12) != 0) {
                            ints_2[i_8] = ints_2[i_8 - 1]
                            break
                        }

                        ints_2[i_8] = i_9 or i_12
                        --i_8
                    }
                }

                ints_2[i_4] = i_7

                for (i_8 in i_4 + 1..32) {
                    i_9 = ints_2[i_8]
                    if (i_9 == i_6) {
                        ints_2[i_8] = i_7
                    }
                }
            }
        }

        huffmanTree = IntArray(8)
        i_3 = 0

        for (i_4 in 0 until entries) {
            i_5 = lengths[i_4]
            if (i_5 != 0) {
                i_6 = ints_1[i_4]
                i_7 = 0

                for (i_8 in 0 until i_5) {
                    i_9 = Int.MIN_VALUE ushr i_8
                    if ((i_6 and i_9) != 0) {
                        if (huffmanTree!![i_7] == 0) {
                            huffmanTree!![i_7] = i_3
                        }

                        i_7 = huffmanTree!![i_7]
                    } else {
                        ++i_7
                    }

                    if (i_7 >= huffmanTree!!.size) {
                        val ints_10 = IntArray(huffmanTree!!.size * 2)

                        System.arraycopy(huffmanTree, 0, ints_10, 0, huffmanTree!!.size)

                        huffmanTree = ints_10
                    }

                    i_9 = i_9 ushr 1
                }

                huffmanTree!![i_7] = i_4.inv()
                if (i_7 >= i_3) {
                    i_3 = i_7 + 1
                }
            }
        }

    }

    fun decode(): Int {
        var i_1 = 0
        while (huffmanTree!![i_1] >= 0) {
            i_1 = if (MusicSample.getBit() != 0) huffmanTree!![i_1] else i_1 + 1
        }

        return huffmanTree!![i_1].inv()
    }

    companion object {
        @JvmStatic
        fun lookup1Values(entries: Int, dim: Int): Int {
            var r = entries.toDouble().pow(1.0 / dim).toInt() + 1
            while (MathUtils.power(r, dim) > entries) {
                --r
            }
            return r
        }
    }

}
