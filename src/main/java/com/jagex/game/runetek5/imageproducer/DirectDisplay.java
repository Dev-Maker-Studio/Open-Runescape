package com.jagex.game.runetek5.imageproducer;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;

public abstract class DirectDisplay extends AbstractDisplay {

    public abstract void setFramebuffer(int slot, FrameBuffer handle);

    public abstract boolean isFramebufferComplete();

    public abstract void setRenderBuffer(RenderBuffer renderBuffer);

}
