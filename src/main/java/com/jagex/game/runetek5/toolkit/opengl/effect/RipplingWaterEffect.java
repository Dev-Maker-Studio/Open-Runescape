package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.graphics.texture.OpenGl1DTexture;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaggl.OpenGL;

/**
 * 
 * @author Steven - 19/07/2023
 * 
 * Displays white waves on water tiles
 *
 */
public class RipplingWaterEffect extends MaterialEffectGL {

    public static final float[] PLANE_PARAMETERS = {0.0F, 0.0F, 0.0F, 0.0F};
    public GlListUtils glLists;
    public final TextureContainerGL textureContainer;
    public final OpenGl1DTexture texture1d;

    public RipplingWaterEffect(OpenGlToolkit toolkit, TextureContainerGL textureContainer) {
        super(toolkit);
        this.textureContainer = textureContainer;
        method14423();
        this.texture1d = new OpenGl1DTexture(super.glToolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, new byte[]{(byte) 0, (byte) -1}, GLFormat.RGBA);
        this.texture1d.setActive();
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void loadCombineMode() {
        super.glToolkit.loadTextureCombineMode(GL_ADD, GL_MODULATE);
    }

    @Override
    public void disable() {
        this.glLists.callList(1);
        if (super.glToolkit.fogDensity > 0) {
            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(null);
            super.glToolkit.configureFog(1.0F, 0.0F);
            super.glToolkit.activateTexture(0);
        }

        super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        OpenGL.glMatrixMode(GL_TEXTURE);
        OpenGL.glPopMatrix();
        OpenGL.glMatrixMode(GL_MODELVIEW);
    }

    @Override
    public void toggle(boolean bool1) {
    	//System.out.println("Enabled material effect "+this.getClass().getSimpleName());
    	if(Loader.MATERIAL_EFFECTS_DISABLED[4]) return;
        if (super.glToolkit.fogDensity > 0) {
            float density = -0.5f / super.glToolkit.fogDensity;
            super.glToolkit.activateTexture(1);
            PLANE_PARAMETERS[0] = 0.0F;
            PLANE_PARAMETERS[1] = 0.0F;
            PLANE_PARAMETERS[2] = density;
            PLANE_PARAMETERS[3] = super.glToolkit.fogStartPosition * density + 0.25F;
            OpenGL.glPushMatrix();
            OpenGL.glLoadIdentity();
            OpenGL.glTexGenfv(GL_S, GL_EYE_PLANE, PLANE_PARAMETERS, 0);
            OpenGL.glPopMatrix();
            super.glToolkit.configureFog(0.5F, super.glToolkit.fogDensity);
            super.glToolkit.setActiveTexture(this.texture1d);
            super.glToolkit.activateTexture(0);
        }

        this.glLists.callList(0);
        OpenGL.glMatrixMode(GL_TEXTURE);
        OpenGL.glPushMatrix();
        OpenGL.glScalef(0.25F, 0.25F, 1.0F);
        OpenGL.glMatrixMode(GL_MODELVIEW);
    }

    @Override
    public void loadCombiner(OpenGlTexture texture, int combineMode) {
    	// NOOP
    }

    public void method14423() {
        this.glLists = new GlListUtils(2);
        this.glLists.newList(0);
        super.glToolkit.activateTexture(1);
        super.glToolkit.loadTextureCombineMode(GL_REPLACE, GL_ADD);
        super.glToolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR);
        OpenGL.glEnable(GL_TEXTURE_GEN_S);
        super.glToolkit.activateTexture(0);
        OpenGL.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 2.0F);
        
        if (this.textureContainer.texture3dSupported) {
            OpenGL.glTexGeni(GL_R, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            OpenGL.glTexGeni(GL_Q, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            OpenGL.glTexGenfv(GL_Q, GL_OBJECT_PLANE, new float[]{0.0F, 0.0F, 0.0F, 1.0F}, 0);
            OpenGL.glEnable(GL_TEXTURE_GEN_R);
            OpenGL.glEnable(GL_TEXTURE_GEN_Q);
        }

        this.glLists.endList();
        this.glLists.newList(1);
        super.glToolkit.activateTexture(1);
        super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        super.glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        OpenGL.glDisable(GL_TEXTURE_GEN_S);
        super.glToolkit.activateTexture(0);
        OpenGL.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 1.0F);
        if (this.textureContainer.texture3dSupported) {
            OpenGL.glDisable(GL_TEXTURE_GEN_R);
            OpenGL.glDisable(GL_TEXTURE_GEN_Q);
        }

        this.glLists.endList();
    }

    @Override
    public void applyEffectParams(int param1, int param2) {
        if ((param1 & 0x1) == 1) {
            if (this.textureContainer.texture3dSupported) {
                super.glToolkit.setActiveTexture(this.textureContainer.texture3d);
                PLANE_PARAMETERS[0] = 0.0F;
                PLANE_PARAMETERS[1] = 0.0F;
                PLANE_PARAMETERS[2] = 0.0F;
                PLANE_PARAMETERS[3] = (super.glToolkit.textureAnimatingProgression % 4000) / 4000.0F;
                OpenGL.glTexGenfv(GL_R, GL_OBJECT_PLANE, PLANE_PARAMETERS, 0);
            } else {
                int step = super.glToolkit.textureAnimatingProgression % 4000 * 16 / 4000;
                super.glToolkit.setActiveTexture(this.textureContainer.textures2d[step]);
            }
        } else if (this.textureContainer.texture3dSupported) {
            super.glToolkit.setActiveTexture(this.textureContainer.texture3d);
            PLANE_PARAMETERS[0] = 0.0F;
            PLANE_PARAMETERS[1] = 0.0F;
            PLANE_PARAMETERS[2] = 0.0F;
            PLANE_PARAMETERS[3] = 0.0F;
            OpenGL.glTexGenfv(GL_R, GL_OBJECT_PLANE, PLANE_PARAMETERS, 0);
        } else {
            super.glToolkit.setActiveTexture(this.textureContainer.textures2d[0]);
        }
    }
}
