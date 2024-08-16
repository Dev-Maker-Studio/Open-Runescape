package com.jagex.game.runetek5.buffer;

import jaclib.memory.heap.NativeHeap;

public class NativeMemoryDX extends NativeMemoryNode {

	public final NativeHeap nativeHeap;

    public NativeMemoryDX(int heapSize) {
        this.nativeHeap = new NativeHeap(heapSize);
    }

    public void deallocate() {
        this.nativeHeap.deallocate();
    }
}
