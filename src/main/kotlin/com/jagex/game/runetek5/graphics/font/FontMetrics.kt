package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.graphics.sprite.Sprite
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.utils.Utils
import java.awt.Point

class FontMetrics(bytes: ByteArray) {
    @JvmField
    var verticalSpacing: Int = 0
    @JvmField
    var topPadding: Int = 0
    @JvmField
    var bottomPadding: Int = 0
    @JvmField
    var glyphWidths: ByteArray = ByteArray(0)
    @JvmField
    var kerning: Array<ByteArray>? = null

    init {
        val stream = Packet(bytes)
        val check = stream.readUByte()
        if (check != 0) {
            throw RuntimeException("")
        } else {
            val variadicWidth = stream.readUByte() == 1
            glyphWidths = ByteArray(256)
            stream.readBytes(glyphWidths, 0, 256)

            if (variadicWidth) {
                val widths = IntArray(256)
                val heights = IntArray(256)

                for (i in 0 until 256) {
                    widths[i] = stream.readUByte()
                }

                for (i in 0 until 256) {
                    heights[i] = stream.readUByte()
                }

                val horizontalOffsets = Array(256) { ByteArray(0) }

                for (i in 0 until 256) {
                    horizontalOffsets[i] = ByteArray(widths[i])
                    var offset: Byte = 0

                    for (j in horizontalOffsets[i].indices) {
                        offset = (offset + stream.readByte()).toByte()
                        horizontalOffsets[i][j] = offset
                    }
                }

                val verticalOffsets = Array(256) { ByteArray(0) }

                for (i in 0 until 256) {
                    verticalOffsets[i] = ByteArray(widths[i])
                    var offset: Byte = 0

                    for (j in verticalOffsets[i].indices) {
                        offset = (offset + stream.readByte()).toByte()
                        verticalOffsets[i][j] = offset
                    }
                }

                kerning = Array(256) { ByteArray(256) }

                for (i in 0 until 256) {
                    if (i != 32 && i != 160) {
                        for (j in 0 until 256) {
                            if (j != 32 && j != 160) {
                                kerning!![i][j] = Utils.calculateKerning(
                                    horizontalOffsets, verticalOffsets, heights,
                                    glyphWidths, widths, i, j
                                ).toByte()
                            }
                        }
                    }
                }

                verticalSpacing = heights[32] + widths[32]
            } else {
                verticalSpacing = stream.readUByte()
            }

            stream.readUByte()
            stream.readUByte()
            topPadding = stream.readUByte()
            bottomPadding = stream.readUByte()
        }
    }

    fun getKerning(i: Int, ch: Char): Int {
        return if (kerning != null) kerning!![i][ch.code].toInt() else 0
    }

    fun getGlyphWidth(i: Int): Int {
        return glyphWidths[i].toInt() and 0xff
    }

    fun getWidthNoSprites(text: String): Int {
        return getTextWidth(text, null)
    }

    fun getTextWidth(text: String?, sprites: Array<Sprite>?): Int {
        if (text == null) {
            return 0
        }

        var tagStart = -1
        var previousChar = -1
        var width = 0
        val length = text.length

        for (i in 0 until length) {
            var ch = text[i]
            if (ch == '<') {
                tagStart = i
            } else {
                if (ch == '>' && tagStart != -1) {
                    val tag = text.substring(tagStart + 1, i)
                    tagStart = -1

                    when (tag) {
                        "lt" -> ch = '<'
                        "gt" -> ch = '>'
                        "nbsp" -> ch = 160.toChar()
                        "shy" -> ch = 173.toChar()
                        "times" -> ch = 215.toChar()
                        "euro" -> ch = 8364.toChar()
                        "copy" -> ch = 169.toChar()
                        "reg" -> ch = 174.toChar()
                        else -> {
                            if (tag.startsWith("img=") && sprites != null) {
                                try {
                                    val id = Utils.parseInt(tag.substring(4))
                                    width += sprites[id].getSpriteWidth()
                                    previousChar = -1
                                } catch (ignored: Exception) {
                                }
                            }
                            continue
                        }
                    }
                }

                if (tagStart == -1) {
                    width += glyphWidths[Utils.byteForChar(ch).toInt() and 0xff].toInt() and 0xff
                    if (kerning != null && previousChar != -1) {
                        width += kerning!![previousChar][ch.code]
                    }
                    previousChar = ch.code
                }
            }
        }

        return width
    }

