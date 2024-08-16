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
public class TextureOpWaveform extends TextureOperation {

    public int anInt9682;
    public int anInt9684;
    public int anInt9680 = 1;

    public TextureOpWaveform() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = TextureRaster.verticalTable[i_1];
            int i_5 = i_4 - 2048 >> 1;

            for (int i_6 = 0; i_6 < TextureRaster.currentWidth; i_6++) {
                int i_7 = TextureRaster.horizontalTable[i_6];
                int i_8 = i_7 - 2048 >> 1;
                int i_9;
                if (this.anInt9682 == 0) {
                    i_9 = (i_7 - i_4) * this.anInt9680;
                } else {
                    int i_10 = i_5 * i_5 + i_8 * i_8 >> 12;
                    i_9 = (int) (Math.sqrt(i_10 / 4096.0F) * 4096.0D);
                    i_9 = (int) ((i_9 * this.anInt9680) * Math.PI);
                }

                i_9 -= i_9 & -4096;
                if (this.anInt9684 == 0) {
                    i_9 = TextureRaster.PALETTE_SIN[i_9 >> 4 & 0xff] + 4096 >> 1;
                } else if (this.anInt9684 == 2) {
                    i_9 -= 2048;
                    if (i_9 < 0) {
                        i_9 = -i_9;
                    }

                    i_9 = 2048 - i_9 << 1;
                }

                ints_3[i_6] = i_9;
            }
        }

        return ints_3;
    }

    @Override
    public void postDecode() {
        TextureRaster.caculateTable();
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9682 = rsbytebuffer2.readUnsignedByte();
                break;
            case 1:
                this.anInt9684 = rsbytebuffer2.readUnsignedByte();
            case 2:
            default:
                break;
            case 3:
                this.anInt9680 = rsbytebuffer2.readUnsignedByte();
        }

    }

}
