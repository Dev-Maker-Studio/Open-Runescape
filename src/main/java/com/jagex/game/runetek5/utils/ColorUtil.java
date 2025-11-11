package com.jagex.game.runetek5.utils;

import com.jagex.game.runetek5.config.flo.FloType;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

/**
 * 
 * @author Brainpower - 28/01/2023
 * Reference: https://www.rune-server.ee/runescape-development/rs2-client/tools/589900-rs2-hsb-color-picker.html
 *
 */
public class ColorUtil {

    public static int[] hsvToRgbTable;
    public static int hslToRgbUsers;
    public static int hsvToRgbUsers;
	public static int[] hslToRgb;
	public static int[] unsignedHslToRgb;

    ColorUtil() {
        throw new Error();
    }
    
    public static int getOverlayColorHsl(AbstractToolkit drawingarea, FloType overlay) {
        if (overlay.minimapColorHsl != -1) {
            return overlay.minimapColorHsl;
        } 
        if (overlay.texture != -1) {
            MaterialType material = drawingarea.materialProvider.getMaterial(overlay.texture);
            if (!material.detailsOnly) {
                return material.colorHsl;
            }
        }
        return overlay.tileColorHsl;
    }
    
    public static int blend(int colorA, int colorB, int i_2) {
        if (colorA == colorB) {
            return colorA;
        } else {
            int i_4 = 128 - i_2;
            int i_5 = i_4 * (colorA & 0x7f) + (colorB & 0x7f) * i_2 >> 7;
            int i_6 = i_4 * (colorA & 0x380) + i_2 * (colorB & 0x380) >> 7;
            int i_7 = (colorB & 0xfc00) * i_2 + i_4 * (colorA & 0xfc00) >> 7;
            return i_7 & 0xfc00 | i_6 & 0x380 | i_5 & 0x7f;
        }
    }

    public static int hsl16to24(int hue, int saturation, int lightness) {
        if (lightness > 243) {
            saturation >>= 4;
        } else if (lightness > 217) {
            saturation >>= 3;
        } else if (lightness > 192) {
            saturation >>= 2;
        } else if (lightness > 179) {
            saturation >>= 1;
        }
        return ((hue & 0xff) >> 2 << 10) + (lightness >> 1) + (saturation >> 5 << 7);
    }

    public static void calculateHsvToRgbTable() {
        if (ColorUtil.hsvToRgbTable == null) {
            ColorUtil.hsvToRgbTable = new int[65536];
            double d_1 = 0.7D + (Math.random() * 0.03D - 0.015D);
            int i_3 = 0;
            for (int i_4 = 0; i_4 < 512; i_4++) {
                float f_5 = 360.0F * ((i_4 >> 3) / 64.0F + 0.0078125F);
                float f_6 = (i_4 & 0x7) / 8.0F + 0.0625F;
                for (int i_7 = 0; i_7 < 128; i_7++) {
                    float f_8 = i_7 / 128.0F;
                    float f_9 = 0.0F;
                    float f_10 = 0.0F;
                    float f_11 = 0.0F;
                    float f_12 = f_5 / 60.0F;
                    int i_13 = (int) f_12;
                    int i_14 = i_13 % 6;
                    float f_15 = f_12 - i_13;
                    float f_16 = f_8 * (1.0F - f_6);
                    float f_17 = f_8 * (1.0F - f_6 * f_15);
                    float f_18 = (1.0F - f_6 * (1.0F - f_15)) * f_8;
                    if (i_14 == 0) {
                        f_9 = f_8;
                        f_10 = f_18;
                        f_11 = f_16;
                    } else if (i_14 == 1) {
                        f_9 = f_17;
                        f_10 = f_8;
                        f_11 = f_16;
                    } else if (i_14 == 2) {
                        f_9 = f_16;
                        f_10 = f_8;
                        f_11 = f_18;
                    } else if (i_14 == 3) {
                        f_9 = f_16;
                        f_10 = f_17;
                        f_11 = f_8;
                    } else if (i_14 == 4) {
                        f_9 = f_18;
                        f_10 = f_16;
                        f_11 = f_8;
                    } else if (i_14 == 5) {
                        f_9 = f_8;
                        f_10 = f_16;
                        f_11 = f_17;
                    }
                    f_9 = (float) Math.pow(f_9, d_1);
                    f_10 = (float) Math.pow(f_10, d_1);
                    f_11 = (float) Math.pow(f_11, d_1);
                    int i_19 = (int) (f_9 * 256.0F);
                    int i_20 = (int) (256.0F * f_10);
                    int i_21 = (int) (256.0F * f_11);
                    int i_22 = i_21 + (i_20 << 8) + (i_19 << 16) + ~0xffffff;
                    ColorUtil.hsvToRgbTable[i_3++] = i_22;
                }
            }
        }
    }

