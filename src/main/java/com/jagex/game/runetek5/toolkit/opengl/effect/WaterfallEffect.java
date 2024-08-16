package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.launch.Loader;

/**
 * 
 * @author Brainpower - 04/03/2023
 * Handles the display of waterfall,
 * without this the water flowing from
 * an edge downwards renders as a white texture
 * Example at coords: 2507, 3459, 0
 *
 */
public class WaterfallEffect extends MaterialEffectGL {

	/**
	 * Example: https://cpp.hotexamples.com/examples/-/-/glTexGenfv/cpp-gltexgenfv-function-examples.html
	 * float X[4] = { 1,0,0,0 };
	 * float Y[4] = { 0,1,0,0 };
	 * float Z[4] = { 0,0,1,0 };
	 * glTexGenfv( GL_S, GL_OBJECT_PLANE, X );
	 * glTexGenfv( GL_T, GL_OBJECT_PLANE, Y );
	 * glTexGenfv( GL_R, GL_OBJECT_PLANE, Z );
	 */
    public static final float[] PLANE_PARAMETERS = new float[4];
    
    public final TextureContainerGL textureContainer;
    public final GlListUtils listUtils;

    public WaterfallEffect(OpenGlToolkit openglToolkit, TextureContainerGL textureContainer) {
        super(openglToolkit);
        this.textureContainer = textureContainer;
        this.listUtils = new GlListUtils(2);
        this.listUtils.newList(0);
        super.glToolkit.activateTexture(1);
        if (this.textureContainer.texture3dSupported) {
            OpenGL.glTexGeni(GL_R, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            OpenGL.glEnable(GL_TEXTURE_GEN_R);
        }

        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR);
        OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR);
        OpenGL.glEnable(GL_TEXTURE_GEN_S);
        OpenGL.glEnable(GL_TEXTURE_GEN_T);
        super.glToolkit.activateTexture(0);
        this.listUtils.endList();
        this.listUtils.newList(1);
        super.glToolkit.activateTexture(1);
        if (this.textureContainer.texture3dSupported) {
            OpenGL.glDisable(GL_TEXTURE_GEN_R);
        }

        OpenGL.glDisable(GL_TEXTURE_GEN_S);
        OpenGL.glDisable(GL_TEXTURE_GEN_T);
        super.glToolkit.activateTexture(0);
        this.listUtils.endList();
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void loadCombineMode() {
    }

    @Override
    public void toggle(boolean enabled) {
    	//System.out.println("Enabled material effect "+this.getClass().getSimpleName());
    	if(Loader.MATERIAL_EFFECTS_DISABLED[5]) return;
        this.listUtils.callList(0);
        if (this.textureContainer.texture3dSupported) {
            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(this.textureContainer.aClass137Sub41636);
            super.glToolkit.activateTexture(0);
        }
    }

    @Override
    public void disable() {
        this.listUtils.callList(1);
        super.glToolkit.activateTexture(1);
        super.glToolkit.setActiveTexture(null);
        super.glToolkit.activateTexture(0);
    }

    @Override
    public void applyEffectParams(int param1, int param2) {
        float wNeg = -5.0E-4f * ((param1 & 0x3) + 1);
        float wPos = 5.0E-4F * ((param1 >> 3 & 0x3) + 1);
        float position = (param1 & 0x40) == 0 ? 4.8828125E-4F : 9.765625E-4F;
        boolean flipped = (param1 & 0x80) != 0;
        super.glToolkit.activateTexture(1);
        if (flipped) { 
        	// X
            PLANE_PARAMETERS[0] = position;
            PLANE_PARAMETERS[1] = 0.0F;
            PLANE_PARAMETERS[2] = 0.0F;
        } else {
        	// Z
            PLANE_PARAMETERS[0] = 0.0F;
            PLANE_PARAMETERS[1] = 0.0F;
            PLANE_PARAMETERS[2] = position;
        }
        PLANE_PARAMETERS[3] = 0.0F;

        OpenGL.glTexGenfv(GL_S, GL_EYE_PLANE, PLANE_PARAMETERS, 0);
        
        // Y
        PLANE_PARAMETERS[0] = 0.0F;
        PLANE_PARAMETERS[1] = position;
        PLANE_PARAMETERS[2] = 0.0F;
        PLANE_PARAMETERS[3] = super.glToolkit.textureAnimatingProgression * wNeg % 1.0F;
        
        OpenGL.glTexGenfv(GL_T, GL_EYE_PLANE, PLANE_PARAMETERS, 0);
        if (!this.textureContainer.texture3dSupported) {
            int texture = (int) (wPos * super.glToolkit.textureAnimatingProgression * 16.0F);
            super.glToolkit.setActiveTexture(this.textureContainer.aClass137Sub1Array1634[texture % 16]);
        } else {
            PLANE_PARAMETERS[0] = 0.0F;
            PLANE_PARAMETERS[1] = 0.0F;
            PLANE_PARAMETERS[2] = 0.0F;
            PLANE_PARAMETERS[3] = super.glToolkit.textureAnimatingProgression * wPos % 1.0F;
            OpenGL.glTexGenfv(GL_R, GL_OBJECT_PLANE, PLANE_PARAMETERS, 0);
        }

        super.glToolkit.activateTexture(0);
    }

    @Override
    public void loadCombiner(OpenGlTexture texture, int mode) {
        super.glToolkit.setActiveTexture(texture);
        super.glToolkit.loadTextureCombineMode(mode);
    }

}
