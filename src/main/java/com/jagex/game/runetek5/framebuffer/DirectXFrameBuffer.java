package com.jagex.game.runetek5.framebuffer;

public interface DirectXFrameBuffer extends FrameBuffer {

    void destroy();

    long getSurfaceLevel();

    @Override
    void deallocate();

}
