package com.jagex.game.runetek5.buffer;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.directx.DirectXFrameBufferObject;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;

import jagdx.IDirect3DDevice;
import jagdx.IUnknown;

public class DirectXNativeRenderBuffer implements DirectXFrameBufferObject, AutoCloseable {

    public long aLong3610;
    public final DirectXNativeToolkit aGraphicalRendererSub2Sub23608;
    public final int anInt3606;
    public final int anInt3609;
    public final GLActionFormat aClass763607;

    public DirectXNativeRenderBuffer(DirectXNativeToolkit class505Sub2Sub21, GLActionFormat class762, int i_3, int i_4) {
        this.aGraphicalRendererSub2Sub23608 = class505Sub2Sub21;
        this.anInt3606 = i_3;
        this.anInt3609 = i_4;
        this.aClass763607 = class762;
        this.aLong3610 = IDirect3DDevice.CreateDepthStencilSurface(this.aGraphicalRendererSub2Sub23608.renderTarget, i_3, i_4, method15653(this.aClass763607), 0, 0, false);
        this.aGraphicalRendererSub2Sub23608.addWeakReference(this);
    }

    public static int method15653(GLActionFormat class760) {
        if (GLActionFormat.TEXTURE_FORMAT == class760) {
            return 80;
        } else if (GLActionFormat.DEPTH_COMPONENT_24 == class760) {
            return 77;
        } else {
            throw new IllegalArgumentException("");
        }
    }

    @Override
    public int getUseWidth() {
        return this.anInt3606;
    }

    @Override
    public int getUseHeight() {
        return this.anInt3609;
    }

    @Override
    public long getSurfaceLevel() {
        return this.aLong3610;
    }

    @Override
    public void deallocate() {
        if (this.aLong3610 != 0L) {
            IUnknown.Release(this.aLong3610);
            this.aLong3610 = 0L;
        }

        this.aGraphicalRendererSub2Sub23608.clearReference(this);
    }

    @Override
    public void close() {
        destroy();
    }


    @Override
    public void destroy() {
        if (this.aLong3610 != 0L) {
            this.aGraphicalRendererSub2Sub23608.requestHandleCleanup(this.aLong3610);
            this.aLong3610 = 0L;
        }

    }
}