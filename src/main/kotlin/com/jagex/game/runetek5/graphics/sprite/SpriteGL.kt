package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture_Sub1
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.SpriteSurfaceGL
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import jaggl.OpenGL

class SpriteGL : NativeSprite {

    @JvmField var translateX: Int = 0
    @JvmField var translateY: Int = 0
    @JvmField var trimRight: Int = 0
    @JvmField var trimBottom: Int = 0
    @JvmField var stretched: Boolean = false
    @JvmField var channelMaskIndex: Int = 0
    @JvmField val toolkit: OpenGlToolkit
    @JvmField val texture2d: OpenGl2DTexture_Sub1
    @JvmField var channelMaskTexture: OpenGl2DTexture_Sub1? = null

    constructor(class505Sub11: OpenGlToolkit, i_2: Int, i_3: Int, ints_4: IntArray, i_5: Int, i_6: Int) {
        toolkit = class505Sub11
        texture2d = OpenGl2DTexture_Sub1.method15537(class505Sub11, i_2, i_3, ints_4, i_6, i_5)
    }

    constructor(class505Sub11: OpenGlToolkit, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        toolkit = class505Sub11
        texture2d = OpenGl2DTexture_Sub1.method15538(class505Sub11, i_2, i_3, i_4, i_5)
    }

    constructor(class505Sub11: OpenGlToolkit, i_2: Int, i_3: Int, bool4: Boolean) {
        toolkit = class505Sub11
        texture2d = OpenGl2DTexture_Sub1.method15547(
            class505Sub11,
            if (bool4) GLFormat.HDR else GLFormat.RGB,
            GLActionFormat.PIXEL_FORMAT,
            i_2,
            i_3
        )
    }

    override fun setSize(translateX: Int, translateY: Int, i_3: Int, i_4: Int) {
        this.translateX = translateX
        this.translateY = translateY
        trimRight = i_3
        trimBottom = i_4
        stretched = this.translateX != 0 || this.translateY != 0 || trimRight != 0 || trimBottom != 0
    }

    override fun getWidth(): Int {
        return texture2d.singleWidth
    }

    override fun getHeight(): Int {
        return texture2d.singleLength
    }

    override fun getSpriteHeight(): Int {
        return texture2d.singleLength + translateY + trimBottom
    }

    override fun captureScreenRegion(offsetX: Int, offsetY: Int, width: Int, height: Int, x: Int, y: Int) {
        if (toolkit.appleOperatingSystem) {
            val pixels = toolkit.copyPixelRectangle(x, y, width, height)
            if (pixels != null) {
                for (i in pixels.indices) {
                    pixels[i] = pixels[i] or 0xffffff.inv()
                }
                updateTextureRegion(offsetX, offsetY, width, height, pixels, width)
            }
        } else {
            texture2d.copyTextureSubImage(offsetX, offsetY, width, height, x, y)
        }
    }

    override fun pixelTransfer() {
        OpenGL.glPixelTransferf(GL_RED_SCALE, 0.5f)
        OpenGL.glPixelTransferf(GL_RED_BIAS, 0.499f)
        OpenGL.glPixelTransferf(GL_GREEN_SCALE, 0.5f)
        OpenGL.glPixelTransferf(GL_GREEN_BIAS, 0.499f)
        OpenGL.glPixelTransferf(GL_BLUE_SCALE, 0.5f)
        OpenGL.glPixelTransferf(GL_BLUE_BIAS, 0.499f)
        channelMaskTexture = OpenGl2DTexture_Sub1.method15538(toolkit, 0, 0, texture2d.singleWidth, texture2d.singleLength)
        channelMaskIndex = 3
        OpenGL.glPixelTransferf(GL_RED_SCALE, 1.0f)
        OpenGL.glPixelTransferf(GL_RED_BIAS, 0.0f)
        OpenGL.glPixelTransferf(GL_GREEN_SCALE, 1.0f)
        OpenGL.glPixelTransferf(GL_GREEN_BIAS, 0.0f)
        OpenGL.glPixelTransferf(GL_BLUE_SCALE, 1.0f)
        OpenGL.glPixelTransferf(GL_BLUE_BIAS, 0.0f)
    }

    fun updateTextureRegion(i_1: Int, i_2: Int, i_3: Int, i_4: Int, ints_5: IntArray, i_7: Int) {
        texture2d.updateTextureRegion(i_1, i_2, i_3, i_4, ints_5, i_7)
    }

