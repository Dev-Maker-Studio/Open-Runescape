package com.jagex.game.runetek5.config.midi.instrument;

import com.jagex.game.runetek5.utils.BufferUtils;
import com.jagex.game.runetek5.utils.MathUtils;

/**
 * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
 * @author Brainpower
 *
 */
public class MusicSampleCodebook {

    final int dimensions;
    public final int entries;
    public final int[] lengths;
    public int[] mults;
    public float[][] multiplicands;
    public int[] anIntArray3151;

    public MusicSampleCodebook() {
        MusicSample.getBits(24);
        this.dimensions = MusicSample.getBits(16);
        this.entries = MusicSample.getBits(24);
        this.lengths = new int[this.entries];
        boolean ordered = MusicSample.getBit() != 0;
        int lookupType;
        if (ordered) {
            lookupType = 0;
            for (int currentLength = MusicSample.getBits(5) + 1; lookupType < this.entries; currentLength++) {
                int limit = MusicSample.getBits(BufferUtils.ilog(this.entries - lookupType));
                for (int i_5 = 0; i_5 < limit; i_5++) {
                    this.lengths[lookupType++] = currentLength;
                }
            }
        } else {
            boolean present = MusicSample.getBit() != 0;
            for (int j = 0; j < this.entries; j++) {
                if (present && MusicSample.getBit() == 0) {
                    this.lengths[j] = 0;
                } else {
                    this.lengths[j] = MusicSample.getBits(5) + 1;
                }
            }
        }

        computeAcceleratedHuffman();
        
        lookupType = MusicSample.getBits(4);
        if (lookupType > 0) {
            float minimumValue = MusicSample.float32Unpack(MusicSample.getBits(32));
            float deltaValue = MusicSample.float32Unpack(MusicSample.getBits(32));
            int valueBits = MusicSample.getBits(4) + 1;
            boolean sequenceP = MusicSample.getBit() != 0;
            int lookupValues;
            if (lookupType == 1) {
                lookupValues = lookup1Values(this.entries, this.dimensions);
            } else {
                lookupValues = this.entries * this.dimensions;
            }

            this.mults = new int[lookupValues];
            for (int j = 0; j < lookupValues; j++) {
                this.mults[j] = MusicSample.getBits(valueBits);
            }

            this.multiplicands = new float[this.entries][this.dimensions];
            if (lookupType == 1) {
                for (int i = 0; i < this.entries; i++) {
                    float last = 0.0F;
                    int div = 1;

                    for (int j = 0; j < this.dimensions; j++) {
                        int off = i / div % lookupValues;
                        float val = this.mults[off] * deltaValue + minimumValue + last;
                        this.multiplicands[i][j] = val;
                        if (sequenceP) {
                            last = val;
                        }
                        div *= lookupValues;
                    }
                }
            } else {
                for (int i = 0; i < this.entries; i++) {
                    float last = 0.0F;
                    int off = i * this.dimensions;

                    for (int j = 0; j < this.dimensions; j++) {
                        float val = this.mults[off] * deltaValue + minimumValue + last;
                        this.multiplicands[i][j] = val;
                        if (sequenceP) {
                            last = val;
                        }
                        off++;
                    }
                }
            }
        }

    }

    public static int lookup1Values(int entries, int dim) {
        int r;
        for (r = (int) Math.pow(entries, 1.0D / dim) + 1; MathUtils.power(r, dim) > entries; --r) { }
        return r;
    }

    float[] method4382() {
        return this.multiplicands[decode()];
    }

    //buildhuffmantree
    public void computeAcceleratedHuffman() {
        int[] ints_1 = new int[this.entries];
        int[] ints_2 = new int[33];

        int i_3;
        int i_4;
        int i_5;
        int i_6;
        int i_7;
        int i_8;
        int i_9;
        for (i_3 = 0; i_3 < this.entries; i_3++) {
            i_4 = this.lengths[i_3];
            if (i_4 != 0) {
                i_5 = 1 << 32 - i_4;
                i_6 = ints_2[i_4];
                ints_1[i_3] = i_6;
                if ((i_6 & i_5) != 0) {
                    i_7 = ints_2[i_4 - 1];
                } else {
                    i_7 = i_6 | i_5;

                    for (i_8 = i_4 - 1; i_8 >= 1; --i_8) {
                        i_9 = ints_2[i_8];
                        if (i_9 != i_6) {
                            break;
                        }

                        int i_12 = 1 << 32 - i_8;
                        if ((i_9 & i_12) != 0) {
                            ints_2[i_8] = ints_2[i_8 - 1];
                            break;
                        }

                        ints_2[i_8] = i_9 | i_12;
                    }
                }

                ints_2[i_4] = i_7;

                for (i_8 = i_4 + 1; i_8 <= 32; i_8++) {
                    i_9 = ints_2[i_8];
                    if (i_9 == i_6) {
                        ints_2[i_8] = i_7;
                    }
                }
            }
        }

        this.anIntArray3151 = new int[8];
        i_3 = 0;

        for (i_4 = 0; i_4 < this.entries; i_4++) {
            i_5 = this.lengths[i_4];
            if (i_5 != 0) {
                i_6 = ints_1[i_4];
                i_7 = 0;

                for (i_8 = 0; i_8 < i_5; i_8++) {
                    i_9 = Integer.MIN_VALUE >>> i_8;
                    if ((i_6 & i_9) != 0) {
                        if (this.anIntArray3151[i_7] == 0) {
                            this.anIntArray3151[i_7] = i_3;
                        }

                        i_7 = this.anIntArray3151[i_7];
                    } else {
                        ++i_7;
                    }

                    if (i_7 >= this.anIntArray3151.length) {
                        int[] ints_10 = new int[this.anIntArray3151.length * 2];

                        System.arraycopy(this.anIntArray3151, 0, ints_10, 0, this.anIntArray3151.length);

                        this.anIntArray3151 = ints_10;
                    }

                    i_9 >>>= 1;
                }

                this.anIntArray3151[i_7] = ~i_4;
                if (i_7 >= i_3) {
                    i_3 = i_7 + 1;
                }
            }
        }

    }

    int decode() {
        int i_1;
        for (i_1 = 0; this.anIntArray3151[i_1] >= 0; i_1 = MusicSample.getBit() != 0 ? this.anIntArray3151[i_1] : i_1 + 1) {
        }

        return ~this.anIntArray3151[i_1];
    }

}
