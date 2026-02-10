package com.jagex.game.runetek5.graphics.font

import java.util.Random

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.Sprite
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.Utils

/**
 *
 * @author Brainpower - 21/03/2023
 *
 */
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

    fun calibrate(textColor: Int, shadowEffect: Int) {
        var shadeEff = shadowEffect
        strikeThroughEffect = -1
        underlineEffect = -1
        colorArgb = textColor
        argbColorEffect = textColor
        justifiedSpaceWidth = 0
        justifiedSpaceAccumulator = 0
        if (shadeEff == -1) {
            shadeEff = 0
        }
        originalShadowColor = shadeEff
        Companion.shadingEffect = shadeEff
    }

    fun renderTextAntiMacro(chatText: String?, x: Int, y: Int, maxWidth: Int, maxHeight: Int, textEffectParam1: Int,
                            textEffectParam2: Int, horizontalAlign: Int, verticalAlign: Int, random: Random, seed: Int, dimensions: IntArray?,
                            chatIcons: Array<NativeSprite>?, chatScaleHeights: IntArray?): Int {
        if (chatText == null) {
            return 0
        }
        random.setSeed(seed.toLong())
        val textColor = 192 + (random.nextInt() and 0x1f)
        calibrate(textColor shl 24 or (textEffectParam1 and 0xffffff),
                if (textEffectParam2 == -1) 0 else textColor shl 24 or (textEffectParam2 and 0xffffff))
        val textLength = chatText.length
        val glyphWidths = IntArray(textLength)
        var textWidth = 0
        for (index in 0 until textLength) {
            glyphWidths[index] = textWidth
            if ((random.nextInt() and 0x3) == 0) {
                textWidth++
            }
        }
        var drawX = x
        var drawY = y + this.fontMetrics.topPadding
        var plainWidth = -1
        if (verticalAlign == 1) {
            drawY += (maxHeight - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding) / 2
        } else if (verticalAlign == 2) {
            drawY = y + maxHeight - this.fontMetrics.bottomPadding
        }
        if (horizontalAlign == 1) {
            plainWidth = this.fontMetrics.getWidthNoSprites(chatText) + textWidth
            drawX = (maxWidth - plainWidth) / 2 + x
        } else if (horizontalAlign == 2) {
            plainWidth = this.fontMetrics.getWidthNoSprites(chatText) + textWidth
            drawX = x + (maxWidth - plainWidth)
        }
        renderLineWithOffsets(chatText, drawX, drawY, chatIcons, chatScaleHeights, glyphWidths, null)
        if (dimensions != null) {
            if (plainWidth == -1) {
                plainWidth = this.fontMetrics.getWidthNoSprites(chatText) + textWidth
            }
            dimensions[0] = drawX
            dimensions[1] = drawY - this.fontMetrics.topPadding
            dimensions[2] = plainWidth
            dimensions[3] = this.fontMetrics.bottomPadding + this.fontMetrics.topPadding
        }
        return textWidth
    }

    fun updateTextEffects(effectString: String) {
        try {
            if (effectString.startsWith("col=")) {
                argbColorEffect = argbColorEffect and 0xffffff.inv() or (Utils.parse_int(effectString.substring(4)) and 0xffffff)
            } else if ("/col" == effectString) {
                argbColorEffect = argbColorEffect and 0xffffff.inv() or (colorArgb and 0xffffff)
            }
            if ("argb=" == effectString) {
                argbColorEffect = Utils.parse_int(effectString.substring(5))
            } else if ("/argb" == effectString) {
                argbColorEffect = colorArgb
            } else if ("str=" == effectString) {
                strikeThroughEffect = argbColorEffect and 0xffffff.inv() or Utils.parse_int(effectString.substring(4))
            } else if ("str" == effectString) {
                strikeThroughEffect = argbColorEffect and 0xffffff.inv() or 0x800000
            } else if ("/str" == effectString) {
                strikeThroughEffect = -1
            } else if ("u=" == effectString) {
                underlineEffect = argbColorEffect and 0xffffff.inv() or Utils.parse_int(effectString.substring(2))
            } else if ("u" == effectString) {
                underlineEffect = argbColorEffect and 0xffffff.inv()
            } else if ("/u" == effectString) {
                underlineEffect = -1
            } else if ("shad=-1".equals(effectString, ignoreCase = true)) {
                Companion.shadingEffect = 0
            } else if ("shad=" == effectString) {
                Companion.shadingEffect = argbColorEffect and 0xffffff.inv() or Utils.parse_int(effectString.substring(5))
            } else if ("shad" == effectString) {
                Companion.shadingEffect = argbColorEffect and 0xffffff.inv()
            } else if ("/shad" == effectString) {
                Companion.shadingEffect = originalShadowColor
            } else if ("br" == effectString) {
                calibrate(colorArgb, originalShadowColor)
            }
        } catch (exception: Exception) {
            // NOOP
        }
    }

    fun calculateJustifiedSpacing(string_1: String, i_2: Int) {
        var i_4 = 0
        var bool5 = false
        for (i_6 in 0 until string_1.length) {
            val var_7 = string_1[i_6]
            if (var_7 == '<') {
                bool5 = true
            } else if (var_7 == '>') {
                bool5 = false
            } else if (!bool5 && var_7 == ' ') {
                ++i_4
            }
        }
        if (i_4 > 0) {
            justifiedSpaceWidth = (i_2 - this.fontMetrics.getWidthNoSprites(string_1) shl 8) / i_4
        }
    }

    fun renderLine(text: String, x: Int, y: Int, sprites: Array<NativeSprite>?, spriteHeights: IntArray?, dimensions: ComponentSurface?, shadingEffect: Int, colorEffect: Int) {
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
                    val string_15 = text.substring(tagStart + 1, i)
                    tagStart = -1
                    if ("lt" == string_15) {
                        ch = '<'
                    } else if ("gt" == string_15) {
                        ch = '>'
                    } else if ("nbsp" == string_15) {
                        ch = 160.toChar()
                    } else if ("shy" == string_15) {
                        ch = 173.toChar()
                    } else if ("times" == string_15) {
                        ch = 215.toChar()
                    } else if ("euro" == string_15) {
                        ch = 8364.toChar()
                    } else if ("copy" == string_15) {
                        ch = 169.toChar()
                    } else {
                        if ("reg" != string_15) {
                            if (string_15.startsWith("img=")) {
                                try {
                                    val spriteIndex = Utils.parseInt(string_15.substring(4))
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
                                updateTextEffects(string_15)
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
                            if ((Companion.shadingEffect and 0xffffff.inv()) != 0) {
                                drawChar(ch.code, currentX + 1, currentY + 1, Companion.shadingEffect, true)
                            }
                            drawChar(ch.code, currentX, currentY, argbColorEffect, false)
                        } else {
                            if ((Companion.shadingEffect and 0xffffff.inv()) != 0) {
                                drawChar(ch.code, currentX + 1, currentY + 1, Companion.shadingEffect, true, dimensions, shadingEffect, colorEffect)
                            }
                            drawChar(ch.code, currentX, currentY, argbColorEffect, false, dimensions, shadingEffect, colorEffect)
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

    fun renderLineWithOffsets(chatText: String, drawX: Int, drawY: Int, chatIcons: Array<NativeSprite>?, chatScaleHeights: IntArray?, widths: IntArray?, heights: IntArray?) {
        var currentX = drawX
        var currentY = drawY - this.fontMetrics.verticalSpacing
        var beginIndex = -1
        var i_10 = -1
        var imageIndex = 0
        val textLength = chatText.length
        for (charIndex in 0 until textLength) {
            var reference = (Utils.byteForChar(chatText[charIndex]).toInt() and 0xff).toChar()
            if (reference == '<') {
                beginIndex = charIndex
            } else {
                if (reference == '>' && beginIndex != -1) {
                    val effect = chatText.substring(beginIndex + 1, charIndex)
                    beginIndex = -1
                    if ("lt" == effect) {
                        reference = '<'
                    } else if ("gt" == effect) {
                        reference = '>'
                    } else if ("nbsp" == effect) {
                        reference = 160.toChar()
                    } else if ("shy" == effect) {
                        reference = 173.toChar()
                    } else if ("times" == effect) {
                        reference = 215.toChar()
                    } else if ("euro" == effect) {
                        reference = 8364.toChar()
                    } else if ("copy" == effect) {
                        reference = 169.toChar()
                    } else {
                        if ("reg" != effect) {
                            if (effect.startsWith("img=")) {
                                try {
                                    val w: Int
                                    val h: Int
                                    if (widths != null) {
                                        w = widths[imageIndex]
                                    } else {
                                        w = 0
                                    }
                                    if (heights != null) {
                                        h = heights[imageIndex]
                                    } else {
                                        h = 0
                                    }
                                    imageIndex++
                                    val iconIndex = Utils.parseInt(effect.substring(4))
                                    val chatIcon = chatIcons!![iconIndex]
                                    val scaleHeight = if (chatScaleHeights != null) chatScaleHeights[iconIndex] else chatIcon.getSpriteHeight()
                                    chatIcon.draw(w + currentX, currentY + this.fontMetrics.verticalSpacing - scaleHeight + h, 1, 0, 1)
                                    currentX += chatIcons[iconIndex].getSpriteWidth()
                                    i_10 = -1
                                } catch (exception: Exception) {
                                    // NOOP
                                }
                            } else {
                                updateTextEffects(effect)
                            }
                            continue
                        }
                        reference = 174.toChar()
                    }
                }
                if (beginIndex == -1) {
                    if (i_10 != -1) {
                        currentX += this.fontMetrics.getKerning(i_10, reference)
                    }
                    val w: Int
                    val h: Int
                    if (widths != null) {
                        w = widths[imageIndex]
                    } else {
                        w = 0
                    }
                    if (heights != null) {
                        h = heights[imageIndex]
                    } else {
                        h = 0
                    }
                    ++imageIndex
                    if (reference != ' ') {
                        if ((shadingEffect and 0xffffff.inv()) != 0) {
                            drawChar(reference.code, w + currentX + 1, currentY + h + 1, shadingEffect, true)
                        }
                        drawChar(reference.code, w + currentX, currentY + h, argbColorEffect, false)
                    } else if (justifiedSpaceWidth > 0) {
                        justifiedSpaceAccumulator += justifiedSpaceWidth
                        currentX += justifiedSpaceAccumulator shr 8
                        justifiedSpaceAccumulator = justifiedSpaceAccumulator and 0xff
                    }
                    val glyphWidth = this.fontMetrics.getGlyphWidth(reference.code)
                    if (strikeThroughEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(currentX, currentY + (this.fontMetrics.verticalSpacing * 0.7).toInt(), glyphWidth, strikeThroughEffect)
                    }
                    if (underlineEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(currentX, currentY + this.fontMetrics.verticalSpacing, glyphWidth, underlineEffect)
                    }
                    currentX += glyphWidth
                    i_10 = reference.code
                }
            }
        }
    }

    abstract fun drawChar(var1: Int, var2: Int, var3: Int, var4: Int, var5: Boolean)

    fun renderText(text: String?, x: Int, y: Int, width: Int, height: Int, textColor: Int, shadowColor: Int, alignment: Int, verticalAlignment: Int, lineSpacing: Int, maxLines: Int, sprites: Array<NativeSprite>?, spriteIndices: IntArray?, dimensions: ComponentSurface?, leftPadding: Int, topPadding: Int): Int {
        var currentLineSpacing = lineSpacing
        var maxLinesToShow = maxLines
        var verticalAlignmentType = verticalAlignment
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
        if (verticalAlignmentType == 3 && numLines == 1) {
            verticalAlignmentType = 1
        }
        val currentY: Int
        if (verticalAlignmentType == 0) {
            currentY = y + this.fontMetrics.topPadding
        } else if (verticalAlignmentType == 1) {
            currentY = (height - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding - currentLineSpacing * (numLines - 1)) / 2 + y + this.fontMetrics.topPadding
        } else if (verticalAlignmentType == 2) {
            currentY = y + height - this.fontMetrics.bottomPadding - currentLineSpacing * (numLines - 1)
        } else {
            var lineSpacingToAdd = (height - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding - currentLineSpacing * (numLines - 1)) / (numLines + 1)
            if (lineSpacingToAdd < 0) {
                lineSpacingToAdd = 0
            }
            currentY = y + this.fontMetrics.topPadding + lineSpacingToAdd
            currentLineSpacing += lineSpacingToAdd
        }
        var renderY = currentY
        for (i in 0 until numLines) {
            if (alignment == 0) {
                renderLine(LINE_BUFFER[i]!!, x, renderY, sprites, spriteIndices, dimensions, leftPadding, topPadding)
            } else if (alignment == 1) {
                renderLine(LINE_BUFFER[i]!!, x + (width - this.fontMetrics.getWidthNoSprites(LINE_BUFFER[i]!!)) / 2, renderY, sprites, spriteIndices, dimensions, leftPadding, topPadding)
            } else if (alignment == 2) {
                renderLine(LINE_BUFFER[i]!!, x + width - this.fontMetrics.getWidthNoSprites(LINE_BUFFER[i]!!), renderY, sprites, spriteIndices, dimensions, leftPadding, topPadding)
            } else if (i == numLines - 1) {
                renderLine(LINE_BUFFER[i]!!, x, renderY, sprites, spriteIndices, dimensions, leftPadding, topPadding)
            } else {
                calculateJustifiedSpacing(LINE_BUFFER[i]!!, width)
                renderLine(LINE_BUFFER[i]!!, x, renderY, sprites, spriteIndices, dimensions, leftPadding, topPadding)
                justifiedSpaceWidth = 0
            }
            renderY += currentLineSpacing
        }
        return numLines
    }

    abstract fun drawChar(var1: Int, var2: Int, var3: Int, var4: Int, var5: Boolean, dimension: ComponentSurface, shadingEffect: Int, colorEffect: Int)

    fun drawText(text: String?, x: Int, y: Int, width: Int, height: Int, textColor: Int, shadowColor: Int, allignment: Int, verticalAllignment: Int, lineSpacing: Int, sprites: Array<NativeSprite>?, spriteIndices: IntArray?, dimensions: ComponentSurface?, leftPadding: Int, topPadding: Int): Int {
        return renderText(text, x, y, width, height, textColor, shadowColor, allignment, verticalAllignment, lineSpacing, 0, sprites, spriteIndices, dimensions, leftPadding, topPadding)
    }

    fun renderCenteredShake(text: String?, x: Int, y: Int, color: Int, i_6: Int, i_7: Int) {
        if (text != null) {
            calibrate(color, 0xffffff.inv())
            var step = 7.0 - i_7 / 8.0
            if (step < 0.0) {
                step = 0.0
            }
            val length = text.length
            val shakeOffsetsY = IntArray(length)
            for (i in 0 until length) {
                shakeOffsetsY[i] = (Math.sin(i_6 + i / 1.5) * step).toInt()
            }
            renderLineWithOffsets(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, null, shakeOffsetsY)
        }
    }

    fun renderAntiMacroText(text: String?, x: Int, y: Int, i_4: Int, random: Random, randomSeed: Int, sprites: Array<NativeSprite>?, scaleHeights: IntArray?): Int {
        if (text == null) {
            return 0
        }
        random.setSeed(randomSeed.toLong())
        val color = 192 + (random.nextInt() and 0x1f)
        calibrate(color shl 24 or (i_4 and 0xffffff), color shl 24)
        val length = text.length
        val heights = IntArray(length)
        var height = 0
        for (i in 0 until length) {
            heights[i] = height
            if ((random.nextInt() and 0x3) == 0) {
                height++
            }
        }
        renderLineWithOffsets(text, x, y, sprites, scaleHeights, heights, null)
        return height
    }

    // Only vertical
    fun renderCenteredWave(text: String?, x: Int, y: Int, color: Int, waveSeed: Int) {
        if (text != null) {
            calibrate(color, 0xffffff.inv())
            val length = text.length
            val waveOffsetsY = IntArray(length)
            for (i in 0 until length) {
                waveOffsetsY[i] = (Math.sin(waveSeed / 5.0 + i / 2.0) * 5.0).toInt()
            }
            renderLineWithOffsets(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, null, waveOffsetsY)
        }
    }

    // Horizontal & vertical
    fun renderCenteredWave2(text: String?, x: Int, y: Int, color: Int, waveSeed: Int) {
        if (text != null) {
            calibrate(color, 0xffffff.inv())
            val length = text.length
            val waveOffsetsX = IntArray(length)
            val waveOffsetsY = IntArray(length)
            for (i in 0 until length) {
                waveOffsetsX[i] = (Math.sin(waveSeed / 5.0 + i / 5.0) * 5.0).toInt()
                waveOffsetsY[i] = (Math.sin(waveSeed / 5.0 + i / 3.0) * 5.0).toInt()
            }
            renderLineWithOffsets(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, waveOffsetsX, waveOffsetsY)
        }
    }
}
