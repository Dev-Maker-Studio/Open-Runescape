package com.jagex.game.runetek5.sound.jagtheora

/**
 *
 * @author Brainpower - 19/04/2022
 * @author lequietriot - 07/05/2020
 * Reference: https://github.com/lequietriot/RuneScape-MIDI-Player-Suite/blob/master/src/main/AudioBuffer.java
 *
 */
class AudioBuffer(
    sampleRate: Int,
    @JvmField val samples: FloatArray,
    start: Int,
    end: Int,
    enableLoop: Boolean
) : AbstractAudioFile() {

    init {
        this.sampleRate = sampleRate
        this.start = start
        this.end = end
        this.enableLoop = enableLoop
    }

    override fun getTotalSamples(): Int {
        return this.samples.size
    }

    override fun getStream(sampleRate: Int, volume: Int, pan: Int): AreaSoundStream {
        return AudioBufferStream(this, sampleRate, volume, pan)
    }

    fun getSamples(): FloatArray {
        return this.samples
    }
}
