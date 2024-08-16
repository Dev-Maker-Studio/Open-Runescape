package com.jagex.game.runetek5.buffer;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaggl.OpenGL;

public class OpenGlRenderBuffer extends CacheableNode implements NativeRenderBuffer {

    public static final int[] BUFFERS = new int[1];
    public int handle;
    public final OpenGlToolkit activeToolkit;
    public final int width;
    public final int height;
    public final GLFormat format;
    public final GLActionFormat actionformat;
    public final int anInt10284;

    public OpenGlRenderBuffer(OpenGlToolkit openglToolkit, GLFormat glformat, GLActionFormat glactionformat, int width, int height, int sample) {
    	this.activeToolkit = openglToolkit;
        this.width = width;
        this.height = height;
        this.format = glformat;
        this.actionformat = glactionformat;
        OpenGL.glGenRenderbuffersEXT(1, BUFFERS, 0);
        this.handle = BUFFERS[0];
        OpenGL.glBindRenderbufferEXT(GL_RENDERBUFFER, this.handle);
        OpenGL.glRenderbufferStorageMultisampleEXT(GL_RENDERBUFFER, sample, OpenGlToolkit.getInternalTextureFormat(this.format, this.actionformat), this.width, this.height);
        this.anInt10284 = this.format.bytesPerPixel * this.actionformat.bytesPerEntry * this.width * this.height;
    }

    public OpenGlRenderBuffer(OpenGlToolkit class505Sub11, GLFormat glformat, GLActionFormat glactionformat, int width, int height) {
    	this.activeToolkit = class505Sub11;
        this.width = width;
        this.height = height;
        this.format = glformat;
        this.actionformat = glactionformat;
        OpenGL.glGenRenderbuffersEXT(1, BUFFERS, 0);
        this.handle = BUFFERS[0];
        OpenGL.glBindRenderbufferEXT(GL_RENDERBUFFER, this.handle);
        OpenGL.glRenderbufferStorageEXT(GL_RENDERBUFFER, OpenGlToolkit.getInternalTextureFormat(this.format, this.actionformat), this.width, this.height);
        this.anInt10284 = this.format.bytesPerPixel * this.actionformat.bytesPerEntry * this.width * this.height;
    }

    @Override
    public int getUseHeight() {
        return this.height;
    }

    @Override
    public int getUseWidth() {
        return this.width;
    }

    @Override
    public void deallocate() {
        if (this.handle > 0) {
            int[] activeHandle = {this.handle};
            OpenGL.glDeleteRenderbuffersEXT(1, activeHandle, 0);
            this.handle = 0;
        }
    }

    public void dispose() {
        if (this.handle > 0) {
            this.activeToolkit.requestRenderBufferCleanup(this.handle, this.anInt10284);
            this.handle = 0;
        }
    }

    @SuppressWarnings("removal")
	@Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    @Override
    public void getFrameRenderBuffer(int attachment) {
        OpenGL.glFramebufferRenderbufferEXT(GL_FRAMEBUFFER, attachment, GL_RENDERBUFFER, this.handle);
    }
}
