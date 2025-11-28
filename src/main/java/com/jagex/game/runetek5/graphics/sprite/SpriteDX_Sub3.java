package com.jagex.game.runetek5.graphics.sprite;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.unidentified.Class455_Sub1;

public class SpriteDX_Sub3 extends NativeSprite {

    public boolean aBool9051;
    public int anInt9057;
    public int anInt9055;
    public int anInt9049;
    public int anInt9058;
    public final AbstractNativeToolkit nativeToolkit;
    public final int anInt9060;
    public final int anInt9053;
    public final NativeTextureSampler anInterface69050;
    public final FrameBuffer anInterface99054;
    public final boolean aBool9059;
    public final boolean aBool9056;
    public final boolean aBool9061;
    public final boolean aBool9062;

    public SpriteDX_Sub3(AbstractNativeToolkit class505Sub21, NativeTextureSampler interface62) {
        this(class505Sub21, interface62, interface62.getTextureWidth(), interface62.getTextureHeight());
    }

    public SpriteDX_Sub3(AbstractNativeToolkit class505Sub21, NativeTextureSampler interface62, int i_3, int i_4) {
        this.aBool9051 = false;
        this.anInt9057 = 0;
        this.anInt9055 = 0;
        this.anInt9049 = 0;
        this.anInt9058 = 0;
        this.nativeToolkit = class505Sub21;
        this.anInt9060 = i_3;
        this.anInt9053 = i_4;
        this.anInterface69050 = interface62;
        this.anInterface99054 = null;
        this.aBool9059 = this.anInterface69050.getTextureWidth() != i_3;
        this.aBool9056 = this.anInterface69050.getTextureHeight() != i_4;
        this.aBool9061 = !this.aBool9059 && this.anInterface69050.method46();
        this.aBool9062 = !this.aBool9056 && this.anInterface69050.method46();
        this.nativeToolkit.getShaderByEffect(12);
    }

    public SpriteDX_Sub3(AbstractNativeToolkit class505Sub21, int i_2, int i_3, int[] ints_4, int i_5, int i_6) {
        this.aBool9051 = false;
        this.anInt9057 = 0;
        this.anInt9055 = 0;
        this.anInt9049 = 0;
        this.anInt9058 = 0;
        this.nativeToolkit = class505Sub21;
        this.anInt9060 = i_2;
        this.anInt9053 = i_3;
        this.anInterface99054 = null;
        this.anInterface69050 = class505Sub21.createTextureSampler(i_2, i_3, false, ints_4, i_5, i_6);
        this.anInterface69050.setRepeat(true, true);
        this.aBool9059 = this.anInterface69050.getTextureWidth() != i_2;
        this.aBool9056 = this.anInterface69050.getTextureHeight() != i_3;
        this.aBool9061 = !this.aBool9059 && this.anInterface69050.method46();
        this.aBool9062 = !this.aBool9056 && this.anInterface69050.method46();
        this.nativeToolkit.getShaderByEffect(12);
    }

    public SpriteDX_Sub3(AbstractNativeToolkit class505Sub21, int i_2, int i_3, boolean bool4, boolean bool5) {
        this.aBool9051 = false;
        this.anInt9057 = 0;
        this.anInt9055 = 0;
        this.anInt9049 = 0;
        this.anInt9058 = 0;
        this.nativeToolkit = class505Sub21;
        this.anInt9060 = i_2;
        this.anInt9053 = i_3;
        if (bool5) {
            RectangleTextureSampler interface296 = class505Sub21.method14064(bool4 ? GLFormat.HDR : GLFormat.aClass1501950, GLActionFormat.PIXEL_FORMAT, i_2, i_3);
            this.anInterface99054 = interface296.getFramebuffer();
            this.anInterface69050 = interface296;
        } else {
            this.anInterface69050 = class505Sub21.method14072(bool4 ? GLFormat.HDR : GLFormat.aClass1501950, GLActionFormat.PIXEL_FORMAT, i_2, i_3);
            this.anInterface99054 = null;
        }

        this.anInterface69050.setRepeat(true, true);
        this.aBool9059 = this.anInterface69050.getTextureWidth() != i_2;
        this.aBool9056 = this.anInterface69050.getTextureHeight() != i_3;
        this.aBool9061 = !this.aBool9059 && this.anInterface69050.method46();
        this.aBool9062 = !this.aBool9056 && this.anInterface69050.method46();
        this.nativeToolkit.getShaderByEffect(12);
    }


