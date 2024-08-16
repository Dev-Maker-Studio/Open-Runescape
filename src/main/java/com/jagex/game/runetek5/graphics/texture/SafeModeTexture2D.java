package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.cache.node.Node;

public class SafeModeTexture2D extends Node<SafeModeTexture2D> {

    public static int[] colorsArgb;
    public int cycle;
    public boolean isAnimated;
    public int[] colorArgb;
    public final int materialId;
    public final int totalColorCount;

    public SafeModeTexture2D(int idMaterial, int numColors, int[] argbColor, boolean bool4) {
        this.materialId = idMaterial;
        this.totalColorCount = numColors;
        this.colorArgb = argbColor;
        if (bool4) {
            int[] a = new int[this.totalColorCount];
            int[] r = new int[this.totalColorCount];
            int[] g = new int[this.totalColorCount];
            int[] b = new int[this.totalColorCount];
            if (colorsArgb == null || colorsArgb.length != this.colorArgb.length) {
                colorsArgb = new int[this.colorArgb.length];
            }

            int startCount = this.totalColorCount;
            int endCount = this.totalColorCount;
            int start = startCount - 1;
            int end = endCount - 1;
            int initialColorIndex = startCount * endCount;
            int primaryColorIndex = startCount;
            int secondaryColorIndex = startCount;

            int startColorIndex;
            int idx;
            int secondary;
            for (startColorIndex = 2; startColorIndex >= 0; --startColorIndex) {
                for (idx = start; idx >= 0; --idx) {
                    secondary = this.colorArgb[--primaryColorIndex];
                    a[idx] += secondary >> 24 & 0xff;
                    r[idx] += secondary >> 16 & 0xff;
                    g[idx] += secondary >> 8 & 0xff;
                    b[idx] += secondary & 0xff;
                }

                if (primaryColorIndex == 0) {
                    primaryColorIndex = initialColorIndex;
                }
            }

            startColorIndex = initialColorIndex;

            for (idx = end; idx >= 0; --idx) {
                secondary = 1;
                int primary = 1;
                int sumGreen = 0;
                int sumBlue = 0;
                int sumRed = 0;
                int sumAlpha = 0;

                int indx;
                for (indx = 2; indx >= 0; --indx) {
                    sumAlpha += a[--primary];
                    sumRed += r[primary];
                    sumGreen += g[primary];
                    sumBlue += b[primary];
                    if (primary == 0) {
                        primary = startCount;
                    }
                }

                int calcedA;
                int calcedR;
                for (indx = start; indx >= 0; --indx) {
                    --primary;
                    --secondary;
                    calcedA = sumAlpha / 9;
                    calcedR = sumRed / 9;
                    int calcedG = sumGreen / 9;
                    int calcedB = sumBlue / 9;
                    --startColorIndex;
                    colorsArgb[startColorIndex] = calcedA << 24 | calcedR << 16 | calcedG << 8 | calcedB;
                    sumAlpha += a[primary] - a[secondary];
                    sumRed += r[primary] - r[secondary];
                    sumBlue += b[primary] - b[secondary];
                    sumGreen += g[primary] - g[secondary];
                    if (primary == 0) {
                        primary = startCount;
                    }

                    if (secondary == 0) {
                        secondary = startCount;
                    }
                }

                for (indx = start; indx >= 0; --indx) {
                    calcedA = this.colorArgb[--primaryColorIndex];
                    calcedR = this.colorArgb[--secondaryColorIndex];
                    a[indx] += (calcedA >> 24 & 0xff) - (calcedR >> 24 & 0xff);
                    r[indx] += (calcedA >> 16 & 0xff) - (calcedR >> 16 & 0xff);
                    g[indx] += (calcedA >> 8 & 0xff) - (calcedR >> 8 & 0xff);
                    b[indx] += (calcedA & 0xff) - (calcedR & 0xff);
                }

                if (primaryColorIndex == 0) {
                    primaryColorIndex = initialColorIndex;
                }

                if (secondaryColorIndex == 0) {
                    secondaryColorIndex = initialColorIndex;
                }
            }

            int[] colorArgb = this.colorArgb;
            this.colorArgb = colorsArgb;
            colorsArgb = colorArgb;
        }

    }

    public void animate(int i_1, int i_2) {
        if (i_1 != 0 || i_2 != 0) {
            if (colorsArgb == null || colorsArgb.length != this.colorArgb.length) {
                colorsArgb = new int[this.colorArgb.length];
            }

            int colorCount = this.colorArgb.length;
            int mask = this.totalColorCount - 1;
            int i_6 = i_2 * this.totalColorCount;
            int lastIndex = colorCount - 1;

            for (int i_8 = 0; i_8 < colorCount; i_8 += this.totalColorCount) {
                int i_9 = i_8 + i_6 & lastIndex;

                for (int pos = 0; pos < this.totalColorCount; pos++) {
                    int dst = i_8 + pos;
                    int src = i_9 + (pos + i_1 & mask);
                    colorsArgb[dst] = this.colorArgb[src];
                }
            }

            int[] colorRgb = this.colorArgb;
            this.colorArgb = colorsArgb;
            colorsArgb = colorRgb;
        }

    }

    public int[] getArgbColor() {
        return this.colorArgb;
    }

}
