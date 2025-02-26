package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.utils.ArrayUtils;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpSquareWaveform extends TextureOperation {

    public int anInt9593 = 10;
    public int anInt9594 = 2048;
    public int anInt9595;
    public int[] anIntArray9596;
    public int[] anIntArray9592;

    public TextureOpSquareWaveform() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = TextureRaster.verticalTable[i_1];
            int i_6;
            if (this.anInt9595 != 0) {
                for (int i_5 = 0; i_5 < TextureRaster.currentWidth; i_5++) {
                    i_6 = 0;
                    short s_7 = 0;
                    int i_8 = TextureRaster.horizontalTable[i_5];
                    switch (this.anInt9595) {
                        case 1:
                            i_6 = i_8;
                            break;
                        case 2:
                            i_6 = (i_8 - (4096 - i_4) >> 1) + 2048;
                            break;
                        case 3:
                            i_6 = (i_8 - i_4 >> 1) + 2048;
                    }

                    for (int i_9 = 0; i_9 < this.anInt9593; i_9++) {
                        if (i_6 >= this.anIntArray9592[i_9] && i_6 < this.anIntArray9592[i_9 + 1]) {
                            if (i_6 < this.anIntArray9596[i_9]) {
                                s_7 = 4096;
                            }
                            break;
                        }
                    }

                    ints_3[i_5] = s_7;
                }
            } else {
                short s_10 = 0;

                for (i_6 = 0; i_6 < this.anInt9593; i_6++) {
                    if (i_4 >= this.anIntArray9592[i_6] && i_4 < this.anIntArray9592[i_6 + 1]) {
                        if (i_4 < this.anIntArray9596[i_6]) {
                            s_10 = 4096;
                        }
                        break;
                    }
                }

                ArrayUtils.setVariables(ints_3, 0, TextureRaster.currentWidth, s_10);
            }
        }

        return ints_3;
    }

    @Override
    public void postDecode() {
        method14842();
    }

    public void method14842() {
        int i_2 = 0;
        this.anIntArray9596 = new int[this.anInt9593 + 1];
        this.anIntArray9592 = new int[this.anInt9593 + 1];
        int i_3 = 4096 / this.anInt9593;
        int i_4 = i_3 * this.anInt9594 >> 12;

        for (int i_5 = 0; i_5 < this.anInt9593; i_5++) {
            this.anIntArray9592[i_5] = i_2;
            this.anIntArray9596[i_5] = i_4 + i_2;
            i_2 += i_3;
        }

        this.anIntArray9592[this.anInt9593] = 4096;
        this.anIntArray9596[this.anInt9593] = this.anIntArray9596[0] + 4096;
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9593 = rsbytebuffer2.readUByte();
                break;
            case 1:
                this.anInt9594 = rsbytebuffer2.readUShort();
                break;
            case 2:
                this.anInt9595 = rsbytebuffer2.readUByte();
        }

    }

}
