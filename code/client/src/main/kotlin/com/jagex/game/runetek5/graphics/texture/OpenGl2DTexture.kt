package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.imageproducer.AbstractDisplay
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

open class OpenGl2DTexture : OpenGlTexture {

    @JvmField val width: Int
    @JvmField val length: Int

    constructor(
        toolkit: OpenGlToolkit,
        textureTarget: Int,
        width: Int,
        height: Int,
        mipmapping: Boolean,
        pixels: IntArray,
        rowLength: Int,
        size: Int,
        flipVertical: Boolean
    ) : super(toolkit, textureTarget, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, width * height, mipmapping) {
        this.width = width
        this.length = height
        var pixelsToUse = pixels
        if (flipVertical) {
            val modifiedPixels = IntArray(pixels.size)

            for (row in 0 until height) {
                var dst = width * row
                var src = width * (height - row - 1)

                for (col in 0 until width) {
                    modifiedPixels[dst++] = pixels[src++]
                }
            }

            pixelsToUse = modifiedPixels
        }

        activeToolkit.setActiveTexture(this)
        if (textureTarget != GL_TEXTURE_RECTANGLE && mipmapping && rowLength == 0 && size == 0) {
            createMipmappedCubeTexture(textureTarget, OpenGlToolkit.getInternalTextureFormat(internalFormat, actionFormat), width, height, activeToolkit.pixelType, pixelsToUse)
            setMipmapping(true)
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength)
            OpenGL.glTexImage2Di(textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(internalFormat, actionFormat), width, height, 0, GL_BGRA, activeToolkit.pixelType, pixelsToUse, size * 4)
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            setMipmapping(false)
        }

