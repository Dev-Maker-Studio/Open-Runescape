package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;

public class TextureOp37 extends TextureOperation {

    public int anInt9830 = 2048;
    public int anInt9829;
    public int anInt9833;
    public int anInt9831 = 2048;
    public int anInt9832 = 12288;
    public int anInt9828 = 4096;
    public int anInt9834 = 8192;

    public TextureOp37() {
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
                this.anInt9830 = rsbytebuffer2.readUShort();
                break;
            case 1:
                this.anInt9829 = rsbytebuffer2.readUShort();
                break;
            case 2:
                this.anInt9833 = rsbytebuffer2.readUShort();
                break;
            case 3:
                this.anInt9831 = rsbytebuffer2.readUShort();
                break;
            case 4:
                this.anInt9832 = rsbytebuffer2.readUShort();
                break;
            case 5:
                this.anInt9828 = rsbytebuffer2.readUShort();
                break;
            case 6:
                this.anInt9834 = rsbytebuffer2.readUShort();
        }

    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = TextureRaster.verticalTable[i_1] - 2048;

            for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                int i_6 = TextureRaster.horizontalTable[i_5] - 2048;
                int i_7 = i_6 + this.anInt9830;
                i_7 = i_7 < -2048 ? i_7 + 4096 : i_7;
                i_7 = i_7 > 2048 ? i_7 - 4096 : i_7;
                int i_8 = i_4 + this.anInt9829;
                i_8 = i_8 < -2048 ? i_8 + 4096 : i_8;
                i_8 = i_8 > 2048 ? i_8 - 4096 : i_8;
                int i_9 = i_6 + this.anInt9833;
                i_9 = i_9 < -2048 ? i_9 + 4096 : i_9;
                i_9 = i_9 > 2048 ? i_9 - 4096 : i_9;
                int i_10 = i_4 + this.anInt9831;
                i_10 = i_10 < -2048 ? i_10 + 4096 : i_10;
                i_10 = i_10 > 2048 ? i_10 - 4096 : i_10;
                ints_3[i_5] = !method15302(i_7, i_8) && !method15300(i_9, i_10) ? 0 : 4096;
            }
        }

        return ints_3;
    }

    public boolean method15300(int i_1, int i_2) {
        int i_4 = (i_2 + i_1) * this.anInt9832 >> 12;
        int i_5 = TextureRaster.PALETTE_COS[i_4 * 255 >> 12 & 0xff];
        i_5 = (i_5 << 12) / this.anInt9832;
        i_5 = (i_5 << 12) / this.anInt9834;
        i_5 = i_5 * this.anInt9828 >> 12;
        return i_2 - i_1 < i_5 && i_2 - i_1 > -i_5;
    }

    public boolean method15302(int i_1, int i_2) {
        int i_4 = (i_2 - i_1) * this.anInt9832 >> 12;
        int i_5 = TextureRaster.PALETTE_COS[i_4 * 255 >> 12 & 0xff];
        i_5 = (i_5 << 12) / this.anInt9832;
        i_5 = (i_5 << 12) / this.anInt9834;
        i_5 = i_5 * this.anInt9828 >> 12;
        return i_2 + i_1 < i_5 && i_2 + i_1 > -i_5;
    }

}
