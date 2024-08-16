package com.jagex.game.runetek5.config.material.texture.operation;

import java.util.Random;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.utils.ArrayUtils;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpBricks extends TextureOperation {

    public int anInt9982 = 4;
    public int anInt9994 = 8;
    public int anInt9986;
    public int anInt9987 = 1024;
    public int anInt9984 = 1024;
    public int anInt9989 = 409;
    public int anInt9990 = 204;
    public int anInt9991 = 81;
    public int anInt9993;
    public int anInt9988;
    public int anInt9995;
    public int[] anIntArray9996;
    public int[][] anIntArrayArray9979;
    public int[][] anIntArrayArray9992;

    public TextureOpBricks() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = 0;

            int i_5;
            for (i_5 = TextureRaster.verticalTable[i_1] + this.anInt9986; i_5 < 0; i_5 += 4096) {
            }

            while (i_5 > 4096) {
                i_5 -= 4096;
            }

            while (i_4 < this.anInt9994 && i_5 >= this.anIntArray9996[i_4]) {
                ++i_4;
            }

            int i_6 = i_4 - 1;
            boolean bool7 = (i_4 & 0x1) == 0;
            int i_8 = this.anIntArray9996[i_4];
            int i_9 = this.anIntArray9996[i_4 - 1];
            if (i_5 > i_9 + this.anInt9993 && i_5 < i_8 - this.anInt9993) {
                for (int i_10 = 0; i_10 < TextureRaster.currentWidth; i_10++) {
                    int i_11 = 0;
                    int i_12 = bool7 ? this.anInt9987 : -this.anInt9987;

                    int i_13;
                    for (i_13 = TextureRaster.horizontalTable[i_10] + (i_12 * this.anInt9988 >> 12); i_13 < 0; i_13 += 4096) {
                    }

                    while (i_13 > 4096) {
                        i_13 -= 4096;
                    }

                    while (i_11 < this.anInt9982 && i_13 >= this.anIntArrayArray9979[i_6][i_11]) {
                        ++i_11;
                    }

                    int i_14 = i_11 - 1;
                    int i_15 = this.anIntArrayArray9979[i_6][i_14];
                    int i_16 = this.anIntArrayArray9979[i_6][i_11];
                    if (i_13 > i_15 + this.anInt9993 && i_13 < i_16 - this.anInt9993) {
                        ints_3[i_10] = this.anIntArrayArray9992[i_6][i_14];
                    } else {
                        ints_3[i_10] = 0;
                    }
                }
            } else {
                ArrayUtils.setVariables(ints_3, 0, TextureRaster.currentWidth, 0);
            }
        }

        return ints_3;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9982 = rsbytebuffer2.readUnsignedByte();
                break;
            case 1:
                this.anInt9994 = rsbytebuffer2.readUnsignedByte();
                break;
            case 2:
                this.anInt9989 = rsbytebuffer2.readUnsignedShort();
                break;
            case 3:
                this.anInt9990 = rsbytebuffer2.readUnsignedShort();
                break;
            case 4:
                this.anInt9987 = rsbytebuffer2.readUnsignedShort();
                break;
            case 5:
                this.anInt9986 = rsbytebuffer2.readUnsignedShort();
                break;
            case 6:
                this.anInt9991 = rsbytebuffer2.readUnsignedShort();
                break;
            case 7:
                this.anInt9984 = rsbytebuffer2.readUnsignedShort();
        }

    }

    public void method15422() {
        Random random2 = new Random(this.anInt9994);
        this.anInt9993 = this.anInt9991 / 2;
        this.anInt9988 = 4096 / this.anInt9982;
        this.anInt9995 = 4096 / this.anInt9994;
        int i_3 = this.anInt9988 / 2;
        int i_4 = this.anInt9995 / 2;
        this.anIntArray9996 = new int[this.anInt9994 + 1];
        this.anIntArrayArray9979 = new int[this.anInt9994][this.anInt9982 + 1];
        this.anIntArrayArray9992 = new int[this.anInt9994][this.anInt9982];
        this.anIntArray9996[0] = 0;

        for (int i_5 = 0; i_5 < this.anInt9994; i_5++) {
            int i_6;
            int i_7;
            if (i_5 > 0) {
                i_6 = this.anInt9995;
                i_7 = (StaticMethods.method7931(random2, 4096) - 2048) * this.anInt9990 >> 12;
                i_6 += i_7 * i_4 >> 12;
                this.anIntArray9996[i_5] = i_6 + this.anIntArray9996[i_5 - 1];
            }

            this.anIntArrayArray9979[i_5][0] = 0;

            for (i_6 = 0; i_6 < this.anInt9982; i_6++) {
                if (i_6 > 0) {
                    i_7 = this.anInt9988;
                    int i_8 = (StaticMethods.method7931(random2, 4096) - 2048) * this.anInt9989 >> 12;
                    i_7 += i_8 * i_3 >> 12;
                    this.anIntArrayArray9979[i_5][i_6] = this.anIntArrayArray9979[i_5][i_6 - 1] + i_7;
                }

                this.anIntArrayArray9992[i_5][i_6] = this.anInt9984 > 0 ? 4096 - StaticMethods.method7931(random2, this.anInt9984) : 4096;
            }

            this.anIntArrayArray9979[i_5][this.anInt9982] = 4096;
        }

        this.anIntArray9996[this.anInt9994] = 4096;
    }

    @Override
    public void postDecode() {
        method15422();
    }

}
