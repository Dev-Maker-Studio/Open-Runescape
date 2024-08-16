package com.jagex.game.runetek5.graphics.font;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import com.jagex.game.runetek5.unidentified.ComponentDimensionSM;

public class OpaqueFontRendererSM extends AbstractFontRenderer {

    public final SafeModeToolkit toolkit;
    public final int[] anIntArray8511;
    public final int[] anIntArray8512;
    public final byte[][] spritePixels;
    public final int[] spriteMinY;
    public final int[] spriteMinX;

    public OpaqueFontRendererSM(SafeModeToolkit toolkit, FontMetrics fontMetrics, SpriteType[] sprites, int[] ints_4, int[] ints_5) {
        super(toolkit, fontMetrics);
        this.toolkit = toolkit;
        this.anIntArray8511 = ints_4;
        this.anIntArray8512 = ints_5;
        this.spritePixels = new byte[sprites.length][];
        this.spriteMinY = new int[sprites.length];
        this.spriteMinX = new int[sprites.length];

        for (int index = 0; index < sprites.length; index++) {
        	this.spritePixels[index] = sprites[index].pixels;
        	this.spriteMinY[index] = sprites[index].minY;
        	this.spriteMinX[index] = sprites[index].minX;
        }

    }

    @Override
    public void drawChar(int var_1, int i_2, int i_3, int i_4, boolean bool5) {
        int y = i_3;
        int x = i_2;
        if (this.toolkit.colourBuffer != null) {
            x += this.spriteMinX[var_1];
            y += this.spriteMinY[var_1];
            int i_6 = this.anIntArray8511[var_1];
            int i_7 = this.anIntArray8512[var_1];
            int i_8 = this.toolkit.viewportWidth;
            int i_9 = y * i_8 + x;
            int i_10 = i_8 - i_6;
            int i_11 = 0;
            int i_12 = 0;
            int i_13;
            if (y < this.toolkit.clipTop) {
                i_13 = this.toolkit.clipTop - y;
                i_7 -= i_13;
                y = this.toolkit.clipTop;
                i_12 += i_6 * i_13;
                i_9 += i_8 * i_13;
            }

            if (y + i_7 > this.toolkit.clipBottom) {
                i_7 -= y + i_7 - this.toolkit.clipBottom;
            }

            if (x < this.toolkit.clipLeft) {
                i_13 = this.toolkit.clipLeft - x;
                i_6 -= i_13;
                x = this.toolkit.clipLeft;
                i_12 += i_13;
                i_9 += i_13;
                i_11 += i_13;
                i_10 += i_13;
            }

            if (i_6 + x > this.toolkit.clipRight) {
                i_13 = i_6 + x - this.toolkit.clipRight;
                i_6 -= i_13;
                i_11 += i_13;
                i_10 += i_13;
            }

            if (i_6 > 0 && i_7 > 0) {
                if ((i_4 & -16777216) == -16777216) {
                    method13771(this.spritePixels[var_1], this.toolkit.colourBuffer, i_4, i_12, i_9, i_6, i_7, i_10, i_11);
                } else if ((i_4 & -16777216) != 0) {
                    blendImage(this.spritePixels[var_1], this.toolkit.colourBuffer, i_4, i_12, i_9, i_6, i_7, i_10, i_11);
                }
            }
        }

    }

