package com.jagex.game.runetek5.config.midi;

import java.util.Arrays;
import java.util.Random;

import com.jagex.game.runetek5.buffer.Packet;

public class Instrument {

    public static final int[] NOISE = new int[32768];
    public static final int[] SINE;
    public static final int[] OUTPUT;
    public static final int[] PHASES;
    public static final int[] DELAYS;
    public static final int[] VOLUME_STEP;
    public static final int[] PITCH_STEP;
    public static final int[] PITCH_BASE_STEP;

    static {
        Random rand = new Random(0L);
        for (int i = 0; i < 32768; i++) {
            NOISE[i] = (rand.nextInt() & 0x2) - 1;
        }
        SINE = new int[32768];
        for (int i = 0; i < 32768; i++) {
            SINE[i] = (int) (Math.sin(i / 5215.1903D) * 16384.0D);
        }
        OUTPUT = new int[220500];
        PHASES = new int[5];
        DELAYS = new int[5];
        VOLUME_STEP = new int[5];
        PITCH_STEP = new int[5];
        PITCH_BASE_STEP = new int[5];
    }

    public Envelope pitch;
    public Envelope volume;
    public Envelope pitchModifier;
    public Envelope pitchModifierAmplitude;
    public Envelope volumeMultiplier;
    public Envelope volumeAmplitude;
    public Envelope release;
    public Envelope attack;
    public int delayTime;
    public int delayDecay = 100;
    public int duration = 500;
    public int offset;
    public Filter filter;
    public Envelope filterEnvelope;
    public final int[] oscillatorVolume = new int[5];
    public final int[] oscillatorPitch = new int[5];
    public final int[] oscillatorDelays = new int[5];

    public int[] synthesize(int mixDuration, int instrDuration) {
        Arrays.fill(OUTPUT, 0, mixDuration, 0);
        if (instrDuration >= 10) {
            double fs = mixDuration / (instrDuration + 0.0D);
            this.pitch.reset();
            this.volume.reset();
            int pitchModStep = 0;
            int pitchModBaseStep = 0;
            int pitchModPhase = 0;
            if (this.pitchModifier != null) {
                this.pitchModifier.reset();
                this.pitchModifierAmplitude.reset();
                pitchModStep = (int) ((this.pitchModifier.end - this.pitchModifier.start) * 32.768D / fs);
                pitchModBaseStep = (int) (this.pitchModifier.start * 32.768D / fs);
            }

            int volumeModStep = 0;
            int volumeModBaseStep = 0;
            int volumeModPhase = 0;
            if (this.volumeMultiplier != null) {
                this.volumeMultiplier.reset();
                this.volumeAmplitude.reset();
                volumeModStep = (int) ((this.volumeMultiplier.end - this.volumeMultiplier.start) * 32.768D / fs);
                volumeModBaseStep = (int) (this.volumeMultiplier.start * 32.768D / fs);
            }

            for (int i = 0; i < 5; i++) {
                if (this.oscillatorVolume[i] != 0) {
                    PHASES[i] = 0;
                    DELAYS[i] = (int) (this.oscillatorDelays[i] * fs);
                    VOLUME_STEP[i] = (this.oscillatorVolume[i] << 14) / 100;
                    PITCH_STEP[i] = (int) ((this.pitch.end - this.pitch.start) * 32.768D * Math.pow(1.0057929410678534D, this.oscillatorPitch[i]) / fs);
                    PITCH_BASE_STEP[i] = (int) (this.pitch.start * 32.768D / fs);
                }
            }

            for (int i = 0; i < mixDuration; i++) {
                int pitchChange = this.pitch.step(mixDuration);
                int volumeChange = this.volume.step(mixDuration);
                if (this.pitchModifier != null) {
                    int mod = this.pitchModifier.step(mixDuration);
                    int modAmplitude = this.pitchModifierAmplitude.step(mixDuration);
                    pitchChange += evaluateWave(pitchModPhase, modAmplitude, this.pitchModifier.form) >> 1;
                    pitchModPhase += pitchModBaseStep + (mod * pitchModStep >> 16);
                }

                if (this.volumeMultiplier != null) {
                    int mod = this.volumeMultiplier.step(mixDuration);
                    int modAmplitude = this.volumeAmplitude.step(mixDuration);
                    volumeChange = volumeChange * ((evaluateWave(volumeModPhase, modAmplitude, this.volumeMultiplier.form) >> 1) + 32768) >> 15;
                    volumeModPhase += volumeModBaseStep + (mod * volumeModStep >> 16);
                }

                for (int j = 0; j < 5; j++) {
                    if (this.oscillatorVolume[j] != 0) {
                        int i_15 = DELAYS[j] + i;
                        if (i_15 < mixDuration) {
                            OUTPUT[i_15] += evaluateWave(PHASES[j], volumeChange * VOLUME_STEP[j] >> 15, this.pitch.form);
                            PHASES[j] += (pitchChange * PITCH_STEP[j] >> 16) + PITCH_BASE_STEP[j];
                        }
                    }
                }
            }

            if (this.release != null) {
                this.release.reset();
                this.attack.reset();
                int counter = 0;
                boolean muted = true;

                for (int i = 0; i < mixDuration; i++) {
                    int onStep = this.release.step(mixDuration);
                    int offStep = this.attack.step(mixDuration);
                    int threshold;
                    if (muted) {
                        threshold = (onStep * (this.release.end - this.release.start) >> 8) + this.release.start;
                    } else {
                        threshold = (offStep * (this.release.end - this.release.start) >> 8) + this.release.start;
                    }

                    counter += 256;
                    if (counter >= threshold) {
                        counter = 0;
                        muted = !muted;
                    }

                    if (muted) {
                        OUTPUT[i] = 0;
                    }
                }
            }

            if (this.delayTime > 0 && this.delayDecay > 0) {
                int delay = (int) (this.delayTime * fs);

                for (int i = delay; i < mixDuration; i++) {
                    OUTPUT[i] += OUTPUT[i - delay] * this.delayDecay / 100;
                }
            }

            if (this.filter.numPairs[0] > 0 || this.filter.numPairs[1] > 0) {
                this.filterEnvelope.reset();
                int t = this.filterEnvelope.step(mixDuration + 1);
                int M = this.filter.compute(0, t / 65536.0F);
                int N = this.filter.compute(1, t / 65536.0F);
                if (mixDuration >= M + N) {
                    int n = 0;
                    int delay = N;
                    if (N > mixDuration - M) {
                        delay = mixDuration - M;
                    }

                    while (n < delay) {
                        int y = (int) ((long) OUTPUT[n + M] * Filter.invUnityInt >> 16);

                        for (int i_17 = 0; i_17 < M; i_17++) {
                            y += (int) ((long) OUTPUT[n + M - 1 - i_17] * Filter.COEFFICIENT_INT[0][i_17] >> 16);
                        }

                        for (int i_17 = 0; i_17 < n; i_17++) {
                            y -= (int) ((long) OUTPUT[n - 1 - i_17] * Filter.COEFFICIENT_INT[1][i_17] >> 16);
                        }

                        OUTPUT[n] = y;
                        t = this.filterEnvelope.step(mixDuration + 1);
                        ++n;
                    }

                    delay = 128;

                    while (true) {
                        if (delay > mixDuration - M) {
                            delay = mixDuration - M;
                        }

                        while (n < delay) {
                            int y = (int) ((long) OUTPUT[n + M] * Filter.invUnityInt >> 16);

                            for (int i = 0; i < M; i++) {
                                y += (int) ((long) OUTPUT[n + M - 1 - i] * Filter.COEFFICIENT_INT[0][i] >> 16);
                            }

                            for (int i = 0; i < N; i++) {
                                y -= (int) ((long) OUTPUT[n - 1 - i] * Filter.COEFFICIENT_INT[1][i] >> 16);
                            }

                            OUTPUT[n] = y;
                            t = this.filterEnvelope.step(mixDuration + 1);
                            ++n;
                        }

                        if (n >= mixDuration - M) {
                            while (n < mixDuration) {
                                int y = 0;

                                for (int i = n + M - mixDuration; i < M; i++) {
                                    y += (int) ((long) OUTPUT[n + M - 1 - i] * Filter.COEFFICIENT_INT[0][i] >> 16);
                                }

                                for (int i = 0; i < N; i++) {
                                    y -= (int) ((long) OUTPUT[n - 1 - i] * Filter.COEFFICIENT_INT[1][i] >> 16);
                                }

                                OUTPUT[n] = y;
                                this.filterEnvelope.step(mixDuration + 1);
                                ++n;
                            }
                            break;
                        }

                        M = this.filter.compute(0, t / 65536.0F);
                        N = this.filter.compute(1, t / 65536.0F);
                        delay += 128;
                    }
                }
            }

            for (int i = 0; i < mixDuration; i++) {
                if (OUTPUT[i] < -32768) {
                    OUTPUT[i] = -32768;
                }

                if (OUTPUT[i] > 32767) {
                    OUTPUT[i] = 32767;
                }
            }

        }
        return OUTPUT;
    }

