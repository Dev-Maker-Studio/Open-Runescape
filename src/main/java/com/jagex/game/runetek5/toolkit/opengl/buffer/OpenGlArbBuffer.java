package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.buffer.NativeBuffer;
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaclib.memory.Source;
import jaclib.memory.heap.NativeHeapBuffer;
import jaggl.OpenGL;

public abstract class OpenGlArbBuffer implements NativeBuffer {

    public static final int[] HANDLE_BUFFER = new int[1];
    public int activeHandle = -1;
    public final OpenGLNativeToolkit nativeToolkit;
    public final int bufferTarget;
    public final boolean drawStream;
    public NativeHeapBuffer nativeheapBuffer;
    public int capacity;
    public int initialCapacity;

    protected OpenGlArbBuffer(OpenGLNativeToolkit nativeToolkit, int bufferTarget, boolean drawStream) {
        this.nativeToolkit = nativeToolkit;
        this.bufferTarget = bufferTarget;
        this.drawStream = drawStream;
    }

    @Override
    public int getCapacity() {
        return this.capacity;
    }

    public void bind() {
        if (this.nativeToolkit.GLARBVertexBufferObject) {
            OpenGL.glBindBufferARB(this.bufferTarget, this.activeHandle);
        }
    }

    public void initialize() {
        if (this.activeHandle < 0) {
            if (this.nativeToolkit.GLARBVertexBufferObject) {
                OpenGL.glGenBuffersARB(1, HANDLE_BUFFER, 0);
                this.activeHandle = HANDLE_BUFFER[0];
                OpenGL.glBindBufferARB(this.bufferTarget, this.activeHandle);
            } else {
                this.activeHandle = 0;
            }
        }
    }

    @Override
    public long getDataStorePointer(int offset, int unused) {
        OpenGL.glBindBufferARB(this.bufferTarget, this.activeHandle);
        return OpenGL.glMapBufferARB(this.bufferTarget, GL_WRITE_ONLY) + offset;
    }

    public long getBufferAddress() {
        return this.activeHandle == 0 ? this.nativeheapBuffer.getBufferAddress() : 0L;
    }

    @Override
    public boolean upload(int offset, int size, long dataPointer) {
        initialize();
        if (this.activeHandle > 0) {
            OpenGL.glBindBufferARB(this.bufferTarget, this.activeHandle);
            OpenGL.glBufferSubDataARBa(this.bufferTarget, offset, size, dataPointer);
        } else {
            this.nativeheapBuffer.nativeHeap.copy(this.nativeheapBuffer.getBufferAddress() + offset, dataPointer, size);
        }

        return true;
    }

    protected void destroy() {
        if (this.activeHandle > 0) {
            this.nativeToolkit.requestTextureBufferDeletion(this.activeHandle, this.capacity);
            this.activeHandle = -1;
        }
    }

    @SuppressWarnings("removal")
	@Override
    protected void finalize() throws Throwable {
    	destroy();
        super.finalize();
    }

    @Override
    public void unlock() {
        OpenGL.glUnmapBufferARB(this.bufferTarget);
    }

    protected boolean createFromSource(int size, Source source) {
        if (size > this.initialCapacity) {
            initialize();
            if (this.activeHandle > 0) {
                OpenGL.glBindBufferARB(this.bufferTarget, this.activeHandle);
                OpenGL.glBufferDataARBa(this.bufferTarget, size, source.getBufferAddress(), this.drawStream ? GL_STREAM_DRAW : GL_STATIC_DRAW);
                this.nativeToolkit.textureBuffersSizeOccupied += size - this.capacity;
            } else {
                this.nativeheapBuffer = this.nativeToolkit.generateNativeHeapBuffer(size, false);
                this.nativeheapBuffer.copy(source);
            }

            this.initialCapacity = size;
        } else if (this.activeHandle > 0) {
            OpenGL.glBindBufferARB(this.bufferTarget, this.activeHandle);
            OpenGL.glBufferSubDataARBa(this.bufferTarget, 0, this.capacity, source.getBufferAddress());
            this.nativeToolkit.textureBuffersSizeOccupied += size - this.capacity;
        } else {
            this.nativeheapBuffer = this.nativeToolkit.generateNativeHeapBuffer(size, false);
            this.nativeheapBuffer.copy(source);
        }

        this.capacity = size;
        return true;
    }

    protected void create(int size) {
        if (size > this.initialCapacity) {
            initialize();
            if (this.activeHandle > 0) {
                OpenGL.glBindBufferARB(this.bufferTarget, this.activeHandle);
                OpenGL.glBufferDataARBub(this.bufferTarget, size, null, 0, this.drawStream ? GL_STREAM_DRAW : GL_STATIC_DRAW);
                this.nativeToolkit.textureBuffersSizeOccupied += size - this.initialCapacity;
            } else {
                this.nativeheapBuffer = this.nativeToolkit.generateNativeHeapBuffer(size, false);
            }

            this.initialCapacity = size;
        }

        this.capacity = size;
    }
}
