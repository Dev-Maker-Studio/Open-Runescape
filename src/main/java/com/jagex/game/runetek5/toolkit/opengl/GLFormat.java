package com.jagex.game.runetek5.toolkit.opengl;

public class GLFormat {

    public static final GLFormat aClass1501950 = new GLFormat(5, 3);
    public static final GLFormat HDR = new GLFormat(6, 4);
    public static final GLFormat RGBA = new GLFormat(1, 1);
    public static final GLFormat aClass1501948 = new GLFormat(0, 1);
    public static final GLFormat aClass1501952 = new GLFormat(4, 2);
    public static final GLFormat aClass1501953 = new GLFormat(8, 1);
    public static final GLFormat aClass1501956 = new GLFormat(7, 1);
    public static final GLFormat aClass1501957 = new GLFormat(9, 1);
    public final int formatType;

    public final int bytesPerPixel;

    public GLFormat(int internalFormatType, int i_2) {
        this.formatType = internalFormatType;
        this.bytesPerPixel = i_2;
    }

}
