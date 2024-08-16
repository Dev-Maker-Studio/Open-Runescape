package com.jagex.game.runetek5.toolkit.companion;

public enum TextureCombineMode {

    ADD(0),
    SUBTRACT(1),
    REPLACE(2),
    INTERPOLATE(3),
    MODULATE(4);
    
    public final int mode;

    TextureCombineMode(int mode) {
        this.mode = mode;
    }
}
