package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM
import kotlin.math.min

class TransparentFontRendererSM(
    toolkit: SafeModeToolkit,
    fontMetrics: FontMetrics,
    sprites: Array<SpriteType>,
    spriteWidths: IntArray,
    spriteHeights: IntArray
) : AbstractFontRenderer(toolkit, fontMetrics) {

    @JvmField
    var safeModeToolkit: SafeModeToolkit = toolkit

    @JvmField
    val glyphWidths: IntArray = spriteWidths

    @JvmField
    val glyphHeights: IntArray = spriteHeights

    @JvmField
    val glyphAlphaData: Array<ByteArray?>

    @JvmField
    val glyphOffsetY: IntArray

    @JvmField
    val glyphOffsetX: IntArray

    init {
        glyphAlphaData = arrayOfNulls(sprites.size)
        glyphOffsetY = IntArray(sprites.size)
        glyphOffsetX = IntArray(sprites.size)

        for (index in sprites.indices) {
            val sprite = sprites[index]
            if (sprite.alpha != null) {
                glyphAlphaData[index] = sprite.alpha
            } else {
                val pixels = sprite.pixels
                val alphaData = ByteArray(pixels.size)
                glyphAlphaData[index] = alphaData

                for (i in pixels.indices) {
                    alphaData[i] = if (pixels[i].toInt() == 0) 0 else -1
                }
            }

            glyphOffsetY[index] = sprite.minY
            glyphOffsetX[index] = sprite.minX
        }
    }

    override fun drawChar(glyphIndex: Int, drawX: Int, drawY: Int, color: Int, useAlpha: Boolean) {
        val baseY = drawY
        var x = drawX
        if (safeModeToolkit.colourBuffer != null) {
            x += glyphOffsetX[glyphIndex]
            var y = baseY + glyphOffsetY[glyphIndex]
            var glyphWidth = glyphWidths[glyphIndex]
            var glyphHeight = glyphHeights[glyphIndex]
            val viewportWidth = safeModeToolkit.viewportWidth
            var pixelIndex = x + y * viewportWidth
            var dstRowPadding = viewportWidth - glyphWidth
            var srcRowPadding = 0
            var srcOffset = 0
            var clippedAmount: Int
            if (y < safeModeToolkit.clipTop) {
                clippedAmount = safeModeToolkit.clipTop - y
                glyphHeight -= clippedAmount
                y = safeModeToolkit.clipTop
                srcOffset += glyphWidth * clippedAmount
                pixelIndex += viewportWidth * clippedAmount
            }

            if (y + glyphHeight > safeModeToolkit.clipBottom) {
                glyphHeight -= y + glyphHeight - safeModeToolkit.clipBottom
            }

            if (x < safeModeToolkit.clipLeft) {
                clippedAmount = safeModeToolkit.clipLeft - x
                glyphWidth -= clippedAmount
                x = safeModeToolkit.clipLeft
                srcOffset += clippedAmount
                pixelIndex += clippedAmount
                srcRowPadding += clippedAmount
                dstRowPadding += clippedAmount
            }

            if (glyphWidth + x > safeModeToolkit.clipRight) {
                clippedAmount = glyphWidth + x - safeModeToolkit.clipRight
                glyphWidth -= clippedAmount
                srcRowPadding += clippedAmount
                dstRowPadding += clippedAmount
            }

            if (glyphWidth > 0 && glyphHeight > 0) {
                blendGlyphPixels(glyphAlphaData[glyphIndex]!!, safeModeToolkit.colourBuffer!!, color, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding)
            }
        }
    }

    fun blendGlyphPixels(glyphData: ByteArray, pixels: IntArray, color: Int, glyphOffset: Int, pixelOffset: Int, clippedWidth: Int, clippedHeight: Int, dstRowPadding: Int, srcRowPadding: Int) {
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset
        for (row in -clippedHeight until 0) {
            for (col in -clippedWidth until 0) {
                var alpha = glyphData[glyphPos++].toInt() and 0xff
                if (alpha != 0) {
                    val blendedSrc = (alpha * (color and 0xff00) and 0xff0000) + ((color and 0xff00ff) * alpha and -16711936) shr 8
                    alpha = 256 - alpha
                    val dstPixel = pixels[pixelPos]
                    pixels[pixelPos++] = blendedSrc + ((alpha * (dstPixel and 0xff00) and 0xff0000) + ((dstPixel and 0xff00ff) * alpha and -16711936) shr 8)
                } else {
                    ++pixelPos
                }
            }

            pixelPos += dstRowPadding
            glyphPos += srcRowPadding
        }
    }

    fun blendGlyphPixelsMasked(
        glyphData: ByteArray,
        pixels: IntArray,
        color: Int,
        glyphOffset: Int,
        pixelOffset: Int,
        clippedWidth: Int,
        clippedHeight: Int,
        dstRowPadding: Int,
        srcRowPadding: Int,
        drawX: Int,
        drawY: Int,
        fullGlyphWidth: Int,
        surface: ComponentSurface,
        surfaceOffsetX: Int,
        surfaceOffsetY: Int
    ) {
        val dimensionSM = surface as ComponentDimensionSM
        val rowStartXArr = dimensionSM.rowStartX
        val rowWidthArr = dimensionSM.rowWidth
        val relativeDrawX = drawX - safeModeToolkit.clipLeft
        var currentY = drawY
        if (surfaceOffsetY > drawY) {
            currentY = surfaceOffsetY
            var pixelPos = pixelOffset + safeModeToolkit.viewportWidth * (surfaceOffsetY - drawY)
            var glyphPos = glyphOffset + (surfaceOffsetY - drawY) * fullGlyphWidth
        }

        val maxY = min(rowStartXArr.size + surfaceOffsetY, drawY + clippedHeight)
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset

        for (scanlineY in currentY until maxY) {
            val rowStartX = rowStartXArr[scanlineY - surfaceOffsetY] + surfaceOffsetX
            val rowWidthOrig = rowWidthArr[scanlineY - surfaceOffsetY]
            var remainingWidth = clippedWidth
            val adjustAmount: Int
            var rowWidth = rowWidthOrig
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

            var skipCount = 0
            if (remainingWidth < rowWidth) {
                rowWidth = remainingWidth
            } else {
                skipCount = remainingWidth - rowWidth
            }

            for (col in -rowWidth until 0) {
                var alpha = glyphData[glyphPos++].toInt() and 0xff
                if (alpha != 0) {
                    val blendedSrc = (alpha * (color and 0xff00) and 0xff0000) + ((color and 0xff00ff) * alpha and -16711936) shr 8
                    alpha = 256 - alpha
                    val dstPixel = pixels[pixelPos]
                    pixels[pixelPos++] = (((dstPixel and 0xff00ff) * alpha and -16711936) + (alpha * (dstPixel and 0xff00) and 0xff0000) shr 8) + blendedSrc
                } else {
                    ++pixelPos
                }
            }

            glyphPos = skipCount + glyphPos + srcRowPadding
            pixelPos += skipCount + dstRowPadding
        }
    }

    override fun drawChar(
        glyphIndex: Int,
        drawX: Int,
        drawY: Int,
        color: Int,
        useAlpha: Boolean,
        dimension: ComponentSurface,
        surfaceOffsetX: Int,
        surfaceOffsetY: Int
    ) {
        val baseY = drawY
        var x = drawX
        if (safeModeToolkit.colourBuffer != null) {
            if (dimension == null) {
                drawChar(glyphIndex, x, baseY, color, useAlpha)
            } else {
                x += glyphOffsetX[glyphIndex]
                var y = baseY + glyphOffsetY[glyphIndex]
                var glyphWidth = glyphWidths[glyphIndex]
                var glyphHeight = glyphHeights[glyphIndex]
                val viewportWidth = safeModeToolkit.viewportWidth
                var pixelIndex = x + y * viewportWidth
                var dstRowPadding = viewportWidth - glyphWidth
                var srcRowPadding = 0
                var srcOffset = 0
                var clippedAmount: Int
                if (y < safeModeToolkit.clipTop) {
                    clippedAmount = safeModeToolkit.clipTop - y
                    glyphHeight -= clippedAmount
                    y = safeModeToolkit.clipTop
                    srcOffset += clippedAmount * glyphWidth
                    pixelIndex += clippedAmount * viewportWidth
                }

                if (y + glyphHeight > safeModeToolkit.clipBottom) {
                    glyphHeight -= y + glyphHeight - safeModeToolkit.clipBottom
                }

                if (x < safeModeToolkit.clipLeft) {
                    clippedAmount = safeModeToolkit.clipLeft - x
                    glyphWidth -= clippedAmount
                    x = safeModeToolkit.clipLeft
                    srcOffset += clippedAmount
                    pixelIndex += clippedAmount
                    srcRowPadding += clippedAmount
                    dstRowPadding += clippedAmount
                }

                if (glyphWidth + x > safeModeToolkit.clipRight) {
                    clippedAmount = glyphWidth + x - safeModeToolkit.clipRight
                    glyphWidth -= clippedAmount
                    srcRowPadding += clippedAmount
                    dstRowPadding += clippedAmount
                }

                if (glyphWidth > 0 && glyphHeight > 0) {
                    blendGlyphPixelsMasked(
                        glyphAlphaData[glyphIndex]!!,
                        safeModeToolkit.colourBuffer!!,
                        color,
                        srcOffset,
                        pixelIndex,
                        glyphWidth,
                        glyphHeight,
                        dstRowPadding,
                        srcRowPadding,
                        x,
                        y,
                        glyphWidths[glyphIndex],
                        dimension,
                        surfaceOffsetX,
                        surfaceOffsetY
                    )
                }
            }
        }
    }
}
