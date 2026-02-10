package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

abstract class OpenGlTexture(
    @JvmField val activeToolkit: OpenGlToolkit,
    @JvmField val textureTarget: Int,
    @JvmField val internalFormat: GLFormat,
    @JvmField val actionFormat: GLActionFormat,
    @JvmField val pixelCount: Int,
    mipmapping: Boolean
) : AutoCloseable {

    @JvmField
    var linearTextureFilter: Boolean = false

    @JvmField
    var mipmappingEnabled: Boolean = false

    @JvmField
    var activeHandle: Int = 0

    init {
        this.mipmappingEnabled = mipmapping
        OpenGL.glGenTextures(1, HANDLE_ALLOCATE, 0)
        this.activeHandle = HANDLE_ALLOCATE[0]
        updateOccupiedSize(0)
    }

    open fun setLinearTextureFilter(linear: Boolean) {
        if (this.linearTextureFilter != linear) {
            this.linearTextureFilter = linear
            prepareTextureRendering()
        }
    }

    fun prepareTextureRendering() {
        this.activeToolkit.setActiveTexture(this)
        if (this.linearTextureFilter) {
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, if (this.mipmappingEnabled) GL_LINEAR_MIPMAP_LINEAR else GL_LINEAR)
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        } else {
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, if (this.mipmappingEnabled) GL_NEAREST_MIPMAP_NEAREST else GL_NEAREST)
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        }
    }

    fun generateMipmaps(): Boolean {
        if (this.activeToolkit.intelGpu) {
            val sizeBefore = getBufferSizeRequirement()
            this.activeToolkit.setActiveTexture(this)
            OpenGL.glGenerateMipmapEXT(this.textureTarget)
            this.mipmappingEnabled = true
            prepareTextureRendering()
            updateOccupiedSize(sizeBefore)
            return true
        }
        return false
    }

    fun setMipmapping(mipmapping: Boolean) {
        if (this.mipmappingEnabled != mipmapping) {
            val sizeBefore = getBufferSizeRequirement()
            this.mipmappingEnabled = true
            prepareTextureRendering()
            updateOccupiedSize(sizeBefore)
        }
    }

    fun delete() {
        if (this.activeHandle > 0) {
            this.activeToolkit.textureBuffersSize -= getBufferSizeRequirement()
            val handleToDelete = intArrayOf(this.activeHandle)
            OpenGL.glDeleteTextures(1, handleToDelete, 0)
            this.activeHandle = 0
        }
    }

    fun updateOccupiedSize(sizeBefore: Int) {
        this.activeToolkit.textureBuffersSize -= sizeBefore
        this.activeToolkit.textureBuffersSize += getBufferSizeRequirement()
    }

    fun getBufferSizeRequirement(): Int {
        val size = this.internalFormat.bytesPerPixel * this.actionFormat.bytesPerEntry * this.pixelCount
        return if (this.mipmappingEnabled) size * 4 / 3 else size
    }

    override fun close() {
        dispose()
    }

    fun dispose() {
        if (this.activeHandle > 0) {
            this.activeToolkit.requestTextureCleanup(this.activeHandle, getBufferSizeRequirement())
            this.activeHandle = 0
        }
    }

    companion object {
        @JvmField
        val HANDLE_ALLOCATE = IntArray(1)

        @JvmStatic
        fun isPowerOfTwo(value: Int): Boolean {
            return value == (value and -value)
        }

        @JvmStatic
        fun uploadMipmappedByteTexture(target: Int, glInternalFormat: Int, width: Int, height: Int, pixelFormat: GLFormat, data: ByteArray) {
            var currentWidth = width
            var currentHeight = height
            if (currentWidth > 0 && !isPowerOfTwo(currentWidth)) {
                throw IllegalArgumentException("")
            }
            if (currentHeight > 0 && !isPowerOfTwo(currentHeight)) {
                throw IllegalArgumentException("")
            }
            val bytesPerPixel = pixelFormat.bytesPerPixel
            var mipmapLevel = 0
            var minDimension = Math.min(currentWidth, currentHeight)
            var halfWidth = currentWidth shr 1
            var halfHeight = currentHeight shr 1
            var sourceData = data
            var destData = ByteArray(halfWidth * halfHeight * bytesPerPixel)

            while (true) {
                OpenGL.glTexImage2Dub(target, mipmapLevel, glInternalFormat, currentWidth, currentHeight, 0, OpenGlToolkit.format(pixelFormat), 5121, sourceData, 0)
                if (minDimension <= 1) {
                    return
                }

                val rowBytes = bytesPerPixel * currentWidth

                for (channel in 0 until bytesPerPixel) {
                    var destIndex = channel
                    var srcTopIndex = channel
                    var srcBottomIndex = channel + rowBytes

                    for (row in 0 until halfHeight) {
                        for (col in 0 until halfWidth) {
                            val topLeftSample = sourceData[srcTopIndex]
                            srcTopIndex += bytesPerPixel
                            var sampleSum = sourceData[srcTopIndex] + topLeftSample
                            srcTopIndex += bytesPerPixel
                            sampleSum += sourceData[srcBottomIndex]
                            srcBottomIndex += bytesPerPixel
                            sampleSum += sourceData[srcBottomIndex]
                            srcBottomIndex += bytesPerPixel
                            destData[destIndex] = (sampleSum shr 2).toByte()
                            destIndex += bytesPerPixel
                        }

                        srcTopIndex += rowBytes
                        srcBottomIndex += rowBytes
                    }
                }

                val swapTemp = destData
                destData = sourceData
                sourceData = swapTemp
                currentWidth = halfWidth
                currentHeight = halfHeight
                halfWidth = halfWidth shr 1
                halfHeight = halfHeight shr 1
                minDimension = minDimension shr 1
                ++mipmapLevel
            }
        }

        @JvmStatic
        fun generateMipmaps(internalFormat: Int, width: Int, height: Int, format: GLFormat, data: FloatArray) {
            var widthMod = width
            var heightMod = height
            if (widthMod > 0 && !isPowerOfTwo(widthMod)) {
                throw IllegalArgumentException("")
            }
            if (heightMod > 0 && !isPowerOfTwo(heightMod)) {
                throw IllegalArgumentException("")
            }
            val bytesPerPixel = format.bytesPerPixel
            var level = 0
            var minDimension = Math.min(widthMod, heightMod)
            var halfWidth = widthMod shr 1
            var halfHeight = heightMod shr 1
            var destData = FloatArray(halfWidth * halfHeight * bytesPerPixel)
            var dataMod = data
            while (true) {
                OpenGL.glTexImage2Df(GL_TEXTURE_2D, level, internalFormat, widthMod, heightMod, 0, OpenGlToolkit.format(format), GL_FLOAT, dataMod, 0)
                if (minDimension <= 1) {
                    return
                }
                val rowBytes = bytesPerPixel * widthMod
                for (channel in 0 until bytesPerPixel) {
                    var destIndex = channel
                    var srcTopIndex = channel
                    var srcBottomIndex = channel + rowBytes
                    for (y in 0 until halfHeight) {
                        for (x in 0 until halfWidth) {
                            var sampleSum = dataMod[srcTopIndex]
                            srcTopIndex += bytesPerPixel
                            sampleSum += dataMod[srcTopIndex]
                            srcTopIndex += bytesPerPixel
                            sampleSum += dataMod[srcBottomIndex]
                            srcBottomIndex += bytesPerPixel
                            sampleSum += dataMod[srcBottomIndex]
                            srcBottomIndex += bytesPerPixel
                            destData[destIndex] = sampleSum * 0.25F
                            destIndex += bytesPerPixel
                        }
                        srcTopIndex += rowBytes
                        srcBottomIndex += rowBytes
                    }
                }
                val swapTemp = destData
                destData = dataMod
                dataMod = swapTemp
                widthMod = halfWidth
                heightMod = halfHeight
                halfWidth = halfWidth shr 1
                halfHeight = halfHeight shr 1
                minDimension = minDimension shr 1
                level++
            }
        }

        @JvmStatic
        fun createMipmappedCubeTexture(target: Int, glInternalFormat: Int, width: Int, height: Int, pixelType: Int, pixels: IntArray) {
            var step = width
            var currentHeight = height
            if (step > 0 && !isPowerOfTwo(step)) {
                throw IllegalArgumentException("")
            }
            if (currentHeight > 0 && !isPowerOfTwo(currentHeight)) {
                throw IllegalArgumentException("")
            }
            var mipmapLevel = 0
            var minDimension = Math.min(step, currentHeight)
            var halfWidth = step shr 1
            var halfHeight = currentHeight shr 1
            var argbPixels = pixels
            var replacementPixels = IntArray(halfWidth * halfHeight)
            while (true) {
                OpenGL.glTexImage2Di(target, mipmapLevel, glInternalFormat, step, currentHeight, 0, GL_BGRA, pixelType, argbPixels, 0)
                if (minDimension <= 1) {
                    return
                }
                var dstPixelIndex = 0
                var srcPixelIndex = 0
                var srcPixelIndexOffset = srcPixelIndex + step
                for (row in 0 until halfHeight) {
                    for (col in 0 until halfWidth) {
                        val colorArgbA = argbPixels[srcPixelIndex++]
                        val colorArgbB = argbPixels[srcPixelIndex++]
                        val colorArgbC = argbPixels[srcPixelIndexOffset++]
                        val colorArgbD = argbPixels[srcPixelIndexOffset++]
                        var red = colorArgbA shr 24 and 0xff
                        var green = colorArgbA shr 16 and 0xff
                        var blue = colorArgbA shr 8 and 0xff
                        var alpha = colorArgbA and 0xff
                        red += colorArgbB shr 24 and 0xff
                        green += colorArgbB shr 16 and 0xff
                        blue += colorArgbB shr 8 and 0xff
                        alpha += colorArgbB and 0xff
                        red += colorArgbC shr 24 and 0xff
                        green += colorArgbC shr 16 and 0xff
                        blue += colorArgbC shr 8 and 0xff
                        alpha += colorArgbC and 0xff
                        red += colorArgbD shr 24 and 0xff
                        green += colorArgbD shr 16 and 0xff
                        blue += colorArgbD shr 8 and 0xff
                        alpha += colorArgbD and 0xff
                        replacementPixels[dstPixelIndex++] = (red and 0x3fc shl 22) or (green and 0x3fc shl 14) or (blue and 0x3fc shl 6) or (alpha and 0x3fc shr 2)
                    }
                    srcPixelIndex += step
                    srcPixelIndexOffset += step
                }
                val swapTemp = replacementPixels
                replacementPixels = argbPixels
                argbPixels = swapTemp
                step = halfWidth
                currentHeight = halfHeight
                halfWidth = halfWidth shr 1
                halfHeight = halfHeight shr 1
                minDimension = minDimension shr 1
                ++mipmapLevel
            }
        }
    }
}
