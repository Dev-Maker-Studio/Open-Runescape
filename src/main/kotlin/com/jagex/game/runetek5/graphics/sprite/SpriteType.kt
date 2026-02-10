package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.utils.ArrayUtils

class SpriteType {
    @JvmField
    var minX: Int = 0
    @JvmField
    var minY: Int = 0
    @JvmField
    var width: Int = 0
    @JvmField
    var height: Int = 0
    @JvmField
    var borderX: Int = 0
    @JvmField
    var borderY: Int = 0
    @JvmField
    var pallete: IntArray = IntArray(0)
    @JvmField
    var pixels: ByteArray = ByteArray(0)
    @JvmField
    var alpha: ByteArray? = null

    fun widthSum(): Int {
        return width + minX + borderX
    }

    fun heightSum(): Int {
        return height + minY + borderY
    }

    fun expandToFullSize() {
        val w = widthSum()
        val h = heightSum()
        if (w != width || h != height) {
            val replacementPixels = ByteArray(w * h)
            if (alpha != null) {
                val replacementAlpha = ByteArray(w * h)
                var currentH = 0
                while (currentH < height) {
                    val pixelIndex = currentH * width
                    var destIndex = w * (currentH + minY) + minX
                    for (i in 0 until width) {
                        replacementPixels[destIndex] = pixels[pixelIndex + i]
                        replacementAlpha[destIndex++] = alpha!![pixelIndex + i]
                    }
                    currentH++
                }
                alpha = replacementAlpha
            } else {
                for (y in 0 until height) {
                    val srcIndex = y * width
                    var destIndex = w * (y + minY) + minX
                    for (x in 0 until width) {
                        replacementPixels[destIndex++] = pixels[srcIndex + x]
                    }
                }
            }

            borderY = 0
            minY = 0
            borderX = 0
            minX = 0
            width = w
            height = h
            pixels = replacementPixels
        }
    }

    fun outline(outlineColor: Int) {
        var finalPaletteIndex = -1
        if (pallete.size < 255) {
            for (paletteIndex in pallete.indices) {
                if (pallete[paletteIndex] == outlineColor) {
                    finalPaletteIndex = paletteIndex
                    break
                }
            }
            if (finalPaletteIndex == -1) {
                finalPaletteIndex = pallete.size
                val replacementPalette = IntArray(pallete.size + 1)
                ArrayUtils.arrayCopy(pallete, 0, replacementPalette, 0, pallete.size)
                pallete = replacementPalette
                replacementPalette[finalPaletteIndex] = outlineColor
            }
        } else {
            var minDeltaSum = Int.MAX_VALUE
            val red = (outlineColor shr 16) and 0xff
            val green = (outlineColor shr 8) and 0xff
            val blue = outlineColor and 0xff
            for (paletteIndex in pallete.indices) {
                val currentColorRgb = pallete[paletteIndex]
                val currentRed = (currentColorRgb shr 16) and 0xff
                val currentGreen = (currentColorRgb shr 8) and 0xff
                val currentBlue = currentColorRgb and 0xff
                var redDelta = red - currentRed
                if (redDelta < 0) {
                    redDelta = -redDelta
                }
                var greenDelta = green - currentGreen
                if (greenDelta < 0) {
                    greenDelta = -greenDelta
                }
                var blueDelta = blue - currentBlue
                if (blueDelta < 0) {
                    blueDelta = -blueDelta
                }
                val deltaSum = redDelta + greenDelta + blueDelta
                if (deltaSum < minDeltaSum) {
                    minDeltaSum = deltaSum
                    finalPaletteIndex = paletteIndex
                }
            }
        }
        for (h in height - 1 downTo 1) {
            val basepos = h * width
            for (w in width - 1 downTo 1) {
                if (pallete[pixels[w + basepos].toInt() and 0xff] == 0 &&
                    pallete[pixels[w + basepos - 1 - width].toInt() and 0xff] != 0) {
                    pixels[w + basepos] = finalPaletteIndex.toByte()
                }
            }
        }
    }

