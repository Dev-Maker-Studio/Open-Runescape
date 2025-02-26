package com.jagex.game.runetek5.graphics.shadow;

import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer;

import jaggl.OpenGL;

public class ShadowMapperGL {

    public final OpenGlToolkit toolkit;
    public final GroundGL ground;
    public final int width;
    public final int length;
    public final byte[] shadowMapperData;
    public final int sizeShift;
    public final int sizeX;
    public final int sizeY;
    public ShadowTileGL[][] aClass124ArrayArray1585;

    public ShadowMapperGL(OpenGlToolkit toolkit, GroundGL ground) {
        this.toolkit = toolkit;
        this.ground = ground;
        this.width = (this.ground.width * this.ground.tileUnits >> this.toolkit.shadowScale) + 2;
        this.length = (this.ground.length * this.ground.tileUnits >> this.toolkit.shadowScale) + 2;
        this.shadowMapperData = new byte[this.width * this.length];
        this.sizeShift = this.toolkit.shadowScale + 7 - this.ground.tileScale;
        this.sizeX = this.ground.width >> this.sizeShift;
        this.sizeY = this.ground.length >> this.sizeShift;
    }

    public static void method2295(byte[] bytes_0, byte[] bytes_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        int i_8 = -(i_4 >> 2);
        i_4 = -(i_4 & 0x3);

        for (int i_9 = -i_5; i_9 < 0; i_9++) {
            int i_10001;
            int i_10;
            for (i_10 = i_8; i_10 < 0; i_10++) {
                i_10001 = i_3++;
                bytes_0[i_10001] += bytes_1[i_2++];
                i_10001 = i_3++;
                bytes_0[i_10001] += bytes_1[i_2++];
                i_10001 = i_3++;
                bytes_0[i_10001] += bytes_1[i_2++];
                i_10001 = i_3++;
                bytes_0[i_10001] += bytes_1[i_2++];
            }

            for (i_10 = i_4; i_10 < 0; i_10++) {
                i_10001 = i_3++;
                bytes_0[i_10001] += bytes_1[i_2++];
            }

            i_3 += i_6;
            i_2 += i_7;
        }

    }

    public static void method2296(byte[] dst, byte[] shade, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        int i_8 = -(i_4 >> 2);
        i_4 = -(i_4 & 0x3);

        for (int i_9 = -i_5; i_9 < 0; i_9++) {
            int i_10001;
            int i_10;
            for (i_10 = i_8; i_10 < 0; i_10++) {
                i_10001 = i_3++;
                dst[i_10001] -= shade[i_2++];
                i_10001 = i_3++;
                dst[i_10001] -= shade[i_2++];
                i_10001 = i_3++;
                dst[i_10001] -= shade[i_2++];
                i_10001 = i_3++;
                dst[i_10001] -= shade[i_2++];
            }

            for (i_10 = i_4; i_10 < 0; i_10++) {
                i_10001 = i_3++;
                dst[i_10001] -= shade[i_2++];
            }

            i_3 += i_6;
            i_2 += i_7;
        }

    }

    public static boolean method2297(byte[] bytes_0, int i_1, int i_2, int i_3, int i_4, int i_5) {
        int i_6 = i_2 % i_5;
        int i_7;
        if (i_6 != 0) {
            i_7 = i_5 - i_6;
        } else {
            i_7 = 0;
        }

        int i_8 = -((i_3 + i_5 - 1) / i_5);
        int i_9 = -((i_5 + i_2 - 1) / i_5);

        for (int i_10 = i_8; i_10 < 0; i_10++) {
            for (int i_11 = i_9; i_11 < 0; i_11++) {
                if (bytes_0[i_1] == 0) {
                    return true;
                }

                i_1 += i_5;
            }

            i_1 -= i_7;
            if (bytes_0[i_1 - 1] == 0) {
                return true;
            }

            i_1 += i_4;
        }

        return false;
    }

