package com.jagex.game.runetek5.graphics.font;

import java.awt.*;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.graphics.sprite.Sprite;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.utils.Utils;

public class FontMetrics {

    public static final String[] aStringArray4980 = new String[100];
    public static FontMetrics aClass4143438;
    public int verticalSpacing;
    public int topPadding;
    public int bottomPadding;
    public byte[] glyphWidths;
    public byte[][] kerning;
	public static boolean useBoldFont; //BOLD?
	public static AbstractFontRenderer aFontRenderer2348;

    public FontMetrics(byte[] bytes_1) {
        Packet stream = new Packet(bytes_1);
        int check = stream.readUnsignedByte();
        if (check != 0) {
            throw new RuntimeException("");
        } else {
            boolean variadicWidth = stream.readUnsignedByte() == 1;
            this.glyphWidths = new byte[256];
            stream.readBytes(this.glyphWidths, 0, 256);
            if (variadicWidth) {
                int[] ints_5 = new int[256];
                int[] ints_6 = new int[256];

                int i_7;
                for (i_7 = 0; i_7 < 256; i_7++) {
                    ints_5[i_7] = stream.readUnsignedByte();
                }

                for (i_7 = 0; i_7 < 256; i_7++) {
                    ints_6[i_7] = stream.readUnsignedByte();
                }

                byte[][] bytes_12 = new byte[256][];

                int i_10;
                for (int i_8 = 0; i_8 < 256; i_8++) {
                    bytes_12[i_8] = new byte[ints_5[i_8]];
                    byte b_9 = 0;

                    for (i_10 = 0; i_10 < bytes_12[i_8].length; i_10++) {
                        b_9 += stream.readByte();
                        bytes_12[i_8][i_10] = b_9;
                    }
                }

                byte[][] bytes_13 = new byte[256][];

                int i_14;
                for (i_14 = 0; i_14 < 256; i_14++) {
                    bytes_13[i_14] = new byte[ints_5[i_14]];
                    byte b_15 = 0;

                    for (int i_11 = 0; i_11 < bytes_13[i_14].length; i_11++) {
                        b_15 += stream.readByte();
                        bytes_13[i_14][i_11] = b_15;
                    }
                }

                this.kerning = new byte[256][256];

                for (i_14 = 0; i_14 < 256; i_14++) {
                    if (i_14 != 32 && i_14 != 160) {
                        for (i_10 = 0; i_10 < 256; i_10++) {
                            if (i_10 != 32 && i_10 != 160) {
                                this.kerning[i_14][i_10] = (byte) Utils.calculateKerning(bytes_12, bytes_13, ints_6, this.glyphWidths, ints_5, i_14, i_10);
                            }
                        }
                    }
                }

                this.verticalSpacing = ints_6[32] + ints_5[32];
            } else {
                this.verticalSpacing = stream.readUnsignedByte();
            }

            stream.readUnsignedByte();
            stream.readUnsignedByte();
            this.topPadding = stream.readUnsignedByte();
            this.bottomPadding = stream.readUnsignedByte();
        }
    }

    public static FontMetrics getFontMetrics(Js5ResourceProvider index0, int i_1) {
        byte[] bytes_4 = index0.getFile(i_1, 0);
        return bytes_4 == null ? null : new FontMetrics(bytes_4);
    }

    public int getKerning(int i_1, char var_2) {
        return this.kerning != null ? this.kerning[i_1][var_2] : 0;
    }

    public int getGlyphWidth(int i_1) {
        return this.glyphWidths[i_1] & 0xff;
    }

    public int getWidthNoSprites(String string_1) {
        return getTextWidth(string_1, null);
    }