    fun getLineCount(text: String, width: Int, sprites: Array<Sprite>?): Int {
        return splitLinesToBuffer(text, intArrayOf(width), lineBuffer, sprites)
    }

    fun splitLinesToBuffer(
        areaName: String?,
        widths: IntArray?,
        buffer: Array<String?>,
        sprites: Array<Sprite>?,
        includeTrailingSpace: Boolean
    ): Int {
        if (areaName == null) {
            return 0
        }

        var currentWidth = 0
        var lineStart = 0
        var lastSpace = -1
        var lastSpaceWidth = 0
        var spaceTrailing = 0
        var tagStart = -1
        var previousChar = -1
        var lineCount = 0
        val length = areaName.length

        for (i in 0 until length) {
            val charCode = Utils.byteForChar(areaName[i]).toInt() and 0xff
            var charWidth = 0

            if (charCode == 60) {
                tagStart = i
            } else {
                if (tagStart != -1) {
                    if (charCode != 62) {
                        continue
                    }

                    val tagPos = tagStart
                    val tag = areaName.substring(tagStart + 1, i)
                    tagStart = -1

                    if (tag == "br") {
                        buffer[lineCount] = areaName.substring(lineStart, i + 1)
                        lineCount++
                        if (lineCount >= buffer.size) {
                            return 0
                        }

                        lineStart = i + 1
                        currentWidth = 0
                        lastSpace = -1
                        previousChar = -1
                        continue
                    }

                    when (tag) {
                        "lt" -> {
                            charWidth += getGlyphWidth(60)
                            if (kerning != null && previousChar != -1) {
                                charWidth += kerning!![previousChar][60]
                            }
                            previousChar = 60
                        }
                        "gt" -> {
                            charWidth += getGlyphWidth(62)
                            if (kerning != null && previousChar != -1) {
                                charWidth += kerning!![previousChar][62]
                            }
                            previousChar = 62
                        }
                        "nbsp" -> {
                            charWidth += getGlyphWidth(160)
                            if (kerning != null && previousChar != -1) {
                                charWidth += kerning!![previousChar][160]
                            }
                            previousChar = 160
                        }
                        "shy" -> {
                            charWidth += getGlyphWidth(173)
                            if (kerning != null && previousChar != -1) {
                                charWidth += kerning!![previousChar][173]
                            }
                            previousChar = 173
                        }
                        "times" -> {
                            charWidth += getGlyphWidth(215)
                            if (kerning != null && previousChar != -1) {
                                charWidth += kerning!![previousChar][215]
                            }
                            previousChar = 215
                        }
                        "euro" -> {
                            charWidth += getGlyphWidth(8364)
                            if (kerning != null && previousChar != -1) {
                                charWidth += kerning!![previousChar][8364]
                            }
                            previousChar = 8364
                        }
                        "copy" -> {
                            charWidth += getGlyphWidth(169)
                            if (kerning != null && previousChar != -1) {
                                charWidth += kerning!![previousChar][169]
                            }
                            previousChar = 169
                        }
                        "reg" -> {
                            charWidth += getGlyphWidth(174)
                            if (kerning != null && previousChar != -1) {
                                charWidth += kerning!![previousChar][174]
                            }
                            previousChar = 174
                        }
                        else -> {
                            if (tag.startsWith("img=") && sprites != null) {
                                try {
                                    val id = Utils.parseInt(tag.substring(4))
                                    charWidth += sprites[id].getSpriteWidth()
                                    previousChar = -1
                                } catch (ignored: Exception) {
                                }
                            }
                        }
                    }
                } else {
                    charWidth += getGlyphWidth(charCode)
                    if (kerning != null && previousChar != -1) {
                        charWidth += kerning!![previousChar][charCode]
                    }
                    previousChar = charCode
                }

                if (charWidth > 0) {
                    currentWidth += charWidth
                    if (widths != null) {
                        if (charCode == 32) {
                            lastSpace = i
                            lastSpaceWidth = currentWidth
                            spaceTrailing = if (includeTrailingSpace) 1 else 0
                        }

                        val maxWidth = widths[if (lineCount < widths.size) lineCount else widths.size - 1]
                        if (currentWidth > maxWidth) {
                            if (lastSpace >= 0) {
                                buffer[lineCount] = areaName.substring(lineStart, lastSpace + 1 - spaceTrailing)
                                lineCount++
                                if (lineCount >= buffer.size) {
                                    return 0
                                }

                                lineStart = lastSpace + 1
                                lastSpace = -1
                                currentWidth -= lastSpaceWidth
                            } else {
                                buffer[lineCount] = areaName.substring(lineStart, i)
                                lineCount++
                                if (lineCount >= buffer.size) {
                                    return 0
                                }

                                lineStart = i
                                lastSpace = -1
                                currentWidth = charWidth
                            }
                            previousChar = -1
                        }

                        if (charCode == 45) {
                            lastSpace = i
                            lastSpaceWidth = currentWidth
                            spaceTrailing = 0
                        }
                    }
                }
            }
        }

        if (areaName.length > lineStart) {
            buffer[lineCount] = areaName.substring(lineStart)
            lineCount++
        }

        return lineCount
    }

