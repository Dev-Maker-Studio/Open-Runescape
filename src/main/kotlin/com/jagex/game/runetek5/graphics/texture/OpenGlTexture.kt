package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

abstract class OpenGlTexture(
    @JvmField val activeToolkit: OpenGlToolkit,
    @JvmField val textureTarget: Int,
    @JvmField val aClass1501650: GLFormat,
    @JvmField val aClass761651: GLActionFormat,
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
        val size = this.aClass1501650.bytesPerPixel * this.aClass761651.bytesPerEntry * this.pixelCount
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
        fun isPowerOfTwo(i_0: Int): Boolean {
            return i_0 == (i_0 and -i_0)
        }

        @JvmStatic
        fun method2359(i_0: Int, i_1: Int, i_2: Int, i_3: Int, class1504: GLFormat, bytes_5: ByteArray) {
            var i_21 = i_2
            var i_31 = i_3
            if (i_21 > 0 && !isPowerOfTwo(i_21)) {
                throw IllegalArgumentException("")
            }
            if (i_31 > 0 && !isPowerOfTwo(i_31)) {
                throw IllegalArgumentException("")
            }
            val i_6 = class1504.bytesPerPixel
            var i_7 = 0
            var i_8 = Math.min(i_21, i_31)
            var i_9 = i_21 shr 1
            var i_10 = i_31 shr 1
            var bytes_11 = bytes_5
            var bytes_12 = ByteArray(i_9 * i_10 * i_6)

            while (true) {
                OpenGL.glTexImage2Dub(i_0, i_7, i_1, i_21, i_31, 0, OpenGlToolkit.format(class1504), 5121, bytes_11, 0)
                if (i_8 <= 1) {
                    return
                }

                val i_13 = i_6 * i_21

                for (i_14 in 0 until i_6) {
                    var i_15 = i_14
                    var i_16 = i_14
                    var i_17 = i_14 + i_13

                    for (i_18 in 0 until i_10) {
                        for (i_19 in 0 until i_9) {
                            val b_20 = bytes_11[i_16]
                            i_16 += i_6
                            var i_22 = bytes_11[i_16] + b_20
                            i_16 += i_6
                            i_22 += bytes_11[i_17]
                            i_17 += i_6
                            i_22 += bytes_11[i_17]
                            i_17 += i_6
                            bytes_12[i_15] = (i_22 shr 2).toByte()
                            i_15 += i_6
                        }

                        i_16 += i_13
                        i_17 += i_13
                    }
                }

                val bytes_21 = bytes_12
                bytes_12 = bytes_11
                bytes_11 = bytes_21
                i_21 = i_9
                i_31 = i_10
                i_9 = i_9 shr 1
                i_10 = i_10 shr 1
                i_8 = i_8 shr 1
                ++i_7
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
            var floats_12 = FloatArray(halfWidth * halfHeight * bytesPerPixel)
            var dataMod = data
            while (true) {
                OpenGL.glTexImage2Df(GL_TEXTURE_2D, level, internalFormat, widthMod, heightMod, 0, OpenGlToolkit.format(format), GL_FLOAT, dataMod, 0)
                if (minDimension <= 1) {
                    return
                }
                val rowBytes = bytesPerPixel * widthMod
                for (i_14 in 0 until bytesPerPixel) {
                    var i_15 = i_14
                    var i_16 = i_14
                    var i_17 = i_14 + rowBytes
                    for (y in 0 until halfHeight) {
                        for (x in 0 until halfWidth) {
                            var f_20 = dataMod[i_16]
                            i_16 += bytesPerPixel
                            f_20 += dataMod[i_16]
                            i_16 += bytesPerPixel
                            f_20 += dataMod[i_17]
                            i_17 += bytesPerPixel
                            f_20 += dataMod[i_17]
                            i_17 += bytesPerPixel
                            floats_12[i_15] = f_20 * 0.25F
                            i_15 += bytesPerPixel
                        }
                        i_16 += rowBytes
                        i_17 += rowBytes
                    }
                }
                val floats_21 = floats_12
                floats_12 = dataMod
                dataMod = floats_21
                widthMod = halfWidth
                heightMod = halfHeight
                halfWidth = halfWidth shr 1
                halfHeight = halfHeight shr 1
                minDimension = minDimension shr 1
                level++
            }
        }

        @JvmStatic
        fun createMipmappedCubeTexture(target: Int, i_1: Int, i_2: Int, i_3: Int, i_5: Int, ints_6: IntArray) {
            var step = i_2
            var i_31 = i_3
            if (step > 0 && !isPowerOfTwo(step)) {
                throw IllegalArgumentException("")
            }
            if (i_31 > 0 && !isPowerOfTwo(i_31)) {
                throw IllegalArgumentException("")
            }
            var i_7 = 0
            var i_8 = Math.min(step, i_31)
            var i_9 = step shr 1
            var i_10 = i_31 shr 1
            var argbPixels = ints_6
            var replacementPixels = IntArray(i_9 * i_10)
            while (true) {
                OpenGL.glTexImage2Di(target, i_7, i_1, step, i_31, 0, GL_BGRA, i_5, argbPixels, 0)
                if (i_8 <= 1) {
                    return
                }
                var dstPixelIndex = 0
                var srcPixelIndex = 0
                var srcPixelIndexOffset = srcPixelIndex + step
                for (i_16 in 0 until i_10) {
                    for (i_17 in 0 until i_9) {
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
                val ints_26 = replacementPixels
                replacementPixels = argbPixels
                argbPixels = ints_26
                step = i_9
                i_31 = i_10
                i_9 = i_9 shr 1
                i_10 = i_10 shr 1
                i_8 = i_8 shr 1
                ++i_7
            }
        }
    }
}
