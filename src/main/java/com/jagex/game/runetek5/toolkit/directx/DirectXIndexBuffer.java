package com.jagex.game.runetek5.toolkit.directx;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.utils.Utils;

import jagdx.IDirect3DDevice;
import jagdx.IDirect3DIndexBuffer;
import jagdx.IUnknown;

public class DirectXIndexBuffer implements DirectXBuffer, AutoCloseable {

    public long handle;
    public int sizeOccupied;
    public int sizeRequired;
    public final DirectXNativeToolkit dxToolkit;
    public final GLActionFormat actionFormat;
    public final boolean aBool4236;

    public DirectXIndexBuffer(DirectXNativeToolkit dxToolkit, GLActionFormat actionFormat, boolean bool3) {
        this.dxToolkit = dxToolkit;
        this.actionFormat = actionFormat;
        this.aBool4236 = bool3;
        this.dxToolkit.addWeakReference(this);
    }

    @Override
    public int getCapacity() {
        return this.sizeRequired;
    }

    @Override
    public long getDataStorePointer(int i_1, int i_2) {
        return IDirect3DIndexBuffer.Lock(this.handle, i_1, i_2, this.aBool4236 ? 8192 : 0);
    }

    @Override
    public void unlock() {
        IDirect3DIndexBuffer.Unlock(this.handle);
    }

    @Override
    public void deallocate() {
        if (this.handle != 0L) {
            IUnknown.Release(this.handle);
            this.handle = 0L;
        }

        this.sizeOccupied = 0;
        this.sizeRequired = 0;
        this.dxToolkit.clearReference(this);
    }

    @Override
    public void close() {
        cleanup();
    }

    @Override
    public void create(int i_1) {
        this.sizeRequired = this.actionFormat.bytesPerEntry * i_1;
        if (this.sizeRequired > this.sizeOccupied) {
            if (this.handle != 0L) {
                IUnknown.Release(this.handle);
            }

            int mask = 8;
            byte type;
            if (this.aBool4236) {
                type = 0;
                mask |= 0x200;
            } else {
                type = 1;
            }

            this.handle = IDirect3DDevice.CreateIndexBuffer(this.dxToolkit.renderTarget, this.sizeRequired, mask, this.actionFormat == GLActionFormat.TEXTURE_FORMAT ? 101 : 102, type);
            this.sizeOccupied = this.sizeRequired;
        }

    }

    @Override
    public boolean upload(int i_1, int i_2, long long_3) {
        return Utils.equalOrAboveZero(IDirect3DIndexBuffer.Upload(this.handle, i_1, i_2, this.aBool4236 ? 8192 : 0, long_3));
    }

    public void cleanup() {
        if (this.handle != 0L) {
            this.dxToolkit.requestHandleCleanup(this.handle);
            this.handle = 0L;
        }

        this.sizeOccupied = 0;
        this.sizeRequired = 0;
    }
}
