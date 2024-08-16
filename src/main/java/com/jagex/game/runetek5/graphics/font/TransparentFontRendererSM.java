package com.jagex.game.runetek5.graphics.font;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import com.jagex.game.runetek5.unidentified.ComponentDimensionSM;

public class TransparentFontRendererSM extends AbstractFontRenderer {

    public SafeModeToolkit aGraphicalRendererSub38889;
    public final int[] anIntArray8886;
    public final int[] anIntArray8888;
    public final byte[][] aByteArrayArray8887;
    public final int[] anIntArray8890;
    public final int[] anIntArray8891;

    public TransparentFontRendererSM(SafeModeToolkit hardwarerenderer1, FontMetrics fontmetrics2, SpriteType[] arr_3, int[] ints_4, int[] ints_5) {
        super(hardwarerenderer1, fontmetrics2);
        this.aGraphicalRendererSub38889 = hardwarerenderer1;
        this.anIntArray8886 = ints_4;
        this.anIntArray8888 = ints_5;
        this.aByteArrayArray8887 = new byte[arr_3.length][];
        this.anIntArray8890 = new int[arr_3.length];
        this.anIntArray8891 = new int[arr_3.length];

        for (int i_6 = 0; i_6 < arr_3.length; i_6++) {
            SpriteType class917 = arr_3[i_6];
            if (class917.alpha != null) {
                this.aByteArrayArray8887[i_6] = class917.alpha;
            } else {
                byte[] bytes_8 = class917.pixels;
                byte[] bytes_9 = this.aByteArrayArray8887[i_6] = new byte[bytes_8.length];

                for (int i_10 = 0; i_10 < bytes_8.length; i_10++) {
                    bytes_9[i_10] = (byte) (bytes_8[i_10] == 0 ? 0 : -1);
                }
            }

            this.anIntArray8890[i_6] = class917.minY;
            this.anIntArray8891[i_6] = class917.minX;
        }

    }

    @Override
    public void drawChar(int var_1, int i_2, int i_3, int i_4, boolean bool5) {
        int i_31 = i_3;
        int i_21 = i_2;
        if (this.aGraphicalRendererSub38889.colourBuffer != null) {
            i_21 += this.anIntArray8891[var_1];
            i_31 += this.anIntArray8890[var_1];
            int i_6 = this.anIntArray8886[var_1];
            int i_7 = this.anIntArray8888[var_1];
            int i_8 = this.aGraphicalRendererSub38889.viewportWidth;
            int i_9 = i_21 + i_31 * i_8;
            int i_10 = i_8 - i_6;
            int i_11 = 0;
            int i_12 = 0;
            int i_13;
            if (i_31 < this.aGraphicalRendererSub38889.clipTop) {
                i_13 = this.aGraphicalRendererSub38889.clipTop - i_31;
                i_7 -= i_13;
                i_31 = this.aGraphicalRendererSub38889.clipTop;
                i_12 += i_6 * i_13;
                i_9 += i_8 * i_13;
            }

            if (i_31 + i_7 > this.aGraphicalRendererSub38889.clipBottom) {
                i_7 -= i_31 + i_7 - this.aGraphicalRendererSub38889.clipBottom;
            }

            if (i_21 < this.aGraphicalRendererSub38889.clipLeft) {
                i_13 = this.aGraphicalRendererSub38889.clipLeft - i_21;
                i_6 -= i_13;
                i_21 = this.aGraphicalRendererSub38889.clipLeft;
                i_12 += i_13;
                i_9 += i_13;
                i_11 += i_13;
                i_10 += i_13;
            }

            if (i_6 + i_21 > this.aGraphicalRendererSub38889.clipRight) {
                i_13 = i_6 + i_21 - this.aGraphicalRendererSub38889.clipRight;
                i_6 -= i_13;
                i_11 += i_13;
                i_10 += i_13;
            }

            if (i_6 > 0 && i_7 > 0) {
                method14257(this.aByteArrayArray8887[var_1], this.aGraphicalRendererSub38889.colourBuffer, i_4, i_12, i_9, i_6, i_7, i_10, i_11);
            }
        }

    }

    public void method14257(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9) {
        for (int i_10 = -i_7; i_10 < 0; i_10++) {
            for (int i_11 = -i_6; i_11 < 0; i_11++) {
                int i_12 = bytes_1[i_4++] & 0xff;
                if (i_12 != 0) {
                    int i_13 = (i_12 * (i_3 & 0xff00) & 0xff0000) + ((i_3 & 0xff00ff) * i_12 & -16711936) >> 8;
                    i_12 = 256 - i_12;
                    int i_14 = ints_2[i_5];
                    ints_2[i_5++] = i_13 + ((i_12 * (i_14 & 0xff00) & 0xff0000) + ((i_14 & 0xff00ff) * i_12 & -16711936) >> 8);
                } else {
                    ++i_5;
                }
            }

            i_5 += i_8;
            i_4 += i_9;
        }

    }


