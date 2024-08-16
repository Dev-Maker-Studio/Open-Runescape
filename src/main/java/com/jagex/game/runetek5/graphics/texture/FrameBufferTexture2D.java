package com.jagex.game.runetek5.graphics.texture;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler2D;

import jaggl.OpenGL;

public class FrameBufferTexture2D implements TextureFramebufferGenerator {

    public final int anInt3756;
    public final MipmapTextureSampler2D mipmapTextureSampler;

    public FrameBufferTexture2D(MipmapTextureSampler2D class319Sub21, int i_2) {
        this.anInt3756 = i_2;
        this.mipmapTextureSampler = class319Sub21;
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
        OpenGL.glFramebufferTexture2DEXT(GL_FRAMEBUFFER, i_1, this.mipmapTextureSampler.textureTarget, this.mipmapTextureSampler.texture, this.anInt3756);
    }
}