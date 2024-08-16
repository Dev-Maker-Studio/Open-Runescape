package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.buffer.ArrayBufferGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import jaclib.memory.Buffer;

/**
 * 
 * @author Steven - 13/09/2023
 *
 */
public class GlArrayBuffer extends GlBuffer implements ArrayBufferGL {

	/**
	 * Padding between elements
	 */
    public int stride;

    public GlArrayBuffer(OpenGlToolkit toolkit, int stride, byte[] buffer, int initialCapacity) {
        super(toolkit, buffer, initialCapacity);
        this.stride = stride;
    }

    public GlArrayBuffer(OpenGlToolkit toolkit, int stride, Buffer buffer) {
        super(toolkit, buffer);
        this.stride = stride;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public long getBufferAddress() {
        return super.buffer.getBufferAddress();
    }

    @Override
    public void putArray(int stride, byte[] data, int requiredSize) {
        put(data, requiredSize);
        this.stride = stride;
    }

    @Override
    public int getStride() {
        return this.stride;
    }
}
