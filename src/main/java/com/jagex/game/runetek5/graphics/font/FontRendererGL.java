package com.jagex.game.runetek5.graphics.font;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture_Sub1;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils;
import com.jagex.game.runetek5.unidentified.SpriteSurfaceGL;

import jaggl.OpenGL;

public class FontRendererGL extends AbstractFontRenderer {

    public final OpenGlToolkit toolkit;
    public final OpenGl2DTexture_Sub1 texture;
    public final boolean aBool9326;
    public final GlListUtils listUtils;

    public FontRendererGL(OpenGlToolkit toolkit, FontMetrics fontmetrics2, SpriteType[] characterSprites, boolean bool4) {
        super(toolkit, fontmetrics2);
        this.toolkit = toolkit;
        int maxDimension = 0;

        for (int charIndex = 0; charIndex < 256; charIndex++) {
            SpriteType charSprite = characterSprites[charIndex];
            if (charSprite.height > maxDimension) {
                maxDimension = charSprite.height;
            }

            if (charSprite.width > maxDimension) {
                maxDimension = charSprite.width;
            }
        }

        int size = maxDimension * 16;
        if (bool4) {
            byte[] pixels = new byte[size * size];

            for (int charIndex = 0; charIndex < 256; charIndex++) {
            	SpriteType charSprite = characterSprites[charIndex];
                int i_26 = charSprite.height;
                int i_11 = charSprite.width;
                int i_12 = maxDimension * (charIndex % 16);
                int i_13 = charIndex / 16 * maxDimension;
                int i_14 = i_12 + i_13 * size;
                int i_15 = 0;
                byte[] bytes_27;
                if (charSprite.alpha == null) {
                    bytes_27 = charSprite.pixels;

                    for (int i_17 = 0; i_17 < i_26; i_17++) {
                        for (int i_18 = 0; i_18 < i_11; i_18++) {
                            pixels[i_14++] = (byte) (bytes_27[i_15++] == 0 ? 0 : -1);
                        }

                        i_14 += size - i_11;
                    }
                } else {
                    bytes_27 = charSprite.alpha;

                    for (int i_17 = 0; i_17 < i_26; i_17++) {
                        for (int i_18 = 0; i_18 < i_11; i_18++) {
                            pixels[i_14++] = bytes_27[i_15++];
                        }

                        i_14 += size - i_11;
                    }
                }
            }

            this.texture = OpenGl2DTexture_Sub1.createSurface(toolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, size, size, pixels, GLFormat.RGBA);
            this.aBool9326 = true;
        } else {
            int[] pixels = new int[size * size];

            for (int charIndex = 0; charIndex < 256; charIndex++) {
            	SpriteType charSprite = characterSprites[charIndex];
                int[] palette = charSprite.pallete;
                byte[] alpha = charSprite.alpha;
                byte[] charPixels = charSprite.pixels;
                int height = charSprite.height;
                int width = charSprite.width;
                int i_15 = maxDimension * (charIndex % 16);
                int i_16 = charIndex / 16 * maxDimension;
                int i_17 = i_15 + i_16 * size;
                int pixel = 0;
                if (alpha != null) {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            pixels[i_17++] = alpha[pixel] << 24 | palette[charPixels[pixel++] & 0xff];
                        }

                        i_17 += size - width;
                    }
                } else {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            byte charPixel = charPixels[pixel++];
                            if (charPixel != 0) {
                                pixels[i_17++] = ~0xffffff | palette[charPixel & 0xff];
                            } else {
                                i_17++;
                            }
                        }

                        i_17 += size - width;
                    }
                }
            }

            this.texture = OpenGl2DTexture_Sub1.method15537(toolkit, size, size, pixels, 0, 0);
            this.aBool9326 = false;
        }

        this.texture.setLinearTextureFilter(false);
        this.listUtils = new GlListUtils(256);
        float f_22 = this.texture.s / this.texture.singleWidth;
        float f_30 = this.texture.t / this.texture.singleLength;

        for (int spriteIndex = 0; spriteIndex < 256; spriteIndex++) {
            SpriteType spriteType = characterSprites[spriteIndex];
            int height = spriteType.height;
            int width = spriteType.width;
            int minY = spriteType.minY;
            int minX = spriteType.minX;
            float f_32 = (maxDimension * (spriteIndex % 16));
            float f_33 = (spriteIndex / 16 * maxDimension);
            float f_34 = f_32 * f_22;
            float f_35 = f_33 * f_30;
            float f_36 = (f_32 + width) * f_22;
            float f_37 = (f_33 + height) * f_30;
            this.listUtils.newList(spriteIndex);
            OpenGL.glBegin(GL_QUADS);
            OpenGL.glTexCoord2f(f_34, this.texture.t - f_35);
            OpenGL.glVertex2i(minX, minY);
            OpenGL.glTexCoord2f(f_34, this.texture.t - f_37);
            OpenGL.glVertex2i(minX, minY + height);
            OpenGL.glTexCoord2f(f_36, this.texture.t - f_37);
            OpenGL.glVertex2i(minX + width, minY + height);
            OpenGL.glTexCoord2f(f_36, this.texture.t - f_35);
            OpenGL.glVertex2i(minX + width, minY);
            OpenGL.glEnd();
            this.listUtils.endList();
        }

    }

    @Override
    public void drawChar(int charToDraw, int translateX, int translateY, int colorArgb, boolean bool5, ComponentSurface surface, int i_7, int i_8) {
        SpriteSurfaceGL spriteSurface = (SpriteSurfaceGL) surface;
        OpenGl2DTexture_Sub1 class137Sub1Sub110 = spriteSurface.surface;
        this.toolkit.setupTextRendering();
        this.toolkit.setActiveTexture(this.texture);
        if (!this.aBool9326 && !bool5) {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_REPLACE);
        } else {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE);
            this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
        }

        this.toolkit.activateTexture(1);
        this.toolkit.setActiveTexture(class137Sub1Sub110);
        this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE);
        this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR);
        OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR);
        float f_11 = class137Sub1Sub110.s / class137Sub1Sub110.singleWidth;
        float f_12 = class137Sub1Sub110.t / class137Sub1Sub110.singleLength;
        OpenGL.glTexGenfv(GL_S, GL_EYE_PLANE, new float[]{f_11, 0.0F, 0.0F, (-i_7) * f_11}, 0);
        OpenGL.glTexGenfv(GL_T, GL_EYE_PLANE, new float[]{0.0F, f_12, 0.0F, (-i_8) * f_12}, 0);
        OpenGL.glEnable(GL_TEXTURE_GEN_S);
        OpenGL.glEnable(GL_TEXTURE_GEN_T);
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        OpenGL.glTranslatef(translateX, translateY, 0.0F);
        this.listUtils.callList(charToDraw);
        OpenGL.glLoadIdentity();
        OpenGL.glDisable(GL_TEXTURE_GEN_S);
        OpenGL.glDisable(GL_TEXTURE_GEN_T);
        this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        this.toolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        this.toolkit.setActiveTexture(null);
        this.toolkit.activateTexture(0);
        if (this.aBool9326 || bool5) {
            this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        }
    }

    @Override
    public void drawChar(int character, int translateX, int translateY, int colorArgb, boolean bool5) {
        this.toolkit.setupTextRendering();
        this.toolkit.setActiveTexture(this.texture);
        if (!this.aBool9326 && !bool5) {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_REPLACE);
        } else {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE);
            this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
        }

        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        OpenGL.glTranslatef(translateX, translateY, 0.0F);
        this.listUtils.callList(character);
        OpenGL.glLoadIdentity();
        if (this.aBool9326 || bool5) {
            this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        }
    }
}
