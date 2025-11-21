package com.jagex.game.runetek5.framebuffer

import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject

/**
 * Software-based render buffer with native rendering methods.
 */
class SoftwareRenderBuffer(
    width: Int,
    height: Int
) : RenderBuffer, INativeObject, AutoCloseable {

    @JvmField var nativeid: Long = 0
    @JvmField val anInt2681: Int = width
    @JvmField val anInt2682: Int = height

    init {
        g(width, height)
    }

    private external fun e(var1: Long, var3: Boolean)

    private external fun g(var1: Int, var2: Int)

    override fun getUseWidth(): Int {
        return anInt2681
    }

    override fun close() {
        if (nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    private external fun za(var1: Long, var3: Boolean)

    private external fun w(var1: Long, var3: Boolean)

    override fun destroy(shouldDestroy: Boolean) {
        za(nativeid, shouldDestroy)
    }

    private external fun j(var1: Int, var2: Int)

    private external fun o(var1: Int, var2: Int)

    private external fun l(var1: Long, var3: Boolean)

    override fun getUseHeight(): Int {
        return anInt2682
    }

    override fun deallocate() {
        destroy(false)
    }

    private external fun s(var1: Long, var3: Boolean)
}
