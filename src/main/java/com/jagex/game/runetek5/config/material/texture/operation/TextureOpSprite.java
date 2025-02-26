package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.config.material.texture.TextureTypeList;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpSprite extends TextureOperation {

    int[] anIntArray9916;
    int anInt9914;
    int anInt9915;
    public int anInt9912 = -1;

    public TextureOpSprite() {
        super(0, false);
    }

    @Override
    public void clearImageCache() {
        super.clearImageCache();
        this.anIntArray9916 = null;
    }

    boolean method15398() {
        if (this.anIntArray9916 != null) {
            return true;
        } else if (this.anInt9912 >= 0) {
            SpriteType class912 = TextureTypeList.TEXTURE_GROUP_ID >= 0 ? SpriteType.listSingle(TextureTypeList.textureSpriteResource, TextureTypeList.TEXTURE_GROUP_ID, this.anInt9912) : SpriteType.listSingle(TextureTypeList.textureSpriteResource, this.anInt9912);
            assert class912 != null;
            class912.method1523();
            this.anIntArray9916 = class912.getPixels();
            this.anInt9914 = class912.width;
            this.anInt9915 = class912.height;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        if (i_1 == 0) {
            this.anInt9912 = rsbytebuffer2.readUShort();
        }

    }

    @Override
    public int getTextureId() {
        return this.anInt9912;
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty && method15398()) {
            int[] ints_4 = ints_3[0];
            int[] ints_5 = ints_3[1];
            int[] ints_6 = ints_3[2];
            int i_7 = (this.anInt9915 != TextureRaster.currentHeight ? this.anInt9915 * i_1 / TextureRaster.currentHeight : i_1) * this.anInt9914;
            int i_8;
            int i_9;
            if (this.anInt9914 == TextureRaster.currentWidth) {
                for (i_8 = 0; i_8 < TextureRaster.currentWidth; i_8++) {
                    i_9 = this.anIntArray9916[i_7++];
                    ints_6[i_8] = (i_9 & 0xff) << 4;
                    ints_5[i_8] = (i_9 & 0xff00) >> 4;
                    ints_4[i_8] = (i_9 & 0xff0000) >> 12;
                }
            } else {
                for (i_8 = 0; i_8 < TextureRaster.currentWidth; i_8++) {
                    i_9 = i_8 * this.anInt9914 / TextureRaster.currentWidth;
                    int i_10 = this.anIntArray9916[i_7 + i_9];
                    ints_6[i_8] = (i_10 & 0xff) << 4;
                    ints_5[i_8] = (i_10 & 0xff00) >> 4;
                    ints_4[i_8] = (i_10 & 0xff0000) >> 12;
                }
            }
        }

        return ints_3;
    }

}
