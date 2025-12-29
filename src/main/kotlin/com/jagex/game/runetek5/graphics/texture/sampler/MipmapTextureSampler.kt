package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import jaggl.OpenGL

abstract class MipmapTextureSampler(
    val activeToolkit: OpenGLNativeToolkit,
    @JvmField val textureTarget: Int,
    val pixelFormat: GLFormat,
    val pixelActionFormat: GLActionFormat,
    val pixelCount: Int,
    var mipmapping: Boolean
) : TextureSampler {
    @JvmField
    var texture: Int = 0
    var sampleMode: SampleMode = SampleMode.LINEAR

    init {
        OpenGL.glGenTextures(1, textures, 0)
        texture = textures[0]
        initializeSampler()
        updateTexturesSizeSum(0)
    }

    fun getSize(): Int {
        val size = pixelFormat.bytesPerPixel * pixelActionFormat.bytesPerEntry * pixelCount
        return if (mipmapping) size * 4 / 3 else size
    }

    fun updateTexturesSizeSum(size: Int) {
        activeToolkit.texturesSizeOccupied -= size
        activeToolkit.texturesSizeOccupied += getSize()
    }

    override fun deallocate() {
        if (texture > 0) {
            activeToolkit.requestTextureDeletion(texture, getSize())
            texture = 0
        }
    }

    fun initializeSampler() {
        activeToolkit.setTextureSampler(this)
        if (sampleMode == SampleMode.LINEAR) {
            OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, if (mipmapping) GL_LINEAR_MIPMAP_LINEAR else GL_LINEAR)
            OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        } else {
            OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, if (mipmapping) GL_NEAREST_MIPMAP_NEAREST else GL_NEAREST)
            OpenGL.glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        }
    }

    open fun enableMipmapping(): Boolean {
        val size = getSize()
        activeToolkit.setTextureSampler(this)
        OpenGL.glGenerateMipmapEXT(textureTarget)
        mipmapping = true
        initializeSampler()
        updateTexturesSizeSum(size)
        return true
    }

    @Suppress("removal")
    @Deprecated("Deprecated in Java")
    protected fun finalize() {
        deallocate()
    }

    override fun bindTexture() {
        val slot = activeToolkit.getCurrentTextureSlot()
        val type = activeToolkit.textureSlots!![slot]
        if (type != textureTarget) {
            if (type != 0) {
                OpenGL.glBindTexture(type, 0)
                OpenGL.glDisable(type)
            }

            OpenGL.glEnable(textureTarget)
            activeToolkit.textureSlots!![slot] = textureTarget
        }

        OpenGL.glBindTexture(textureTarget, texture)
    }

    override fun setSamplerMode(sampleMode: SampleMode) {
        if (this.sampleMode != sampleMode) {
            this.sampleMode = sampleMode
            initializeSampler()
        }
    }

    fun generateMipmaps(i1: Int, i2: Int, i3: Int, floats4: FloatArray) {
        var i21 = i2
        var i31 = i3
        if (i21 > 0 && !OpenGlTexture.isPowerOfTwo(i21)) {
            throw IllegalArgumentException("")
        }
        if (i31 > 0 && !OpenGlTexture.isPowerOfTwo(i31)) {
            throw IllegalArgumentException("")
        }
        val i5 = pixelFormat.bytesPerPixel
        var i6 = 0
        var i7 = Math.min(i21, i31)
        var i8 = i21 shr 1
        var i9 = i31 shr 1
        var floats10 = floats4
        var floats11 = FloatArray(i8 * i9 * i5)

        while (true) {
            OpenGL.glTexImage2Df(
                i1,
                i6,
                OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
                i21,
                i31,
                0,
                OpenGLNativeToolkit.texture2dFormat(pixelFormat),
                5126,
                floats10,
                0
            )
            if (i7 <= 1) {
                return
            }

            val i12 = i5 * i21

            for (i13 in 0 until i5) {
                var i14 = i13
                var i15 = i13
                var i16 = i13 + i12

                for (i17 in 0 until i9) {
                    for (i18 in 0 until i8) {
                        var f19 = floats10[i15]
                        i15 += i5
                        f19 += floats10[i15]
                        i15 += i5
                        f19 += floats10[i16]
                        i16 += i5
                        f19 += floats10[i16]
                        i16 += i5
                        floats11[i14] = f19 * 0.25f
                        i14 += i5
                    }

                    i15 += i12
                    i16 += i12
                }
            }

            val floats20 = floats11
            floats11 = floats10
            floats10 = floats20
            i21 = i8
            i31 = i9
            i8 = i8 shr 1
            i9 = i9 shr 1
            i7 = i7 shr 1
            ++i6
        }
    }

    fun generateMipmaps(textureType: Int, width: Int, height: Int, pixels: ByteArray) {
        var currentWidth = width
        var currentHeight = height
        if (currentWidth > 0 && !OpenGlTexture.isPowerOfTwo(currentWidth)) {
            throw IllegalArgumentException("")
        }
        if (currentHeight > 0 && !OpenGlTexture.isPowerOfTwo(currentHeight)) {
            throw IllegalArgumentException("")
        }
        val pixelByteCount = pixelFormat.bytesPerPixel
        var i6 = 0
        var shortestDimension = Math.min(currentWidth, currentHeight)
        var centerX = currentWidth shr 1
        var centerY = currentHeight shr 1
        var bytes10 = pixels
        var bytes11 = ByteArray(centerX * centerY * pixelByteCount)

        while (true) {
            OpenGL.glTexImage2Dub(
                textureType,
                i6,
                OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
                currentWidth,
                currentHeight,
                0,
                OpenGLNativeToolkit.texture2dFormat(pixelFormat),
                GL_UNSIGNED_BYTE,
                bytes10,
                0
            )
            if (shortestDimension <= 1) {
                return
            }

            val i12 = pixelByteCount * currentWidth

            for (i13 in 0 until pixelByteCount) {
                var i14 = i13
                var i15 = i13
                var i16 = i13 + i12

                for (i17 in 0 until centerY) {
                    for (i18 in 0 until centerX) {
                        val b19 = bytes10[i15]
                        i15 += pixelByteCount
                        var i21 = bytes10[i15] + b19
                        i15 += pixelByteCount
                        i21 += bytes10[i16]
                        i16 += pixelByteCount
                        i21 += bytes10[i16]
                        i16 += pixelByteCount
                        bytes11[i14] = (i21 shr 2).toByte()
                        i14 += pixelByteCount
                    }

                    i15 += i12
                    i16 += i12
                }
            }

            val bytes20 = bytes11
            bytes11 = bytes10
            bytes10 = bytes20
            currentWidth = centerX
            currentHeight = centerY
            centerX = centerX shr 1
            centerY = centerY shr 1
            shortestDimension = shortestDimension shr 1
            ++i6
        }
    }

    fun generateMipmaps(textureTarget: Int, width: Int, height: Int, pixels: IntArray) {
        var currentWidth = width
        var currentHeight = height
        if (currentWidth > 0 && !OpenGlTexture.isPowerOfTwo(currentWidth)) {
            throw IllegalArgumentException("")
        }
        if (currentHeight > 0 && !OpenGlTexture.isPowerOfTwo(currentHeight)) {
            throw IllegalArgumentException("")
        }
        if (pixelFormat != GLFormat.HDR) {
            throw IllegalArgumentException("")
        }
        var i5 = 0
        var i6 = Math.min(currentWidth, currentHeight)
        var i7 = currentWidth shr 1
        var i8 = currentHeight shr 1
        var ints9 = pixels
        var ints10 = IntArray(i7 * i8)

        while (true) {
            OpenGL.glTexImage2Di(
                textureTarget,
                i5,
                OpenGLNativeToolkit.texture2dInternalFormat(pixelFormat, pixelActionFormat),
                currentWidth,
                currentHeight,
                0,
                32993,
                activeToolkit.pixelType,
                ints9,
                0
            )
            if (i6 <= 1) {
                return
            }

            var i11 = 0
            var i12 = 0
            var i13 = i12 + currentWidth

            for (i14 in 0 until i8) {
                for (i15 in 0 until i7) {
                    val i16 = ints9[i12++]
                    val i17 = ints9[i12++]
                    val i18 = ints9[i13++]
                    val i19 = ints9[i13++]
                    var i20 = i16 shr 24 and 0xff
                    var i21 = i16 shr 16 and 0xff
                    var i22 = i16 shr 8 and 0xff
                    var i23 = i16 and 0xff
                    i20 += i17 shr 24 and 0xff
                    i21 += i17 shr 16 and 0xff
                    i22 += i17 shr 8 and 0xff
                    i23 += i17 and 0xff
                    i20 += i18 shr 24 and 0xff
                    i21 += i18 shr 16 and 0xff
                    i22 += i18 shr 8 and 0xff
                    i23 += i18 and 0xff
                    i20 += i19 shr 24 and 0xff
                    i21 += i19 shr 16 and 0xff
                    i22 += i19 shr 8 and 0xff
                    i23 += i19 and 0xff
                    ints10[i11++] = (i20 and 0x3fc shl 22) or (i21 and 0x3fc shl 14) or (i22 and 0x3fc shl 6) or (i23 and 0x3fc shr 2)
                }

                i12 += currentWidth
                i13 += currentWidth
            }

            val ints24 = ints10
            ints10 = ints9
            ints9 = ints24
            currentWidth = i7
            currentHeight = i8
            i7 = i7 shr 1
            i8 = i8 shr 1
            i6 = i6 shr 1
            ++i5
        }
    }

    companion object {
        @JvmField
        val textures = IntArray(1)
    }
}
