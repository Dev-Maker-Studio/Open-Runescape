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
public class TextureOpWeave extends TextureOperation {

    public int anInt9885 = 585;

    public TextureOpWeave() {
        super(0, true);
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9885 = rsbytebuffer2.readUShort();
            default:
        }
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = TextureRaster.verticalTable[i_1];
            for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                int i_6 = TextureRaster.horizontalTable[i_5];
                int i_7;
                if (i_6 > this.anInt9885 && i_6 < 4096 - this.anInt9885 && i_4 > 2048 - this.anInt9885 && i_4 < this.anInt9885 + 2048) {
                    i_7 = 2048 - i_6;
                    i_7 = i_7 < 0 ? -i_7 : i_7;
                    i_7 <<= 12;
                    i_7 /= 2048 - this.anInt9885;
                    ints_3[i_5] = 4096 - i_7;
                } else if (i_6 > 2048 - this.anInt9885 && i_6 < this.anInt9885 + 2048) {
                    i_7 = i_4 - 2048;
                    i_7 = i_7 < 0 ? -i_7 : i_7;
                    i_7 -= this.anInt9885;
                    i_7 <<= 12;
                    ints_3[i_5] = i_7 / (2048 - this.anInt9885);
                } else if (i_4 >= this.anInt9885 && i_4 <= 4096 - this.anInt9885) {
                    if (i_6 >= this.anInt9885 && i_6 <= 4096 - this.anInt9885) {
                        ints_3[i_5] = 0;
                    } else {
                        i_7 = 2048 - i_4;
                        i_7 = i_7 < 0 ? -i_7 : i_7;
                        i_7 <<= 12;
                        i_7 /= 2048 - this.anInt9885;
                        ints_3[i_5] = 4096 - i_7;
                    }
                } else {
                    i_7 = i_6 - 2048;
                    i_7 = i_7 < 0 ? -i_7 : i_7;
                    i_7 -= this.anInt9885;
                    i_7 <<= 12;
                    ints_3[i_5] = i_7 / (2048 - this.anInt9885);
                }
            }
        }
        return ints_3;
    }
}
