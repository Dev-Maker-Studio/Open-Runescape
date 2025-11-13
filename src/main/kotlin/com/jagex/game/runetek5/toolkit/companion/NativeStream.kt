package com.jagex.game.runetek5.toolkit.companion

import com.jagex.game.runetek5.buffer.NativeBuffer
import jaclib.memory.Source

interface NativeStream : NativeBuffer, Binding {
    override fun deallocate()

    fun createFromSource(var1: Int, var2: Int, var3: Source): Boolean

    fun create(var1: Int, var2: Int): Boolean
}
