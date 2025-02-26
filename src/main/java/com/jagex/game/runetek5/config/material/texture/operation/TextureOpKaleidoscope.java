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
public class TextureOpKaleidoscope extends TextureOperation {

    public static int anInt9873;

    public static int anInt9872;

    public TextureOpKaleidoscope() {
        super(1, false);
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        if (i_1 == 0) {
            super.monochrome = rsbytebuffer2.readUByte() == 1;
        }
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int[] ints_4 = ints_3[0];
            int[] ints_5 = ints_3[1];
            int[] ints_6 = ints_3[2];
            for (int i_7 = 0; i_7 < TextureRaster.currentWidth; i_7++) {
                method15383(i_7, i_1);
                int[][] ints_8 = getChildColorOutput(0, anInt9872);
                ints_4[i_7] = ints_8[0][anInt9873];
                ints_5[i_7] = ints_8[1][anInt9873];
                ints_6[i_7] = ints_8[2][anInt9873];
            }
        }
        return ints_3;
    }

    public void method15383(int i_1, int i_2) {
        int i_4 = TextureRaster.horizontalTable[i_1];
        int i_5 = TextureRaster.verticalTable[i_2];
        float f_6 = (float) Math.atan2(i_4 - 2048, i_5 - 2048);
        if (f_6 >= -3.141592653589793 && f_6 <= -2.356194490192345) {
            anInt9873 = i_1;
            anInt9872 = i_2;
        } else if (f_6 <= -1.5707963267948966 && f_6 >= -2.356194490192345) {
            anInt9873 = i_2;
            anInt9872 = i_1;
        } else if (f_6 <= -0.7853981633974483 && f_6 >= -1.5707963267948966) {
            anInt9873 = TextureRaster.currentWidth - i_2;
            anInt9872 = i_1;
        } else if (f_6 <= 0.0F && f_6 >= -0.7853981633974483) {
            anInt9873 = i_1;
            anInt9872 = TextureRaster.currentHeight - i_2;
        } else if (f_6 >= 0.0F && f_6 <= 0.7853981633974483D) {
            anInt9873 = TextureRaster.currentWidth - i_1;
            anInt9872 = TextureRaster.currentHeight - i_2;
        } else if (f_6 >= 0.7853981633974483D && f_6 <= 1.5707963267948966D) {
            anInt9873 = TextureRaster.currentWidth - i_2;
            anInt9872 = TextureRaster.currentHeight - i_1;
        } else if (f_6 >= 1.5707963267948966D && f_6 <= 2.356194490192345D) {
            anInt9873 = i_2;
            anInt9872 = TextureRaster.currentHeight - i_1;
        } else if (f_6 >= 2.356194490192345D && f_6 <= Math.PI) {
            anInt9873 = TextureRaster.currentWidth - i_1;
            anInt9872 = i_2;
        }
        anInt9873 &= TextureRaster.rowEnd;
        anInt9872 &= TextureRaster.columnEnd;
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            for (int i_4 = 0; i_4 < TextureRaster.currentWidth; i_4++) {
                method15383(i_4, i_1);
                int[] ints_5 = getChildMonochromeOutput(0, anInt9872);
                ints_3[i_4] = ints_5[anInt9873];
            }
        }
        return ints_3;
    }
}
