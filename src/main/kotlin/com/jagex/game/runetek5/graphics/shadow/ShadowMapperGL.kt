package com.jagex.game.runetek5.graphics.shadow

import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import jaggl.OpenGL

class ShadowMapperGL(
    val toolkit: OpenGlToolkit,
    val ground: GroundGL
) {

    val width: Int
    val length: Int
    val shadowMapperData: ByteArray
    /** Bumped whenever the intensity map changes, so the modern texture re-uploads. */
    @JvmField var shadowVersion: Int = 0
    val sizeShift: Int
    val sizeX: Int
    val sizeY: Int
    var shadowTiles: Array<Array<ShadowTileGL?>>? = null

    init {
        width = (ground.width * ground.tileUnits shr toolkit.shadowScale) + 2
        length = (ground.length * ground.tileUnits shr toolkit.shadowScale) + 2
        shadowMapperData = ByteArray(width * length)
        sizeShift = toolkit.shadowScale + 7 - ground.tileScale
        sizeX = ground.width shr sizeShift
        sizeY = ground.length shr sizeShift
    }

    fun renderShadowOverlay(cameraGridX: Int, cameraGridY: Int, visibleRange: Int, tilesOnScreen: Array<BooleanArray>, belowLowestPlane: Boolean) {
        toolkit.setLightingEnabled(false)
        toolkit.setDepthMask(false)
        toolkit.renderTexture(-2)
        toolkit.loadTextureCombineMode(1)
        toolkit.setBlendMode(1)
        val texCoordScale = 1.0f / (toolkit.openglFlags * 128)
        var chunkY: Int
        var chunkStartY: Int
        var chunkEndY: Int
        var chunkX: Int
        var chunkStartX: Int
        var chunkEndX: Int
        var indexCount: Int
        if (belowLowestPlane) {
            chunkY = 0
            while (chunkY < sizeY) {
                chunkStartY = chunkY shl sizeShift
                chunkEndY = (chunkY + 1) shl sizeShift

                label136@ for (chunkX in 0 until sizeX) {
                    chunkStartX = chunkX shl sizeShift
                    chunkEndX = (chunkX + 1) shl sizeShift

                    for (tileX in chunkStartX until chunkEndX) {
                        if (tileX - cameraGridX >= -visibleRange && tileX - cameraGridX <= visibleRange) {
                            for (tileY in chunkStartY until chunkEndY) {
                                if (tileY - cameraGridY >= -visibleRange && tileY - cameraGridY <= visibleRange && tilesOnScreen[visibleRange + (tileX - cameraGridX)][tileY - cameraGridY + visibleRange]) {
                                    OpenGL.glMatrixMode(5890)
                                    OpenGL.glLoadIdentity()
                                    OpenGL.glScalef(texCoordScale, texCoordScale, 1.0f)
                                    OpenGL.glTranslatef((-chunkX) / texCoordScale, (-chunkY) / texCoordScale, 1.0f)
                                    OpenGL.glMatrixMode(5888)
                                    shadowTiles!![chunkX][chunkY]!!.drawShadow()
                                    continue@label136
                                }
                            }
                        }
                    }
                }
                chunkY++
            }
        } else {
            chunkY = 0
            while (chunkY < sizeY) {
                chunkStartY = chunkY shl sizeShift
                chunkEndY = (chunkY + 1) shl sizeShift

                for (chunkX in 0 until sizeX) {
                    indexCount = 0
                    chunkStartX = chunkX shl sizeShift
                    chunkEndX = (chunkX + 1) shl sizeShift
                    val textureBuffer = toolkit.textureBuffer
                    textureBuffer.currentPosition = 0

                    for (tileY in chunkStartY until chunkEndY) {
                        if (tileY - cameraGridY >= -visibleRange && tileY - cameraGridY <= visibleRange) {
                            var materialOffset = chunkStartX + ground.width * tileY

                            for (tileX in chunkStartX until chunkEndX) {
                                if (tileX - cameraGridX >= -visibleRange && tileX - cameraGridX <= visibleRange && tilesOnScreen[visibleRange + (tileX - cameraGridX)][tileY - cameraGridY + visibleRange]) {
                                    val materialIndices = ground.materialIndices[materialOffset]
                                    if (materialIndices != null) {
                                        var matIdx: Int
                                        if (toolkit.bigEndianess) {
                                            matIdx = 0
                                            while (matIdx < materialIndices.size) {
                                                textureBuffer.writeShort(materialIndices[matIdx].toInt() and 0xffff)
                                                ++indexCount
                                                matIdx++
                                            }
                                        } else {
                                            matIdx = 0
                                            while (matIdx < materialIndices.size) {
                                                textureBuffer.writeShortLittle(materialIndices[matIdx].toInt() and 0xffff)
                                                ++indexCount
                                                matIdx++
                                            }
                                        }
                                    }
                                }

                                ++materialOffset
                            }
                        }
                    }

                    if (indexCount > 0) {
                        OpenGL.glMatrixMode(5890)
                        OpenGL.glLoadIdentity()
                        OpenGL.glScalef(texCoordScale, texCoordScale, 1.0f)
                        OpenGL.glTranslatef((-chunkX) / texCoordScale, (-chunkY) / texCoordScale, 1.0f)
                        OpenGL.glMatrixMode(5888)
                        shadowTiles!![chunkX][chunkY]!!.drawShadowWithIndices(textureBuffer.buffer, indexCount)
                    }
                }
                chunkY++
            }
        }

        OpenGL.glMatrixMode(5890) //GL_TEXTURE
        OpenGL.glLoadIdentity()
        OpenGL.glMatrixMode(5888) //GL_MODELVIEW
    }

    fun applyShadow(shadow: Shadow, shadowX: Int, shadowY: Int): Boolean {
        var adjustedY = shadowY
        var adjustedX = shadowX
        val shadowGL = shadow as ShadowGL
        adjustedX += shadowGL.startX + 1
        adjustedY += shadowGL.startY + 1
        var dataOffset = adjustedX + adjustedY * width
        var clippedHeight = shadowGL.sizeY
        var clippedWidth = shadowGL.sizeX
        var rowStride = width - clippedWidth
        var clippedAmount: Int
        if (adjustedY <= 0) {
            clippedAmount = 1 - adjustedY
            clippedHeight -= clippedAmount
            dataOffset += clippedAmount * width
            adjustedY = 1
        }

        if (adjustedY + clippedHeight >= length) {
            clippedAmount = adjustedY + clippedHeight + 1 - length
            clippedHeight -= clippedAmount
        }

        if (adjustedX <= 0) {
            clippedAmount = 1 - adjustedX
            clippedWidth -= clippedAmount
            dataOffset += clippedAmount
            rowStride += clippedAmount
            adjustedX = 1
        }

        if (clippedWidth + adjustedX >= width) {
            clippedAmount = clippedWidth + adjustedX + 1 - width
            clippedWidth -= clippedAmount
            rowStride += clippedAmount
        }

        if (clippedWidth > 0 && clippedHeight > 0) {
            val sampleStep: Byte = 8
            rowStride += (sampleStep - 1) * width
            return hasShadowData(shadowMapperData, dataOffset, clippedWidth, clippedHeight, rowStride, sampleStep.toInt())
        } else {
            return false
        }
    }

    fun invalidateShadowTiles(startX: Int, startY: Int, regionWidth: Int, regionHeight: Int) {
        if (shadowTiles != null) {
            val minChunkX = (startX - 1) shr 7
            val maxChunkX = (regionWidth + (startX - 1) - 1) shr 7
            val minChunkY = (startY - 1) shr 7
            val maxChunkY = (startY - 1 + regionHeight - 1) shr 7

            for (chunkX in minChunkX..maxChunkX) {
                val chunkColumn = shadowTiles!![chunkX]

                for (chunkY in minChunkY..maxChunkY) {
                    chunkColumn[chunkY]!!.needsTextureRegeneration = true
                }
            }
        }
    }

    fun initializeShadowTiles() {
        shadowTiles = Array(sizeX) { arrayOfNulls(sizeY) }

        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                shadowTiles!![x][y] = ShadowTileGL(toolkit, this, ground, x, y, sizeShift, x * 128 + 1, y * 128 + 1)
            }
        }
    }

    fun addShadow(shadow: Shadow, shadowX: Int, shadowY: Int) {
        var adjustedY = shadowY
        var adjustedX = shadowX
        val shadowGL = shadow as ShadowGL
        adjustedX += shadowGL.startX + 1
        adjustedY += shadowGL.startY + 1
        var dataOffset = adjustedX + adjustedY * width
        var srcOffset = 0
        var clippedHeight = shadowGL.sizeY
        var clippedWidth = shadowGL.sizeX
        var dstRowStride = width - clippedWidth
        var srcRowStride = 0
        var clippedAmount: Int
        if (adjustedY <= 0) {
            clippedAmount = 1 - adjustedY
            clippedHeight -= clippedAmount
            srcOffset += clippedAmount * clippedWidth
            dataOffset += clippedAmount * width
            adjustedY = 1
        }

        if (adjustedY + clippedHeight >= length) {
            clippedAmount = adjustedY + clippedHeight + 1 - length
            clippedHeight -= clippedAmount
        }

        if (adjustedX <= 0) {
            clippedAmount = 1 - adjustedX
            clippedWidth -= clippedAmount
            srcOffset += clippedAmount
            dataOffset += clippedAmount
            srcRowStride += clippedAmount
            dstRowStride += clippedAmount
            adjustedX = 1
        }

        if (clippedWidth + adjustedX >= width) {
            clippedAmount = clippedWidth + adjustedX + 1 - width
            clippedWidth -= clippedAmount
            srcRowStride += clippedAmount
            dstRowStride += clippedAmount
        }

        if (clippedWidth > 0 && clippedHeight > 0) {
            shadowVersion++
            addShadowData(shadowMapperData, shadowGL.shade, srcOffset, dataOffset, clippedWidth, clippedHeight, dstRowStride, srcRowStride)
            invalidateShadowTiles(adjustedX, adjustedY, clippedWidth, clippedHeight)
        }
    }

    fun removeShadow(shadow: Shadow, shadowX: Int, shadowY: Int) {
        var adjustedY = shadowY
        var adjustedX = shadowX
        val shadowGL = shadow as ShadowGL
        adjustedX += shadowGL.startX + 1
        adjustedY += shadowGL.startY + 1
        var dataOffset = adjustedX + adjustedY * width
        var srcOffset = 0
        var clippedHeight = shadowGL.sizeY
        var clippedWidth = shadowGL.sizeX
        var dstRowStride = width - clippedWidth
        var srcRowStride = 0
        var clippedAmount: Int
        if (adjustedY <= 0) {
            clippedAmount = 1 - adjustedY
            clippedHeight -= clippedAmount
            srcOffset += clippedAmount * clippedWidth
            dataOffset += clippedAmount * width
            adjustedY = 1
        }

        if (adjustedY + clippedHeight >= length) {
            clippedAmount = adjustedY + clippedHeight + 1 - length
            clippedHeight -= clippedAmount
        }

        if (adjustedX <= 0) {
            clippedAmount = 1 - adjustedX
            clippedWidth -= clippedAmount
            srcOffset += clippedAmount
            dataOffset += clippedAmount
            srcRowStride += clippedAmount
            dstRowStride += clippedAmount
            adjustedX = 1
        }

        if (clippedWidth + adjustedX >= width) {
            clippedAmount = clippedWidth + adjustedX + 1 - width
            clippedWidth -= clippedAmount
            srcRowStride += clippedAmount
            dstRowStride += clippedAmount
        }

        if (clippedWidth > 0 && clippedHeight > 0) {
            shadowVersion++
            subtractShadowData(shadowMapperData, shadowGL.shade, srcOffset, dataOffset, clippedWidth, clippedHeight, dstRowStride, srcRowStride)
            invalidateShadowTiles(adjustedX, adjustedY, clippedWidth, clippedHeight)
        }
    }

    companion object {
        @JvmStatic
        fun subtractShadowData(dstData: ByteArray, srcData: ByteArray, srcOffset: Int, dstOffset: Int, width: Int, height: Int, dstRowStride: Int, srcRowStride: Int) {
            var dstPos = dstOffset
            var srcPos = srcOffset
            val unrolledCount = -(width shr 2)
            val remainderCount = -(width and 0x3)

            for (row in -height until 0) {
                var col = unrolledCount
                while (col < 0) {
                    val idx0 = dstPos++
                    dstData[idx0] = (dstData[idx0] + srcData[srcPos++]).toByte()
                    val idx1 = dstPos++
                    dstData[idx1] = (dstData[idx1] + srcData[srcPos++]).toByte()
                    val idx2 = dstPos++
                    dstData[idx2] = (dstData[idx2] + srcData[srcPos++]).toByte()
                    val idx3 = dstPos++
                    dstData[idx3] = (dstData[idx3] + srcData[srcPos++]).toByte()
                    col++
                }

                col = remainderCount
                while (col < 0) {
                    val idx = dstPos++
                    dstData[idx] = (dstData[idx] + srcData[srcPos++]).toByte()
                    col++
                }

                dstPos += dstRowStride
                srcPos += srcRowStride
            }
        }

        @JvmStatic
        fun addShadowData(dst: ByteArray, shade: ByteArray, srcOffset: Int, dstOffset: Int, width: Int, height: Int, dstRowStride: Int, srcRowStride: Int) {
            var dstPos = dstOffset
            var srcPos = srcOffset
            val unrolledCount = -(width shr 2)
            val remainderCount = -(width and 0x3)

            for (row in -height until 0) {
                var col = unrolledCount
                while (col < 0) {
                    val idx0 = dstPos++
                    dst[idx0] = (dst[idx0] - shade[srcPos++]).toByte()
                    val idx1 = dstPos++
                    dst[idx1] = (dst[idx1] - shade[srcPos++]).toByte()
                    val idx2 = dstPos++
                    dst[idx2] = (dst[idx2] - shade[srcPos++]).toByte()
                    val idx3 = dstPos++
                    dst[idx3] = (dst[idx3] - shade[srcPos++]).toByte()
                    col++
                }

                col = remainderCount
                while (col < 0) {
                    val idx = dstPos++
                    dst[idx] = (dst[idx] - shade[srcPos++]).toByte()
                    col++
                }

                dstPos += dstRowStride
                srcPos += srcRowStride
            }
        }

        @JvmStatic
        fun hasShadowData(shadowData: ByteArray, startPos: Int, width: Int, height: Int, rowStride: Int, sampleStep: Int): Boolean {
            var pos = startPos
            val widthRemainder = width % sampleStep
            val widthAdjust = if (widthRemainder != 0) {
                sampleStep - widthRemainder
            } else {
                0
            }

            val rowCount = -((height + sampleStep - 1) / sampleStep)
            val colCount = -((sampleStep + width - 1) / sampleStep)

            for (row in rowCount until 0) {
                for (col in colCount until 0) {
                    if (shadowData[pos] == 0.toByte()) {
                        return true
                    }

                    pos += sampleStep
                }

                pos -= widthAdjust
                if (shadowData[pos - 1] == 0.toByte()) {
                    return true
                }

                pos += rowStride
            }

            return false
        }
    }
}
