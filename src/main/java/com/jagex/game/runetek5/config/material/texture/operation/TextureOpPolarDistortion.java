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
public class TextureOpPolarDistortion extends TextureOperation {

    public int anInt9836 = 32768;

    public TextureOpPolarDistortion() {
        super(3, false);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(1, i_1);
            int[] ints_5 = getChildMonochromeOutput(2, i_1);
            for (int i_6 = 0; i_6 < TextureRaster.currentWidth; i_6++) {
                int i_7 = ints_4[i_6] >> 4 & 0xff;
                int i_8 = this.anInt9836 * ints_5[i_6] >> 12;
                int i_9 = TextureRaster.PALETTE_COS[i_7] * i_8 >> 12;
                int i_10 = TextureRaster.PALETTE_SIN[i_7] * i_8 >> 12;
                int i_11 = i_6 + (i_9 >> 12) & TextureRaster.rowEnd;
                int i_12 = (i_10 >> 12) + i_1 & TextureRaster.columnEnd;
                int[] ints_13 = getChildMonochromeOutput(0, i_12);
                ints_3[i_6] = ints_13[i_11];
            }
        }
        return ints_3;
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(1, i_1);
            int[] ints_5 = getChildMonochromeOutput(2, i_1);
            int[] ints_6 = ints_3[0];
            int[] ints_7 = ints_3[1];
            int[] ints_8 = ints_3[2];
            for (int i_9 = 0; i_9 < TextureRaster.currentWidth; i_9++) {
                int i_10 = ints_4[i_9] * 255 >> 12 & 0xff;
                int i_11 = this.anInt9836 * ints_5[i_9] >> 12;
                int i_12 = TextureRaster.PALETTE_COS[i_10] * i_11 >> 12;
                int i_13 = TextureRaster.PALETTE_SIN[i_10] * i_11 >> 12;
                int i_14 = i_9 + (i_12 >> 12) & TextureRaster.rowEnd;
                int i_15 = (i_13 >> 12) + i_1 & TextureRaster.columnEnd;
                int[][] ints_16 = getChildColorOutput(0, i_15);
                ints_6[i_9] = ints_16[0][i_14];
                ints_7[i_9] = ints_16[1][i_14];
                ints_8[i_9] = ints_16[2][i_14];
            }
        }
        return ints_3;
    }

    @Override
    public void postDecode() {
        TextureRaster.caculateTable();
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9836 = rsbytebuffer2.readUShort() << 4;
                break;
            case 1:
                super.monochrome = rsbytebuffer2.readUByte() == 1;
        }
    }
}
