package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpMonochrome extends TextureOperation {

    public TextureOpMonochrome() {
        super(1, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[][] ints_4 = getChildColorOutput(0, i_1);
            int[] ints_5 = ints_4[0];
            int[] ints_6 = ints_4[1];
            int[] ints_7 = ints_4[2];
            for (int i_8 = 0; i_8 < TextureRaster.currentWidth; i_8++) {
                ints_3[i_8] = (ints_7[i_8] + ints_6[i_8] + ints_5[i_8]) / 3;
            }
        }
        return ints_3;
    }
}
