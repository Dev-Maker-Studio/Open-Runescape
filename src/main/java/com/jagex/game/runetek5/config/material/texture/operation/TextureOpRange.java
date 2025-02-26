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
public class TextureOpRange extends TextureOperation {

    public int anInt9761 = 2048;
    public int anInt9759 = 1024;
    public int anInt9760 = 3072;

    public TextureOpRange() {
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
                ints_8[i_11] = (this.anInt9761 * ints_5[i_11] >> 12) + this.anInt9759;
                ints_9[i_11] = (this.anInt9761 * ints_6[i_11] >> 12) + this.anInt9759;
                ints_10[i_11] = (this.anInt9761 * ints_7[i_11] >> 12) + this.anInt9759;
            }
        }

        return ints_3;
    }

    @Override
    public void postDecode() {
        this.anInt9761 = this.anInt9760 - this.anInt9759;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9759 = rsbytebuffer2.readUShort();
                break;
            case 1:
                this.anInt9760 = rsbytebuffer2.readUShort();
                break;
            case 2:
                super.monochrome = rsbytebuffer2.readUByte() == 1;
        }

    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(0, i_1);

            for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                ints_3[i_5] = (this.anInt9761 * ints_4[i_5] >> 12) + this.anInt9759;
            }
        }

        return ints_3;
    }

}
