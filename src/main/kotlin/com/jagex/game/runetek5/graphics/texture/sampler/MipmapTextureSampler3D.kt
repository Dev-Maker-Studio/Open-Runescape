package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import jaggl.OpenGL

/**
 *
 * @author Steven - 23/08/2023
 *
 */
class MipmapTextureSampler3D(
    toolkit: OpenGLNativeToolkit,
    format: GLFormat,
    data: ByteArray
) : MipmapTextureSampler(toolkit, GL_TEXTURE_3D, format, GLActionFormat.PIXEL_FORMAT, 128 * 16 * 128, true), Texture3DSampler {

    @JvmField val width: Int
    @JvmField val height: Int
    @JvmField val depth: Int

    init {
        this.width = 128
        this.height = 128
        this.depth = 16
        super.activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glTexImage3Dub(
            super.textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat),
            this.width, this.height, this.depth, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), GL_UNSIGNED_BYTE, data, 0
        )
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
        enableMipmapping()
    }

    override fun setSamplerMode(sampleMode: SampleMode) {
        super.setSamplerMode(sampleMode)
    }

    override fun bindTexture() {
        super.bindTexture()
    }

    override fun deallocate() {
        super.deallocate()
    }
}
