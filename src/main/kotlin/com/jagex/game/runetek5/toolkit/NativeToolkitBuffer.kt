package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.buffer.NativeBuffer
import com.jagex.game.runetek5.toolkit.companion.Binding

/**
 * Native toolkit buffer interface.
 * Note: Previously named DirectXBuffer (incorrectly), used by both OpenGL and DirectX.
 */
interface NativeToolkitBuffer : NativeBuffer, Binding {
    override fun deallocate()

    fun create(var1: Int)
}
