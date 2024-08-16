package com.jagex.game.runetek5.toolkit.opengl.buffer;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import jaclib.memory.Buffer;
import jaggl.OpenGL;

abstract class AbstractArbBufferGL {

    public static final int[] HANDLE = new int[1];
    public boolean deallocated;
    final OpenGlToolkit toolkit;
    public final int bufferTarget;
    public int bufferSize;
    public final boolean streamDraw;
    final int id;

    AbstractArbBufferGL(OpenGlToolkit toolkit, Buffer buffer, int bufferSize) {
    	this.toolkit = toolkit;
        this.bufferTarget = GL_ARRAY_BUFFER;
        this.bufferSize = bufferSize;
        this.streamDraw = false;
        OpenGL.glGenBuffersARB(1, HANDLE, 0);
        this.id = HANDLE[0];
        setAsActiveBuffer();
        OpenGL.glBufferDataARBa(GL_ARRAY_BUFFER, bufferSize, buffer.getBufferAddress(), GL_STATIC_DRAW);
        this.toolkit.vboBuffersSize += this.bufferSize;
    }

    AbstractArbBufferGL(OpenGlToolkit toolkit, int bufferTarget, byte[] data, int bufferSize, boolean streamDraw) {
        this.toolkit = toolkit;
        this.bufferTarget = bufferTarget;
        this.bufferSize = bufferSize;
        this.streamDraw = streamDraw;
        OpenGL.glGenBuffersARB(1, HANDLE, 0);
        this.id = HANDLE[0];
        setAsActiveBuffer();
        OpenGL.glBufferDataARBub(bufferTarget, this.bufferSize, data, 0, this.streamDraw ? GL_STREAM_DRAW : GL_STATIC_DRAW);
        this.toolkit.vboBuffersSize += this.bufferSize;
    }

    @Override
    protected void finalize() throws Throwable {
        if (!this.deallocated) {
            this.toolkit.requestArbBufferCleanup(this.id, this.bufferSize);
            this.deallocated = true;
        }

        super.finalize();
    }

    abstract void setAsActiveBuffer();

    void put(byte[] data, int size) {
        setAsActiveBuffer();
        if (size > this.bufferSize) {
            OpenGL.glBufferDataARBub(this.bufferTarget, size, data, 0, this.streamDraw ? GL_STREAM_DRAW : GL_STATIC_DRAW);
            this.toolkit.vboBuffersSize += size - this.bufferSize;
            this.bufferSize = size;
        } else {
            OpenGL.glBufferSubDataARBub(this.bufferTarget, 0, size, data, 0);
        }
    }
}