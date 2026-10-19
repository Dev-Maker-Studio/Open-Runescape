package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.sound.jagtheora.AreaSoundStream
import com.jagex.game.runetek5.sound.jagtheora.MixedAudioStream
import com.jagex.game.runetek5.sound.Resampler
import com.jagex.game.runetek5.sound.jagtheora.AbstractAudioFile

/**
 *
 * @author Brainpower - 19/04/2022
 * @author lequietriot - 07/05/2020
 * Reference: https://github.com/lequietriot/RuneScape-MIDI-Player-Suite/blob/master/src/main/AudioBuffer.java
 *
 */
class MixedAudioBuffer(
    @JvmField val samples: ByteArray,
    start: Int,
    end: Int
) : AbstractAudioFile() {

    init {
        this.sampleRate = 22050
        this.start = start
        this.end = end
    }

    fun resample(resampler: Resampler): MixedAudioBuffer {
        val resampled = resampler.resample(this.samples)
        super.sampleRate = resampler.scaleRate(super.sampleRate)
        if (super.start == super.end) {
            super.start = resampler.scalePosition(super.start).also { super.end = it }
        } else {
            super.start = resampler.scalePosition(super.start)
            super.end = resampler.scalePosition(super.end)
            if (super.start == super.end) {
                super.start--
            }
        }
        return MixedAudioBuffer(resampled, super.start, super.end)
    }

    override fun getTotalSamples(): Int {
        return this.samples.size
    }

    override fun getStream(sampleRate: Int, volume: Int, pan: Int): AreaSoundStream {
        return MixedAudioStream(this, sampleRate, volume, pan)
    }
}
