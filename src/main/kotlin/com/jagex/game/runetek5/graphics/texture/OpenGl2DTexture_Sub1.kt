package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.toolkit.opengl.*
class OpenGl2DTexture_Sub1 : OpenGl2DTexture {

    @JvmField val singleWidth: Int
    @JvmField val singleLength: Int
    @JvmField val t: Float
    @JvmField val s: Float
    @JvmField val tiled: Boolean
    @JvmField val mipmapNearest: Boolean

    constructor(
        toolkit: OpenGlToolkit,
        class1502: GLFormat,
        class763: GLActionFormat,
        i_4: Int,
        i_5: Int,
        width: Int,
        legth: Int
    ) : super(toolkit, GL_TEXTURE_2D, class1502, class763, width, legth) {
        singleWidth = i_4
        singleLength = i_5
        t = i_5.toFloat() / legth
        s = i_4.toFloat() / width
        tiled = false
        mipmapNearest = true
        setRepeat(false, false)
    }

    constructor(
        toolkit: OpenGlToolkit,
        class1502: GLFormat,
        class763: GLActionFormat,
        i_4: Int,
        i_5: Int,
        i_6: Int,
        i_7: Int,
        bytes_8: ByteArray,
        class1509: GLFormat
    ) : super(toolkit, GL_TEXTURE_2D, class1502, class763, i_6, i_7) {
        singleWidth = i_4
        singleLength = i_5
        uploadTextureData(i_7 - i_5, i_4, i_5, bytes_8, class1509, true)
        t = i_5.toFloat() / i_7
        s = i_4.toFloat() / i_6
        tiled = false
        mipmapNearest = true
        setRepeat(false, false)
    }

    constructor(
        toolkit: OpenGlToolkit,
        textureTarget: Int,
        format: GLFormat,
        actionFormat: GLActionFormat,
        width: Int,
        length: Int
    ) : super(toolkit, textureTarget, format, actionFormat, width, length) {
        singleWidth = width
        singleLength = length
        if (textureTarget == GL_TEXTURE_RECTANGLE) {
            t = length.toFloat()
            s = width.toFloat()
            tiled = false
        } else {
            t = 1.0f
            s = 1.0f
            tiled = true
        }
        mipmapNearest = false
    }

    constructor(
        toolkit: OpenGlToolkit,
        target: Int,
        x: Int,
        y: Int,
        width: Int,
        length: Int
    ) : super(toolkit, target, x, y, width, length) {
        singleWidth = width
        singleLength = length
        if (textureTarget == GL_TEXTURE_RECTANGLE) {
            t = length.toFloat()
            s = width.toFloat()
            tiled = false
        } else {
            t = 1.0f
            s = 1.0f
            tiled = true
        }
        mipmapNearest = false
    }

    constructor(
        toolkit: OpenGlToolkit,
        width: Int,
        length: Int,
        totalWidth: Int,
        totalLength: Int,
        ints_6: IntArray
    ) : super(toolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, totalWidth, totalLength) {
        singleWidth = width
        singleLength = length
        updateTextureRegion(0, totalLength - length, width, length, ints_6, 0)
        t = length.toFloat() / totalLength
        s = width.toFloat() / totalWidth
        tiled = false
        mipmapNearest = true
        setRepeat(false, false)
    }

    constructor(
        toolkit: OpenGlToolkit,
        x: Int,
        y: Int,
        i_4: Int,
        i_5: Int,
        width: Int,
        length: Int
    ) : super(toolkit, GL_TEXTURE_2D, x, y, width, length) {
        singleWidth = i_4
        singleLength = i_5
        t = i_5.toFloat() / length
        s = i_4.toFloat() / width
        tiled = false
        mipmapNearest = true
        setRepeat(false, false)
    }

    constructor(
        toolkit: OpenGlToolkit,
        i_2: Int,
        i_3: Int,
        i_4: Int,
        ints_6: IntArray,
        i_7: Int,
        i_8: Int
    ) : super(toolkit, i_2, i_3, i_4, false, ints_6, i_7, i_8, true) {
        singleWidth = i_3
        singleLength = i_4
        if (textureTarget == GL_TEXTURE_RECTANGLE) {
            t = i_4.toFloat()
            s = i_3.toFloat()
            tiled = false
        } else {
            t = 1.0f
            s = 1.0f
            tiled = true
        }
        mipmapNearest = false
    }

