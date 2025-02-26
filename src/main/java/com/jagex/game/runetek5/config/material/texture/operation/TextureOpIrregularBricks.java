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
public class TextureOpIrregularBricks extends TextureOperation {

    public static int anInt9792;
    public int anInt9782;
    public int anInt9791 = 1024;
    public int anInt9784 = 2048;
    public int anInt9772 = 409;
    public int anInt9786 = 819;
    public int anInt9787 = 1024;
    public int anInt9788;
    public int anInt9780 = 1024;
    public int anInt9790 = 1024;
    public int anInt9789;

    public TextureOpIrregularBricks() {
        super(0, true);
    }

    public void method15253(Random random1, int[][] ints_2, int i_3, int i_4, int i_5, int i_6) {
        int i_8 = this.anInt9790 > 0 ? 4096 - StaticMethods.method7931(random1, this.anInt9790) : 4096;
        int i_9 = this.anInt9780 * this.anInt9789 >> 12;
        int i_10 = this.anInt9789 - (i_9 > 0 ? StaticMethods.method7931(random1, i_9) : 0);
        if (i_3 >= TextureRaster.currentWidth) {
            i_3 -= TextureRaster.currentWidth;
        }
        int i_11;
        int i_12;
        if (i_10 > 0) {
            if (i_6 > 0 && i_5 > 0) {
                i_11 = i_5 / 2;
                i_12 = i_6 / 2;
                int i_13 = Math.min(i_11, i_10);
                int i_14 = Math.min(i_12, i_10);
                int i_15 = i_3 + i_13;
                int i_16 = i_5 - i_13 * 2;
                for (int i_17 = 0; i_17 < i_6; i_17++) {
                    int[] ints_18 = ints_2[i_17 + i_4];
                    int i_19;
                    int i_20;
                    int i_21;
                    if (i_17 < i_14) {
                        i_19 = i_8 * i_17 / i_14;
                        if (this.anInt9788 == 0) {
                            for (i_20 = 0; i_20 < i_13; i_20++) {
                                i_21 = i_8 * i_20 / i_13;
                                ints_18[i_3 + i_20 & TextureRaster.rowEnd] = ints_18[i_3 + i_5 - i_20 - 1 & TextureRaster.rowEnd] = i_21 * i_19 >> 12;
                            }
                        } else {
                            for (i_20 = 0; i_20 < i_13; i_20++) {
                                i_21 = i_20 * i_8 / i_13;
                                ints_18[i_3 + i_20 & TextureRaster.rowEnd] = ints_18[i_3 + i_5 - i_20 - 1 & TextureRaster.rowEnd] = Math.min(i_21, i_19);
                            }
                        }
                        if (i_16 + i_15 > TextureRaster.currentWidth) {
                            i_20 = TextureRaster.currentWidth - i_15;
                            ArrayUtils.setVariables(ints_18, i_15, i_20, i_19);
                            ArrayUtils.setVariables(ints_18, 0, i_16 - i_20, i_19);
                        } else {
                            ArrayUtils.setVariables(ints_18, i_15, i_16, i_19);
                        }
                    } else {
                        i_19 = i_6 - i_17 - 1;
                        if (i_19 < i_14) {
                            i_20 = i_8 * i_19 / i_14;
                            int i_22;
                            if (this.anInt9788 == 0) {
                                for (i_21 = 0; i_21 < i_13; i_21++) {
                                    i_22 = i_21 * i_8 / i_13;
                                    ints_18[i_3 + i_21 & TextureRaster.rowEnd] = ints_18[i_3 + i_5 - i_21 - 1 & TextureRaster.rowEnd] = i_20 * i_22 >> 12;
                                }
                            } else {
                                for (i_21 = 0; i_21 < i_13; i_21++) {
                                    i_22 = i_21 * i_8 / i_13;
                                    ints_18[i_3 + i_21 & TextureRaster.rowEnd] = ints_18[i_3 + i_5 - i_21 - 1 & TextureRaster.rowEnd] = Math.min(i_22, i_20);
                                }
                            }
                            if (i_16 + i_15 > TextureRaster.currentWidth) {
                                i_21 = TextureRaster.currentWidth - i_15;
                                ArrayUtils.setVariables(ints_18, i_15, i_21, i_20);
                                ArrayUtils.setVariables(ints_18, 0, i_16 - i_21, i_20);
                            } else {
                                ArrayUtils.setVariables(ints_18, i_15, i_16, i_20);
                            }
                        } else {
                            for (i_20 = 0; i_20 < i_13; i_20++) {
                                ints_18[i_3 + i_20 & TextureRaster.rowEnd] = ints_18[i_3 + i_5 - i_20 - 1 & TextureRaster.rowEnd] = i_20 * i_8 / i_13;
                            }
                            if (i_15 + i_16 > TextureRaster.currentWidth) {
                                i_20 = TextureRaster.currentWidth - i_15;
                                ArrayUtils.setVariables(ints_18, i_15, i_20, i_8);
                                ArrayUtils.setVariables(ints_18, 0, i_16 - i_20, i_8);
                            } else {
                                ArrayUtils.setVariables(ints_18, i_15, i_16, i_8);
                            }
                        }
                    }
                }
            }
        } else if (i_3 + i_5 > TextureRaster.currentWidth) {
            i_11 = TextureRaster.currentWidth - i_3;
            for (i_12 = 0; i_12 < i_6; i_12++) {
                int[] ints_23 = ints_2[i_12 + i_4];
                ArrayUtils.setVariables(ints_23, i_3, i_11, i_8);
                ArrayUtils.setVariables(ints_23, 0, i_5 - i_11, i_8);
            }
        } else {
            for (i_11 = 0; i_11 < i_6; i_11++) {
                ArrayUtils.setVariables(ints_2[i_11 + i_4], i_3, i_5, i_8);
            }
        }
    }

