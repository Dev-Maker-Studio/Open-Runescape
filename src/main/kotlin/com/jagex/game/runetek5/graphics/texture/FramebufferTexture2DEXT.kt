package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer
import jaggl.OpenGL

class FramebufferTexture2DEXT(
    @JvmField val aClass137Sub11966: OpenGl2DTexture
) : NativeRenderBuffer {

    @JvmField val anInt1967: Int = 0

    override fun getUseWidth(): Int {
        return this.aClass137Sub11966.width
    }

    override fun getUseHeight(): Int {
        return this.aClass137Sub11966.length
    }

    override fun getFrameRenderBuffer(i_1: Int) {
        OpenGL.glFramebufferTexture2DEXT(36160, i_1, this.aClass137Sub11966.textureTarget, this.aClass137Sub11966.activeHandle, this.anInt1967)
    }

    override fun deallocate() {
    }
}
