package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.buffer.ElementsBufferGL;

/**
 * 
 * @author Steven - 13/09/2023
 *
 */
public class GlVboElementsArrayBuffer extends AbstractArbBufferGL implements ElementsBufferGL {

    public GlVboElementsArrayBuffer(OpenGlToolkit toolkit, byte[] data, int bufferSize, boolean streamDraw) {
        super(toolkit, GL_ELEMENT_ARRAY_BUFFER, data, bufferSize, streamDraw);
    }

    @Override
    public int getId() {
        return super.id;
    }

    @Override
    public long getBufferAddress() {
        return 0L;
    }

    @Override
    public void putElements(byte[] data, int size) {
        put(data, size);
    }

    @Override
    void setAsActiveBuffer() {
        super.toolkit.setElementsBuffer(this);
    }
}
