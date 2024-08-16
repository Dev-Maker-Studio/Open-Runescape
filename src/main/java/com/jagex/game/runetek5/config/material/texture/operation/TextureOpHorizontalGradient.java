package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpHorizontalGradient extends TextureOperation {

    public TextureOpHorizontalGradient() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        return TextureRaster.horizontalTable;
    }

}
