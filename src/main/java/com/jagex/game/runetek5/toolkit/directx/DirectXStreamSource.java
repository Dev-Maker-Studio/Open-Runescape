package com.jagex.game.runetek5.toolkit.directx;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.utils.Utils;

import jaclib.memory.Source;
import jagdx.IDirect3DDevice;
import jagdx.IDirect3DVertexBuffer;
import jagdx.IUnknown;

public class DirectXStreamSource implements NativeStream {

    public int capacity;
    public byte aByte3547;
    public long handle;
    public int anInt3548;
    public final DirectXNativeToolkit directxToolkit;
    public final boolean aBool3552;

    public DirectXStreamSource(DirectXNativeToolkit directxToolkit, boolean bool2) {
        this.directxToolkit = directxToolkit;
        this.aBool3552 = bool2;
        this.directxToolkit.addWeakReference(this);
    }

    @Override
    public boolean createFromSource(int i_1, int i_2, Source source3) {
        return create(i_1, i_2) && Utils.equalOrAboveZero(IDirect3DVertexBuffer.Upload(this.handle, 0, this.capacity, this.aBool3552 ? 8192 : 0, source3.getBufferAddress()));
    }

    public int method5317() {
        return this.aByte3547;
    }

    @Override
    public boolean create(int i_1, int i_2) {
        this.capacity = i_1;
        this.aByte3547 = (byte) i_2;
        if (this.capacity > this.anInt3548) {
            int i_3 = 8;
            byte b_4;
            if (this.aBool3552) {
                b_4 = 0;
                i_3 |= 0x200;
            } else {
                b_4 = 1;
            }

            if (this.handle != 0L) {
                IUnknown.Release(this.handle);
            }

            this.handle = IDirect3DDevice.CreateVertexBuffer(this.directxToolkit.renderTarget, this.capacity, i_3, 0, b_4);
            this.anInt3548 = this.capacity;
        }

        return this.handle != 0L;
    }

    @Override
    public long getDataStorePointer(int i_1, int i_2) {
        return IDirect3DVertexBuffer.Lock(this.handle, i_1, i_2, this.aBool3552 ? 8192 : 0);
    }

    @Override
    public void unlock() {
        IDirect3DVertexBuffer.Unlock(this.handle);
    }

    @Override
    public boolean upload(int i_1, int i_2, long long_3) {
        return Utils.equalOrAboveZero(IDirect3DVertexBuffer.Upload(this.handle, i_1, i_2, this.aBool3552 ? 8192 : 0, long_3));
    }

    @Override
    public void deallocate() {
        if (this.handle != 0L) {
            IUnknown.Release(this.handle);
            this.handle = 0L;
        }

        this.anInt3548 = 0;
        this.capacity = 0;
        this.directxToolkit.clearReference(this);
    }

    public void cleanup() {
        if (this.handle != 0L) {
            this.directxToolkit.requestHandleCleanup(this.handle);
            this.handle = 0L;
            this.anInt3548 = 0;
            this.capacity = 0;
        }

    }

    @Override
    protected void finalize() {
        cleanup();
    }

    @Override
    public int getCapacity() {
        return this.capacity;
    }
}
