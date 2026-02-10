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
        this.targetVolume = volume
        this.panPosition = i_4
        this.playbackPosition = 0
        recalculateChannelVolumes()
    }

    @Synchronized
    override fun fill(ints_1: IntArray, i_2: Int, i_3: Int) {
        if (super.targetVolume == 0 && super.fadeSamplesRemaining == 0) {
            skip(i_3)
        } else {
            val i_4 = super.start shl 8
            val i_5 = super.end shl 8
            val i_6 = ((buffer as MixedAudioBuffer).samples.size shl 8)
            val i_7 = i_5 - i_4
            if (i_7 <= 0) {
                super.loopsRemaining = 0
            }

            var i_8 = i_2
            val i_3_end = i_3 + i_2
            if (super.playbackPosition < 0) {
                if (super.sampleRate <= 0) {
                    cancelFade()
                    unlink()
                    return
                }

                super.playbackPosition = 0
            }

            if (super.playbackPosition >= i_6) {
                if (super.sampleRate >= 0) {
                    cancelFade()
                    unlink()
                    return
                }

                super.playbackPosition = i_6 - 1
            }

            if (super.loopsRemaining < 0) {
                if (super.enableLoop) {
                    if (super.sampleRate < 0) {
                        i_8 = mixSamplesBackward(ints_1, i_2, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.playbackPosition >= i_4) {
                            return
                        }

                        super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }

                    while (true) {
                        i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3_end, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                        if (super.playbackPosition < i_5) {
                            break
                        }

                        super.playbackPosition = i_5 + i_5 - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                        i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.playbackPosition >= i_4) {
                            break
                        }

                        super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }
                } else if (super.sampleRate < 0) {
                    while (true) {
                        i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                        if (super.playbackPosition >= i_4) {
                            break
                        }

                        super.playbackPosition = i_5 - 1 - (i_5 - 1 - super.playbackPosition) % i_7
                    }
                } else {
                    while (true) {
                        i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.playbackPosition < i_5) {
                            break
                        }

                        super.playbackPosition = i_4 + (super.playbackPosition - i_4) % i_7
                    }
                }
            } else {
                if (super.loopsRemaining > 0) {
                    if (super.enableLoop) {
                        run loop@ {
                            if (super.sampleRate < 0) {
                                i_8 = mixSamplesBackward(ints_1, i_2, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.playbackPosition >= i_4) {
                                    return@fill
                                }

                                super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.loopsRemaining == 0) {
                                    return@loop
                                }
                            }

                            do {
                                i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3_end, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                                if (super.playbackPosition < i_5) {
                                    return@fill
                                }

                                super.playbackPosition = i_5 + i_5 - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.loopsRemaining == 0) {
                                    break
                                }

                                i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.playbackPosition >= i_4) {
                                    return@fill
                                }

                                super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            } while (--super.loopsRemaining != 0)
                        }
                    } else {
                        var i_9: Int
                        if (super.sampleRate < 0) {
                            while (true) {
                                i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3_end, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                                if (super.playbackPosition >= i_4) {
                                    return
                                }

                                i_9 = (i_5 - 1 - super.playbackPosition) / i_7
                                if (i_9 >= super.loopsRemaining) {
                                    super.playbackPosition += i_7 * super.loopsRemaining
                                    super.loopsRemaining = 0
                                    break
                                }

                                super.playbackPosition += i_7 * i_9
                                super.loopsRemaining -= i_9
                            }
                        } else {
                            while (true) {
                                i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3_end, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.playbackPosition < i_5) {
                                    return
                                }

                                i_9 = (super.playbackPosition - i_4) / i_7
                                if (i_9 >= super.loopsRemaining) {
                                    super.playbackPosition -= i_7 * super.loopsRemaining
                                    super.loopsRemaining = 0
                                    break
                                }

                                super.playbackPosition -= i_7 * i_9
                                super.loopsRemaining -= i_9
                            }
                        }
                    }
                }

                if (super.sampleRate < 0) {
                    mixSamplesBackward(ints_1, i_8, 0, i_3_end, 0)
                    if (super.playbackPosition < 0) {
                        super.playbackPosition = -1
                        cancelFade()
                        unlink()
                    }
                } else {
                    mixSamplesForward(ints_1, i_8, i_6, i_3_end, 0)
                    if (super.playbackPosition >= i_6) {
                        super.playbackPosition = i_6
                        cancelFade()
                        unlink()
                    }
                }
            }
        }

    }

    fun mixSamplesForward(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Int {
        var i_2_var = i_2
        do {
            if (super.fadeSamplesRemaining <= 0) {
                if (super.sampleRate == 256 && (super.playbackPosition and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return mixForwardStereoConstant((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.leftChannelVolume, super.rightChannelVolume, i_4, i_3, this)
                    }

                    return mixForwardMonoConstant((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.currentVolume, i_4, i_3, this)
                }

                if (PcmPlayerManager.stereo) {
                    return mixForwardStereoInterpolated((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.leftChannelVolume, super.rightChannelVolume, i_4, i_3, this, super.sampleRate, i_5)
                }

                return mixForwardMonoInterpolated((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.currentVolume, i_4, i_3, this, super.sampleRate, i_5)
            }

            var i_6 = i_2_var + super.fadeSamplesRemaining
            if (i_6 > i_4) {
                i_6 = i_4
            }

            super.fadeSamplesRemaining += i_2_var
            if (super.sampleRate == 256 && (super.playbackPosition and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    i_2_var = mixForwardStereoConstantFading((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, i_6, i_3, this)
                } else {
                    i_2_var = mixForwardMonoConstantFading((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.currentVolume, super.volumeFadeDelta, i_6, i_3, this)
                }
            } else if (PcmPlayerManager.stereo) {
                i_2_var = mixForwardStereoInterpolatedFading((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, i_6, i_3, this, super.sampleRate, i_5)
            } else {
                i_2_var = mixForwardMonoInterpolatedFading((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.currentVolume, super.volumeFadeDelta, i_6, i_3, this, super.sampleRate, i_5)
            }

            super.fadeSamplesRemaining -= i_2_var
            if (super.fadeSamplesRemaining != 0) {
                return i_2_var
            }
        } while (!updateFadeState())

        return i_4
    }

    fun mixSamplesBackward(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Int {
        var i_2_var = i_2
        do {
            if (super.fadeSamplesRemaining <= 0) {
                if (super.sampleRate == -256 && (super.playbackPosition and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return mixBackwardStereoConstant((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.leftChannelVolume, super.rightChannelVolume, i_4, i_3, this)
                    }

                    return mixBackwardMonoConstant((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.currentVolume, i_4, i_3, this)
                }

                if (PcmPlayerManager.stereo) {
                    return mixBackwardStereoInterpolated((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.leftChannelVolume, super.rightChannelVolume, i_4, i_3, this, super.sampleRate, i_5)
                }

                return mixBackwardMonoInterpolated((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.currentVolume, i_4, i_3, this, super.sampleRate, i_5)
            }

            var i_6 = i_2_var + super.fadeSamplesRemaining
            if (i_6 > i_4) {
                i_6 = i_4
            }

            super.fadeSamplesRemaining += i_2_var
            if (super.sampleRate == -256 && (super.playbackPosition and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    i_2_var = mixBackwardStereoConstantFading((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, i_6, i_3, this)
                } else {
                    i_2_var = mixBackwardMonoConstantFading((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.currentVolume, super.volumeFadeDelta, i_6, i_3, this)
                }
            } else if (PcmPlayerManager.stereo) {
                i_2_var = mixBackwardStereoInterpolatedFading((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, i_6, i_3, this, super.sampleRate, i_5)
            } else {
                i_2_var = mixBackwardMonoInterpolatedFading((buffer as MixedAudioBuffer).samples, ints_1, super.playbackPosition, i_2_var, super.currentVolume, super.volumeFadeDelta, i_6, i_3, this, super.sampleRate, i_5)
            }

            super.fadeSamplesRemaining -= i_2_var
            if (super.fadeSamplesRemaining != 0) {
                return i_2_var
            }
        } while (!updateFadeState())

        return i_4
    }

    companion object {
        @JvmStatic
        fun mixBackwardMonoConstant(bytes_0: ByteArray, ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_6: Int, i_7: Int, class282Sub15Sub5Sub28: MixedAudioStream): Int {
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

            class282Sub15Sub5Sub28.playbackPosition = i_2_var shl 8
            return i_3_var
        }

        @JvmStatic
        fun mixForwardMonoConstant(bytes_0: ByteArray, ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_6: Int, i_7: Int, class282Sub15Sub5Sub28: MixedAudioStream): Int {
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

            class282Sub15Sub5Sub28.playbackPosition = i_2_var shl 8
            return i_3_var
        }

        @JvmStatic
        fun mixBackwardStereoConstant(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_8: Int, i_9: Int, class282Sub15Sub5Sub210: MixedAudioStream): Int {
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

            class282Sub15Sub5Sub210.playbackPosition = i_3_var shl 8
            return i_4_var shr 1
        }

        @JvmStatic
        fun mixForwardMonoInterpolated(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_8: Int, i_9: Int, class282Sub15Sub5Sub210: MixedAudioStream, i_11: Int, i_12: Int): Int {
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

            class282Sub15Sub5Sub210.playbackPosition = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun mixBackwardMonoInterpolated(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_8: Int, i_9: Int, class282Sub15Sub5Sub210: MixedAudioStream, i_11: Int, i_12: Int): Int {
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

            class282Sub15Sub5Sub210.playbackPosition = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun mixBackwardStereoInterpolated(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_9: Int, i_10: Int, class282Sub15Sub5Sub211: MixedAudioStream, i_12: Int, i_13: Int): Int {
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

            class282Sub15Sub5Sub211.playbackPosition = i_4_var
            return i_5_var shr 1
        }

        @JvmStatic
        fun mixForwardMonoConstantFading(bytes_0: ByteArray, ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_7: Int, i_8: Int, class282Sub15Sub5Sub29: MixedAudioStream): Int {
            var i_61 = 0
            var i_2_var = i_2 shr 8
            var i_3_var = i_3
            var i_4_var = i_4 shl 2
            var i_5_var = i_5 shl 2
            var i_8_var = i_8 shr 8
            if ((i_3_var + i_8 - i_2_var).also { i_61 = it } > i_7) {
                i_61 = i_7
            }

            class282Sub15Sub5Sub29.leftChannelVolume += class282Sub15Sub5Sub29.leftVolumeFadeDelta * (i_61 - i_3_var)
            class282Sub15Sub5Sub29.rightChannelVolume += class282Sub15Sub5Sub29.rightVolumeFadeDelta * (i_61 - i_3_var)

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

            class282Sub15Sub5Sub29.currentVolume = i_4_var shr 2
            class282Sub15Sub5Sub29.playbackPosition = i_2_var shl 8
            return i_3_var
        }

        @JvmStatic
        fun mixForwardStereoConstantFading(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_10: Int, i_11: Int, class282Sub15Sub5Sub212: MixedAudioStream): Int {
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

            class282Sub15Sub5Sub212.currentVolume += class282Sub15Sub5Sub212.volumeFadeDelta * (i_91 - i_4_var)
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

            class282Sub15Sub5Sub212.leftChannelVolume = i_5_var shr 2
            class282Sub15Sub5Sub212.rightChannelVolume = i_6_var shr 2
            class282Sub15Sub5Sub212.playbackPosition = i_3_var shl 8
            return i_4_var shr 1
        }

        @JvmStatic
        fun mixBackwardMonoConstantFading(bytes_0: ByteArray, ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_7: Int, i_8: Int, class282Sub15Sub5Sub29: MixedAudioStream): Int {
            var i_61 = 0
            var i_2_var = i_2 shr 8
            var i_3_var = i_3
            var i_4_var = i_4 shl 2
            var i_5_var = i_5 shl 2
            var i_8_var = i_8 shr 8
            if ((i_3_var + i_2_var - (i_8 - 1)).also { i_61 = it } > i_7) {
                i_61 = i_7
            }

            class282Sub15Sub5Sub29.leftChannelVolume += class282Sub15Sub5Sub29.leftVolumeFadeDelta * (i_61 - i_3_var)
            class282Sub15Sub5Sub29.rightChannelVolume += class282Sub15Sub5Sub29.rightVolumeFadeDelta * (i_61 - i_3_var)

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

            class282Sub15Sub5Sub29.currentVolume = i_4_var shr 2
            class282Sub15Sub5Sub29.playbackPosition = i_2_var shl 8
            return i_3_var
        }

        @JvmStatic
        fun mixBackwardStereoConstantFading(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_10: Int, i_11: Int, class282Sub15Sub5Sub212: MixedAudioStream): Int {
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

            class282Sub15Sub5Sub212.currentVolume += class282Sub15Sub5Sub212.volumeFadeDelta * (i_91 - i_4_var)
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

            class282Sub15Sub5Sub212.leftChannelVolume = i_5_var shr 2
            class282Sub15Sub5Sub212.rightChannelVolume = i_6_var shr 2
            class282Sub15Sub5Sub212.playbackPosition = i_3_var shl 8
            return i_4_var shr 1
        }

        @JvmStatic
        fun mixForwardMonoInterpolatedFading(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_9: Int, i_10: Int, class282Sub15Sub5Sub211: MixedAudioStream, i_12: Int, i_13: Int): Int {
            var i_81 = 0
            var i_11 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            var i_6_var = i_6
            class282Sub15Sub5Sub211.leftChannelVolume -= class282Sub15Sub5Sub211.leftVolumeFadeDelta * i_5_var
            class282Sub15Sub5Sub211.rightChannelVolume -= class282Sub15Sub5Sub211.rightVolumeFadeDelta * i_5_var
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

            class282Sub15Sub5Sub211.leftChannelVolume += class282Sub15Sub5Sub211.leftVolumeFadeDelta * i_5_var
            class282Sub15Sub5Sub211.rightChannelVolume += class282Sub15Sub5Sub211.rightVolumeFadeDelta * i_5_var
            class282Sub15Sub5Sub211.currentVolume = i_6_var
            class282Sub15Sub5Sub211.playbackPosition = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun mixForwardStereoInterpolatedFading(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, i_11: Int, i_12: Int, class282Sub15Sub5Sub213: MixedAudioStream, i_14: Int, i_15: Int): Int {
            var i_101 = 0
            var i_13 = 0
            var i_01 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            var i_6_var = i_6
            var i_7_var = i_7
            class282Sub15Sub5Sub213.currentVolume -= i_5_var * class282Sub15Sub5Sub213.volumeFadeDelta
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
            class282Sub15Sub5Sub213.currentVolume += class282Sub15Sub5Sub213.volumeFadeDelta * i_5_var
            class282Sub15Sub5Sub213.leftChannelVolume = i_6_var
            class282Sub15Sub5Sub213.rightChannelVolume = i_7_var
            class282Sub15Sub5Sub213.playbackPosition = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun mixBackwardMonoInterpolatedFading(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_9: Int, i_10: Int, class282Sub15Sub5Sub211: MixedAudioStream, i_12: Int, i_13: Int): Int {
            var i_81 = 0
            var i_11 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            var i_6_var = i_6
            class282Sub15Sub5Sub211.leftChannelVolume -= class282Sub15Sub5Sub211.leftVolumeFadeDelta * i_5_var
            class282Sub15Sub5Sub211.rightChannelVolume -= class282Sub15Sub5Sub211.rightVolumeFadeDelta * i_5_var
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

            class282Sub15Sub5Sub211.leftChannelVolume += class282Sub15Sub5Sub211.leftVolumeFadeDelta * i_5_var
            class282Sub15Sub5Sub211.rightChannelVolume += class282Sub15Sub5Sub211.rightVolumeFadeDelta * i_5_var
            class282Sub15Sub5Sub211.currentVolume = i_6_var
            class282Sub15Sub5Sub211.playbackPosition = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun mixBackwardStereoInterpolatedFading(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, i_11: Int, i_12: Int, class282Sub15Sub5Sub213: MixedAudioStream, i_14: Int, i_15: Int): Int {
            var i_101 = 0
            var i_13 = 0
            var i_01 = 0
            var i_4_var = i_4
            var i_5_var = i_5
            var i_6_var = i_6
            var i_7_var = i_7
            class282Sub15Sub5Sub213.currentVolume -= i_5_var * class282Sub15Sub5Sub213.volumeFadeDelta
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
            class282Sub15Sub5Sub213.currentVolume += class282Sub15Sub5Sub213.volumeFadeDelta * i_5_var
            class282Sub15Sub5Sub213.leftChannelVolume = i_6_var
            class282Sub15Sub5Sub213.rightChannelVolume = i_7_var
            class282Sub15Sub5Sub213.playbackPosition = i_4_var
            return i_5_var
        }

        @JvmStatic
        fun mixForwardStereoInterpolated(bytes_2: ByteArray, ints_3: IntArray, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_9: Int, i_10: Int, class282Sub15Sub5Sub211: MixedAudioStream, i_12: Int, i_13: Int): Int {
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

            class282Sub15Sub5Sub211.playbackPosition = i_4_var
            return i_5_var shr 1
        }

        @JvmStatic
        fun mixForwardStereoConstant(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_8: Int, i_9: Int, class282Sub15Sub5Sub210: MixedAudioStream): Int {
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

            class282Sub15Sub5Sub210.playbackPosition = i_3_var shl 8
            return i_4_var shr 1
        }
    }
}
