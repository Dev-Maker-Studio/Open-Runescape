package com.jagex.game.runetek5.graphics.font;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import com.jagex.game.runetek5.unidentified.ComponentDimensionSM;

/**
 * The default font renderer for safe mode, doesn't support alpha
 * @author Brainpower
 *
 */
public class FontRendererSM extends AbstractFontRenderer {

    public SafeModeToolkit toolkit;
    public final int[] charWidths;
    public final int[] charHeights;
    public final byte[][] aByteArrayArray8967;
    public final int[] anIntArray8972;
    public final int[] anIntArray8973;
    public final int[] anIntArray8970;

    public FontRendererSM(SafeModeToolkit toolkit, FontMetrics fontMetrics, SpriteType[] arr_3, int[] ints_4, int[] ints_5) {
        super(toolkit, fontMetrics);
        this.toolkit = toolkit;
        this.charWidths = ints_4;
        this.charHeights = ints_5;
        this.aByteArrayArray8967 = new byte[arr_3.length][];
        this.anIntArray8972 = new int[arr_3.length];
        this.anIntArray8973 = new int[arr_3.length];
        for (int i_6 = 0; i_6 < arr_3.length; i_6++) {
        	this.aByteArrayArray8967[i_6] = arr_3[i_6].pixels;
        	this.anIntArray8972[i_6] = arr_3[i_6].minY;
        	this.anIntArray8973[i_6] = arr_3[i_6].minX;
        }
        this.anIntArray8970 = arr_3[0].pallete;
    }

    @Override
    public void drawChar(int ch, int x, int y, int color, boolean useAlpha) {
        int drawY = y;
        int drawX = x;
        if (this.toolkit.colourBuffer != null) {
            drawX += this.anIntArray8973[ch];
            drawY += this.anIntArray8972[ch];
            int charWidth = this.charWidths[ch];
            int charHeight = this.charHeights[ch];
            int viewportWidth = this.toolkit.viewportWidth;
            int pixelIndex = drawX + drawY * viewportWidth;
            int rowPadding = viewportWidth - charWidth;
            int i_11 = 0;
            int rowOffset = 0;
            int rowCount;
            if (drawY < this.toolkit.clipTop) {
                rowCount = this.toolkit.clipTop - drawY;
                charHeight -= rowCount;
                drawY = this.toolkit.clipTop;
                rowOffset += rowCount * charWidth;
                pixelIndex += rowCount * viewportWidth;
            }
            if (drawY + charHeight > this.toolkit.clipBottom) {
                charHeight -= drawY + charHeight - this.toolkit.clipBottom;
            }
            if (drawX < this.toolkit.clipLeft) {
                rowCount = this.toolkit.clipLeft - drawX;
                charWidth -= rowCount;
                drawX = this.toolkit.clipLeft;
                rowOffset += rowCount;
                pixelIndex += rowCount;
                i_11 += rowCount;
                rowPadding += rowCount;
            }
            if (charWidth + drawX > this.toolkit.clipRight) {
                rowCount = charWidth + drawX - this.toolkit.clipRight;
                charWidth -= rowCount;
                i_11 += rowCount;
                rowPadding += rowCount;
            }
            if (charWidth > 0 && charHeight > 0) {
                if (useAlpha) {
                    method14325(this.aByteArrayArray8967[ch], this.toolkit.colourBuffer, color, rowOffset, pixelIndex, charWidth, charHeight, rowPadding, i_11);
                } else {
                    method14334(this.aByteArrayArray8967[ch], this.toolkit.colourBuffer, this.anIntArray8970, rowOffset, pixelIndex, charWidth, charHeight, rowPadding, i_11);
                }
            }
        }
    }

