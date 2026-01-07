package com.jagex.game.runetek5.graphics.shadow

import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

class ShadowTileGL(
    @JvmField val toolkit: OpenGlToolkit,
    @JvmField val shadowMapper: ShadowMapperGL,
    ground: GroundGL,
    x: Int,
    y: Int,
    sizeShift: Int,
    @JvmField val tileWidth: Int,
    @JvmField val tileLength: Int
) {

    @JvmField var needsTextureRegeneration: Boolean = true
    @JvmField var lastModifiedPixelCount: Int = -1
    @JvmField val materialCount: Int
    @JvmField var cachedTexture: OpenGl2DTexture? = null
    @JvmField var elementsBuffer: ElementsBufferGL? = null
    @JvmField var aClass131Sub21567: GlElementsArrayBuffer? = null

    init {
        val size = 1 shl sizeShift
        var materialCount = 0
        val upscaledX = x shl sizeShift
        val upscaledY = y shl sizeShift

        for (px in 0 until size) {
            var i_14 = ground.width * (upscaledY + px) + upscaledX

            for (py in 0 until size) {
                val shorts_20 = ground.materialIndices[i_14++]
                if (shorts_20 != null) {
                    materialCount += shorts_20.size
                }
            }
        }

        this.materialCount = materialCount
        if (materialCount > 0) {
            val buffer = Packet(materialCount * 2)
            var materialIndices: ShortArray?
            if (toolkit.bigEndianess) {
                for (px in 0 until size) {
                    var indicePtr = ground.width * (upscaledY + px) + upscaledX

                    for (py in 0 until size) {
                        materialIndices = ground.materialIndices[indicePtr++]
                        if (materialIndices != null) {
                            for (i_18 in materialIndices.indices) {
                                buffer.writeShort(materialIndices[i_18].toInt() and 0xffff)
                            }
                        }
                    }
                }
            } else {
                for (px in 0 until size) {
                    var indicePtr = ground.width * (upscaledY + px) + upscaledX
                    for (py in 0 until size) {
                        materialIndices = ground.materialIndices[indicePtr++]
                        if (materialIndices != null) {
                            for (idx in materialIndices.indices) {
                                buffer.writeShortLittle(materialIndices[idx].toInt() and 0xffff)
                            }
                        }
                    }
                }
            }

            elementsBuffer = toolkit.createElementsArrayBuffer(buffer.buffer, buffer.currentPosition, false)
            aClass131Sub21567 = GlElementsArrayBuffer(toolkit, null, 1)
        } else {
            cachedTexture = null
        }
    }

    fun method2155() {
        method2165(elementsBuffer, materialCount)
    }

    fun method2156(bytes_1: ByteArray, i_2: Int) {
        aClass131Sub21567!!.putElements(bytes_1, i_2 * 2)
        method2165(aClass131Sub21567!!, i_2)
    }

    fun regenerateTexture() {
        if (needsTextureRegeneration) {
            needsTextureRegeneration = false
            val shadowMapperData = shadowMapper.shadowMapperData
            val shadowTextureData = toolkit.shadowTextureData
            var modifiedPixelCount = 0
            val shadowMapperWidth = shadowMapper.width
            var totalOffset = tileWidth + shadowMapper.width * tileLength

            for (x in -128 until 0) {
                modifiedPixelCount = (modifiedPixelCount shl 8) - modifiedPixelCount

                for (y in -128 until 0) {
                    if (shadowMapperData[totalOffset++] != 0.toByte()) {
                        modifiedPixelCount++
                    }
                }

                totalOffset += shadowMapperWidth - 128
            }

            if (cachedTexture != null && modifiedPixelCount == lastModifiedPixelCount) {
            } else {
                lastModifiedPixelCount = modifiedPixelCount
                var dataIndex = 0
                totalOffset = shadowMapperWidth * tileLength + tileWidth

                for (x in -128 until 0) {
                    for (y in -128 until 0) {
                        if (shadowMapperData[totalOffset] != 0.toByte()) {
                            shadowTextureData[dataIndex++] = 68
                        } else {
                            var neighborCount = 0
                            if (shadowMapperData[totalOffset - 1] != 0.toByte()) {
                                neighborCount++
                            }

                            if (shadowMapperData[totalOffset + 1] != 0.toByte()) {
                                neighborCount++
                            }

                            if (shadowMapperData[totalOffset - shadowMapperWidth] != 0.toByte()) {
                                neighborCount++
                            }

                            if (shadowMapperData[totalOffset + shadowMapperWidth] != 0.toByte()) {
                                neighborCount++
                            }

                            shadowTextureData[dataIndex++] = (neighborCount * 17).toByte()
                        }

                        totalOffset++
                    }

                    totalOffset += shadowMapper.width - 128
                }

                if (cachedTexture == null) {
                    cachedTexture = OpenGl2DTexture(
                        toolkit, 3553, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT,
                        128, 128, false, toolkit.shadowTextureData, GLFormat.RGBA, false
                    )
                    cachedTexture!!.setRepeat(false, false)
                    cachedTexture!!.setLinearTextureFilter(true)
                } else {
                    cachedTexture!!.uploadTextureData(0, 128, 128, toolkit.shadowTextureData, GLFormat.RGBA, false)
                }
            }
        }
    }

    fun method2165(interface151: ElementsBufferGL?, i_2: Int) {
        if (i_2 != 0) {
            regenerateTexture()
            toolkit.setActiveTexture(cachedTexture)
            toolkit.drawElements(interface151!!, 0, i_2)
        }
    }
}
