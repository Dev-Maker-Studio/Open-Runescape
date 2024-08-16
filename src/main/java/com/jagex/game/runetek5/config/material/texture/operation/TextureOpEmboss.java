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
public class TextureOpEmboss extends TextureOperation {

    public static final byte[] aByteArray9814;

    static {
        int i_0 = 0;
        aByteArray9814 = new byte[32896];

        for (int i_1 = 0; i_1 < 256; i_1++) {
            for (int i_2 = 0; i_2 <= i_1; i_2++) {
                aByteArray9814[i_0++] = (byte) ((int) (255.0D / Math.sqrt((i_2 * i_2 + i_1 * i_1 + 65535) / 65535.0F)));
            }
        }

    }

    public int anInt9810 = 4096;
    public int anInt9813 = 3216;
    public int anInt9807 = 3216;
    public final int[] anIntArray9808 = new int[3];

    public TextureOpEmboss() {
        super(1, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = this.anInt9810 * TextureRaster.anInt3678 >> 12;
            int[] ints_5 = getChildMonochromeOutput(0, i_1 - 1 & TextureRaster.columnEnd);
            int[] ints_6 = getChildMonochromeOutput(0, i_1);
            int[] ints_7 = getChildMonochromeOutput(0, i_1 + 1 & TextureRaster.columnEnd);

            for (int i_8 = 0; i_8 < TextureRaster.currentWidth; i_8++) {
                int i_9 = (ints_7[i_8] - ints_5[i_8]) * i_4 >> 12;
                int i_10 = i_4 * (ints_6[i_8 - 1 & TextureRaster.rowEnd] - ints_6[i_8 + 1 & TextureRaster.rowEnd]) >> 12;
                int i_11 = i_10 >> 4;
                int i_12 = i_9 >> 4;
                if (i_11 < 0) {
                    i_11 = -i_11;
                }

                if (i_11 > 255) {
                    i_11 = 255;
                }

                if (i_12 < 0) {
                    i_12 = -i_12;
                }

                if (i_12 > 255) {
                    i_12 = 255;
                }

                int i_13 = aByteArray9814[i_11 + (i_12 * (i_12 + 1) >> 1)] & 0xff;
                int i_14 = i_10 * i_13 >> 8;
                int i_15 = i_9 * i_13 >> 8;
                int i_16 = i_13 * 4096 >> 8;
                i_14 = i_14 * this.anIntArray9808[0] >> 12;
                i_15 = this.anIntArray9808[1] * i_15 >> 12;
                i_16 = i_16 * this.anIntArray9808[2] >> 12;
                ints_3[i_8] = i_16 + i_15 + i_14;
            }
        }

        return ints_3;
    }

    @Override
    public void postDecode() {
        method15266();
    }

    public void method15266() {
        double d_2 = Math.cos(this.anInt9807 / 4096.0F);
        this.anIntArray9808[0] = (int) (Math.sin(this.anInt9813 / 4096.0F) * d_2 * 4096.0D);
        this.anIntArray9808[1] = (int) (Math.cos(this.anInt9813 / 4096.0F) * d_2 * 4096.0D);
        this.anIntArray9808[2] = (int) (Math.sin(this.anInt9807 / 4096.0F) * 4096.0D);
        int i_4 = this.anIntArray9808[0] * this.anIntArray9808[0] >> 12;
        int i_5 = this.anIntArray9808[1] * this.anIntArray9808[1] >> 12;
        int i_6 = this.anIntArray9808[2] * this.anIntArray9808[2] >> 12;
        int i_7 = (int) (Math.sqrt(i_4 + i_5 + i_6 >> 12) * 4096.0D);
        if (i_7 != 0) {
            this.anIntArray9808[0] = (this.anIntArray9808[0] << 12) / i_7;
            this.anIntArray9808[1] = (this.anIntArray9808[1] << 12) / i_7;
            this.anIntArray9808[2] = (this.anIntArray9808[2] << 12) / i_7;
        }

    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9810 = rsbytebuffer2.readUnsignedShort();
                break;
            case 1:
                this.anInt9813 = rsbytebuffer2.readUnsignedShort();
                break;
            case 2:
                this.anInt9807 = rsbytebuffer2.readUnsignedShort();
        }

    }

}
