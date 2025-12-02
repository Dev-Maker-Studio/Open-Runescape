package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class TextureType(stream: Packet) : CacheableNode() {

    val textureOperations: Array<TextureOperation>
    val textureFileIds: IntArray
    val materialFileIds: IntArray
    val opaqueOperation: TextureOperation
    val opacityOperation: TextureOperation
    val hdrOperation: TextureOperation

    init {
        val totalOperations = stream.readUByte()
        var textureFileCount = 0
        var materialFileCount = 0
        val operationIndices = Array(totalOperations) { IntArray(0) }
        @Suppress("UNCHECKED_CAST")
        textureOperations = arrayOfNulls<TextureOperation>(totalOperations) as Array<TextureOperation>

        for (index in 0 until totalOperations) {
            val textureOperation = TextureTypeList.listOperation(stream)
            if (textureOperation.getTextureId() >= 0) {
                textureFileCount++
            }
            if (textureOperation.getMaterialId() >= 0) {
                materialFileCount++
            }
            val operationCount = textureOperation.operations.size
            operationIndices[index] = IntArray(operationCount)
            for (sub in 0 until operationCount) {
                operationIndices[index][sub] = stream.readUByte()
            }
            textureOperations[index] = textureOperation
        }

        textureFileIds = IntArray(textureFileCount)
        textureFileCount = 0
        materialFileIds = IntArray(materialFileCount)
        materialFileCount = 0

        for (operationIndex in 0 until totalOperations) {
            val textureOperation = textureOperations[operationIndex]
            val operationCount = textureOperation.operations.size
            for (sub in 0 until operationCount) {
                textureOperation.operations[sub] = textureOperations[operationIndices[operationIndex][sub]]
            }
            val textureFileId = textureOperation.getTextureId()
            val materialFileId = textureOperation.getMaterialId()
            if (textureFileId > 0) {
                textureFileIds[textureFileCount++] = textureFileId
            }
            if (materialFileId > 0) {
                materialFileIds[materialFileCount++] = materialFileId
            }
        }

        opaqueOperation = textureOperations[stream.readUByte()]
        opacityOperation = textureOperations[stream.readUByte()]
        hdrOperation = textureOperations[stream.readUByte()]
    }

    fun getPixelsRgb(
        textureSpriteResource: Js5ResourceProvider,
        materialProvider: MaterialProvider,
        brightness: Double,
        width: Int,
        height: Int,
        readReversed: Boolean,
        writeReversed: Boolean
    ): IntArray {
        TextureTypeList.textureSpriteResource = textureSpriteResource
        TextureTypeList.materialProvider = materialProvider
        for (index in textureOperations.indices) {
            textureOperations[index].createImageCache(width, height)
        }
        TextureTypeList.setBrightness(brightness)
        TextureRaster.setDimensions(width, height)
        val texturePixels = IntArray(width * height)

        val startReadingFrom: Int
        val pixelCount: Int
        val pixelOffset: Byte
        if (readReversed) {
            startReadingFrom = width - 1
            pixelCount = -1
            pixelOffset = -1
        } else {
            startReadingFrom = 0
            pixelCount = width
            pixelOffset = 1
        }

        var writePosition = 0
        for (row in 0 until height) {
            val colorsRed: IntArray
            val colorsGreen: IntArray
            val colorsBlue: IntArray
            if (opaqueOperation.monochrome) {
                val palette = opaqueOperation.getMonochromeOutput(row)
                colorsRed = palette
                colorsGreen = palette
                colorsBlue = palette
            } else {
                val palette = opaqueOperation.getColorOutput(row)
                colorsRed = palette[0]
                colorsGreen = palette[1]
                colorsBlue = palette[2]
            }

            if (writeReversed) {
                writePosition = row
            }

            var pixelIndex = startReadingFrom
            while (pixelIndex != pixelCount) {
                var red = colorsRed[pixelIndex] shr 4
                if (red > 255) {
                    red = 255
                }
                if (red < 0) {
                    red = 0
                }
                var green = colorsGreen[pixelIndex] shr 4
                if (green > 255) {
                    green = 255
                }
                if (green < 0) {
                    green = 0
                }
                var blue = colorsBlue[pixelIndex] shr 4
                if (blue > 255) {
                    blue = 255
                }
                if (blue < 0) {
                    blue = 0
                }
                red = TextureTypeList.BRIGHTNESS_TABLE[red]
                green = TextureTypeList.BRIGHTNESS_TABLE[green]
                blue = TextureTypeList.BRIGHTNESS_TABLE[blue]
                var pixel = blue + (green shl 8) + (red shl 16)
                if (pixel != 0) {
                    pixel = pixel or 0xff000000.toInt()
                }
                texturePixels[writePosition++] = pixel
                if (writeReversed) {
                    writePosition += width - 1
                }
                pixelIndex += pixelOffset
            }
        }

        for (index in textureOperations.indices) {
            textureOperations[index].clearImageCache()
        }
        return texturePixels
    }

    fun getPixelsArgb(
        textureSprites: Js5ResourceProvider,
        materialProvider: MaterialProvider,
        rasterWidth: Int,
        rasterHeight: Int,
        writeReversed: Boolean
    ): IntArray {
        TextureTypeList.textureSpriteResource = textureSprites
        TextureTypeList.materialProvider = materialProvider
        for (index in textureOperations.indices) {
            textureOperations[index].createImageCache(rasterWidth, rasterHeight)
        }
        TextureTypeList.setBrightness(0.7)
        TextureRaster.setDimensions(rasterWidth, rasterHeight)
        val pixelColorBuffer = IntArray(rasterWidth * rasterHeight)
        var pixelIndex = 0

        for (row in 0 until rasterHeight) {
            val redBuffer: IntArray
            val greenBuffer: IntArray
            val blueBuffer: IntArray
            if (opaqueOperation.monochrome) {
                val buffer = opaqueOperation.getMonochromeOutput(row)
                redBuffer = buffer
                greenBuffer = buffer
                blueBuffer = buffer
            } else {
                val palette2d = opaqueOperation.getColorOutput(row)
                redBuffer = palette2d[0]
                greenBuffer = palette2d[1]
                blueBuffer = palette2d[2]
            }

            val opacity: IntArray = if (opacityOperation.monochrome) {
                opacityOperation.getMonochromeOutput(row)
            } else {
                opacityOperation.getColorOutput(row)[0]
            }

            if (writeReversed) {
                pixelIndex = row
            }

            for (x in rasterWidth - 1 downTo 0) {
                var red = redBuffer[x] shr 4
                if (red > 255) {
                    red = 255
                }
                if (red < 0) {
                    red = 0
                }
                var green = greenBuffer[x] shr 4
                if (green > 255) {
                    green = 255
                }
                if (green < 0) {
                    green = 0
                }
                var blue = blueBuffer[x] shr 4
                if (blue > 255) {
                    blue = 255
                }
                if (blue < 0) {
                    blue = 0
                }
                red = TextureTypeList.BRIGHTNESS_TABLE[red]
                green = TextureTypeList.BRIGHTNESS_TABLE[green]
                blue = TextureTypeList.BRIGHTNESS_TABLE[blue]

                val alpha: Int = if (red == 0 && green == 0 && blue == 0) {
                    0
                } else {
                    var a = opacity[x] shr 4
                    if (a > 255) {
                        a = 255
                    }
                    if (a < 0) {
                        a = 0
                    }
                    a
                }

                pixelColorBuffer[pixelIndex++] = blue + (green shl 8) + (alpha shl 24) + (red shl 16)
                if (writeReversed) {
                    pixelIndex += rasterWidth - 1
                }
            }
        }

        for (index in textureOperations.indices) {
            textureOperations[index].clearImageCache()
        }
        return pixelColorBuffer
    }

    fun getPixelsHdr(
        textureSpriteResource: Js5ResourceProvider,
        materialProvider: MaterialProvider,
        width: Int,
        height: Int,
        writeReversed: Boolean
    ): FloatArray {
        TextureTypeList.textureSpriteResource = textureSpriteResource
        TextureTypeList.materialProvider = materialProvider
        for (index in textureOperations.indices) {
            textureOperations[index].createImageCache(width, height)
        }
        TextureRaster.setDimensions(width, height)
        val pixelsHdr = FloatArray(width * height * 4)
        var bufferPosition = 0

        for (row in 0 until height) {
            val colorsRed: IntArray
            val colorsGreen: IntArray
            val colorsBlue: IntArray
            if (opaqueOperation.monochrome) {
                val uniform = opaqueOperation.getMonochromeOutput(row)
                colorsRed = uniform
                colorsGreen = uniform
                colorsBlue = uniform
            } else {
                val spectrum = opaqueOperation.getColorOutput(row)
                colorsRed = spectrum[0]
                colorsGreen = spectrum[1]
                colorsBlue = spectrum[2]
            }

            val opacityBuffer: IntArray = if (opacityOperation.monochrome) {
                opacityOperation.getMonochromeOutput(row)
            } else {
                opacityOperation.getColorOutput(row)[0]
            }

            val hdrBuffer: IntArray = if (hdrOperation.monochrome) {
                hdrOperation.getMonochromeOutput(row)
            } else {
                hdrOperation.getColorOutput(row)[0]
            }

            if (writeReversed) {
                bufferPosition = row shl 2
            }

            for (x in width - 1 downTo 0) {
                var opacity = opacityBuffer[x] / 4096.0f
                val intensity = (1.0f + hdrBuffer[x] * 31.0f / 4096.0f) / 4096.0f
                if (opacity < 0.0f) {
                    opacity = 0.0f
                } else if (opacity > 1.0f) {
                    opacity = 1.0f
                }
                pixelsHdr[bufferPosition++] = colorsRed[x] * intensity
                pixelsHdr[bufferPosition++] = colorsGreen[x] * intensity
                pixelsHdr[bufferPosition++] = colorsBlue[x] * intensity
                pixelsHdr[bufferPosition++] = opacity
                if (writeReversed) {
                    bufferPosition += (width shl 2) - 4
                }
            }
        }

        for (index in textureOperations.indices) {
            textureOperations[index].clearImageCache()
        }
        return pixelsHdr
    }

    fun isCached(spriteResource: Js5ResourceProvider, materialprovider: MaterialProvider): Boolean {
        if (TextureTypeList.TEXTURE_GROUP_ID >= 0) {
            for (fileIndex in textureFileIds.indices) {
                if (!spriteResource.isCached(TextureTypeList.TEXTURE_GROUP_ID, textureFileIds[fileIndex])) {
                    return false
                }
            }
        } else {
            for (fileIndex in textureFileIds.indices) {
                if (!spriteResource.isFileCached(textureFileIds[fileIndex])) {
                    return false
                }
            }
        }
        for (fileIndex in materialFileIds.indices) {
            if (!materialprovider.isCached(materialFileIds[fileIndex])) {
                return false
            }
        }
        return true
    }
}
