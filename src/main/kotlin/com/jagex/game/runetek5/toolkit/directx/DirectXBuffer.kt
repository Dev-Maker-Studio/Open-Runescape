package com.jagex.game.runetek5.toolkit.directx

import com.jagex.game.runetek5.buffer.NativeBuffer
import com.jagex.game.runetek5.toolkit.companion.Binding

// FIXME: quite certain this name is incorrect
interface DirectXBuffer : NativeBuffer, Binding {
    override fun deallocate()

    fun create(var1: Int)
}
