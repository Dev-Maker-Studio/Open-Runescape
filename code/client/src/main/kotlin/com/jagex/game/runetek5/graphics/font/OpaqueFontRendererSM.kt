package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM

class OpaqueFontRendererSM(
    override val toolkit: SafeModeToolkit,
    fontMetrics: FontMetrics,
    sprites: Array<SpriteType>,
    val glyphWidths: IntArray,
    val glyphHeights: IntArray
) : AbstractFontRenderer(toolkit, fontMetrics) {
    val spritePixels: Array<ByteArray?>
    val spriteMinY: IntArray
    val spriteMinX: IntArray

    init {
        this.spritePixels = arrayOfNulls(sprites.size)
        this.spriteMinY = IntArray(sprites.size)
        this.spriteMinX = IntArray(sprites.size)

        for (index in sprites.indices) {
            this.spritePixels[index] = sprites[index].pixels
            this.spriteMinY[index] = sprites[index].minY
            this.spriteMinX[index] = sprites[index].minX
        }
    }

    override fun drawChar(glyphIndex: Int, drawX: Int, drawY: Int, color: Int, useAlpha: Boolean) {
        var y = drawY
        var x = drawX
        if (this.toolkit.colourBuffer != null) {
            x += this.spriteMinX[glyphIndex]
            y += this.spriteMinY[glyphIndex]
            var glyphWidth = this.glyphWidths[glyphIndex]
            var glyphHeight = this.glyphHeights[glyphIndex]
            val viewportWidth = this.toolkit.viewportWidth
            var pixelIndex = y * viewportWidth + x
            var dstRowPadding = viewportWidth - glyphWidth
            var srcRowPadding = 0
            var srcOffset = 0
            var clippedAmount: Int
            if (y < this.toolkit.clipTop) {
                clippedAmount = this.toolkit.clipTop - y
                glyphHeight -= clippedAmount
                y = this.toolkit.clipTop
                srcOffset += glyphWidth * clippedAmount
                pixelIndex += viewportWidth * clippedAmount
            }

            if (y + glyphHeight > this.toolkit.clipBottom) {
                glyphHeight -= y + glyphHeight - this.toolkit.clipBottom
            }

            if (x < this.toolkit.clipLeft) {
                clippedAmount = this.toolkit.clipLeft - x
                glyphWidth -= clippedAmount
                x = this.toolkit.clipLeft
                srcOffset += clippedAmount
                pixelIndex += clippedAmount
                srcRowPadding += clippedAmount
                dstRowPadding += clippedAmount
            }

            if (glyphWidth + x > this.toolkit.clipRight) {
                clippedAmount = glyphWidth + x - this.toolkit.clipRight
                glyphWidth -= clippedAmount
                srcRowPadding += clippedAmount
                dstRowPadding += clippedAmount
            }

            if (glyphWidth > 0 && glyphHeight > 0) {
                if ((color and 0xffffff.inv()) == 0xffffff.inv()) {
                    drawGlyphOpaque(this.spritePixels[glyphIndex]!!, this.toolkit.colourBuffer!!, color, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding)
                } else if ((color and 0xffffff.inv()) != 0) {
                    blendImage(this.spritePixels[glyphIndex]!!, this.toolkit.colourBuffer!!, color, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding)
                }
            }
        }
    }

    override fun drawChar(glyphIndex: Int, drawX: Int, drawY: Int, color: Int, useAlpha: Boolean, dimension: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        var y = drawY
        var x = drawX
        if (this.toolkit.colourBuffer != null) {
            if (dimension == null) {
                drawChar(glyphIndex, x, y, color, useAlpha)
            } else {
                x += this.spriteMinX[glyphIndex]
                y += this.spriteMinY[glyphIndex]
                var glyphWidth = this.glyphWidths[glyphIndex]
                var glyphHeight = this.glyphHeights[glyphIndex]
                val viewportWidth = this.toolkit.viewportWidth
                var pixelIndex = y * viewportWidth + x
                var dstRowPadding = viewportWidth - glyphWidth
                var srcRowPadding = 0
                var srcOffset = 0
                var clippedAmount: Int
                if (y < this.toolkit.clipTop) {
                    clippedAmount = this.toolkit.clipTop - y
                    glyphHeight -= clippedAmount
                    y = this.toolkit.clipTop
                    srcOffset += glyphWidth * clippedAmount
                    pixelIndex += viewportWidth * clippedAmount
                }

                if (y + glyphHeight > this.toolkit.clipBottom) {
                    glyphHeight -= y + glyphHeight - this.toolkit.clipBottom
                }

                if (x < this.toolkit.clipLeft) {
                    clippedAmount = this.toolkit.clipLeft - x
                    glyphWidth -= clippedAmount
                    x = this.toolkit.clipLeft
                    srcOffset += clippedAmount
                    pixelIndex += clippedAmount
                    srcRowPadding += clippedAmount
                    dstRowPadding += clippedAmount
                }

                if (glyphWidth + x > this.toolkit.clipRight) {
                    clippedAmount = glyphWidth + x - this.toolkit.clipRight
                    glyphWidth -= clippedAmount
                    srcRowPadding += clippedAmount
                    dstRowPadding += clippedAmount
                }

                if (glyphWidth > 0 && glyphHeight > 0) {
                    if ((color and 0xffffff.inv()) == 0xffffff.inv()) {
                        drawImage(this.spritePixels[glyphIndex]!!, this.toolkit.colourBuffer!!, color, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding, x, y, this.glyphWidths[glyphIndex], dimension, surfaceOffsetX, surfaceOffsetY)
                    } else {
                        drawGlyphBlendedMasked(this.spritePixels[glyphIndex]!!, this.toolkit.colourBuffer!!, color, srcOffset, pixelIndex, glyphWidth, glyphHeight, dstRowPadding, srcRowPadding, x, y, this.glyphWidths[glyphIndex], dimension, surfaceOffsetX, surfaceOffsetY)
                    }
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
            var col: Int
            col = unrolledCount
            while (col < 0) {
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
                col++
            }

            col = remainderCount
            while (col < 0) {
                if (glyphData[glyphPos++] != 0.toByte()) {
                    pixels[pixelPos++] = color
                } else {
                    ++pixelPos
                }
                col++
            }

            pixelPos += dstRowPadding
            glyphPos += srcRowPadding
        }
    }

    fun blendImage(glyphData: ByteArray, pixels: IntArray, color: Int, glyphOffset: Int, pixelOffset: Int, width: Int, height: Int, dstRowPadding: Int, srcRowPadding: Int) {
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset
        val alpha = color ushr 24
        val invAlpha = 255 - alpha

        for (row in -height until 0) {
            for (col in -width until 0) {
                if (glyphData[glyphPos++] != 0.toByte()) {
                    val blendedSrc = (alpha * (color and 0xff00) and 0xff0000) + ((color and 0xff00ff) * alpha and -16711936) shr 8
                    val dstPixel = pixels[pixelPos]
                    pixels[pixelPos++] = blendedSrc + ((invAlpha * (dstPixel and 0xff00) and 0xff0000) + ((dstPixel and 0xff00ff) * invAlpha and -16711936) shr 8)
                } else {
                    pixelPos++
                }
            }

            pixelPos += dstRowPadding
            glyphPos += srcRowPadding
        }
    }

    fun drawImage(glyphData: ByteArray, pixels: IntArray, color: Int, glyphOffset: Int, pixelOffset: Int, clippedWidth: Int, clippedHeight: Int, dstRowPadding: Int, srcRowPadding: Int, drawX: Int, drawY: Int, fullGlyphWidth: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset
        val dimensionSM = surface as ComponentDimensionSM
        val rowStartXArr = dimensionSM.rowStartX
        val rowWidthArr = dimensionSM.rowWidth
        var currentY = drawY
        if (surfaceOffsetY > drawY) {
            currentY = surfaceOffsetY
            pixelPos += this.toolkit.viewportWidth * (surfaceOffsetY - drawY)
            glyphPos += (surfaceOffsetY - drawY) * fullGlyphWidth
        }

        val maxY = Math.min(rowStartXArr.size + surfaceOffsetY, drawY + clippedHeight)

        for (scanlineY in currentY until maxY) {
            var rowStartX = rowStartXArr[scanlineY - surfaceOffsetY] + surfaceOffsetX
            var rowWidth = rowWidthArr[scanlineY - surfaceOffsetY]
            var remainingWidth = clippedWidth
            val adjustAmount: Int
            if (drawX > rowStartX) {
                adjustAmount = drawX - rowStartX
                if (adjustAmount >= rowWidth) {
                    glyphPos += srcRowPadding + clippedWidth
                    pixelPos += dstRowPadding + clippedWidth
                    continue
                }

                rowWidth -= adjustAmount
            } else {
                adjustAmount = rowStartX - drawX
                if (adjustAmount >= clippedWidth) {
                    glyphPos += srcRowPadding + clippedWidth
                    pixelPos += dstRowPadding + clippedWidth
                    continue
                }

                glyphPos += adjustAmount
                remainingWidth = clippedWidth - adjustAmount
                pixelPos += adjustAmount
            }

            val skipCount: Int
            if (remainingWidth < rowWidth) {
                rowWidth = remainingWidth
                skipCount = 0
            } else {
                skipCount = remainingWidth - rowWidth
            }

            for (col in 0 until rowWidth) {
                if (glyphData[glyphPos++] != 0.toByte()) {
                    pixels[pixelPos++] = color
                } else {
                    pixelPos++
                }
            }

            glyphPos = skipCount + glyphPos + srcRowPadding
            pixelPos += skipCount + dstRowPadding
        }
    }

    fun drawGlyphBlendedMasked(glyphData: ByteArray, pixels: IntArray, color: Int, glyphOffset: Int, pixelOffset: Int, clippedWidth: Int, clippedHeight: Int, dstRowPadding: Int, srcRowPadding: Int, drawX: Int, drawY: Int, fullGlyphWidth: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        var glyphPos = glyphOffset
        var pixelPos = pixelOffset
        val dimensionSM = surface as ComponentDimensionSM
        val rowStartXArr = dimensionSM.rowStartX
        val rowWidthArr = dimensionSM.rowWidth
        val relativeDrawX = drawX - this.toolkit.clipLeft
        var currentY = drawY
        if (surfaceOffsetY > drawY) {
            currentY = surfaceOffsetY
            pixelPos += this.toolkit.viewportWidth * (surfaceOffsetY - drawY)
            glyphPos += (surfaceOffsetY - drawY) * fullGlyphWidth
        }

        val maxY = Math.min(rowStartXArr.size + surfaceOffsetY, drawY + clippedHeight)
        val alpha = color ushr 24
        val invAlpha = 255 - alpha

        for (scanlineY in currentY until maxY) {
            var rowStartX = rowStartXArr[scanlineY - surfaceOffsetY] + surfaceOffsetX
            var rowWidth = rowWidthArr[scanlineY - surfaceOffsetY]
            var remainingWidth = clippedWidth
            val adjustAmount: Int
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

            val skipCount: Int
            if (remainingWidth < rowWidth) {
                rowWidth = remainingWidth
                skipCount = 0
            } else {
                skipCount = remainingWidth - rowWidth
            }

            for (col in -rowWidth until 0) {
                if (glyphData[glyphPos++] != 0.toByte()) {
                    val blendedSrc = (alpha * (color and 0xff00) and 0xff0000) + ((color and 0xff00ff) * alpha and -16711936) shr 8
                    val dstPixel = pixels[pixelPos]
                    pixels[pixelPos++] = (((dstPixel and 0xff00ff) * invAlpha and -16711936) + (invAlpha * (dstPixel and 0xff00) and 0xff0000) shr 8) + blendedSrc
                } else {
                    ++pixelPos
                }
            }

            glyphPos = skipCount + glyphPos + srcRowPadding
            pixelPos += skipCount + dstRowPadding
        }
    }
}
