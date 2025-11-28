package com.jagex.game.runetek5.graphics.font;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.SampleMode;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class NativeFont extends AbstractFontRenderer {

    public final AbstractNativeToolkit aGraphicalRendererSub29321;
    public final NativeTextureSampler anInterface69322;
    public final NativeStream anInterface49320;

    public NativeFont(AbstractNativeToolkit class505Sub21, FontMetrics fontmetrics2, SpriteType[] arr_3, boolean bool4) {
        super(class505Sub21, fontmetrics2);
        this.aGraphicalRendererSub29321 = class505Sub21;
        int i_5 = 0;

        int i_6;
        for (i_6 = 0; i_6 < 256; i_6++) {
            SpriteType class917 = arr_3[i_6];
            if (class917.height > i_5) {
                i_5 = class917.height;
            }

            if (class917.width > i_5) {
                i_5 = class917.width;
            }
        }

        i_6 = i_5 * 16;
        int i_8;
        int i_10;
        int i_12;
        int i_13;
        int i_14;
        int i_15;
        int i_17;
        int i_18;
        SpriteType class9123;
        if (bool4) {
            byte[] bytes_28 = new byte[i_6 * i_6];

            for (i_8 = 0; i_8 < 256; i_8++) {
                class9123 = arr_3[i_8];
                i_10 = class9123.height;
                int i_11 = class9123.width;
                i_12 = i_5 * (i_8 % 16);
                i_13 = i_8 / 16 * i_5;
                i_14 = i_12 + i_13 * i_6;
                i_15 = 0;
                byte[] bytes_16;
                if (class9123.alpha == null) {
                    bytes_16 = class9123.pixels;

                    for (i_17 = 0; i_17 < i_10; i_17++) {
                        for (i_18 = 0; i_18 < i_11; i_18++) {
                            bytes_28[i_14++] = (byte) (bytes_16[i_15++] == 0 ? 0 : -1);
                        }

                        i_14 += i_6 - i_11;
                    }
                } else {
                    bytes_16 = class9123.alpha;

                    for (i_17 = 0; i_17 < i_10; i_17++) {
                        for (i_18 = 0; i_18 < i_11; i_18++) {
                            bytes_28[i_14++] = bytes_16[i_15++];
                        }

                        i_14 += i_6 - i_11;
                    }
                }
            }

            if (!class505Sub21.method13954(GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT)) {
                int[] ints_22 = new int[bytes_28.length];

                for (int i_9 = 0; i_9 < bytes_28.length; i_9++) {
                    ints_22[i_9] = bytes_28[i_9] << 24;
                }

                this.anInterface69322 = class505Sub21.createTexture(i_6, i_6, false, ints_22);
            } else {
                this.anInterface69322 = class505Sub21.createTextureSampler(GLFormat.RGBA, i_6, i_6, false, bytes_28);
            }
        } else {
            int[] ints_29 = new int[i_6 * i_6];

            for (i_8 = 0; i_8 < 256; i_8++) {
                class9123 = arr_3[i_8];
                int[] ints_24 = class9123.pallete;
                byte[] bytes_25 = class9123.alpha;
                byte[] bytes_26 = class9123.pixels;
                i_13 = class9123.height;
                i_14 = class9123.width;
                i_15 = i_5 * (i_8 % 16);
                int i_27 = i_8 / 16 * i_5;
                i_17 = i_15 + i_27 * i_6;
                i_18 = 0;
                int i_19;
                int i_20;
                if (bytes_25 != null) {
                    for (i_19 = 0; i_19 < i_13; i_19++) {
                        for (i_20 = 0; i_20 < i_14; i_20++) {
                            ints_29[i_17++] = bytes_25[i_18] << 24 | ints_24[bytes_26[i_18] & 0xff];
                            ++i_18;
                        }

                        i_17 += i_6 - i_14;
                    }
                } else {
                    for (i_19 = 0; i_19 < i_13; i_19++) {
                        for (i_20 = 0; i_20 < i_14; i_20++) {
                            byte b_21;
                            if ((b_21 = bytes_26[i_18++]) != 0) {
                                ints_29[i_17++] = ~0xffffff | ints_24[b_21 & 0xff];
                            } else {
                                ++i_17;
                            }
                        }

                        i_17 += i_6 - i_14;
                    }
                }
            }

            this.anInterface69322 = class505Sub21.createTexture(i_6, i_6, false, ints_29);
        }

        this.anInterface69322.setSamplerMode(SampleMode.NEAREST);
        this.anInterface49320 = class505Sub21.createStream(false);
        this.anInterface49320.create(20480, 20);
        ByteBuffer bytebuffer30 = this.aGraphicalRendererSub29321.directBuffer;
        bytebuffer30.clear();
        float f_31 = this.anInterface69322.method47(i_6) / i_6;
        float f_32 = this.anInterface69322.method62(i_6) / i_6;

        for (i_10 = 0; i_10 < 256; i_10++) {
            SpriteType class9138 = arr_3[i_10];
            i_12 = class9138.height;
            i_13 = class9138.width;
            i_14 = class9138.minY;
            i_15 = class9138.minX;
            float f_39 = (i_5 * (i_10 % 16));
            float f_33 = (i_10 / 16 * i_5);
            float f_34 = f_39 * f_31;
            float f_35 = f_33 * f_32;
            float f_36 = (f_39 + i_13) * f_31;
            float f_37 = (f_33 + i_12) * f_32;
            bytebuffer30.putFloat(i_15);
            bytebuffer30.putFloat(i_14);
            bytebuffer30.putFloat(0.0F);
            bytebuffer30.putFloat(f_34);
            bytebuffer30.putFloat(f_35);
            bytebuffer30.putFloat(i_15);
            bytebuffer30.putFloat((i_14 + i_12));
            bytebuffer30.putFloat(0.0F);
            bytebuffer30.putFloat(f_34);
            bytebuffer30.putFloat(f_37);
            bytebuffer30.putFloat((i_15 + i_13));
            bytebuffer30.putFloat((i_14 + i_12));
            bytebuffer30.putFloat(0.0F);
            bytebuffer30.putFloat(f_36);
            bytebuffer30.putFloat(f_37);
            bytebuffer30.putFloat((i_15 + i_13));
            bytebuffer30.putFloat(i_14);
            bytebuffer30.putFloat(0.0F);
            bytebuffer30.putFloat(f_36);
            bytebuffer30.putFloat(f_35);
        }

        this.anInterface49320.upload(0, bytebuffer30.position(), this.aGraphicalRendererSub29321.directBufferAddress);
        class505Sub21.getShaderByEffect(12);
    }

    @Override
    public void drawChar(int var_1, int i_2, int i_3, int i_4, boolean bool5) {
        this.aGraphicalRendererSub29321.method13935();
        this.aGraphicalRendererSub29321.toggleBlending(1);
        AbstractSpriteShader class476 = this.aGraphicalRendererSub29321.spriteShader;
        class476.anInterface6443 = this.anInterface69322;
        class476.setColours(3, i_4);
        float f_7 = this.aGraphicalRendererSub29321.getDisplay().getWidth();
        float f_8 = this.aGraphicalRendererSub29321.getDisplay().getHeight();
        class476.worldViewProjectionMatrix.method6525(2.0F / f_7, 2.0F / f_8, 1.0F, 1.0F);
        class476.worldViewProjectionMatrix.getFaceList()[12] = (i_2 + this.aGraphicalRendererSub29321.method13905()) * 2.0F / f_7 - 1.0F;
        class476.worldViewProjectionMatrix.getFaceList()[13] = (i_3 + this.aGraphicalRendererSub29321.method13905()) * 2.0F / f_8 - 1.0F;
        class476.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
        class476.aClass384440.identity();
        class476.anInterface4445 = this.anInterface49320;
        class476.anInt446 = var_1 * 4;
        class476.aClass70447 = this.aGraphicalRendererSub29321.aClass708832;
        class476.loadNormal();
    }

    @Override
    public void drawChar(int var_1, int i_2, int i_3, int i_4, boolean bool5, ComponentSurface class4556, int i_7, int i_8) {
    }

}
