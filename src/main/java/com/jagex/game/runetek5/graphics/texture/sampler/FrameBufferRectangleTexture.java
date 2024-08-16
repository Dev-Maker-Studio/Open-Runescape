package com.jagex.game.runetek5.graphics.texture.sampler;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.graphics.texture.TextureFramebufferGenerator;

import jaggl.OpenGL;

public class FrameBufferRectangleTexture implements TextureFramebufferGenerator {

    public final int anInt3667;
    public final MipmapRectangleTextureSampler mipmapTextureSampler;

    FrameBufferRectangleTexture(MipmapRectangleTextureSampler class319Sub31, int i_2) {
        this.anInt3667 = i_2;
        this.mipmapTextureSampler = class319Sub31;
    }

    @Override
    public int getUseWidth() {
        return this.mipmapTextureSampler.getTextureWidth();
    }

    @Override
    public int getUseHeight() {
        return this.mipmapTextureSampler.getTextureHeight();
    }

    @Override
    public void deallocate() {
    }

    @Override
    public void generateTextureFramebuffer(int i_1) {
        OpenGL.glFramebufferTexture2DEXT(GL_FRAMEBUFFER, i_1, this.mipmapTextureSampler.textureTarget, this.mipmapTextureSampler.texture, this.anInt3667);
    }
}
