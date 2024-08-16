package com.jagex.game.runetek5.graphics.sprite;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import com.jagex.game.runetek5.unidentified.ComponentDimensionSM;

public abstract class SpriteSM extends NativeSprite {

    static int anInt8878;
    static int anInt8879;
    static int anInt8880;
    static int anInt8852;
    static int anInt8882;
    public static int anInt8883;
    public static int anInt8884;
    static int anInt8885;
    static int anInt8850;
    static int anInt8870;
    static int anInt8865;
    static int anInt8868;
    static int anInt8869;
    static int anInt8876;
    public static int anInt8871;
    public static int anInt8872;
    static int anInt8873;
    static int anInt8874;
    static int anInt8858;
    static int anInt8866;
    public static int anInt8861;
    static int anInt8862;
    public static int anInt8863;
    static int anInt8864;
    static int anInt8877;
    int anInt8853;
    int anInt8854;
    int anInt8855;
    int anInt8856;
    public int[] cachedClipping;
    final SafeModeToolkit toolkit;
    final int anInt8851;
    final int anInt8867;

    SpriteSM(SafeModeToolkit hardwarerenderer1, int i_2, int i_3) {
        this.toolkit = hardwarerenderer1;
        this.anInt8851 = i_2;
        this.anInt8867 = i_3;
    }


    @Override
    public void setSize(int minX, int minY, int i_3, int i_4) {
        this.anInt8853 = minX;
        this.anInt8854 = minY;
        this.anInt8855 = i_3;
        this.anInt8856 = i_4;
    }


    @Override
    public int getWidth() {
        return this.anInt8851;
    }

    @Override
    public int getHeight() {
        return this.anInt8867;
    }

    abstract void method14245(int var1, int var2);

    @Override
    public int getSpriteHeight() {
        return this.anInt8854 + this.anInt8867 + this.anInt8856;
    }

    @Override
    public abstract void draw(int var1, int var2, int var3, int var4, int var5);

    @Override
    public abstract void method2773(int var1, int var2, ComponentSurface var3, int var4, int var5);

    public abstract void method14247(boolean colorWrite, boolean depthWrite, boolean transparent, int var4, int var5, float var6, int var7, int var8, int blendType, int color, int type, boolean var13);

    @Override
    public void method2772(int x, int y, int width, int height, int combineMode, int colorArgb, int blendMode) {
        if (this.toolkit.hasBeenShutdown()) {
            throw new IllegalStateException();
        } 
        if (this.cachedClipping == null) {
            this.cachedClipping = new int[4];
        }
        this.toolkit.getClipping(this.cachedClipping);
        this.toolkit.expandClipping(this.toolkit.clipLeft, this.toolkit.clipTop, width + x, y + height);
        int i_8 = getSpriteWidth();
        int i_9 = getSpriteHeight();
        int i_10 = (width + i_8 - 1) / i_8;
        int i_11 = (i_9 + height - 1) / i_9;

        for (int i_12 = 0; i_12 < i_11; i_12++) {
            int i_13 = i_12 * i_9;

            for (int i_14 = 0; i_14 < i_10; i_14++) {
                draw(i_14 * i_8 + x, i_13 + y, combineMode, colorArgb, blendMode);
            }
        }

        this.toolkit.setClipping(this.cachedClipping[0], this.cachedClipping[1], this.cachedClipping[2], this.cachedClipping[3]);
    }

