package com.jagex.game.runetek5.toolkit.opengl;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

/**
 * @author Brainpower 04/03/2023
 */
public class GlListUtils {

    public final int reservedRange;

    public GlListUtils(int range) {
        this.reservedRange = OpenGL.glGenLists(range);
    }

    public void callList(int offset) {
        OpenGL.glCallList(this.reservedRange + offset);
    }

    public void newList(int offset) {
        OpenGL.glNewList(this.reservedRange + offset, GL_COMPILE);
    }

    public void endList() {
        OpenGL.glEndList();
    }
}
