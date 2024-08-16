package com.jagex.game.runetek5.sound.jagtheora;

/**
 * 
 * @author Brainpower - 19/04/2022
 * @author lequietriot - 07/05/2020
 * Reference: https://github.com/lequietriot/RuneScape-MIDI-Player-Suite/blob/master/src/main/AudioBuffer.java
 *
 */
public class AudioBuffer extends AbstractAudioFile {

    public final float[] samples;

    public AudioBuffer(int sampleRate, float[] samples, int start, int end, boolean enableLoop) {
        this.sampleRate = sampleRate;
        this.samples = samples;
        this.start = start;
        this.end = end;
        this.enableLoop = enableLoop;
    }

    @Override
    public int getTotalSamples() {
        return this.samples.length;
    }

    @Override
    public AreaSoundStream getStream(int sampleRate, int volume, int i_3) {
        return this.samples != null && this.samples.length != 0 ? new AudioBufferStream(this, sampleRate, volume, i_3) : null;
    }

    float[] getSamples() {
        return this.samples;
    }
}
