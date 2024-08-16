package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import jaclib.memory.Buffer;

/**
 * 
 * @author Steven - 13/09/2023
 *
 */
public class GlBuffer {

    public final OpenGlToolkit toolkit;
    public Buffer buffer;

    public GlBuffer(OpenGlToolkit toolkit, Buffer buffer) {
        this.toolkit = toolkit;
        this.buffer = buffer;
    }

    public GlBuffer(OpenGlToolkit toolkit, byte[] data, int initialCapacity) {
    	this.toolkit = toolkit;
    	this.buffer = this.toolkit.nativeHeap.createBuffer(initialCapacity, false);
        if (data != null) {
        	this.buffer.put(data, 0, 0, initialCapacity);
        }
    }

    public void put(byte[] data, int requiredSize) {
        if (this.buffer == null || this.buffer.getSize() < requiredSize) {
            this.buffer = this.toolkit.nativeHeap.createBuffer(requiredSize, false);
        }

        this.buffer.put(data, 0, 0, requiredSize);
    }
}
