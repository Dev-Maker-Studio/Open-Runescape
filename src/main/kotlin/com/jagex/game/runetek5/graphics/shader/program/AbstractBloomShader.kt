package com.jagex.game.runetek5.graphics.shader.program

import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

abstract class AbstractBloomShader protected constructor(
    @JvmField protected val nativeToolkit: AbstractNativeToolkit
) {

    @JvmField var hasLinkedVector: Boolean = false

    abstract fun method1097(): Boolean

    abstract fun isLoaded(): Boolean

    open fun method1099(): Boolean {
        return method1097()
    }

    open fun getIterations(): Int {
        return 1
    }

    abstract fun method1102()

    abstract fun initialize(type: Int, display: DirectDisplay, compositeSampler: NativeTextureSampler, var5: NativeTextureSampler)

    abstract fun reset()

    open fun method1105() {
    }

    open fun getFormatType(): Int {
        return 0
    }

    fun method1107(): Boolean {
        return this.hasLinkedVector
    }

    abstract fun destroy()
}
