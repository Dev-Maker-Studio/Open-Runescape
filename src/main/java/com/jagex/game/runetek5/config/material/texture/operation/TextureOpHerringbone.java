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
public class TextureOpHerringbone extends TextureOperation {

    public int anInt9762 = 1;
    public int anInt9767 = 1;
    public int anInt9768 = 204;

    public TextureOpHerringbone() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            for (int i_4 = 0; i_4 < TextureRaster.currentWidth; i_4++) {
                int i_5 = TextureRaster.horizontalTable[i_4];
                int i_6 = TextureRaster.verticalTable[i_1];
                int i_7 = i_5 * this.anInt9762 >> 12;
                int i_8 = i_6 * this.anInt9767 >> 12;
                int i_9 = i_5 % (4096 / this.anInt9762) * this.anInt9762;
                int i_10 = i_6 % (4096 / this.anInt9767) * this.anInt9767;
                if (i_10 < this.anInt9768) {
                    for (i_7 -= i_8; i_7 < 0; i_7 += 4) {
                    }

                    while (i_7 > 3) {
                        i_7 -= 4;
                    }

                    if (i_7 != 1) {
                        ints_3[i_4] = 0;
                        continue;
                    }

                    if (i_9 < this.anInt9768) {
                        ints_3[i_4] = 0;
                        continue;
                    }
                }

                if (i_9 < this.anInt9768) {
                    for (i_7 -= i_8; i_7 < 0; i_7 += 4) {
                    }

                    while (i_7 > 3) {
                        i_7 -= 4;
                    }

                    if (i_7 > 0) {
                        ints_3[i_4] = 0;
                        continue;
                    }
                }

                ints_3[i_4] = 4096;
            }
        }

        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9762 = rsbytebuffer2.readUByte();
                break;
            case 1:
                this.anInt9767 = rsbytebuffer2.readUByte();
                break;
            case 2:
                this.anInt9768 = rsbytebuffer2.readUShort();
        }

    }

}
