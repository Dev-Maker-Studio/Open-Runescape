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
        ints_5: IntArray,
        i_6: Int,
        i_7: Int
    ) : super(toolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, height * width, mipmapsGenerated) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        if (mipmapsGenerated && i_7 == 0 && i_6 == 0) {
            generateMipmaps(textureTarget, width, height, ints_5)
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_7)
            OpenGL.glTexImage2Di(textureTarget, 0, GL_RGBA, width, height, 0, GL_BGRA, activeToolkit.pixelType, ints_5, i_6 * 4)
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }
    }

    constructor(
        toolkit: OpenGLNativeToolkit,
        class1502: GLFormat,
        width: Int,
        height: Int,
        bool5: Boolean,
        pixels: ByteArray
    ) : super(toolkit, GL_TEXTURE_2D, class1502, GLActionFormat.PIXEL_FORMAT, width * height, bool5) {
        this.width = width
        this.height = height
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        if (bool5) {
            generateMipmaps(textureTarget, width, height, pixels)
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            val class1509 = pixelFormat
            if (class1509 === GLFormat.aClass1501956) {
                OpenGL.glCompressedTexImage2Dub(textureTarget, 0, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, width, height, 0, width * height / 2, pixels, 0)
            } else {
                val class15010 = pixelFormat
                if (class15010 === GLFormat.aClass1501957) {
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
        class505Sub2Sub11: OpenGLNativeToolkit,
        class1502: GLFormat,
        i_3: Int,
        i_4: Int,
        floats_6: FloatArray
    ) : super(class505Sub2Sub11, GL_TEXTURE_2D, class1502, GLActionFormat.FLOAT_32_ARB, i_3 * i_4, true) {
        width = i_3
        height = i_4
        activeToolkit.setTextureSampler(this)
        if (true) {
            generateMipmaps(textureTarget, i_3, i_4, floats_6)
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            OpenGL.glTexImage2Df(
                textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
                i_3, i_4, 0, OpenGLNativeToolkit.texture2dFormat(pixelFormat), 5126, floats_6, 0
            )
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }
    }

    constructor(
        toolkit: OpenGLNativeToolkit,
        class1502: GLFormat,
        class763: GLActionFormat,
        width: Int,
        height: Int
    ) : super(toolkit, GL_TEXTURE_2D, class1502, class763, height * width, false) {
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

    fun method14494(i_3: Int, i_4: Int, floats_5: FloatArray, class1506: GLFormat) {
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        OpenGL.glTexSubImage2Df(textureTarget, 0, 0, 0, i_3, i_4, OpenGLNativeToolkit.texture2dFormat(class1506), GL_UNSIGNED_BYTE, floats_5, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
    }

    override fun method46(): Boolean {
        return true
    }

    override fun setRepeat(repeatS: Boolean, repeatT: Boolean) {
        activeToolkit.setTextureSampler(this)
        OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, if (repeatS) GL_REPEAT else GL_CLAMP_TO_EDGE)
        OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, if (repeatT) GL_REPEAT else GL_CLAMP_TO_EDGE)
    }

    override fun method52(i_1: Int, i_2: Int, i_3: Int, i_4: Int, bytes_5: ByteArray, class1506: GLFormat, i_7: Int, i_8: Int) {
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_8)
        OpenGL.glTexSubImage2Dub(textureTarget, 0, i_1, i_2, i_3, i_4, OpenGLNativeToolkit.texture2dFormat(class1506), GL_UNSIGNED_BYTE, bytes_5, i_7)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
    }

    override fun method47(f_1: Float): Float {
        return f_1 / width
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

    override fun method48(i_1: Int, i_2: Int, i_3: Int, i_4: Int, ints_5: IntArray, i_6: Int, i_7: Int) {
        activeToolkit.setTextureSampler(this)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_7)
        OpenGL.glTexSubImage2Di(textureTarget, 0, i_1, i_2, i_3, i_4, GL_BGRA, activeToolkit.pixelType, ints_5, i_6)
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
    }

    override fun method62(f_1: Float): Float {
        return f_1 / height
    }

    override fun method53(i_3: Int, i_4: Int, ints_5: IntArray) {
        val ints_7 = IntArray(width * height)
        activeToolkit.setTextureSampler(this)
        OpenGL.glGetTexImagei(textureTarget, 0, GL_BGRA, GL_UNSIGNED_BYTE, ints_7, 0)

        for (i_8 in 0 until i_4) {
            System.arraycopy(ints_7, width * ((i_4 - 1) - i_8), ints_5, i_3 * i_8, i_3)
        }
    }

    override fun enableMipmapping(): Boolean {
        return super.enableMipmapping()
    }
}