    public boolean method14248(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6) {
        int i_7 = this.anInt8853 + this.anInt8851 + this.anInt8855;
        int i_8 = this.anInt8854 + this.anInt8867 + this.anInt8856;
        float f_9;
        float f_10;
        float f_11;
        float f_12;
        float f_13;
        float f_14;
        float f_15;
        float f_16;
        if (i_7 != this.anInt8851 || i_8 != this.anInt8867) {
            f_9 = (f_3 - f_1) / i_7;
            f_10 = (f_4 - f_2) / i_7;
            f_11 = (f_5 - f_1) / i_8;
            f_12 = (f_6 - f_2) / i_8;
            f_13 = f_11 * this.anInt8854;
            f_14 = f_12 * this.anInt8854;
            f_15 = f_9 * this.anInt8853;
            f_16 = f_10 * this.anInt8853;
            float f_17 = -f_9 * this.anInt8855;
            float f_18 = -f_10 * this.anInt8855;
            float f_19 = -f_11 * this.anInt8856;
            float f_20 = -f_12 * this.anInt8856;
            f_1 += f_15 + f_13;
            f_2 += f_16 + f_14;
            f_3 += f_17 + f_13;
            f_4 += f_18 + f_14;
            f_5 += f_15 + f_19;
            f_6 += f_16 + f_20;
        }

        f_9 = f_5 + (f_3 - f_1);
        f_10 = f_4 + (f_6 - f_2);
        if (f_1 < f_3) {
            f_11 = f_1;
            f_12 = f_3;
        } else {
            f_11 = f_3;
            f_12 = f_1;
        }

        if (f_5 < f_11) {
            f_11 = f_5;
        }

        if (f_9 < f_11) {
            f_11 = f_9;
        }

        if (f_5 > f_12) {
            f_12 = f_5;
        }

        if (f_9 > f_12) {
            f_12 = f_9;
        }

        if (f_2 < f_4) {
            f_13 = f_2;
            f_14 = f_4;
        } else {
            f_13 = f_4;
            f_14 = f_2;
        }

        if (f_6 < f_13) {
            f_13 = f_6;
        }

        if (f_10 < f_13) {
            f_13 = f_10;
        }

        if (f_6 > f_14) {
            f_14 = f_6;
        }

        if (f_10 > f_14) {
            f_14 = f_10;
        }

        if (f_11 < this.toolkit.clipLeft) {
            f_11 = this.toolkit.clipLeft;
        }

        if (f_12 > this.toolkit.clipRight) {
            f_12 = this.toolkit.clipRight;
        }

        if (f_13 < this.toolkit.clipTop) {
            f_13 = this.toolkit.clipTop;
        }

        if (f_14 > this.toolkit.clipBottom) {
            f_14 = this.toolkit.clipBottom;
        }

        f_12 = f_11 - f_12;
        if (f_12 >= 0.0F) {
            return false;
        } else {
            f_14 = f_13 - f_14;
            if (f_14 >= 0.0F) {
                return false;
            } else {
                anInt8850 = this.toolkit.viewportWidth;
                anInt8870 = (int) (((int) f_13 * anInt8850) + f_11);
                f_15 = (f_3 - f_1) * (f_6 - f_2) - (f_4 - f_2) * (f_5 - f_1);
                f_16 = (f_5 - f_1) * (f_4 - f_2) - (f_6 - f_2) * (f_3 - f_1);
                anInt8865 = (int) ((f_6 - f_2) * 4096.0F * this.anInt8851 / f_15);
                anInt8868 = (int) ((f_4 - f_2) * 4096.0F * this.anInt8867 / f_16);
                anInt8869 = (int) ((f_5 - f_1) * 4096.0F * this.anInt8851 / f_16);
                anInt8876 = (int) ((f_3 - f_1) * 4096.0F * this.anInt8867 / f_15);
                anInt8871 = (int) (f_11 * 16.0F + 8.0F - (f_1 + f_3 + f_5 + f_9) / 4.0F * 16.0F);
                anInt8872 = (int) (f_13 * 16.0F + 8.0F - (f_2 + f_4 + f_6 + f_10) / 4.0F * 16.0F);
                anInt8873 = (this.anInt8851 >> 1 << 12) + (anInt8872 * anInt8869 >> 4);
                anInt8874 = (this.anInt8867 >> 1 << 12) + (anInt8872 * anInt8876 >> 4);
                anInt8858 = anInt8871 * anInt8865 >> 4;
                anInt8866 = anInt8871 * anInt8868 >> 4;
                anInt8861 = (int) f_11;
                anInt8862 = (int) f_12;
                anInt8863 = (int) f_13;
                anInt8864 = (int) f_14;
                return true;
            }
        }
    }

    @Override
    public void method2764(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, int i_8, int i_9) {
        if (this.toolkit.hasBeenShutdown()) {
            throw new IllegalStateException();
        } else {
            if (method14248(f_1, f_2, f_3, f_4, f_5, f_6)) {
                anInt8877 = i_8;
                if (i_7 != 1) {
                    anInt8878 = i_8 >>> 24;
                    anInt8879 = 256 - anInt8878;
                    if (i_7 == 0) {
                        anInt8880 = (i_8 & 0xff0000) >> 16;
                        anInt8852 = (i_8 & 0xff00) >> 8;
                        anInt8882 = i_8 & 0xff;
                    } else if (i_7 == 2) {
                        anInt8883 = i_8 >>> 24;
                        anInt8884 = 256 - anInt8883;
                        int i_11 = (i_8 & 0xff00ff) * anInt8884 & -16711936;
                        int i_12 = (i_8 & 0xff00) * anInt8884 & 0xff0000;
                        anInt8885 = (i_11 | i_12) >>> 8;
                    }
                }

                if (i_7 == 1) {
                    if (i_9 == 0) {
                        method14245(1, 0);
                    } else if (i_9 == 1) {
                        method14245(1, 1);
                    } else if (i_9 == 2) {
                        method14245(1, 2);
                    }
                } else if (i_7 == 0) {
                    if (i_9 == 0) {
                        method14245(0, 0);
                    } else if (i_9 == 1) {
                        method14245(0, 1);
                    } else if (i_9 == 2) {
                        method14245(0, 2);
                    }
                } else if (i_7 == 3) {
                    if (i_9 == 0) {
                        method14245(3, 0);
                    } else if (i_9 == 1) {
                        method14245(3, 1);
                    } else if (i_9 == 2) {
                        method14245(3, 2);
                    }
                } else if (i_7 == 2) {
                    if (i_9 == 0) {
                        method14245(2, 0);
                    } else if (i_9 == 1) {
                        method14245(2, 1);
                    } else if (i_9 == 2) {
                        method14245(2, 2);
                    }
                }
            }

        }
    }

    @Override
    public int getSpriteWidth() {
        return this.anInt8853 + this.anInt8851 + this.anInt8855;
    }

    abstract void method14250(int[] var1, int[] var2, int var3, int var4);

    @Override
    public void cacheSizeInto(int[] ints_1) {
        ints_1[0] = this.anInt8853;
        ints_1[1] = this.anInt8854;
        ints_1[2] = this.anInt8855;
        ints_1[3] = this.anInt8856;
    }

    @Override
    public abstract void drawColoredCircle(int var1, int var2, int var3, int var4, int var5, int colorArgb, int blendMode);

    @Override
    public void method2766(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, ComponentSurface class4558, int i_9, int i_10) {
        if (this.toolkit.hasBeenShutdown()) {
            throw new IllegalStateException();
        } 
        if (method14248(f_1, f_2, f_3, f_4, f_5, f_6)) {
            ComponentDimensionSM packetsdecoder11 = (ComponentDimensionSM) class4558;
            method14250(packetsdecoder11.anIntArray9077, packetsdecoder11.anIntArray9078, anInt8861 - i_9, -i_10 - (anInt8864 - anInt8863));
        }

    }

}