    public int getTextWidth(String string_1, Sprite[] arr_2) {
        if (string_1 == null) {
            return 0;
        } else {
            int i_3 = -1;
            int i_4 = -1;
            int i_5 = 0;
            int i_6 = string_1.length();

            for (int i_7 = 0; i_7 < i_6; i_7++) {
                char var_8 = string_1.charAt(i_7);
                if (var_8 == 60) {
                    i_3 = i_7;
                } else {
                    if (var_8 == 62 && i_3 != -1) {
                        String string_9 = string_1.substring(i_3 + 1, i_7);
                        i_3 = -1;
                        if ("lt".equals(string_9)) {
                            var_8 = 60;
                        } else if ("gt".equals(string_9)) {
                            var_8 = 62;
                        } else if ("nbsp".equals(string_9)) {
                            var_8 = 160;
                        } else if ("shy".equals(string_9)) {
                            var_8 = 173;
                        } else if ("times".equals(string_9)) {
                            var_8 = 215;
                        } else if ("euro".equals(string_9)) {
                            var_8 = 8364;
                        } else if ("copy".equals(string_9)) {
                            var_8 = 169;
                        } else {
                            if (!"reg".equals(string_9)) {
                                if (string_9.startsWith("img=") && arr_2 != null) {
                                    try {
                                        int i_10 = Utils.parseInt(string_9.substring(4));
                                        i_5 += arr_2[i_10].getSpriteWidth();
                                        i_4 = -1;
                                    } catch (Exception ignored) {
                                    }
                                }
                                continue;
                            }

                            var_8 = 174;
                        }
                    }

                    if (i_3 == -1) {
                        i_5 += this.glyphWidths[Utils.byteForChar(var_8) & 0xff] & 0xff;
                        if (this.kerning != null && i_4 != -1) {
                            i_5 += this.kerning[i_4][var_8];
                        }

                        i_4 = var_8;
                    }
                }
            }

            return i_5;
        }
    }

    public int method6949(String string_1, int i_2, Sprite[] arr_3) {
        return splitLinesToBuffer(string_1, new int[]{i_2}, aStringArray4980, arr_3);
    }

    public int splitLinesToBuffer(String areaName, int[] ints_2, String[] areaNameBuffer, Sprite[] arr_4, boolean bool5) {
        if (areaName == null) {
            return 0;
        } 
        int i_7 = 0;
        int i_8 = 0;
        int i_9 = -1;
        int i_10 = 0;
        int i_11 = 0;
        int i_12 = -1;
        int i_13 = -1;
        int i_14 = 0;
        int i_15 = areaName.length();

        for (int i_16 = 0; i_16 < i_15; i_16++) {
            int i_17 = Utils.byteForChar(areaName.charAt(i_16)) & 0xff;
            int i_18 = 0;
            if (i_17 == 60) {
                i_12 = i_16;
            } else {
                int i_19;
                if (i_12 != -1) {
                    if (i_17 != 62) {
                        continue;
                    }

                    i_19 = i_12;
                    String string_20 = areaName.substring(i_12 + 1, i_16);
                    i_12 = -1;
                    if ("br".equals(string_20)) {
                        areaNameBuffer[i_14] = areaName.substring(i_8, i_16 + 1);
                        ++i_14;
                        if (i_14 >= areaNameBuffer.length) {
                            return 0;
                        }

                        i_8 = i_16 + 1;
                        i_7 = 0;
                        i_9 = -1;
                        i_13 = -1;
                        continue;
                    }

                    if ("lt".equals(string_20)) {
                        i_18 += getGlyphWidth(60);
                        if (this.kerning != null && i_13 != -1) {
                            i_18 += this.kerning[i_13][60];
                        }

                        i_13 = 60;
                    } else if ("gt".equals(string_20)) {
                        i_18 += getGlyphWidth(62);
                        if (this.kerning != null && i_13 != -1) {
                            i_18 += this.kerning[i_13][62];
                        }

                        i_13 = 62;
                    } else if ("nbsp".equals(string_20)) {
                        i_18 += getGlyphWidth(160);
                        if (this.kerning != null && i_13 != -1) {
                            i_18 += this.kerning[i_13][160];
                        }

                        i_13 = 160;
                    } else if ("shy".equals(string_20)) {
                        i_18 += getGlyphWidth(173);
                        if (this.kerning != null && i_13 != -1) {
                            i_18 += this.kerning[i_13][173];
                        }

                        i_13 = 173;
                    } else if ("times".equals(string_20)) {
                        i_18 += getGlyphWidth(215);
                        if (this.kerning != null && i_13 != -1) {
                            i_18 += this.kerning[i_13][215];
                        }

                        i_13 = 215;
                    } else if ("euro".equals(string_20)) {
                        i_18 += getGlyphWidth(8364);
                        if (this.kerning != null && i_13 != -1) {
                            i_18 += this.kerning[i_13][8364];
                        }

                        i_13 = 8364;
                    } else if ("copy".equals(string_20)) {
                        i_18 += getGlyphWidth(169);
                        if (this.kerning != null && i_13 != -1) {
                            i_18 += this.kerning[i_13][169];
                        }

                        i_13 = 169;
                    } else if ("reg".equals(string_20)) {
                        i_18 += getGlyphWidth(174);
                        if (this.kerning != null && i_13 != -1) {
                            i_18 += this.kerning[i_13][174];
                        }

                        i_13 = 174;
                    } else if (string_20.startsWith("img=") && arr_4 != null) {
                        try {
                            int i_21 = Utils.parseInt(string_20.substring(4));
                            i_18 += arr_4[i_21].getSpriteWidth();
                            i_13 = -1;
                        } catch (Exception ignored) {
                        }
                    }

                    i_17 = -1;
                } else {
                    i_19 = i_16;
                    i_18 += getGlyphWidth(i_17);
                    if (this.kerning != null && i_13 != -1) {
                        i_18 += this.kerning[i_13][i_17];
                    }

                    i_13 = i_17;
                }

                if (i_18 > 0) {
                    i_7 += i_18;
                    if (ints_2 != null) {
                        if (i_17 == 32) {
                            i_9 = i_16;
                            i_10 = i_7;
                            i_11 = bool5 ? 1 : 0;
                        }

                        if (i_7 > ints_2[i_14 < ints_2.length ? i_14 : ints_2.length - 1]) {
                            if (i_9 >= 0) {
                                areaNameBuffer[i_14] = areaName.substring(i_8, i_9 + 1 - i_11);
                                ++i_14;
                                if (i_14 >= areaNameBuffer.length) {
                                    return 0;
                                }

                                i_8 = i_9 + 1;
                                i_9 = -1;
                                i_7 -= i_10;
                            } else {
                                areaNameBuffer[i_14] = areaName.substring(i_8, i_19);
                                ++i_14;
                                if (i_14 >= areaNameBuffer.length) {
                                    return 0;
                                }

                                i_8 = i_19;
                                i_9 = -1;
                                i_7 = i_18;
                            }
                            i_13 = -1;
                        }

                        if (i_17 == 45) {
                            i_9 = i_16;
                            i_10 = i_7;
                            i_11 = 0;
                        }
                    }
                }
            }
        }

        if (areaName.length() > i_8) {
            areaNameBuffer[i_14] = areaName.substring(i_8);
            ++i_14;
        }

        return i_14;
    }

