package com.jagex.game.runetek5.buffer;

import com.jagex.game.runetek5.graphics.texture.TextureFramebufferGenerator;
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import jaggl.OpenGL;

public class OpenGlNativeRenderBuffer implements TextureFramebufferGenerator, AutoCloseable {

    public int handle;
    public final GLFormat aClass1503880;
    public final GLActionFormat aClass763878;
    public final int useWidth;
    public final int useHeight;
    public final OpenGLNativeToolkit aGraphicalRendererSub2Sub13881;

    public OpenGlNativeRenderBuffer(OpenGLNativeToolkit class505Sub2Sub11, GLFormat class1502, GLActionFormat class763, int i_4, int i_5) {
        this.aClass1503880 = class1502;
        this.aClass763878 = class763;
        this.useWidth = i_4;
        this.useHeight = i_5;
        this.aGraphicalRendererSub2Sub13881 = class505Sub2Sub11;
        int[] bufferHandle = new int[1];
        OpenGL.glGenRenderbuffersEXT(1, bufferHandle, 0);
        this.handle = bufferHandle[0];
        OpenGL.glBindRenderbufferEXT(36161, this.handle);
        OpenGL.glRenderbufferStorageEXT(36161, OpenGLNativeToolkit.texture2dInternalFormat(this.aClass1503880, this.aClass763878), i_4, i_5);
    }

    @Override
    public void close() {
        method5931();
    }

    @Override
    public int getUseWidth() {
        return this.useWidth;
    }

    @Override
    public int getUseHeight() {
        return this.useHeight;
    }

    public void method5931() {
        if (this.handle > 0) {
            this.aGraphicalRendererSub2Sub13881.requestRenderBufferDeletion(this.handle, this.aClass763878.bytesPerEntry * this.aClass1503880.bytesPerPixel * this.useWidth * this.useHeight);
            this.handle = 0;
        }
    }

    @Override
    public void deallocate() {
        if (this.handle > 0) {
            int[] bufferHandle = {this.handle};
            OpenGL.glDeleteRenderbuffersEXT(1, bufferHandle, 0);
            this.handle = 0;
        }
    }

    @Override
    public void generateTextureFramebuffer(int i_1) {
        OpenGL.glFramebufferRenderbufferEXT(36160, i_1, 36161, this.handle);
    }
}
