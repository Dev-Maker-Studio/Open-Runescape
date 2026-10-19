package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM
import kotlin.math.min

/**
 * The default font renderer for safe mode, doesn't support alpha
 * @author Brainpower
 */
class FontRendererSM(
    override val toolkit: SafeModeToolkit,
    fontMetrics: FontMetrics,
    sprites: Array<SpriteType>,
    glyphWidthArray: IntArray,
    glyphHeightArray: IntArray
) : AbstractFontRenderer(toolkit, fontMetrics) {

    @JvmField
    val charWidths: IntArray = glyphWidthArray

    @JvmField
    val charHeights: IntArray = glyphHeightArray

    @JvmField
    val glyphPixelData: Array<ByteArray?>

    @JvmField
    val glyphOffsetY: IntArray

    @JvmField
    val glyphOffsetX: IntArray

    @JvmField
    val glyphPalette: IntArray?

    init {
        glyphPixelData = Array(sprites.size) { null }
        glyphOffsetY = IntArray(sprites.size)
        glyphOffsetX = IntArray(sprites.size)
        for (index in sprites.indices) {
            glyphPixelData[index] = sprites[index].pixels
            glyphOffsetY[index] = sprites[index].minY
            glyphOffsetX[index] = sprites[index].minX
        }
        glyphPalette = sprites[0].pallete
    }

    override fun drawChar(glyphIndex: Int, drawX: Int, drawY: Int, color: Int, useAlpha: Boolean) {
        var y = drawY
        var x = drawX
        if (toolkit.colourBuffer != null) {
            x += glyphOffsetX[glyphIndex]
            y += glyphOffsetY[glyphIndex]
            var glyphWidth = charWidths[glyphIndex]
            var glyphHeight = charHeights[glyphIndex]
            val viewportWidth = toolkit.viewportWidth
            var pixelIndex = x + y * viewportWidth
            var dstRowPadding = viewportWidth - glyphWidth
            var srcRowPadding = 0
            var srcOffset = 0
            var clippedAmount: Int
            if (y < toolkit.clipTop) {
                clippedAmount = toolkit.clipTop - y
                glyphHeight -= clippedAmount
                y = toolkit.clipTop
                srcOffset += clippedAmount * glyphWidth
                pixelIndex += clippedAmount * viewportWidth
            }
            if (y + glyphHeight > toolkit.clipBottom) {
                glyphHeight -= y + glyphHeight - toolkit.clipBottom
            }
            if (x < toolkit.clipLeft) {
                clippedAmount = toolkit.clipLeft - x
                glyphWidth -= clippedAmount
                x = toolkit.clipLeft
                srcOffset += clippedAmount
                pixelIndex += clippedAmount
                srcRowPadding += clippedAmount
                dstRowPadding += clippedAmount
            }
            if (glyphWidth + x > toolkit.clipRight) {
                clippedAmount = glyphWidth + x - toolkit.clipRight
                glyphWidth -= clippedAmount
                srcRowPadding += clippedAmount
                dstRowPadding += clippedAmount
            }
            if (glyphWidth > 0 && glyphHeight > 0) {
                if (useAlpha) {
                    drawGlyphOpaque(glyphPixelData[glyphIndex]!!, toolkit.colourBuffer!!, color, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding)
                } else {
                    drawGlyphPaletted(glyphPixelData[glyphIndex]!!, toolkit.colourBuffer!!, glyphPalette!!, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding)
                }
            }
        }
    }

    fun drawGlyphOpaque(glyphData: ByteArray, pixels: IntArray, color: Int, glyphOffset: Int, pixelOffset: Int, width: Int, height: Int, dstRowPadding: Int, srcRowPadding: Int) {
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset
        val unrolledCount = -(width shr 2)
        val remainderCount = -(width and 0x3)
        for (row in -height until 0) {
            for (col in unrolledCount until 0) {
                if (glyphData[glyphPos++] != 0.toByte()) {
                    pixels[pixelPos++] = color
                } else {
                    ++pixelPos
                }
                if (glyphData[glyphPos++] != 0.toByte()) {
                    pixels[pixelPos++] = color
                } else {
                    ++pixelPos
                }
                if (glyphData[glyphPos++] != 0.toByte()) {
                    pixels[pixelPos++] = color
                } else {
                    ++pixelPos
                }
                if (glyphData[glyphPos++] != 0.toByte()) {
                    pixels[pixelPos++] = color
                } else {
                    ++pixelPos
                }
            }
            for (col in remainderCount until 0) {
                if (glyphData[glyphPos++] != 0.toByte()) {
                    pixels[pixelPos++] = color
                } else {
                    ++pixelPos
                }
            }
            pixelPos += dstRowPadding
            glyphPos += srcRowPadding
        }
    }

    override fun drawChar(glyphIndex: Int, drawX: Int, drawY: Int, color: Int, useAlpha: Boolean, dimension: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        var y = drawY
        var x = drawX
        if (toolkit.colourBuffer != null) {
            if (dimension == null) {
                drawChar(glyphIndex, x, y, color, useAlpha)
            } else {
                x += glyphOffsetX[glyphIndex]
                y += glyphOffsetY[glyphIndex]
                var glyphWidth = charWidths[glyphIndex]
                var glyphHeight = charHeights[glyphIndex]
                val viewportWidth = toolkit.viewportWidth
                var pixelIndex = x + y * viewportWidth
                var dstRowPadding = viewportWidth - glyphWidth
                var srcRowPadding = 0
                var srcOffset = 0
                var clippedAmount: Int
                if (y < toolkit.clipTop) {
                    clippedAmount = toolkit.clipTop - y
                    glyphHeight -= clippedAmount
                    y = toolkit.clipTop
                    srcOffset += glyphWidth * clippedAmount
                    pixelIndex += clippedAmount * viewportWidth
                }
                if (y + glyphHeight > toolkit.clipBottom) {
                    glyphHeight -= y + glyphHeight - toolkit.clipBottom
                }
                if (x < toolkit.clipLeft) {
                    clippedAmount = toolkit.clipLeft - x
                    glyphWidth -= clippedAmount
                    x = toolkit.clipLeft
                    srcOffset += clippedAmount
                    pixelIndex += clippedAmount
                    srcRowPadding += clippedAmount
                    dstRowPadding += clippedAmount
                }
                if (glyphWidth + x > toolkit.clipRight) {
                    clippedAmount = glyphWidth + x - toolkit.clipRight
                    glyphWidth -= clippedAmount
                    srcRowPadding += clippedAmount
                    dstRowPadding += clippedAmount
                }
                if (glyphWidth > 0 && glyphHeight > 0) {
                    if (useAlpha) {
                        drawGlyphOpaqueMasked(glyphPixelData[glyphIndex]!!, toolkit.colourBuffer!!, color, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding, x, y, charWidths[glyphIndex], dimension, surfaceOffsetX, surfaceOffsetY)
                    } else {
                        drawGlyphPalettedMasked(glyphPixelData[glyphIndex]!!, toolkit.colourBuffer!!, glyphPalette!!, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding, x, y, charWidths[glyphIndex], dimension, surfaceOffsetX, surfaceOffsetY)
                    }
                }
            }
        }
    }

    fun drawGlyphPalettedMasked(glyphData: ByteArray, pixels: IntArray, palette: IntArray, glyphOffset: Int, pixelOffset: Int, clippedWidth: Int, clippedHeight: Int, dstRowPadding: Int, srcRowPadding: Int, drawX: Int, drawY: Int, fullGlyphWidth: Int, dimension: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        val dimensionSM = dimension as ComponentDimensionSM
        val rowStartXArr = dimensionSM.rowStartX
        val rowWidthArr = dimensionSM.rowWidth
        val relativeDrawX = drawX - toolkit.clipLeft
        var currentY = drawY
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset
        if (surfaceOffsetY > drawY) {
            currentY = surfaceOffsetY
            pixelPos += (surfaceOffsetY - drawY) * toolkit.viewportWidth
            glyphPos += fullGlyphWidth * (surfaceOffsetY - drawY)
        }
        val maxY = min(rowStartXArr.size + surfaceOffsetY, clippedHeight + drawY)
        for (scanlineY in currentY until maxY) {
            var rowStartX = surfaceOffsetX + rowStartXArr[scanlineY - surfaceOffsetY]
            var rowWidth = rowWidthArr[scanlineY - surfaceOffsetY]
            var remainingWidth = clippedWidth
            var adjustAmount: Int
            if (relativeDrawX > rowStartX) {
                adjustAmount = relativeDrawX - rowStartX
                if (adjustAmount >= rowWidth) {
                    glyphPos += srcRowPadding + clippedWidth
                    pixelPos += clippedWidth + dstRowPadding
                    continue
                }
                rowWidth -= adjustAmount
            } else {
                adjustAmount = rowStartX - relativeDrawX
                if (adjustAmount >= clippedWidth) {
                    glyphPos += srcRowPadding + clippedWidth
                    pixelPos += clippedWidth + dstRowPadding
                    continue
                }
                glyphPos += adjustAmount
                remainingWidth = clippedWidth - adjustAmount
                pixelPos += adjustAmount
            }
            adjustAmount = 0
            if (remainingWidth < rowWidth) {
                rowWidth = remainingWidth
            } else {
                adjustAmount = remainingWidth - rowWidth
            }
            for (col in -rowWidth until 0) {
                val paletteIndex: Byte
                if (glyphData[glyphPos++].also { paletteIndex = it } != 0.toByte()) {
                    pixels[pixelPos++] = palette[paletteIndex.toInt() and 0xff]
                } else {
                    ++pixelPos
                }
            }
            glyphPos += srcRowPadding + adjustAmount
            pixelPos += adjustAmount + dstRowPadding
        }
    }

    fun drawGlyphOpaqueMasked(glyphData: ByteArray, pixels: IntArray, color: Int, glyphOffset: Int, pixelOffset: Int, clippedWidth: Int, clippedHeight: Int, dstRowPadding: Int, srcRowPadding: Int, drawX: Int, drawY: Int, fullGlyphWidth: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        val dimensionSM = surface as ComponentDimensionSM
        val rowStartXArr = dimensionSM.rowStartX
        val rowWidthArr = dimensionSM.rowWidth
        val relativeDrawX = drawX - toolkit.clipLeft
        var currentY = drawY
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset
        if (surfaceOffsetY > drawY) {
            currentY = surfaceOffsetY
            pixelPos += toolkit.viewportWidth * (surfaceOffsetY - drawY)
            glyphPos += (surfaceOffsetY - drawY) * fullGlyphWidth
        }
        val maxY = min(rowStartXArr.size + surfaceOffsetY, drawY + clippedHeight)
        for (scanlineY in currentY until maxY) {
            val rowStartX = rowStartXArr[scanlineY - surfaceOffsetY] + surfaceOffsetX
            var rowWidth = rowWidthArr[scanlineY - surfaceOffsetY]
            var remainingWidth = clippedWidth
            var adjustAmount: Int
            if (relativeDrawX > rowStartX) {
                adjustAmount = relativeDrawX - rowStartX
                if (adjustAmount >= rowWidth) {
                    glyphPos += srcRowPadding + clippedWidth
                    pixelPos += dstRowPadding + clippedWidth
                    continue
                }
                rowWidth -= adjustAmount
            } else {
                adjustAmount = rowStartX - relativeDrawX
                if (adjustAmount >= clippedWidth) {
                    glyphPos += srcRowPadding + clippedWidth
                    pixelPos += dstRowPadding + clippedWidth
                    continue
                }
                glyphPos += adjustAmount
                remainingWidth = clippedWidth - adjustAmount
                pixelPos += adjustAmount
            }
            adjustAmount = 0
            if (remainingWidth < rowWidth) {
                rowWidth = remainingWidth
            } else {
                adjustAmount = remainingWidth - rowWidth
            }
            for (col in -rowWidth until 0) {
                if (glyphData[glyphPos++] != 0.toByte()) {
                    pixels[pixelPos++] = color
                } else {
                    ++pixelPos
                }
            }
            glyphPos += adjustAmount + glyphPos
            pixelPos += adjustAmount + dstRowPadding
        }
    }

    fun drawGlyphPaletted(glyphData: ByteArray, pixels: IntArray, palette: IntArray, glyphOffset: Int, pixelOffset: Int, width: Int, height: Int, dstRowPadding: Int, srcRowPadding: Int) {
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset
        val unrolledCount = -(width shr 2)
        val remainderCount = -(width and 0x3)
        for (row in -height until 0) {
            var col: Int
            var paletteIndex: Byte
            col = unrolledCount
            while (col < 0) {
                if (glyphData[glyphPos++].also { paletteIndex = it } != 0.toByte()) {
                    pixels[pixelPos++] = palette[paletteIndex.toInt() and 0xff]
                } else {
                    ++pixelPos
                }
                if (glyphData[glyphPos++].also { paletteIndex = it } != 0.toByte()) {
                    pixels[pixelPos++] = palette[paletteIndex.toInt() and 0xff]
                } else {
                    ++pixelPos
                }
                if (glyphData[glyphPos++].also { paletteIndex = it } != 0.toByte()) {
                    pixels[pixelPos++] = palette[paletteIndex.toInt() and 0xff]
                } else {
                    ++pixelPos
                }
                if (glyphData[glyphPos++].also { paletteIndex = it } != 0.toByte()) {
                    pixels[pixelPos++] = palette[paletteIndex.toInt() and 0xff]
                } else {
                    ++pixelPos
                }
                col++
            }
            col = remainderCount
            while (col < 0) {
                if (glyphData[glyphPos++].also { paletteIndex = it } != 0.toByte()) {
                    pixels[pixelPos++] = palette[paletteIndex.toInt() and 0xff]
                } else {
                    ++pixelPos
                }
                col++
            }
            pixelPos += dstRowPadding
            glyphPos += srcRowPadding
        }
    }
}
