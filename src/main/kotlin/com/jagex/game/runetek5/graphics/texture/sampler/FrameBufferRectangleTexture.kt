package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.graphics.texture.TextureFramebufferGenerator
import com.jagex.game.runetek5.toolkit.opengl.GL_FRAMEBUFFER
import jaggl.OpenGL

class FrameBufferRectangleTexture(
    @JvmField val mipmapTextureSampler: MipmapRectangleTextureSampler,
    @JvmField val anInt3667: Int
) : TextureFramebufferGenerator {

    override fun getUseWidth(): Int {
        return this.mipmapTextureSampler.getTextureWidth()
    }

    override fun getUseHeight(): Int {
        return this.mipmapTextureSampler.getTextureHeight()
    }

    override fun deallocate() {
    }

    override fun generateTextureFramebuffer(i_1: Int) {
        OpenGL.glFramebufferTexture2DEXT(GL_FRAMEBUFFER, i_1, this.mipmapTextureSampler.textureTarget, this.mipmapTextureSampler.texture, this.anInt3667)
    }
}
