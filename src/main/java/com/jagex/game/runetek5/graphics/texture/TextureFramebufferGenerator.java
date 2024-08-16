package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;

public interface TextureFramebufferGenerator extends FrameBuffer, RenderBuffer {

    void generateTextureFramebuffer(int var1);

}
