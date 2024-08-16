package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaclib.memory.Source;

/**
 * 
 * @author Steven - 13/09/2023
 *
 */
public class OpenGlArbArrayBuffer extends OpenGlArbBuffer implements NativeStream {

    public byte stride;

    public OpenGlArbArrayBuffer(OpenGLNativeToolkit nativeToolkit, boolean drawStream) {
        super(nativeToolkit, GL_ARRAY_BUFFER, drawStream);
    }

    @Override
    public int getCapacity() {
        return super.getCapacity();
    }

    @Override
    public boolean createFromSource(int size, int stride, Source source) {
        this.stride = (byte) stride;
        createFromSource(size, source);
        return true;
    }

    @Override
    public boolean create(int size, int stride) {
        this.stride = (byte) stride;
        create(size);
        return true;
    }

    @Override
    public void unlock() {
        super.unlock();
    }

    public int getStride() {
        return this.stride;
    }

    @Override
    public long getDataStorePointer(int offset, int unused) {
        return super.getDataStorePointer(offset, unused);
    }

    @Override
    public void deallocate() {
        super.destroy();
    }

    @Override
    public boolean upload(int offset, int size, long dataPointer) {
        return super.upload(offset, size, dataPointer);
    }

}
