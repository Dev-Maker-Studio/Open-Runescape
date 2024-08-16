package com.jagex.game.runetek5.toolkit.companion;

import com.jagex.game.runetek5.buffer.NativeBuffer;

import jaclib.memory.Source;

public interface NativeStream extends NativeBuffer, Binding {

    @Override
    void deallocate();

    boolean createFromSource(int var1, int var2, Source var3);

    boolean create(int var1, int var2);

}