    @Override
    public void drawColoredCircle(int i_1, int i_2, int i_3, int i_4, int i_5, int colorArgb, int blending) {
        this.nativeToolkit.method13935();
        this.nativeToolkit.toggleBlending(blending);
        AbstractSpriteShader shader = this.nativeToolkit.spriteShader;
        shader.anInterface6443 = this.anInterface69050;
        shader.setColours(i_5, colorArgb);
        if (this.aBool9051) {
            i_3 = i_3 * this.anInt9060 / getSpriteWidth();
            i_4 = i_4 * this.anInt9053 / getSpriteHeight();
            i_1 += i_3 * this.anInt9055 / this.anInt9060;
            i_2 += i_4 * this.anInt9057 / this.anInt9053;
        }

        float displayWidth = this.nativeToolkit.getDisplay().getWidth();
        float displayHeight = this.nativeToolkit.getDisplay().getHeight();
        shader.worldViewProjectionMatrix.method6525(i_3 * 2.0F / displayWidth, i_4 * 2.0F / displayHeight, 1.0F, 1.0F);
        shader.worldViewProjectionMatrix.getFaceList()[12] = (i_1 + this.nativeToolkit.method13905()) * 2.0F / displayWidth - 1.0F;
        shader.worldViewProjectionMatrix.getFaceList()[13] = (i_2 + this.nativeToolkit.method13905()) * 2.0F / displayHeight - 1.0F;
        shader.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
        shader.aClass384440.method6525(this.anInterface69050.method47(this.anInt9060), this.anInterface69050.method62(this.anInt9053), 1.0F, 1.0F);
        shader.anInterface4445 = this.nativeToolkit.anInterface48746;
        shader.anInt446 = 0;
        shader.aClass70447 = this.nativeToolkit.aClass708786;
        shader.loadNormal();
    }

    @Override
    public void pixelTransfer() {
        int[] ints_4 = this.nativeToolkit.copyPixelRectangle(0, 0, this.anInt9060, this.anInt9053);
        int[] ints_5 = new int[this.anInt9060 * this.anInt9053];
        this.anInterface69050.method53(this.anInt9060, this.anInt9053, ints_5);
        int i_6;
        int i_7;
        int i_8;
        for (i_6 = 0; i_6 < this.anInt9053; i_6++) {
            i_7 = i_6 * this.anInt9060;

            for (i_8 = 0; i_8 < this.anInt9060; i_8++) {
                ints_5[i_7 + i_8] = ints_5[i_7 + i_8] & 0xffffff | (ints_4[i_7 + i_8] != 0 ? ~0xffffff : 0);
            }
        }

        method14425(0, 0, this.anInt9060, this.anInt9053, ints_5, this.anInt9060);
    }

    @Override
    public void method2750(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        int[] ints_7 = this.nativeToolkit.copyPixelRectangle(i_5, i_6, i_3, i_4);
        if (ints_7 != null) {
            for (int i_8 = 0; i_8 < ints_7.length; i_8++) {
                ints_7[i_8] |= ~0xffffff;
            }

            method14425(i_1, i_2, i_3, i_4, ints_7, i_3);
        }

    }

    @Override
    public int getWidth() {
        return this.anInt9060;
    }

    @Override
    public int getHeight() {
        return this.anInt9053;
    }

