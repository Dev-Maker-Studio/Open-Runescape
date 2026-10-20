package com.jagex.game.runetek5.graphics.font

import java.util.Random

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.Sprite
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.Utils

abstract class AbstractFontRenderer(
    open val toolkit: AbstractToolkit,
    open val fontMetrics: FontMetrics
) {

    companion object {
        @JvmStatic
        var strikeThroughEffect: Int = -1
        @JvmStatic
        var underlineEffect: Int = -1
        @JvmStatic
        var colorArgb: Int = 0
        @JvmStatic
        var argbColorEffect: Int = 0
        @JvmStatic
        var originalShadowColor: Int = 0
        @JvmStatic
        var shadingEffect: Int = 0
        @JvmStatic
        var justifiedSpaceWidth: Int = 0
        @JvmStatic
        var justifiedSpaceAccumulator: Int = 0
        @JvmField
        val LINE_BUFFER: Array<String?> = arrayOfNulls(100)
        private const val EURO_GLYPH = 128
    }

    fun renderPlain(text: String?, x: Int, y: Int, color: Int, shadow: Int) {
        if (text != null) {
            calibrate(color, shadow)
            renderLine(text, x, y, null, null, null, 0, 0)
        }
    }

    fun renderLeftAligned(text: String?, x: Int, y: Int, color: Int, shadow: Int) {
        if (text != null) {
            calibrate(color, shadow)
            renderLine(text, x - this.fontMetrics.getWidthNoSprites(text), y, null, null, null, 0, 0)
        }
    }

    fun renderCentered(text: String?, x: Int, y: Int, color: Int, shadow: Int) {
        if (text != null) {
            calibrate(color, shadow)
            renderLine(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, null, 0, 0)
        }
    }

    fun calibrate(textColor: Int, shadowColor: Int) {
        var shadow = shadowColor
        strikeThroughEffect = -1
        underlineEffect = -1
        colorArgb = textColor
        argbColorEffect = textColor
        justifiedSpaceWidth = 0
        justifiedSpaceAccumulator = 0
        if (shadow == -1) {
            shadow = 0
        }
        originalShadowColor = shadow
        shadingEffect = shadow
    }

    fun renderTextAntiMacro(text: String?, x: Int, y: Int, maxWidth: Int, maxHeight: Int, color: Int,
                            shadow: Int, horizontalAlign: Int, verticalAlign: Int, random: Random, seed: Int, bounds: IntArray?,
                            sprites: Array<NativeSprite>?, spriteHeights: IntArray?): Int {
        if (text == null) {
            return 0
        }
        random.setSeed(seed.toLong())
        val alpha = 192 + (random.nextInt() and 0x1f)
        calibrate(alpha shl 24 or (color and 0xffffff),
                if (shadow == -1) 0 else alpha shl 24 or (shadow and 0xffffff))
        val textLength = text.length
        val offsetsX = IntArray(textLength)
        var extraWidth = 0
        for (i in 0 until textLength) {
            offsetsX[i] = extraWidth
            if ((random.nextInt() and 0x3) == 0) {
                extraWidth++
            }
        }
        var drawX = x
        var drawY = y + this.fontMetrics.topPadding
        var lineWidth = -1
        if (verticalAlign == 1) {
            drawY += (maxHeight - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding) / 2
        } else if (verticalAlign == 2) {
            drawY = y + maxHeight - this.fontMetrics.bottomPadding
        }
        if (horizontalAlign == 1) {
            lineWidth = this.fontMetrics.getWidthNoSprites(text) + extraWidth
            drawX = (maxWidth - lineWidth) / 2 + x
        } else if (horizontalAlign == 2) {
            lineWidth = this.fontMetrics.getWidthNoSprites(text) + extraWidth
            drawX = x + (maxWidth - lineWidth)
        }
        renderLineWithOffsets(text, drawX, drawY, sprites, spriteHeights, offsetsX, null)
        if (bounds != null) {
            if (lineWidth == -1) {
                lineWidth = this.fontMetrics.getWidthNoSprites(text) + extraWidth
            }
            bounds[0] = drawX
            bounds[1] = drawY - this.fontMetrics.topPadding
            bounds[2] = lineWidth
            bounds[3] = this.fontMetrics.bottomPadding + this.fontMetrics.topPadding
        }
        return extraWidth
    }

    fun updateTextEffects(effect: String) {
        try {
            if (effect.startsWith("col=")) {
                argbColorEffect = argbColorEffect and 0xffffff.inv() or (Utils.parse_int(effect.substring(4)) and 0xffffff)
            } else if ("/col" == effect) {
                argbColorEffect = argbColorEffect and 0xffffff.inv() or (colorArgb and 0xffffff)
            }
            if (effect.startsWith("argb=")) {
                argbColorEffect = Utils.parse_int(effect.substring(5))
            } else if ("/argb" == effect) {
                argbColorEffect = colorArgb
            } else if (effect.startsWith("str=")) {
                strikeThroughEffect = argbColorEffect and 0xffffff.inv() or Utils.parse_int(effect.substring(4))
            } else if ("str" == effect) {
                strikeThroughEffect = argbColorEffect and 0xffffff.inv() or 0x800000
            } else if ("/str" == effect) {
                strikeThroughEffect = -1
            } else if (effect.startsWith("u=")) {
                underlineEffect = argbColorEffect and 0xffffff.inv() or Utils.parse_int(effect.substring(2))
            } else if ("u" == effect) {
                underlineEffect = argbColorEffect and 0xffffff.inv()
            } else if ("/u" == effect) {
                underlineEffect = -1
            } else if ("shad=-1".equals(effect, ignoreCase = true)) {
                shadingEffect = 0
            } else if (effect.startsWith("shad=")) {
                shadingEffect = argbColorEffect and 0xffffff.inv() or Utils.parse_int(effect.substring(5))
            } else if ("shad" == effect) {
                shadingEffect = argbColorEffect and 0xffffff.inv()
            } else if ("/shad" == effect) {
                shadingEffect = originalShadowColor
            } else if ("br" == effect) {
                calibrate(colorArgb, originalShadowColor)
            }
        } catch (exception: Exception) {
        }
    }

    private fun drawInlineSprite(tag: String, x: Int, lineTop: Int): Int =
        InlineSprites.draw(toolkit, tag, x, lineTop + fontMetrics.verticalSpacing, fontMetrics.topPadding, fontMetrics.lineHeight())

    fun calculateJustifiedSpacing(line: String, width: Int) {
        var spaceCount = 0
        var inTag = false
        for (i in 0 until line.length) {
            val ch = line[i]
            if (ch == '<') {
                inTag = true
            } else if (ch == '>') {
                inTag = false
            } else if (!inTag && ch == ' ') {
                ++spaceCount
            }
        }
        if (spaceCount > 0) {
            justifiedSpaceWidth = (width - this.fontMetrics.getWidthNoSprites(line) shl 8) / spaceCount
        }
    }

    fun renderLine(text: String, x: Int, y: Int, sprites: Array<NativeSprite>?, spriteHeights: IntArray?, dimensions: ComponentSurface?, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        var currentX = x
        var currentY = y - this.fontMetrics.verticalSpacing
        var tagStart = -1
        var prevChar = -1
        val textLength = text.length
        for (i in 0 until textLength) {
            var ch = (Utils.byteForChar(text[i]).toInt() and 0xff).toChar()
            if (ch == '<') {
                tagStart = i
            } else {
                if (ch == '>' && tagStart != -1) {
                    val effect = text.substring(tagStart + 1, i)
                    tagStart = -1
                    if ("lt" == effect) {
                        ch = '<'
                    } else if ("gt" == effect) {
                        ch = '>'
                    } else if ("nbsp" == effect) {
                        ch = 160.toChar()
                    } else if ("shy" == effect) {
                        ch = 173.toChar()
                    } else if ("times" == effect) {
                        ch = 215.toChar()
                    } else if ("euro" == effect) {
                        ch = EURO_GLYPH.toChar()
                    } else if ("copy" == effect) {
                        ch = 169.toChar()
                    } else {
                        if ("reg" != effect) {
                            if (InlineSprites.isTag(effect)) {
                                currentX += drawInlineSprite(effect, currentX, currentY)
                                prevChar = -1
                            } else if (effect.startsWith("img=")) {
                                try {
                                    val spriteIndex = Utils.parseInt(effect.substring(4))
                                    val sprite = sprites!![spriteIndex]
                                    val spriteHeight = if (spriteHeights != null) spriteHeights[spriteIndex] else sprite.getSpriteHeight()
                                    if ((argbColorEffect and 0xffffff.inv()) == 0xffffff.inv()) {
                                        sprite.draw(currentX, currentY + this.fontMetrics.verticalSpacing - spriteHeight, 1, 0, 1)
                                    } else {
                                        sprite.draw(currentX, currentY + this.fontMetrics.verticalSpacing - spriteHeight, 0, argbColorEffect and 0xffffff.inv() or 0xffffff, 1)
                                    }
                                    currentX += sprites[spriteIndex].getSpriteWidth()
                                    prevChar = -1
                                } catch (ignored: Exception) {
                                }
                            } else {
                                updateTextEffects(effect)
                            }
                            continue
                        }
                        ch = 174.toChar()
                    }
                }
                if (tagStart == -1) {
                    if (prevChar != -1) {
                        currentX += this.fontMetrics.getKerning(prevChar, ch)
                    }
                    if (ch != ' ') {
                        if (dimensions == null) {
                            if ((shadingEffect and 0xffffff.inv()) != 0) {
                                drawChar(ch.code, currentX + 1, currentY + 1, shadingEffect, true)
                            }
                            drawChar(ch.code, currentX, currentY, argbColorEffect, false)
                        } else {
                            if ((shadingEffect and 0xffffff.inv()) != 0) {
                                drawChar(ch.code, currentX + 1, currentY + 1, shadingEffect, true, dimensions, surfaceOffsetX, surfaceOffsetY)
                            }
                            drawChar(ch.code, currentX, currentY, argbColorEffect, false, dimensions, surfaceOffsetX, surfaceOffsetY)
                        }
                    } else if (justifiedSpaceWidth > 0) {
                        justifiedSpaceAccumulator += justifiedSpaceWidth
                        currentX += justifiedSpaceAccumulator shr 8
                        justifiedSpaceAccumulator = justifiedSpaceAccumulator and 0xff
                    }
                    val glyphWidth = this.fontMetrics.getGlyphWidth(ch.code)
                    if (strikeThroughEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(currentX, currentY + (0.7 * this.fontMetrics.verticalSpacing).toInt(), glyphWidth, strikeThroughEffect)
                    }
                    if (underlineEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(currentX, currentY + this.fontMetrics.verticalSpacing + 1, glyphWidth, underlineEffect)
                    }
                    currentX += glyphWidth
                    prevChar = ch.code
                }
            }
        }
    }

    fun renderLineWithOffsets(text: String, x: Int, y: Int, sprites: Array<NativeSprite>?, spriteHeights: IntArray?, offsetsX: IntArray?, offsetsY: IntArray?) {
        var currentX = x
        var currentY = y - this.fontMetrics.verticalSpacing
        var tagStart = -1
        var prevChar = -1
        var offsetIndex = 0
        val textLength = text.length
        for (i in 0 until textLength) {
            var ch = (Utils.byteForChar(text[i]).toInt() and 0xff).toChar()
            if (ch == '<') {
                tagStart = i
            } else {
                if (ch == '>' && tagStart != -1) {
                    val effect = text.substring(tagStart + 1, i)
                    tagStart = -1
                    if ("lt" == effect) {
                        ch = '<'
                    } else if ("gt" == effect) {
                        ch = '>'
                    } else if ("nbsp" == effect) {
                        ch = 160.toChar()
                    } else if ("shy" == effect) {
                        ch = 173.toChar()
                    } else if ("times" == effect) {
                        ch = 215.toChar()
                    } else if ("euro" == effect) {
                        ch = EURO_GLYPH.toChar()
                    } else if ("copy" == effect) {
                        ch = 169.toChar()
                    } else {
                        if ("reg" != effect) {
                            if (InlineSprites.isTag(effect)) {
                                currentX += drawInlineSprite(effect, currentX, currentY)
                                prevChar = -1
                            } else if (effect.startsWith("img=")) {
                                try {
                                    val offsetX: Int
                                    val offsetY: Int
                                    if (offsetsX != null) {
                                        offsetX = offsetsX[offsetIndex]
                                    } else {
                                        offsetX = 0
                                    }
                                    if (offsetsY != null) {
                                        offsetY = offsetsY[offsetIndex]
                                    } else {
                                        offsetY = 0
                                    }
                                    offsetIndex++
                                    val spriteIndex = Utils.parseInt(effect.substring(4))
                                    val sprite = sprites!![spriteIndex]
                                    val spriteHeight = if (spriteHeights != null) spriteHeights[spriteIndex] else sprite.getSpriteHeight()
                                    sprite.draw(offsetX + currentX, currentY + this.fontMetrics.verticalSpacing - spriteHeight + offsetY, 1, 0, 1)
                                    currentX += sprites[spriteIndex].getSpriteWidth()
                                    prevChar = -1
                                } catch (exception: Exception) {
                                }
                            } else {
                                updateTextEffects(effect)
                            }
                            continue
                        }
                        ch = 174.toChar()
                    }
                }
                if (tagStart == -1) {
                    if (prevChar != -1) {
                        currentX += this.fontMetrics.getKerning(prevChar, ch)
                    }
                    val offsetX: Int
                    val offsetY: Int
                    if (offsetsX != null) {
                        offsetX = offsetsX[offsetIndex]
                    } else {
                        offsetX = 0
                    }
                    if (offsetsY != null) {
                        offsetY = offsetsY[offsetIndex]
                    } else {
                        offsetY = 0
                    }
                    ++offsetIndex
                    if (ch != ' ') {
                        if ((shadingEffect and 0xffffff.inv()) != 0) {
                            drawChar(ch.code, offsetX + currentX + 1, currentY + offsetY + 1, shadingEffect, true)
                        }
                        drawChar(ch.code, offsetX + currentX, currentY + offsetY, argbColorEffect, false)
                    } else if (justifiedSpaceWidth > 0) {
                        justifiedSpaceAccumulator += justifiedSpaceWidth
                        currentX += justifiedSpaceAccumulator shr 8
                        justifiedSpaceAccumulator = justifiedSpaceAccumulator and 0xff
                    }
                    val glyphWidth = this.fontMetrics.getGlyphWidth(ch.code)
                    if (strikeThroughEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(currentX, currentY + (this.fontMetrics.verticalSpacing * 0.7).toInt(), glyphWidth, strikeThroughEffect)
                    }
                    if (underlineEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(currentX, currentY + this.fontMetrics.verticalSpacing, glyphWidth, underlineEffect)
                    }
                    currentX += glyphWidth
                    prevChar = ch.code
                }
            }
        }
    }

    abstract fun drawChar(charIndex: Int, drawX: Int, drawY: Int, colorArgb: Int, isShadow: Boolean)

    fun renderText(text: String?, x: Int, y: Int, width: Int, height: Int, textColor: Int, shadowColor: Int, horizontalAlign: Int, verticalAlign: Int, lineSpacing: Int, maxLines: Int, sprites: Array<NativeSprite>?, spriteHeights: IntArray?, dimensions: ComponentSurface?, surfaceOffsetX: Int, surfaceOffsetY: Int): Int {
        var currentLineSpacing = lineSpacing
        var maxLinesToShow = maxLines
        var currentVerticalAlign = verticalAlign
        if (text == null) {
            return 0
        }
        calibrate(textColor, shadowColor)
        if (currentLineSpacing == 0) {
            currentLineSpacing = this.fontMetrics.verticalSpacing
        }
        val lineWidths: IntArray?
        if (height < currentLineSpacing + this.fontMetrics.bottomPadding + this.fontMetrics.topPadding && height < currentLineSpacing + currentLineSpacing) {
            lineWidths = null
        } else {
            lineWidths = intArrayOf(width)
        }
        var numLines = this.fontMetrics.splitLinesToBuffer(text, lineWidths, LINE_BUFFER, sprites as Array<Sprite>?)
        if (maxLinesToShow == -1) {
            maxLinesToShow = height / currentLineSpacing
            if (maxLinesToShow <= 0) {
                maxLinesToShow = 1
            }
        }
        if (maxLinesToShow > 0 && numLines >= maxLinesToShow) {
            LINE_BUFFER[maxLinesToShow - 1] = this.fontMetrics.fitText(LINE_BUFFER[maxLinesToShow - 1]!!, width, sprites as Array<Sprite>?)
            numLines = maxLinesToShow
        }
        if (numLines <= 0) {
            return 0
        }
        if (currentVerticalAlign == 3 && numLines == 1) {
            currentVerticalAlign = 1
        }
        val above = IntArray(numLines)
        val below = IntArray(numLines)
        val extents = IntArray(2)
        var inlineExtra = 0
        for (i in 0 until numLines) {
            InlineSprites.extents(LINE_BUFFER[i]!!, this.fontMetrics.lineHeight(), extents)
            above[i] = extents[0]
            below[i] = extents[1]
            inlineExtra += extents[0] + extents[1]
        }
        val firstToLast = currentLineSpacing * (numLines - 1) + inlineExtra - above[0] - below[numLines - 1]
        val startY: Int
        if (currentVerticalAlign == 0) {
            startY = y + this.fontMetrics.topPadding + above[0]
        } else if (currentVerticalAlign == 1) {
            startY = (height - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding - currentLineSpacing * (numLines - 1) - inlineExtra) / 2 + y + this.fontMetrics.topPadding + above[0]
        } else if (currentVerticalAlign == 2) {
            startY = y + height - this.fontMetrics.bottomPadding - below[numLines - 1] - firstToLast
        } else {
            var lineSpacingToAdd = (height - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding - currentLineSpacing * (numLines - 1) - inlineExtra) / (numLines + 1)
            if (lineSpacingToAdd < 0) {
                lineSpacingToAdd = 0
            }
            startY = y + this.fontMetrics.topPadding + lineSpacingToAdd + above[0]
            currentLineSpacing += lineSpacingToAdd
        }
        var renderY = startY
        for (i in 0 until numLines) {
            if (horizontalAlign == 0) {
                renderLine(LINE_BUFFER[i]!!, x, renderY, sprites, spriteHeights, dimensions, surfaceOffsetX, surfaceOffsetY)
            } else if (horizontalAlign == 1) {
                renderLine(LINE_BUFFER[i]!!, x + (width - this.fontMetrics.getWidthNoSprites(LINE_BUFFER[i]!!)) / 2, renderY, sprites, spriteHeights, dimensions, surfaceOffsetX, surfaceOffsetY)
            } else if (horizontalAlign == 2) {
                renderLine(LINE_BUFFER[i]!!, x + width - this.fontMetrics.getWidthNoSprites(LINE_BUFFER[i]!!), renderY, sprites, spriteHeights, dimensions, surfaceOffsetX, surfaceOffsetY)
            } else if (i == numLines - 1) {
                renderLine(LINE_BUFFER[i]!!, x, renderY, sprites, spriteHeights, dimensions, surfaceOffsetX, surfaceOffsetY)
            } else {
                calculateJustifiedSpacing(LINE_BUFFER[i]!!, width)
                renderLine(LINE_BUFFER[i]!!, x, renderY, sprites, spriteHeights, dimensions, surfaceOffsetX, surfaceOffsetY)
                justifiedSpaceWidth = 0
            }
            renderY += currentLineSpacing + below[i] + if (i + 1 < numLines) above[i + 1] else 0
        }
        return numLines
    }

    abstract fun drawChar(charIndex: Int, drawX: Int, drawY: Int, colorArgb: Int, isShadow: Boolean, dimension: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int)

    fun drawText(text: String?, x: Int, y: Int, width: Int, height: Int, textColor: Int, shadowColor: Int, horizontalAlign: Int, verticalAlign: Int, lineSpacing: Int, sprites: Array<NativeSprite>?, spriteHeights: IntArray?, dimensions: ComponentSurface?, surfaceOffsetX: Int, surfaceOffsetY: Int): Int {
        return renderText(text, x, y, width, height, textColor, shadowColor, horizontalAlign, verticalAlign, lineSpacing, 0, sprites, spriteHeights, dimensions, surfaceOffsetX, surfaceOffsetY)
    }

    fun renderCenteredShake(text: String?, x: Int, y: Int, color: Int, shakeSeed: Int, effectProgress: Int) {
        if (text != null) {
            calibrate(color, 0xffffff.inv())
            var amplitude = 7.0 - effectProgress / 8.0
            if (amplitude < 0.0) {
                amplitude = 0.0
            }
            val textLength = text.length
            val offsetsY = IntArray(textLength)
            for (i in 0 until textLength) {
                offsetsY[i] = (Math.sin(shakeSeed + i / 1.5) * amplitude).toInt()
            }
            renderLineWithOffsets(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, null, offsetsY)
        }
    }

    fun renderAntiMacroText(text: String?, x: Int, y: Int, color: Int, random: Random, seed: Int, sprites: Array<NativeSprite>?, spriteHeights: IntArray?): Int {
        if (text == null) {
            return 0
        }
        random.setSeed(seed.toLong())
        val alpha = 192 + (random.nextInt() and 0x1f)
        calibrate(alpha shl 24 or (color and 0xffffff), alpha shl 24)
        val textLength = text.length
        val offsetsX = IntArray(textLength)
        var extraWidth = 0
        for (i in 0 until textLength) {
            offsetsX[i] = extraWidth
            if ((random.nextInt() and 0x3) == 0) {
                extraWidth++
            }
        }
        renderLineWithOffsets(text, x, y, sprites, spriteHeights, offsetsX, null)
        return extraWidth
    }

    fun renderCenteredWave(text: String?, x: Int, y: Int, color: Int, waveSeed: Int) {
        if (text != null) {
            calibrate(color, 0xffffff.inv())
            val textLength = text.length
            val offsetsY = IntArray(textLength)
            for (i in 0 until textLength) {
                offsetsY[i] = (Math.sin(waveSeed / 5.0 + i / 2.0) * 5.0).toInt()
            }
            renderLineWithOffsets(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, null, offsetsY)
        }
    }

    fun renderCenteredWave2(text: String?, x: Int, y: Int, color: Int, waveSeed: Int) {
        if (text != null) {
            calibrate(color, 0xffffff.inv())
            val textLength = text.length
            val offsetsX = IntArray(textLength)
            val offsetsY = IntArray(textLength)
            for (i in 0 until textLength) {
                offsetsX[i] = (Math.sin(waveSeed / 5.0 + i / 5.0) * 5.0).toInt()
                offsetsY[i] = (Math.sin(waveSeed / 5.0 + i / 3.0) * 5.0).toInt()
            }
            renderLineWithOffsets(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, offsetsX, offsetsY)
        }
    }
}
