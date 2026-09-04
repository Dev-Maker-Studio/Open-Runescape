package com.jagex.game.runetek5.graphics.shader.program

import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

abstract class AbstractWaterShader(
    @JvmField val toolkit: AbstractNativeToolkit
) {

    abstract fun activate()

    open fun prepareForRendering() {
    }

    open fun beginWaterTile() {
    }

    open fun updateWaterState() {
    }

    open fun destroy() {
    }

    abstract fun loadDefaultModelMatrix()

    abstract fun deactivate()

    abstract fun resetModelMatrix()

    abstract fun activateForPass()

    abstract fun resetModelMatrixForPass()
}
