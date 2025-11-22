package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit

/**
 *
 * @author Steven - 19/07/2023
 * Disables lighting to prevent colors from becoming too bright
 * Example at coords: 2967, 3303, 0
 *
 */
class HideLightsEffect(toolkit: OpenGlToolkit) : MaterialEffectGL(toolkit) {

    override fun loadCombiner(texture: OpenGlTexture?, combineMode: Int) {
        super.glToolkit.setActiveTexture(texture)
        super.glToolkit.loadTextureCombineMode(combineMode)
    }

    override fun toggle(enabled: Boolean) {
        //System.out.println("Enabled material effect "+this.getClass().getSimpleName());
        if (Loader.MATERIAL_EFFECTS_DISABLED[6]) return
        super.glToolkit.setHideLights(true)
    }

    override fun loadCombineMode() {
        // NOOP
    }

    override fun applyEffectParams(param1: Int, param2: Int) {
        // NOOP
    }

    override fun disable() {
        super.glToolkit.setHideLights(false)
    }

    override fun isInitialized(): Boolean {
        return true
    }
}
