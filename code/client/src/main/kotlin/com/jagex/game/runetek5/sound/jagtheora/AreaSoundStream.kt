package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.staticmethods.StaticMethods

abstract class AreaSoundStream : PcmStream() {

    @JvmField var start: Int = 0
    @JvmField var end: Int = 0
    @JvmField var enableLoop: Boolean = false
    @JvmField var sampleRate: Int = 0
    @JvmField var currentVolume: Int = 0
    @JvmField var leftChannelVolume: Int = 0
    @JvmField var rightChannelVolume: Int = 0
    @JvmField var fadeSamplesRemaining: Int = 0
    @JvmField var loopsRemaining: Int = 0
    @JvmField var targetVolume: Int = 0
    @JvmField var playbackPosition: Int = 0
    @JvmField var panPosition: Int = 0
    @JvmField var volumeFadeDelta: Int = 0
    @JvmField var leftVolumeFadeDelta: Int = 0
    @JvmField var rightVolumeFadeDelta: Int = 0

    @Synchronized
    fun setPlaybackRate(rate: Int) {
        if (this.sampleRate < 0) {
            this.sampleRate = -rate
        } else {
            this.sampleRate = rate
        }
    }

    @Synchronized
    fun isFading(): Boolean {
        return this.fadeSamplesRemaining != 0
    }

    @Synchronized
    @JvmName("setVolumeWithShift")
    fun setVolume(volume: Int) {
        setVolumeAndPan(volume shl 6, getPanPosition())
    }

    @Synchronized
    fun reversePlaybackDirection() {
        this.sampleRate = (this.sampleRate xor (this.sampleRate shr 31)) + (this.sampleRate ushr 31)
        this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
    }

    @Synchronized
    fun getTargetVolume(): Int {
        return if (this.targetVolume == Int.MIN_VALUE) 0 else this.targetVolume
    }

    @Synchronized
    fun getPanPosition(): Int {
        return if (this.panPosition < 0) -1 else this.panPosition
    }

    @Synchronized
    fun setPlaybackPosition(position: Int) {
        var pos = position
        val totalSamplesShifted = ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
        if (pos < -1) {
            pos = -1
        }

        if (pos > totalSamplesShifted) {
            pos = totalSamplesShifted
        }

        this.playbackPosition = pos
    }

    @Synchronized
    fun isFinished(): Boolean {
        return this.playbackPosition < 0 || this.playbackPosition >= ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
    }

    @Synchronized
    fun fadeToVolume(fadeDuration: Int, volume: Int) {
        fadeToVolumeAndPan(fadeDuration, volume, getPanPosition())
    }

    @Synchronized
    fun fadeToVolumeAndPan(fadeDuration: Int, volume: Int, pan: Int) {
        var duration = fadeDuration
        if (duration == 0) {
            setVolumeAndPan(volume, pan)
        } else {
            val targetLeft = StaticMethods.calculateSoundAttenuation(volume, pan)
            val targetRight = StaticMethods.calculateSoundPan(volume, pan)
            if (targetLeft == this.leftChannelVolume && targetRight == this.rightChannelVolume) {
                this.fadeSamplesRemaining = 0
            } else {
                var maxDelta = volume - this.currentVolume
                if (this.currentVolume - volume > maxDelta) {
                    maxDelta = this.currentVolume - volume
                }

                if (targetLeft - this.leftChannelVolume > maxDelta) {
                    maxDelta = targetLeft - this.leftChannelVolume
                }

                if (this.leftChannelVolume - targetLeft > maxDelta) {
                    maxDelta = this.leftChannelVolume - targetLeft
                }

                if (targetRight - this.rightChannelVolume > maxDelta) {
                    maxDelta = targetRight - this.rightChannelVolume
                }

                if (this.rightChannelVolume - targetRight > maxDelta) {
                    maxDelta = this.rightChannelVolume - targetRight
                }

                if (duration > maxDelta) {
                    duration = maxDelta
                }

                this.fadeSamplesRemaining = duration
                this.targetVolume = volume
                this.panPosition = pan
                this.volumeFadeDelta = (volume - this.currentVolume) / duration
                this.leftVolumeFadeDelta = (targetLeft - this.leftChannelVolume) / duration
                this.rightVolumeFadeDelta = (targetRight - this.rightChannelVolume) / duration
            }
        }
    }

