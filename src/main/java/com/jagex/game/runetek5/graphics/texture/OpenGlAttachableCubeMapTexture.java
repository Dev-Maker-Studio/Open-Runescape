package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer;

import jaggl.OpenGL;

public class OpenGlAttachableCubeMapTexture implements NativeRenderBuffer {

    public final int anInt1658;
    public final OpenGlArbCubeMapTexture aClass137Sub21656;
    public final int anInt1657;

    OpenGlAttachableCubeMapTexture(OpenGlArbCubeMapTexture class137Sub21, int i_2) {
        this.anInt1658 = 0;
        this.aClass137Sub21656 = class137Sub21;
        this.anInt1657 = i_2;
    }


    @Override
    public int getUseWidth() {
        return this.aClass137Sub21656.largestMaterialSize;
    }

    @Override
    public int getUseHeight() {
        return this.aClass137Sub21656.largestMaterialSize;
    }

    @Override
    public void getFrameRenderBuffer(int i_1) {
        OpenGL.glFramebufferTexture2DEXT(36160, i_1, this.anInt1657, this.aClass137Sub21656.activeHandle, this.anInt1658);
    }
    
    @Override
    public void deallocate() {
    }
}
