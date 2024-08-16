package com.jagex.game.runetek5.toolkit.directx;

import com.jagex.game.runetek5.framebuffer.RenderBuffer;

public interface DirectXFrameBufferObject extends RenderBuffer {

    void destroy();

    long getSurfaceLevel();

    @Override
    void deallocate();

}