    fun flipHorizontal() {
        val bytes = pixels
        if (alpha == null) {
            for (row in height - 1 downTo 0) {
                var left = row * width
                var right = (row + 1) * width
                while (left < right) {
                    right--
                    val temp = bytes[left]
                    bytes[left] = bytes[right]
                    bytes[right] = temp
                    left++
                }
            }
        } else {
            val alphaBytes = alpha!!
            for (row in height - 1 downTo 0) {
                var left = row * width
                var right = (row + 1) * width
                while (left < right) {
                    right--
                    var temp = bytes[left]
                    bytes[left] = bytes[right]
                    bytes[right] = temp
                    temp = alphaBytes[left]
                    alphaBytes[left] = alphaBytes[right]
                    alphaBytes[right] = temp
                    left++
                }
            }
        }

        val temp = minX
        minX = borderX
        borderX = temp
    }

    fun flipVertical() {
        val bytes = pixels
        if (alpha == null) {
            for (row in (height shr 1) - 1 downTo 0) {
                var topIndex = row * width
                var bottomIndex = (height - row - 1) * width
                for (i in -width until 0) {
                    val temp = bytes[topIndex]
                    bytes[topIndex] = bytes[bottomIndex]
                    bytes[bottomIndex] = temp
                    topIndex++
                    bottomIndex++
                }
            }
        } else {
            val alphaBytes = alpha!!
            for (row in (height shr 1) - 1 downTo 0) {
                var topIndex = row * width
                var bottomIndex = (height - row - 1) * width
                for (i in -width until 0) {
                    var temp = bytes[topIndex]
                    bytes[topIndex] = bytes[bottomIndex]
                    bytes[bottomIndex] = temp
                    temp = alphaBytes[topIndex]
                    alphaBytes[topIndex] = alphaBytes[bottomIndex]
                    alphaBytes[bottomIndex] = temp
                    topIndex++
                    bottomIndex++
                }
            }
        }

        val temp = minY
        minY = borderY
        borderY = temp
    }

    fun rotate90DegLeft() {
        val newPixels = ByteArray(width * height)
        var index = 0
        if (alpha == null) {
            for (x in 0 until width) {
                for (y in height - 1 downTo 0) {
                    newPixels[index++] = pixels[x + y * width]
                }
            }
            pixels = newPixels
        } else {
            val newAlpha = ByteArray(width * height)
            for (x in 0 until width) {
                for (y in height - 1 downTo 0) {
                    newPixels[index] = pixels[x + y * width]
                    newAlpha[index++] = alpha!![x + y * width]
                }
            }
            pixels = newPixels
            alpha = newAlpha
        }

        val tempMinY = minY
        minY = minX
        minX = borderY
        borderY = borderX
        borderX = tempMinY
        val tempHeight = height
        height = width
        width = tempHeight
    }

    fun getPixels(): IntArray {
        val currentWidth = widthSum()
        val pixelBuffer = IntArray(currentWidth * heightSum())
        if (alpha != null) {
            for (y in 0 until height) {
                var srcIndex = y * width
                var destIndex = currentWidth * (y + minY) + minX
                for (x in 0 until width) {
                    pixelBuffer[destIndex++] = (alpha!![srcIndex].toInt() shl 24) or pallete[pixels[srcIndex].toInt() and 0xff]
                    srcIndex++
                }
            }
        } else {
            for (y in 0 until height) {
                var srcIndex = y * width
                var destIndex = currentWidth * (y + minY) + minX
                for (x in 0 until width) {
                    val color = pallete[pixels[srcIndex++].toInt() and 0xff]
                    pixelBuffer[destIndex++] = if (color != 0) {
                        0xffffff.inv() or color
                    } else {
                        0
                    }
                }
            }
        }
        return pixelBuffer
    }

    fun offsetRgbColor(redOffset: Int, greenOffset: Int, blueOffset: Int) {
        for (paletteIndex in 1 until pallete.size) {
            if (pallete[paletteIndex] != 1 && pallete[paletteIndex] != 16711935) {
                var r = (pallete[paletteIndex] shr 16) and 0xff
                r += redOffset
                if (r < 0) {
                    r = 0
                } else if (r > 255) {
                    r = 255
                }

                var g = (pallete[paletteIndex] shr 8) and 0xff
                g += greenOffset
                if (g < 0) {
                    g = 0
                } else if (g > 255) {
                    g = 255
                }

                var b = pallete[paletteIndex] and 0xff
                b += blueOffset
                if (b < 0) {
                    b = 0
                } else if (b > 255) {
                    b = 255
                }
                pallete[paletteIndex] = (r shl 16) or (g shl 8) or b
            }
        }
    }

