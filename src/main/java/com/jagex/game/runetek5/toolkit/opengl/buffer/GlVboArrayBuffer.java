package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.buffer.ArrayBufferGL;

import jaclib.memory.Buffer;

/**
 * 
 * @author Steven - 13/09/2023
 *
 */
public class GlVboArrayBuffer extends AbstractArbBufferGL implements ArrayBufferGL {

    public int stride;

    public GlVboArrayBuffer(OpenGlToolkit toolkit, int stride, byte[] data, int bufferSize, boolean streamDraw) {
        super(toolkit, GL_ARRAY_BUFFER, data, bufferSize, streamDraw);
        this.stride = stride;
    }

    public GlVboArrayBuffer(OpenGlToolkit toolkit, int stride, Buffer buffer, int bufferSize) {
        super(toolkit, buffer, bufferSize);
        this.stride = stride;
    }

    @Override
    public void putArray(int stride, byte[] data, int size) {
        put(data, size);
        this.stride = stride;
    }

    @Override
    public int getStride() {
        return this.stride;
    }

    @Override
    public long getBufferAddress() {
        return 0L;
    }

    @Override
    void setAsActiveBuffer() {
    	this.toolkit.setArrayBuffer(this);
    }

    @Override
    public int getId() {
        return super.id;
    }
}
