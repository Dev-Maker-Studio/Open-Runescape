package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public class OpenGLShaderManager {

    public static final int[] BUFFER = new int[2];
    public final OpenGlToolkit activeToolkit;
    public final int programHandle;

    public OpenGLShaderManager(OpenGlToolkit toolkit, int programHandle) {
        this.activeToolkit = toolkit;
        this.programHandle = programHandle;
    }

    public static OpenGLShaderManager addShaders(OpenGlToolkit toolkit, OpenGlShader[] openglShaders) {
        for (int index = 0; index < openglShaders.length; index++) {
            if (openglShaders[index] == null || openglShaders[index].shaderHandle <= 0) {
                return null;
            }
        }
        int programHandle = OpenGL.glCreateProgram();
        for (int index = 0; index < openglShaders.length; index++) {
            OpenGL.glAttachShader(programHandle, openglShaders[index].shaderHandle);
        }
        OpenGL.glLinkProgram(programHandle);
        OpenGL.glGetProgramiv(programHandle, GL_LINK_STATUS, BUFFER, 0);
        if (BUFFER[0] == 0) {
            OpenGL.glGetProgramiv(programHandle, GL_INFO_LOG_LENGTH, BUFFER, 1);
            if (BUFFER[1] > 1) {
                byte[] infoLogBuffer = new byte[BUFFER[1]];
                OpenGL.glGetProgramInfoLog(programHandle, BUFFER[1], BUFFER, 0, infoLogBuffer, 0);
            }
            if (BUFFER[0] == 0) {
                for (int index = 0; index < openglShaders.length; index++) {
                    OpenGL.glDetachShader(programHandle, openglShaders[index].shaderHandle);
                }
                OpenGL.glDeleteProgram(programHandle);
                return null;
            }
        }
        return new OpenGLShaderManager(toolkit, programHandle);
    }

    @SuppressWarnings("removal")
	@Override
    protected void finalize() throws Throwable {
        this.activeToolkit.requestShaderProgramCleanup(this.programHandle);
        super.finalize();
    }

}
