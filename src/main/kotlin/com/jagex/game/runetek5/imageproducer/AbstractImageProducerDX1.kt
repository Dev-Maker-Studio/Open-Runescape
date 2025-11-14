package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

abstract class AbstractImageProducerDX1(
    @JvmField val aGraphicalRendererSub210171: AbstractNativeToolkit
) : DirectDisplay() {

    fun method15562() {
        if (this === aGraphicalRendererSub210171.getDisplay()) {
            aGraphicalRendererSub210171.autoResize()
        }
    }

    override fun reload(): Boolean {
        aGraphicalRendererSub210171.autoResize2()
        return true
    }
}
