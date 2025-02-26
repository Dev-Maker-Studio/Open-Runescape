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
public class TextureOpMonochromeEdgeDetector extends TextureOperation {

    public int anInt9886 = 4096;

    public TextureOpMonochromeEdgeDetector() {
        super(1, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(0, i_1 - 1 & TextureRaster.columnEnd);
            int[] ints_5 = getChildMonochromeOutput(0, i_1);
            int[] ints_6 = getChildMonochromeOutput(0, i_1 + 1 & TextureRaster.columnEnd);

            for (int i_7 = 0; i_7 < TextureRaster.currentWidth; i_7++) {
                int i_8 = (ints_6[i_7] - ints_4[i_7]) * this.anInt9886;
                int i_9 = (ints_5[i_7 + 1 & TextureRaster.rowEnd] - ints_5[i_7 - 1 & TextureRaster.rowEnd]) * this.anInt9886;
                int i_10 = i_9 >> 12;
                int i_11 = i_8 >> 12;
                int i_12 = i_10 * i_10 >> 12;
                int i_13 = i_11 * i_11 >> 12;
                int i_14 = (int) (Math.sqrt((i_13 + i_12 + 4096) / 4096.0F) * 4096.0D);
                int i_15 = i_14 != 0 ? 16777216 / i_14 : 0;
                ints_3[i_7] = 4096 - i_15;
            }
        }

        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        if (i_1 == 0) {
            this.anInt9886 = rsbytebuffer2.readUShort();
        }

    }

}
