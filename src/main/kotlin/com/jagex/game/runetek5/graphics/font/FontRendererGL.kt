package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture_Sub1
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils
import com.jagex.game.runetek5.unidentified.SpriteSurfaceGL

import jaggl.OpenGL

class FontRendererGL(
    override val toolkit: OpenGlToolkit,
    fontmetrics2: FontMetrics,
    characterSprites: Array<SpriteType>,
    bool4: Boolean
) : AbstractFontRenderer(toolkit, fontmetrics2) {
    @JvmField val texture: OpenGl2DTexture_Sub1
    @JvmField val aBool9326: Boolean
    @JvmField val listUtils: GlListUtils

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

        val size = maxDimension * 16
        if (bool4) {
            val pixels = ByteArray(size * size)

            for (charIndex in 0 until 256) {
                val charSprite = characterSprites[charIndex]
                val i_26 = charSprite.height
                val i_11 = charSprite.width
                val i_12 = maxDimension * (charIndex % 16)
                val i_13 = charIndex / 16 * maxDimension
                var i_14 = i_12 + i_13 * size
                var i_15 = 0
                val bytes_27: ByteArray
                if (charSprite.alpha == null) {
                    bytes_27 = charSprite.pixels

                    for (i_17 in 0 until i_26) {
                        for (i_18 in 0 until i_11) {
                            pixels[i_14++] = (if (bytes_27[i_15++] == 0.toByte()) 0 else -1).toByte()
                        }

                        i_14 += size - i_11
                    }
                } else {
                    bytes_27 = charSprite.alpha!!

                    for (i_17 in 0 until i_26) {
                        for (i_18 in 0 until i_11) {
                            pixels[i_14++] = bytes_27[i_15++]
                        }

                        i_14 += size - i_11
                    }
                }
            }

            this.texture = OpenGl2DTexture_Sub1.createSurface(toolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, size, size, pixels, GLFormat.RGBA)
            this.aBool9326 = true
        } else {
            val pixels = IntArray(size * size)

            for (charIndex in 0 until 256) {
                val charSprite = characterSprites[charIndex]
                val palette = charSprite.pallete
                val alpha = charSprite.alpha
                val charPixels = charSprite.pixels
                val height = charSprite.height
                val width = charSprite.width
                val i_15 = maxDimension * (charIndex % 16)
                val i_16 = charIndex / 16 * maxDimension
                var i_17 = i_15 + i_16 * size
                var pixel = 0
                if (alpha != null) {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            pixels[i_17++] = alpha[pixel].toInt() shl 24 or palette[charPixels[pixel++].toInt() and 0xff]
                        }

                        i_17 += size - width
                    }
                } else {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val charPixel = charPixels[pixel++]
                            if (charPixel != 0.toByte()) {
                                pixels[i_17++] = 0xffffff.inv() or palette[charPixel.toInt() and 0xff]
                            } else {
                                i_17++
                            }
                        }

                        i_17 += size - width
                    }
                }
            }

            this.texture = OpenGl2DTexture_Sub1.method15537(toolkit, size, size, pixels, 0, 0)
            this.aBool9326 = false
        }

        this.texture.setLinearTextureFilter(false)
        this.listUtils = GlListUtils(256)
        val f_22 = this.texture.s / this.texture.singleWidth
        val f_30 = this.texture.t / this.texture.singleLength

        for (spriteIndex in 0 until 256) {
            val spriteType = characterSprites[spriteIndex]
            val height = spriteType.height
            val width = spriteType.width
            val minY = spriteType.minY
            val minX = spriteType.minX
            val f_32 = (maxDimension * (spriteIndex % 16)).toFloat()
            val f_33 = (spriteIndex / 16 * maxDimension).toFloat()
            val f_34 = f_32 * f_22
            val f_35 = f_33 * f_30
            val f_36 = (f_32 + width) * f_22
            val f_37 = (f_33 + height) * f_30
            this.listUtils.newList(spriteIndex)
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glTexCoord2f(f_34, this.texture.t - f_35)
            OpenGL.glVertex2i(minX, minY)
            OpenGL.glTexCoord2f(f_34, this.texture.t - f_37)
            OpenGL.glVertex2i(minX, minY + height)
            OpenGL.glTexCoord2f(f_36, this.texture.t - f_37)
            OpenGL.glVertex2i(minX + width, minY + height)
            OpenGL.glTexCoord2f(f_36, this.texture.t - f_35)
            OpenGL.glVertex2i(minX + width, minY)
            OpenGL.glEnd()
            this.listUtils.endList()
        }

    }

    override fun drawChar(charToDraw: Int, translateX: Int, translateY: Int, colorArgb: Int, bool5: Boolean, surface: ComponentSurface, i_7: Int, i_8: Int) {
        val spriteSurface = surface as SpriteSurfaceGL
        val class137Sub1Sub110 = spriteSurface.surface
        this.toolkit.setupTextRendering()
        this.toolkit.setActiveTexture(this.texture)
        if (!this.aBool9326 && !bool5) {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_REPLACE)
        } else {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
            this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        }

        this.toolkit.activateTexture(1)
        this.toolkit.setActiveTexture(class137Sub1Sub110)
        this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
        this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR)
        OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR)
        val f_11 = class137Sub1Sub110.s / class137Sub1Sub110.singleWidth
        val f_12 = class137Sub1Sub110.t / class137Sub1Sub110.singleLength
        OpenGL.glTexGenfv(GL_S, GL_EYE_PLANE, floatArrayOf(f_11, 0.0F, 0.0F, (-i_7) * f_11), 0)
        OpenGL.glTexGenfv(GL_T, GL_EYE_PLANE, floatArrayOf(0.0F, f_12, 0.0F, (-i_8) * f_12), 0)
        OpenGL.glEnable(GL_TEXTURE_GEN_S)
        OpenGL.glEnable(GL_TEXTURE_GEN_T)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        OpenGL.glTranslatef(translateX.toFloat(), translateY.toFloat(), 0.0F)
        this.listUtils.callList(charToDraw)
        OpenGL.glLoadIdentity()
        OpenGL.glDisable(GL_TEXTURE_GEN_S)
        OpenGL.glDisable(GL_TEXTURE_GEN_T)
        this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        this.toolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        this.toolkit.setActiveTexture(null)
        this.toolkit.activateTexture(0)
        if (this.aBool9326 || bool5) {
            this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        }
    }

    override fun drawChar(character: Int, translateX: Int, translateY: Int, colorArgb: Int, bool5: Boolean) {
        this.toolkit.setupTextRendering()
        this.toolkit.setActiveTexture(this.texture)
        if (!this.aBool9326 && !bool5) {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_REPLACE)
        } else {
            this.toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
            this.toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        }

        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        OpenGL.glTranslatef(translateX.toFloat(), translateY.toFloat(), 0.0F)
        this.listUtils.callList(character)
        OpenGL.glLoadIdentity()
        if (this.aBool9326 || bool5) {
            this.toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        }
    }
}
