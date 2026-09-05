package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import jaggl.OpenGL

/**
 * @author Steven - 23/08/2023
 */
class MipmapRectangleTextureSampler : MipmapTextureSampler, RectangleTextureSampler {

    @JvmField val width: Int
    @JvmField val height: Int

    constructor(
        toolkit: OpenGLNativeToolkit,
        format: GLFormat,
        width: Int,
        height: Int,
        pixels: FloatArray
    ) : super(toolkit, GL_TEXTURE_RECTANGLE, format, GLActionFormat.FLOAT_32_ARB, width * height, false) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        OpenGL.glTexImage2Df(
            textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
            width, height, 0, OpenGLNativeToolkit.texture2dFormat(pixelFormat), 5126, pixels, 0
        )
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
    }

    constructor(
        toolkit: OpenGLNativeToolkit,
        format: GLFormat,
        actionFormat: GLActionFormat,
        width: Int,
        height: Int
    ) : super(toolkit, GL_TEXTURE_RECTANGLE, format, actionFormat, height * width, false) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glTexImage2Dub(
            textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
            width, height, 0, OpenGLNativeToolkit.texture2dFormat(pixelFormat),
            OpenGLNativeToolkit.actionFormatDataType(pixelActionFormat), null, 0
        )
    }

    constructor(
        toolkit: OpenGLNativeToolkit,
        width: Int,
        height: Int,
        pixels: IntArray,
        offset: Int,
        rowLength: Int
    ) : super(toolkit, GL_TEXTURE_RECTANGLE, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, height * width, false) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength)
        OpenGL.glTexImage2Di(textureTarget, 0, GL_RGBA, width, height, 0, GL_BGRA, activeToolkit.pixelType, pixels, offset * 4)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
    }

    constructor(
        toolkit: OpenGLNativeToolkit,
        format: GLFormat,
        width: Int,
        height: Int,
        data: ByteArray
    ) : super(toolkit, GL_TEXTURE_RECTANGLE, format, GLActionFormat.PIXEL_FORMAT, width * height, false) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        OpenGL.glTexImage2Dub(
            textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
            width, height, 0, OpenGLNativeToolkit.texture2dFormat(pixelFormat), GL_UNSIGNED_BYTE, data, 0
        )
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
    }

    override fun toTextureCoordS(x: Float): Float {
        return x
    }

    override fun getTextureWidth(): Int {
        return width
    }

    override fun getTextureHeight(): Int {
        return height
    }

    override fun toTextureCoordT(y: Float): Float {
        return y
    }

    override fun updateTextureRegion(offsetX: Int, offsetY: Int, regionWidth: Int, regionHeight: Int, pixels: IntArray, offset: Int, rowLength: Int) {
        var effectiveRowLength = rowLength
        if (effectiveRowLength == 0) {
            effectiveRowLength = regionWidth
        }

        activeToolkit.setTextureSampler(this)
        if (regionWidth != effectiveRowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, effectiveRowLength)
        }

        OpenGL.glTexSubImage2Di(textureTarget, 0, offsetX, offsetY, regionWidth, regionHeight, GL_BGRA, activeToolkit.pixelType, pixels, offset)
        if (regionWidth != effectiveRowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }
    }

    override fun supportsTextureRepeat(): Boolean {
        return false
    }

    override fun setRepeat(repeatS: Boolean, repeatT: Boolean) {
        // NOOP
    }

    override fun updateSubImageByte(offsetX: Int, offsetY: Int, regionWidth: Int, regionHeight: Int, data: ByteArray, format: GLFormat, offset: Int, rowLength: Int) {
        var effectiveRowLength = rowLength
        if (effectiveRowLength == 0) {
            effectiveRowLength = regionWidth
        }

        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        if (regionWidth != effectiveRowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, effectiveRowLength)
        }

        OpenGL.glTexSubImage2Dub(textureTarget, 0, offsetX, offsetY, regionWidth, regionHeight, OpenGLNativeToolkit.texture2dFormat(format), GL_UNSIGNED_BYTE, data, offset)
        if (regionWidth != effectiveRowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }

        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
    }

    override fun enableMipmapping(): Boolean {
        return super.enableMipmapping()
    }

    override fun getFramebuffer(): FrameBuffer {
        return FrameBufferRectangleTexture(this, 0)
    }

    override fun setSamplerMode(mode: SampleMode) {
        super.setSamplerMode(mode)
    }

    override fun deallocate() {
        super.deallocate()
    }

    override fun readPixels(destWidth: Int, destHeight: Int, dest: IntArray) {
        val src = IntArray(width * height)
        activeToolkit.setTextureSampler(this)
        OpenGL.glGetTexImagei(textureTarget, 0, GL_BGRA, GL_UNSIGNED_BYTE, src, 0)

        for (row in 0 until destHeight) {
            val srcPos = width * ((destHeight - 1) - row)
            val dstPos = destWidth * row
            System.arraycopy(src, srcPos, dest, dstPos, destWidth)
        }
    }

    override fun bindTexture() {
        super.bindTexture()
    }
}
