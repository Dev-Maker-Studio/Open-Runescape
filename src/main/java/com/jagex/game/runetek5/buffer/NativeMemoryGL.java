package com.jagex.game.runetek5.buffer;

import jaclib.memory.heap.NativeHeap;

public class NativeMemoryGL extends NativeMemoryNode {

    public final NativeHeap nativeHeap;

    public NativeMemoryGL(int size) {
        this.nativeHeap = new NativeHeap(size);
    }

    public void deallocate() {
        this.nativeHeap.deallocate();
    }
}
