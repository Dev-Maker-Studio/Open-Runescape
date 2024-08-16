package com.jagex.game.runetek5.framebuffer;

public interface NativeRenderBuffer extends FrameBuffer, RenderBuffer {

    void getFrameRenderBuffer(int colorAttachmentSlot);
}