    @Override
    public void method2764(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, int i_8, int i_9) {
        this.nativeToolkit.method13935();
        this.nativeToolkit.toggleBlending(i_9);
        float f_12;
        float f_13;
        if (this.aBool9051) {
            float f_11 = getSpriteWidth();
            f_12 = getSpriteHeight();
            f_13 = (f_3 - f_1) / f_11;
            float f_14 = (f_4 - f_2) / f_11;
            float f_15 = (f_5 - f_1) / f_12;
            float f_16 = (f_6 - f_2) / f_12;
            float f_17 = f_15 * this.anInt9057;
            float f_18 = f_16 * this.anInt9057;
            float f_19 = f_13 * this.anInt9055;
            float f_20 = f_14 * this.anInt9055;
            float f_21 = -f_13 * this.anInt9058;
            float f_22 = -f_14 * this.anInt9058;
            float f_23 = -f_15 * this.anInt9049;
            float f_24 = -f_16 * this.anInt9049;
            f_1 = f_1 + f_19 + f_17;
            f_2 = f_2 + f_20 + f_18;
            f_3 = f_3 + f_21 + f_17;
            f_4 = f_4 + f_22 + f_18;
            f_5 = f_5 + f_19 + f_23;
            f_6 = f_6 + f_20 + f_24;
        }

        AbstractSpriteShader class4725 = this.nativeToolkit.spriteShader;
        class4725.anInterface6443 = this.anInterface69050;
        class4725.setColours(i_7, i_8);
        f_12 = this.nativeToolkit.getDisplay().getWidth();
        f_13 = this.nativeToolkit.getDisplay().getHeight();
        class4725.worldViewProjectionMatrix.identity();
        class4725.worldViewProjectionMatrix.getFaceList()[0] = (f_3 - f_1) * 2.0F / f_12;
        class4725.worldViewProjectionMatrix.getFaceList()[1] = (f_4 - f_2) * 2.0F / f_13;
        class4725.worldViewProjectionMatrix.getFaceList()[4] = (f_5 - f_1) * 2.0F / f_12;
        class4725.worldViewProjectionMatrix.getFaceList()[5] = (f_6 - f_2) * 2.0F / f_13;
        class4725.worldViewProjectionMatrix.getFaceList()[12] = (f_1 + this.nativeToolkit.method13905()) * 2.0F / f_12 - 1.0F;
        class4725.worldViewProjectionMatrix.getFaceList()[13] = (f_2 + this.nativeToolkit.method13905()) * 2.0F / f_13 - 1.0F;
        class4725.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
        class4725.aClass384440.method6525(this.anInterface69050.method47(this.anInt9060), this.anInterface69050.method62(this.anInt9053), 1.0F, 1.0F);
        class4725.anInterface4445 = this.nativeToolkit.anInterface48746;
        class4725.anInt446 = 0;
        class4725.aClass70447 = this.nativeToolkit.aClass708786;
        class4725.loadNormal();
    }

    @Override
    public FrameBuffer getFramebuffer() {
        return this.anInterface99054;
    }

