package com.jagex.game.runetek5.config.midi.instrument;

/**
 * 
 * @author Brainpower
 * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
 *
 */
public class MusicSampleResidue {

    public final int anInt3285 = MusicSample.getBits(16);
    public final int anInt3283 = MusicSample.getBits(24);
    public final int anInt3281 = MusicSample.getBits(24);
    public final int anInt3282 = MusicSample.getBits(24) + 1;
    public final int classifications = MusicSample.getBits(6) + 1;
    public final int anInt3286 = MusicSample.getBits(8);
    public final int[] residueBooks;

    public MusicSampleResidue() {
        int[] residueCascade = new int[this.classifications];
        for (int j = 0; j < this.classifications; j++) {
            int highBits = 0;
            int lowBits = MusicSample.getBits(3);
            if (MusicSample.getBit() != 0) {
                highBits = MusicSample.getBits(5);
            }

            residueCascade[j] = highBits << 3 | lowBits;
        }
        this.residueBooks = new int[this.classifications * 8];
        for (int k = 0; k < this.classifications * 8; k++) {
            this.residueBooks[k] = (residueCascade[k >> 3] & 1 << (k & 0x7)) != 0 ? MusicSample.getBits(8) : -1;
        }
    }

    public void decodeResidue(float[] output, int outputSize, boolean isFloat) {
        for (int i = 0; i < outputSize; i++) {
            output[i] = 0.0F;
        }

        if (!isFloat) {
            int classwords = MusicSample.codebooks[this.anInt3286].dimensions;
            int windowSize = this.anInt3281 - this.anInt3283;
            int numWindows = windowSize / this.anInt3282;
            int[] Classification = new int[numWindows];

            for (int pass = 0; pass < 8; pass++) {
                int pcount = 0;

                while (pcount < numWindows) {
                    if (pass == 0) {
                        int q = MusicSample.codebooks[this.anInt3286].decode();

                        for (int i = classwords - 1; i >= 0; --i) {
                            if (pcount + i < numWindows) {
                                Classification[pcount + i] = q % this.classifications;
                            }
                            q /= this.classifications;
                        }
                    }

                    for (int i = 0; i < classwords; i++) {
                        int c = Classification[pcount];
                        int i_12 = this.residueBooks[pass + c * 8];
                        if (i_12 >= 0) {
                            int i_13 = pcount * this.anInt3282 + this.anInt3283;
                            MusicSampleCodebook codebook = MusicSample.codebooks[i_12];
                            int i_15;
                            if (this.anInt3285 == 0) {
                                i_15 = this.anInt3282 / codebook.dimensions;

                                for (int i_16 = 0; i_16 < i_15; i_16++) {
                                    float[] floats_20 = codebook.method4382();

                                    for (int i_18 = 0; i_18 < codebook.dimensions; i_18++) {
                                        output[i_13 + i_16 + i_18 * i_15] += floats_20[i_18];
                                    }
                                }
                            } else {
                                i_15 = 0;

                                while (i_15 < this.anInt3282) {
                                    float[] floats_19 = codebook.method4382();

                                    for (int i_17 = 0; i_17 < codebook.dimensions; i_17++) {
                                        output[i_13 + i_15] += floats_19[i_17];
                                        ++i_15;
                                    }
                                }
                            }
                        }

                        if (++pcount >= numWindows) {
                            break;
                        }
                    }
                }
            }
        }

    }

}
