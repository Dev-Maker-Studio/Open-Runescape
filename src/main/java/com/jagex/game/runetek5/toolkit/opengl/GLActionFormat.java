package com.jagex.game.runetek5.toolkit.opengl;

public class GLActionFormat {

    public static final GLActionFormat PIXEL_FORMAT = new GLActionFormat(4, 1);
    public static final GLActionFormat TEXTURE_FORMAT = new GLActionFormat(5, 2);
    public static final GLActionFormat DEPTH_COMPONENT_24 = new GLActionFormat(1, 3);
    public static final GLActionFormat FLOAT_16_ARB = new GLActionFormat(8, 2);
    public static final GLActionFormat FLOAT_32_ARB = new GLActionFormat(3, 4);
    static GLActionFormat aClass76753 = new GLActionFormat(6, 1);
    static GLActionFormat aClass76756 = new GLActionFormat(0, 2);
    static GLActionFormat aClass76750 = new GLActionFormat(7, 4);
    static GLActionFormat aClass76754 = new GLActionFormat(2, 4);
    public final int anInt757;

    public final int bytesPerEntry;

    public GLActionFormat(int i_1, int bytesPerEntry) {
    	this.anInt757 = i_1;
        this.bytesPerEntry = bytesPerEntry;
    }
}
