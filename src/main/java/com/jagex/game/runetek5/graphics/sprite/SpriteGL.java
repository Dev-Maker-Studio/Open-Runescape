package com.jagex.game.runetek5.graphics.sprite;

import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture_Sub1;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.unidentified.SpriteSurfaceGL;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.FrameBuffer;

import jaggl.OpenGL;

public class SpriteGL extends NativeSprite {

    public int translateX;
    public int translateY;
    public int anInt9036;
    public int anInt9035;
    public boolean stretched;
    public int anInt9031;
    public final OpenGlToolkit toolkit;
    public final OpenGl2DTexture_Sub1 texture2d;
    public OpenGl2DTexture_Sub1 aClass137Sub1Sub19029;

    public SpriteGL(OpenGlToolkit class505Sub11, int i_2, int i_3, int[] ints_4, int i_5, int i_6) {
        this.toolkit = class505Sub11;
        this.texture2d = OpenGl2DTexture_Sub1.method15537(class505Sub11, i_2, i_3, ints_4, i_6, i_5);
    }

    public SpriteGL(OpenGlToolkit class505Sub11, int i_2, int i_3, int i_4, int i_5) {
        this.toolkit = class505Sub11;
        this.texture2d = OpenGl2DTexture_Sub1.method15538(class505Sub11, i_2, i_3, i_4, i_5);
    }

    public SpriteGL(OpenGlToolkit class505Sub11, int i_2, int i_3, boolean bool4) {
        this.toolkit = class505Sub11;
        this.texture2d = OpenGl2DTexture_Sub1.method15547(class505Sub11, bool4 ? GLFormat.HDR : GLFormat.aClass1501950, GLActionFormat.PIXEL_FORMAT, i_2, i_3);
    }


    @Override
    public void setSize(int translateX, int translateY, int i_3, int i_4) {
        this.translateX = translateX;
        this.translateY = translateY;
        this.anInt9036 = i_3;
        this.anInt9035 = i_4;
        this.stretched = this.translateX != 0 || this.translateY != 0 || this.anInt9036 != 0 || this.anInt9035 != 0;
    }


    @Override
    public int getWidth() {
        return this.texture2d.singleWidth;
    }

    @Override
    public int getHeight() {
        return this.texture2d.singleLength;
    }

    @Override
    public int getSpriteHeight() {
        return this.texture2d.singleLength + this.translateY + this.anInt9035;
    }


