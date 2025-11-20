package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.Binding
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import jagdx.IUnknown

/**
 * Base class for Direct3D texture samplers.
 * Manages texture resources in the DirectX rendering pipeline.
 */
abstract class Direct3DTextureSampler protected constructor(
    @JvmField val dxToolkit: DirectXNativeToolkit,
    @JvmField val glformat: GLFormat,
    @JvmField val glactionFormat: GLActionFormat,
    @JvmField val aBool4166: Boolean
) : Binding, AutoCloseable {

    @JvmField var handle: Long = 0

    init {
        dxToolkit.addWeakReference(this)
    }

    open fun getHandle(): Long = handle

    open fun setSamplerMode(sampleMode: SampleMode) {
        // Default implementation does nothing
    }

    override fun deallocate() {
        if (handle != 0L) {
            IUnknown.Release(handle)
            handle = 0L
        }
        dxToolkit.clearReference(this)
    }

    /**
     * Requests cleanup of the texture handle via the toolkit
     */
    fun requestCleanup() {
        if (handle != 0L) {
            dxToolkit.requestHandleCleanup(handle)
            handle = 0L
        }
    }

    override fun close() {
        requestCleanup()
    }
}
