package com.jagex.game.runetek5.imageproducer;

import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public abstract class AbstractImageProducerDX extends DelegatingDisplay {

    public final AbstractNativeToolkit toolkitDx;

    AbstractImageProducerDX(AbstractNativeToolkit toolkit) {
        this.toolkitDx = toolkit;
    }

    @Override
    public void resize(int i_1, int i_2) {
        if (this == this.toolkitDx.getDisplay()) {
            this.toolkitDx.autoResize();
        }
    }

    @Override
    public boolean reload() {
        this.toolkitDx.autoResize2();
        return true;
    }
}