    @Override
    public void drawChar(int var_1, int i_2, int i_3, int i_4, boolean bool5, ComponentSurface class4556, int i_7, int i_8) {
        int i_31 = i_3;
        int i_21 = i_2;
        if (this.toolkit.colourBuffer != null) {
            if (class4556 == null) {
                drawChar(var_1, i_21, i_31, i_4, bool5);
            } else {
                i_21 += this.spriteMinX[var_1];
                i_31 += this.spriteMinY[var_1];
                int i_9 = this.anIntArray8511[var_1];
                int i_10 = this.anIntArray8512[var_1];
                int i_11 = this.toolkit.viewportWidth;
                int i_12 = i_31 * i_11 + i_21;
                int i_13 = i_11 - i_9;
                int i_14 = 0;
                int i_15 = 0;
                int i_16;
                if (i_31 < this.toolkit.clipTop) {
                    i_16 = this.toolkit.clipTop - i_31;
                    i_10 -= i_16;
                    i_31 = this.toolkit.clipTop;
                    i_15 += i_9 * i_16;
                    i_12 += i_11 * i_16;
                }

                if (i_31 + i_10 > this.toolkit.clipBottom) {
                    i_10 -= i_31 + i_10 - this.toolkit.clipBottom;
                }

                if (i_21 < this.toolkit.clipLeft) {
                    i_16 = this.toolkit.clipLeft - i_21;
                    i_9 -= i_16;
                    i_21 = this.toolkit.clipLeft;
                    i_15 += i_16;
                    i_12 += i_16;
                    i_14 += i_16;
                    i_13 += i_16;
                }

                if (i_9 + i_21 > this.toolkit.clipRight) {
                    i_16 = i_9 + i_21 - this.toolkit.clipRight;
                    i_9 -= i_16;
                    i_14 += i_16;
                    i_13 += i_16;
                }

                if (i_9 > 0 && i_10 > 0) {
                    if ((i_4 & -16777216) == -16777216) {
                        drawImage(this.spritePixels[var_1], this.toolkit.colourBuffer, i_4, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, this.anIntArray8511[var_1], class4556, i_7, i_8);
                    } else {
                        method13774(this.spritePixels[var_1], this.toolkit.colourBuffer, i_4, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, this.anIntArray8511[var_1], class4556, i_7, i_8);
                    }
                }
            }
        }

    }

    public void method13771(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9) {
        int i_10 = -(i_6 >> 2);
        i_6 = -(i_6 & 0x3);

        for (int i_11 = -i_7; i_11 < 0; i_11++) {
            int i_12;
            for (i_12 = i_10; i_12 < 0; i_12++) {
                if (bytes_1[i_4++] != 0) {
                    ints_2[i_5++] = i_3;
                } else {
                    ++i_5;
                }

                if (bytes_1[i_4++] != 0) {
                    ints_2[i_5++] = i_3;
                } else {
                    ++i_5;
                }

                if (bytes_1[i_4++] != 0) {
                    ints_2[i_5++] = i_3;
                } else {
                    ++i_5;
                }

                if (bytes_1[i_4++] != 0) {
                    ints_2[i_5++] = i_3;
                } else {
                    ++i_5;
                }
            }

            for (i_12 = i_6; i_12 < 0; i_12++) {
                if (bytes_1[i_4++] != 0) {
                    ints_2[i_5++] = i_3;
                } else {
                    ++i_5;
                }
            }

            i_5 += i_8;
            i_4 += i_9;
        }

    }

    /**
     * Applies an alpha-blended color transformation to a rectangular region
     */
    public void blendImage(byte[] palette, int[] pixels, int color, int dstPos, int srcPos, int width, int height, int srcScanSize, int dstScanSize) {
        int alpha = color >>> 24;
        int invAlpha = 255 - alpha;

        for (int y = -height; y < 0; y++) {
            for (int x = -width; x < 0; x++) {
                if (palette[dstPos++] != 0) {
                    int blendedPixel = (alpha * (color & 0xff00) & 0xff0000) + ((color & 0xff00ff) * alpha & -16711936) >> 8;
                    int dstPixel = pixels[srcPos];
                    pixels[srcPos++] = blendedPixel + ((invAlpha * (dstPixel & 0xff00) & 0xff0000) + ((dstPixel & 0xff00ff) * invAlpha & -16711936) >> 8);
                } else {
                    srcPos++;
                }
            }

            srcPos += srcScanSize;
            dstPos += dstScanSize;
        }

    }

