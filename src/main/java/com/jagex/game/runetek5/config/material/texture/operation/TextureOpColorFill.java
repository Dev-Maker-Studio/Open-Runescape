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
public class TextureOpColorFill extends TextureOperation {

    public int anInt9869;
    public int anInt9870;
    public int anInt9871;

    public TextureOpColorFill() {
        super(0, false);
        method15378(0);
    }

    public void method15378(int i_1) {
        this.anInt9869 = (i_1 & 0xff) << 4;
        this.anInt9870 = (i_1 & 0xff00) >> 4;
        this.anInt9871 = (i_1 & 0xff0000) >> 12;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                method15378(rsbytebuffer2.read24BitUnsignedInteger());
            default:
        }
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int[] ints_4 = ints_3[0];
            int[] ints_5 = ints_3[1];
            int[] ints_6 = ints_3[2];

            for (int i_7 = 0; i_7 < TextureRaster.currentWidth; i_7++) {
                ints_4[i_7] = this.anInt9871;
                ints_5[i_7] = this.anInt9870;
                ints_6[i_7] = this.anInt9869;
            }
        }

        return ints_3;
    }

}