    public void method14258(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_10, int i_11, int i_12, ComponentSurface class45513, int i_14, int i_15) {
        ComponentDimensionSM packetsdecoder16 = (ComponentDimensionSM) class45513;
        int[] ints_17 = packetsdecoder16.anIntArray9077;
        int[] ints_18 = packetsdecoder16.anIntArray9078;
        int i_19 = i_10 - this.aGraphicalRendererSub38889.clipLeft;
        int i_20 = i_11;
        if (i_15 > i_11) {
            i_20 = i_15;
            i_5 += this.aGraphicalRendererSub38889.viewportWidth * (i_15 - i_11);
            i_4 += (i_15 - i_11) * i_12;
        }

        int i_21 = Math.min(ints_17.length + i_15, i_11 + i_7);

        for (int i_22 = i_20; i_22 < i_21; i_22++) {
            int i_23 = ints_17[i_22 - i_15] + i_14;
            int i_24 = ints_18[i_22 - i_15];
            int i_25 = i_6;
            int i_26;
            if (i_19 > i_23) {
                i_26 = i_19 - i_23;
                if (i_26 >= i_24) {
                    i_4 += i_9 + i_6;
                    i_5 += i_8 + i_6;
                    continue;
                }

                i_24 -= i_26;
            } else {
                i_26 = i_23 - i_19;
                if (i_26 >= i_6) {
                    i_4 += i_9 + i_6;
                    i_5 += i_8 + i_6;
                    continue;
                }

                i_4 += i_26;
                i_25 = i_6 - i_26;
                i_5 += i_26;
            }

            i_26 = 0;
            if (i_25 < i_24) {
                i_24 = i_25;
            } else {
                i_26 = i_25 - i_24;
            }

            for (int i_27 = -i_24; i_27 < 0; i_27++) {
                int i_28 = bytes_1[i_4++] & 0xff;
                if (i_28 != 0) {
                    int i_29 = (i_28 * (i_3 & 0xff00) & 0xff0000) + ((i_3 & 0xff00ff) * i_28 & -16711936) >> 8;
                    i_28 = 256 - i_28;
                    int i_30 = ints_2[i_5];
                    ints_2[i_5++] = (((i_30 & 0xff00ff) * i_28 & -16711936) + (i_28 * (i_30 & 0xff00) & 0xff0000) >> 8) + i_29;
                } else {
                    ++i_5;
                }
            }

            i_4 = i_26 + i_4 + i_9;
            i_5 += i_26 + i_8;
        }

    }



    @Override
    public void drawChar(int var_1, int i_2, int i_3, int i_4, boolean bool5, ComponentSurface class4556, int i_7, int i_8) {
        int i_31 = i_3;
        int i_21 = i_2;
        if (this.aGraphicalRendererSub38889.colourBuffer != null) {
            if (class4556 == null) {
                drawChar(var_1, i_21, i_31, i_4, bool5);
            } else {
                i_21 += this.anIntArray8891[var_1];
                i_31 += this.anIntArray8890[var_1];
                int i_9 = this.anIntArray8886[var_1];
                int i_10 = this.anIntArray8888[var_1];
                int i_11 = this.aGraphicalRendererSub38889.viewportWidth;
                int i_12 = i_21 + i_31 * i_11;
                int i_13 = i_11 - i_9;
                int i_14 = 0;
                int i_15 = 0;
                int i_16;
                if (i_31 < this.aGraphicalRendererSub38889.clipTop) {
                    i_16 = this.aGraphicalRendererSub38889.clipTop - i_31;
                    i_10 -= i_16;
                    i_31 = this.aGraphicalRendererSub38889.clipTop;
                    i_15 += i_16 * i_9;
                    i_12 += i_16 * i_11;
                }

                if (i_31 + i_10 > this.aGraphicalRendererSub38889.clipBottom) {
                    i_10 -= i_31 + i_10 - this.aGraphicalRendererSub38889.clipBottom;
                }

                if (i_21 < this.aGraphicalRendererSub38889.clipLeft) {
                    i_16 = this.aGraphicalRendererSub38889.clipLeft - i_21;
                    i_9 -= i_16;
                    i_21 = this.aGraphicalRendererSub38889.clipLeft;
                    i_15 += i_16;
                    i_12 += i_16;
                    i_14 += i_16;
                    i_13 += i_16;
                }

                if (i_9 + i_21 > this.aGraphicalRendererSub38889.clipRight) {
                    i_16 = i_9 + i_21 - this.aGraphicalRendererSub38889.clipRight;
                    i_9 -= i_16;
                    i_14 += i_16;
                    i_13 += i_16;
                }

                if (i_9 > 0 && i_10 > 0) {
                    method14258(this.aByteArrayArray8887[var_1], this.aGraphicalRendererSub38889.colourBuffer, i_4, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, this.anIntArray8886[var_1], class4556, i_7, i_8);
                }
            }
        }

    }

}