    override fun drawScaledColored(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        val endX = x + width
        val endY = y + height
        texture2d.setLinearTextureFilter(false)
        toolkit.setupSpriteRendering()
        toolkit.setActiveTexture(texture2d)
        toolkit.setBlendMode(blendMode)
        toolkit.loadTextureCombineMode(combineMode)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        if (texture2d.tiled && !stretched) {
            val t = texture2d.t * height / texture2d.singleLength
            val s = texture2d.s * width / texture2d.singleWidth
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glTexCoord2f(0.0f, t)
            OpenGL.glVertex2i(x, y)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(x, endY)
            OpenGL.glTexCoord2f(s, 0.0f)
            OpenGL.glVertex2i(endX, endY)
            OpenGL.glTexCoord2f(s, t)
            OpenGL.glVertex2i(endX, y)
            OpenGL.glEnd()
        } else {
            OpenGL.glPushMatrix()
            OpenGL.glTranslatef(translateX.toFloat(), translateY.toFloat(), 0.0f)
            val spriteWidth = getSpriteWidth()
            val spriteHeight = getSpriteHeight()
            var i_12 = texture2d.singleLength + y
            OpenGL.glBegin(GL_QUADS)

            var startY: Int
            var startX: Int
            startY = y
            while (i_12 <= endY) {
                var i_14 = texture2d.singleWidth + x
                startX = x
                while (i_14 <= endX) {
                    OpenGL.glTexCoord2f(0.0f, texture2d.t)
                    OpenGL.glVertex2i(startX, startY)
                    OpenGL.glTexCoord2f(0.0f, 0.0f)
                    OpenGL.glVertex2i(startX, i_12)
                    OpenGL.glTexCoord2f(texture2d.s, 0.0f)
                    OpenGL.glVertex2i(i_14, i_12)
                    OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
                    OpenGL.glVertex2i(i_14, startY)
                    i_14 += spriteWidth
                    startX += spriteWidth
                }

                if (startX < endX) {
                    val f_16 = texture2d.s * (endX - startX) / texture2d.singleWidth
                    OpenGL.glTexCoord2f(0.0f, texture2d.t)
                    OpenGL.glVertex2i(startX, startY)
                    OpenGL.glTexCoord2f(0.0f, 0.0f)
                    OpenGL.glVertex2i(startX, i_12)
                    OpenGL.glTexCoord2f(f_16, 0.0f)
                    OpenGL.glVertex2i(endX, i_12)
                    OpenGL.glTexCoord2f(f_16, texture2d.t)
                    OpenGL.glVertex2i(endX, startY)
                }

                startY += spriteHeight
                i_12 += spriteHeight
            }

            if (startY < endY) {
                val t = texture2d.t * (texture2d.singleLength - (endY - startY)) / texture2d.singleLength
                startX = texture2d.singleWidth + x

                var i_21 = x
                while (startX <= endX) {
                    OpenGL.glTexCoord2f(0.0f, texture2d.t)
                    OpenGL.glVertex2i(i_21, startY)
                    OpenGL.glTexCoord2f(0.0f, t)
                    OpenGL.glVertex2i(i_21, endY)
                    OpenGL.glTexCoord2f(texture2d.s, t)
                    OpenGL.glVertex2i(startX, endY)
                    OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
                    OpenGL.glVertex2i(startX, startY)
                    startX += spriteWidth
                    i_21 += spriteWidth
                }

                if (i_21 < endX) {
                    val s = texture2d.s * (endX - i_21) / texture2d.singleWidth
                    OpenGL.glTexCoord2f(0.0f, texture2d.t)
                    OpenGL.glVertex2i(i_21, startY)
                    OpenGL.glTexCoord2f(0.0f, t)
                    OpenGL.glVertex2i(i_21, endY)
                    OpenGL.glTexCoord2f(s, t)
                    OpenGL.glVertex2i(endX, endY)
                    OpenGL.glTexCoord2f(s, texture2d.t)
                    OpenGL.glVertex2i(endX, startY)
                }
            }

            OpenGL.glEnd()
            OpenGL.glPopMatrix()
        }
    }

    fun resetChannelMaskState() {
        toolkit.activateTexture(1)
        toolkit.setActiveTexture(null)
        toolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        toolkit.setTexture(1, GL_PREVIOUS, GL_SRC_COLOR)
        toolkit.setTextureTransparency(0, GL_TEXTURE)
        toolkit.activateTexture(0)
        toolkit.setTexture(1, GL_PREVIOUS, GL_SRC_COLOR)
    }

