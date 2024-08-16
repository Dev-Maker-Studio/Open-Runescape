package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

/**
 * 
 * @author Steven - 19/07/2023
 * Disables lighting to prevent colors from becoming too bright
 * Example at coords: 2967, 3303, 0
 *
 */
public class HideLightsEffect extends MaterialEffectGL {

	public HideLightsEffect(OpenGlToolkit toolkit) {
        super(toolkit);
    }

    @Override
    public void loadCombiner(OpenGlTexture texture, int combineMode) {
        super.glToolkit.setActiveTexture(texture);
        super.glToolkit.loadTextureCombineMode(combineMode);
    }

    @Override
    public void toggle(boolean enabled) {
    	//System.out.println("Enabled material effect "+this.getClass().getSimpleName());
    	if(Loader.MATERIAL_EFFECTS_DISABLED[6]) return;
        super.glToolkit.setHideLights(true);
    }

    @Override
    public void loadCombineMode() {
    	// NOOP
    }

    @Override
    public void applyEffectParams(int param1, int param2) {
    	// NOOP
    }

    @Override
    public void disable() {
        super.glToolkit.setHideLights(false);
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

}
