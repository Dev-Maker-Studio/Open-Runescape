package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpRasterizer extends TextureOperation {

    public Class149[] aClass149Array9862;
	public static int anInt7071;
	public static int anInt7069 = 100;
	public static int anInt7070;
	public static int anInt7068 = 100;

    public TextureOpRasterizer() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            method15372(super.monochromeImageCache.method5722());
        }
        return ints_3;
    }

    @Override
    public int[][] getColorOutput(int i_1) {
        int[][] ints_3 = super.colorImageCache.getPalette(i_1);
        if (super.colorImageCache.dirty) {
            int i_4 = TextureRaster.currentWidth;
            int i_5 = TextureRaster.currentHeight;
            int[][] ints_6 = new int[i_5][i_4];
            int[][][] ints_7 = super.colorImageCache.method5464();
            method15372(ints_6);
            for (int i_8 = 0; i_8 < TextureRaster.currentHeight; i_8++) {
                int[] ints_9 = ints_6[i_8];
                int[][] ints_10 = ints_7[i_8];
                int[] ints_11 = ints_10[0];
                int[] ints_12 = ints_10[1];
                int[] ints_13 = ints_10[2];
                for (int i_14 = 0; i_14 < TextureRaster.currentWidth; i_14++) {
                    int i_15 = ints_9[i_14];
                    ints_13[i_14] = (i_15 & 0xff) << 4;
                    ints_12[i_14] = (i_15 & 0xff00) >> 4;
                    ints_11[i_14] = (i_15 & 0xff0000) >> 12;
                }
            }
        }
        return ints_3;
    }

    public void method15372(int[][] ints_1) {
        int i_3 = TextureRaster.currentWidth;
        int i_4 = TextureRaster.currentHeight;
        StaticMethods.method3936(ints_1);
        TextureOpRasterizer.method16086(TextureRaster.rowEnd, TextureRaster.columnEnd);
        if (this.aClass149Array9862 != null) {
            for (int i_5 = 0; i_5 < this.aClass149Array9862.length; i_5++) {
                Class149 class1496 = this.aClass149Array9862[i_5];
                int i_7 = class1496.anInt1743;
                int i_8 = class1496.anInt1741;
                if (i_7 >= 0) {
                    if (i_8 >= 0) {
                        class1496.method2557(i_3, i_4);
                    } else {
                        class1496.method2556(i_3, i_4);
                    }
                } else if (i_8 >= 0) {
                    class1496.method2561(i_3, i_4);
                }
            }
        }
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        if (i_1 == 0) {
            this.aClass149Array9862 = new Class149[rsbytebuffer2.readUByte()];
            for (int i_4 = 0; i_4 < this.aClass149Array9862.length; i_4++) {
                int i_5 = rsbytebuffer2.readUByte();
                switch (i_5) {
                    case 0:
                        this.aClass149Array9862[i_4] = Class149_Sub3.decode(rsbytebuffer2);
                        break;
                    case 1:
                        this.aClass149Array9862[i_4] = Class149_Sub2.decode(rsbytebuffer2);
                        break;
                    case 2:
                        this.aClass149Array9862[i_4] = Class149_Sub4.decode(rsbytebuffer2);
                        break;
                    case 3:
                        this.aClass149Array9862[i_4] = Class149_Sub1.decode(rsbytebuffer2);
                }
            }
        } else if (i_1 == 1) {
            super.monochrome = rsbytebuffer2.readUByte() == 1;
        }
    }

	public static void method16086(int i_2, int i_3) {
	    TextureOpRasterizer.anInt7071 = 0;
	    TextureOpRasterizer.anInt7069 = i_2;
	    TextureOpRasterizer.anInt7070 = 0;
	    TextureOpRasterizer.anInt7068 = i_3;
	}
}
