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
                for (entryIndex in 0 until limit) {
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
        val codewords = IntArray(entries)
        val sortedCodes = IntArray(33)

        var entryIdx: Int
        var codeLength: Int
        var highBit: Int
        var codeword: Int
        var nextCode: Int
        var parentIdx: Int
        var parentCode: Int
        for (entryIdx in 0 until entries) {
            codeLength = lengths[entryIdx]
            if (codeLength != 0) {
                highBit = 1 shl 32 - codeLength
                codeword = sortedCodes[codeLength]
                codewords[entryIdx] = codeword
                if ((codeword and highBit) != 0) {
                    nextCode = sortedCodes[codeLength - 1]
                } else {
                    nextCode = codeword or highBit

                    parentIdx = codeLength - 1
                    while (parentIdx >= 1) {
                        parentCode = sortedCodes[parentIdx]
                        if (parentCode != codeword) {
                            break
                        }

                        val mask = 1 shl 32 - parentIdx
                        if ((parentCode and mask) != 0) {
                            sortedCodes[parentIdx] = sortedCodes[parentIdx - 1]
                            break
                        }

                        sortedCodes[parentIdx] = parentCode or mask
                        --parentIdx
                    }
                }

                sortedCodes[codeLength] = nextCode

                for (parentIdx in codeLength + 1..32) {
                    parentCode = sortedCodes[parentIdx]
                    if (parentCode == codeword) {
                        sortedCodes[parentIdx] = nextCode
                    }
                }
            }
        }

        huffmanTree = IntArray(8)
        entryIdx = 0

        for (codeLength in 0 until entries) {
            highBit = lengths[codeLength]
            if (highBit != 0) {
                codeword = codewords[codeLength]
                nextCode = 0

                for (parentIdx in 0 until highBit) {
                    parentCode = Int.MIN_VALUE ushr parentIdx
                    if ((codeword and parentCode) != 0) {
                        if (huffmanTree!![nextCode] == 0) {
                            huffmanTree!![nextCode] = entryIdx
                        }

                        nextCode = huffmanTree!![nextCode]
                    } else {
                        ++nextCode
                    }

                    if (nextCode >= huffmanTree!!.size) {
                        val codewords0 = IntArray(huffmanTree!!.size * 2)

                        System.arraycopy(huffmanTree, 0, codewords0, 0, huffmanTree!!.size)

                        huffmanTree = codewords0
                    }

                    parentCode = parentCode ushr 1
                }

                huffmanTree!![nextCode] = codeLength.inv()
                if (nextCode >= entryIdx) {
                    entryIdx = nextCode + 1
                }
            }
        }

    }

    fun decode(): Int {
        var treeIndex = 0
        while (huffmanTree!![treeIndex] >= 0) {
            treeIndex = if (MusicSample.getBit() != 0) huffmanTree!![treeIndex] else treeIndex + 1
        }

        return huffmanTree!![treeIndex].inv()
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
