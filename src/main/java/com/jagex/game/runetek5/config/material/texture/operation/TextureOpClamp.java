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
public class TextureOpClamp extends TextureOperation {

    public int anInt9864;
    public int anInt9868 = 4096;

    public TextureOpClamp() {
        super(1, false);
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int[][] ints_4 = getChildColorOutput(0, i_1);
            int[] ints_5 = ints_4[0];
            int[] ints_6 = ints_4[1];
            int[] ints_7 = ints_4[2];
            int[] ints_8 = ints_3[0];
            int[] ints_9 = ints_3[1];
            int[] ints_10 = ints_3[2];

            for (int i_11 = 0; i_11 < TextureRaster.currentWidth; i_11++) {
                int i_12 = ints_5[i_11];
                int i_13 = ints_6[i_11];
                int i_14 = ints_7[i_11];
                if (i_12 < this.anInt9864) {
                    ints_8[i_11] = this.anInt9864;
                } else ints_8[i_11] = Math.min(i_12, this.anInt9868);

                if (i_13 < this.anInt9864) {
                    ints_9[i_11] = this.anInt9864;
                } else ints_9[i_11] = Math.min(i_13, this.anInt9868);

                if (i_14 < this.anInt9864) {
                    ints_10[i_11] = this.anInt9864;
                } else ints_10[i_11] = Math.min(i_14, this.anInt9868);
            }
        }

        return ints_3;
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(0, i_1);

            for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                int i_6 = ints_4[i_5];
                if (i_6 < this.anInt9864) {
                    ints_3[i_5] = this.anInt9864;
                } else ints_3[i_5] = Math.min(i_6, this.anInt9868);
            }
        }

        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9864 = rsbytebuffer2.readUnsignedShort();
                break;
            case 1:
                this.anInt9868 = rsbytebuffer2.readUnsignedShort();
                break;
            case 2:
                super.monochrome = rsbytebuffer2.readUnsignedByte() == 1;
        }

    }

}