    @Override
	public
    void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9782 = rsbytebuffer2.readUByte();
                break;
            case 1:
                this.anInt9791 = rsbytebuffer2.readUShort();
                break;
            case 2:
                this.anInt9784 = rsbytebuffer2.readUShort();
                break;
            case 3:
                this.anInt9772 = rsbytebuffer2.readUShort();
                break;
            case 4:
                this.anInt9786 = rsbytebuffer2.readUShort();
                break;
            case 5:
                this.anInt9787 = rsbytebuffer2.readUShort();
                break;
            case 6:
                this.anInt9788 = rsbytebuffer2.readUByte();
                break;
            case 7:
                this.anInt9780 = rsbytebuffer2.readUShort();
                break;
            case 8:
                this.anInt9790 = rsbytebuffer2.readUShort();
        }
    }

    @Override
    public void postDecode() {
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int[][] ints_4 = super.monochromeImageCache.method5722();
            int i_5 = 0;
            int i_6 = 0;
            int i_7 = 0;
            int i_8 = 0;
            int i_9 = 0;
            boolean bool10 = true;
            boolean bool11 = true;
            int i_12 = 0;
            int i_13 = 0;
            int i_14 = this.anInt9791 * TextureRaster.currentWidth >> 12;
            int i_15 = this.anInt9784 * TextureRaster.currentWidth >> 12;
            int i_16 = this.anInt9772 * TextureRaster.currentHeight >> 12;
            int i_17 = this.anInt9786 * TextureRaster.currentHeight >> 12;
            if (i_17 <= 1) {
                return ints_4[i_1];
            } else {
                this.anInt9789 = TextureRaster.currentWidth / 8 * this.anInt9787 >> 12;
                int i_18 = TextureRaster.currentWidth / i_14 + 1;
                int[][] ints_19 = new int[i_18][3];
                int[][] ints_20 = new int[i_18][3];
                Random random21 = new Random(this.anInt9782);
                while (true) {
                    while (true) {
                        int i_22 = i_14 + StaticMethods.method7931(random21, i_15 - i_14);
                        int i_23 = i_16 + StaticMethods.method7931(random21, i_17 - i_16);
                        int i_24 = i_22 + i_8;
                        if (i_24 > TextureRaster.currentWidth) {
                            i_22 = TextureRaster.currentWidth - i_8;
                            i_24 = TextureRaster.currentWidth;
                        }
                        int i_25;
                        int i_28;
                        if (bool11) {
                            i_25 = 0;
                        } else {
                            int i_26 = i_9;
                            int[] ints_27 = ints_20[i_9];
                            i_28 = 0;
                            int i_29 = i_24 + i_5;
                            if (i_29 < 0) {
                                i_29 += TextureRaster.currentWidth;
                            }
                            if (i_29 > TextureRaster.currentWidth) {
                                i_29 -= TextureRaster.currentWidth;
                            }
                            while (true) {
                                int[] ints_30 = ints_20[i_26];
                                if (i_29 >= ints_30[0] && i_29 <= ints_30[1]) {
                                    i_25 = ints_27[2];
                                    if (i_26 != i_9) {
                                        int i_38 = i_8 + i_5;
                                        if (i_38 < 0) {
                                            i_38 += TextureRaster.currentWidth;
                                        }
                                        if (i_38 > TextureRaster.currentWidth) {
                                            i_38 -= TextureRaster.currentWidth;
                                        }
                                        int i_31;
                                        int[] ints_32;
                                        for (i_31 = 1; i_31 <= i_28; i_31++) {
                                            ints_32 = ints_20[(i_31 + i_9) % i_12];
                                            i_25 = Math.max(i_25, ints_32[2]);
                                        }
                                        for (i_31 = 0; i_31 <= i_28; i_31++) {
                                            ints_32 = ints_20[(i_31 + i_9) % i_12];
                                            int i_33 = ints_32[2];
                                            if (i_33 != i_25) {
                                                int i_34 = ints_32[0];
                                                int i_35 = ints_32[1];
                                                int i_36;
                                                int i_37;
                                                if (i_38 < i_29) {
                                                    i_36 = Math.max(i_38, i_34);
                                                    i_37 = Math.min(i_29, i_35);
                                                } else if (i_34 == 0) {
                                                    i_36 = 0;
                                                    i_37 = Math.min(i_29, i_35);
                                                } else {
                                                    i_36 = Math.max(i_38, i_34);
                                                    i_37 = TextureRaster.currentWidth;
                                                }
                                                method15253(random21, ints_4, i_36 + i_7, i_33, i_37 - i_36, i_25 - i_33);
                                            }
                                        }
                                    }
                                    i_9 = i_26;
                                    break;
                                }
                                ++i_26;
                                if (i_26 >= i_12) {
                                    i_26 = 0;
                                }
                                ++i_28;
                            }
                        }
                        if (i_25 + i_23 > TextureRaster.currentHeight) {
                            i_23 = TextureRaster.currentHeight - i_25;
                        } else {
                            bool10 = false;
                        }
                        int[] ints_39;
                        if (i_24 == TextureRaster.currentWidth) {
                            method15253(random21, ints_4, i_6 + i_8, i_25, i_22, i_23);
                            if (bool10) {
                                return ints_3;
                            }
                            bool10 = true;
                            ints_39 = ints_19[i_13++];
                            ints_39[0] = i_8;
                            ints_39[1] = i_24;
                            ints_39[2] = i_23 + i_25;
                            int[][] ints_41 = ints_20;
                            ints_20 = ints_19;
                            ints_19 = ints_41;
                            i_12 = i_13;
                            i_13 = 0;
                            i_7 = i_6;
                            i_6 = StaticMethods.method7931(random21, TextureRaster.currentWidth);
                            i_5 = i_6 - i_7;
                            i_8 = 0;
                            i_28 = i_5;
                            if (i_5 < 0) {
                                i_28 = i_5 + TextureRaster.currentWidth;
                            }
                            if (i_28 > TextureRaster.currentWidth) {
                                i_28 -= TextureRaster.currentWidth;
                            }
                            i_9 = 0;
                            while (true) {
                                int[] ints_40 = ints_20[i_9];
                                if (i_28 >= ints_40[0] && i_28 <= ints_40[1]) {
                                    bool11 = false;
                                    break;
                                }
                                ++i_9;
                                if (i_9 >= i_12) {
                                    i_9 = 0;
                                }
                            }
                        } else {
                            ints_39 = ints_19[i_13++];
                            ints_39[0] = i_8;
                            ints_39[1] = i_24;
                            ints_39[2] = i_23 + i_25;
                            method15253(random21, ints_4, i_6 + i_8, i_25, i_22, i_23);
                            i_8 = i_24;
                        }
                    }
                }
            }
        } else {
            return ints_3;
        }
    }
}