    fun drawThickBorder(color: Int) {
        var lowestRgbIndex = -1
        if (pallete.size < 255) {
            for (paletteIndex in pallete.indices) {
                if (pallete[paletteIndex] == color) {
                    lowestRgbIndex = paletteIndex
                    break
                }
            }
            if (lowestRgbIndex == -1) {
                lowestRgbIndex = pallete.size
                val copiedPalette = IntArray(pallete.size + 1)
                ArrayUtils.arrayCopy(pallete, 0, copiedPalette, 0, pallete.size)
                pallete = copiedPalette
                copiedPalette[lowestRgbIndex] = color
            }
        } else {
            var lowestRgb = Int.MAX_VALUE
            val originalRed = (color shr 16) and 0xff
            val originalGreen = (color shr 8) and 0xff
            val originalBlue = color and 0xff

            for (index in pallete.indices) {
                val paletteColor = pallete[index]
                val red = (paletteColor shr 16) and 0xff
                val green = (paletteColor shr 8) and 0xff
                val blue = paletteColor and 0xff
                var redDelta = originalRed - red
                if (redDelta < 0) {
                    redDelta = -redDelta
                }

                var greenDelta = originalGreen - green
                if (greenDelta < 0) {
                    greenDelta = -greenDelta
                }

                var blueDelta = originalBlue - blue
                if (blueDelta < 0) {
                    blueDelta = -blueDelta
                }

                val rgbSum = redDelta + greenDelta + blueDelta
                if (rgbSum < lowestRgb) {
                    lowestRgb = rgbSum
                    lowestRgbIndex = index
                }
            }
        }
        var replacementPixelIndex = 0
        val replacementPixels = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var paletteIndex = pixels[replacementPixelIndex].toInt() and 0xff
                if (pallete[paletteIndex] == 0) {
                    if (x > 0 && pallete[pixels[replacementPixelIndex - 1].toInt() and 0xff] != 0) {
                        paletteIndex = lowestRgbIndex
                    } else if (y > 0 && pallete[pixels[replacementPixelIndex - width].toInt() and 0xff] != 0) {
                        paletteIndex = lowestRgbIndex
                    } else if (x < width - 1 && pallete[pixels[replacementPixelIndex + 1].toInt() and 0xff] != 0) {
                        paletteIndex = lowestRgbIndex
                    } else if (y < height - 1 && pallete[pixels[replacementPixelIndex + width].toInt() and 0xff] != 0) {
                        paletteIndex = lowestRgbIndex
                    }
                }
                replacementPixels[replacementPixelIndex++] = paletteIndex.toByte()
            }
        }
        pixels = replacementPixels
    }

    fun drawSmallBorder(borderThickness: Int) {
        val totalWidth = widthSum()
        val totalHeight = heightSum()
        if (totalWidth != width || totalHeight != height) {
            var leftBorder = borderThickness
            if (borderThickness > minX) {
                leftBorder = minX
            }

            var rightBorder = borderThickness
            if (borderThickness + minX + width > totalWidth) {
                rightBorder = totalWidth - minX - width
            }

            var topBorder = borderThickness
            if (borderThickness > minY) {
                topBorder = minY
            }

            var bottomBorder = borderThickness
            if (borderThickness + minY + height > totalHeight) {
                bottomBorder = totalHeight - minY - height
            }

            val newWidth = leftBorder + rightBorder + width
            val newHeight = topBorder + bottomBorder + height
            val newPixels = ByteArray(newWidth * newHeight)
            if (alpha == null) {
                for (y in 0 until height) {
                    var srcIndex = y * width
                    var destIndex = newWidth * (y + topBorder) + leftBorder
                    for (x in 0 until width) {
                        newPixels[destIndex++] = pixels[srcIndex++]
                    }
                }
            } else {
                val newAlpha = ByteArray(newWidth * newHeight)
                for (y in 0 until height) {
                    var srcIndex = y * width
                    var destIndex = newWidth * (y + topBorder) + leftBorder
                    for (x in 0 until width) {
                        newAlpha[destIndex] = alpha!![srcIndex]
                        newPixels[destIndex++] = pixels[srcIndex++]
                    }
                }
                alpha = newAlpha
            }

            minX -= leftBorder
            minY -= topBorder
            borderX -= rightBorder
            borderY -= bottomBorder
            width = newWidth
            height = newHeight
            pixels = newPixels
        }
    }

    companion object {
        @JvmStatic
        fun listSingle(resource: Js5ResourceProvider?, archiveId: Int, fileId: Int): SpriteType? {
            val data = resource?.getFile(archiveId, fileId)
            return if (data == null) null else decode(data)[0]
        }

        @JvmStatic
        fun listSingle(resource: Js5ResourceProvider?, fileId: Int): SpriteType? {
            val data = resource?.getFile(fileId)
            return if (data == null) null else decode(data)[0]
        }

        @JvmStatic
        fun listGroup(resource: Js5ResourceProvider?, fileId: Int): Array<SpriteType>? {
            val data = resource?.getFile(fileId)
            return if (data == null) null else decode(data)
        }

        @JvmStatic
        fun decodeGroup(resource: Js5ResourceProvider?, archive: Int): Array<SpriteType>? {
            val data = resource?.getFile(archive, 0)
            return if (data == null) null else decode(data)
        }

        @JvmStatic
        fun decode(data: ByteArray): Array<SpriteType> {
            val stream = Packet(data)
            stream.currentPosition = data.size - 2
            val count = stream.readUShort()
            val sprites = Array(count) { SpriteType() }

            stream.currentPosition = data.size - 7 - count * 8
            val biggestWidth = stream.readUShort()
            val biggestHeight = stream.readUShort()
            val palleteLength = (stream.readUByte() and 0xff) + 1

            for (i in 0 until count) {
                sprites[i].minX = stream.readUShort()
            }

            for (i in 0 until count) {
                sprites[i].minY = stream.readUShort()
            }

            for (i in 0 until count) {
                sprites[i].width = stream.readUShort()
            }

            for (i in 0 until count) {
                sprites[i].height = stream.readUShort()
            }

            for (i in 0 until count) {
                val sprite = sprites[i]
                sprite.borderX = biggestWidth - sprite.width - sprite.minX
                sprite.borderY = biggestHeight - sprite.height - sprite.minY
            }

            stream.currentPosition = data.size - 7 - count * 8 - (palleteLength - 1) * 3
            val pallete = IntArray(palleteLength)

            for (i in 1 until palleteLength) {
                pallete[i] = stream.readUMedium()
                if (pallete[i] == 0) {
                    pallete[i] = 1
                }
            }

            for (i in 0 until count) {
                sprites[i].pallete = pallete
            }

            stream.currentPosition = 0

            for (i in 0 until count) {
                val sprite = sprites[i]
                val numPixels = sprite.width * sprite.height
                sprite.pixels = ByteArray(numPixels)
                val flags = stream.readUByte()
                if ((flags and 0x2) == 0) {
                    if ((flags and 0x1) == 0) {
                        for (j in 0 until numPixels) {
                            sprite.pixels[j] = stream.readByte()
                        }
                    } else {
                        for (x in 0 until sprite.width) {
                            for (y in 0 until sprite.height) {
                                sprite.pixels[x + y * sprite.width] = stream.readByte()
                            }
                        }
                    }
                } else {
                    var usesAlpha = false
                    sprite.alpha = ByteArray(numPixels)
                    if ((flags and 0x1) == 0) {
                        for (j in 0 until numPixels) {
                            sprite.pixels[j] = stream.readByte()
                        }

                        for (j in 0 until numPixels) {
                            val alpha = stream.readByte()
                            sprite.alpha!![j] = alpha
                            usesAlpha = usesAlpha or (alpha != (-1).toByte())
                        }
                    } else {
                        for (x in 0 until sprite.width) {
                            for (y in 0 until sprite.height) {
                                sprite.pixels[x + y * sprite.width] = stream.readByte()
                            }
                        }

                        for (x in 0 until sprite.width) {
                            for (y in 0 until sprite.height) {
                                val alpha = stream.readByte()
                                sprite.alpha!![x + y * sprite.width] = alpha
                                usesAlpha = usesAlpha or (alpha != (-1).toByte())
                            }
                        }
                    }

                    if (!usesAlpha) {
                        sprite.alpha = null
                    }
                }
            }

            return sprites
        }
    }
}