    public void drawImage(byte[] palette, int[] pixels, int color, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_10, int i_11, int i_12, ComponentSurface class45513, int i_14, int i_15) {
        ComponentDimensionSM packetsdecoder16 = (ComponentDimensionSM) class45513;
        int[] offsetsX = packetsdecoder16.anIntArray9077;
        int[] offsetsY = packetsdecoder16.anIntArray9078;
        int i_19 = i_11;
        if (i_15 > i_11) {
            i_19 = i_15;
            i_5 += this.toolkit.viewportWidth * (i_15 - i_11);
            i_4 += (i_15 - i_11) * i_12;
        }

        int i_20 = Math.min(offsetsX.length + i_15, i_11 + i_7);

        for (int i_21 = i_19; i_21 < i_20; i_21++) {
            int offX = offsetsX[i_21 - i_15] + i_14;
            int offY = offsetsY[i_21 - i_15];
            int i_24 = i_6;
            int i_25;
            if (i_10 > offX) {
                i_25 = i_10 - offX;
                if (i_25 >= offY) {
                    i_4 += i_9 + i_6;
                    i_5 += i_8 + i_6;
                    continue;
                }

                offY -= i_25;
            } else {
                i_25 = offX - i_10;
                if (i_25 >= i_6) {
                    i_4 += i_9 + i_6;
                    i_5 += i_8 + i_6;
                    continue;
                }

                i_4 += i_25;
                i_24 = i_6 - i_25;
                i_5 += i_25;
            }

            i_25 = 0;
            if (i_24 < offY) {
                offY = i_24;
            } else {
                i_25 = i_24 - offY;
            }

            for (int i_26 = 0; i_26 < offY; i_26++) {
                if (palette[i_4++] != 0) {
                    pixels[i_5++] = color;
                } else {
                    i_5++;
                }
            }

            i_4 = i_25 + i_4 + i_9;
            i_5 += i_25 + i_8;
        }

    }

    public void method13774(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_10, int i_11, int i_12, ComponentSurface class45513, int i_14, int i_15) {
        ComponentDimensionSM packetsdecoder16 = (ComponentDimensionSM) class45513;
        int[] ints_17 = packetsdecoder16.anIntArray9077;
        int[] ints_18 = packetsdecoder16.anIntArray9078;
        int i_19 = i_10 - this.toolkit.clipLeft;
        int i_20 = i_11;
        if (i_15 > i_11) {
            i_20 = i_15;
            i_5 += this.toolkit.viewportWidth * (i_15 - i_11);
            i_4 += (i_15 - i_11) * i_12;
        }

        int i_21 = Math.min(ints_17.length + i_15, i_11 + i_7);
        int i_22 = i_3 >>> 24;
        int i_23 = 255 - i_22;

        for (int i_24 = i_20; i_24 < i_21; i_24++) {
            int i_25 = ints_17[i_24 - i_15] + i_14;
            int i_26 = ints_18[i_24 - i_15];
            int i_27 = i_6;
            int i_28;
            if (i_19 > i_25) {
                i_28 = i_19 - i_25;
                if (i_28 >= i_26) {
                    i_4 += i_9 + i_6;
                    i_5 += i_8 + i_6;
                    continue;
                }

                i_26 -= i_28;
            } else {
                i_28 = i_25 - i_19;
                if (i_28 >= i_6) {
                    i_4 += i_9 + i_6;
                    i_5 += i_8 + i_6;
                    continue;
                }

                i_4 += i_28;
                i_27 = i_6 - i_28;
                i_5 += i_28;
            }

            i_28 = 0;
            if (i_27 < i_26) {
                i_26 = i_27;
            } else {
                i_28 = i_27 - i_26;
            }

            for (int i_29 = -i_26; i_29 < 0; i_29++) {
                if (bytes_1[i_4++] != 0) {
                    int i_30 = (i_22 * (i_3 & 0xff00) & 0xff0000) + ((i_3 & 0xff00ff) * i_22 & -16711936) >> 8;
                    int i_31 = ints_2[i_5];
                    ints_2[i_5++] = (((i_31 & 0xff00ff) * i_23 & -16711936) + (i_23 * (i_31 & 0xff00) & 0xff0000) >> 8) + i_30;
                } else {
                    ++i_5;
                }
            }

            i_4 = i_28 + i_4 + i_9;
            i_5 += i_28 + i_8;
        }

    }

}
