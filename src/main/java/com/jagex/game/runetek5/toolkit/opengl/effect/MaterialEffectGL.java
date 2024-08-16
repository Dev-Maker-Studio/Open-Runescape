package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

public abstract class MaterialEffectGL {

	public final OpenGlToolkit glToolkit;

	public MaterialEffectGL(OpenGlToolkit openglToolkit) {
        this.glToolkit = openglToolkit;
    }

    public abstract boolean isInitialized();

    public abstract void toggle(boolean enabled);

    public abstract void disable();

    public abstract void applyEffectParams(int effectParam1, int effectParam2);

    public abstract void loadCombiner(OpenGlTexture combiner, int combineMode);

    public abstract void loadCombineMode();

}
