package com.jagex.game.runetek5.imageproducer;

import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public abstract class AbstractImageProducerDX1 extends DirectDisplay {

    public final AbstractNativeToolkit aGraphicalRendererSub210171;

    AbstractImageProducerDX1(AbstractNativeToolkit class505Sub21) {
        this.aGraphicalRendererSub210171 = class505Sub21;
    }

    void method15562() {
        if (this == this.aGraphicalRendererSub210171.getDisplay()) {
            this.aGraphicalRendererSub210171.autoResize();
        }
    }

    @Override
    public boolean reload() {
        this.aGraphicalRendererSub210171.autoResize2();
        return true;
    }

}
