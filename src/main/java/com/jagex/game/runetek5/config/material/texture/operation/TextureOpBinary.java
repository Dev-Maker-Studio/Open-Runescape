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
public class TextureOpBinary extends TextureOperation {

    public int anInt9805;
    public int anInt9803 = 4096;

    public TextureOpBinary() {
        super(1, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[] ints_4 = getChildMonochromeOutput(0, i_1);

            for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                int i_6 = ints_4[i_5];
                ints_3[i_5] = i_6 >= this.anInt9805 && i_6 <= this.anInt9803 ? 4096 : 0;
            }
        }

        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9805 = rsbytebuffer2.readUShort();
                break;
            case 1:
                this.anInt9803 = rsbytebuffer2.readUShort();
        }

    }

}