    fun getTextWidthMultiline(text: String, width: Int, sprites: Array<Sprite>?): Int {
        val lineCount = splitLinesToBuffer(text, intArrayOf(width), lineBuffer, sprites)
        var maxWidth = 0

        for (i in 0 until lineCount) {
            val lineWidth = getTextWidth(lineBuffer[i], sprites)
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth
            }
        }

        return maxWidth
    }

    fun getPositionAtIndex(text: String, width: Int, height: Int, index: Int, sprites: Array<Sprite>?): Point? {
        var adjustedIndex = index
        var adjustedHeight = height

        if (adjustedIndex <= 0) {
            return Point(0, adjustedHeight + topPadding)
        }

        if (adjustedIndex > text.length) {
            adjustedIndex = text.length
        }

        if (adjustedHeight == 0) {
            adjustedHeight = verticalSpacing
        }

        val lineCount = splitLinesToBuffer(text, intArrayOf(width), lineBuffer, sprites, false)
        var charCount = 0

        for (i in 0 until lineCount) {
            val lineLength = lineBuffer[i]!!.length
            if (charCount + lineLength > adjustedIndex || i == lineCount - 1 && adjustedIndex == text.length) {
                var substring = text.substring(charCount, adjustedIndex)
                if (substring.endsWith("<br>")) {
                    substring = ""
                }

                val lineWidth = getTextWidth(substring, sprites)
                val y = topPadding + adjustedHeight * i
                return Point(lineWidth, y)
            }

            charCount += lineLength
        }

        return null
    }

    fun getIndexAtPosition(text: String, width: Int, lineHeight: Int, x: Int, y: Int, sprites: Array<Sprite>?): Int {
        var adjustedLineHeight = lineHeight
        var adjustedY = y

        if (adjustedLineHeight == 0) {
            adjustedLineHeight = verticalSpacing
        }

        val lineCount = splitLinesToBuffer(text, intArrayOf(width), lineBuffer, sprites)
        if (lineCount == 0) {
            return 0
        }

        if (adjustedY < 0) {
            adjustedY = 0
        }

        adjustedY /= adjustedLineHeight
        if (adjustedY >= lineCount) {
            adjustedY = lineCount - 1
        }

        val line = lineBuffer[adjustedY]!!
        var charIndex = 0
        var prevWidth = 0

        var currentWidth = 0
        while (charIndex < line.length) {
            prevWidth = currentWidth
            charIndex++
            currentWidth = getTextWidth(line.substring(0, charIndex), sprites)
            if (currentWidth >= x) break
        }

        if (charIndex == line.length && line.endsWith("<br>")) {
            charIndex -= 4
        }

        if (x - prevWidth < currentWidth - x) {
            charIndex--
        }

        for (i in 0 until adjustedY) {
            charIndex += lineBuffer[i]!!.length
        }

        return charIndex
    }

    fun fitText(text: String, maximumWidth: Int, icons: Array<Sprite>?): String {
        if (getTextWidth(text, icons) <= maximumWidth) {
            return text
        }

        var adjustedWidth = maximumWidth - getTextWidth("...", null)
        var escapeStart = -1
        var previous = -1
        var width = 0
        val length = text.length
        var prefix = ""

        for (i in 0 until length) {
            var current = text[i]
            if (current == '<') {
                escapeStart = i
            } else {
                if (current == '>' && escapeStart != -1) {
                    val tag = text.substring(escapeStart + 1, i)
                    escapeStart = -1

                    when (tag) {
                        "lt" -> current = '<'
                        "gt" -> current = '>'
                        "nbsp" -> current = 160.toChar()
                        "shy" -> current = 173.toChar()
                        "times" -> current = 215.toChar()
                        "euro" -> current = 8364.toChar()
                        "copy" -> current = 169.toChar()
                        "reg" -> current = 174.toChar()
                        else -> {
                            if (tag.startsWith("img=") && icons != null) {
                                try {
                                    val id = Utils.parseInt(tag.substring(4))
                                    width += icons[id].getSpriteWidth()
                                    previous = -1
                                    if (width > adjustedWidth) {
                                        return "$prefix..."
                                    }
                                    prefix = text.substring(0, i + 1)
                                } catch (ignored: Exception) {
                                }
                            }
                            continue
                        }
                    }
                }

                if (escapeStart == -1) {
                    width += glyphWidths[Utils.byteForChar(current).toInt() and 0xff].toInt() and 0xff
                    if (kerning != null && previous != -1) {
                        width += kerning!![previous][current.code]
                    }

                    previous = current.code
                    var total = width
                    if (kerning != null) {
                        total = width + kerning!![current.code][46]
                    }

                    if (total > adjustedWidth) {
                        return "$prefix..."
                    }

                    prefix = text.substring(0, i + 1)
                }
            }
        }

        return text
    }

    fun getTextHeightMultiline(text: String, width: Int, lineHeight: Int, sprites: Array<Sprite>?): Int {
        var adjustedLineHeight = lineHeight
        if (adjustedLineHeight == 0) {
            adjustedLineHeight = verticalSpacing
        }

        val lineCount = splitLinesToBuffer(text, intArrayOf(width), lineBuffer, sprites)
        val height = adjustedLineHeight * (lineCount - 1)
        return height + bottomPadding + topPadding
    }

    fun splitLinesToBuffer(text: String, widths: IntArray?, buffer: Array<String?>, sprites: Array<Sprite>?): Int {
        return splitLinesToBuffer(text, widths, buffer, sprites, true)
    }

    companion object {
        @JvmField
        val lineBuffer = arrayOfNulls<String>(100)

        @JvmField
        var defaultFontMetrics: FontMetrics? = null

        @JvmField
        var useBoldFont: Boolean = false

        @JvmField
        var boldFontRenderer: AbstractFontRenderer? = null

        @JvmStatic
        fun getFontMetrics(index0: Js5ResourceProvider?, fileId: Int): FontMetrics? {
            val bytes = index0?.getFile(fileId, 0)
            return if (bytes == null) null else FontMetrics(bytes)
        }

        @JvmStatic
        fun list(fontResource: Js5ResourceProvider?, fileId: Int): FontMetrics? {
            val fontData = fontResource?.getFile(fileId)
            return if (fontData == null) null else FontMetrics(fontData)
        }

        @JvmStatic
        fun getForMinimenu(): FontMetrics {
            val fontmetrics = if (useBoldFont) {
                if (boldFontRenderer != null && StaticMedia.boldFontMetrics != null) {
                    StaticMedia.boldFontMetrics
                } else {
                    defaultFontMetrics
                }
            } else {
                defaultFontMetrics
            }
            MiniMenu.entryRowHeight = fontmetrics!!.bottomPadding + fontmetrics.topPadding
            return fontmetrics
        }
    }
}
