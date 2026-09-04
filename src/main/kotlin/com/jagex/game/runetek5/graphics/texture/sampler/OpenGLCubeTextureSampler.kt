package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_CUBE_MAP
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import jaggl.OpenGL

class OpenGLCubeTextureSampler(
    toolkit: OpenGLNativeToolkit,
    sizeXy: Int,
    mipmapsGenerated: Boolean,
    materialPixels: Array<IntArray>
) : MipmapTextureSampler(toolkit, GL_TEXTURE_CUBE_MAP, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, sizeXy * sizeXy * 6, mipmapsGenerated), CubeTextureSampler {

    init {
        super.activeToolkit.setTextureSampler(this)
        if (mipmapsGenerated) {
            for (index in 0 until 6) {
                generateMipmaps(index + GL_TEXTURE_CUBE_MAP_POSITIVE_X, sizeXy, sizeXy, materialPixels[index])
            }
        } else {
            for (index in 0 until 6) {
                OpenGL.glTexImage2Di(index + GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat),
                    sizeXy, sizeXy, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), super.activeToolkit.pixelType, materialPixels[index], 0)
            }
        }
    }

    override fun bindTexture() {
        super.bindTexture()
    }

    override fun setSamplerMode(sampleMode: SampleMode) {
        super.setSamplerMode(sampleMode)
    }

    override fun deallocate() {
        super.deallocate()
    }
}
