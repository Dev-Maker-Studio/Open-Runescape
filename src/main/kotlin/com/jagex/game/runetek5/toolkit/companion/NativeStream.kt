package com.jagex.game.runetek5.toolkit.companion

import com.jagex.game.runetek5.buffer.NativeBuffer
import jaclib.memory.Source

interface NativeStream : NativeBuffer, Binding {
    override fun deallocate()

    fun createFromSource(size: Int, stride: Int, source: Source): Boolean

    fun create(size: Int, stride: Int): Boolean
}
