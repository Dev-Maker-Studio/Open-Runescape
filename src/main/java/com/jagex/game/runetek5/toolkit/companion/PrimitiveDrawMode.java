package com.jagex.game.runetek5.toolkit.companion;

public enum PrimitiveDrawMode {

    LINE_STRIP(0),
    LINES(1),
    TRIANGLE_STRIP(2),
    TRIANGLE_FAN(3),
    TRIANGLES(4),
    GL_POINTS(5);
	
    public final int mode;

    PrimitiveDrawMode(int mode) {
        this.mode = mode;
    }

}
