package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

/**
 * 
 * @author Steven - 13/09/2023
 *
 */
public class OpenGlArbElementArrayBuffer extends OpenGlArbBuffer implements DirectXBuffer {

    public final GLActionFormat actionFormat;

    public OpenGlArbElementArrayBuffer(OpenGLNativeToolkit toolkit, GLActionFormat actionFormat, boolean drawStream) {
        super(toolkit, GL_ELEMENT_ARRAY_BUFFER, drawStream);
        this.actionFormat = actionFormat;
    }

    @Override
    public int getCapacity() {
        return super.getCapacity();
    }

    @Override
    public void unlock() {
        super.unlock();
    }

    public GLActionFormat getActionFormat() {
        return this.actionFormat;
    }

    @Override
    public void create(int size) {
        super.create(this.actionFormat.bytesPerEntry * size);
    }

    @Override
    public void deallocate() {
        super.destroy();
    }

    @Override
    public long getDataStorePointer(int offset, int i_2) {
        return super.getDataStorePointer(offset, i_2);
    }

    @Override
    public boolean upload(int offset, int size, long dataPointer) {
        return super.upload(offset, size, dataPointer);
    }
}