    @Synchronized
    fun fadeOutAndStop(fadeDuration: Int) {
        var duration = fadeDuration
        if (duration == 0) {
            muteVolume()
            unlink()
        } else if (this.leftChannelVolume == 0 && this.rightChannelVolume == 0) {
            this.fadeSamplesRemaining = 0
            this.targetVolume = 0
            this.currentVolume = 0
            unlink()
        } else {
            var maxDelta = -this.currentVolume
            if (this.currentVolume > maxDelta) {
                maxDelta = this.currentVolume
            }

            if (-this.leftChannelVolume > maxDelta) {
                maxDelta = -this.leftChannelVolume
            }

            if (this.leftChannelVolume > maxDelta) {
                maxDelta = this.leftChannelVolume
            }

            if (-this.rightChannelVolume > maxDelta) {
                maxDelta = -this.rightChannelVolume
            }

            if (this.rightChannelVolume > maxDelta) {
                maxDelta = this.rightChannelVolume
            }

            if (duration > maxDelta) {
                duration = maxDelta
            }

            this.fadeSamplesRemaining = duration
            this.targetVolume = Int.MIN_VALUE
            this.volumeFadeDelta = -this.currentVolume / duration
            this.leftVolumeFadeDelta = -this.leftChannelVolume / duration
            this.rightVolumeFadeDelta = -this.rightChannelVolume / duration
        }
    }

    @Synchronized
    fun getAbsolutePlaybackRate(): Int {
        return if (this.sampleRate < 0) -this.sampleRate else this.sampleRate
    }

    override fun getActiveVoiceCount(): Int {
        return if (this.targetVolume == 0 && this.fadeSamplesRemaining == 0) 0 else 1
    }

    fun updateFadeState(): Boolean {
        var volume = this.targetVolume
        val targetLeft: Int
        val targetRight: Int
        if (volume == Int.MIN_VALUE) {
            targetRight = 0
            targetLeft = 0
            volume = 0
        } else {
            targetLeft = StaticMethods.calculateSoundAttenuation(volume, this.panPosition)
            targetRight = StaticMethods.calculateSoundPan(volume, this.panPosition)
        }

        if (volume == this.currentVolume && targetLeft == this.leftChannelVolume && targetRight == this.rightChannelVolume) {
            if (this.targetVolume == Int.MIN_VALUE) {
                this.targetVolume = 0
                this.rightChannelVolume = 0
                this.leftChannelVolume = 0
                this.currentVolume = 0
                unlink()
                return true
            } else {
                recalculateChannelVolumes()
                return false
            }
        } else {
            if (this.currentVolume < volume) {
                this.volumeFadeDelta = 1
                this.fadeSamplesRemaining = volume - this.currentVolume
            } else if (this.currentVolume > volume) {
                this.volumeFadeDelta = -1
                this.fadeSamplesRemaining = this.currentVolume - volume
            } else {
                this.volumeFadeDelta = 0
            }

            if (this.leftChannelVolume < targetLeft) {
                this.leftVolumeFadeDelta = 1
                if (this.fadeSamplesRemaining == 0 || this.fadeSamplesRemaining > targetLeft - this.leftChannelVolume) {
                    this.fadeSamplesRemaining = targetLeft - this.leftChannelVolume
                }
            } else if (this.leftChannelVolume > targetLeft) {
                this.leftVolumeFadeDelta = -1
                if (this.fadeSamplesRemaining == 0 || this.fadeSamplesRemaining > this.leftChannelVolume - targetLeft) {
                    this.fadeSamplesRemaining = this.leftChannelVolume - targetLeft
                }
            } else {
                this.leftVolumeFadeDelta = 0
            }

            if (this.rightChannelVolume < targetRight) {
                this.rightVolumeFadeDelta = 1
                if (this.fadeSamplesRemaining == 0 || this.fadeSamplesRemaining > targetRight - this.rightChannelVolume) {
                    this.fadeSamplesRemaining = targetRight - this.rightChannelVolume
                }
            } else if (this.rightChannelVolume > targetRight) {
                this.rightVolumeFadeDelta = -1
                if (this.fadeSamplesRemaining == 0 || this.fadeSamplesRemaining > this.rightChannelVolume - targetRight) {
                    this.fadeSamplesRemaining = this.rightChannelVolume - targetRight
                }
            } else {
                this.rightVolumeFadeDelta = 0
            }

            return false
        }
    }

