package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer
import jaggl.OpenGL

class OpenGlAttachableCubeMapTexture(
    @JvmField val aClass137Sub21656: OpenGlArbCubeMapTexture,
    @JvmField val anInt1657: Int
) : NativeRenderBuffer {

    @JvmField val anInt1658: Int = 0

    override fun getUseWidth(): Int {
        return this.aClass137Sub21656.largestMaterialSize
    }

    override fun getUseHeight(): Int {
        return this.aClass137Sub21656.largestMaterialSize
    }

    override fun getFrameRenderBuffer(i_1: Int) {
        OpenGL.glFramebufferTexture2DEXT(36160, i_1, this.anInt1657, this.aClass137Sub21656.activeHandle, this.anInt1658)
    }

    override fun deallocate() {
    }
}
