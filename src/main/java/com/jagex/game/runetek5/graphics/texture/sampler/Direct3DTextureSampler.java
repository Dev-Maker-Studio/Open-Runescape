package com.jagex.game.runetek5.graphics.texture.sampler;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.Binding;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import jagdx.IUnknown;

public abstract class Direct3DTextureSampler implements Binding, AutoCloseable {

	public long handle;
	public final DirectXNativeToolkit dxToolkit;
	public final GLFormat glformat;
	public final GLActionFormat glactionFormat;
	public final boolean aBool4166;

    protected Direct3DTextureSampler(DirectXNativeToolkit class505Sub2Sub21, GLFormat class1502, GLActionFormat class763, boolean bool4) {
        this.dxToolkit = class505Sub2Sub21;
        this.glformat = class1502;
        this.glactionFormat = class763;
        this.aBool4166 = bool4;
        this.dxToolkit.addWeakReference(this);
    }

    public long getHandle() {
        return this.handle;
    }

    public void setSamplerMode(SampleMode class351) {
    }

    @Override
    public void deallocate() {
        if (this.handle != 0L) {
            IUnknown.Release(this.handle);
            this.handle = 0L;
        }

        this.dxToolkit.clearReference(this);
    }

    public void method6246() {
        if (this.handle != 0L) {
            this.dxToolkit.requestHandleCleanup(this.handle);
            this.handle = 0L;
        }

    }

    @Override
    public void close() {
        method6246();
    }

}
