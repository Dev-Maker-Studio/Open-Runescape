package com.jagex.game.runetek5.graphics.texture.water

import com.jagex.game.runetek5.buffer.DataBuffer
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.Texture3DSampler
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import kotlin.math.sqrt

class NativeWaterTexture(val activeToolkit: AbstractNativeToolkit) {

    var normalSampler: Texture3DSampler? = null
    var textureSamplers: Array<NativeTextureSampler?>? = null
    var billowSampler: Texture3DSampler? = null
    var hasDepth: Boolean = false
    var waterTextureSamplers: Array<NativeTextureSampler?>? = null
    var waterSampler3d: Texture3DSampler? = null

    init {
        hasDepth = activeToolkit.SUPPORTS_TEXTURE3D
        if (hasDepth && !activeToolkit.method13955(GLFormat.aClass1501952, GLActionFormat.PIXEL_FORMAT)) {
            hasDepth = false
        }

        if (hasDepth || activeToolkit.method13954(GLFormat.aClass1501952, GLActionFormat.PIXEL_FORMAT)) {
            initializeWaterData()
            if (!hasDepth) {
                waterTextureSamplers = arrayOfNulls(16)

                for (index in 0 until 16) {
                    val data = DataBuffer.readDataBuffer(anObject482, index * 32768)
                    waterTextureSamplers!![index] = activeToolkit.createTextureSampler(
                        GLFormat.aClass1501952,
                        128,
                        128,
                        true,
                        data
                    )
                }

                textureSamplers = arrayOfNulls(16)

                for (index in 0 until 16) {
                    val data = DataBuffer.readDataBuffer(anObject477, index * 32768)
                    textureSamplers!![index] = activeToolkit.createTextureSampler(
                        GLFormat.aClass1501952,
                        128,
                        128,
                        true,
                        data
                    )
                }
            } else {
                var bytes = DataBuffer.unpackDataBuffer(anObject482, false)
                waterSampler3d = activeToolkit.method13962(GLFormat.aClass1501952, bytes!!)
                bytes = DataBuffer.unpackDataBuffer(anObject477, false)
                billowSampler = activeToolkit.method13962(GLFormat.aClass1501952, bytes!!)
            }
        }
    }

    fun loadSampledTexture(): Boolean {
        if (!activeToolkit.SUPPORTS_TEXTURE3D) {
            return false
        }

        if (normalSampler == null) {
            val sourceBytes: ByteArray
            if (hdrDatabuffer == null) {
                sourceBytes = PerlinWaterTextureGenerator.generatePerlinWaterTexture(PerlinNoiseGenerator())
                hdrDatabuffer = DataBuffer.createDataBuffer(sourceBytes)
            }

            val bytes1 = DataBuffer.unpackDataBuffer(hdrDatabuffer!!, false)!!
            val bytes2 = ByteArray(bytes1.size * 4)
            var index = 0

            for (depth in 0 until 16) {
                val depthOffset = depth * 16384
                var sourceIndex = depthOffset

                for (y in 0 until 128) {
                    val currentRow = depthOffset + y * 128
                    val prevRow = depthOffset + ((y - 1) and 0x7f) * 128
                    val nextRow = depthOffset + ((y + 1) and 0x7f) * 128

                    for (x in 0 until 128) {
                        val verticalGradient = (bytes1[prevRow + x].toInt() and 0xff) - (bytes1[nextRow + x].toInt() and 0xff)
                        val horizontalGradient = (bytes1[currentRow + ((x - 1) and 0x7f)].toInt() and 0xff) -
                                                 (bytes1[currentRow + ((x + 1) and 0x7f)].toInt() and 0xff)
                        val normalScale = (128.0 / sqrt(
                            horizontalGradient * horizontalGradient + 16384.0 + verticalGradient * verticalGradient
                        )).toFloat()

                        bytes2[index++] = (horizontalGradient * normalScale + 127.0f).toInt().toByte()
                        bytes2[index++] = (128.0f * normalScale + 127.0f).toInt().toByte()
                        bytes2[index++] = (verticalGradient * normalScale + 127.0f).toInt().toByte()
                        bytes2[index++] = bytes1[sourceIndex++]
                    }
                }
            }

            normalSampler = activeToolkit.method13962(GLFormat.HDR, bytes2)
        }

        return normalSampler != null
    }

    fun readyToSample(): Boolean {
        return if (hasDepth) waterSampler3d != null else waterTextureSamplers != null
    }

    companion object {
        @JvmStatic
        var anObject482: Any? = null

        @JvmStatic
        var anObject477: Any? = null

        @JvmStatic
        var hdrDatabuffer: Any? = null

        @JvmStatic
        fun initializeWaterData() {
            if (anObject482 == null) {
                val generator1 = Class370_Sub1_Sub1()
                val data = generator1.getData()
                anObject482 = DataBuffer.createDataBuffer(data)
            }

            if (anObject477 == null) {
                val generator2 = Class370_Sub2_Sub1()
                val data = generator2.getData()
                anObject477 = DataBuffer.createDataBuffer(data)
            }
        }
    }
}