    @Synchronized
    override fun skip(sampleCount: Int) {
        var remaining = sampleCount
        if (this.fadeSamplesRemaining > 0) {
            if (remaining >= this.fadeSamplesRemaining) {
                if (this.targetVolume == Int.MIN_VALUE) {
                    this.targetVolume = 0
                    this.rightChannelVolume = 0
                    this.leftChannelVolume = 0
                    this.currentVolume = 0
                    unlink()
                    remaining = this.fadeSamplesRemaining
                }

                this.fadeSamplesRemaining = 0
                recalculateChannelVolumes()
            } else {
                this.currentVolume += remaining * this.volumeFadeDelta
                this.leftChannelVolume += this.leftVolumeFadeDelta * remaining
                this.rightChannelVolume += this.rightVolumeFadeDelta * remaining
                this.fadeSamplesRemaining -= remaining
            }
        }

        val loopStart = this.start shl 8
        val loopEnd = this.end shl 8
        val totalSamplesShifted = ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
        val loopLength = loopEnd - loopStart
        if (loopLength <= 0) {
            this.loopsRemaining = 0
        }

        if (this.playbackPosition < 0) {
            if (this.sampleRate <= 0) {
                cancelFade()
                unlink()
                return
            }

            this.playbackPosition = 0
        }

        if (this.playbackPosition >= totalSamplesShifted) {
            if (this.sampleRate >= 0) {
                cancelFade()
                unlink()
                return
            }

            this.playbackPosition = totalSamplesShifted - 1
        }

        this.playbackPosition += this.sampleRate * remaining
        if (this.loopsRemaining < 0) {
            if (this.enableLoop) {
                if (this.sampleRate < 0) {
                    if (this.playbackPosition >= loopStart) {
                        return
                    }

                    this.playbackPosition = loopStart + loopStart - 1 - this.playbackPosition
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                }

                while (this.playbackPosition >= loopEnd) {
                    this.playbackPosition = loopEnd + loopEnd - 1 - this.playbackPosition
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                    if (this.playbackPosition >= loopStart) {
                        break
                    }

                    this.playbackPosition = loopStart + loopStart - 1 - this.playbackPosition
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                }
            } else if (this.sampleRate < 0) {
                if (this.playbackPosition < loopStart) {
                    this.playbackPosition = loopEnd - 1 - (loopEnd - 1 - this.playbackPosition) % loopLength
                }
            } else if (this.playbackPosition >= loopEnd) {
                this.playbackPosition = loopStart + (this.playbackPosition - loopStart) % loopLength
            }
        } else {
            if (this.loopsRemaining > 0) {
                if (this.enableLoop) {
                    run pingPongLoop@ {
                        if (this.sampleRate < 0) {
                            if (this.playbackPosition >= loopStart) {
                                return@pingPongLoop
                            }

                            this.playbackPosition = loopStart + loopStart - 1 - this.playbackPosition
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                            if (--this.loopsRemaining == 0) {
                                return@pingPongLoop
                            }
                        }

                        do {
                            if (this.playbackPosition < loopEnd) {
                                return@pingPongLoop
                            }

                            this.playbackPosition = loopEnd + loopEnd - 1 - this.playbackPosition
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                            if (--this.loopsRemaining == 0) {
                                break
                            }

                            if (this.playbackPosition >= loopStart) {
                                return@pingPongLoop
                            }

                            this.playbackPosition = loopStart + loopStart - 1 - this.playbackPosition
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                        } while (--this.loopsRemaining != 0)
                    }
                } else {
                    var loopSkips: Int
                    if (this.sampleRate < 0) {
                        if (this.playbackPosition >= loopStart) {
                            return
                        }

                        loopSkips = (loopEnd - 1 - this.playbackPosition) / loopLength
                        if (loopSkips < this.loopsRemaining) {
                            this.playbackPosition += loopLength * loopSkips
                            this.loopsRemaining -= loopSkips
                            return
                        }

                        this.playbackPosition += this.loopsRemaining * loopLength
                    } else {
                        if (this.playbackPosition < loopEnd) {
                            return
                        }

                        loopSkips = (this.playbackPosition - loopStart) / loopLength
                        if (loopSkips < this.loopsRemaining) {
                            this.playbackPosition -= loopLength * loopSkips
                            this.loopsRemaining -= loopSkips
                            return
                        }

                        this.playbackPosition -= loopLength * this.loopsRemaining
                    }
                    this.loopsRemaining = 0
                }
            }

            if (this.sampleRate < 0) {
                if (this.playbackPosition < 0) {
                    this.playbackPosition = -1
                    cancelFade()
                    unlink()
                }
            } else if (this.playbackPosition >= totalSamplesShifted) {
                this.playbackPosition = totalSamplesShifted
                cancelFade()
                unlink()
            }
        }
    }