        setLinearTextureFilter(true)
    }

    constructor(
        toolkit: OpenGlToolkit,
        textureTarget: Int,
        internalFormat: GLFormat,
        actionFormat: GLActionFormat,
        width: Int,
        height: Int,
        mipmapping: Boolean,
        pixels: ByteArray,
        pixelFormat: GLFormat,
        flipVertical: Boolean
    ) : super(toolkit, textureTarget, internalFormat, actionFormat, width * height, mipmapping) {
        this.width = width
        this.length = height
        var pixelsToUse = pixels
        if (flipVertical) {
            val modifiedPixels = ByteArray(pixels.size)
            for (row in 0 until height) {
                var src = width * row
                var dst = width * (height - row - 1)
                for (col in 0 until width) {
                    modifiedPixels[src++] = pixels[dst++]
                }
            }
            pixelsToUse = modifiedPixels
        }
        activeToolkit.setActiveTexture(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        if (mipmapping && textureTarget != GL_TEXTURE_RECTANGLE) {
            uploadMipmappedByteTexture(textureTarget, OpenGlToolkit.getInternalTextureFormat(internalFormat, actionFormat), width, height, pixelFormat, pixelsToUse)
            setMipmapping(true)
        } else {
            OpenGL.glTexImage2Dub(textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(internalFormat, actionFormat), width, height, 0, OpenGlToolkit.format(pixelFormat), GL_UNSIGNED_BYTE, pixelsToUse, 0)
            setMipmapping(false)
        }

        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
        setLinearTextureFilter(true)
    }

    constructor(
        toolkit: OpenGlToolkit,
        textureTarget: Int,
        x: Int,
        y: Int,
        width: Int,
        length: Int
    ) : super(toolkit, textureTarget, GLFormat.RGB, GLActionFormat.PIXEL_FORMAT, width * length, false) {
        this.width = width
        this.length = length
        activeToolkit.setActiveTexture(this)
        val display = activeToolkit.getDisplay()
        if (display != null) {
            val adjustedY = display.getHeight() - (y + length)
            val internalFormat = OpenGlToolkit.getInternalTextureFormat(internalFormat, actionFormat)
            OpenGL.glCopyTexImage2D(textureTarget, 0, internalFormat, x, adjustedY, width, length, 0)
        }

        setLinearTextureFilter(true)
    }

    constructor(
        toolkit: OpenGlToolkit,
        internalFormat: GLFormat,
        actionFormat: GLActionFormat,
        width: Int,
        height: Int,
        mipmapping: Boolean,
        pixels: FloatArray,
        pixelFormat: GLFormat
    ) : super(toolkit, GL_TEXTURE_2D, internalFormat, actionFormat, width * height, mipmapping) {
        this.width = width
        this.length = height
        activeToolkit.setActiveTexture(this)
        if (mipmapping && textureTarget != GL_TEXTURE_RECTANGLE) {
            generateMipmaps(OpenGlToolkit.getInternalTextureFormat(internalFormat, actionFormat), width, height, pixelFormat, pixels)
            setMipmapping(true)
        } else {
            OpenGL.glTexImage2Df(textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(internalFormat, actionFormat), width, height, 0, OpenGlToolkit.format(pixelFormat), 5126, pixels, 0)
            setMipmapping(false)
        }
        setLinearTextureFilter(true)
    }

    constructor(
        toolkit: OpenGlToolkit,
        textureTarget: Int,
        format: GLFormat,
        action: GLActionFormat,
        width: Int,
        length: Int
    ) : super(toolkit, textureTarget, format, action, width * length, false) {
        this.width = width
        this.length = length
        activeToolkit.setActiveTexture(this)
        OpenGL.glTexImage2Dub(textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(internalFormat, actionFormat), width, length, 0, OpenGlToolkit.format(internalFormat), GL_UNSIGNED_BYTE, null, 0)
        setLinearTextureFilter(true)
    }

    fun copyTextureSubImage(offsetX: Int, offsetY: Int, width: Int, height: Int, x: Int, y: Int) {
        val display = activeToolkit.getDisplay()
        if (display != null) {
            val adjustedY = display.getHeight() - (height + y)
            activeToolkit.setActiveTexture(this)
            OpenGL.glCopyTexSubImage2D(textureTarget, 0, offsetX, length - (offsetY + height), x, adjustedY, width, height)
            OpenGL.glFlush()
        }
    }

    fun createRenderBuffer(): RenderBuffer {
        return FramebufferTexture2DEXT(this)
    }

    fun createFrameBuffer(): FrameBuffer {
        return FramebufferTexture2DEXT(this)
    }

    fun setRepeat(repeatS: Boolean, repeatT: Boolean) {
        if (textureTarget == GL_TEXTURE_2D) {
            activeToolkit.setActiveTexture(this)
            OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, if (repeatS) GL_REPEAT else GL_CLAMP_TO_EDGE)
            OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, if (repeatT) GL_REPEAT else GL_CLAMP_TO_EDGE)
        }
    }

    fun updateTextureRegion(x: Int, y: Int, width: Int, height: Int, originalPixels: IntArray, rowLength: Int) {
        var effectiveRowLength = rowLength
        if (effectiveRowLength == 0) {
            effectiveRowLength = width
        }
        val modifiedPixels = IntArray(width * height)
        for (row in 0 until height) {
            var dst = width * row
            var src = (height - row - 1) * effectiveRowLength
            for (col in 0 until width) {
                modifiedPixels[dst++] = originalPixels[src++]
            }
        }
        activeToolkit.setActiveTexture(this)
        if (width != effectiveRowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, effectiveRowLength)
        }
        OpenGL.glTexSubImage2Di(textureTarget, 0, x, length - y - height, width, height, GL_BGRA, activeToolkit.pixelType, modifiedPixels, 0)
        if (width != effectiveRowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        }
    }

    fun uploadTextureData(offsetX: Int, offsetY: Int, width: Int, data: ByteArray, format: GLFormat, flipVertical: Boolean) {
        var dataToUse = data
        if (flipVertical) {
            val bytesPerPixel = format.bytesPerPixel
            val rowSize = offsetY * bytesPerPixel
            val flippedRowSize = bytesPerPixel * offsetY
            val modifiedData = ByteArray(rowSize * width)
            for (row in 0 until width) {
                var originalRowIndex = row * rowSize
                var flippedRowIndex = flippedRowSize * (width - row - 1)
                for (col in 0 until rowSize) {
                    modifiedData[originalRowIndex++] = data[flippedRowIndex++]
                }
            }
            dataToUse = modifiedData
        }
        activeToolkit.setActiveTexture(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glTexSubImage2Dub(textureTarget, 0, 0, length - offsetX - width, offsetY, width, OpenGlToolkit.format(format), GL_UNSIGNED_BYTE, dataToUse, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
    }
}
