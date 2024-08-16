package com.jagex.game.runetek5.toolkit.sw3d;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;

public class wa extends ComponentSurface implements INativeObject {

    public long nativeid;

    public wa(SoftwareToolkit ja1, ba ba2, int i_3, int i_4, int[] ints_5, int[] ints_6) {
        l(ja1, ba2, i_3, i_4, ints_5, ints_6);
    }

    native void g(SoftwareToolkit var1, ba var2, int var3, int var4, int[] var5, int[] var6);

    native void l(SoftwareToolkit var1, ba var2, int var3, int var4, int[] var5, int[] var6);

    @Override
    protected void finalize() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }
    }

    @Override
    public native void destroy(boolean var1);


    native void n(SoftwareToolkit var1, ba var2, int var3, int var4, int[] var5, int[] var6);

}
