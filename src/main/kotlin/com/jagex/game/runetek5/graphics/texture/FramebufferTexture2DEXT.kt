package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer
import jaggl.OpenGL

class FramebufferTexture2DEXT(
    @JvmField val texture2d: OpenGl2DTexture
) : NativeRenderBuffer {

    @JvmField val mipmapLevel: Int = 0

    override fun getUseWidth(): Int {
        return this.texture2d.width
    }

    override fun getUseHeight(): Int {
        return this.texture2d.length
    }

    override fun getFrameRenderBuffer(i_1: Int) {
        OpenGL.glFramebufferTexture2DEXT(36160, i_1, this.texture2d.textureTarget, this.texture2d.activeHandle, this.mipmapLevel)
    }

    override fun deallocate() {
    }
}
