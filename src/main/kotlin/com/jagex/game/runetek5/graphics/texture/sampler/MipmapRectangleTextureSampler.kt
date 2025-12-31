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
        class1502: GLFormat,
        width: Int,
        height: Int,
        floats_5: FloatArray
    ) : super(toolkit, GL_TEXTURE_RECTANGLE, class1502, GLActionFormat.FLOAT_32_ARB, width * height, false) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        OpenGL.glTexImage2Df(
            textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
            width, height, 0, OpenGLNativeToolkit.texture2dFormat(pixelFormat), 5126, floats_5, 0
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
        ints_4: IntArray,
        i_5: Int,
        i_6: Int
    ) : super(toolkit, GL_TEXTURE_RECTANGLE, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, height * width, false) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_6)
        OpenGL.glTexImage2Di(textureTarget, 0, GL_RGBA, width, height, 0, GL_BGRA, activeToolkit.pixelType, ints_4, i_5 * 4)
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

    override fun method47(f_1: Float): Float {
        return f_1
    }

    override fun getTextureWidth(): Int {
        return width
    }

    override fun getTextureHeight(): Int {
        return height
    }

    override fun method62(f_1: Float): Float {
        return f_1
    }

    override fun method48(i_1: Int, i_2: Int, i_3: Int, i_4: Int, data: IntArray, offset: Int, rows: Int) {
        var rowLength = rows
        if (rowLength == 0) {
            rowLength = i_3
        }

        activeToolkit.setTextureSampler(this)
        if (i_3 != rowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength)
        }

        OpenGL.glTexSubImage2Di(textureTarget, 0, i_1, i_2, i_3, i_4, GL_BGRA, activeToolkit.pixelType, data, offset)
        if (i_3 != rowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }
    }

    override fun method46(): Boolean {
        return false
    }

    override fun setRepeat(repeatS: Boolean, repeatT: Boolean) {
        // NOOP
    }

    override fun method52(offsetX: Int, offsetY: Int, width: Int, height: Int, data: ByteArray, class1506: GLFormat, offset: Int, i_8: Int) {
        var i_81 = i_8
        if (i_81 == 0) {
            i_81 = width
        }

        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        if (width != i_81) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_81)
        }

        OpenGL.glTexSubImage2Dub(textureTarget, 0, offsetX, offsetY, width, height, OpenGLNativeToolkit.texture2dFormat(class1506), GL_UNSIGNED_BYTE, data, offset)
        if (width != i_81) {
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

    override fun method53(length: Int, i_4: Int, dest: IntArray) {
        val src = IntArray(width * height)
        activeToolkit.setTextureSampler(this)
        OpenGL.glGetTexImagei(textureTarget, 0, GL_BGRA, GL_UNSIGNED_BYTE, src, 0)

        for (i_8 in 0 until i_4) {
            val srcPos = width * ((i_4 - 1) - i_8)
            val dstPos = length * i_8
            System.arraycopy(src, srcPos, dest, dstPos, length)
        }
    }

    override fun bindTexture() {
        super.bindTexture()
    }
}