    override fun draw(x: Int, y: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        texture2d.setLinearTextureFilter(false)
        toolkit.setupSpriteRendering()
        toolkit.setBlendMode(blendMode)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        var drawX = x + translateX
        var drawY = y + translateY
        if (channelMaskTexture == null) {
            toolkit.setActiveTexture(texture2d)
            toolkit.loadTextureCombineMode(combineMode)
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glTexCoord2f(0.0f, texture2d.t)
            OpenGL.glVertex2i(drawX, drawY)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(drawX, texture2d.singleLength + drawY)
            OpenGL.glTexCoord2f(texture2d.s, 0.0f)
            OpenGL.glVertex2i(texture2d.singleWidth + drawX, texture2d.singleLength + drawY)
            OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
            OpenGL.glVertex2i(texture2d.singleWidth + drawX, drawY)
            OpenGL.glEnd()
        } else {
            setupChannelMaskRendering(combineMode)
            channelMaskTexture!!.setLinearTextureFilter(false)
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, texture2d.t)
            OpenGL.glTexCoord2f(0.0f, texture2d.t)
            OpenGL.glVertex2i(drawX, drawY)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 0.0f)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(drawX, texture2d.singleLength + drawY)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, 0.0f)
            OpenGL.glTexCoord2f(texture2d.s, 0.0f)
            OpenGL.glVertex2i(texture2d.singleWidth + drawX, texture2d.singleLength + drawY)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, texture2d.t)
            OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
            OpenGL.glVertex2i(texture2d.singleWidth + drawX, drawY)
            OpenGL.glEnd()
            resetChannelMaskState()
        }
    }

    override fun drawWithSurface(i_1: Int, i_2: Int, class4553: ComponentSurface, i_4: Int, i_5: Int) {
        val class455Sub26 = class4553 as SpriteSurfaceGL
        val class137Sub1Sub17 = class455Sub26.surface
        texture2d.setLinearTextureFilter(false)
        toolkit.setupSpriteRendering()
        toolkit.setActiveTexture(texture2d)
        toolkit.loadTextureCombineMode(1)
        toolkit.activateTexture(1)
        toolkit.setActiveTexture(class137Sub1Sub17)
        toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
        toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        toolkit.setBlendMode(1)
        val drawX = i_1 + translateX
        val drawY = i_2 + translateY
        val i_8 = texture2d.singleWidth + drawX
        val i_9 = texture2d.singleLength + drawY
        val f_10 = class137Sub1Sub17.s / class137Sub1Sub17.singleWidth
        val f_11 = class137Sub1Sub17.t / class137Sub1Sub17.singleLength
        val f_12 = (drawX - i_4) * f_10
        val f_13 = (i_8 - i_4) * f_10
        val f_14 = class137Sub1Sub17.t - (drawY - i_5) * f_11
        val f_15 = class137Sub1Sub17.t - (i_9 - i_5) * f_11
        OpenGL.glBegin(7)
        OpenGL.glColor3f(1.0f, 1.0f, 1.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0f, texture2d.t)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_12, f_14)
        OpenGL.glVertex2i(drawX, drawY)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0f, 0.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_12, f_15)
        OpenGL.glVertex2i(drawX, texture2d.singleLength + drawY)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, texture2d.s, 0.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_13, f_15)
        OpenGL.glVertex2i(texture2d.singleWidth + drawX, texture2d.singleLength + drawY)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, texture2d.s, texture2d.t)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_13, f_14)
        OpenGL.glVertex2i(texture2d.singleWidth + drawX, drawY)
        OpenGL.glEnd()
        toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        toolkit.loadTextureCombineMode(0)
        toolkit.setActiveTexture(null)
        toolkit.activateTexture(0)
    }

    override fun drawColoredCircle(i_1: Int, i_2: Int, i_3: Int, i_4: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        texture2d.setLinearTextureFilter(true)
        toolkit.setupSpriteRendering()
        toolkit.setBlendMode(blendMode)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        if (stretched) {
            val f_9 = i_3.toFloat() / getSpriteWidth()
            val f_10 = i_4.toFloat() / getSpriteHeight()
            val f_11 = i_1 + translateX * f_9
            val f_12 = i_2 + translateY * f_10
            val f_13 = f_11 + texture2d.singleWidth * f_9
            val f_14 = f_12 + texture2d.singleLength * f_10
            if (channelMaskTexture == null) {
                toolkit.setActiveTexture(texture2d)
                toolkit.loadTextureCombineMode(combineMode)
                OpenGL.glBegin(GL_QUADS)
                OpenGL.glTexCoord2f(0.0f, texture2d.t)
                OpenGL.glVertex2f(f_11, f_12)
                OpenGL.glTexCoord2f(0.0f, 0.0f)
                OpenGL.glVertex2f(f_11, f_14)
                OpenGL.glTexCoord2f(texture2d.s, 0.0f)
                OpenGL.glVertex2f(f_13, f_14)
                OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
                OpenGL.glVertex2f(f_13, f_12)
                OpenGL.glEnd()
            } else {
                setupChannelMaskRendering(combineMode)
                channelMaskTexture!!.setLinearTextureFilter(true)
                OpenGL.glBegin(GL_QUADS)
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, texture2d.t)
                OpenGL.glTexCoord2f(0.0f, texture2d.t)
                OpenGL.glVertex2f(f_11, f_12)
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 0.0f)
                OpenGL.glTexCoord2f(0.0f, 0.0f)
                OpenGL.glVertex2f(f_11, f_14)
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, 0.0f)
                OpenGL.glTexCoord2f(texture2d.s, 0.0f)
                OpenGL.glVertex2f(f_13, f_14)
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, texture2d.t)
                OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
                OpenGL.glVertex2f(f_13, f_12)
                OpenGL.glEnd()
                resetChannelMaskState()
            }
        } else if (channelMaskTexture == null) {
            toolkit.setActiveTexture(texture2d)
            toolkit.loadTextureCombineMode(combineMode)
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glTexCoord2f(0.0f, texture2d.t)
            OpenGL.glVertex2i(i_1, i_2)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(i_1, i_2 + i_4)
            OpenGL.glTexCoord2f(texture2d.s, 0.0f)
            OpenGL.glVertex2i(i_3 + i_1, i_2 + i_4)
            OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
            OpenGL.glVertex2i(i_3 + i_1, i_2)
            OpenGL.glEnd()
        } else {
            setupChannelMaskRendering(combineMode)
            channelMaskTexture!!.setLinearTextureFilter(true)
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, texture2d.t)
            OpenGL.glTexCoord2f(0.0f, texture2d.t)
            OpenGL.glVertex2i(i_1, i_2)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 0.0f)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(i_1, i_2 + i_4)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, 0.0f)
            OpenGL.glTexCoord2f(texture2d.s, 0.0f)
            OpenGL.glVertex2i(i_3 + i_1, i_2 + i_4)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, texture2d.t)
            OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
            OpenGL.glVertex2i(i_3 + i_1, i_2)
            OpenGL.glEnd()
            resetChannelMaskState()
        }
    }

    override fun drawAffine(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, i_8: Int, i_9: Int) {
        var x1 = f_1
        var y1 = f_2
        var x2 = f_3
        var y2 = f_4
        var x3 = f_5
        var y3 = f_6
        var f_11: Float
        var f_12: Float
        if (stretched) {
            f_11 = getSpriteWidth().toFloat()
            f_12 = getSpriteHeight().toFloat()
            val f_13 = (x2 - x1) / f_11
            val f_14 = (y2 - y1) / f_11
            val f_15 = (x3 - x1) / f_12
            val f_16 = (y3 - y1) / f_12
            val f_17 = f_15 * translateY
            val f_18 = f_16 * translateY
            val f_19 = f_13 * translateX
            val f_20 = f_14 * translateX
            val f_21 = -f_13 * trimRight
            val f_22 = -f_14 * trimRight
            val f_23 = -f_15 * trimBottom
            val f_24 = -f_16 * trimBottom
            x1 = x1 + f_19 + f_17
            y1 = y1 + f_20 + f_18
            x2 = x2 + f_21 + f_17
            y2 = y2 + f_22 + f_18
            x3 = x3 + f_19 + f_23
            y3 = y3 + f_20 + f_24
        }

        f_11 = x3 + (x2 - x1)
        f_12 = y2 + (y3 - y1)
        texture2d.setLinearTextureFilter(true)
        toolkit.setupSpriteRendering()
        toolkit.setActiveTexture(texture2d)
        toolkit.setBlendMode(i_9)
        toolkit.loadTextureCombineMode(i_7)
        OpenGL.glColor4ub((i_8 shr 16).toByte(), (i_8 shr 8).toByte(), i_8.toByte(), (i_8 shr 24).toByte())
        OpenGL.glBegin(7)
        OpenGL.glTexCoord2f(0.0f, texture2d.t)
        OpenGL.glVertex2f(x1, y1)
        OpenGL.glTexCoord2f(0.0f, 0.0f)
        OpenGL.glVertex2f(x3, y3)
        OpenGL.glTexCoord2f(texture2d.s, 0.0f)
        OpenGL.glVertex2f(f_11, f_12)
        OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
        OpenGL.glVertex2f(x2, y2)
        OpenGL.glEnd()
    }

    override fun getSpriteWidth(): Int {
        return texture2d.singleWidth + translateX + trimRight
    }

    override fun drawAffineMasked(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, class4558: ComponentSurface, i_9: Int, i_10: Int) {
        val class137Sub1Sub111 = (class4558 as SpriteSurfaceGL).surface
        var x1 = f_1
        var y1 = f_2
        var x2 = f_3
        var y2 = f_4
        var x3 = f_5
        var y3 = f_6
        var f_12: Float
        var f_13: Float
        var f_14: Float
        var f_15: Float
        if (stretched) {
            f_12 = getSpriteWidth().toFloat()
            f_13 = getSpriteHeight().toFloat()
            f_14 = (x2 - x1) / f_12
            f_15 = (y2 - y1) / f_12
            val f_16 = (x3 - x1) / f_13
            val f_17 = (y3 - y1) / f_13
            val f_18 = f_16 * translateY
            val f_19 = f_17 * translateY
            val f_20 = f_14 * translateX
            val f_21 = f_15 * translateX
            val f_22 = -f_14 * trimRight
            val f_23 = -f_15 * trimRight
            val f_24 = -f_16 * trimBottom
            val f_25 = -f_17 * trimBottom
            x1 = x1 + f_20 + f_18
            y1 = y1 + f_21 + f_19
            x2 = x2 + f_22 + f_18
            y2 = y2 + f_23 + f_19
            x3 = x3 + f_20 + f_24
            y3 = y3 + f_21 + f_25
        }

        f_12 = x3 + (x2 - x1)
        f_13 = y2 + (y3 - y1)
        texture2d.setLinearTextureFilter((i_7 and 0x1) != 0)
        toolkit.setupSpriteRendering()
        toolkit.setActiveTexture(texture2d)
        toolkit.loadTextureCombineMode(1)
        toolkit.activateTexture(1)
        toolkit.setActiveTexture(class137Sub1Sub111)
        toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
        toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        toolkit.setBlendMode(1)
        f_14 = class137Sub1Sub111.s / class137Sub1Sub111.singleWidth
        f_15 = class137Sub1Sub111.t / class137Sub1Sub111.singleLength
        OpenGL.glBegin(7)
        OpenGL.glColor3f(1.0f, 1.0f, 1.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0f, texture2d.t)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_14 * (x1 - i_9), class137Sub1Sub111.t - f_15 * (y1 - i_10))
        OpenGL.glVertex2f(x1, y1)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0f, 0.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_14 * (x3 - i_9), class137Sub1Sub111.t - f_15 * (y3 - i_10))
        OpenGL.glVertex2f(x3, y3)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, texture2d.s, 0.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_14 * (f_12 - i_9), class137Sub1Sub111.t - f_15 * (f_13 - i_10))
        OpenGL.glVertex2f(f_12, f_13)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, texture2d.s, texture2d.t)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, f_14 * (x2 - i_9), class137Sub1Sub111.t - f_15 * (y2 - i_10))
        OpenGL.glVertex2f(x2, y2)
        OpenGL.glEnd()
        toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        toolkit.loadTextureCombineMode(0)
        toolkit.setActiveTexture(null)
        toolkit.activateTexture(0)
    }

    override fun cacheSizeInto(ints_1: IntArray) {
        ints_1[0] = translateX
        ints_1[1] = translateY
        ints_1[2] = trimRight
        ints_1[3] = trimBottom
    }

    override fun getFramebuffer(): FrameBuffer {
        return texture2d.createFrameBuffer()
    }

    fun setupChannelMaskRendering(i_1: Int) {
        toolkit.activateTexture(1)
        toolkit.setActiveTexture(texture2d)
        toolkit.loadTextureCombineMode(toolkit.getCombineModeEffectType(i_1), GL_REPLACE)
        toolkit.setTexture(1, GL_PRIMARY_COLOR, GL_SRC_COLOR)
        toolkit.setTextureTransparency(0, GL_PREVIOUS)
        toolkit.activateTexture(0)
        toolkit.setActiveTexture(channelMaskTexture)
        toolkit.loadTextureCombineMode(GL_DOT3_RGBA, GL_REPLACE)
        toolkit.setTexture(1, GL_CONSTANT, GL_SRC_COLOR)
        when (channelMaskIndex) {
            0 -> toolkit.updatePointSizeRange(1.0f, 0.5f, 0.5f, 0.0f)
            1 -> toolkit.updatePointSizeRange(0.5f, 1.0f, 0.5f, 0.0f)
            2 -> toolkit.updatePointSizeRange(0.5f, 0.5f, 1.0f, 0.0f)
            3 -> toolkit.updatePointSizeRange(128.5f, 128.5f, 128.5f, 0.0f)
        }
    }
}
