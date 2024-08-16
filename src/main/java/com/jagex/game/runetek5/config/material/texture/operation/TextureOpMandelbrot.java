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
public class TextureOpMandelbrot extends TextureOperation {

    public int anInt9741;
    public int anInt9740;
    public int anInt9742 = 1365;
    public int anInt9743 = 20;

    public TextureOpMandelbrot() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            for (int i_4 = 0; i_4 < TextureRaster.currentWidth; i_4++) {
                int i_5 = (TextureRaster.horizontalTable[i_4] << 12) / this.anInt9742 + this.anInt9741;
                int i_6 = (TextureRaster.verticalTable[i_1] << 12) / this.anInt9742 + this.anInt9740;
                int i_7 = i_5;
                int i_8 = i_6;
                int i_9 = i_5;
                int i_10 = i_6;
                int i_11 = i_5 * i_5 >> 12;
                int i_12 = i_6 * i_6 >> 12;

                int i_13;
                for (i_13 = 0; i_11 + i_12 < 16384 && i_13 < this.anInt9743; i_13++) {
                    i_10 = (i_9 * i_10 >> 12) * 2 + i_8;
                    i_9 = i_7 + (i_11 - i_12);
                    i_11 = i_9 * i_9 >> 12;
                    i_12 = i_10 * i_10 >> 12;
                }

                ints_3[i_4] = i_13 < this.anInt9743 - 1 ? (i_13 << 12) / this.anInt9743 : 0;
            }
        }

        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9742 = rsbytebuffer2.readUnsignedShort();
                break;
            case 1:
                this.anInt9743 = rsbytebuffer2.readUnsignedShort();
                break;
            case 2:
                this.anInt9741 = rsbytebuffer2.readUnsignedShort();
                break;
            case 3:
                this.anInt9740 = rsbytebuffer2.readUnsignedShort();
        }

    }

}