    public static void calculateHslToRgbTable() {
        if (ColorUtil.hslToRgb == null) {
            ColorUtil.hslToRgb = new int[65536];
            ColorUtil.unsignedHslToRgb = new int[65536];
            double d_1 = 0.7D + (Math.random() * 0.03D - 0.015D);
            for (int i_3 = 0; i_3 < 65536; i_3++) {
                double d_4 = (i_3 >> 10 & 0x3f) / 64.0D + 0.0078125D;
                double d_6 = 0.0625D + (i_3 >> 7 & 0x7) / 8.0D;
                double d_8 = (i_3 & 0x7f) / 128.0D;
                double d_10 = d_8;
                double d_12 = d_8;
                double d_14 = d_8;
                if (d_6 != 0.0D) {
                    double d_16;
                    if (d_8 < 0.5D) {
                        d_16 = d_8 * (d_6 + 1.0D);
                    } else {
                        d_16 = d_6 + d_8 - d_8 * d_6;
                    }
                    double d_18 = d_8 * 2.0D - d_16;
                    double d_20 = 0.3333333333333333D + d_4;
                    if (d_20 > 1.0D) {
                        --d_20;
                    }
                    double d_24 = d_4 - 0.3333333333333333D;
                    if (d_24 < 0.0D) {
                        ++d_24;
                    }
                    if (d_20 * 6.0D < 1.0D) {
                        d_10 = d_18 + (d_16 - d_18) * 6.0D * d_20;
                    } else if (2.0D * d_20 < 1.0D) {
                        d_10 = d_16;
                    } else if (3.0D * d_20 < 2.0D) {
                        d_10 = d_18 + 6.0D * (0.6666666666666666D - d_20) * (d_16 - d_18);
                    } else {
                        d_10 = d_18;
                    }
                    if (6.0D * d_4 < 1.0D) {
                        d_12 = d_18 + 6.0D * (d_16 - d_18) * d_4;
                    } else if (d_4 * 2.0D < 1.0D) {
                        d_12 = d_16;
                    } else if (3.0D * d_4 < 2.0D) {
                        d_12 = 6.0D * (0.6666666666666666D - d_4) * (d_16 - d_18) + d_18;
                    } else {
                        d_12 = d_18;
                    }
                    if (d_24 * 6.0D < 1.0D) {
                        d_14 = d_24 * 6.0D * (d_16 - d_18) + d_18;
                    } else if (d_24 * 2.0D < 1.0D) {
                        d_14 = d_16;
                    } else if (d_24 * 3.0D < 2.0D) {
                        d_14 = 6.0D * (0.6666666666666666D - d_24) * (d_16 - d_18) + d_18;
                    } else {
                        d_14 = d_18;
                    }
                }
                d_10 = Math.pow(d_10, d_1);
                d_12 = Math.pow(d_12, d_1);
                d_14 = Math.pow(d_14, d_1);
                int i_26 = (int) (d_10 * 256.0D);
                int i_17 = (int) (d_12 * 256.0D);
                int i_27 = (int) (256.0D * d_14);
                int i_19 = i_27 + (i_17 << 8) + (i_26 << 16);
                ColorUtil.hslToRgb[i_3] = i_19 & 0xffffff;
                int i_28 = i_26 + (i_17 << 8) + (i_27 << 16);
                ColorUtil.unsignedHslToRgb[i_3] = i_28;
            }
        }
    }

    public static void initializeTables(boolean hsvColorTable, boolean hslColorTable) {
        if (hsvColorTable) {
            hsvToRgbUsers++;
            calculateHsvToRgbTable();
        }
        if (hslColorTable) {
            hslToRgbUsers++;
            calculateHslToRgbTable();
        }
    }

    public static int blendMaterialColor(int materialColor, int i_1) {
        int i_3 = i_1 >>> 24;
        int i_4 = 255 - i_3;
        i_1 = (i_3 * (i_1 & 0xff00ff) & -16711936 | i_3 * (i_1 & 0xff00) & 0xff0000) >>> 8;
        return ((i_4 * (materialColor & 0xff00ff) & -16711936 | (materialColor & 0xff00) * i_4 & 0xff0000) >>> 8) + i_1;
    }

    public static int mixColorsWithTransparency(int a, int b, float alpha) {
        return ColorUtil.blendColorsWithAlpha(a, b, (int) alpha);
    }
    
	public static int blendColorsWithAlpha(int color, int colorB, int alpha) {
	    int transparency = 255 - alpha;
	    colorB = ((colorB & 0xff00ff) * alpha & -16711936 | (colorB & 0xff00) * alpha & 0xff0000) >>> 8;
	    return ((transparency * (color & 0xff00ff) & -16711936 | (color & 0xff00) * transparency & 0xff0000) >>> 8) + colorB;
	}

	public static void resetTables(boolean hsvToRgb, boolean hslToRgb) {
	    if (hsvToRgb) {
	        if (--hsvToRgbUsers == 0) {
	            hsvToRgbTable = null;
	        }
	    }
	    if (hslToRgb) {
	        if (--hslToRgbUsers == 0) {
	            ColorUtil.hslToRgb = null;
	        }
	    }
	}