    constructor(
        toolkit: OpenGlToolkit,
        target: Int,
        class1503: GLFormat,
        class764: GLActionFormat,
        t: Int,
        s: Int,
        pixels: ByteArray,
        class1509: GLFormat
    ) : super(toolkit, target, class1503, class764, t, s, false, pixels, class1509, true) {
        singleWidth = t
        singleLength = s
        if (textureTarget == GL_TEXTURE_RECTANGLE) {
            this.t = s.toFloat()
            this.s = t.toFloat()
            tiled = false
        } else {
            this.t = 1.0f
            this.s = 1.0f
            tiled = true
        }
        mipmapNearest = false
    }

    override fun setLinearTextureFilter(linear: Boolean) {
        super.setLinearTextureFilter(linear && !mipmapNearest)
    }

    companion object {
        @JvmStatic
        fun method15537(toolkit: OpenGlToolkit, i_1: Int, i_2: Int, ints_4: IntArray, i_5: Int, i_6: Int): OpenGl2DTexture_Sub1 {
            if (!toolkit.aBool8498 && (!OpenGlTexture.isPowerOfTwo(i_1) || !OpenGlTexture.isPowerOfTwo(i_2))) {
                if (toolkit.arbRectangleTexturesSupported)
                    return OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_RECTANGLE, i_1, i_2, ints_4, i_5, i_6)
                return OpenGl2DTexture_Sub1(
                    toolkit, i_1, i_2, MathUtils.nextPowerOfTwo(i_1),
                    MathUtils.nextPowerOfTwo(i_2), ints_4
                )
            }
            return OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_2D, i_1, i_2, ints_4, i_5, i_6)
        }

        @JvmStatic
        fun method15538(toolkit: OpenGlToolkit, x: Int, y: Int, width: Int, length: Int): OpenGl2DTexture_Sub1 {
            if (!toolkit.aBool8498 && (!OpenGlTexture.isPowerOfTwo(width) || !OpenGlTexture.isPowerOfTwo(length))) {
                if (toolkit.arbRectangleTexturesSupported) {
                    return OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_RECTANGLE, x, y, width, length)
                }
                return OpenGl2DTexture_Sub1(toolkit, x, y, width, length, MathUtils.nextPowerOfTwo(width), MathUtils.nextPowerOfTwo(length))
            }
            return OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_2D, x, y, width, length)
        }

        @JvmStatic
        fun createSurface(
            toolkit: OpenGlToolkit,
            class1501: GLFormat,
            class762: GLActionFormat,
            i_3: Int,
            i_4: Int,
            bytes_6: ByteArray,
            class1507: GLFormat
        ): OpenGl2DTexture_Sub1 {
            if (!toolkit.aBool8498 && (!OpenGlTexture.isPowerOfTwo(i_3) || !OpenGlTexture.isPowerOfTwo(i_4))) {
                if (toolkit.arbRectangleTexturesSupported)
                    return OpenGl2DTexture_Sub1(
                        toolkit, GL_TEXTURE_RECTANGLE, class1501, class762, i_3, i_4,
                        bytes_6, class1507
                    )
                return OpenGl2DTexture_Sub1(
                    toolkit, class1501, class762, i_3, i_4,
                    MathUtils.nextPowerOfTwo(i_3), MathUtils.nextPowerOfTwo(i_4), bytes_6, class1507
                )
            }
            return OpenGl2DTexture_Sub1(
                toolkit, GL_TEXTURE_2D, class1501, class762, i_3, i_4,
                bytes_6, class1507
            )
        }

        @JvmStatic
        fun method15547(
            toolkit: OpenGlToolkit,
            class1501: GLFormat,
            class762: GLActionFormat,
            i_3: Int,
            i_4: Int
        ): OpenGl2DTexture_Sub1 {
            if (!toolkit.aBool8498 && (!OpenGlTexture.isPowerOfTwo(i_3) || !OpenGlTexture.isPowerOfTwo(i_4))) {
                if (toolkit.arbRectangleTexturesSupported)
                    return OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_RECTANGLE, class1501, class762, i_3, i_4)
                return OpenGl2DTexture_Sub1(
                    toolkit, class1501, class762, i_3, i_4,
                    MathUtils.nextPowerOfTwo(i_3), MathUtils.nextPowerOfTwo(i_4)
                )
            }
            return OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_2D, class1501, class762, i_3, i_4)
        }
    }
}
