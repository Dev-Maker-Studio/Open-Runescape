package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.OpenGl2DImageTexture
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

class ModernFontRenderer(
    override val toolkit: ModernGLToolkit,
    fontMetrics: FontMetrics,
    characterSprites: Array<SpriteType>,
    isPlain: Boolean
) : AbstractFontRenderer(toolkit, fontMetrics) {

    private data class GlyphData(
        val offsetX: Int,
        val offsetY: Int,
        val width: Int,
        val height: Int,
        val u0: Float,
        val v0: Float,
        val u1: Float,
        val v1: Float
    )

    private val atlasTextureHandle: Int

    private val plainFont: Boolean = isPlain

    private val glyphs: Array<GlyphData>

    init {

        var maxDimension = 0
        for (charIndex in 0 until 256) {
            val charSprite = characterSprites[charIndex]
            if (charSprite.height > maxDimension) maxDimension = charSprite.height
            if (charSprite.width > maxDimension) maxDimension = charSprite.width
        }

        val atlasSize = maxDimension * 16

        val texture: OpenGl2DImageTexture
        if (isPlain) {
            val pixels = IntArray(atlasSize * atlasSize)
            for (charIndex in 0 until 256) {
                val charSprite = characterSprites[charIndex]
                val charHeight = charSprite.height
                val charWidth = charSprite.width
                val cellX = maxDimension * (charIndex % 16)
                val cellY = charIndex / 16 * maxDimension
                var dstIdx = cellX + cellY * atlasSize
                var srcIdx = 0
                if (charSprite.alpha == null) {
                    val spritePixels = charSprite.pixels
                    for (y in 0 until charHeight) {
                        for (x in 0 until charWidth) {
                            pixels[dstIdx++] = if (spritePixels[srcIdx++] == 0.toByte()) 0 else -1
                        }
                        dstIdx += atlasSize - charWidth
                    }
                } else {
                    val alphaData = charSprite.alpha!!
                    for (y in 0 until charHeight) {
                        for (x in 0 until charWidth) {
                            pixels[dstIdx++] = ((alphaData[srcIdx++].toInt() and 0xFF) shl 24) or 0xFFFFFF
                        }
                        dstIdx += atlasSize - charWidth
                    }
                }
            }
            texture = OpenGl2DImageTexture.createFromPixels(toolkit, atlasSize, atlasSize, pixels, 0, 0)
        } else {
            val pixels = IntArray(atlasSize * atlasSize)
            for (charIndex in 0 until 256) {
                val charSprite = characterSprites[charIndex]
                val palette = charSprite.pallete
                val alpha = charSprite.alpha
                val charPixels = charSprite.pixels
                val charHeight = charSprite.height
                val charWidth = charSprite.width
                val cellX = maxDimension * (charIndex % 16)
                val cellY = charIndex / 16 * maxDimension
                var dstIdx = cellX + cellY * atlasSize
                var srcIdx = 0
                if (alpha != null) {
                    for (y in 0 until charHeight) {
                        for (x in 0 until charWidth) {
                            pixels[dstIdx++] = alpha[srcIdx].toInt() shl 24 or palette[charPixels[srcIdx].toInt() and 0xFF]
                            srcIdx++
                        }
                        dstIdx += atlasSize - charWidth
                    }
                } else {
                    for (y in 0 until charHeight) {
                        for (x in 0 until charWidth) {
                            val charPixel = charPixels[srcIdx++]
                            if (charPixel != 0.toByte()) {
                                pixels[dstIdx++] = 0xFFFFFF.inv() or palette[charPixel.toInt() and 0xFF]
                            } else {
                                dstIdx++
                            }
                        }
                        dstIdx += atlasSize - charWidth
                    }
                }
            }
            texture = OpenGl2DImageTexture.createFromPixels(toolkit, atlasSize, atlasSize, pixels, 0, 0)
        }

        texture.setLinearTextureFilter(false)
        atlasTextureHandle = texture.activeHandle

        val uScale = texture.s / texture.singleWidth
        val vScale = texture.t / texture.singleLength

        glyphs = Array(256) { charIndex ->
            val charSprite = characterSprites[charIndex]
            val charHeight = charSprite.height
            val charWidth = charSprite.width
            val minY = charSprite.minY
            val minX = charSprite.minX
            val cellX = (maxDimension * (charIndex % 16)).toFloat()
            val cellY = (charIndex / 16 * maxDimension).toFloat()

            val u0 = cellX * uScale
            val v0 = texture.t - cellY * vScale
            val u1 = (cellX + charWidth) * uScale
            val v1 = texture.t - (cellY + charHeight) * vScale

            GlyphData(
                offsetX = minX,
                offsetY = minY,
                width = charWidth,
                height = charHeight,
                u0 = u0,
                v0 = v0,
                u1 = u1,
                v1 = v1
            )
        }
    }

    override fun drawChar(charIndex: Int, translateX: Int, translateY: Int, colorArgb: Int, isShadow: Boolean) {
        if (charIndex < 0 || charIndex >= 256) return
        val glyph = glyphs[charIndex]
        if (glyph.width <= 0 || glyph.height <= 0) return

        val spriteRenderer = toolkit.getSpriteRenderer() ?: return
        if (!spriteRenderer.isReady()) return

        toolkit.ensureSpriteBatchForFont()
        if (!spriteRenderer.isBatching()) {
            toolkit.countLegacySkip("ModernFontRenderer.drawChar(skipped)")
            return
        }

        val x0 = (translateX + glyph.offsetX).toFloat()
        val y0 = (translateY + glyph.offsetY).toFloat()
        val x1 = x0 + glyph.width
        val y1 = y0 + glyph.height

        val effectiveColor = if (plainFont || isShadow) colorArgb else -1

        spriteRenderer.drawSprite(
            textureId = atlasTextureHandle,
            x0 = x0, y0 = y0,
            x1 = x1, y1 = y1,
            u0 = glyph.u0, v0 = glyph.v0,
            u1 = glyph.u1, v1 = glyph.v1,
            colorArgb = effectiveColor,
            blendMode = 1
        )
    }

    override fun drawChar(
        charIndex: Int,
        translateX: Int,
        translateY: Int,
        colorArgb: Int,
        isShadow: Boolean,
        surface: ComponentSurface,
        surfaceOffsetX: Int,
        surfaceOffsetY: Int
    ) {
        drawChar(charIndex, translateX, translateY, colorArgb, isShadow)
    }
}
