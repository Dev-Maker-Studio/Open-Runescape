package com.jagex.game.runetek5.graphics.font

import java.awt.*
import java.awt.image.PixelGrabber

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class HelveticaFont(toolkit: AbstractToolkit, fontSize: Int, component: Component) {

    @JvmField var sizeY = 0
    @JvmField var startY = 0
    @JvmField var loadPlain = false
    @JvmField val clipping = IntArray(4)
    @JvmField var nativeSprites = arrayOfNulls<NativeSprite>(256)
    @JvmField var characterWidths = IntArray(256)

    init {
        loadPlain = false
        var font = Font("Helvetica", Font.BOLD, fontSize)
        var fontmetrics = component.getFontMetrics(font)
        for (index in 0 until VALID_CHARACTER_COUNT) {
            initialize(toolkit, font, fontmetrics, VALID_CHARACTERS_STRING[index], index, false)
        }
        if (loadPlain) {
            loadPlain = false
            font = Font("Helvetica", Font.PLAIN, fontSize)
            fontmetrics = component.getFontMetrics(font)
            for (index in 0 until VALID_CHARACTER_COUNT) {
                initialize(toolkit, font, fontmetrics, VALID_CHARACTERS_STRING[index], index, false)
            }
            if (!loadPlain) {
                loadPlain = false
                for (index in 0 until VALID_CHARACTER_COUNT) {
                    initialize(toolkit, font, fontmetrics, VALID_CHARACTERS_STRING[index], index, true)
                }
            }
        }
    }

    fun getLineHeight(): Int {
        return startY - 1
    }

    fun drawText(text: String, drawX: Int, drawY: Int, color: Int) {
        val hasColor = color != 0
        var adjustedColor = color or 0xffffff.inv()
        var currentX = drawX

        for (index in text.indices) {
            val c = VALID_CHARACTERS[text[index].code]
            if (hasColor) {
                nativeSprites[c]!!.draw(currentX + 1, drawY + 1, 0, 0xffffff.inv(), 1)
            }

            nativeSprites[c]!!.draw(currentX, drawY, 0, adjustedColor, 1)
            currentX += characterWidths[c]
        }
    }

    fun getTextWidth(text: String): Int {
        var totalWidth = 0

        for (index in text.indices) {
            val c = VALID_CHARACTERS[text[index].code]
            totalWidth += characterWidths[c]
        }

        return totalWidth
    }

    fun drawClippedText(toolkit: AbstractToolkit, text: String, x: Int, y: Int, color: Int) {
        val textCenterX = getTextWidth(text) / 2
        toolkit.getClipping(clipping)
        if (x - textCenterX <= clipping[2] && x + textCenterX >= clipping[0] && y - sizeY <= clipping[3] && y + startY >= clipping[1]) {
            drawText(text, x - textCenterX, y, color)
        }
    }

    fun initialize(toolkit: AbstractToolkit, font: Font, fontmetrics: java.awt.FontMetrics, c: Char, cacheIndex: Int, large: Boolean) {
        var width = fontmetrics.charWidth(c)
        val characterWidth = width
        var isLarge = large
        if (isLarge) {
            try {
                if (c == '/') {
                    isLarge = false
                }

                if (c == 'f' || c == 't' || c == 'w' || c == 'v' || c == 'k' || c == 'x' || c == 'y' || c == 'A' || c == 'V' || c == 'W') {
                    ++width
                }
            } catch (ignored: Exception) {
                // NOOP
            }
        }

        val maxAscent = fontmetrics.maxAscent
        val height = fontmetrics.maxAscent + fontmetrics.maxDescent
        val strtY = fontmetrics.height
        val image = GameShell.gameCanvas!!.createImage(width, height)
        val graphics = image.graphics
        graphics.color = Color.black
        graphics.fillRect(0, 0, width, height)
        graphics.color = Color.white
        graphics.font = font
        graphics.drawString(c.toString() + "", 0, maxAscent)
        if (isLarge) {
            graphics.drawString(c.toString() + "", 1, maxAscent)
        }

        val pixels = IntArray(width * height)
        val pixelGrabber = PixelGrabber(image, 0, 0, width, height, pixels, 0, width)

        try {
            pixelGrabber.grabPixels()
        } catch (ignored: Exception) {
            // NOOP
        }

        image.flush()
        var Y = 0

        label66@
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[x + y * width]
                if (pixel and 0xffffff != 0) {
                    Y = y
                    break@label66
                }
            }
        }

        for (index in pixels.indices) {
            if (pixels[index] and 0xffffff == 0) {
                pixels[index] = 0
            }
        }

        sizeY = maxAscent - Y
        startY = strtY
        characterWidths[cacheIndex] = characterWidth
        nativeSprites[cacheIndex] = toolkit.createNativeSprite(pixels, width, width, height)
    }

    fun method3186(): Int {
        return sizeY
    }

    companion object {
        const val VALID_CHARACTERS_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| \u00c4\u00cb\u00cf\u00d6\u00dc\u00e4\u00eb\u00ef\u00f6\u00fc\u00ff\u00df\u00c1\u00c0\u00c9\u00c8\u00cd\u00cc\u00d3\u00d2\u00da\u00d9\u00e1\u00e0\u00e9\u00e8\u00ed\u00ec\u00f3\u00f2\u00fa\u00f9\u00c2\u00ca\u00ce\u00d4\u00db\u00e2\u00ea\u00ee\u00f4\u00fb\u00c6\u00e6\u00e3\u00c3\u00f5\u00d5\u00e7\u00c7"
        const val VALID_CHARACTER_COUNT = VALID_CHARACTERS_STRING.length

        @JvmField
        val VALID_CHARACTERS = IntArray(256)

        init {
            for (i in 0 until 256) {
                var ch = VALID_CHARACTERS_STRING.indexOf(i.toChar())
                if (ch == -1)
                    ch = 74
                VALID_CHARACTERS[i] = ch
            }
        }
    }
}