    public void method2290(int i_1, int i_2, int i_3, boolean[][] bools_4, boolean bool5) {
        this.toolkit.setLightingEnabled(false);
        this.toolkit.setDepthMask(false);
        this.toolkit.renderTexture(-2);
        this.toolkit.loadTextureCombineMode(1);
        this.toolkit.setBlendMode(1);
        float f_6 = 1.0F / (this.toolkit.openglFlags * 128);
        int i_7;
        int i_8;
        int i_9;
        int i_10;
        int i_11;
        int i_12;
        int i_13;
        if (bool5) {
            for (i_7 = 0; i_7 < this.sizeY; i_7++) {
                i_8 = i_7 << this.sizeShift;
                i_9 = i_7 + 1 << this.sizeShift;

                label136:
                for (i_10 = 0; i_10 < this.sizeX; i_10++) {
                    i_11 = i_10 << this.sizeShift;
                    i_12 = i_10 + 1 << this.sizeShift;

                    for (i_13 = i_11; i_13 < i_12; i_13++) {
                        if (i_13 - i_1 >= -i_3 && i_13 - i_1 <= i_3) {
                            for (int i_20 = i_8; i_20 < i_9; i_20++) {
                                if (i_20 - i_2 >= -i_3 && i_20 - i_2 <= i_3 && bools_4[i_3 + (i_13 - i_1)][i_20 - i_2 + i_3]) {
                                    OpenGL.glMatrixMode(5890);
                                    OpenGL.glLoadIdentity();
                                    OpenGL.glScalef(f_6, f_6, 1.0F);
                                    OpenGL.glTranslatef((-i_10) / f_6, (-i_7) / f_6, 1.0F);
                                    OpenGL.glMatrixMode(5888);
                                    this.aClass124ArrayArray1585[i_10][i_7].method2155();
                                    continue label136;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (i_7 = 0; i_7 < this.sizeY; i_7++) {
                i_8 = i_7 << this.sizeShift;
                i_9 = i_7 + 1 << this.sizeShift;

                for (i_10 = 0; i_10 < this.sizeX; i_10++) {
                    i_11 = 0;
                    i_12 = i_10 << this.sizeShift;
                    i_13 = i_10 + 1 << this.sizeShift;
                    GlBitBuffer class282Sub35Sub114 = this.toolkit.textureBuffer;
                    class282Sub35Sub114.currentPosition = 0;

                    for (int i_15 = i_8; i_15 < i_9; i_15++) {
                        if (i_15 - i_2 >= -i_3 && i_15 - i_2 <= i_3) {
                            int i_16 = i_12 + this.ground.width * i_15;

                            for (int i_17 = i_12; i_17 < i_13; i_17++) {
                                if (i_17 - i_1 >= -i_3 && i_17 - i_1 <= i_3 && bools_4[i_3 + (i_17 - i_1)][i_15 - i_2 + i_3]) {
                                    short[] shorts_18 = this.ground.materialIndices[i_16];
                                    if (shorts_18 != null) {
                                        int i_19;
                                        if (this.toolkit.bigEndianess) {
                                            for (i_19 = 0; i_19 < shorts_18.length; i_19++) {
                                                class282Sub35Sub114.writeShort(shorts_18[i_19] & 0xffff);
                                                ++i_11;
                                            }
                                        } else {
                                            for (i_19 = 0; i_19 < shorts_18.length; i_19++) {
                                                class282Sub35Sub114.writeShortLittle(shorts_18[i_19] & 0xffff);
                                                ++i_11;
                                            }
                                        }
                                    }
                                }

                                ++i_16;
                            }
                        }
                    }

                    if (i_11 > 0) {
                        OpenGL.glMatrixMode(5890);
                        OpenGL.glLoadIdentity();
                        OpenGL.glScalef(f_6, f_6, 1.0F);
                        OpenGL.glTranslatef((-i_10) / f_6, (-i_7) / f_6, 1.0F);
                        OpenGL.glMatrixMode(5888);
                        this.aClass124ArrayArray1585[i_10][i_7].method2156(class282Sub35Sub114.buffer, i_11);
                    }
                }
            }
        }

        OpenGL.glMatrixMode(5890); //GL_TEXTURE
        OpenGL.glLoadIdentity();
        OpenGL.glMatrixMode(5888); //GL_MODELVIEW
    }

    public boolean method2293(Shadow class282Sub50Sub171, int i_2, int i_3) {
        int i_31 = i_3;
        int i_21 = i_2;
        ShadowGL class282Sub50Sub17Sub14 = (ShadowGL) class282Sub50Sub171;
        i_21 += class282Sub50Sub17Sub14.startX + 1;
        i_31 += class282Sub50Sub17Sub14.startY + 1;
        int i_5 = i_21 + i_31 * this.width;
        int i_6 = class282Sub50Sub17Sub14.sizeY;
        int i_7 = class282Sub50Sub17Sub14.sizeX;
        int i_8 = this.width - i_7;
        int i_9;
        if (i_31 <= 0) {
            i_9 = 1 - i_31;
            i_6 -= i_9;
            i_5 += i_9 * this.width;
            i_31 = 1;
        }

        if (i_31 + i_6 >= this.length) {
            i_9 = i_31 + i_6 + 1 - this.length;
            i_6 -= i_9;
        }

        if (i_21 <= 0) {
            i_9 = 1 - i_21;
            i_7 -= i_9;
            i_5 += i_9;
            i_8 += i_9;
            i_21 = 1;
        }

        if (i_7 + i_21 >= this.width) {
            i_9 = i_7 + i_21 + 1 - this.width;
            i_7 -= i_9;
            i_8 += i_9;
        }

        if (i_7 > 0 && i_6 > 0) {
            byte b_10 = 8;
            i_8 += (b_10 - 1) * this.width;
            return method2297(this.shadowMapperData, i_5, i_7, i_6, i_8, b_10);
        } else {
            return false;
        }
    }

    public void method2298(int i_1, int i_2, int i_3, int i_4) {
        if (this.aClass124ArrayArray1585 != null) {
            int i_5 = i_1 - 1 >> 7;
            int i_6 = i_3 + (i_1 - 1) - 1 >> 7;
            int i_7 = i_2 - 1 >> 7;
            int i_8 = i_2 - 1 + i_4 - 1 >> 7;

            for (int i_9 = i_5; i_9 <= i_6; i_9++) {
                ShadowTileGL[] arr_10 = this.aClass124ArrayArray1585[i_9];

                for (int i_11 = i_7; i_11 <= i_8; i_11++) {
                    arr_10[i_11].needsTextureRegeneration = true;
                }
            }
        }

    }

    public void method2309() {
        this.aClass124ArrayArray1585 = new ShadowTileGL[this.sizeX][this.sizeY];

        for (int y = 0; y < this.sizeY; y++) {
            for (int x = 0; x < this.sizeX; x++) {
                this.aClass124ArrayArray1585[x][y] = new ShadowTileGL(this.toolkit, this, this.ground, x, y, this.sizeShift, x * 128 + 1, y * 128 + 1);
            }
        }

    }

    public void addShadow(Shadow class282Sub50Sub171, int i_2, int i_3) {
        int i_31 = i_3;
        int i_21 = i_2;
        ShadowGL shadow = (ShadowGL) class282Sub50Sub171;
        i_21 += shadow.startX + 1;
        i_31 += shadow.startY + 1;
        int i_5 = i_21 + i_31 * this.width;
        int i_6 = 0;
        int i_7 = shadow.sizeY;
        int i_8 = shadow.sizeX;
        int i_9 = this.width - i_8;
        int i_10 = 0;
        int i_11;
        if (i_31 <= 0) {
            i_11 = 1 - i_31;
            i_7 -= i_11;
            i_6 += i_11 * i_8;
            i_5 += i_11 * this.width;
            i_31 = 1;
        }

        if (i_31 + i_7 >= this.length) {
            i_11 = i_31 + i_7 + 1 - this.length;
            i_7 -= i_11;
        }

        if (i_21 <= 0) {
            i_11 = 1 - i_21;
            i_8 -= i_11;
            i_6 += i_11;
            i_5 += i_11;
            i_10 += i_11;
            i_9 += i_11;
            i_21 = 1;
        }

        if (i_8 + i_21 >= this.width) {
            i_11 = i_8 + i_21 + 1 - this.width;
            i_8 -= i_11;
            i_10 += i_11;
            i_9 += i_11;
        }

        if (i_8 > 0 && i_7 > 0) {
            method2296(this.shadowMapperData, shadow.shade, i_6, i_5, i_8, i_7, i_9, i_10);
            method2298(i_21, i_31, i_8, i_7);
        }

    }

    public void method2314(Shadow class282Sub50Sub171, int i_2, int i_3) {
        int i_31 = i_3;
        int i_21 = i_2;
        ShadowGL class282Sub50Sub17Sub14 = (ShadowGL) class282Sub50Sub171;
        i_21 += class282Sub50Sub17Sub14.startX + 1;
        i_31 += class282Sub50Sub17Sub14.startY + 1;
        int i_5 = i_21 + i_31 * this.width;
        int i_6 = 0;
        int i_7 = class282Sub50Sub17Sub14.sizeY;
        int i_8 = class282Sub50Sub17Sub14.sizeX;
        int i_9 = this.width - i_8;
        int i_10 = 0;
        int i_11;
        if (i_31 <= 0) {
            i_11 = 1 - i_31;
            i_7 -= i_11;
            i_6 += i_11 * i_8;
            i_5 += i_11 * this.width;
            i_31 = 1;
        }

        if (i_31 + i_7 >= this.length) {
            i_11 = i_31 + i_7 + 1 - this.length;
            i_7 -= i_11;
        }

        if (i_21 <= 0) {
            i_11 = 1 - i_21;
            i_8 -= i_11;
            i_6 += i_11;
            i_5 += i_11;
            i_10 += i_11;
            i_9 += i_11;
            i_21 = 1;
        }

        if (i_8 + i_21 >= this.width) {
            i_11 = i_8 + i_21 + 1 - this.width;
            i_8 -= i_11;
            i_10 += i_11;
            i_9 += i_11;
        }

        if (i_8 > 0 && i_7 > 0) {
            method2295(this.shadowMapperData, class282Sub50Sub17Sub14.shade, i_6, i_5, i_8, i_7, i_9, i_10);
            method2298(i_21, i_31, i_8, i_7);
        }

    }

}