    public int getTextWidthMultiline(String text, int i_2, Sprite[] arr_3) {
        int i_5 = splitLinesToBuffer(text, new int[]{i_2}, aStringArray4980, arr_3);
        int i_6 = 0;

        for (int i_7 = 0; i_7 < i_5; i_7++) {
            int i_8 = getTextWidth(aStringArray4980[i_7], arr_3);
            if (i_8 > i_6) {
                i_6 = i_8;
            }
        }

        return i_6;
    }

    public Point method6954(String text, int width, int height, int index, Sprite[] arr_5) {
        int index1 = index;
        int height1 = height;
        if (index1 <= 0) {
            return new Point(0, height1 + this.topPadding);
        } else {
            if (index1 > text.length()) {
                index1 = text.length();
            }

            if (height1 == 0) {
                height1 = this.verticalSpacing;
            }

            int i_7 = splitLinesToBuffer(text, new int[]{width}, aStringArray4980, arr_5, false);
            int i_8 = 0;

            for (int i_9 = 0; i_9 < i_7; i_9++) {
                int i_10 = aStringArray4980[i_9].length();
                if (i_10 + i_8 > index1 || i_9 == i_7 - 1 && index1 == text.length()) {
                    String string_11 = text.substring(i_8, index1);
                    if (string_11.endsWith("<br>")) {
                        string_11 = "";
                        int i_10000 = i_8 + i_10;
                        ++i_9;
                    }

                    int i_12 = getTextWidth(string_11, arr_5);
                    int i_13 = this.topPadding + height1 * i_9;
                    return new Point(i_12, i_13);
                }

                i_8 += i_10;
            }

            return null;
        }
    }

