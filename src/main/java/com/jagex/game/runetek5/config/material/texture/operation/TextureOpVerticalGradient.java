package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.utils.ArrayUtils;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpVerticalGradient extends TextureOperation {

    public TextureOpVerticalGradient() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            ArrayUtils.setVariables(ints_3, 0, TextureRaster.currentWidth, TextureRaster.verticalTable[i_1]);
        }
        return ints_3;
    }
}
