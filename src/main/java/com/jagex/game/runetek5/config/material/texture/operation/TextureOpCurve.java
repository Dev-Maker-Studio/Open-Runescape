package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpCurve extends TextureOperation {

    public int[][] anIntArrayArray9800;
    public int[] anIntArray9798;
    public int[] anIntArray9796;
    public int anInt9799;
    public final short[] aShortArray9801 = new short[257];

    public TextureOpCurve() {
        super(1, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(0, i_1);

            for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                int i_6 = ints_4[i_5] >> 4;
                if (i_6 < 0) {
                    i_6 = 0;
                }

                if (i_6 > 256) {
                    i_6 = 256;
                }

                ints_3[i_5] = this.aShortArray9801[i_6];
            }
        }

        return ints_3;
    }

    public int[] method15257(int i_1) {
        return i_1 < 0 ? this.anIntArray9798 : (i_1 >= this.anIntArrayArray9800.length ? this.anIntArray9796 : this.anIntArrayArray9800[i_1]);
    }

    @Override
    public void postDecode() {
        if (this.anIntArrayArray9800 == null) {
            this.anIntArrayArray9800 = new int[][]{new int[2], {4096, 4096}};
        }

        if (this.anIntArrayArray9800.length < 2) {
            throw new RuntimeException();
        } else {
            if (this.anInt9799 == 2) {
                method15259();
            }

            TextureRaster.caculateTable();
            method15258();
        }
    }

    public void method15258() {
        int i_2;
        int i_3;
        int i_4;
        int[] ints_5;
        int[] ints_6;
        int i_7;
        int i_8;
        int i_9;
        int i_10;
        switch (this.anInt9799) {
            case 1:
                for (i_2 = 0; i_2 < 257; i_2++) {
                    i_3 = i_2 << 4;

                    for (i_4 = 1; i_4 < this.anIntArrayArray9800.length - 1 && this.anIntArrayArray9800[i_4][0] <= i_3; i_4++) {
                    }

                    ints_5 = this.anIntArrayArray9800[i_4 - 1];
                    ints_6 = this.anIntArrayArray9800[i_4];
                    i_7 = (i_3 - ints_5[0] << 12) / (ints_6[0] - ints_5[0]);
                    i_8 = 4096 - TextureRaster.PALETTE_COS[i_7 >> 5 & 0xff] >> 1;
                    i_9 = 4096 - i_8;
                    i_10 = i_8 * ints_6[1] + i_9 * ints_5[1] >> 12;
                    if (i_10 <= -32768) {
                        i_10 = -32767;
                    }

                    if (i_10 >= 32768) {
                        i_10 = 32767;
                    }

                    this.aShortArray9801[i_2] = (short) i_10;
                }

                return;
            case 2:
                for (i_2 = 0; i_2 < 257; i_2++) {
                    i_3 = i_2 << 4;

                    for (i_4 = 1; i_4 < this.anIntArrayArray9800.length - 1 && this.anIntArrayArray9800[i_4][0] <= i_3; i_4++) {
                    }

                    ints_5 = this.anIntArrayArray9800[i_4 - 1];
                    ints_6 = this.anIntArrayArray9800[i_4];
                    i_7 = method15257(i_4 - 2)[1];
                    i_8 = ints_5[1];
                    i_9 = ints_6[1];
                    i_10 = method15257(i_4 + 1)[1];
                    int i_11 = (i_3 - ints_5[0] << 12) / (ints_6[0] - ints_5[0]);
                    int i_12 = i_11 * i_11 >> 12;
                    int i_13 = i_8 + (i_10 - i_9 - i_7);
                    int i_14 = i_7 - i_8 - i_13;
                    int i_15 = i_9 - i_7;
                    int i_17 = i_12 * (i_13 * i_11 >> 12) >> 12;
                    int i_18 = i_14 * i_12 >> 12;
                    int i_19 = i_11 * i_15 >> 12;
                    int i_20 = i_17 + i_18 + i_19 + i_8;
                    if (i_20 <= -32768) {
                        i_20 = -32767;
                    }

                    if (i_20 >= 32768) {
                        i_20 = 32767;
                    }

                    this.aShortArray9801[i_2] = (short) i_20;
                }

                return;
            default:
                for (i_2 = 0; i_2 < 257; i_2++) {
                    i_3 = i_2 << 4;

                    for (i_4 = 1; i_4 < this.anIntArrayArray9800.length - 1 && this.anIntArrayArray9800[i_4][0] <= i_3; i_4++) {
                    }

                    ints_5 = this.anIntArrayArray9800[i_4 - 1];
                    ints_6 = this.anIntArrayArray9800[i_4];
                    i_7 = (i_3 - ints_5[0] << 12) / (ints_6[0] - ints_5[0]);
                    i_8 = 4096 - i_7;
                    i_9 = i_7 * ints_6[1] + i_8 * ints_5[1] >> 12;
                    if (i_9 <= -32768) {
                        i_9 = -32767;
                    }

                    if (i_9 >= 32768) {
                        i_9 = 32767;
                    }

                    this.aShortArray9801[i_2] = (short) i_9;
                }

        }
    }

    public void method15259() {
        int[] ints_2 = this.anIntArrayArray9800[0];
        int[] ints_3 = this.anIntArrayArray9800[1];
        int[] ints_4 = this.anIntArrayArray9800[this.anIntArrayArray9800.length - 2];
        int[] ints_5 = this.anIntArrayArray9800[this.anIntArrayArray9800.length - 1];
        this.anIntArray9798 = new int[]{ints_2[0] - ints_3[0] + ints_2[0], ints_2[1] - ints_3[1] + ints_2[1]};
        this.anIntArray9796 = new int[]{ints_4[0] - ints_5[0] + ints_4[0], ints_4[1] - ints_5[1] + ints_4[1]};
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        if (i_1 == 0) {
            this.anInt9799 = rsbytebuffer2.readUByte();
            this.anIntArrayArray9800 = new int[rsbytebuffer2.readUByte()][2];

            for (int i_4 = 0; i_4 < this.anIntArrayArray9800.length; i_4++) {
                this.anIntArrayArray9800[i_4][0] = rsbytebuffer2.readUShort();
                this.anIntArrayArray9800[i_4][1] = rsbytebuffer2.readUShort();
            }
        }

    }

}
