package com.jagex.game.runetek5.toolkit.sw3d;

import com.jagex.game.addon.NativeName;
import com.jagex.game.runetek5.buffer.NativeMemoryNode;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;

/**
 * Native name: ba
 * @author Brainpower
 *
 */
public class ba extends NativeMemoryNode implements INativeObject {

    public long nativeid;

    public ba(SoftwareToolkit ja1, int i_2) {
        sa(ja1, i_2);
    }

    @NativeName("sa")
    native void sa(SoftwareToolkit var1, int var2);

    @Override
    //TODO:
    public native void destroy(boolean var1);

    @NativeName("ha")
    public native void ha();

    @NativeName("u")
    public native void u();

    @NativeName("v")
    native void v();

    @NativeName("j")
    native void j();

    @NativeName("n")
    native void n(SoftwareToolkit var1, int var2);

    @NativeName("g")
    native void g(SoftwareToolkit var1, int var2);

    @Override
    protected void finalize() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }
    }
}