    public int method6956(String string_1, int i_2, int i_3, int i_4, int i_5, Sprite[] arr_6) {
        int i_31 = i_3;
        int i_51 = i_5;
        if (i_31 == 0) {
            i_31 = this.verticalSpacing;
        }

        int i_8 = splitLinesToBuffer(string_1, new int[]{i_2}, aStringArray4980, arr_6);
        if (i_8 == 0) {
            return 0;
        } else {
            if (i_51 < 0) {
                i_51 = 0;
            }

            i_51 /= i_31;
            if (i_51 >= i_8) {
                i_51 = i_8 - 1;
            }

            String string_9 = aStringArray4980[i_51];
            int i_10 = 0;
            int i_11 = 0;

            int i_12;
            for (i_12 = 0; i_12 < i_4 && i_10 < string_9.length(); i_12 = getTextWidth(string_9.substring(0, i_10), arr_6)) {
                i_11 = i_12;
                ++i_10;
            }

            if (i_10 == string_9.length() && string_9.endsWith("<br>")) {
                i_10 -= 4;
            }

            if (i_4 - i_11 < i_12 - i_4) {
                --i_10;
            }

            for (int i_13 = 0; i_13 < i_51; i_13++) {
                i_10 += aStringArray4980[i_13].length();
            }

            return i_10;
        }
    }

    public String fitText(String string, int maximumWidth, Sprite[] icons) {
        if (getTextWidth(string, icons) <= maximumWidth) {
            return string;
        } else {
            maximumWidth -= getTextWidth("...", null);
            int escapeStart = -1;
            int previous = -1;
            int width = 0;
            int length = string.length();
            String prefix = "";

            for (int i = 0; i < length; i++) {
                char current = string.charAt(i);
                if (current == 60) {
                    escapeStart = i;
                } else {
                    if (current == 62 && escapeStart != -1) {
                        String string_12 = string.substring(escapeStart + 1, i);
                        escapeStart = -1;
                        if ("lt".equals(string_12)) {
                            current = 60;
                        } else if ("gt".equals(string_12)) {
                            current = 62;
                        } else if ("nbsp".equals(string_12)) {
                            current = 160;
                        } else if ("shy".equals(string_12)) {
                            current = 173;
                        } else if ("times".equals(string_12)) {
                            current = 215;
                        } else if ("euro".equals(string_12)) {
                            current = 8364;
                        } else if ("copy".equals(string_12)) {
                            current = 169;
                        } else {
                            if (!"reg".equals(string_12)) {
                                if (string_12.startsWith("img=") && icons != null) {
                                    try {
                                        int id = Utils.parseInt(string_12.substring(4));
                                        width += icons[id].getSpriteWidth();
                                        previous = -1;
                                        if (width > maximumWidth) {
                                            return prefix + "...";
                                        }

                                        prefix = string.substring(0, i + 1);
                                    } catch (Exception ignored) {
                                    }
                                }
                                continue;
                            }

                            current = 174;
                        }
                    }

                    if (escapeStart == -1) {
                        width += this.glyphWidths[Utils.byteForChar(current) & 0xff] & 0xff;
                        if (this.kerning != null && previous != -1) {
                            width += this.kerning[previous][current];
                        }

                        previous = current;
                        int total = width;
                        if (this.kerning != null) {
                            total = width + this.kerning[current][46];
                        }

                        if (total > maximumWidth) {
                            return prefix + "...";
                        }

                        prefix = string.substring(0, i + 1);
                    }
                }
            }

            return string;
        }
    }

    public int getTextHeightMultiline(String string_1, int i_2, int i_3, Sprite[] arr_4) {
        int i_31 = i_3;
        if (i_31 == 0) {
            i_31 = this.verticalSpacing;
        }

        int i_6 = splitLinesToBuffer(string_1, new int[]{i_2}, aStringArray4980, arr_4);
        int i_7 = i_31 * (i_6 - 1);
        return i_7 + this.bottomPadding + this.topPadding;
    }

    public int splitLinesToBuffer(String areaName, int[] ints_2, String[] areaNameBuffer, Sprite[] arr_4) {
        return splitLinesToBuffer(areaName, ints_2, areaNameBuffer, arr_4, true);
    }

	public static FontMetrics list(Js5ResourceProvider fontResource, int fileId) {
	    byte[] fontData = fontResource.getFile(fileId);
	    return fontData == null ? null : new FontMetrics(fontData);
	}

	public static FontMetrics getForMinimenu() {
	    FontMetrics fontmetrics1;
	    if (FontMetrics.useBoldFont) {
	        if (FontMetrics.aFontRenderer2348 != null && StaticMedia.aClass4142200 != null) {
	            fontmetrics1 = StaticMedia.aClass4142200;
	        } else {
	            fontmetrics1 = aClass4143438;
	        }
	    } else {
	        fontmetrics1 = aClass4143438;
	    }
	    MiniMenu.anInt178 = fontmetrics1.bottomPadding + fontmetrics1.topPadding;
	    return fontmetrics1;
	}

}
