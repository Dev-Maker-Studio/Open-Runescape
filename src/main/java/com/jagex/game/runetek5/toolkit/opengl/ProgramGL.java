package com.jagex.game.runetek5.toolkit.opengl;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public class ProgramGL {

    public static final int[] HANDLE = new int[1];
    public final OpenGlToolkit toolkit;
    public final int programHandle;

    public ProgramGL(OpenGlToolkit openglToolkit, int handle) {
        this.toolkit = openglToolkit;
        this.programHandle = handle;
    }

    public static ProgramGL generateProgram(OpenGlToolkit toolkit, String programCode) {
        int programLocation = OpenGL.glGenProgramARB();
        OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, programLocation);
        OpenGL.glProgramStringARB(GL_VERTEX_PROGRAM_ARB, 34933, programCode);
        OpenGL.glGetIntegerv(34379, HANDLE, 0);
        if (HANDLE[0] != -1) {
            OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, 0);
            return null;
        } else {
            OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, 0);
            return new ProgramGL(toolkit, programLocation);
        }
    }

    @SuppressWarnings("removal")
	@Override
    protected void finalize() throws Throwable {
        this.toolkit.requestArbProgramCleanup(this.programHandle);
        super.finalize();
    }

}
