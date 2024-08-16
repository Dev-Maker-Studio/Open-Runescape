package com.jagex.game.runetek5.graphics.shadow;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.scene.ground.GroundDX;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

class ShadowTileDX {

    public static int[] pixels;
    public static byte[] aByteArray722;
    boolean needsUpdate = true;
    public int anInt715 = -1;
    public NativeTextureSampler textureSampler;
    public final AbstractNativeToolkit toolkit;
    public final ShadowMapperDX aClass74711;
    public final int anInt712;
    public final int anInt718;
    final int anInt716;
    public int anInt709;
    public int anInt719;
    public DirectXBuffer anInterface32717;

    ShadowTileDX(AbstractNativeToolkit toolkit, ShadowMapperDX class742, GroundDX ground, int i_4, int i_5, int i_6, int i_7, int i_8) {
        this.toolkit = toolkit;
        this.aClass74711 = class742;
        this.anInt712 = i_7;
        this.anInt718 = i_8;
        int i_9 = 1 << i_6;
        int i_10 = 0;
        int i_11 = i_4 << i_6;
        int i_12 = i_5 << i_6;

        int i_14;
        int i_15;
        for (int i_13 = 0; i_13 < i_9; i_13++) {
            i_14 = ground.width * (i_12 + i_13) + i_11;

            for (i_15 = 0; i_15 < i_9; i_15++) {
                short[] shorts_21 = ground.tileVertices[i_14++];
                if (shorts_21 != null) {
                    i_10 += shorts_21.length;
                }
            }
        }

        if (i_10 > 0) {
        	this.anInt709 = Integer.MIN_VALUE;
        	this.anInt719 = Integer.MAX_VALUE;
        	this.anInterface32717 = toolkit.method13993(false);
        	this.anInterface32717.create(i_10);
            ByteBuffer buffer = toolkit.directBuffer;
            buffer.clear();

            for (i_14 = 0; i_14 < i_9; i_14++) {
                i_15 = ground.width * (i_12 + i_14) + i_11;

                for (int i_16 = 0; i_16 < i_9; i_16++) {
                    short[] shorts_17 = ground.tileVertices[i_15++];
                    if (shorts_17 != null) {
                        for (int i_18 = 0; i_18 < shorts_17.length; i_18++) {
                            int i_19 = shorts_17[i_18] & 0xffff;
                            if (i_19 < this.anInt719) {
                            	this.anInt719 = i_19;
                            }

                            if (i_19 > this.anInt709) {
                            	this.anInt709 = i_19;
                            }

                            buffer.putShort((short) i_19);
                        }
                    }
                }
            }

            this.anInterface32717.upload(0, buffer.position(), this.toolkit.directBufferAddress);
            this.anInt716 = i_10 / 3;
        } else {
        	this.anInt716 = 0;
        	this.textureSampler = null;
        }

    }

    void method1288(AbstractEnvironmentShader class481) {
        method1290(class481, this.anInterface32717, 0, this.anInt716);
    }

    void method1290(AbstractEnvironmentShader environmentShader, DirectXBuffer buffer, int i_3, int i_4) {
        if (i_4 > 0) {
            method1293();
            this.toolkit.method13997(buffer);
            environmentShader.textureSampler = this.textureSampler;
            environmentShader.anInt467 = this.anInt719;
            environmentShader.anInt468 = this.anInt709 - this.anInt719 + 1;
            environmentShader.anInt469 = i_3;
            environmentShader.anInt470 = i_4;
            environmentShader.loadStandartPointlight(0);
        }

    }

    public void method1293() {
        if (this.needsUpdate) {
            this.needsUpdate = false;
            byte[] shadowedPixel = this.aClass74711.shadowMapperData;
            int i_2 = 0;
            int i_3 = this.aClass74711.width;
            int i_4 = this.anInt712 + this.aClass74711.width * this.anInt718;


            for (int x = -128; x < 0; x++) {
                i_2 = (i_2 << 8) - i_2;

                for (int i_6 = -128; i_6 < 0; i_6++) {
                    if (shadowedPixel[i_4++] != 0) {
                        ++i_2;
                    }
                }

                i_4 += i_3 - 128;
            }

            if (this.textureSampler != null && i_2 == this.anInt715) {
            } else {
                this.anInt715 = i_2;
                int i_5 = 0;
                i_4 = i_3 * this.anInt718 + this.anInt712;
                int x;
                int y;
                int i_9;
                if (this.toolkit.method13954(GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT)) {
                    if (aByteArray722 == null) {
                        aByteArray722 = new byte[16384 /*128*128*/]; 
                    }

                    byte[] bytes_10 = aByteArray722;

                    for (x = -128; x < 0; x++) {
                        for (y = -128; y < 0; y++) {
                            if (shadowedPixel[i_4] != 0) {
                                bytes_10[i_5++] = 68;
                            } else {
                                i_9 = 0;
                                if (shadowedPixel[i_4 - 1] != 0) {
                                    ++i_9;
                                }

                                if (shadowedPixel[i_4 + 1] != 0) {
                                    ++i_9;
                                }

                                if (shadowedPixel[i_4 - i_3] != 0) {
                                    ++i_9;
                                }

                                if (shadowedPixel[i_4 + i_3] != 0) {
                                    ++i_9;
                                }

                                bytes_10[i_5++] = (byte) (i_9 * 17);
                            }

                            ++i_4;
                        }

                        i_4 += this.aClass74711.width - 128;
                    }

                    if (this.textureSampler == null) {
                        this.textureSampler = this.toolkit.createTextureSampler(GLFormat.RGBA, 128, 128, false, aByteArray722);
                        this.textureSampler.setRepeat(false, false);
                    } else {
                        this.textureSampler.method52(0, 0, 128, 128, aByteArray722, GLFormat.RGBA, 0, 128);
                    }
                } else {
                    if (pixels == null) {
                        pixels = new int[16384];
                    }

                    int[] ints_11 = pixels;

                    for (x = -128; x < 0; x++) {
                        for (y = -128; y < 0; y++) {
                            if (shadowedPixel[i_4] != 0) {
                                ints_11[i_5++] = 1140850688;
                            } else {
                                i_9 = 0;
                                if (shadowedPixel[i_4 - 1] != 0) {
                                    ++i_9;
                                }

                                if (shadowedPixel[i_4 + 1] != 0) {
                                    ++i_9;
                                }

                                if (shadowedPixel[i_4 - i_3] != 0) {
                                    ++i_9;
                                }

                                if (shadowedPixel[i_4 + i_3] != 0) {
                                    ++i_9;
                                }

                                ints_11[i_5++] = i_9 * 17 << 24;
                            }

                            ++i_4;
                        }

                        i_4 += this.aClass74711.width - 128;
                    }

                    if (this.textureSampler == null) {
                        this.textureSampler = this.toolkit.createTexture(128, 128, false, pixels);
                        this.textureSampler.setRepeat(false, false);
                    } else {
                        this.textureSampler.method48(0, 0, 128, 128, pixels, 0, 128);
                    }
                }
            }
        }

    }

}
