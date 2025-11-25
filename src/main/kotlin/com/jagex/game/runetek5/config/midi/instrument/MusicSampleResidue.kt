package com.jagex.game.runetek5.config.midi.instrument

/**
 *
 * @author Brainpower
 * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
 *
 */
class MusicSampleResidue {

    @JvmField val anInt3285: Int = MusicSample.getBits(16)
    @JvmField val anInt3283: Int = MusicSample.getBits(24)
    @JvmField val anInt3281: Int = MusicSample.getBits(24)
    @JvmField val anInt3282: Int = MusicSample.getBits(24) + 1
    @JvmField val classifications: Int = MusicSample.getBits(6) + 1
    @JvmField val anInt3286: Int = MusicSample.getBits(8)
    @JvmField val residueBooks: IntArray

    init {
        val residueCascade = IntArray(classifications)
        for (j in 0 until classifications) {
            var highBits = 0
            val lowBits = MusicSample.getBits(3)
            if (MusicSample.getBit() != 0) {
                highBits = MusicSample.getBits(5)
            }

            residueCascade[j] = highBits shl 3 or lowBits
        }
        residueBooks = IntArray(classifications * 8)
        for (k in 0 until classifications * 8) {
            residueBooks[k] = if ((residueCascade[k shr 3] and (1 shl (k and 0x7))) != 0) MusicSample.getBits(8) else -1
        }
    }

    fun decodeResidue(output: FloatArray, outputSize: Int, isFloat: Boolean) {
        for (i in 0 until outputSize) {
            output[i] = 0.0f
        }

        if (!isFloat) {
            val classwords = MusicSample.codebooks[anInt3286].dimensions
            val windowSize = anInt3281 - anInt3283
            val numWindows = windowSize / anInt3282
            val classification = IntArray(numWindows)

            for (pass in 0 until 8) {
                var pcount = 0

                while (pcount < numWindows) {
                    if (pass == 0) {
                        var q = MusicSample.codebooks[anInt3286].decode()

                        for (i in classwords - 1 downTo 0) {
                            if (pcount + i < numWindows) {
                                classification[pcount + i] = q % classifications
                            }
                            q /= classifications
                        }
                    }

                    for (i in 0 until classwords) {
                        val c = classification[pcount]
                        val bookIndex = residueBooks[pass + c * 8]
                        if (bookIndex >= 0) {
                            val offset = pcount * anInt3282 + anInt3283
                            val codebook = MusicSample.codebooks[bookIndex]
                            val step: Int
                            if (anInt3285 == 0) {
                                step = anInt3282 / codebook.dimensions

                                for (index in 0 until step) {
                                    val values = codebook.method4382()

                                    for (dim in 0 until codebook.dimensions) {
                                        output[offset + index + dim * step] += values[dim]
                                    }
                                }
                            } else {
                                var pos = 0

                                while (pos < anInt3282) {
                                    val values = codebook.method4382()

                                    for (dim in 0 until codebook.dimensions) {
                                        output[offset + pos] += values[dim]
                                        ++pos
                                    }
                                }
                            }
                        }

                        if (++pcount >= numWindows) {
                            break
                        }
                    }
                }
            }
        }
    }
}
