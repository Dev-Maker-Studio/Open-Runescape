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
public class TextureOpInterpolate extends TextureOperation {

    public TextureOpInterpolate() {
        super(3, false);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(0, i_1);
            int[] ints_5 = getChildMonochromeOutput(1, i_1);
            int[] ints_6 = getChildMonochromeOutput(2, i_1);

            for (int i_7 = 0; i_7 < TextureRaster.currentWidth; i_7++) {
                int i_8 = ints_6[i_7];
                if (i_8 == 4096) {
                    ints_3[i_7] = ints_4[i_7];
                } else if (i_8 == 0) {
                    ints_3[i_7] = ints_5[i_7];
                } else {
                    ints_3[i_7] = (4096 - i_8) * ints_5[i_7] + i_8 * ints_4[i_7] >> 12;
                }
            }
        }

        return ints_3;
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(2, i_1);
            int[][] ints_5 = getChildColorOutput(0, i_1);
            int[][] ints_6 = getChildColorOutput(1, i_1);
            int[] ints_7 = ints_3[0];
            int[] ints_8 = ints_3[1];
            int[] ints_9 = ints_3[2];
            int[] ints_10 = ints_5[0];
            int[] ints_11 = ints_5[1];
            int[] ints_12 = ints_5[2];
            int[] ints_13 = ints_6[0];
            int[] ints_14 = ints_6[1];
            int[] ints_15 = ints_6[2];

            for (int i_16 = 0; i_16 < TextureRaster.currentWidth; i_16++) {
                int i_17 = ints_4[i_16];
                if (i_17 == 4096) {
                    ints_7[i_16] = ints_10[i_16];
                    ints_8[i_16] = ints_11[i_16];
                    ints_9[i_16] = ints_12[i_16];
                } else if (i_17 == 0) {
                    ints_7[i_16] = ints_13[i_16];
                    ints_8[i_16] = ints_14[i_16];
                    ints_9[i_16] = ints_15[i_16];
                } else {
                    int i_18 = 4096 - i_17;
                    ints_7[i_16] = i_17 * ints_10[i_16] + i_18 * ints_13[i_16] >> 12;
                    ints_8[i_16] = i_17 * ints_11[i_16] + i_18 * ints_14[i_16] >> 12;
                    ints_9[i_16] = i_17 * ints_12[i_16] + i_18 * ints_15[i_16] >> 12;
                }
            }
        }

        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        if (i_1 == 0) {
            super.monochrome = rsbytebuffer2.readUByte() == 1;
        }

    }

}