    public void method14325(byte[] bytes_1, int[] pixels, int color, int x, int y, int width, int height, int scanWidth, int padding) {
        int dw = -(width >> 2);
        width = -(width & 0x3);
        for (int dy = -height; dy < 0; dy++) {
            for (int dx = dw; dx < 0; dx++) {
                if (bytes_1[x++] != 0) {
                    pixels[y++] = color;
                } else {
                    ++y;
                }
                if (bytes_1[x++] != 0) {
                    pixels[y++] = color;
                } else {
                    ++y;
                }
                if (bytes_1[x++] != 0) {
                    pixels[y++] = color;
                } else {
                    ++y;
                }
                if (bytes_1[x++] != 0) {
                    pixels[y++] = color;
                } else {
                    ++y;
                }
            }
            for (int dx = width; dx < 0; dx++) {
                if (bytes_1[x++] != 0) {
                    pixels[y++] = color;
                } else {
                    ++y;
                }
            }
            y += scanWidth;
            x += padding;
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
                i_21 += this.anIntArray8973[var_1];
                i_31 += this.anIntArray8972[var_1];
                int i_9 = this.charWidths[var_1];
                int i_10 = this.charHeights[var_1];
                int i_11 = this.toolkit.viewportWidth;
                int i_12 = i_21 + i_31 * i_11;
                int i_13 = i_11 - i_9;
                int i_14 = 0;
                int i_15 = 0;
                int i_16;
                if (i_31 < this.toolkit.clipTop) {
                    i_16 = this.toolkit.clipTop - i_31;
                    i_10 -= i_16;
                    i_31 = this.toolkit.clipTop;
                    i_15 += i_9 * i_16;
                    i_12 += i_16 * i_11;
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
                    if (bool5) {
                        method14327(this.aByteArrayArray8967[var_1], this.toolkit.colourBuffer, i_4, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, this.charWidths[var_1], class4556, i_7, i_8);
                    } else {
                        method14326(this.aByteArrayArray8967[var_1], this.toolkit.colourBuffer, this.anIntArray8970, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, this.charWidths[var_1], class4556, i_7, i_8);
                    }
                }
            }
        }
    }

    public void method14326(byte[] bytes_1, int[] ints_2, int[] ints_3, int i_5, int i_6, int i_7, int i_8, int i_9, int i_10, int i_11, int i_12, int i_13, ComponentSurface class45514, int i_15, int i_16) {
        ComponentDimensionSM packetsdecoder17 = (ComponentDimensionSM) class45514;
        int[] ints_18 = packetsdecoder17.anIntArray9077;
        int[] ints_19 = packetsdecoder17.anIntArray9078;
        int i_20 = i_11 - this.toolkit.clipLeft;
        int i_21 = i_12;
        if (i_16 > i_12) {
            i_21 = i_16;
            i_6 += (i_16 - i_12) * this.toolkit.viewportWidth;
            i_5 += i_13 * (i_16 - i_12);
        }
        int i_22 = Math.min(ints_18.length + i_16, i_8 + i_12);
        boolean bool23 = false;
        for (int i_24 = i_21; i_24 < i_22; i_24++) {
            int i_25 = i_15 + ints_18[i_24 - i_16];
            int i_26 = ints_19[i_24 - i_16];
            int i_27 = i_7;
            int i_28;
            if (i_20 > i_25) {
                i_28 = i_20 - i_25;
                if (i_28 >= i_26) {
                    i_5 += i_10 + i_7;
                    i_6 += i_7 + i_9;
                    continue;
                }
                i_26 -= i_28;
            } else {
                i_28 = i_25 - i_20;
                if (i_28 >= i_7) {
                    i_5 += i_10 + i_7;
                    i_6 += i_7 + i_9;
                    continue;
                }
                i_5 += i_28;
                i_27 = i_7 - i_28;
                i_6 += i_28;
            }
            i_28 = 0;
            if (i_27 < i_26) {
                i_26 = i_27;
            } else {
                i_28 = i_27 - i_26;
            }
            for (int i_29 = -i_26; i_29 < 0; i_29++) {
                byte b_30;
                if ((b_30 = bytes_1[i_5++]) != 0) {
                    ints_2[i_6++] = ints_3[b_30 & 0xff];
                } else {
                    ++i_6;
                }
            }
            i_5 += i_10 + i_28;
            i_6 += i_28 + i_9;
        }
    }

    public void method14327(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_10, int i_11, int i_12, ComponentSurface class45513, int i_14, int i_15) {
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
                if (bytes_1[i_4++] != 0) {
                    ints_2[i_5++] = i_3;
                } else {
                    ++i_5;
                }
            }
            i_4 += i_26 + i_4;
            i_5 += i_26 + i_8;
        }
    }

    public void method14334(byte[] bytes_1, int[] ints_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9) {
        int i_10 = -(i_6 >> 2);
        i_6 = -(i_6 & 0x3);
        boolean bool11 = false;
        for (int i_12 = -i_7; i_12 < 0; i_12++) {
            int i_13;
            byte b_14;
            for (i_13 = i_10; i_13 < 0; i_13++) {
                if ((b_14 = bytes_1[i_4++]) != 0) {
                    ints_2[i_5++] = ints_3[b_14 & 0xff];
                } else {
                    ++i_5;
                }
                if ((b_14 = bytes_1[i_4++]) != 0) {
                    ints_2[i_5++] = ints_3[b_14 & 0xff];
                } else {
                    ++i_5;
                }
                if ((b_14 = bytes_1[i_4++]) != 0) {
                    ints_2[i_5++] = ints_3[b_14 & 0xff];
                } else {
                    ++i_5;
                }
                if ((b_14 = bytes_1[i_4++]) != 0) {
                    ints_2[i_5++] = ints_3[b_14 & 0xff];
                } else {
                    ++i_5;
                }
            }
            for (i_13 = i_6; i_13 < 0; i_13++) {
                if ((b_14 = bytes_1[i_4++]) != 0) {
                    ints_2[i_5++] = ints_3[b_14 & 0xff];
                } else {
                    ++i_5;
                }
            }
            i_5 += i_8;
            i_4 += i_9;
        }
    }
}
