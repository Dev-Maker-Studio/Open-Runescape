package com.jagex.game.runetek5.config.material.texture.operation;

import java.util.Random;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpLineNoise extends TextureOperation {

    public int anInt10009;
    public int anInt10013 = 2000;
    public int anInt10010 = 16;
    public int anInt10004;
    public int anInt10011 = 4096;

    public TextureOpLineNoise() {
        super(0, true);
    }

    @Override
    public void postDecode() {
        TextureRaster.caculateTable();
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt10009 = rsbytebuffer2.readUByte();
                break;
            case 1:
                this.anInt10013 = rsbytebuffer2.readUShort();
                break;
            case 2:
                this.anInt10010 = rsbytebuffer2.readUByte();
                break;
            case 3:
                this.anInt10004 = rsbytebuffer2.readUShort();
                break;
            case 4:
                this.anInt10011 = rsbytebuffer2.readUShort();
        }

    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = this.anInt10011 >> 1;
            int[][] ints_5 = super.monochromeImageCache.method5722();
            Random random6 = new Random(this.anInt10009);

            for (int i_7 = 0; i_7 < this.anInt10013; i_7++) {
                int i_8 = this.anInt10011 > 0 ? this.anInt10004 + (StaticMethods.method7931(random6, this.anInt10011) - i_4) : this.anInt10004;
                i_8 = i_8 >> 4 & 0xff;
                int i_9 = StaticMethods.method7931(random6, TextureRaster.currentWidth);
                int i_10 = StaticMethods.method7931(random6, TextureRaster.currentHeight);
                int i_11 = i_9 + (TextureRaster.PALETTE_COS[i_8] * this.anInt10010 >> 12);
                int i_12 = i_10 + (TextureRaster.PALETTE_SIN[i_8] * this.anInt10010 >> 12);
                int i_13 = i_12 - i_10;
                int i_14 = i_11 - i_9;
                if (i_14 != 0 || i_13 != 0) {
                    if (i_13 < 0) {
                        i_13 = -i_13;
                    }

                    if (i_14 < 0) {
                        i_14 = -i_14;
                    }

                    boolean bool15 = i_13 > i_14;
                    int i_16;
                    int i_17;
                    if (bool15) {
                        i_16 = i_9;
                        i_17 = i_11;
                        i_9 = i_10;
                        i_10 = i_16;
                        i_11 = i_12;
                        i_12 = i_17;
                    }

                    if (i_9 > i_11) {
                        i_16 = i_9;
                        i_17 = i_10;
                        i_9 = i_11;
                        i_11 = i_16;
                        i_10 = i_12;
                        i_12 = i_17;
                    }

                    i_16 = i_10;
                    i_17 = i_11 - i_9;
                    int i_18 = i_12 - i_10;
                    int i_19 = -i_17 / 2;
                    int i_20 = 2048 / i_17;
                    int i_21 = 1024 - (StaticMethods.method7931(random6, 4096) >> 2);
                    int i_22 = i_10 < i_12 ? 1 : -1;
                    if (i_18 < 0) {
                        i_18 = -i_18;
                    }

                    for (int i_23 = i_9; i_23 < i_11; i_23++) {
                        int i_24 = i_21 + i_20 * (i_23 - i_9) + 1024;
                        int i_25 = i_23 & TextureRaster.rowEnd;
                        int i_26 = i_16 & TextureRaster.columnEnd;
                        if (bool15) {
                            ints_5[i_26][i_25] = i_24;
                        } else {
                            ints_5[i_25][i_26] = i_24;
                        }

                        i_19 += i_18;
                        if (i_19 > 0) {
                            i_19 -= i_17;
                            i_16 += i_22;
                        }
                    }
                }
            }
        }

        return ints_3;
    }

}
