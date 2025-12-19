package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.config.midi.MixedAudioBuffer
import com.jagex.game.runetek5.sound.player.PcmPlayerManager

open class MixedAudioStream(buffer: MixedAudioBuffer, sampleRate: Int, volume: Int, i_4: Int) : AreaSoundStream() {

    init {
        this.buffer = buffer
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        run {
            this.start = buffer.start
            this.end = buffer.end
            this.enableLoop = buffer.enableLoop
        }
        this.sampleRate = sampleRate
        this.volume = volume
        this.anInt9847 = i_4
        this.anInt9853 = 0
        method15332()
    }

    @Synchronized
    override fun fill(ints_1: IntArray, i_2: Int, i_3: Int) {
        if (super.volume == 0 && super.anInt9858 == 0) {
            skip(i_3)
        } else {
            val i_4 = super.start shl 8
            val i_5 = super.end shl 8
            val i_6 = ((buffer as MixedAudioBuffer).samples.size shl 8)
            val i_7 = i_5 - i_4
            if (i_7 <= 0) {
                super.anInt9854 = 0
            }

            var i_8 = i_2
            val i_3_end = i_3 + i_2
            if (super.anInt9853 < 0) {
                if (super.sampleRate <= 0) {
                    method15354()
                    unlink()
                    return
                }

                super.anInt9853 = 0
            }

            if (super.anInt9853 >= i_6) {
                if (super.sampleRate >= 0) {
                    method15354()
                    unlink()
                    return
                }

                super.anInt9853 = i_6 - 1
            }

            if (super.anInt9854 < 0) {
                if (super.enableLoop) {
                    if (super.sampleRate < 0) {
                        i_8 = method16018(ints_1, i_2, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.anInt9853 >= i_4) {
                            return
                        }

                        super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }

                    while (true) {
                        i_8 = method15990(ints_1, i_8, i_5, i_3_end, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                        if (super.anInt9853 < i_5) {
                            break
                        }

                        super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                        i_8 = method16018(ints_1, i_8, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.anInt9853 >= i_4) {
                            break
                        }

                        super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }
                } else if (super.sampleRate < 0) {
                    while (true) {
                        i_8 = method16018(ints_1, i_8, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                        if (super.anInt9853 >= i_4) {
                            break
                        }

                        super.anInt9853 = i_5 - 1 - (i_5 - 1 - super.anInt9853) % i_7
                    }
                } else {
                    while (true) {
                        i_8 = method15990(ints_1, i_8, i_5, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.anInt9853 < i_5) {
                            break
                        }

                        super.anInt9853 = i_4 + (super.anInt9853 - i_4) % i_7
                    }
                }
            } else {
                if (super.anInt9854 > 0) {
                    if (super.enableLoop) {
                        run loop@ {
                            if (super.sampleRate < 0) {
                                i_8 = method16018(ints_1, i_2, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.anInt9853 >= i_4) {
                                    return@fill
                                }

                                super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.anInt9854 == 0) {
                                    return@loop
                                }
                            }

                            do {
                                i_8 = method15990(ints_1, i_8, i_5, i_3_end, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                                if (super.anInt9853 < i_5) {
                                    return@fill
                                }

                                super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.anInt9854 == 0) {
                                    break
                                }

                                i_8 = method16018(ints_1, i_8, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.anInt9853 >= i_4) {
                                    return@fill
                                }

                                super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            } while (--super.anInt9854 != 0)
                        }
                    } else {
                        var i_9: Int
                        if (super.sampleRate < 0) {
                            while (true) {
                                i_8 = method16018(ints_1, i_8, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                                if (super.anInt9853 >= i_4) {
                                    return
                                }

                                i_9 = (i_5 - 1 - super.anInt9853) / i_7
                                if (i_9 >= super.anInt9854) {
                                    super.anInt9853 += i_7 * super.anInt9854
                                    super.anInt9854 = 0
                                    break
                                }

                                super.anInt9853 += i_7 * i_9
                                super.anInt9854 -= i_9
                            }
                        } else {
                            while (true) {
                                i_8 = method15990(ints_1, i_8, i_5, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.anInt9853 < i_5) {
                                    return
                                }

                                i_9 = (super.anInt9853 - i_4) / i_7
                                if (i_9 >= super.anInt9854) {
                                    super.anInt9853 -= i_7 * super.anInt9854
                                    super.anInt9854 = 0
                                    break
                                }

                                super.anInt9853 -= i_7 * i_9
                                super.anInt9854 -= i_9
                            }
                        }
                    }
                }

                if (super.sampleRate < 0) {
                    method16018(ints_1, i_8, 0, i_3_end, 0)
                    if (super.anInt9853 < 0) {
                        super.anInt9853 = -1
                        method15354()
                        unlink()
                    }
                } else {
                    method15990(ints_1, i_8, i_6, i_3_end, 0)
                    if (super.anInt9853 >= i_6) {
                        super.anInt9853 = i_6
                        method15354()
                        unlink()
                    }
                }
            }
        }

    }

    fun method15990(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Int {
        var i_2_var = i_2
        do {
            if (super.anInt9858 <= 0) {
                if (super.sampleRate == 256 && (super.anInt9853 and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return method16031((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9845, super.anInt9850, i_4, i_3, this)
                    }

                    return method15992((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9848, i_4, i_3, this)
                }

                if (PcmPlayerManager.stereo) {
                    return method16019((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9845, super.anInt9850, i_4, i_3, this, super.sampleRate, i_5)
                }

                return method15996((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9848, i_4, i_3, this, super.sampleRate, i_5)
            }

            var i_6 = i_2_var + super.anInt9858
            if (i_6 > i_4) {
                i_6 = i_4
            }

            super.anInt9858 += i_2_var
            if (super.sampleRate == 256 && (super.anInt9853 and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    i_2_var = method16001((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this)
                } else {
                    i_2_var = method16000((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9848, super.anInt9849, i_6, i_3, this)
                }
            } else if (PcmPlayerManager.stereo) {
                i_2_var = method16005((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this, super.sampleRate, i_5)
            } else {
                i_2_var = method16004((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9848, super.anInt9849, i_6, i_3, this, super.sampleRate, i_5)
            }

            super.anInt9858 -= i_2_var
            if (super.anInt9858 != 0) {
                return i_2_var
            }
        } while (!method15324())

        return i_4
    }

    fun method16018(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Int {
        var i_2_var = i_2
        do {
            if (super.anInt9858 <= 0) {
                if (super.sampleRate == -256 && (super.anInt9853 and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return method15995((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9845, super.anInt9850, i_4, i_3, this)
                    }

                    return method15991((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9848, i_4, i_3, this)
                }

                if (PcmPlayerManager.stereo) {
                    return method15999((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9845, super.anInt9850, i_4, i_3, this, super.sampleRate, i_5)
                }

                return method15998((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9848, i_4, i_3, this, super.sampleRate, i_5)
            }

            var i_6 = i_2_var + super.anInt9858
            if (i_6 > i_4) {
                i_6 = i_4
            }

            super.anInt9858 += i_2_var
            if (super.sampleRate == -256 && (super.anInt9853 and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    i_2_var = method16003((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this)
                } else {
                    i_2_var = method16002((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9848, super.anInt9849, i_6, i_3, this)
                }
            } else if (PcmPlayerManager.stereo) {
                i_2_var = method16007((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this, super.sampleRate, i_5)
            } else {
                i_2_var = method16006((buffer as MixedAudioBuffer).samples, ints_1, super.anInt9853, i_2_var, super.anInt9848, super.anInt9849, i_6, i_3, this, super.sampleRate, i_5)
            }

            super.anInt9858 -= i_2_var
            if (super.anInt9858 != 0) {
                return i_2_var
            }
        } while (!method15324())

        return i_4
    }

    companion object {
        @JvmStatic
        fun method15991(bytes_0: ByteArray, ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_6: Int, i_7: Int, class282Sub15Sub5Sub28: MixedAudioStream): Int {
            var i_51 = 0
            var i_2_var = i_2 shr 8
            var i_3_var = i_3
            var i_4_var = i_4 shl 2
            var i_7_var = i_7 shr 8
            if ((i_3_var + i_2_var - (i_7_var - 1)).also { i_51 = it } > i_6) {
                i_51 = i_6
            }

            i_51 -= 3
            while (i_3_var < i_51) {
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
            }

            i_51 += 3
            while (i_3_var < i_51) {
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
            }

            class282Sub15Sub5Sub28.anInt9853 = i_2_var shl 8
            return i_3_var
        }

        @JvmStatic
        fun method15992(bytes_0: ByteArray, ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_6: Int, i_7: Int, class282Sub15Sub5Sub28: MixedAudioStream): Int {
            var i_51 = 0
            var i_2_var = i_2 shr 8
            var i_3_var = i_3
            var i_4_var = i_4 shl 2
            var i_7_var = i_7 shr 8
            if ((i_3_var + i_7_var - i_2_var).also { i_51 = it } > i_6) {
                i_51 = i_6
            }

            i_51 -= 3
            while (i_3_var < i_51) {
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
            }

            i_51 += 3
            while (i_3_var < i_51) {
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
            }

            class282Sub15Sub5Sub28.anInt9853 = i_2_var shl 8
            return i_3_var
        }

        @JvmStatic
        fun method15995(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_8: Int, i_9: Int, class282Sub15Sub5Sub210: MixedAudioStream): Int {
            var i_71 = 0
            var i_3_var = i_3 shr 8
            var i_4_var = i_4
            var i_5_var = i_5 shl 2
            var i_6_var = i_6 shl 2
            var i_9_var = i_9 shr 8
            if ((i_3_var + i_4_var - (i_9 - 1)).also { i_71 = it } > i_8) {
                i_71 = i_8
            }

            i_4_var = i_4_var shl 1
            i_71 = i_71 shl 1

            i_71 -= 6
            while (i_4_var < i_71) {
                var b_11 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
                b_11 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
                b_11 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
                b_11 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
            }

            i_71 += 6
            while (i_4_var < i_71) {
                val b_11 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
            }

            class282Sub15Sub5Sub210.anInt9853 = i_3_var shl 8
            return i_4_var shr 1
        }

        @JvmStatic
        fun method15996(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_8: Int, i_9: Int, class282Sub15Sub5Sub210: MixedAudioStream, i_11: Int, i_12: Int): Int {
            var i_71 = 0
            var i_13 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            if (i_11 == 0 || (i_5_var + (i_11 + (i_9 - i_4_var) - 257) / i_11).also { i_71 = it } > i_8) {
                i_71 = i_8
            }

            while (i_5_var < i_71) {
                i_13 = i_4_var shr 8
                val b_13 = bytes_2[i_13]
                ints_3[i_5_var++] += ((b_13.toInt() shl 8) + (bytes_2[i_13 + 1] - b_13) * (i_4_var and 0xff)) * i_6 shr 6
                i_4_var += i_11
            }

            if (i_11 == 0 || (i_5_var + (i_11 + (i_9 - i_4_var) - 1) / i_11).also { i_71 = it } > i_8) {
                i_71 = i_8
            }

            i_13 = i_12; while (i_5_var < i_71) { i_4_var += i_11;
                val b_13 = bytes_2[i_4_var shr 8]
                ints_3[i_5_var++] += ((b_13.toInt() shl 8) + (i_13 - b_13) * (i_4_var and 0xff)) * i_6 shr 6
            }

            class282Sub15Sub5Sub210.anInt9853 = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun method15998(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_8: Int, i_9: Int, class282Sub15Sub5Sub210: MixedAudioStream, i_11: Int, i_12: Int): Int {
            var i_71 = 0
            var i_13 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            if (i_11 == 0 || (i_5_var + (i_11 + (i_9 + 256 - i_4_var)) / i_11).also { i_71 = it } > i_8) {
                i_71 = i_8
            }

            while (i_5_var < i_71) {
                i_13 = i_4_var shr 8
                val b_13 = bytes_2[i_13 - 1]
                ints_3[i_5_var++] += ((b_13.toInt() shl 8) + (bytes_2[i_13] - b_13) * (i_4_var and 0xff)) * i_6 shr 6
                i_4_var += i_11
            }

            if (i_11 == 0 || (i_5_var + (i_11 + (i_9 - i_4_var)) / i_11).also { i_71 = it } > i_8) {
                i_71 = i_8
            }

            var i_01 = i_12

            i_13 = i_11; while (i_5_var < i_71) { i_4_var += i_13;
                ints_3[i_5_var++] += ((i_01.toInt() shl 8) + (bytes_2[i_4_var shr 8] - i_01) * (i_4_var and 0xff)) * i_6 shr 6
            }

            class282Sub15Sub5Sub210.anInt9853 = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun method15999(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_9: Int, i_10: Int, class282Sub15Sub5Sub211: MixedAudioStream, i_12: Int, i_13: Int): Int {
            var i_81 = 0
            var i_11 = 0
            var i_01 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            if (i_12 == 0 || (i_5_var + (i_10 + 256 - i_4_var + i_12) / i_12).also { i_81 = it } > i_9) {
                i_81 = i_9
            }

            i_5_var = i_5_var shl 1

            i_81 = i_81 shl 1; while (i_5_var < i_81) { i_4_var += i_12;
                i_11 = i_4_var shr 8
                val b_14 = bytes_2[i_11 - 1]
                i_01 = (bytes_2[i_11] - b_14) * (i_4_var and 0xff) + (b_14.toInt() shl 8)
                ints_3[i_5_var++] += i_01 * i_6 shr 6
                ints_3[i_5_var++] += i_01 * i_7 shr 6
            }

            if (i_12 == 0 || ((i_5_var shr 1) + (i_10 - i_4_var + i_12) / i_12).also { i_81 = it } > i_9) {
                i_81 = i_9
            }

            i_81 = i_81 shl 1

            i_11 = i_13; while (i_5_var < i_81) { i_4_var += i_12;
                i_01 = (i_11.toInt() shl 8) + (i_4_var and 0xff) * (bytes_2[i_4_var shr 8] - i_11)
                ints_3[i_5_var++] += i_01 * i_6 shr 6
                ints_3[i_5_var++] += i_01 * i_7 shr 6
            }

            class282Sub15Sub5Sub211.anInt9853 = i_4_var
            return i_5_var shr 1
        }

        @JvmStatic
        fun method16000(bytes_0: ByteArray, ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_7: Int, i_8: Int, class282Sub15Sub5Sub29: MixedAudioStream): Int {
            var i_61 = 0
            var i_2_var = i_2 shr 8
            var i_3_var = i_3
            var i_4_var = i_4 shl 2
            var i_5_var = i_5 shl 2
            var i_8_var = i_8 shr 8
            if ((i_3_var + i_8 - i_2_var).also { i_61 = it } > i_7) {
                i_61 = i_7
            }

            class282Sub15Sub5Sub29.anInt9845 += class282Sub15Sub5Sub29.anInt9852 * (i_61 - i_3_var)
            class282Sub15Sub5Sub29.anInt9850 += class282Sub15Sub5Sub29.anInt9851 * (i_61 - i_3_var)

            i_61 -= 3; while (i_3_var < i_61) { i_4_var += i_5_var;
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
                i_4_var += i_5_var
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
                i_4_var += i_5_var
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
                i_4_var += i_5_var
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
            }

            i_61 += 3; while (i_3_var < i_61) { i_4_var += i_5_var;
                ints_1[i_3_var++] += bytes_0[i_2_var++] * i_4_var
            }

            class282Sub15Sub5Sub29.anInt9848 = i_4_var shr 2
            class282Sub15Sub5Sub29.anInt9853 = i_2_var shl 8
            return i_3_var
        }

        @JvmStatic
        fun method16001(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_10: Int, i_11: Int, class282Sub15Sub5Sub212: MixedAudioStream): Int {
            var i_91 = 0
            var i_3_var = i_3 shr 8
            var i_4_var = i_4
            var i_5_var = i_5 shl 2
            var i_6_var = i_6 shl 2
            var i_7_var = i_7 shl 2
            var i_8_var = i_8 shl 2
            var i_11_var = i_11 shr 8
            if ((i_11 + i_4_var - i_3_var).also { i_91 = it } > i_10) {
                i_91 = i_10
            }

            class282Sub15Sub5Sub212.anInt9848 += class282Sub15Sub5Sub212.anInt9849 * (i_91 - i_4_var)
            i_4_var = i_4_var shl 1
            i_91 = i_91 shl 1

            i_91 -= 6; while (i_4_var < i_91) { i_6_var += i_8_var;
                var b_13 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
                i_6_var += i_8_var
                b_13 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
                i_6_var += i_8_var
                b_13 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
                i_6_var += i_8_var
                b_13 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
            }

            i_91 += 6; while (i_4_var < i_91) { i_6_var += i_8_var;
                val b_13 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
            }

            class282Sub15Sub5Sub212.anInt9845 = i_5_var shr 2
            class282Sub15Sub5Sub212.anInt9850 = i_6_var shr 2
            class282Sub15Sub5Sub212.anInt9853 = i_3_var shl 8
            return i_4_var shr 1
        }

        @JvmStatic
        fun method16002(bytes_0: ByteArray, ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_7: Int, i_8: Int, class282Sub15Sub5Sub29: MixedAudioStream): Int {
            var i_61 = 0
            var i_2_var = i_2 shr 8
            var i_3_var = i_3
            var i_4_var = i_4 shl 2
            var i_5_var = i_5 shl 2
            var i_8_var = i_8 shr 8
            if ((i_3_var + i_2_var - (i_8 - 1)).also { i_61 = it } > i_7) {
                i_61 = i_7
            }

            class282Sub15Sub5Sub29.anInt9845 += class282Sub15Sub5Sub29.anInt9852 * (i_61 - i_3_var)
            class282Sub15Sub5Sub29.anInt9850 += class282Sub15Sub5Sub29.anInt9851 * (i_61 - i_3_var)

            i_61 -= 3; while (i_3_var < i_61) { i_4_var += i_5_var;
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
                i_4_var += i_5_var
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
                i_4_var += i_5_var
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
                i_4_var += i_5_var
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
            }

            i_61 += 3; while (i_3_var < i_61) { i_4_var += i_5_var;
                ints_1[i_3_var++] += bytes_0[i_2_var--] * i_4_var
            }

            class282Sub15Sub5Sub29.anInt9848 = i_4_var shr 2
            class282Sub15Sub5Sub29.anInt9853 = i_2_var shl 8
            return i_3_var
        }

        @JvmStatic
        fun method16003(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_10: Int, i_11: Int, class282Sub15Sub5Sub212: MixedAudioStream): Int {
            var i_91 = 0
            var i_3_var = i_3 shr 8
            var i_4_var = i_4
            var i_5_var = i_5 shl 2
            var i_6_var = i_6 shl 2
            var i_7_var = i_7 shl 2
            var i_8_var = i_8 shl 2
            var i_11_var = i_11 shr 8
            if ((i_3_var + i_4_var - (i_11 - 1)).also { i_91 = it } > i_10) {
                i_91 = i_10
            }

            class282Sub15Sub5Sub212.anInt9848 += class282Sub15Sub5Sub212.anInt9849 * (i_91 - i_4_var)
            i_4_var = i_4_var shl 1
            i_91 = i_91 shl 1

            i_91 -= 6; while (i_4_var < i_91) { i_6_var += i_8_var;
                var b_13 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
                i_6_var += i_8_var
                b_13 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
                i_6_var += i_8_var
                b_13 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
                i_6_var += i_8_var
                b_13 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
            }

            i_91 += 6; while (i_4_var < i_91) { i_6_var += i_8_var;
                val b_13 = bytes_1[i_3_var--]
                ints_2[i_4_var++] += b_13 * i_5_var
                i_5_var += i_7_var
                ints_2[i_4_var++] += b_13 * i_6_var
            }

            class282Sub15Sub5Sub212.anInt9845 = i_5_var shr 2
            class282Sub15Sub5Sub212.anInt9850 = i_6_var shr 2
            class282Sub15Sub5Sub212.anInt9853 = i_3_var shl 8
            return i_4_var shr 1
        }

        @JvmStatic
        fun method16004(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_9: Int, i_10: Int, class282Sub15Sub5Sub211: MixedAudioStream, i_12: Int, i_13: Int): Int {
            var i_81 = 0
            var i_11 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            var i_6_var = i_6
            class282Sub15Sub5Sub211.anInt9845 -= class282Sub15Sub5Sub211.anInt9852 * i_5_var
            class282Sub15Sub5Sub211.anInt9850 -= class282Sub15Sub5Sub211.anInt9851 * i_5_var
            if (i_12 == 0 || (i_5_var + (i_10 - i_4_var + i_12 - 257) / i_12).also { i_81 = it } > i_9) {
                i_81 = i_9
            }

            while (i_5_var < i_81) {
                i_11 = i_4_var shr 8
                val b_14 = bytes_2[i_11]
                ints_3[i_5_var++] += ((b_14.toInt() shl 8) + (bytes_2[i_11 + 1] - b_14) * (i_4_var and 0xff)) * i_6_var shr 6
                i_6_var += i_7
                i_4_var += i_12
            }

            if (i_12 == 0 || (i_5_var + (i_10 - i_4_var + i_12 - 1) / i_12).also { i_81 = it } > i_9) {
                i_81 = i_9
            }

            i_11 = i_13; while (i_5_var < i_81) { i_4_var += i_12;
                val b_14 = bytes_2[i_4_var shr 8]
                ints_3[i_5_var++] += ((b_14.toInt() shl 8) + (i_11 - b_14) * (i_4_var and 0xff)) * i_6_var shr 6
                i_6_var += i_7
            }

            class282Sub15Sub5Sub211.anInt9845 += class282Sub15Sub5Sub211.anInt9852 * i_5_var
            class282Sub15Sub5Sub211.anInt9850 += class282Sub15Sub5Sub211.anInt9851 * i_5_var
            class282Sub15Sub5Sub211.anInt9848 = i_6_var
            class282Sub15Sub5Sub211.anInt9853 = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun method16005(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, i_11: Int, i_12: Int, class282Sub15Sub5Sub213: MixedAudioStream, i_14: Int, i_15: Int): Int {
            var i_101 = 0
            var i_13 = 0
            var i_01 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            var i_6_var = i_6
            var i_7_var = i_7
            class282Sub15Sub5Sub213.anInt9848 -= i_5_var * class282Sub15Sub5Sub213.anInt9849
            if (i_14 == 0 || (i_5_var + (i_12 - i_4_var + i_14 - 257) / i_14).also { i_101 = it } > i_11) {
                i_101 = i_11
            }

            i_5_var = i_5_var shl 1

            i_101 = i_101 shl 1; while (i_5_var < i_101) { i_4_var += i_14;
                i_13 = i_4_var shr 8
                val b_16 = bytes_2[i_13]
                i_01 = (b_16.toInt() shl 8) + (i_4_var and 0xff) * (bytes_2[i_13 + 1] - b_16)
                ints_3[i_5_var++] += i_01 * i_6_var shr 6
                i_6_var += i_8
                ints_3[i_5_var++] += i_01 * i_7_var shr 6
                i_7_var += i_9
            }

            if (i_14 == 0 || ((i_5_var shr 1) + (i_12 - i_4_var + i_14 - 1) / i_14).also { i_101 = it } > i_11) {
                i_101 = i_11
            }

            i_101 = i_101 shl 1

            i_13 = i_15; while (i_5_var < i_101) { i_4_var += i_14;
                val b_16 = bytes_2[i_4_var shr 8]
                i_01 = (b_16.toInt() shl 8) + (i_13 - b_16) * (i_4_var and 0xff)
                ints_3[i_5_var++] += i_01 * i_6_var shr 6
                i_6_var += i_8
                ints_3[i_5_var++] += i_01 * i_7_var shr 6
                i_7_var += i_9
            }

            i_5_var = i_5_var shr 1
            class282Sub15Sub5Sub213.anInt9848 += class282Sub15Sub5Sub213.anInt9849 * i_5_var
            class282Sub15Sub5Sub213.anInt9845 = i_6_var
            class282Sub15Sub5Sub213.anInt9850 = i_7_var
            class282Sub15Sub5Sub213.anInt9853 = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun method16006(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_9: Int, i_10: Int, class282Sub15Sub5Sub211: MixedAudioStream, i_12: Int, i_13: Int): Int {
            var i_81 = 0
            var i_11 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            var i_6_var = i_6
            class282Sub15Sub5Sub211.anInt9845 -= class282Sub15Sub5Sub211.anInt9852 * i_5_var
            class282Sub15Sub5Sub211.anInt9850 -= class282Sub15Sub5Sub211.anInt9851 * i_5_var
            if (i_12 == 0 || (i_5_var + (i_10 + 256 - i_4_var + i_12) / i_12).also { i_81 = it } > i_9) {
                i_81 = i_9
            }

            while (i_5_var < i_81) {
                i_11 = i_4_var shr 8
                val b_14 = bytes_2[i_11 - 1]
                ints_3[i_5_var++] += ((b_14.toInt() shl 8) + (bytes_2[i_11] - b_14) * (i_4_var and 0xff)) * i_6_var shr 6
                i_6_var += i_7
                i_4_var += i_12
            }

            if (i_12 == 0 || (i_5_var + (i_10 - i_4_var + i_12) / i_12).also { i_81 = it } > i_9) {
                i_81 = i_9
            }

            var i_01 = i_13

            i_11 = i_12; while (i_5_var < i_81) { i_4_var += i_11;
                ints_3[i_5_var++] += ((i_01.toInt() shl 8) + (bytes_2[i_4_var shr 8] - i_01) * (i_4_var and 0xff)) * i_6_var shr 6
                i_6_var += i_7
            }

            class282Sub15Sub5Sub211.anInt9845 += class282Sub15Sub5Sub211.anInt9852 * i_5_var
            class282Sub15Sub5Sub211.anInt9850 += class282Sub15Sub5Sub211.anInt9851 * i_5_var
            class282Sub15Sub5Sub211.anInt9848 = i_6_var
            class282Sub15Sub5Sub211.anInt9853 = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun method16007(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, i_11: Int, i_12: Int, class282Sub15Sub5Sub213: MixedAudioStream, i_14: Int, i_15: Int): Int {
            var i_101 = 0
            var i_13 = 0
            var i_01 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            var i_6_var = i_6
            var i_7_var = i_7
            class282Sub15Sub5Sub213.anInt9848 -= i_5_var * class282Sub15Sub5Sub213.anInt9849
            if (i_14 == 0 || (i_5_var + (i_12 + 256 - i_4_var + i_14) / i_14).also { i_101 = it } > i_11) {
                i_101 = i_11
            }

            i_5_var = i_5_var shl 1

            i_101 = i_101 shl 1; while (i_5_var < i_101) { i_4_var += i_14;
                i_13 = i_4_var shr 8
                val b_16 = bytes_2[i_13 - 1]
                i_01 = (bytes_2[i_13] - b_16) * (i_4_var and 0xff) + (b_16.toInt() shl 8)
                ints_3[i_5_var++] += i_01 * i_6_var shr 6
                i_6_var += i_8
                ints_3[i_5_var++] += i_01 * i_7_var shr 6
                i_7_var += i_9
            }

            if (i_14 == 0 || ((i_5_var shr 1) + (i_12 - i_4_var + i_14) / i_14).also { i_101 = it } > i_11) {
                i_101 = i_11
            }

            i_101 = i_101 shl 1

            i_13 = i_15; while (i_5_var < i_101) { i_4_var += i_14;
                i_01 = (i_13.toInt() shl 8) + (i_4_var and 0xff) * (bytes_2[i_4_var shr 8] - i_13)
                ints_3[i_5_var++] += i_01 * i_6_var shr 6
                i_6_var += i_8
                ints_3[i_5_var++] += i_01 * i_7_var shr 6
                i_7_var += i_9
            }

            i_5_var = i_5_var shr 1
            class282Sub15Sub5Sub213.anInt9848 += class282Sub15Sub5Sub213.anInt9849 * i_5_var
            class282Sub15Sub5Sub213.anInt9845 = i_6_var
            class282Sub15Sub5Sub213.anInt9850 = i_7_var
            class282Sub15Sub5Sub213.anInt9853 = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun method16019(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_9: Int, i_10: Int, class282Sub15Sub5Sub211: MixedAudioStream, i_12: Int, i_13: Int): Int {
            var i_81 = 0
            var i_11 = 0
            var i_01 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            if (i_12 == 0 || (i_5_var + (i_10 - i_4_var + i_12 - 257) / i_12).also { i_81 = it } > i_9) {
                i_81 = i_9
            }

            i_5_var = i_5_var shl 1

            i_81 = i_81 shl 1; while (i_5_var < i_81) { i_4_var += i_12;
                i_11 = i_4_var shr 8
                val b_14 = bytes_2[i_11]
                i_01 = (b_14.toInt() shl 8) + (i_4_var and 0xff) * (bytes_2[i_11 + 1] - b_14)
                ints_3[i_5_var++] += i_01 * i_6 shr 6
                ints_3[i_5_var++] += i_01 * i_7 shr 6
            }

            if (i_12 == 0 || ((i_5_var shr 1) + (i_10 - i_4_var + i_12 - 1) / i_12).also { i_81 = it } > i_9) {
                i_81 = i_9
            }

            i_81 = i_81 shl 1

            i_11 = i_13; while (i_5_var < i_81) { i_4_var += i_12;
                val b_14 = bytes_2[i_4_var shr 8]
                i_01 = (b_14.toInt() shl 8) + (i_11 - b_14) * (i_4_var and 0xff)
                ints_3[i_5_var++] += i_01 * i_6 shr 6
                ints_3[i_5_var++] += i_01 * i_7 shr 6
            }

            class282Sub15Sub5Sub211.anInt9853 = i_4_var
            return i_5_var shr 1
        }

        @JvmStatic
        fun method16031(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_8: Int, i_9: Int, class282Sub15Sub5Sub210: MixedAudioStream): Int {
            var i_71 = 0
            var i_3_var = i_3 shr 8
            var i_4_var = i_4
            var i_5_var = i_5 shl 2
            var i_6_var = i_6 shl 2
            var i_9_var = i_9 shr 8
            if ((i_4_var + i_9 - i_3_var).also { i_71 = it } > i_8) {
                i_71 = i_8
            }

            i_4_var = i_4_var shl 1
            i_71 = i_71 shl 1

            i_71 -= 6
            while (i_4_var < i_71) {
                var b_11 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
                b_11 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
                b_11 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
                b_11 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
            }

            i_71 += 6
            while (i_4_var < i_71) {
                val b_11 = bytes_1[i_3_var++]
                ints_2[i_4_var++] += b_11 * i_5_var
                ints_2[i_4_var++] += b_11 * i_6_var
            }

            class282Sub15Sub5Sub210.anInt9853 = i_3_var shl 8
            return i_4_var shr 1
        }
    }
}
