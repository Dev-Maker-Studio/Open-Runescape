package com.jagex.game.runetek5.buffer

import jaclib.memory.heap.NativeHeap

class NativeMemoryDX(heapSize: Int) : NativeMemoryNode() {
    @JvmField
    val nativeHeap: NativeHeap = NativeHeap(heapSize)

    fun deallocate() {
        nativeHeap.deallocate()
    }
}
