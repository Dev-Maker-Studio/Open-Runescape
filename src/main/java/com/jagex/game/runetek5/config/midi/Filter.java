package com.jagex.game.runetek5.config.midi;

import com.jagex.game.runetek5.buffer.Packet;

class Filter {

    public static final float[][] COEFFICIENT_FLOAT = new float[2][8];
    static final int[][] COEFFICIENT_INT = new int[2][8];
    public static float invUnityFloat;
    static int invUnityInt;
    final int[] numPairs = new int[2];
    public final int[][][] pairPhase = new int[2][2][4];
    public final int[][][] pairMagnitude = new int[2][2][4];
    public final int[] unity = new int[2];

    public static float normalize(float f) {
        float F = 32.703197F * (float) Math.pow(2.0D, f);
        return F * 3.1415927F / 11025.0F;
    }

    public float adaptMagnitude(int dir, int k, float t) {
        float alpha = this.pairMagnitude[dir][0][k] + t * (this.pairMagnitude[dir][1][k] - this.pairMagnitude[dir][0][k]);
        alpha *= 0.0015258789F;
        return 1.0F - (float) Math.pow(10.0D, -alpha / 20.0F);
    }

    public float adaptPhase(int dir, int i, float t) {
        float F = this.pairPhase[dir][0][i] + t * (this.pairPhase[dir][1][i] - this.pairPhase[dir][0][i]);
        F *= 1.2207031E-4F;
        return normalize(F);
    }

    void decode(Packet buffer, Envelope envelope) {
        int count = buffer.readUByte();
        this.numPairs[0] = count >> 4;
        this.numPairs[1] = count & 0xf;
        if (count != 0) {
            this.unity[0] = buffer.readUShort();
            this.unity[1] = buffer.readUShort();
            int migrated = buffer.readUByte();

            for (int dir = 0; dir < 2; dir++) {
                for (int term = 0; term < this.numPairs[dir]; term++) {
                    this.pairPhase[dir][0][term] = buffer.readUShort();
                    this.pairMagnitude[dir][0][term] = buffer.readUShort();
                }
            }

            for (int dir = 0; dir < 2; dir++) {
                for (int phase = 0; phase < this.numPairs[dir]; phase++) {
                    if ((migrated & 1 << dir * 4 << phase) != 0) {
                        this.pairPhase[dir][1][phase] = buffer.readUShort();
                        this.pairMagnitude[dir][1][phase] = buffer.readUShort();
                    } else {
                        this.pairPhase[dir][1][phase] = this.pairPhase[dir][0][phase];
                        this.pairMagnitude[dir][1][phase] = this.pairMagnitude[dir][0][phase];
                    }
                }
            }

            if (migrated != 0 || this.unity[1] != this.unity[0]) {
                envelope.decodeShape(buffer);
            }
        } else {
            int[] placeholder = this.unity;
            this.unity[1] = 0;
            placeholder[0] = 0;
        }

    }

    int compute(int dir, float t) {
        if (dir == 0) {
            float a0 = this.unity[0] + (this.unity[1] - this.unity[0]) * t;
            a0 *= 0.0030517578F;
            invUnityFloat = (float) Math.pow(0.1D, a0 / 20.0F);
            invUnityInt = (int) (invUnityFloat * 65536.0F);
        }

        if (this.numPairs[dir] == 0) {
            return 0;
        } 
        float f_3 = adaptMagnitude(dir, 0, t);
        COEFFICIENT_FLOAT[dir][0] = -2.0f * f_3 * (float) Math.cos(adaptPhase(dir, 0, t));
        COEFFICIENT_FLOAT[dir][1] = f_3 * f_3;

        for (int pair = 1; pair < this.numPairs[dir]; pair++) {
            f_3 = adaptMagnitude(dir, pair, t);
            float f_5 = -2.0f * f_3 * (float) Math.cos(adaptPhase(dir, pair, t));
            float f_6 = f_3 * f_3;
            COEFFICIENT_FLOAT[dir][pair * 2 + 1] = COEFFICIENT_FLOAT[dir][pair * 2 - 1] * f_6;
            COEFFICIENT_FLOAT[dir][pair * 2] = COEFFICIENT_FLOAT[dir][pair * 2 - 1] * f_5 + COEFFICIENT_FLOAT[dir][pair * 2 - 2] * f_6;

            for (int i_7 = pair * 2 - 1; i_7 >= 2; --i_7) {
                COEFFICIENT_FLOAT[dir][i_7] += COEFFICIENT_FLOAT[dir][i_7 - 1] * f_5 + COEFFICIENT_FLOAT[dir][i_7 - 2] * f_6;
            }

            COEFFICIENT_FLOAT[dir][1] += COEFFICIENT_FLOAT[dir][0] * f_5 + f_6;
            COEFFICIENT_FLOAT[dir][0] += f_5;
        }

        if (dir == 0) {
            for (int pair = 0; pair < this.numPairs[0] * 2; pair++) {
                COEFFICIENT_FLOAT[0][pair] *= invUnityFloat;
            }
        }

        for (int pair = 0; pair < this.numPairs[dir] * 2; pair++) {
            COEFFICIENT_INT[dir][pair] = (int) (COEFFICIENT_FLOAT[dir][pair] * 65536.0F);
        }

        return this.numPairs[dir] * 2;
    }
}