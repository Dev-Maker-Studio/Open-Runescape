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
public class TextureOpColorEdgeDetector extends TextureOperation {

    public boolean aBool9974 = true;
    public int anInt9975 = 4096;

    public TextureOpColorEdgeDetector() {
        super(1, false);
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(0, i_1 - 1 & TextureRaster.columnEnd);
            int[] ints_5 = getChildMonochromeOutput(0, i_1);
            int[] ints_6 = getChildMonochromeOutput(0, i_1 + 1 & TextureRaster.columnEnd);
            int[] ints_7 = ints_3[0];
            int[] ints_8 = ints_3[1];
            int[] ints_9 = ints_3[2];
            for (int i_10 = 0; i_10 < TextureRaster.currentWidth; i_10++) {
                int i_11 = (ints_6[i_10] - ints_4[i_10]) * this.anInt9975;
                int i_12 = (ints_5[i_10 + 1 & TextureRaster.rowEnd] - ints_5[i_10 - 1 & TextureRaster.rowEnd]) * this.anInt9975;
                int i_13 = i_12 >> 12;
                int i_14 = i_11 >> 12;
                int i_15 = i_13 * i_13 >> 12;
                int i_16 = i_14 * i_14 >> 12;
                int i_17 = (int) (Math.sqrt((i_15 + i_16 + 4096) / 4096.0F) * 4096.0D);
                int i_18;
                int i_19;
                int i_20;
                if (i_17 != 0) {
                    i_18 = i_12 / i_17;
                    i_19 = i_11 / i_17;
                    i_20 = 16777216 / i_17;
                } else {
                    i_18 = 0;
                    i_19 = 0;
                    i_20 = 0;
                }
                if (this.aBool9974) {
                    i_18 = (i_18 >> 1) + 2048;
                    i_19 = (i_19 >> 1) + 2048;
                    i_20 = (i_20 >> 1) + 2048;
                }
                ints_7[i_10] = i_18;
                ints_8[i_10] = i_19;
                ints_9[i_10] = i_20;
            }
        }
        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9975 = rsbytebuffer2.readUnsignedShort();
                break;
            case 1:
                this.aBool9974 = rsbytebuffer2.readUnsignedByte() == 1;
        }
    }
}
