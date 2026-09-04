package com.jagex.game.runetek5.buffer

import jaclib.memory.heap.NativeHeap

class NativeMemoryGL(size: Int) : NativeMemoryNode() {
    @JvmField
    val nativeHeap: NativeHeap = NativeHeap(size)

    fun deallocate() {
        nativeHeap.deallocate()
    }
}