    @Override
    public void method2773(int i_1, int i_2, ComponentSurface class4553, int i_4, int i_5) {
        this.nativeToolkit.method13935();
        AbstractSpriteShader class476 = this.nativeToolkit.spriteShader;
        class476.anInterface6443 = this.anInterface69050;
        class476.setColours(1, -1);
        i_1 += this.anInt9055;
        i_2 += this.anInt9057;
        float f_7 = this.nativeToolkit.getDisplay().getWidth();
        float f_8 = this.nativeToolkit.getDisplay().getHeight();
        class476.worldViewProjectionMatrix.method6525(this.anInt9060 * 2.0F / f_7, this.anInt9053 * 2.0F / f_8, 1.0F, 1.0F);
        class476.worldViewProjectionMatrix.getFaceList()[12] = (i_1 + this.nativeToolkit.method13905()) * 2.0F / f_7 - 1.0F;
        class476.worldViewProjectionMatrix.getFaceList()[13] = (i_2 + this.nativeToolkit.method13905()) * 2.0F / f_8 - 1.0F;
        class476.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
        class476.aClass384440.method6525(this.anInterface69050.method47(this.anInt9060), this.anInterface69050.method62(this.anInt9053), 1.0F, 1.0F);
        class476.anInterface4445 = this.nativeToolkit.anInterface48746;
        class476.anInt446 = 0;
        class476.aClass70447 = this.nativeToolkit.aClass708786;
        NativeTextureSampler interface69 = ((Class455_Sub1) class4553).nativeTextureSampler;
        class476.anInterface6441 = interface69;
        class476.aClass384444.method6525(interface69.method47(this.anInt9060), interface69.method62(this.anInt9053), 1.0F, 1.0F);
        class476.aClass384444.getFaceList()[12] = interface69.method47((i_1 - i_4));
        class476.aClass384444.getFaceList()[13] = interface69.method62((i_2 - i_5));
        class476.loadMasked();
    }


