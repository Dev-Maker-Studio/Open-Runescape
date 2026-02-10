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
    fun setPlaybackRate(i_1: Int) {
        if (this.sampleRate < 0) {
            this.sampleRate = -i_1
        } else {
            this.sampleRate = i_1
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
    fun setPlaybackPosition(i_1: Int) {
        var i_11 = i_1
        val i_3 = ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
        if (i_11 < -1) {
            i_11 = -1
        }

        if (i_11 > i_3) {
            i_11 = i_3
        }

        this.playbackPosition = i_11
    }

    @Synchronized
    fun isFinished(): Boolean {
        return this.playbackPosition < 0 || this.playbackPosition >= ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
    }

    @Synchronized
    fun fadeToVolume(i_1: Int, i_2: Int) {
        fadeToVolumeAndPan(i_1, i_2, getPanPosition())
    }

    @Synchronized
    fun fadeToVolumeAndPan(i_1: Int, i_2: Int, i_3: Int) {
        var i_11 = i_1
        if (i_11 == 0) {
            setVolumeAndPan(i_2, i_3)
        } else {
            val i_5 = StaticMethods.calculateSoundAttenuation(i_2, i_3)
            val i_6 = StaticMethods.calculateSoundPan(i_2, i_3)
            if (i_5 == this.leftChannelVolume && i_6 == this.rightChannelVolume) {
                this.fadeSamplesRemaining = 0
            } else {
                var i_7 = i_2 - this.currentVolume
                if (this.currentVolume - i_2 > i_7) {
                    i_7 = this.currentVolume - i_2
                }

                if (i_5 - this.leftChannelVolume > i_7) {
                    i_7 = i_5 - this.leftChannelVolume
                }

                if (this.leftChannelVolume - i_5 > i_7) {
                    i_7 = this.leftChannelVolume - i_5
                }

                if (i_6 - this.rightChannelVolume > i_7) {
                    i_7 = i_6 - this.rightChannelVolume
                }

                if (this.rightChannelVolume - i_6 > i_7) {
                    i_7 = this.rightChannelVolume - i_6
                }

                if (i_11 > i_7) {
                    i_11 = i_7
                }

                this.fadeSamplesRemaining = i_11
                this.targetVolume = i_2
                this.panPosition = i_3
                this.volumeFadeDelta = (i_2 - this.currentVolume) / i_11
                this.leftVolumeFadeDelta = (i_5 - this.leftChannelVolume) / i_11
                this.rightVolumeFadeDelta = (i_6 - this.rightChannelVolume) / i_11
            }
        }
    }

    @Synchronized
    fun fadeOutAndStop(i_1: Int) {
        var i_11 = i_1
        if (i_11 == 0) {
            muteVolume()
            unlink()
        } else if (this.leftChannelVolume == 0 && this.rightChannelVolume == 0) {
            this.fadeSamplesRemaining = 0
            this.targetVolume = 0
            this.currentVolume = 0
            unlink()
        } else {
            var i_3 = -this.currentVolume
            if (this.currentVolume > i_3) {
                i_3 = this.currentVolume
            }

            if (-this.leftChannelVolume > i_3) {
                i_3 = -this.leftChannelVolume
            }

            if (this.leftChannelVolume > i_3) {
                i_3 = this.leftChannelVolume
            }

            if (-this.rightChannelVolume > i_3) {
                i_3 = -this.rightChannelVolume
            }

            if (this.rightChannelVolume > i_3) {
                i_3 = this.rightChannelVolume
            }

            if (i_11 > i_3) {
                i_11 = i_3
            }

            this.fadeSamplesRemaining = i_11
            this.targetVolume = Int.MIN_VALUE
            this.volumeFadeDelta = -this.currentVolume / i_11
            this.leftVolumeFadeDelta = -this.leftChannelVolume / i_11
            this.rightVolumeFadeDelta = -this.rightChannelVolume / i_11
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
        var i_2 = this.targetVolume
        val i_3: Int
        val i_4: Int
        if (i_2 == Int.MIN_VALUE) {
            i_4 = 0
            i_3 = 0
            i_2 = 0
        } else {
            i_3 = StaticMethods.calculateSoundAttenuation(i_2, this.panPosition)
            i_4 = StaticMethods.calculateSoundPan(i_2, this.panPosition)
        }

        if (i_2 == this.currentVolume && i_3 == this.leftChannelVolume && i_4 == this.rightChannelVolume) {
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
            if (this.currentVolume < i_2) {
                this.volumeFadeDelta = 1
                this.fadeSamplesRemaining = i_2 - this.currentVolume
            } else if (this.currentVolume > i_2) {
                this.volumeFadeDelta = -1
                this.fadeSamplesRemaining = this.currentVolume - i_2
            } else {
                this.volumeFadeDelta = 0
            }

            if (this.leftChannelVolume < i_3) {
                this.leftVolumeFadeDelta = 1
                if (this.fadeSamplesRemaining == 0 || this.fadeSamplesRemaining > i_3 - this.leftChannelVolume) {
                    this.fadeSamplesRemaining = i_3 - this.leftChannelVolume
                }
            } else if (this.leftChannelVolume > i_3) {
                this.leftVolumeFadeDelta = -1
                if (this.fadeSamplesRemaining == 0 || this.fadeSamplesRemaining > this.leftChannelVolume - i_3) {
                    this.fadeSamplesRemaining = this.leftChannelVolume - i_3
                }
            } else {
                this.leftVolumeFadeDelta = 0
            }

            if (this.rightChannelVolume < i_4) {
                this.rightVolumeFadeDelta = 1
                if (this.fadeSamplesRemaining == 0 || this.fadeSamplesRemaining > i_4 - this.rightChannelVolume) {
                    this.fadeSamplesRemaining = i_4 - this.rightChannelVolume
                }
            } else if (this.rightChannelVolume > i_4) {
                this.rightVolumeFadeDelta = -1
                if (this.fadeSamplesRemaining == 0 || this.fadeSamplesRemaining > this.rightChannelVolume - i_4) {
                    this.fadeSamplesRemaining = this.rightChannelVolume - i_4
                }
            } else {
                this.rightVolumeFadeDelta = 0
            }

            return false
        }
    }

    @Synchronized
    override fun skip(i_1: Int) {
        var i_11 = i_1
        if (this.fadeSamplesRemaining > 0) {
            if (i_11 >= this.fadeSamplesRemaining) {
                if (this.targetVolume == Int.MIN_VALUE) {
                    this.targetVolume = 0
                    this.rightChannelVolume = 0
                    this.leftChannelVolume = 0
                    this.currentVolume = 0
                    unlink()
                    i_11 = this.fadeSamplesRemaining
                }

                this.fadeSamplesRemaining = 0
                recalculateChannelVolumes()
            } else {
                this.currentVolume += i_11 * this.volumeFadeDelta
                this.leftChannelVolume += this.leftVolumeFadeDelta * i_11
                this.rightChannelVolume += this.rightVolumeFadeDelta * i_11
                this.fadeSamplesRemaining -= i_11
            }
        }

        val i_2 = this.start shl 8
        val i_3 = this.end shl 8
        val i_4 = ((super.buffer as AbstractAudioFile).getTotalSamples() shl 8)
        val i_5 = i_3 - i_2
        if (i_5 <= 0) {
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

        if (this.playbackPosition >= i_4) {
            if (this.sampleRate >= 0) {
                cancelFade()
                unlink()
                return
            }

            this.playbackPosition = i_4 - 1
        }

        this.playbackPosition += this.sampleRate * i_11
        if (this.loopsRemaining < 0) {
            if (this.enableLoop) {
                if (this.sampleRate < 0) {
                    if (this.playbackPosition >= i_2) {
                        return
                    }

                    this.playbackPosition = i_2 + i_2 - 1 - this.playbackPosition
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                }

                while (this.playbackPosition >= i_3) {
                    this.playbackPosition = i_3 + i_3 - 1 - this.playbackPosition
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                    if (this.playbackPosition >= i_2) {
                        break
                    }

                    this.playbackPosition = i_2 + i_2 - 1 - this.playbackPosition
                    this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                }
            } else if (this.sampleRate < 0) {
                if (this.playbackPosition < i_2) {
                    this.playbackPosition = i_3 - 1 - (i_3 - 1 - this.playbackPosition) % i_5
                }
            } else if (this.playbackPosition >= i_3) {
                this.playbackPosition = i_2 + (this.playbackPosition - i_2) % i_5
            }
        } else {
            if (this.loopsRemaining > 0) {
                if (this.enableLoop) {
                    label123@ {
                        if (this.sampleRate < 0) {
                            if (this.playbackPosition >= i_2) {
                                return@label123
                            }

                            this.playbackPosition = i_2 + i_2 - 1 - this.playbackPosition
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                            if (--this.loopsRemaining == 0) {
                                return@label123
                            }
                        }

                        do {
                            if (this.playbackPosition < i_3) {
                                return@label123
                            }

                            this.playbackPosition = i_3 + i_3 - 1 - this.playbackPosition
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                            if (--this.loopsRemaining == 0) {
                                break
                            }

                            if (this.playbackPosition >= i_2) {
                                return@label123
                            }

                            this.playbackPosition = i_2 + i_2 - 1 - this.playbackPosition
                            this.sampleRate = -(this.sampleRate * 1927994969) * -177305111
                        } while (--this.loopsRemaining != 0)
                    }
                } else {
                    var i_6: Int
                    if (this.sampleRate < 0) {
                        if (this.playbackPosition >= i_2) {
                            return
                        }

                        i_6 = (i_3 - 1 - this.playbackPosition) / i_5
                        if (i_6 < this.loopsRemaining) {
                            this.playbackPosition += i_5 * i_6
                            this.loopsRemaining -= i_6
                            return
                        }

                        this.playbackPosition += this.loopsRemaining * i_5
                    } else {
                        if (this.playbackPosition < i_3) {
                            return
                        }

                        i_6 = (this.playbackPosition - i_2) / i_5
                        if (i_6 < this.loopsRemaining) {
                            this.playbackPosition -= i_5 * i_6
                            this.loopsRemaining -= i_6
                            return
                        }

                        this.playbackPosition -= i_5 * this.loopsRemaining
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
            } else if (this.playbackPosition >= i_4) {
                this.playbackPosition = i_4
                cancelFade()
                unlink()
            }
        }
    }

    override fun getFirstStream(): PcmStream? {
        return null
    }

    @Synchronized
    fun setNumLoops(i_1: Int) {
        this.loopsRemaining = i_1
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
        var i_1 = this.currentVolume * 3 shr 6
        i_1 = (i_1 xor (i_1 shr 31)) + (i_1 ushr 31)
        if (this.loopsRemaining == 0) {
            i_1 -= i_1 * this.playbackPosition / (((super.buffer as AbstractAudioFile).getTotalSamples() shl 8))
        } else if (this.loopsRemaining >= 0) {
            i_1 -= i_1 * this.start / ((super.buffer as AbstractAudioFile).getTotalSamples())
        }

        return Math.min(255, i_1)
    }

    @Synchronized
    fun setPanPosition(i_1: Int) {
        setVolumeAndPan(getTargetVolume(), i_1)
    }

    override abstract fun fill(var1: IntArray, var2: Int, var3: Int)

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
    fun setVolumeAndPan(i_1: Int, i_2: Int) {
        this.targetVolume = i_1
        this.panPosition = i_2
        this.fadeSamplesRemaining = 0
        recalculateChannelVolumes()
    }
}
