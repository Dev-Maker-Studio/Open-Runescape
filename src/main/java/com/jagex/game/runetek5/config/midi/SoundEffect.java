package com.jagex.game.runetek5.config.midi;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 05/02/2023
 *
 */
public class SoundEffect {

    public final Instrument[] instruments = new Instrument[10];
    public int loopBegin;
    public int loopEnd;

    public SoundEffect(Packet buffer) {
        for (int index = 0; index < 10; index++) {
            int hasInstruments = buffer.readUByte();
            if (hasInstruments != 0) {
                buffer.currentPosition--;
                this.instruments[index] = new Instrument();
                this.instruments[index].decodeInstruments(buffer);
            }
        }

        this.loopBegin = buffer.readUShort();
        this.loopEnd = buffer.readUShort();
    }

    public MixedAudioBuffer getMixedAudio() {
        return new MixedAudioBuffer(mix(), this.loopBegin * 22050 / 1000, this.loopEnd * 22050 / 1000);
    }

    public int getDelay() {
        int delay = 9999999;
        for (int index = 0; index < 10; index++) {
            if (this.instruments[index] != null && this.instruments[index].offset / 20 < delay) {
                delay = this.instruments[index].offset / 20;
            }
        }

        if (this.loopBegin < this.loopEnd && this.loopBegin / 20 < delay) {
            delay = this.loopBegin / 20;
        }

        if (delay != 9999999 && delay != 0) {
            for (int index = 0; index < 10; index++) {
                if (this.instruments[index] != null) {
                    this.instruments[index].offset -= delay * 20;
                }
            }

            if (this.loopBegin < this.loopEnd) {
                this.loopBegin -= delay * 20;
                this.loopEnd -= delay * 20;
            }

            return delay;
        } 
        return 0;
    }

    public byte[] mix() {
        int duration = 0;

        for (int i = 0; i < 10; i++) {
            if (this.instruments[i] != null && this.instruments[i].duration + this.instruments[i].offset > duration) {
                duration = this.instruments[i].duration + this.instruments[i].offset;
            }
        }

        if (duration == 0) {
            return new byte[0];
        } 
        int ns = duration * 22050 / 1000;
        byte[] mixed = new byte[ns];
        
        for (int i = 0; i < 10; i++) {
            if (this.instruments[i] != null) {
                int mixDuration = this.instruments[i].duration * 22050 / 1000;
                int offset = this.instruments[i].offset * 22050 / 1000;
                int[] samples = this.instruments[i].synthesize(mixDuration, this.instruments[i].duration);
                for (int j = 0; j < mixDuration; j++) {
                    int out = (samples[j] >> 8) + mixed[j + offset];
                    if ((out + 128 & -256) != 0) {
                        out = out >> 31 ^ 0x7f;
                    }
                    mixed[j + offset] = (byte) out;
                }
            }
        }
        return mixed;
    }
}