    public int evaluateWave(int table, int phase, int amplitude) {
        return amplitude == 1 ? ((table & 0x7fff) < 16384 ? phase : -phase) : (amplitude == 2 ? SINE[table & 0x7fff] * phase >> 14 : (amplitude == 3 ? (phase * (table & 0x7fff) >> 14) - phase : (amplitude == 4 ? phase * NOISE[table / 2607 & 0x7fff] : 0)));
    }

    public void decodeInstruments(Packet buffer) {
        this.pitch = new Envelope();
        this.pitch.decode(buffer);
        this.volume = new Envelope();
        this.volume.decode(buffer);
        int option = buffer.readUByte();
        if (option != 0) {
            buffer.currentPosition--;
            this.pitchModifier = new Envelope();
            this.pitchModifier.decode(buffer);
            this.pitchModifierAmplitude = new Envelope();
            this.pitchModifierAmplitude.decode(buffer);
        }

        option = buffer.readUByte();
        if (option != 0) {
            buffer.currentPosition--;
            this.volumeMultiplier = new Envelope();
            this.volumeMultiplier.decode(buffer);
            this.volumeAmplitude = new Envelope();
            this.volumeAmplitude.decode(buffer);
        }

        option = buffer.readUByte();
        if (option != 0) {
            buffer.currentPosition--;
            this.release = new Envelope();
            this.release.decode(buffer);
            this.attack = new Envelope();
            this.attack.decode(buffer);
        }

        for (int i = 0; i < 10; i++) {
            int volume = buffer.readUSmart();
            if (volume == 0) {
                break;
            }

            this.oscillatorVolume[i] = volume;
            this.oscillatorPitch[i] = buffer.readSmart();
            this.oscillatorDelays[i] = buffer.readUSmart();
        }

        this.delayTime = buffer.readUSmart();
        this.delayDecay = buffer.readUSmart();
        this.duration = buffer.readUShort();
        this.offset = buffer.readUShort();
        this.filter = new Filter();
        this.filterEnvelope = new Envelope();
        this.filter.decode(buffer, this.filterEnvelope);
    }

}