    @Override
    public void method2750(int offsetX, int offsetY, int width, int height, int x, int y) {
        if (this.toolkit.appleOperatingSystem) {
            int[] pixels = this.toolkit.copyPixelRectangle(x, y, width, height);
            if (pixels != null) {
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] |= ~0xffffff;
                }

                method14414(offsetX, offsetY, width, height, pixels, width);
            }
        } else {
            this.texture2d.copyTextureSubImage(offsetX, offsetY, width, height, x, y);
        }

    }

    @Override
    public void pixelTransfer() {
        OpenGL.glPixelTransferf(GL_RED_SCALE, 0.5F);
        OpenGL.glPixelTransferf(GL_RED_BIAS, 0.499F);
        OpenGL.glPixelTransferf(GL_GREEN_SCALE, 0.5F);
        OpenGL.glPixelTransferf(GL_GREEN_BIAS, 0.499F);
        OpenGL.glPixelTransferf(GL_BLUE_SCALE, 0.5F);
        OpenGL.glPixelTransferf(GL_BLUE_BIAS, 0.499F);
        this.aClass137Sub1Sub19029 = OpenGl2DTexture_Sub1.method15538(this.toolkit, 0, 0, this.texture2d.singleWidth, this.texture2d.singleLength);
        this.anInt9031 = 3;
        OpenGL.glPixelTransferf(GL_RED_SCALE, 1.0F);
        OpenGL.glPixelTransferf(GL_RED_BIAS, 0.0F);
        OpenGL.glPixelTransferf(GL_GREEN_SCALE, 1.0F);
        OpenGL.glPixelTransferf(GL_GREEN_BIAS, 0.0F);
        OpenGL.glPixelTransferf(GL_BLUE_SCALE, 1.0F);
        OpenGL.glPixelTransferf(GL_BLUE_BIAS, 0.0F);
    }

    public void method14414(int i_1, int i_2, int i_3, int i_4, int[] ints_5, int i_7) {
        this.texture2d.updateTextureRegion(i_1, i_2, i_3, i_4, ints_5, i_7);
    }

    @Override
    public void method2772(int x, int y, int width, int height, int combineMode, int colorArgb, int blendMode) {
        int endX = x + width;
        int endY = y + height;
        this.texture2d.setLinearTextureFilter(false);
        this.toolkit.setupSpriteRendering();
        this.toolkit.setActiveTexture(this.texture2d);
        this.toolkit.setBlendMode(blendMode);
        this.toolkit.loadTextureCombineMode(combineMode);
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        if (this.texture2d.tiled && !this.stretched) {
            float t = this.texture2d.t * height / this.texture2d.singleLength;
            float s = this.texture2d.s * width / this.texture2d.singleWidth;
            OpenGL.glBegin(GL_QUADS);
            OpenGL.glTexCoord2f(0.0F, t);
            OpenGL.glVertex2i(x, y);
            OpenGL.glTexCoord2f(0.0F, 0.0F);
            OpenGL.glVertex2i(x, endY);
            OpenGL.glTexCoord2f(s, 0.0F);
            OpenGL.glVertex2i(endX, endY);
            OpenGL.glTexCoord2f(s, t);
            OpenGL.glVertex2i(endX, y);
            OpenGL.glEnd();
        } else {
            OpenGL.glPushMatrix();
            OpenGL.glTranslatef(this.translateX, this.translateY, 0.0F);
            int spriteWidth = getSpriteWidth();
            int spriteHeight = getSpriteHeight();
            int i_12 = this.texture2d.singleLength + y;
            OpenGL.glBegin(GL_QUADS);

            int startY;
            int startX;
            for (startY = y; i_12 <= endY; i_12 += spriteHeight) {
                int i_14 = this.texture2d.singleWidth + x;

                for (startX = x; i_14 <= endX; startX += spriteWidth) {
                    OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
                    OpenGL.glVertex2i(startX, startY);
                    OpenGL.glTexCoord2f(0.0F, 0.0F);
                    OpenGL.glVertex2i(startX, i_12);
                    OpenGL.glTexCoord2f(this.texture2d.s, 0.0F);
                    OpenGL.glVertex2i(i_14, i_12);
                    OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
                    OpenGL.glVertex2i(i_14, startY);
                    i_14 += spriteWidth;
                }

                if (startX < endX) {
                    float f_16 = this.texture2d.s * (endX - startX) / this.texture2d.singleWidth;
                    OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
                    OpenGL.glVertex2i(startX, startY);
                    OpenGL.glTexCoord2f(0.0F, 0.0F);
                    OpenGL.glVertex2i(startX, i_12);
                    OpenGL.glTexCoord2f(f_16, 0.0F);
                    OpenGL.glVertex2i(endX, i_12);
                    OpenGL.glTexCoord2f(f_16, this.texture2d.t);
                    OpenGL.glVertex2i(endX, startY);
                }

                startY += spriteHeight;
            }

            if (startY < endY) {
                float t = this.texture2d.t * (this.texture2d.singleLength - (endY - startY)) / this.texture2d.singleLength;
                startX = this.texture2d.singleWidth + x;

                int i_21;
                for (i_21 = x; startX <= endX; i_21 += spriteWidth) {
                    OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
                    OpenGL.glVertex2i(i_21, startY);
                    OpenGL.glTexCoord2f(0.0F, t);
                    OpenGL.glVertex2i(i_21, endY);
                    OpenGL.glTexCoord2f(this.texture2d.s, t);
                    OpenGL.glVertex2i(startX, endY);
                    OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
                    OpenGL.glVertex2i(startX, startY);
                    startX += spriteWidth;
                }

                if (i_21 < endX) {
                    float s = this.texture2d.s * (endX - i_21) / this.texture2d.singleWidth;
                    OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
                    OpenGL.glVertex2i(i_21, startY);
                    OpenGL.glTexCoord2f(0.0F, t);
                    OpenGL.glVertex2i(i_21, endY);
                    OpenGL.glTexCoord2f(s, t);
                    OpenGL.glVertex2i(endX, endY);
                    OpenGL.glTexCoord2f(s, this.texture2d.t);
                    OpenGL.glVertex2i(endX, startY);
                }
            }

            OpenGL.glEnd();
            OpenGL.glPopMatrix();
        }
    }

    public void method14415() {
        this.toolkit.activateTexture(1);
        this.toolkit.setActiveTexture(null);
        this.toolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        this.toolkit.setTexture(1, GL_PREVIOUS, GL_SRC_COLOR);
        this.toolkit.setTextureTransparency(0, GL_TEXTURE);
        this.toolkit.activateTexture(0);
        this.toolkit.setTexture(1, GL_PREVIOUS, GL_SRC_COLOR);
    }

    @Override
    public void draw(int x, int y, int combineMode, int colorArgb, int blendMode) {
        this.texture2d.setLinearTextureFilter(false);
        this.toolkit.setupSpriteRendering();
        this.toolkit.setBlendMode(blendMode);
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        x += this.translateX;
        y += this.translateY;
        if (this.aClass137Sub1Sub19029 == null) {
            this.toolkit.setActiveTexture(this.texture2d);
            this.toolkit.loadTextureCombineMode(combineMode);
            OpenGL.glBegin(GL_QUADS);
            OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
            OpenGL.glVertex2i(x, y);
            OpenGL.glTexCoord2f(0.0F, 0.0F);
            OpenGL.glVertex2i(x, this.texture2d.singleLength + y);
            OpenGL.glTexCoord2f(this.texture2d.s, 0.0F);
            OpenGL.glVertex2i(this.texture2d.singleWidth + x, this.texture2d.singleLength + y);
            OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
            OpenGL.glVertex2i(this.texture2d.singleWidth + x, y);
            OpenGL.glEnd();
        } else {
            method14419(combineMode);
            this.aClass137Sub1Sub19029.setLinearTextureFilter(false);
            OpenGL.glBegin(GL_QUADS);
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, this.texture2d.t);
            OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
            OpenGL.glVertex2i(x, y);
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, 0.0F);
            OpenGL.glTexCoord2f(0.0F, 0.0F);
            OpenGL.glVertex2i(x, this.texture2d.singleLength + y);
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, this.texture2d.s, 0.0F);
            OpenGL.glTexCoord2f(this.texture2d.s, 0.0F);
            OpenGL.glVertex2i(this.texture2d.singleWidth + x, this.texture2d.singleLength + y);
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, this.texture2d.s, this.texture2d.t);
            OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
            OpenGL.glVertex2i(this.texture2d.singleWidth + x, y);
            OpenGL.glEnd();
            method14415();
        }

    }

    @Override
    public void method2773(int i_1, int i_2, ComponentSurface class4553, int i_4, int i_5) {
        SpriteSurfaceGL class455Sub26 = (SpriteSurfaceGL) class4553;
        OpenGl2DTexture_Sub1 class137Sub1Sub17 = class455Sub26.surface;
        this.texture2d.setLinearTextureFilter(false);
        this.toolkit.setupSpriteRendering();
        this.toolkit.setActiveTexture(this.texture2d);
        this.toolkit.loadTextureCombineMode(1);
        this.toolkit.activateTexture(1);
        this.toolkit.setActiveTexture(class137Sub1Sub17);
        this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE);
        this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
        this.toolkit.setBlendMode(1);
        i_1 += this.translateX;
        i_2 += this.translateY;
        int i_8 = this.texture2d.singleWidth + i_1;
        int i_9 = this.texture2d.singleLength + i_2;
        float f_10 = class137Sub1Sub17.s / class137Sub1Sub17.singleWidth;
        float f_11 = class137Sub1Sub17.t / class137Sub1Sub17.singleLength;
        float f_12 = (i_1 - i_4) * f_10;
        float f_13 = (i_8 - i_4) * f_10;
        float f_14 = class137Sub1Sub17.t - (i_2 - i_5) * f_11;
        float f_15 = class137Sub1Sub17.t - (i_9 - i_5) * f_11;
        OpenGL.glBegin(7);
        OpenGL.glColor3f(1.0F, 1.0F, 1.0F);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0F, this.texture2d.t);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_12, f_14);
        OpenGL.glVertex2i(i_1, i_2);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0F, 0.0F);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_12, f_15);
        OpenGL.glVertex2i(i_1, this.texture2d.singleLength + i_2);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, this.texture2d.s, 0.0F);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_13, f_15);
        OpenGL.glVertex2i(this.texture2d.singleWidth + i_1, this.texture2d.singleLength + i_2);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, this.texture2d.s, this.texture2d.t);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_13, f_14);
        OpenGL.glVertex2i(this.texture2d.singleWidth + i_1, i_2);
        OpenGL.glEnd();
        this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        this.toolkit.loadTextureCombineMode(0);
        this.toolkit.setActiveTexture(null);
        this.toolkit.activateTexture(0);
    }

    @Override
    public void drawColoredCircle(int i_1, int i_2, int i_3, int i_4, int combineMode, int colorArgb, int blendMode) {
        this.texture2d.setLinearTextureFilter(true);
        this.toolkit.setupSpriteRendering();
        this.toolkit.setBlendMode(blendMode);
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        if (this.stretched) {
            float f_9 = (float) i_3 / getSpriteWidth();
            float f_10 = (float) i_4 / getSpriteHeight();
            float f_11 = i_1 + this.translateX * f_9;
            float f_12 = i_2 + this.translateY * f_10;
            float f_13 = f_11 + this.texture2d.singleWidth * f_9;
            float f_14 = f_12 + this.texture2d.singleLength * f_10;
            if (this.aClass137Sub1Sub19029 == null) {
                this.toolkit.setActiveTexture(this.texture2d);
                this.toolkit.loadTextureCombineMode(combineMode);
                OpenGL.glBegin(GL_QUADS);
                OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
                OpenGL.glVertex2f(f_11, f_12);
                OpenGL.glTexCoord2f(0.0F, 0.0F);
                OpenGL.glVertex2f(f_11, f_14);
                OpenGL.glTexCoord2f(this.texture2d.s, 0.0F);
                OpenGL.glVertex2f(f_13, f_14);
                OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
                OpenGL.glVertex2f(f_13, f_12);
                OpenGL.glEnd();
            } else {
                method14419(combineMode);
                this.aClass137Sub1Sub19029.setLinearTextureFilter(true);
                OpenGL.glBegin(GL_QUADS);
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, this.texture2d.t);
                OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
                OpenGL.glVertex2f(f_11, f_12);
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, 0.0F);
                OpenGL.glTexCoord2f(0.0F, 0.0F);
                OpenGL.glVertex2f(f_11, f_14);
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, this.texture2d.s, 0.0F);
                OpenGL.glTexCoord2f(this.texture2d.s, 0.0F);
                OpenGL.glVertex2f(f_13, f_14);
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, this.texture2d.s, this.texture2d.t);
                OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
                OpenGL.glVertex2f(f_13, f_12);
                OpenGL.glEnd();
                method14415();
            }
        } else if (this.aClass137Sub1Sub19029 == null) {
            this.toolkit.setActiveTexture(this.texture2d);
            this.toolkit.loadTextureCombineMode(combineMode);
            OpenGL.glBegin(GL_QUADS);
            OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
            OpenGL.glVertex2i(i_1, i_2);
            OpenGL.glTexCoord2f(0.0F, 0.0F);
            OpenGL.glVertex2i(i_1, i_2 + i_4);
            OpenGL.glTexCoord2f(this.texture2d.s, 0.0F);
            OpenGL.glVertex2i(i_3 + i_1, i_2 + i_4);
            OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
            OpenGL.glVertex2i(i_3 + i_1, i_2);
            OpenGL.glEnd();
        } else {
            method14419(combineMode);
            this.aClass137Sub1Sub19029.setLinearTextureFilter(true);
            OpenGL.glBegin(GL_QUADS);
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, this.texture2d.t);
            OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
            OpenGL.glVertex2i(i_1, i_2);
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, 0.0F);
            OpenGL.glTexCoord2f(0.0F, 0.0F);
            OpenGL.glVertex2i(i_1, i_2 + i_4);
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, this.texture2d.s, 0.0F);
            OpenGL.glTexCoord2f(this.texture2d.s, 0.0F);
            OpenGL.glVertex2i(i_3 + i_1, i_2 + i_4);
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, this.texture2d.s, this.texture2d.t);
            OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
            OpenGL.glVertex2i(i_3 + i_1, i_2);
            OpenGL.glEnd();
            method14415();
        }

    }

    @Override
    public void method2764(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, int i_8, int i_9) {
        float f_11;
        float f_12;
        if (this.stretched) {
            f_11 = getSpriteWidth();
            f_12 = getSpriteHeight();
            float f_13 = (f_3 - f_1) / f_11;
            float f_14 = (f_4 - f_2) / f_11;
            float f_15 = (f_5 - f_1) / f_12;
            float f_16 = (f_6 - f_2) / f_12;
            float f_17 = f_15 * this.translateY;
            float f_18 = f_16 * this.translateY;
            float f_19 = f_13 * this.translateX;
            float f_20 = f_14 * this.translateX;
            float f_21 = -f_13 * this.anInt9036;
            float f_22 = -f_14 * this.anInt9036;
            float f_23 = -f_15 * this.anInt9035;
            float f_24 = -f_16 * this.anInt9035;
            f_1 = f_1 + f_19 + f_17;
            f_2 = f_2 + f_20 + f_18;
            f_3 = f_3 + f_21 + f_17;
            f_4 = f_4 + f_22 + f_18;
            f_5 = f_5 + f_19 + f_23;
            f_6 = f_6 + f_20 + f_24;
        }

        f_11 = f_5 + (f_3 - f_1);
        f_12 = f_4 + (f_6 - f_2);
        this.texture2d.setLinearTextureFilter(true);
        this.toolkit.setupSpriteRendering();
        this.toolkit.setActiveTexture(this.texture2d);
        this.toolkit.setBlendMode(i_9);
        this.toolkit.loadTextureCombineMode(i_7);
        OpenGL.glColor4ub((byte) (i_8 >> 16), (byte) (i_8 >> 8), (byte) i_8, (byte) (i_8 >> 24));
        OpenGL.glBegin(7);
        OpenGL.glTexCoord2f(0.0F, this.texture2d.t);
        OpenGL.glVertex2f(f_1, f_2);
        OpenGL.glTexCoord2f(0.0F, 0.0F);
        OpenGL.glVertex2f(f_5, f_6);
        OpenGL.glTexCoord2f(this.texture2d.s, 0.0F);
        OpenGL.glVertex2f(f_11, f_12);
        OpenGL.glTexCoord2f(this.texture2d.s, this.texture2d.t);
        OpenGL.glVertex2f(f_3, f_4);
        OpenGL.glEnd();
    }

    @Override
    public int getSpriteWidth() {
        return this.texture2d.singleWidth + this.translateX + this.anInt9036;
    }

    @Override
    public void method2766(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, ComponentSurface class4558, int i_9, int i_10) {
        OpenGl2DTexture_Sub1 class137Sub1Sub111 = ((SpriteSurfaceGL) class4558).surface;
        float f_12;
        float f_13;
        float f_14;
        float f_15;
        if (this.stretched) {
            f_12 = getSpriteWidth();
            f_13 = getSpriteHeight();
            f_14 = (f_3 - f_1) / f_12;
            f_15 = (f_4 - f_2) / f_12;
            float f_16 = (f_5 - f_1) / f_13;
            float f_17 = (f_6 - f_2) / f_13;
            float f_18 = f_16 * this.translateY;
            float f_19 = f_17 * this.translateY;
            float f_20 = f_14 * this.translateX;
            float f_21 = f_15 * this.translateX;
            float f_22 = -f_14 * this.anInt9036;
            float f_23 = -f_15 * this.anInt9036;
            float f_24 = -f_16 * this.anInt9035;
            float f_25 = -f_17 * this.anInt9035;
            f_1 = f_1 + f_20 + f_18;
            f_2 = f_2 + f_21 + f_19;
            f_3 = f_3 + f_22 + f_18;
            f_4 = f_4 + f_23 + f_19;
            f_5 = f_5 + f_20 + f_24;
            f_6 = f_6 + f_21 + f_25;
        }

        f_12 = f_5 + (f_3 - f_1);
        f_13 = f_4 + (f_6 - f_2);
        this.texture2d.setLinearTextureFilter((i_7 & 0x1) != 0);
        this.toolkit.setupSpriteRendering();
        this.toolkit.setActiveTexture(this.texture2d);
        this.toolkit.loadTextureCombineMode(1);
        this.toolkit.activateTexture(1);
        this.toolkit.setActiveTexture(class137Sub1Sub111);
        this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE);
        this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
        this.toolkit.setBlendMode(1);
        f_14 = class137Sub1Sub111.s / class137Sub1Sub111.singleWidth;
        f_15 = class137Sub1Sub111.t / class137Sub1Sub111.singleLength;
        OpenGL.glBegin(7);
        OpenGL.glColor3f(1.0F, 1.0F, 1.0F);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0F, this.texture2d.t);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_14 * (f_1 - i_9), class137Sub1Sub111.t - f_15 * (f_2 - i_10));
        OpenGL.glVertex2f(f_1, f_2);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0F, 0.0F);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_14 * (f_5 - i_9), class137Sub1Sub111.t - f_15 * (f_6 - i_10));
        OpenGL.glVertex2f(f_5, f_6);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, this.texture2d.s, 0.0F);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_14 * (f_12 - i_9), class137Sub1Sub111.t - f_15 * (f_13 - i_10));
        OpenGL.glVertex2f(f_12, f_13);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, this.texture2d.s, this.texture2d.t);
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_14 * (f_3 - i_9), class137Sub1Sub111.t - f_15 * (f_4 - i_10));
        OpenGL.glVertex2f(f_3, f_4);
        OpenGL.glEnd();
        this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        this.toolkit.loadTextureCombineMode(0);
        this.toolkit.setActiveTexture(null);
        this.toolkit.activateTexture(0);
    }

    @Override
    public void cacheSizeInto(int[] ints_1) {
        ints_1[0] = this.translateX;
        ints_1[1] = this.translateY;
        ints_1[2] = this.anInt9036;
        ints_1[3] = this.anInt9035;
    }


    @Override
    public FrameBuffer getFramebuffer() {
        return this.texture2d.createFrameBuffer();
    }

    public void method14419(int i_1) {
        this.toolkit.activateTexture(1);
        this.toolkit.setActiveTexture(this.texture2d);
        this.toolkit.loadTextureCombineMode(this.toolkit.getCombineModeEffectType(i_1), GL_REPLACE);
        this.toolkit.setTexture(1, GL_PRIMARY_COLOR, GL_SRC_COLOR);
        this.toolkit.setTextureTransparency(0, GL_PREVIOUS);
        this.toolkit.activateTexture(0);
        this.toolkit.setActiveTexture(this.aClass137Sub1Sub19029);
        this.toolkit.loadTextureCombineMode(GL_DOT3_RGBA, GL_REPLACE);
        this.toolkit.setTexture(1, GL_CONSTANT, GL_SRC_COLOR);
        if (this.anInt9031 == 0) {
            this.toolkit.updatePointSizeRange(1.0F, 0.5F, 0.5F, 0.0F);
        } else if (this.anInt9031 == 1) {
            this.toolkit.updatePointSizeRange(0.5F, 1.0F, 0.5F, 0.0F);
        } else if (this.anInt9031 == 2) {
            this.toolkit.updatePointSizeRange(0.5F, 0.5F, 1.0F, 0.0F);
        } else if (this.anInt9031 == 3) {
            this.toolkit.updatePointSizeRange(128.5F, 128.5F, 128.5F, 0.0F);
        }

    }

}
