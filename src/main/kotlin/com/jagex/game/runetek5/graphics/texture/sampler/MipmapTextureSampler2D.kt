package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.graphics.texture.FrameBufferTexture2D
import jaggl.OpenGL

class MipmapTextureSampler2D : MipmapTextureSampler, RectangleTextureSampler {

    @JvmField val width: Int
    @JvmField val height: Int

    constructor(
        toolkit: OpenGLNativeToolkit,
        width: Int,
        height: Int,
        mipmapsGenerated: Boolean,
        pixels: IntArray,
        offset: Int,
        rowLength: Int
    ) : super(toolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, height * width, mipmapsGenerated) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        if (mipmapsGenerated && rowLength == 0 && offset == 0) {
            generateMipmaps(textureTarget, width, height, pixels)
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength)
            OpenGL.glTexImage2Di(textureTarget, 0, GL_RGBA, width, height, 0, GL_BGRA, activeToolkit.pixelType, pixels, offset * 4)
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }
    }

    constructor(
        toolkit: OpenGLNativeToolkit,
        format: GLFormat,
        width: Int,
        height: Int,
        mipmapped: Boolean,
        pixels: ByteArray
    ) : super(toolkit, GL_TEXTURE_2D, format, GLActionFormat.PIXEL_FORMAT, width * height, mipmapped) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        if (mipmapped) {
            generateMipmaps(textureTarget, width, height, pixels)
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            val dxt1TestFormat = pixelFormat
            if (dxt1TestFormat === GLFormat.COMPRESSED_DXT1) {
                OpenGL.glCompressedTexImage2Dub(textureTarget, 0, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, width, height, 0, width * height / 2, pixels, 0)
            } else {
                val dxt5TestFormat = pixelFormat
                if (dxt5TestFormat === GLFormat.COMPRESSED_DXT5) {
                    OpenGL.glCompressedTexImage2Dub(textureTarget, 0, GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, width, height, 0, width * height, pixels, 0)
                } else {
                    OpenGL.glTexImage2Dub(textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat), width, height, 0, OpenGLNativeToolkit.texture2dFormat(pixelFormat), GL_UNSIGNED_BYTE, pixels, 0)
                }
            }

            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }

        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
    }

    @Suppress("unused")
    constructor(
        toolkit: OpenGLNativeToolkit,
        format: GLFormat,
        width: Int,
        height: Int,
        pixels: FloatArray
    ) : super(toolkit, GL_TEXTURE_2D, format, GLActionFormat.FLOAT_32_ARB, width * height, true) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        if (true) {
            generateMipmaps(textureTarget, width, height, pixels)
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            OpenGL.glTexImage2Df(
                textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
                width, height, 0, OpenGLNativeToolkit.texture2dFormat(pixelFormat), 5126, pixels, 0
            )
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }
    }

    constructor(
        toolkit: OpenGLNativeToolkit,
        format: GLFormat,
        actionFormat: GLActionFormat,
        width: Int,
        height: Int
    ) : super(toolkit, GL_TEXTURE_2D, format, actionFormat, height * width, false) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glTexImage2Dub(
            textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
            width, height, 0, OpenGLNativeToolkit.texture2dFormat(pixelFormat),
            OpenGLNativeToolkit.actionFormatDataType(pixelActionFormat), null, 0
        )
    }

    override fun bindTexture() {
        super.bindTexture()
    }

    override fun getTextureWidth(): Int {
        return width
    }

    override fun getTextureHeight(): Int {
        return height
    }

    fun updateSubImageFloat(regionWidth: Int, regionHeight: Int, pixels: FloatArray, format: GLFormat) {
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        OpenGL.glTexSubImage2Df(textureTarget, 0, 0, 0, regionWidth, regionHeight, OpenGLNativeToolkit.texture2dFormat(format), GL_UNSIGNED_BYTE, pixels, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
    }

    override fun supportsTextureRepeat(): Boolean {
        return true
    }

    override fun setRepeat(repeatS: Boolean, repeatT: Boolean) {
        activeToolkit.setTextureSampler(this)
        OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, if (repeatS) GL_REPEAT else GL_CLAMP_TO_EDGE)
        OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, if (repeatT) GL_REPEAT else GL_CLAMP_TO_EDGE)
    }

    override fun updateSubImageByte(offsetX: Int, offsetY: Int, regionWidth: Int, regionHeight: Int, data: ByteArray, format: GLFormat, offset: Int, rowLength: Int) {
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength)
        OpenGL.glTexSubImage2Dub(textureTarget, 0, offsetX, offsetY, regionWidth, regionHeight, OpenGLNativeToolkit.texture2dFormat(format), GL_UNSIGNED_BYTE, data, offset)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
    }

    override fun toTextureCoordS(x: Float): Float {
        return x / width
    }

    override fun getFramebuffer(): FrameBuffer {
        return FrameBufferTexture2D(this, 0)
    }

    override fun setSamplerMode(mode: SampleMode) {
        super.setSamplerMode(mode)
    }

    override fun deallocate() {
        super.deallocate()
    }

    override fun updateTextureRegion(offsetX: Int, offsetY: Int, regionWidth: Int, regionHeight: Int, pixels: IntArray, offset: Int, rowLength: Int) {
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength)
        OpenGL.glTexSubImage2Di(textureTarget, 0, offsetX, offsetY, regionWidth, regionHeight, GL_BGRA, activeToolkit.pixelType, pixels, offset)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
    }

    override fun toTextureCoordT(y: Float): Float {
        return y / height
    }

    override fun readPixels(destWidth: Int, destHeight: Int, dest: IntArray) {
        val src = IntArray(width * height)
        activeToolkit.setTextureSampler(this)
        OpenGL.glGetTexImagei(textureTarget, 0, GL_BGRA, GL_UNSIGNED_BYTE, src, 0)

        for (row in 0 until destHeight) {
            System.arraycopy(src, width * ((destHeight - 1) - row), dest, destWidth * row, destWidth)
        }
    }

    override fun enableMipmapping(): Boolean {
        return super.enableMipmapping()
    }
}
