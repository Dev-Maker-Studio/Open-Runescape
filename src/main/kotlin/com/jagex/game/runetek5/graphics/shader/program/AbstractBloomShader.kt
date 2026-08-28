package com.jagex.game.runetek5.graphics.shader.program

import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

abstract class AbstractBloomShader protected constructor(
    @JvmField protected val nativeToolkit: AbstractNativeToolkit
) {

    @JvmField var hasLinkedVector: Boolean = false

    abstract fun loadShader(): Boolean

    abstract fun isLoaded(): Boolean

    open fun reloadShader(): Boolean {
        return loadShader()
    }

    open fun getIterations(): Int {
        return 1
    }

    abstract fun onRendererInitialized()

    abstract fun initialize(type: Int, display: DirectDisplay, compositeSampler: NativeTextureSampler, sceneSampler: NativeTextureSampler)

    abstract fun reset()

    open fun onShutdown() {
    }

    open fun getFormatType(): Int {
        return 0
    }

    fun isLinked(): Boolean {
        return this.hasLinkedVector
    }

    abstract fun destroy()
}
