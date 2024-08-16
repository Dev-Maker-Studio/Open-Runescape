package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaggl.OpenGL;

/**
 * 
 * @author Brainpower - 19/07/2023
 * Handles textures that reflect light
 * Example at coords; 2854, 3168, 0
 *
 */
public class ReflectingTextureEffect extends MaterialEffectGL {

    public boolean loaded;
    public GlListUtils listUtils;

    public ReflectingTextureEffect(OpenGlToolkit toolkit) {
        super(toolkit);
        if (toolkit.arbTextureCubeMap) {
            this.listUtils = new GlListUtils(2);
            this.listUtils.newList(0);
            super.glToolkit.activateTexture(1);
            super.glToolkit.loadTextureCombineMode(GL_INTERPOLATE, GL_REPLACE);
            super.glToolkit.setTexture(2, GL_PREVIOUS, GL_SRC_ALPHA);
            super.glToolkit.setTextureTransparency(0, GL_PRIMARY_COLOR);
            OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP);
            OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP);
            OpenGL.glTexGeni(GL_R, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP);
            OpenGL.glEnable(GL_TEXTURE_GEN_S);
            OpenGL.glEnable(GL_TEXTURE_GEN_T);
            OpenGL.glEnable(GL_TEXTURE_GEN_R);
            super.glToolkit.activateTexture(0);
            this.listUtils.endList();
            this.listUtils.newList(1);
            super.glToolkit.activateTexture(1);
            super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
            super.glToolkit.setTexture(2, GL_CONSTANT, GL_SRC_ALPHA);
            super.glToolkit.setTextureTransparency(0, GL_TEXTURE);
            OpenGL.glDisable(GL_TEXTURE_GEN_S);
            OpenGL.glDisable(GL_TEXTURE_GEN_T);
            OpenGL.glDisable(GL_TEXTURE_GEN_R);
            OpenGL.glMatrixMode(GL_TEXTURE);
            OpenGL.glLoadIdentity();
            OpenGL.glMatrixMode(GL_MODELVIEW);
            super.glToolkit.activateTexture(0);
            this.listUtils.endList();
        }
    }

    @Override
    public void disable() {
        if (this.loaded) {
            this.listUtils.callList(1);
            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(null);
            super.glToolkit.activateTexture(0);
        } else {
            super.glToolkit.setTextureTransparency(0, GL_TEXTURE);
        }

        super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        this.loaded = false;
    }

    @Override
    public void applyEffectParams(int param1, int param2) {
    	// NOOP
    }

    @Override
    public void loadCombiner(OpenGlTexture texture, int combineMode) {
        super.glToolkit.setActiveTexture(texture);
        super.glToolkit.loadTextureCombineMode(combineMode);
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void loadCombineMode() {
        super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_REPLACE);
    }

    @Override
    public void toggle(boolean enabled) {
    	//System.out.println("Enabled material effect "+this.getClass().getSimpleName());
    	if(Loader.MATERIAL_EFFECTS_DISABLED[7]) return;
        OpenGlArbCubeMapTexture cubeMap = super.glToolkit.getCubeMap();
        if (this.listUtils != null && cubeMap != null && enabled) {
            this.listUtils.callList(0);
            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(cubeMap);
            OpenGL.glMatrixMode(GL_TEXTURE);
            OpenGL.glLoadMatrixf(super.glToolkit.aClass3848391.method6534(super.glToolkit.textureMatrix), 0);
            OpenGL.glMatrixMode(GL_MODELVIEW);
            super.glToolkit.activateTexture(0);
            this.loaded = true;
        } else {
            super.glToolkit.setTextureTransparency(0, GL_PREVIOUS);
        }
    }
}
