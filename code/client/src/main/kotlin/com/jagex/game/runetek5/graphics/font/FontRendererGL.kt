package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.OpenGl2DImageTexture
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.SpriteSurfaceGL

class FontRendererGL(
    override val toolkit: OpenGlToolkit,
    fontMetrics: FontMetrics,
    characterSprites: Array<SpriteType>,
    isAlphaOnly: Boolean
) : AbstractFontRenderer(toolkit, fontMetrics) {
    @JvmField val texture: OpenGl2DImageTexture
    @JvmField val isAlphaOnlyTexture: Boolean

    init {
        var maxDimension = 0

        for (charIndex in 0 until 256) {
            val charSprite = characterSprites[charIndex]
            if (charSprite.height > maxDimension) {
                maxDimension = charSprite.height
            }

            if (charSprite.width > maxDimension) {
                maxDimension = charSprite.width
            }
        }

        val atlasSize = maxDimension * 16
        if (isAlphaOnly) {
            val pixels = ByteArray(atlasSize * atlasSize)

            for (charIndex in 0 until 256) {
                val charSprite = characterSprites[charIndex]
                val height = charSprite.height
                val width = charSprite.width
                val atlasX = maxDimension * (charIndex % 16)
                val atlasY = charIndex / 16 * maxDimension
                var dstIndex = atlasX + atlasY * atlasSize
                var srcIndex = 0
                val srcBytes: ByteArray
                if (charSprite.alpha == null) {
                    srcBytes = charSprite.pixels

                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            pixels[dstIndex++] = (if (srcBytes[srcIndex++] == 0.toByte()) 0 else -1).toByte()
                        }

                        dstIndex += atlasSize - width
                    }
                } else {
                    srcBytes = charSprite.alpha!!

                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            pixels[dstIndex++] = srcBytes[srcIndex++]
                        }

                        dstIndex += atlasSize - width
                    }
                }
            }

            this.texture = OpenGl2DImageTexture.createSurface(toolkit, GLFormat.ALPHA, GLActionFormat.PIXEL_FORMAT, atlasSize, atlasSize, pixels, GLFormat.ALPHA)
            this.isAlphaOnlyTexture = true
        } else {
            val pixels = IntArray(atlasSize * atlasSize)

            for (charIndex in 0 until 256) {
                val charSprite = characterSprites[charIndex]
                val palette = charSprite.pallete
                val alpha = charSprite.alpha
                val charPixels = charSprite.pixels
                val height = charSprite.height
                val width = charSprite.width
                val atlasX = maxDimension * (charIndex % 16)
                val atlasY = charIndex / 16 * maxDimension
                var dstIndex = atlasX + atlasY * atlasSize
                var srcIndex = 0
                if (alpha != null) {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            pixels[dstIndex++] = alpha[srcIndex].toInt() shl 24 or palette[charPixels[srcIndex++].toInt() and 0xff]
                        }

                        dstIndex += atlasSize - width
                    }
                } else {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val charPixel = charPixels[srcIndex++]
                            if (charPixel != 0.toByte()) {
                                pixels[dstIndex++] = 0xffffff.inv() or palette[charPixel.toInt() and 0xff]
                            } else {
                                dstIndex++
                            }
                        }

                        dstIndex += atlasSize - width
                    }
                }
            }

            this.texture = OpenGl2DImageTexture.createFromPixels(toolkit, atlasSize, atlasSize, pixels, 0, 0)
            this.isAlphaOnlyTexture = false
        }

        this.texture.setLinearTextureFilter(false)
    }

    override fun drawChar(charIndex: Int, translateX: Int, translateY: Int, colorArgb: Int, isShadow: Boolean, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        (toolkit as ModernGLToolkit).countLegacySkip("legacy FontRendererGL.drawChar")
    }

    override fun drawChar(charIndex: Int, translateX: Int, translateY: Int, colorArgb: Int, isShadow: Boolean) {
        (toolkit as ModernGLToolkit).countLegacySkip("legacy FontRendererGL.drawChar")
    }
}
