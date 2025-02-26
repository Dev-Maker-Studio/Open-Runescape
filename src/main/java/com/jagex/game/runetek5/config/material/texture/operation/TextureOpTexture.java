package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.config.material.texture.TextureTypeList;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpTexture extends TextureOperation {

    public int[] anIntArray9881;
    public int anInt9883;
    public int anInt9880;
    public int anInt9882 = -1;

    public TextureOpTexture() {
        super(0, false);
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int i_4 = (this.anInt9883 != TextureRaster.currentHeight ? this.anInt9883 * i_1 / TextureRaster.currentHeight : i_1) * this.anInt9880;
            int[] ints_5 = ints_3[0];
            int[] ints_6 = ints_3[1];
            int[] ints_7 = ints_3[2];
            int i_8;
            int i_9;
            if (this.anInt9880 == TextureRaster.currentWidth) {
                for (i_8 = 0; i_8 < TextureRaster.currentWidth; i_8++) {
                    i_9 = this.anIntArray9881[i_4++];
                    ints_7[i_8] = (i_9 & 0xff) << 4;
                    ints_6[i_8] = (i_9 & 0xff00) >> 4;
                    ints_5[i_8] = (i_9 & 0xff0000) >> 12;
                }
            } else {
                for (i_8 = 0; i_8 < TextureRaster.currentWidth; i_8++) {
                    i_9 = i_8 * this.anInt9880 / TextureRaster.currentWidth;
                    int i_10 = this.anIntArray9881[i_4 + i_9];
                    ints_7[i_8] = (i_10 & 0xff) << 4;
                    ints_6[i_8] = (i_10 & 0xff00) >> 4;
                    ints_5[i_8] = (i_10 & 0xff0000) >> 12;
                }
            }
        }

        return ints_3;
    }

    @Override
    public void createImageCache(int i_1, int i_2) {
        super.createImageCache(i_1, i_2);
        if (this.anInt9882 >= 0 && TextureTypeList.materialProvider != null) {
            int i_4 = TextureTypeList.materialProvider.getMaterial(this.anInt9882).isSmall ? 64 : 128;
            this.anIntArray9881 = TextureTypeList.materialProvider.getPixels(this.anInt9882, 1.0F, i_4, i_4, false);
            this.anInt9883 = i_4;
            this.anInt9880 = i_4;
        }

    }

    @Override
    public void clearImageCache() {
        super.clearImageCache();
        this.anIntArray9881 = null;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        if (i_1 == 0) {
            this.anInt9882 = rsbytebuffer2.readUShort();
        }

    }

    @Override
    public int getMaterialId() {
        return this.anInt9882;
    }

}
