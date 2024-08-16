package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.buffer.ElementsBufferGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBuffer;

/**
 * 
 * @author Steven - 13/09/2023
 *
 */
public class GlElementsArrayBuffer extends GlBuffer implements ElementsBufferGL {

    public GlElementsArrayBuffer(OpenGlToolkit toolkit, byte[] data, int initialCapacity) {
        super(toolkit, data, initialCapacity);
    }

    @Override
    public void putElements(byte[] data, int size) {
        put(data, size);
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public long getBufferAddress() {
        return super.buffer.getBufferAddress();
    }
}
