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
public class TextureOpTile extends TextureOperation {

    public int anInt9673 = 4;
    public int anInt9674 = 4;

    public TextureOpTile() {
        super(1, false);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = TextureRaster.currentWidth / this.anInt9673;
            int i_5 = TextureRaster.currentHeight / this.anInt9674;
            int[] ints_6;
            int i_7;
            if (i_5 > 0) {
                i_7 = i_1 % i_5;
                ints_6 = getChildMonochromeOutput(0, i_7 * TextureRaster.currentHeight / i_5);
            } else {
                ints_6 = getChildMonochromeOutput(0, 0);
            }

            for (i_7 = 0; i_7 < TextureRaster.currentWidth; i_7++) {
                if (i_4 > 0) {
                    int i_8 = i_7 % i_4;
                    ints_3[i_7] = ints_6[i_8 * TextureRaster.currentWidth / i_4];
                } else {
                    ints_3[i_7] = ints_6[0];
                }
            }
        }

        return ints_3;
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int i_4 = TextureRaster.currentWidth / this.anInt9673;
            int i_5 = TextureRaster.currentHeight / this.anInt9674;
            int[][] ints_6;
            if (i_5 > 0) {
                int i_7 = i_1 % i_5;
                ints_6 = getChildColorOutput(0, i_7 * TextureRaster.currentHeight / i_5);
            } else {
                ints_6 = getChildColorOutput(0, 0);
            }

            int[] ints_16 = ints_6[0];
            int[] ints_8 = ints_6[1];
            int[] ints_9 = ints_6[2];
            int[] ints_10 = ints_3[0];
            int[] ints_11 = ints_3[1];
            int[] ints_12 = ints_3[2];

            for (int i_13 = 0; i_13 < TextureRaster.currentWidth; i_13++) {
                int i_14;
                if (i_4 > 0) {
                    int i_15 = i_13 % i_4;
                    i_14 = i_15 * TextureRaster.currentWidth / i_4;
                } else {
                    i_14 = 0;
                }

                ints_10[i_13] = ints_16[i_14];
                ints_11[i_13] = ints_8[i_14];
                ints_12[i_13] = ints_9[i_14];
            }
        }

        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9673 = rsbytebuffer2.readUByte();
                break;
            case 1:
                this.anInt9674 = rsbytebuffer2.readUByte();
        }

    }

}
