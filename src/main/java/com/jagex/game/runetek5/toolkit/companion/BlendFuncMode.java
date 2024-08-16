package com.jagex.game.runetek5.toolkit.companion;

public enum BlendFuncMode {

    OPAQUE_BLENDING(0), 	// Needs refactor
    ALPHA_BLENDING(1),
    OPAQUE(2), 	
    COLOR_BLENDING(3);// Needs refactor
    
	public final int mode;

    BlendFuncMode(int mode) {
        this.mode = mode;
    }
}
