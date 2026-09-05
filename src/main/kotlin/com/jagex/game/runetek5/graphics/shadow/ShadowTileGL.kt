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
    chunkX: Int,
    chunkY: Int,
    sizeShift: Int,
    @JvmField val tileWidth: Int,
    @JvmField val tileLength: Int
) {

    @JvmField var needsTextureRegeneration: Boolean = true
    @JvmField var lastModifiedPixelCount: Int = -1
    @JvmField val materialCount: Int
    @JvmField var cachedTexture: OpenGl2DTexture? = null
    @JvmField var elementsBuffer: ElementsBufferGL? = null
    @JvmField var indexBuffer: GlElementsArrayBuffer? = null

    init {
        val chunkSize = 1 shl sizeShift
        var count = 0
        val chunkStartX = chunkX shl sizeShift
        val chunkStartY = chunkY shl sizeShift

        for (dy in 0 until chunkSize) {
            var materialOffset = ground.width * (chunkStartY + dy) + chunkStartX

            for (dx in 0 until chunkSize) {
                val materialIndices = ground.materialIndices[materialOffset++]
                if (materialIndices != null) {
                    count += materialIndices.size
                }
            }
        }

        this.materialCount = count
        if (count > 0) {
            val buffer = Packet(count * 2)
            var materialIndices: ShortArray?
            if (toolkit.bigEndianess) {
                for (dy in 0 until chunkSize) {
                    var materialOffset = ground.width * (chunkStartY + dy) + chunkStartX

                    for (dx in 0 until chunkSize) {
                        materialIndices = ground.materialIndices[materialOffset++]
                        if (materialIndices != null) {
                            for (matIdx in materialIndices.indices) {
                                buffer.writeShort(materialIndices[matIdx].toInt() and 0xffff)
                            }
                        }
                    }
                }
            } else {
                for (dy in 0 until chunkSize) {
                    var materialOffset = ground.width * (chunkStartY + dy) + chunkStartX
                    for (dx in 0 until chunkSize) {
                        materialIndices = ground.materialIndices[materialOffset++]
                        if (materialIndices != null) {
                            for (matIdx in materialIndices.indices) {
                                buffer.writeShortLittle(materialIndices[matIdx].toInt() and 0xffff)
                            }
                        }
                    }
                }
            }

            elementsBuffer = toolkit.createElementsArrayBuffer(buffer.buffer, buffer.currentPosition, false)
            indexBuffer = GlElementsArrayBuffer(toolkit, null, 1)
        } else {
            cachedTexture = null
        }
    }

    fun drawShadow() {
        drawShadowElements(elementsBuffer, materialCount)
    }

    fun drawShadowWithIndices(indexData: ByteArray, indexCount: Int) {
        indexBuffer!!.putElements(indexData, indexCount * 2)
        drawShadowElements(indexBuffer!!, indexCount)
    }

    fun regenerateTexture() {
        if (needsTextureRegeneration) {
            needsTextureRegeneration = false
            val shadowMapperData = shadowMapper.shadowMapperData
            val shadowTextureData = toolkit.shadowTextureData
            var modifiedPixelCount = 0
            val shadowMapperWidth = shadowMapper.width
            var shadowOffset = tileWidth + shadowMapper.width * tileLength

            for (y in -128 until 0) {
                modifiedPixelCount = (modifiedPixelCount shl 8) - modifiedPixelCount

                for (x in -128 until 0) {
                    if (shadowMapperData[shadowOffset++] != 0.toByte()) {
                        modifiedPixelCount++
                    }
                }

                shadowOffset += shadowMapperWidth - 128
            }

            if (cachedTexture != null && modifiedPixelCount == lastModifiedPixelCount) {
            } else {
                lastModifiedPixelCount = modifiedPixelCount
                var texelIndex = 0
                shadowOffset = shadowMapperWidth * tileLength + tileWidth

                for (y in -128 until 0) {
                    for (x in -128 until 0) {
                        if (shadowMapperData[shadowOffset] != 0.toByte()) {
                            shadowTextureData[texelIndex++] = 68
                        } else {
                            var neighborCount = 0
                            if (shadowMapperData[shadowOffset - 1] != 0.toByte()) {
                                neighborCount++
                            }

                            if (shadowMapperData[shadowOffset + 1] != 0.toByte()) {
                                neighborCount++
                            }

                            if (shadowMapperData[shadowOffset - shadowMapperWidth] != 0.toByte()) {
                                neighborCount++
                            }

                            if (shadowMapperData[shadowOffset + shadowMapperWidth] != 0.toByte()) {
                                neighborCount++
                            }

                            shadowTextureData[texelIndex++] = (neighborCount * 17).toByte()
                        }

                        shadowOffset++
                    }

                    shadowOffset += shadowMapper.width - 128
                }

                if (cachedTexture == null) {
                    cachedTexture = OpenGl2DTexture(
                        toolkit, 3553, GLFormat.ALPHA, GLActionFormat.PIXEL_FORMAT,
                        128, 128, false, toolkit.shadowTextureData, GLFormat.ALPHA, false
                    )
                    cachedTexture!!.setRepeat(false, false)
                    cachedTexture!!.setLinearTextureFilter(true)
                } else {
                    cachedTexture!!.uploadTextureData(0, 128, 128, toolkit.shadowTextureData, GLFormat.ALPHA, false)
                }
            }
        }
    }

    fun drawShadowElements(elements: ElementsBufferGL?, indexCount: Int) {
        if (indexCount != 0) {
            regenerateTexture()
            toolkit.setActiveTexture(cachedTexture)
            toolkit.drawElements(elements!!, 0, indexCount)
        }
    }
}
