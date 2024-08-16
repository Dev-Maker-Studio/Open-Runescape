package com.jagex.game.runetek5.graphics.shadow;

import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject;

public class ShadowSw3d extends Shadow implements INativeObject {

    public long nativeid;

    ShadowSw3d() {
    }

    @Override
    public native void destroy(boolean var1);

    @Override
    protected void finalize() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }
    }
}
