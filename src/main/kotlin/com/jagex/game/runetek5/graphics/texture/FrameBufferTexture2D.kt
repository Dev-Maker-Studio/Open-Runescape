package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler2D
import com.jagex.game.runetek5.toolkit.opengl.GL_FRAMEBUFFER
import jaggl.OpenGL

class FrameBufferTexture2D(
    @JvmField val mipmapTextureSampler: MipmapTextureSampler2D,
    @JvmField val mipmapLevel: Int
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
        OpenGL.glFramebufferTexture2DEXT(GL_FRAMEBUFFER, i_1, this.mipmapTextureSampler.textureTarget, this.mipmapTextureSampler.texture, this.mipmapLevel)
    }
}
