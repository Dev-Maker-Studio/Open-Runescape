package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer
import jaggl.OpenGL

class OpenGlAttachableCubeMapTexture(
    @JvmField val cubeMapTexture: OpenGlArbCubeMapTexture,
    @JvmField val cubeFaceTarget: Int
) : NativeRenderBuffer {

    @JvmField val mipmapLevel: Int = 0

    override fun getUseWidth(): Int {
        return this.cubeMapTexture.largestMaterialSize
    }

    override fun getUseHeight(): Int {
        return this.cubeMapTexture.largestMaterialSize
    }

    override fun getFrameRenderBuffer(i_1: Int) {
        OpenGL.glFramebufferTexture2DEXT(36160, i_1, this.cubeFaceTarget, this.cubeMapTexture.activeHandle, this.mipmapLevel)
    }

    override fun deallocate() {
    }
}
