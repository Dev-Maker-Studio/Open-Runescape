package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import jaggl.OpenGL;

public class OpenGlShader {

    public static final int[] FETCHED_SHADER_PARAMETERS = new int[2];
    public final OpenGlToolkit activeToolkit;
    final int shaderHandle;

    public OpenGlShader(OpenGlToolkit toolkit, int handle) {
        this.activeToolkit = toolkit;
        this.shaderHandle = handle;
    }

    public static OpenGlShader createShader(OpenGlToolkit toolkit, int shaderType, String string_2) {
        int shaderHandle = OpenGL.glCreateShader(shaderType);
        OpenGL.glShaderSource(shaderHandle, string_2);
        OpenGL.glCompileShader(shaderHandle);
        OpenGL.glGetShaderiv(shaderHandle, 35713, FETCHED_SHADER_PARAMETERS, 0);
        if (FETCHED_SHADER_PARAMETERS[0] == 0) {
            OpenGL.glGetShaderiv(shaderHandle, 35716, FETCHED_SHADER_PARAMETERS, 1);
            if (FETCHED_SHADER_PARAMETERS[1] > 1) {
                byte[] shaderInfoLog = new byte[FETCHED_SHADER_PARAMETERS[1]];
                OpenGL.glGetShaderInfoLog(shaderHandle, FETCHED_SHADER_PARAMETERS[1], FETCHED_SHADER_PARAMETERS, 0, shaderInfoLog, 0);
                System.out.println(new String(shaderInfoLog));
            }
            if (FETCHED_SHADER_PARAMETERS[0] == 0) {
                OpenGL.glDeleteShader(shaderHandle);
                return null;
            }
        }
        return new OpenGlShader(toolkit, shaderHandle);
    }

    @SuppressWarnings("removal")
	@Override
    protected void finalize() throws Throwable {
    	this.activeToolkit.requestShaderProgramCleanup(this.shaderHandle);
        super.finalize();
    }

}
