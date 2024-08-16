package com.jagex.game.runetek5.toolkit.directx;

import com.jagex.game.runetek5.buffer.NativeBuffer;
import com.jagex.game.runetek5.toolkit.companion.Binding;

// FIXME: quite certain this name is incorrect
public interface DirectXBuffer extends NativeBuffer, Binding {

    @Override
    void deallocate();

    void create(int var1);
    
}