    override fun getFirstStream(): PcmStream? {
        return null
    }

    @Synchronized
    fun setNumLoops(numLoops: Int) {
        this.loopsRemaining = numLoops
    }

    @Synchronized
    fun muteVolume() {
        setVolumeAndPan(0, getPanPosition())
    }

    fun recalculateChannelVolumes() {
        this.currentVolume = this.targetVolume
        this.leftChannelVolume = StaticMethods.calculateSoundAttenuation(this.targetVolume, this.panPosition)
        this.rightChannelVolume = StaticMethods.calculateSoundPan(this.targetVolume, this.panPosition)
    }

    override fun getPriority(): Int {
        var priority = this.currentVolume * 3 shr 6
        priority = (priority xor (priority shr 31)) + (priority ushr 31)
        if (this.loopsRemaining == 0) {
            priority -= priority * this.playbackPosition / (((super.buffer as AbstractAudioFile).getTotalSamples() shl 8))
        } else if (this.loopsRemaining >= 0) {
            priority -= priority * this.start / ((super.buffer as AbstractAudioFile).getTotalSamples())
        }

        return Math.min(255, priority)
    }

    @Synchronized
    fun setPanPosition(pan: Int) {
        setVolumeAndPan(getTargetVolume(), pan)
    }

    override abstract fun fill(mixBuffer: IntArray, offset: Int, length: Int)

    override fun getNextStream(): PcmStream? {
        return null
    }

    fun cancelFade() {
        if (this.fadeSamplesRemaining != 0) {
            if (this.targetVolume == Int.MIN_VALUE) {
                this.targetVolume = 0
            }

            this.fadeSamplesRemaining = 0
            recalculateChannelVolumes()
        }
    }

    @Synchronized
    fun setVolumeAndPan(volume: Int, pan: Int) {
        this.targetVolume = volume
        this.panPosition = pan
        this.fadeSamplesRemaining = 0
        recalculateChannelVolumes()
    }
}
