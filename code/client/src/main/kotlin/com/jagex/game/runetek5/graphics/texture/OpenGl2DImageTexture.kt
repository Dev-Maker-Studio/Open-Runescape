package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.toolkit.opengl.*
class OpenGl2DImageTexture : OpenGl2DTexture {

    @JvmField val singleWidth: Int
    @JvmField val singleLength: Int
    @JvmField val t: Float
    @JvmField val s: Float
    @JvmField val tiled: Boolean
    @JvmField val forceNearestFilter: Boolean

    constructor(
        toolkit: OpenGlToolkit,
        format: GLFormat,
        actionFormat: GLActionFormat,
        imageWidth: Int,
        imageLength: Int,
        totalWidth: Int,
        totalLength: Int
    ) : super(toolkit, GL_TEXTURE_2D, format, actionFormat, totalWidth, totalLength) {
        singleWidth = imageWidth
        singleLength = imageLength
        t = imageLength.toFloat() / totalLength
        s = imageWidth.toFloat() / totalWidth
        tiled = false
        forceNearestFilter = true
        setRepeat(false, false)
    }

    constructor(
        toolkit: OpenGlToolkit,
        format: GLFormat,
        actionFormat: GLActionFormat,
        imageWidth: Int,
        imageLength: Int,
        totalWidth: Int,
        totalLength: Int,
        pixels: ByteArray,
        pixelFormat: GLFormat
    ) : super(toolkit, GL_TEXTURE_2D, format, actionFormat, totalWidth, totalLength) {
        singleWidth = imageWidth
        singleLength = imageLength
        uploadTextureData(totalLength - imageLength, imageWidth, imageLength, pixels, pixelFormat, true)
        t = imageLength.toFloat() / totalLength
        s = imageWidth.toFloat() / totalWidth
        tiled = false
        forceNearestFilter = true
        setRepeat(false, false)
    }

    constructor(
        toolkit: OpenGlToolkit,
        target: Int,
        format: GLFormat,
        actionFormat: GLActionFormat,
        width: Int,
        length: Int
    ) : super(toolkit, target, format, actionFormat, width, length) {
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
        forceNearestFilter = false
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
        forceNearestFilter = false
    }

    constructor(
        toolkit: OpenGlToolkit,
        imageWidth: Int,
        imageLength: Int,
        totalWidth: Int,
        totalLength: Int,
        pixels: IntArray
    ) : super(toolkit, GL_TEXTURE_2D, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, totalWidth, totalLength) {
        singleWidth = imageWidth
        singleLength = imageLength
        updateTextureRegion(0, totalLength - imageLength, imageWidth, imageLength, pixels, 0)
        t = imageLength.toFloat() / totalLength
        s = imageWidth.toFloat() / totalWidth
        tiled = false
        forceNearestFilter = true
        setRepeat(false, false)
    }

    constructor(
        toolkit: OpenGlToolkit,
        x: Int,
        y: Int,
        imageWidth: Int,
        imageLength: Int,
        totalWidth: Int,
        totalLength: Int
    ) : super(toolkit, GL_TEXTURE_2D, x, y, totalWidth, totalLength) {
        singleWidth = imageWidth
        singleLength = imageLength
        t = imageLength.toFloat() / totalLength
        s = imageWidth.toFloat() / totalWidth
        tiled = false
        forceNearestFilter = true
        setRepeat(false, false)
    }

    constructor(
        toolkit: OpenGlToolkit,
        target: Int,
        width: Int,
        length: Int,
        pixels: IntArray,
        rowLength: Int,
        pixelOffset: Int
    ) : super(toolkit, target, width, length, false, pixels, rowLength, pixelOffset, true) {
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
        forceNearestFilter = false
    }

    constructor(
        toolkit: OpenGlToolkit,
        target: Int,
        format: GLFormat,
        actionFormat: GLActionFormat,
        width: Int,
        length: Int,
        pixels: ByteArray,
        pixelFormat: GLFormat
    ) : super(toolkit, target, format, actionFormat, width, length, false, pixels, pixelFormat, true) {
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
        forceNearestFilter = false
    }

