package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.OpenGl2DImageTexture
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils
import com.jagex.game.runetek5.toolkit.SpriteSurfaceGL

import jaggl.OpenGL

class FontRendererGL(
    override val toolkit: OpenGlToolkit,
    fontMetrics: FontMetrics,
    characterSprites: Array<SpriteType>,
    isAlphaOnly: Boolean
) : AbstractFontRenderer(toolkit, fontMetrics) {
    @JvmField val texture: OpenGl2DImageTexture
    @JvmField val isAlphaOnlyTexture: Boolean
    @JvmField val listUtils: GlListUtils

    /**
     * Scratch buffers for the eye-plane texgen coefficients used by the
     * component-surface [drawChar] variant, so no per-glyph allocations occur.
     * Only indices 0/3 (S plane) and 1/3 (T plane) are ever non-zero.
     */
    private val texGenPlaneS = FloatArray(4)
    private val texGenPlaneT = FloatArray(4)

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

            this.texture = OpenGl2DImageTexture.createSurface(toolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, atlasSize, atlasSize, pixels, GLFormat.RGBA)
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
        this.listUtils = GlListUtils(256)
        val texelScaleS = this.texture.s / this.texture.singleWidth
        val texelScaleT = this.texture.t / this.texture.singleLength

        for (charIndex in 0 until 256) {
            val charSprite = characterSprites[charIndex]
            val height = charSprite.height
            val width = charSprite.width
            val minY = charSprite.minY
            val minX = charSprite.minX
            val atlasX = (maxDimension * (charIndex % 16)).toFloat()
            val atlasY = (charIndex / 16 * maxDimension).toFloat()
            val u0 = atlasX * texelScaleS
            val v0 = atlasY * texelScaleT
            val u1 = (atlasX + width) * texelScaleS
            val v1 = (atlasY + height) * texelScaleT
            this.listUtils.newList(charIndex)
            toolkit.legacyDrawTrace()
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glTexCoord2f(u0, this.texture.t - v0)
            OpenGL.glVertex2i(minX, minY)
            OpenGL.glTexCoord2f(u0, this.texture.t - v1)
            OpenGL.glVertex2i(minX, minY + height)
            OpenGL.glTexCoord2f(u1, this.texture.t - v1)
            OpenGL.glVertex2i(minX + width, minY + height)
            OpenGL.glTexCoord2f(u1, this.texture.t - v0)
            OpenGL.glVertex2i(minX + width, minY)
            OpenGL.glEnd()
            this.listUtils.endList()
        }

    }

    override fun drawChar(charIndex: Int, translateX: Int, translateY: Int, colorArgb: Int, isShadow: Boolean, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        // Legacy display-list glyph rendering is forbidden under ModernGLToolkit
        // (standalone modern renderer). This only triggers for fonts created before
        // the modern renderer's lazy init; counted in [ModernDiag] legacySkips.
        val tk = this.toolkit
        if (tk is ModernGLToolkit) {
            tk.countLegacySkip("legacy FontRendererGL.drawChar")
            return
        }
        val spriteSurface = surface as SpriteSurfaceGL
        val surfaceTexture = spriteSurface.surface
        this.toolkit.setupTextRendering()
        this.toolkit.setActiveTexture(this.texture)
        if (!this.isAlphaOnlyTexture && !isShadow) {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_REPLACE)
            this.toolkit.restoreGlyphCombinerSource()
        } else {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
            // Held until the next non-glyph draw instead of being restored per glyph.
            this.toolkit.applyGlyphCombinerSource()
        }

        this.toolkit.activateTexture(1)
        this.toolkit.setActiveTexture(surfaceTexture)
        this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
        this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR)
        OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR)
        val texelScaleS = surfaceTexture.s / surfaceTexture.singleWidth
        val texelScaleT = surfaceTexture.t / surfaceTexture.singleLength
        this.texGenPlaneS[0] = texelScaleS
        this.texGenPlaneS[3] = (-surfaceOffsetX) * texelScaleS
        this.texGenPlaneT[1] = texelScaleT
        this.texGenPlaneT[3] = (-surfaceOffsetY) * texelScaleT
        OpenGL.glTexGenfv(GL_S, GL_EYE_PLANE, this.texGenPlaneS, 0)
        OpenGL.glTexGenfv(GL_T, GL_EYE_PLANE, this.texGenPlaneT, 0)
        OpenGL.glEnable(GL_TEXTURE_GEN_S)
        OpenGL.glEnable(GL_TEXTURE_GEN_T)
        this.toolkit.setGlyphColor(colorArgb)
        OpenGL.glTranslatef(translateX.toFloat(), translateY.toFloat(), 0.0F)
        this.listUtils.callList(charIndex)
        OpenGL.glLoadIdentity()
        OpenGL.glDisable(GL_TEXTURE_GEN_S)
        OpenGL.glDisable(GL_TEXTURE_GEN_T)
        this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        this.toolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        this.toolkit.setActiveTexture(null)
        this.toolkit.activateTexture(0)
    }

    override fun drawChar(charIndex: Int, translateX: Int, translateY: Int, colorArgb: Int, isShadow: Boolean) {
        // Legacy display-list glyph rendering is forbidden under ModernGLToolkit
        // (standalone modern renderer). This only triggers for fonts created before
        // the modern renderer's lazy init; counted in [ModernDiag] legacySkips.
        val tk = this.toolkit
        if (tk is ModernGLToolkit) {
            tk.countLegacySkip("legacy FontRendererGL.drawChar")
            return
        }
        this.toolkit.setupTextRendering()
        this.toolkit.setActiveTexture(this.texture)
        if (!this.isAlphaOnlyTexture && !isShadow) {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_REPLACE)
            this.toolkit.restoreGlyphCombinerSource()
        } else {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
            // Held until the next non-glyph draw instead of being restored per glyph.
            this.toolkit.applyGlyphCombinerSource()
        }

        this.toolkit.setGlyphColor(colorArgb)
        OpenGL.glTranslatef(translateX.toFloat(), translateY.toFloat(), 0.0F)
        this.listUtils.callList(charIndex)
        OpenGL.glLoadIdentity()
    }
}
