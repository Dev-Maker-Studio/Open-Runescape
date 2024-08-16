package com.jagex.game.runetek5.config.material.texture;

public class TextureRaster {

    public static int[] PALETTE_SIN;
    public static int[] PALETTE_COS;
    public static int[] horizontalTable;
    public static int rowEnd;
    public static int currentWidth;
    public static int anInt3678;
    public static int[] verticalTable;
    public static int columnEnd;
    public static int currentHeight;
    TextureRaster() {
        throw new Error();
    }

    public static void caculateTable() {
        if (PALETTE_SIN == null || PALETTE_COS == null) {
            PALETTE_SIN = new int[256];
            PALETTE_COS = new int[256];
            for (int i_1 = 0; i_1 < 256; i_1++) {
                double d_2 = 6.283185307179586D * (i_1 / 255.0D);
                PALETTE_SIN[i_1] = (int) (Math.sin(d_2) * 4096.0D);
                PALETTE_COS[i_1] = (int) (Math.cos(d_2) * 4096.0D);
            }
        }
    }

    public static void setDimensions(int width, int height) {
        int idx;
        if (width != currentWidth) {
            horizontalTable = new int[width];
            for (idx = 0; idx < width; idx++) {
                horizontalTable[idx] = (idx << 12) / width;
            }
            rowEnd = width - 1;
            currentWidth = width;
            anInt3678 = width * 32;
        }
        if (currentHeight != height) {
            if (currentWidth != height) {
                verticalTable = new int[height];
                for (idx = 0; idx < height; idx++) {
                    verticalTable[idx] = (idx << 12) / height;
                }
            } else {
                verticalTable = horizontalTable;
            }
            columnEnd = height - 1;
            currentHeight = height;
        }
    }
}