	public static int rgbToHsl24(int i_0) {
	    double d_2 = (i_0 >> 16 & 0xff) / 256.0D;
	    double d_4 = (i_0 >> 8 & 0xff) / 256.0D;
	    double d_6 = (i_0 & 0xff) / 256.0D;
	    double d_8 = d_2;
	    if (d_4 < d_2) {
	        d_8 = d_4;
	    }
	    if (d_6 < d_8) {
	        d_8 = d_6;
	    }
	    double d_10 = d_2;
	    if (d_4 > d_2) {
	        d_10 = d_4;
	    }
	    if (d_6 > d_10) {
	        d_10 = d_6;
	    }
	    double d_12 = 0.0D;
	    double d_14 = 0.0D;
	    double d_16 = (d_10 + d_8) / 2.0D;
	    if (d_10 != d_8) {
	        if (d_16 < 0.5D) {
	            d_14 = (d_10 - d_8) / (d_8 + d_10);
	        }
	        if (d_16 >= 0.5D) {
	            d_14 = (d_10 - d_8) / (2.0D - d_10 - d_8);
	        }
	        if (d_2 == d_10) {
	            d_12 = (d_4 - d_6) / (d_10 - d_8);
	        } else if (d_4 == d_10) {
	            d_12 = 2.0D + (d_6 - d_2) / (d_10 - d_8);
	        } else if (d_10 == d_6) {
	            d_12 = 4.0D + (d_2 - d_4) / (d_10 - d_8);
	        }
	    }
	    d_12 /= 6.0D;
	    int i_18 = (int) (256.0D * d_12);
	    int i_19 = (int) (d_14 * 256.0D);
	    int i_20 = (int) (d_16 * 256.0D);
	    if (i_19 < 0) {
	        i_19 = 0;
	    } else if (i_19 > 255) {
	        i_19 = 255;
	    }
	    if (i_20 < 0) {
	        i_20 = 0;
	    } else if (i_20 > 255) {
	        i_20 = 255;
	    }
	    if (i_20 > 243) {
	        i_19 >>= 4;
	    } else if (i_20 > 217) {
	        i_19 >>= 3;
	    } else if (i_20 > 192) {
	        i_19 >>= 2;
	    } else if (i_20 > 179) {
	        i_19 >>= 1;
	    }
	    return ((i_18 & 0xff) >> 2 << 10) + (i_20 >> 1) + (i_19 >> 5 << 7);
	}

	public static short adjustHsv(int i_0) {
	    int i_2 = i_0 >> 10 & 0x3f;
	    int i_3 = i_0 >> 3 & 0x70;
	    int i_4 = i_0 & 0x7f;
	    i_3 = i_4 <= 64 ? i_4 * i_3 >> 7 : i_3 * (127 - i_4) >> 7;
	    int i_5 = i_3 + i_4;
	    int i_6;
	    if (i_5 != 0) {
	        i_6 = (i_3 << 8) / i_5;
	    } else {
	        i_6 = i_3 << 1;
	    }
	
	    return (short) (i_2 << 10 | i_6 >> 4 << 7 | i_5);
	}

	public static int repackHsl(int hsl, int light) {
	    int Light = light;
	    Light = (hsl & 0x7f) * Light >> 7;
	    if (Light < 2) {
	        Light = 2;
	    } else if (Light > 126) {
	        Light = 126;
	    }
	
	    return (hsl & 0xff80) + Light;
	}

	public static int brighten(int color, int multiplier) {
	    int red = (color & 0xff0000) * multiplier >> 23;
	    if (red < 2) {
	        red = 2;
	    } else if (red > 253) {
	        red = 253;
	    }
	
	    int green = (color & 0xff00) * multiplier >> 15;
	    if (green < 2) {
	        green = 2;
	    } else if (green > 253) {
	        green = 253;
	    }
	
	    int blue = (color & 0xff) * multiplier >> 7;
	    if (blue < 2) {
	        blue = 2;
	    } else if (blue > 253) {
	        blue = 253;
	    }
	
	    return red << 16 | green << 8 | blue;
	}

	public static int method14677(int i_0, int i_1) {
	    int i_11 = i_1;
	    if (i_0 == -1) {
	        return 12345678;
	    } else {
	        i_11 = (i_0 & 0x7f) * i_11 >> 7;
	        if (i_11 < 2) {
	            i_11 = 2;
	        } else if (i_11 > 126) {
	            i_11 = 126;
	        }
	
	        return (i_0 & 0xff80) + i_11;
	    }
	}

	public static int method4162(int colorHsl, int lightness) {
	    if (colorHsl == -2) {
	        return 12345678;
	    } else if (colorHsl == -1) {
	        if (lightness < 2) {
	            lightness = 2;
	        } else if (lightness > 126) {
	            lightness = 126;
	        }
	        return lightness;
	    } else {
	        lightness = (colorHsl & 0x7f) * lightness >> 7;
	        if (lightness < 2) {
	            lightness = 2;
	        } else if (lightness > 126) {
	            lightness = 126;
	        }
	        return (colorHsl & 0xff80) + lightness;
	    }
	}
}