    @Override
    public void method2772(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        this.nativeToolkit.method13935();
        this.nativeToolkit.toggleBlending(i_7);
        AbstractSpriteShader class478 = this.nativeToolkit.spriteShader;
        class478.anInterface6443 = this.anInterface69050;
        class478.setColours(i_5, i_6);
        float f_9 = this.nativeToolkit.getDisplay().getWidth();
        float f_10 = this.nativeToolkit.getDisplay().getHeight();
        class478.anInterface4445 = this.nativeToolkit.anInterface48746;
        class478.anInt446 = 0;
        class478.aClass70447 = this.nativeToolkit.aClass708786;
        boolean bool11 = this.aBool9062 && this.anInt9057 == 0 && this.anInt9049 == 0;
        boolean bool12 = this.aBool9061 && this.anInt9055 == 0 && this.anInt9058 == 0;
        if (bool12 & bool11) {
            class478.worldViewProjectionMatrix.method6525(i_3 * 2.0F / f_9, i_4 * 2.0F / f_10, 1.0F, 1.0F);
            class478.worldViewProjectionMatrix.getFaceList()[12] = (i_1 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
            class478.worldViewProjectionMatrix.getFaceList()[13] = (i_2 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
            class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
            class478.aClass384440.method6525(this.anInterface69050.method47(i_3), this.anInterface69050.method62(i_4), 1.0F, 1.0F);
            class478.loadNormal();
        } else {
            int i_13;
            int i_14;
            int i_15;
            int i_16;
            if (bool12) {
                i_13 = i_2 + i_4;
                i_14 = getSpriteHeight();
                class478.aClass384440.method6525(this.anInterface69050.method47(i_3), this.anInterface69050.method62(this.anInt9053), 1.0F, 1.0F);
                i_15 = i_2 + this.anInt9057;

                for (i_16 = i_15 + this.anInt9053; i_16 <= i_13; i_16 += i_14) {
                    class478.worldViewProjectionMatrix.method6525(i_3 * 2.0F / f_9, this.anInt9053 * 2.0F / f_10, 1.0F, 1.0F);
                    class478.worldViewProjectionMatrix.getFaceList()[12] = (i_1 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
                    class478.worldViewProjectionMatrix.getFaceList()[13] = (i_15 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
                    class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
                    class478.loadNormal();
                    i_15 += i_14;
                }

                if (i_15 < i_13) {
                    i_16 = i_13 - i_15;
                    class478.aClass384440.method6525(this.anInterface69050.method47(i_3), this.anInterface69050.method62(i_16), 1.0F, 1.0F);
                    class478.worldViewProjectionMatrix.method6525(i_3 * 2.0F / f_9, i_16 * 2.0F / f_10, 1.0F, 1.0F);
                    class478.worldViewProjectionMatrix.getFaceList()[12] = (i_1 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
                    class478.worldViewProjectionMatrix.getFaceList()[13] = (i_15 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
                    class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
                    class478.loadNormal();
                }
            } else if (bool11) {
                i_13 = i_3 + i_1;
                i_14 = getSpriteWidth();
                class478.aClass384440.method6525(this.anInterface69050.method47(this.anInt9060), this.anInterface69050.method62(i_4), 1.0F, 1.0F);
                i_15 = this.anInt9055 + i_1;

                for (i_16 = i_15 + this.anInt9060; i_16 <= i_13; i_16 += i_14) {
                    class478.worldViewProjectionMatrix.method6525(this.anInt9060 * 2.0F / f_9, i_4 * 2.0F / f_10, 1.0F, 1.0F);
                    class478.worldViewProjectionMatrix.getFaceList()[12] = (i_15 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
                    class478.worldViewProjectionMatrix.getFaceList()[13] = (i_2 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
                    class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
                    class478.loadNormal();
                    i_15 += i_14;
                }

                if (i_15 < i_13) {
                    i_16 = i_13 - i_15;
                    class478.aClass384440.method6525(this.anInterface69050.method47(i_16), this.anInterface69050.method62(i_4), 1.0F, 1.0F);
                    class478.worldViewProjectionMatrix.method6525(i_16 * 2.0F / f_9, i_4 * 2.0F / f_10, 1.0F, 1.0F);
                    class478.worldViewProjectionMatrix.getFaceList()[12] = (i_15 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
                    class478.worldViewProjectionMatrix.getFaceList()[13] = (i_2 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
                    class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
                    class478.loadNormal();
                }
            } else {
                i_13 = i_2 + i_4;
                i_14 = i_3 + i_1;
                i_15 = getSpriteWidth();
                i_16 = getSpriteHeight();
                int i_17 = i_2 + this.anInt9057;

                int i_18;
                int i_19;
                int i_20;
                for (i_18 = i_17 + this.anInt9053; i_18 <= i_13; i_18 += i_16) {
                    class478.aClass384440.method6525(this.anInterface69050.method47(this.anInt9060), this.anInterface69050.method62(this.anInt9053), 1.0F, 1.0F);
                    i_19 = this.anInt9055 + i_1;

                    for (i_20 = i_19 + this.anInt9060; i_20 <= i_14; i_20 += i_15) {
                        class478.worldViewProjectionMatrix.method6525(this.anInt9060 * 2.0F / f_9, this.anInt9053 * 2.0F / f_10, 1.0F, 1.0F);
                        class478.worldViewProjectionMatrix.getFaceList()[12] = (i_19 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
                        class478.worldViewProjectionMatrix.getFaceList()[13] = (i_17 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
                        class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
                        class478.loadNormal();
                        i_19 += i_15;
                    }

                    if (i_19 < i_14) {
                        i_20 = i_14 - i_19;
                        class478.aClass384440.method6525(this.anInterface69050.method47(i_20), this.anInterface69050.method62(this.anInt9053), 1.0F, 1.0F);
                        class478.worldViewProjectionMatrix.method6525(i_20 * 2.0F / f_9, this.anInt9053 * 2.0F / f_10, 1.0F, 1.0F);
                        class478.worldViewProjectionMatrix.getFaceList()[12] = (i_19 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
                        class478.worldViewProjectionMatrix.getFaceList()[13] = (i_17 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
                        class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
                        class478.loadNormal();
                    }

                    i_17 += i_16;
                }

                if (i_17 < i_13) {
                    i_18 = i_13 - i_17;
                    class478.aClass384440.method6525(this.anInterface69050.method47(this.anInt9060), this.anInterface69050.method62(i_18), 1.0F, 1.0F);
                    i_19 = this.anInt9055 + i_1;

                    for (i_20 = i_19 + this.anInt9060; i_20 <= i_14; i_20 += i_15) {
                        class478.worldViewProjectionMatrix.method6525(this.anInt9060 * 2.0F / f_9, i_18 * 2.0F / f_10, 1.0F, 1.0F);
                        class478.worldViewProjectionMatrix.getFaceList()[12] = (i_19 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
                        class478.worldViewProjectionMatrix.getFaceList()[13] = (i_17 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
                        class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
                        class478.loadNormal();
                        i_19 += i_15;
                    }

                    if (i_19 < i_14) {
                        i_20 = i_14 - i_19;
                        class478.aClass384440.method6525(this.anInterface69050.method47(i_20), this.anInterface69050.method62(i_18), 1.0F, 1.0F);
                        class478.worldViewProjectionMatrix.method6525(i_20 * 2.0F / f_9, i_18 * 2.0F / f_10, 1.0F, 1.0F);
                        class478.worldViewProjectionMatrix.getFaceList()[12] = (i_19 + this.nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F;
                        class478.worldViewProjectionMatrix.getFaceList()[13] = (i_17 + this.nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F;
                        class478.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
                        class478.loadNormal();
                    }
                }
            }
        }

    }


    @Override
	public
    void method2766(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, ComponentSurface class4558, int i_9, int i_10) {
        this.nativeToolkit.method13935();
        AbstractSpriteShader class4711 = this.nativeToolkit.spriteShader;
        class4711.anInterface6443 = this.anInterface69050;
        class4711.setColours(1, -1);
        float f_12 = this.nativeToolkit.getDisplay().getWidth();
        float f_13 = this.nativeToolkit.getDisplay().getHeight();
        class4711.worldViewProjectionMatrix.identity();
        if (this.aBool9051) {
            float f_14 = (float) this.anInt9060 / getSpriteWidth();
            float f_15 = (float) this.anInt9053 / getSpriteHeight();
            class4711.worldViewProjectionMatrix.getFaceList()[0] = (f_3 - f_1) * f_14;
            class4711.worldViewProjectionMatrix.getFaceList()[1] = (f_4 - f_2) * f_14;
            class4711.worldViewProjectionMatrix.getFaceList()[4] = (f_5 - f_1) * f_15;
            class4711.worldViewProjectionMatrix.getFaceList()[5] = (f_6 - f_2) * f_15;
            class4711.worldViewProjectionMatrix.getFaceList()[12] = (f_1 + this.anInt9055) * f_14 + this.nativeToolkit.method13905();
            class4711.worldViewProjectionMatrix.getFaceList()[13] = (f_2 + this.anInt9057) * f_15 + this.nativeToolkit.method13905();
        } else {
            class4711.worldViewProjectionMatrix.getFaceList()[0] = f_3 - f_1;
            class4711.worldViewProjectionMatrix.getFaceList()[1] = f_4 - f_2;
            class4711.worldViewProjectionMatrix.getFaceList()[4] = f_5 - f_1;
            class4711.worldViewProjectionMatrix.getFaceList()[5] = f_6 - f_2;
            class4711.worldViewProjectionMatrix.getFaceList()[12] = f_1 + this.nativeToolkit.method13905();
            class4711.worldViewProjectionMatrix.getFaceList()[13] = f_2 + this.nativeToolkit.method13905();
        }

        Matrix44 matrix4416 = this.nativeToolkit.aClass3848683;
        matrix4416.identity();
        matrix4416.getFaceList()[0] = 2.0F / f_12;
        matrix4416.getFaceList()[5] = 2.0F / f_13;
        matrix4416.getFaceList()[12] = -1.0f;
        matrix4416.getFaceList()[13] = -1.0f;
        matrix4416.getFaceList()[14] = -1.0f;
        class4711.worldViewProjectionMatrix.multiplyBy(matrix4416);
        class4711.aClass384440.method6525(this.anInterface69050.method47(this.anInt9060), this.anInterface69050.method62(this.anInt9053), 1.0F, 1.0F);
        class4711.anInterface4445 = this.nativeToolkit.anInterface48746;
        class4711.anInt446 = 0;
        class4711.aClass70447 = this.nativeToolkit.aClass708786;
        NativeTextureSampler interface617 = ((Class455_Sub1) class4558).nativeTextureSampler;
        class4711.anInterface6441 = interface617;
        class4711.aClass384444.identity();
        class4711.aClass384444.getFaceList()[0] = (f_3 - f_1) * interface617.method47(1.0F);
        class4711.aClass384444.getFaceList()[1] = (f_4 - f_2) * interface617.method47(1.0F);
        class4711.aClass384444.getFaceList()[4] = (f_5 - f_1) * interface617.method62(1.0F);
        class4711.aClass384444.getFaceList()[5] = (f_6 - f_2) * interface617.method62(1.0F);
        class4711.aClass384444.getFaceList()[12] = (f_1 - i_9) * interface617.method47(1.0F);
        class4711.aClass384444.getFaceList()[13] = (f_2 - i_10) * interface617.method62(1.0F);
        class4711.loadMasked();
    }

    @Override
    public void draw(int i_1, int i_2, int i_3, int i_4, int i_5) {
        this.nativeToolkit.method13935();
        this.nativeToolkit.toggleBlending(i_5);
        AbstractSpriteShader class476 = this.nativeToolkit.spriteShader;
        class476.anInterface6443 = this.anInterface69050;
        class476.setColours(i_3, i_4);
        i_1 += this.anInt9055;
        i_2 += this.anInt9057;
        float f_7 = this.nativeToolkit.getDisplay().getWidth();
        float f_8 = this.nativeToolkit.getDisplay().getHeight();
        class476.worldViewProjectionMatrix.method6525(this.anInt9060 * 2.0F / f_7, this.anInt9053 * 2.0F / f_8, 1.0F, 1.0F);
        class476.worldViewProjectionMatrix.getFaceList()[12] = (i_1 + this.nativeToolkit.method13905()) * 2.0F / f_7 - 1.0F;
        class476.worldViewProjectionMatrix.getFaceList()[13] = (i_2 + this.nativeToolkit.method13905()) * 2.0F / f_8 - 1.0F;
        class476.worldViewProjectionMatrix.getFaceList()[14] = -1.0f;
        class476.aClass384440.method6525(this.anInterface69050.method47(this.anInt9060), this.anInterface69050.method62(this.anInt9053), 1.0F, 1.0F);
        class476.anInterface4445 = this.nativeToolkit.anInterface48746;
        class476.anInt446 = 0;
        class476.aClass70447 = this.nativeToolkit.aClass708786;
        class476.loadNormal();
    }


    public void method14425(int i_1, int i_2, int i_3, int i_4, int[] ints_5, int i_7) {
        this.anInterface69050.method48(i_1, i_2, i_3, i_4, ints_5, 0, i_7);
    }


    @Override
    public void setSize(int i_1, int i_2, int i_3, int i_4) {
        this.anInt9055 = i_1;
        this.anInt9057 = i_2;
        this.anInt9058 = i_3;
        this.anInt9049 = i_4;
        this.aBool9051 = this.anInt9055 != 0 || this.anInt9057 != 0 || this.anInt9058 != 0 || this.anInt9049 != 0;
    }


    @Override
    public int getSpriteHeight() {
        return this.anInt9053 + this.anInt9057 + this.anInt9049;
    }

    @Override
    public void cacheSizeInto(int[] ints_1) {
        ints_1[0] = this.anInt9055;
        ints_1[1] = this.anInt9057;
        ints_1[2] = this.anInt9058;
        ints_1[3] = this.anInt9049;
    }

    @Override
    public int getSpriteWidth() {
        return this.anInt9060 + this.anInt9055 + this.anInt9058;
    }

}
