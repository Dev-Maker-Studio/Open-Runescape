package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.config.material.texture.TextureRaster;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpTiledSprite extends TextureOpSprite {

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty && method15398()) {
            int[] ints_4 = ints_3[0];
            int[] ints_5 = ints_3[1];
            int[] ints_6 = ints_3[2];
            int i_7 = i_1 % super.anInt9915 * super.anInt9915;

            for (int i_8 = 0; i_8 < TextureRaster.currentWidth; i_8++) {
                int i_9 = super.anIntArray9916[i_8 % super.anInt9914 + i_7];
                ints_6[i_8] = (i_9 & 0xff) << 4;
                ints_5[i_8] = (i_9 & 0xff00) >> 4;
                ints_4[i_8] = (i_9 & 0xff0000) >> 12;
            }
        }

        return ints_3;
    }

}
