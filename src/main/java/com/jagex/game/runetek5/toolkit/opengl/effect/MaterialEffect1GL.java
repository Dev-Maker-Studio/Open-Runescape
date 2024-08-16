package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaggl.OpenGL;

public class MaterialEffect1GL extends MaterialEffectGL {

    public GlListUtils listUtils;
    public boolean enabled;
    public boolean aBool9025;
    public OpenGlArbCubeMapTexture[] cubeMapTextures;

    public MaterialEffect1GL(OpenGlToolkit toolkit) {
        super(toolkit);
        if (toolkit.arbTextureCubeMap) {
            this.aBool9025 = toolkit.maxTextureUnits < 3;
            int i_2 = this.aBool9025 ? 48 : 127;
            byte[][] bytes_3 = new byte[6][4096];
            byte[][] bytes_4 = new byte[6][4096];
            byte[][] bytes_5 = new byte[6][4096];
            int i_6 = 0;

            for (int i_7 = 0; i_7 < 64; i_7++) {
                for (int i_8 = 0; i_8 < 64; i_8++) {
                    float f_9 = 2.0F * i_8 / 64.0F - 1.0F;
                    float f_10 = 2.0F * i_7 / 64.0F - 1.0F;
                    float f_11 = (float) (1.0D / Math.sqrt(f_9 * f_9 + 1.0F + f_10 * f_10));
                    f_9 *= f_11;
                    f_10 *= f_11;

                    for (int i_12 = 0; i_12 < 6; i_12++) {
                        float f_13;
                        if (i_12 == 0) {
                            f_13 = -f_9;
                        } else if (i_12 == 1) {
                            f_13 = f_9;
                        } else if (i_12 == 2) {
                            f_13 = f_10;
                        } else if (i_12 == 3) {
                            f_13 = -f_10;
                        } else if (i_12 == 4) {
                            f_13 = -f_11;
                        } else {
                            f_13 = f_11;
                        }

                        int i_14;
                        int i_15;
                        int i_16;
                        if (f_13 > 0.0F) {
                            i_16 = (int) (Math.pow(f_13, 96.0D) * i_2);
                            i_15 = (int) (Math.pow(f_13, 36.0D) * i_2);
                            i_14 = (int) (Math.pow(f_13, 12.0D) * i_2);
                        } else {
                            i_14 = 0;
                            i_15 = 0;
                            i_16 = 0;
                        }

                        bytes_4[i_12][i_6] = (byte) i_16;
                        bytes_5[i_12][i_6] = (byte) i_15;
                        bytes_3[i_12][i_6] = (byte) i_14;
                    }

                    i_6++;
                }
            }

            this.cubeMapTextures = new OpenGlArbCubeMapTexture[3];
            this.cubeMapTextures[0] = new OpenGlArbCubeMapTexture(super.glToolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, bytes_4, GLFormat.RGBA);
            this.cubeMapTextures[1] = new OpenGlArbCubeMapTexture(super.glToolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, bytes_5, GLFormat.RGBA);
            this.cubeMapTextures[2] = new OpenGlArbCubeMapTexture(super.glToolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, bytes_3, GLFormat.RGBA);
            method14411();
        }

    }

    public void method14411() {
        this.listUtils = new GlListUtils(2);
        this.listUtils.newList(0);
        super.glToolkit.activateTexture(1);
        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_NORMAL_MAP);
        OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_NORMAL_MAP);
        OpenGL.glTexGeni(GL_R, GL_TEXTURE_GEN_MODE, GL_NORMAL_MAP);
        OpenGL.glEnable(GL_TEXTURE_GEN_S);
        OpenGL.glEnable(GL_TEXTURE_GEN_T);
        OpenGL.glEnable(GL_TEXTURE_GEN_R);
        OpenGL.glMatrixMode(GL_TEXTURE);
        OpenGL.glLoadIdentity();
        OpenGL.glRotatef(22.5F, 1.0F, 0.0F, 0.0F);
        OpenGL.glMatrixMode(GL_MODELVIEW);
        if (!this.aBool9025) {
            super.glToolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE);
            super.glToolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
            super.glToolkit.activateTexture(2);
            super.glToolkit.loadTextureCombineMode(GL_ADD, GL_REPLACE);
            super.glToolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
            super.glToolkit.setTexture(1, GL_PREVIOUS, GL_SRC_ALPHA);
        } else {
            super.glToolkit.loadTextureCombineMode(GL_ADD, GL_REPLACE);
            super.glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_ALPHA);
        }
        super.glToolkit.setTextureTransparency(0, GL_PRIMARY_COLOR);

        super.glToolkit.activateTexture(0);
        this.listUtils.endList();
        this.listUtils.newList(1);
        super.glToolkit.activateTexture(1);
        OpenGL.glDisable(GL_TEXTURE_GEN_S);
        OpenGL.glDisable(GL_TEXTURE_GEN_T);
        OpenGL.glDisable(GL_TEXTURE_GEN_R);
        OpenGL.glMatrixMode(GL_TEXTURE);
        OpenGL.glLoadIdentity();
        OpenGL.glMatrixMode(GL_MODELVIEW);
        if (!this.aBool9025) {
            super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
            super.glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
            super.glToolkit.activateTexture(2);
            super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
            super.glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
            super.glToolkit.setTexture(1, GL_PREVIOUS, GL_SRC_COLOR);
        } else {
            super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
            super.glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        }
        super.glToolkit.setTextureTransparency(0, GL_TEXTURE);

        super.glToolkit.activateTexture(0);
        this.listUtils.endList();
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void toggle(boolean bool1) {
        if (this.listUtils != null && bool1) {
            if (!this.aBool9025) {
                super.glToolkit.activateTexture(2);
                super.glToolkit.setActiveTexture(super.glToolkit.texture2d);
                super.glToolkit.activateTexture(0);
            }

            this.listUtils.callList('\u0000');
            this.enabled = true;
        } else {
            super.glToolkit.setTextureTransparency(0, GL_PREVIOUS);
        }

    }

    @Override
    public void loadCombineMode() {
        super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_REPLACE);
    }

    @Override
    public void disable() {
        if (this.enabled) {
            if (!this.aBool9025) {
                super.glToolkit.activateTexture(2);
                super.glToolkit.setActiveTexture(null);
            }

            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(null);
            super.glToolkit.activateTexture(0);
            this.listUtils.callList(1);
            this.enabled = false;
        } else {
            super.glToolkit.setTextureTransparency(0, GL_TEXTURE);
        }

        super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
    }

    @Override
    public void loadCombiner(OpenGlTexture texture, int combineMode) {
        super.glToolkit.setActiveTexture(texture);
        super.glToolkit.loadTextureCombineMode(combineMode);
    }

    @Override
    public void applyEffectParams(int param1, int param2) {
        if (this.enabled) {
            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(this.cubeMapTextures[param1 - 1]);
            super.glToolkit.activateTexture(0);
        }
    }
}
