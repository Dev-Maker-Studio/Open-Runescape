package com.jagex.game.runetek5.framebuffer;

import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject;

public class SoftwareRenderBuffer implements RenderBuffer, INativeObject {

    public long nativeid;
    public final int anInt2681;
    public final int anInt2682;

    public SoftwareRenderBuffer(int i_1, int i_2) {
        this.anInt2681 = i_1;
        this.anInt2682 = i_2;
        g(i_1, i_2);
    }

    native void e(long var1, boolean var3);

    native void g(int var1, int var2);

    @Override
    public int getUseWidth() {
        return this.anInt2681;
    }

    @Override
    protected void finalize() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }
    }


    native void za(long var1, boolean var3);


    native void w(long var1, boolean var3);


    @Override
    public void destroy(boolean bool1) {
        za(this.nativeid, bool1);
    }


    native void j(int var1, int var2);

    native void o(int var1, int var2);

    native void l(long var1, boolean var3);

    @Override
    public int getUseHeight() {
        return this.anInt2682;
    }

    @Override
    public void deallocate() {
        destroy(false);
    }

    native void s(long var1, boolean var3);

}
