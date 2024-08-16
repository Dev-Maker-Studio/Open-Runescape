package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer;

import jaggl.OpenGL;

public class FramebufferTexture2DEXT implements NativeRenderBuffer {

    public final int anInt1967;
    public final OpenGl2DTexture aClass137Sub11966;

    public FramebufferTexture2DEXT(OpenGl2DTexture class137Sub11) {
        this.anInt1967 = 0;
        this.aClass137Sub11966 = class137Sub11;
    }

    @Override
    public int getUseWidth() {
        return this.aClass137Sub11966.width;
    }

    @Override
    public int getUseHeight() {
        return this.aClass137Sub11966.length;
    }

    @Override
    public void getFrameRenderBuffer(int i_1) {
        OpenGL.glFramebufferTexture2DEXT(36160, i_1, this.aClass137Sub11966.textureTarget, this.aClass137Sub11966.activeHandle, this.anInt1967);
    }

    @Override
    public void deallocate() {
    }
}
