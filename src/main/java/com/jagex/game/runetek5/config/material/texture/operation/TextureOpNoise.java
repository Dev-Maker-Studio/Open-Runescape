package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpNoise extends TextureOperation {

    public TextureOpNoise() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = TextureRaster.verticalTable[i_1];

            for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                ints_3[i_5] = method15428(TextureRaster.horizontalTable[i_5], i_4) % 4096;
            }
        }

        return ints_3;
    }

    public int method15428(int i_1, int i_2) {
        int i_4 = i_2 * 57 + i_1;
        i_4 ^= i_4 << 1;
        return 4096 - ((i_4 * i_4 * 15731 + 789221) * i_4 + 1376312589 & 0x7fffffff) / 262144;
    }

}
