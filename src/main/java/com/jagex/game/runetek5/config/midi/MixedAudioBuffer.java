package com.jagex.game.runetek5.config.midi;

import com.jagex.game.runetek5.sound.jagtheora.AreaSoundStream;
import com.jagex.game.runetek5.sound.jagtheora.MixedAudioStream;
import com.jagex.game.runetek5.sound.Resampler;
import com.jagex.game.runetek5.sound.jagtheora.AbstractAudioFile;

/**
 * 
 * @author Brainpower - 19/04/2022
 * @author lequietriot - 07/05/2020
 * Reference: https://github.com/lequietriot/RuneScape-MIDI-Player-Suite/blob/master/src/main/AudioBuffer.java
 *
 */
public class MixedAudioBuffer extends AbstractAudioFile {

    public byte[] samples;

    public MixedAudioBuffer(byte[] samples, int start, int end) {
        this.sampleRate = 22050;
        this.samples = samples;
        this.start = start;
        this.end = end;
    }

    public MixedAudioBuffer resample(Resampler resampler) {
        this.samples = resampler.resample(this.samples);
        super.sampleRate = resampler.scaleRate(super.sampleRate);
        if (super.start == super.end) {
            super.start = super.end = resampler.scalePosition(super.start);
        } else {
            super.start = resampler.scalePosition(super.start);
            super.end = resampler.scalePosition(super.end);
            if (super.start == super.end) {
                super.start--;
            }
        }
        return this;
    }

    @Override
    public int getTotalSamples() {
        return this.samples.length;
    }

    @Override
    public AreaSoundStream getStream(int sampleRate, int volume, int i_3) {
        return this.samples != null && this.samples.length != 0 ? new MixedAudioStream(this, sampleRate, volume, i_3) : null;
    }
}