package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

abstract class AbstractImageProducerDX(
    @JvmField val toolkitDx: AbstractNativeToolkit
) : DelegatingDisplay() {

    override fun resize(i_1: Int, i_2: Int) {
        if (this === toolkitDx.getDisplay()) {
            toolkitDx.autoResize()
        }
    }

    override fun reload(): Boolean {
        toolkitDx.autoResize2()
        return true
    }
}