    override fun setLinearTextureFilter(linear: Boolean) {
        super.setLinearTextureFilter(linear && !forceNearestFilter)
    }

    companion object {
        @JvmStatic
        fun createFromPixels(toolkit: OpenGlToolkit, width: Int, length: Int, pixels: IntArray, rowLength: Int, pixelOffset: Int): OpenGl2DImageTexture {
            if (!toolkit.framebufferObjectSupported && (!OpenGlTexture.isPowerOfTwo(width) || !OpenGlTexture.isPowerOfTwo(length))) {
                if (toolkit.arbRectangleTexturesSupported)
                    return OpenGl2DImageTexture(toolkit, GL_TEXTURE_RECTANGLE, width, length, pixels, rowLength, pixelOffset)
                return OpenGl2DImageTexture(
                    toolkit, width, length, MathUtils.nextPowerOfTwo(width),
                    MathUtils.nextPowerOfTwo(length), pixels
                )
            }
            return OpenGl2DImageTexture(toolkit, GL_TEXTURE_2D, width, length, pixels, rowLength, pixelOffset)
        }

        @JvmStatic
        fun createFromScreenCapture(toolkit: OpenGlToolkit, x: Int, y: Int, width: Int, length: Int): OpenGl2DImageTexture {
            if (!toolkit.framebufferObjectSupported && (!OpenGlTexture.isPowerOfTwo(width) || !OpenGlTexture.isPowerOfTwo(length))) {
                if (toolkit.arbRectangleTexturesSupported) {
                    return OpenGl2DImageTexture(toolkit, GL_TEXTURE_RECTANGLE, x, y, width, length)
                }
                return OpenGl2DImageTexture(toolkit, x, y, width, length, MathUtils.nextPowerOfTwo(width), MathUtils.nextPowerOfTwo(length))
            }
            return OpenGl2DImageTexture(toolkit, GL_TEXTURE_2D, x, y, width, length)
        }

        @JvmStatic
        fun createSurface(
            toolkit: OpenGlToolkit,
            format: GLFormat,
            actionFormat: GLActionFormat,
            width: Int,
            length: Int,
            pixels: ByteArray,
            pixelFormat: GLFormat
        ): OpenGl2DImageTexture {
            if (!toolkit.framebufferObjectSupported && (!OpenGlTexture.isPowerOfTwo(width) || !OpenGlTexture.isPowerOfTwo(length))) {
                if (toolkit.arbRectangleTexturesSupported)
                    return OpenGl2DImageTexture(
                        toolkit, GL_TEXTURE_RECTANGLE, format, actionFormat, width, length,
                        pixels, pixelFormat
                    )
                return OpenGl2DImageTexture(
                    toolkit, format, actionFormat, width, length,
                    MathUtils.nextPowerOfTwo(width), MathUtils.nextPowerOfTwo(length), pixels, pixelFormat
                )
            }
            return OpenGl2DImageTexture(
                toolkit, GL_TEXTURE_2D, format, actionFormat, width, length,
                pixels, pixelFormat
            )
        }

        @JvmStatic
        fun createEmpty(
            toolkit: OpenGlToolkit,
            format: GLFormat,
            actionFormat: GLActionFormat,
            width: Int,
            length: Int
        ): OpenGl2DImageTexture {
            if (!toolkit.framebufferObjectSupported && (!OpenGlTexture.isPowerOfTwo(width) || !OpenGlTexture.isPowerOfTwo(length))) {
                if (toolkit.arbRectangleTexturesSupported)
                    return OpenGl2DImageTexture(toolkit, GL_TEXTURE_RECTANGLE, format, actionFormat, width, length)
                return OpenGl2DImageTexture(
                    toolkit, format, actionFormat, width, length,
                    MathUtils.nextPowerOfTwo(width), MathUtils.nextPowerOfTwo(length)
                )
            }
            return OpenGl2DImageTexture(toolkit, GL_TEXTURE_2D, format, actionFormat, width, length)
        }
    }
}
