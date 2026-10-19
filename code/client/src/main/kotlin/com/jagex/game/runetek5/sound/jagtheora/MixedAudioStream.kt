package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.config.midi.MixedAudioBuffer
import com.jagex.game.runetek5.sound.player.PcmPlayerManager

open class MixedAudioStream(buffer: MixedAudioBuffer, sampleRate: Int, volume: Int, pan: Int) : AreaSoundStream() {

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
        this.panPosition = pan
        this.playbackPosition = 0
        recalculateChannelVolumes()
    }

    @Synchronized
    override fun fill(mixBuffer: IntArray, offset: Int, length: Int) {
        if (super.targetVolume == 0 && super.fadeSamplesRemaining == 0) {
            skip(length)
        } else {
            val startPos = super.start shl 8
            val endPos = super.end shl 8
            val totalSamples = ((buffer as MixedAudioBuffer).samples.size shl 8)
            val loopLength = endPos - startPos
            if (loopLength <= 0) {
                super.loopsRemaining = 0
            }

            var currentOffset = offset
            val endOffset = length + offset
            if (super.playbackPosition < 0) {
                if (super.sampleRate <= 0) {
                    cancelFade()
                    unlink()
                    return
                }

                super.playbackPosition = 0
            }

            if (super.playbackPosition >= totalSamples) {
                if (super.sampleRate >= 0) {
                    cancelFade()
                    unlink()
                    return
                }

                super.playbackPosition = totalSamples - 1
            }

            if (super.loopsRemaining < 0) {
                if (super.enableLoop) {
                    if (super.sampleRate < 0) {
                        currentOffset = mixSamplesBackward(mixBuffer, offset, startPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.playbackPosition >= startPos) {
                            return
                        }

                        super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }

                    while (true) {
                        currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                        if (super.playbackPosition < endPos) {
                            break
                        }

                        super.playbackPosition = endPos + endPos - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                        currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.playbackPosition >= startPos) {
                            break
                        }

                        super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }
                } else if (super.sampleRate < 0) {
                    while (true) {
                        currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                        if (super.playbackPosition >= startPos) {
                            break
                        }

                        super.playbackPosition = endPos - 1 - (endPos - 1 - super.playbackPosition) % loopLength
                    }
                } else {
                    while (true) {
                        currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                        if (super.playbackPosition < endPos) {
                            break
                        }

                        super.playbackPosition = startPos + (super.playbackPosition - startPos) % loopLength
                    }
                }
            } else {
                if (super.loopsRemaining > 0) {
                    if (super.enableLoop) {
                        run loop@ {
                            if (super.sampleRate < 0) {
                                currentOffset = mixSamplesBackward(mixBuffer, offset, startPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.playbackPosition >= startPos) {
                                    return@fill
                                }

                                super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.loopsRemaining == 0) {
                                    return@loop
                                }
                            }

                            do {
                                currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                                if (super.playbackPosition < endPos) {
                                    return@fill
                                }

                                super.playbackPosition = endPos + endPos - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.loopsRemaining == 0) {
                                    break
                                }

                                currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.playbackPosition >= startPos) {
                                    return@fill
                                }

                                super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            } while (--super.loopsRemaining != 0)
                        }
                    } else {
                        var loopSkips: Int
                        if (super.sampleRate < 0) {
                            while (true) {
                                currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.end - 1]).toInt())
                                if (super.playbackPosition >= startPos) {
                                    return
                                }

                                loopSkips = (endPos - 1 - super.playbackPosition) / loopLength
                                if (loopSkips >= super.loopsRemaining) {
                                    super.playbackPosition += loopLength * super.loopsRemaining
                                    super.loopsRemaining = 0
                                    break
                                }

                                super.playbackPosition += loopLength * loopSkips
                                super.loopsRemaining -= loopSkips
                            }
                        } else {
                            while (true) {
                                currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, ((buffer as MixedAudioBuffer).samples[super.start]).toInt())
                                if (super.playbackPosition < endPos) {
                                    return
                                }

                                loopSkips = (super.playbackPosition - startPos) / loopLength
                                if (loopSkips >= super.loopsRemaining) {
                                    super.playbackPosition -= loopLength * super.loopsRemaining
                                    super.loopsRemaining = 0
                                    break
                                }

                                super.playbackPosition -= loopLength * loopSkips
                                super.loopsRemaining -= loopSkips
                            }
                        }
                    }
                }

                if (super.sampleRate < 0) {
                    mixSamplesBackward(mixBuffer, currentOffset, 0, endOffset, 0)
                    if (super.playbackPosition < 0) {
                        super.playbackPosition = -1
                        cancelFade()
                        unlink()
                    }
                } else {
                    mixSamplesForward(mixBuffer, currentOffset, totalSamples, endOffset, 0)
                    if (super.playbackPosition >= totalSamples) {
                        super.playbackPosition = totalSamples
                        cancelFade()
                        unlink()
                    }
                }
            }
        }

    }

    fun mixSamplesForward(mixBuffer: IntArray, outputOffset: Int, endPosition: Int, maxOutputOffset: Int, wrapSample: Int): Int {
        var currentOffset = outputOffset
        do {
            if (super.fadeSamplesRemaining <= 0) {
                if (super.sampleRate == 256 && (super.playbackPosition and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return mixForwardStereoConstant((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, maxOutputOffset, endPosition, this)
                    }

                    return mixForwardMonoConstant((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, maxOutputOffset, endPosition, this)
                }

                if (PcmPlayerManager.stereo) {
                    return mixForwardStereoInterpolated((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, maxOutputOffset, endPosition, this, super.sampleRate, wrapSample)
                }

                return mixForwardMonoInterpolated((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, maxOutputOffset, endPosition, this, super.sampleRate, wrapSample)
            }

            var fadeEnd = currentOffset + super.fadeSamplesRemaining
            if (fadeEnd > maxOutputOffset) {
                fadeEnd = maxOutputOffset
            }

            super.fadeSamplesRemaining += currentOffset
            if (super.sampleRate == 256 && (super.playbackPosition and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    currentOffset = mixForwardStereoConstantFading((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, fadeEnd, endPosition, this)
                } else {
                    currentOffset = mixForwardMonoConstantFading((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, super.volumeFadeDelta, fadeEnd, endPosition, this)
                }
            } else if (PcmPlayerManager.stereo) {
                currentOffset = mixForwardStereoInterpolatedFading((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, fadeEnd, endPosition, this, super.sampleRate, wrapSample)
            } else {
                currentOffset = mixForwardMonoInterpolatedFading((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, super.volumeFadeDelta, fadeEnd, endPosition, this, super.sampleRate, wrapSample)
            }

            super.fadeSamplesRemaining -= currentOffset
            if (super.fadeSamplesRemaining != 0) {
                return currentOffset
            }
        } while (!updateFadeState())

        return maxOutputOffset
    }

    fun mixSamplesBackward(mixBuffer: IntArray, outputOffset: Int, endPosition: Int, maxOutputOffset: Int, wrapSample: Int): Int {
        var currentOffset = outputOffset
        do {
            if (super.fadeSamplesRemaining <= 0) {
                if (super.sampleRate == -256 && (super.playbackPosition and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return mixBackwardStereoConstant((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, maxOutputOffset, endPosition, this)
                    }

                    return mixBackwardMonoConstant((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, maxOutputOffset, endPosition, this)
                }

                if (PcmPlayerManager.stereo) {
                    return mixBackwardStereoInterpolated((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, maxOutputOffset, endPosition, this, super.sampleRate, wrapSample)
                }

                return mixBackwardMonoInterpolated((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, maxOutputOffset, endPosition, this, super.sampleRate, wrapSample)
            }

            var fadeEnd = currentOffset + super.fadeSamplesRemaining
            if (fadeEnd > maxOutputOffset) {
                fadeEnd = maxOutputOffset
            }

            super.fadeSamplesRemaining += currentOffset
            if (super.sampleRate == -256 && (super.playbackPosition and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    currentOffset = mixBackwardStereoConstantFading((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, fadeEnd, endPosition, this)
                } else {
                    currentOffset = mixBackwardMonoConstantFading((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, super.volumeFadeDelta, fadeEnd, endPosition, this)
                }
            } else if (PcmPlayerManager.stereo) {
                currentOffset = mixBackwardStereoInterpolatedFading((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, fadeEnd, endPosition, this, super.sampleRate, wrapSample)
            } else {
                currentOffset = mixBackwardMonoInterpolatedFading((buffer as MixedAudioBuffer).samples, mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, super.volumeFadeDelta, fadeEnd, endPosition, this, super.sampleRate, wrapSample)
            }

            super.fadeSamplesRemaining -= currentOffset
            if (super.fadeSamplesRemaining != 0) {
                return currentOffset
            }
        } while (!updateFadeState())

        return maxOutputOffset
    }

    companion object {
        @JvmStatic
        fun mixBackwardMonoConstant(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, volume: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream): Int {
            var samplePos = position shr 8
            var outVar = outputIndex
            val scaledVolume = volume shl 2
            val endPos = endPosition shr 8
            var sampleLimit = outVar + samplePos - (endPos - 1)
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit -= 3
            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
            }

            sampleLimit += 3
            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
            }

            stream.playbackPosition = samplePos shl 8
            return outVar
        }

        @JvmStatic
        fun mixForwardMonoConstant(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, volume: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream): Int {
            var samplePos = position shr 8
            var outVar = outputIndex
            val scaledVolume = volume shl 2
            val endPos = endPosition shr 8
            var sampleLimit = outVar + endPos - samplePos
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit -= 3
            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
            }

            sampleLimit += 3
            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
            }

            stream.playbackPosition = samplePos shl 8
            return outVar
        }

        @JvmStatic
        fun mixBackwardStereoConstant(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, leftVolume: Int, rightVolume: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream): Int {
            var samplePos = position shr 8
            val leftVol = leftVolume shl 2
            val rightVol = rightVolume shl 2
            val endPos = endPosition shr 8
            var sampleLimit = outputIndex + samplePos - (endPos - 1)
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var outVar = outputIndex shl 1
            sampleLimit = (sampleLimit shl 1) - 6
            while (outVar < sampleLimit) {
                var sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
                sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
                sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
                sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
            }

            sampleLimit += 6
            while (outVar < sampleLimit) {
                val sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
            }

            stream.playbackPosition = samplePos shl 8
            return outVar shr 1
        }

        @JvmStatic
        fun mixForwardStereoConstant(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, leftVolume: Int, rightVolume: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream): Int {
            var samplePos = position shr 8
            val leftVol = leftVolume shl 2
            val rightVol = rightVolume shl 2
            val endPos = endPosition shr 8
            var sampleLimit = outputIndex + endPos - samplePos
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var outVar = outputIndex shl 1
            sampleLimit = (sampleLimit shl 1) - 6
            while (outVar < sampleLimit) {
                var sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
                sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
                sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
                sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
            }

            sampleLimit += 6
            while (outVar < sampleLimit) {
                val sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                mixBuffer[outVar++] += sample * rightVol
            }

            stream.playbackPosition = samplePos shl 8
            return outVar shr 1
        }

        @JvmStatic
        fun mixForwardMonoInterpolated(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, volume: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream, step: Int, wrapSample: Int): Int {
            var posVar = position
            var outVar = outputIndex
            var sampleLimit = 0
            if (step == 0 || (outVar + (endPosition - posVar + step - 257) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                val sampleIndex = posVar shr 8
                val base = sampleData[sampleIndex].toInt()
                mixBuffer[outVar++] += ((base shl 8) + (sampleData[sampleIndex + 1] - base) * (posVar and 0xff)) * volume shr 6
                posVar += step
            }

            if (step == 0 || (outVar + (endPosition - posVar + step - 1) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                val base = sampleData[posVar shr 8].toInt()
                mixBuffer[outVar++] += ((base shl 8) + (wrapSample - base) * (posVar and 0xff)) * volume shr 6
                posVar += step
            }

            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixBackwardMonoInterpolated(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, volume: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream, step: Int, wrapSample: Int): Int {
            var posVar = position
            var outVar = outputIndex
            var sampleLimit = 0
            if (step == 0 || (outVar + (endPosition + 256 - posVar + step) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                val sampleIndex = posVar shr 8
                val base = sampleData[sampleIndex - 1].toInt()
                mixBuffer[outVar++] += ((base shl 8) + (sampleData[sampleIndex] - base) * (posVar and 0xff)) * volume shr 6
                posVar += step
            }

            if (step == 0 || (outVar + (endPosition - posVar + step) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += ((wrapSample shl 8) + (sampleData[posVar shr 8] - wrapSample) * (posVar and 0xff)) * volume shr 6
                posVar += step
            }

            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixForwardStereoInterpolated(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, leftVolume: Int, rightVolume: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream, step: Int, wrapSample: Int): Int {
            var posVar = position
            var sampleLimit = 0
            if (step == 0 || (outputIndex + (endPosition - posVar + step - 257) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var outVar = outputIndex shl 1
            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                val sampleIndex = posVar shr 8
                val base = sampleData[sampleIndex].toInt()
                val interpolated = (base shl 8) + (sampleData[sampleIndex + 1] - base) * (posVar and 0xff)
                mixBuffer[outVar++] += interpolated * leftVolume shr 6
                mixBuffer[outVar++] += interpolated * rightVolume shr 6
                posVar += step
            }

            if (step == 0 || ((outVar shr 1) + (endPosition - posVar + step - 1) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                val base = sampleData[posVar shr 8].toInt()
                val interpolated = (base shl 8) + (wrapSample - base) * (posVar and 0xff)
                mixBuffer[outVar++] += interpolated * leftVolume shr 6
                mixBuffer[outVar++] += interpolated * rightVolume shr 6
                posVar += step
            }

            stream.playbackPosition = posVar
            return outVar shr 1
        }

        @JvmStatic
        fun mixBackwardStereoInterpolated(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, leftVolume: Int, rightVolume: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream, step: Int, wrapSample: Int): Int {
            var posVar = position
            var sampleLimit = 0
            if (step == 0 || (outputIndex + (endPosition + 256 - posVar + step) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var outVar = outputIndex shl 1
            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                val sampleIndex = posVar shr 8
                val base = sampleData[sampleIndex - 1].toInt()
                val interpolated = (base shl 8) + (sampleData[sampleIndex] - base) * (posVar and 0xff)
                mixBuffer[outVar++] += interpolated * leftVolume shr 6
                mixBuffer[outVar++] += interpolated * rightVolume shr 6
                posVar += step
            }

            if (step == 0 || ((outVar shr 1) + (endPosition - posVar + step) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                val interpolated = (wrapSample shl 8) + (sampleData[posVar shr 8] - wrapSample) * (posVar and 0xff)
                mixBuffer[outVar++] += interpolated * leftVolume shr 6
                mixBuffer[outVar++] += interpolated * rightVolume shr 6
                posVar += step
            }

            stream.playbackPosition = posVar
            return outVar shr 1
        }

        @JvmStatic
        fun mixForwardMonoConstantFading(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, volume: Int, volumeDelta: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream): Int {
            var samplePos = position shr 8
            var outVar = outputIndex
            var scaledVolume = volume shl 2
            val scaledDelta = volumeDelta shl 2
            val endPos = endPosition shr 8
            var sampleLimit = outVar + endPos - samplePos
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            stream.leftChannelVolume += stream.leftVolumeFadeDelta * (sampleLimit - outVar)
            stream.rightChannelVolume += stream.rightVolumeFadeDelta * (sampleLimit - outVar)
            sampleLimit -= 3
            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
                scaledVolume += scaledDelta
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
                scaledVolume += scaledDelta
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
                scaledVolume += scaledDelta
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
                scaledVolume += scaledDelta
            }

            sampleLimit += 3
            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += sampleData[samplePos++] * scaledVolume
                scaledVolume += scaledDelta
            }

            stream.currentVolume = scaledVolume shr 2
            stream.playbackPosition = samplePos shl 8
            return outVar
        }

        @JvmStatic
        fun mixBackwardMonoConstantFading(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, volume: Int, volumeDelta: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream): Int {
            var samplePos = position shr 8
            var outVar = outputIndex
            var scaledVolume = volume shl 2
            val scaledDelta = volumeDelta shl 2
            val endPos = endPosition shr 8
            var sampleLimit = outVar + samplePos - (endPos - 1)
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            stream.leftChannelVolume += stream.leftVolumeFadeDelta * (sampleLimit - outVar)
            stream.rightChannelVolume += stream.rightVolumeFadeDelta * (sampleLimit - outVar)
            sampleLimit -= 3
            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
                scaledVolume += scaledDelta
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
                scaledVolume += scaledDelta
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
                scaledVolume += scaledDelta
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
                scaledVolume += scaledDelta
            }

            sampleLimit += 3
            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += sampleData[samplePos--] * scaledVolume
                scaledVolume += scaledDelta
            }

            stream.currentVolume = scaledVolume shr 2
            stream.playbackPosition = samplePos shl 8
            return outVar
        }

        @JvmStatic
        fun mixForwardStereoConstantFading(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, leftVolume: Int, rightVolume: Int, leftVolumeDelta: Int, rightVolumeDelta: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream): Int {
            var samplePos = position shr 8
            var leftVol = leftVolume shl 2
            var rightVol = rightVolume shl 2
            val leftDelta = leftVolumeDelta shl 2
            val rightDelta = rightVolumeDelta shl 2
            val endPos = endPosition shr 8
            var sampleLimit = outputIndex + endPos - samplePos
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            stream.currentVolume += stream.volumeFadeDelta * (sampleLimit - outputIndex)
            var outVar = outputIndex shl 1
            sampleLimit = (sampleLimit shl 1) - 6
            while (outVar < sampleLimit) {
                var sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
                sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
                sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
                sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
            }

            sampleLimit += 6
            while (outVar < sampleLimit) {
                val sample = sampleData[samplePos++].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
            }

            stream.leftChannelVolume = leftVol shr 2
            stream.rightChannelVolume = rightVol shr 2
            stream.playbackPosition = samplePos shl 8
            return outVar shr 1
        }

        @JvmStatic
        fun mixBackwardStereoConstantFading(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, leftVolume: Int, rightVolume: Int, leftVolumeDelta: Int, rightVolumeDelta: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream): Int {
            var samplePos = position shr 8
            var leftVol = leftVolume shl 2
            var rightVol = rightVolume shl 2
            val leftDelta = leftVolumeDelta shl 2
            val rightDelta = rightVolumeDelta shl 2
            val endPos = endPosition shr 8
            var sampleLimit = outputIndex + samplePos - (endPos - 1)
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            stream.currentVolume += stream.volumeFadeDelta * (sampleLimit - outputIndex)
            var outVar = outputIndex shl 1
            sampleLimit = (sampleLimit shl 1) - 6
            while (outVar < sampleLimit) {
                var sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
                sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
                sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
                sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
            }

            sampleLimit += 6
            while (outVar < sampleLimit) {
                val sample = sampleData[samplePos--].toInt()
                mixBuffer[outVar++] += sample * leftVol
                leftVol += leftDelta
                mixBuffer[outVar++] += sample * rightVol
                rightVol += rightDelta
            }

            stream.leftChannelVolume = leftVol shr 2
            stream.rightChannelVolume = rightVol shr 2
            stream.playbackPosition = samplePos shl 8
            return outVar shr 1
        }

        @JvmStatic
        fun mixForwardMonoInterpolatedFading(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, volume: Int, volumeDelta: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream, step: Int, wrapSample: Int): Int {
            var posVar = position
            var outVar = outputIndex
            var currentVolume = volume
            stream.leftChannelVolume -= stream.leftVolumeFadeDelta * outVar
            stream.rightChannelVolume -= stream.rightVolumeFadeDelta * outVar
            var sampleLimit = 0
            if (step == 0 || (outVar + (endPosition - posVar + step - 257) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                val sampleIndex = posVar shr 8
                val base = sampleData[sampleIndex].toInt()
                mixBuffer[outVar++] += ((base shl 8) + (sampleData[sampleIndex + 1] - base) * (posVar and 0xff)) * currentVolume shr 6
                currentVolume += volumeDelta
                posVar += step
            }

            if (step == 0 || (outVar + (endPosition - posVar + step - 1) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                val base = sampleData[posVar shr 8].toInt()
                mixBuffer[outVar++] += ((base shl 8) + (wrapSample - base) * (posVar and 0xff)) * currentVolume shr 6
                currentVolume += volumeDelta
                posVar += step
            }

            stream.leftChannelVolume += stream.leftVolumeFadeDelta * outVar
            stream.rightChannelVolume += stream.rightVolumeFadeDelta * outVar
            stream.currentVolume = currentVolume
            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixBackwardMonoInterpolatedFading(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, volume: Int, volumeDelta: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream, step: Int, wrapSample: Int): Int {
            var posVar = position
            var outVar = outputIndex
            var currentVolume = volume
            stream.leftChannelVolume -= stream.leftVolumeFadeDelta * outVar
            stream.rightChannelVolume -= stream.rightVolumeFadeDelta * outVar
            var sampleLimit = 0
            if (step == 0 || (outVar + (endPosition + 256 - posVar + step) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                val sampleIndex = posVar shr 8
                val base = sampleData[sampleIndex - 1].toInt()
                mixBuffer[outVar++] += ((base shl 8) + (sampleData[sampleIndex] - base) * (posVar and 0xff)) * currentVolume shr 6
                currentVolume += volumeDelta
                posVar += step
            }

            if (step == 0 || (outVar + (endPosition - posVar + step) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                mixBuffer[outVar++] += ((wrapSample shl 8) + (sampleData[posVar shr 8] - wrapSample) * (posVar and 0xff)) * currentVolume shr 6
                currentVolume += volumeDelta
                posVar += step
            }

            stream.leftChannelVolume += stream.leftVolumeFadeDelta * outVar
            stream.rightChannelVolume += stream.rightVolumeFadeDelta * outVar
            stream.currentVolume = currentVolume
            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixForwardStereoInterpolatedFading(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, leftVolume: Int, rightVolume: Int, leftVolumeDelta: Int, rightVolumeDelta: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream, step: Int, wrapSample: Int): Int {
            var posVar = position
            var leftVol = leftVolume
            var rightVol = rightVolume
            stream.currentVolume -= stream.volumeFadeDelta * outputIndex
            var sampleLimit = 0
            if (step == 0 || (outputIndex + (endPosition - posVar + step - 257) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var outVar = outputIndex shl 1
            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                val sampleIndex = posVar shr 8
                val base = sampleData[sampleIndex].toInt()
                val interpolated = (base shl 8) + (sampleData[sampleIndex + 1] - base) * (posVar and 0xff)
                mixBuffer[outVar++] += interpolated * leftVol shr 6
                leftVol += leftVolumeDelta
                mixBuffer[outVar++] += interpolated * rightVol shr 6
                rightVol += rightVolumeDelta
                posVar += step
            }

            if (step == 0 || ((outVar shr 1) + (endPosition - posVar + step - 1) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                val base = sampleData[posVar shr 8].toInt()
                val interpolated = (base shl 8) + (wrapSample - base) * (posVar and 0xff)
                mixBuffer[outVar++] += interpolated * leftVol shr 6
                leftVol += leftVolumeDelta
                mixBuffer[outVar++] += interpolated * rightVol shr 6
                rightVol += rightVolumeDelta
                posVar += step
            }

            outVar = outVar shr 1
            stream.currentVolume += stream.volumeFadeDelta * outVar
            stream.leftChannelVolume = leftVol
            stream.rightChannelVolume = rightVol
            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixBackwardStereoInterpolatedFading(sampleData: ByteArray, mixBuffer: IntArray, position: Int, outputIndex: Int, leftVolume: Int, rightVolume: Int, leftVolumeDelta: Int, rightVolumeDelta: Int, maxOutputIndex: Int, endPosition: Int, stream: MixedAudioStream, step: Int, wrapSample: Int): Int {
            var posVar = position
            var leftVol = leftVolume
            var rightVol = rightVolume
            stream.currentVolume -= stream.volumeFadeDelta * outputIndex
            var sampleLimit = 0
            if (step == 0 || (outputIndex + (endPosition + 256 - posVar + step) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var outVar = outputIndex shl 1
            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                val sampleIndex = posVar shr 8
                val base = sampleData[sampleIndex - 1].toInt()
                val interpolated = (base shl 8) + (sampleData[sampleIndex] - base) * (posVar and 0xff)
                mixBuffer[outVar++] += interpolated * leftVol shr 6
                leftVol += leftVolumeDelta
                mixBuffer[outVar++] += interpolated * rightVol shr 6
                rightVol += rightVolumeDelta
                posVar += step
            }

            if (step == 0 || ((outVar shr 1) + (endPosition - posVar + step) / step).also { sampleLimit = it } > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                val interpolated = (wrapSample shl 8) + (sampleData[posVar shr 8] - wrapSample) * (posVar and 0xff)
                mixBuffer[outVar++] += interpolated * leftVol shr 6
                leftVol += leftVolumeDelta
                mixBuffer[outVar++] += interpolated * rightVol shr 6
                rightVol += rightVolumeDelta
                posVar += step
            }

            outVar = outVar shr 1
            stream.currentVolume += stream.volumeFadeDelta * outVar
            stream.leftChannelVolume = leftVol
            stream.rightChannelVolume = rightVol
            stream.playbackPosition = posVar
            return outVar
        }
    }
}
