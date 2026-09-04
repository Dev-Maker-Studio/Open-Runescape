package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.graphics.texture.OpenGl2DImageTexture
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.SpriteSurfaceGL
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import jaggl.OpenGL

open class SpriteGL : NativeSprite {

    @JvmField var translateX: Int = 0
    @JvmField var translateY: Int = 0
    @JvmField var trimRight: Int = 0
    @JvmField var trimBottom: Int = 0
    @JvmField var stretched: Boolean = false
    @JvmField var channelMaskIndex: Int = 0
    @JvmField val toolkit: OpenGlToolkit
    @JvmField val texture2d: OpenGl2DImageTexture
    @JvmField var channelMaskTexture: OpenGl2DImageTexture? = null

    /**
     * Legacy immediate-mode sprite drawing is forbidden under ModernGLToolkit
     * (standalone modern renderer — no fixed-function detours). Plain SpriteGL
     * instances only exist under the modern toolkit when they were created before
     * the modern renderer's lazy init; their draws are dropped and counted in the
     * [ModernDiag] legacySkips map. ModernSpriteGL overrides never reach these.
     */
    protected fun blockedUnderModernToolkit(callsite: String): Boolean {
        val tk = toolkit
        if (tk is ModernGLToolkit) {
            tk.countLegacySkip(callsite)
            return true
        }
        return false
    }

    constructor(glToolkit: OpenGlToolkit, width: Int, height: Int, pixels: IntArray, pixelOffset: Int, rowLength: Int) {
        toolkit = glToolkit
        texture2d = OpenGl2DImageTexture.createFromPixels(glToolkit, width, height, pixels, rowLength, pixelOffset)
    }

    constructor(glToolkit: OpenGlToolkit, x: Int, y: Int, width: Int, height: Int) {
        toolkit = glToolkit
        texture2d = OpenGl2DImageTexture.createFromScreenCapture(glToolkit, x, y, width, height)
    }

    constructor(glToolkit: OpenGlToolkit, width: Int, height: Int, hasAlpha: Boolean) {
        toolkit = glToolkit
        texture2d = OpenGl2DImageTexture.createEmpty(
            glToolkit,
            if (hasAlpha) GLFormat.HDR else GLFormat.RGB,
            GLActionFormat.PIXEL_FORMAT,
            width,
            height
        )
    }

    override fun setSize(offsetX: Int, offsetY: Int, rightTrim: Int, bottomTrim: Int) {
        translateX = offsetX
        translateY = offsetY
        trimRight = rightTrim
        trimBottom = bottomTrim
        stretched = translateX != 0 || translateY != 0 || trimRight != 0 || trimBottom != 0
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
        channelMaskTexture = OpenGl2DImageTexture.createFromScreenCapture(toolkit, 0, 0, texture2d.singleWidth, texture2d.singleLength)
        channelMaskIndex = 3
        OpenGL.glPixelTransferf(GL_RED_SCALE, 1.0f)
        OpenGL.glPixelTransferf(GL_RED_BIAS, 0.0f)
        OpenGL.glPixelTransferf(GL_GREEN_SCALE, 1.0f)
        OpenGL.glPixelTransferf(GL_GREEN_BIAS, 0.0f)
        OpenGL.glPixelTransferf(GL_BLUE_SCALE, 1.0f)
        OpenGL.glPixelTransferf(GL_BLUE_BIAS, 0.0f)
    }

    fun updateTextureRegion(x: Int, y: Int, width: Int, height: Int, pixels: IntArray, rowLength: Int) {
        texture2d.updateTextureRegion(x, y, width, height, pixels, rowLength)
    }

    override fun drawScaledColored(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        if (blockedUnderModernToolkit("legacy SpriteGL.drawScaledColored")) return
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
            toolkit.legacyDrawTrace()
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
            var tileEndY = texture2d.singleLength + y
            toolkit.legacyDrawTrace()
            OpenGL.glBegin(GL_QUADS)

            var startY: Int
            var startX: Int
            startY = y
            while (tileEndY <= endY) {
                var tileEndX = texture2d.singleWidth + x
                startX = x
                while (tileEndX <= endX) {
                    OpenGL.glTexCoord2f(0.0f, texture2d.t)
                    OpenGL.glVertex2i(startX, startY)
                    OpenGL.glTexCoord2f(0.0f, 0.0f)
                    OpenGL.glVertex2i(startX, tileEndY)
                    OpenGL.glTexCoord2f(texture2d.s, 0.0f)
                    OpenGL.glVertex2i(tileEndX, tileEndY)
                    OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
                    OpenGL.glVertex2i(tileEndX, startY)
                    tileEndX += spriteWidth
                    startX += spriteWidth
                }

                if (startX < endX) {
                    val partialS = texture2d.s * (endX - startX) / texture2d.singleWidth
                    OpenGL.glTexCoord2f(0.0f, texture2d.t)
                    OpenGL.glVertex2i(startX, startY)
                    OpenGL.glTexCoord2f(0.0f, 0.0f)
                    OpenGL.glVertex2i(startX, tileEndY)
                    OpenGL.glTexCoord2f(partialS, 0.0f)
                    OpenGL.glVertex2i(endX, tileEndY)
                    OpenGL.glTexCoord2f(partialS, texture2d.t)
                    OpenGL.glVertex2i(endX, startY)
                }

                startY += spriteHeight
                tileEndY += spriteHeight
            }

            if (startY < endY) {
                val partialT = texture2d.t * (texture2d.singleLength - (endY - startY)) / texture2d.singleLength
                startX = texture2d.singleWidth + x

                var tileX = x
                while (startX <= endX) {
                    OpenGL.glTexCoord2f(0.0f, texture2d.t)
                    OpenGL.glVertex2i(tileX, startY)
                    OpenGL.glTexCoord2f(0.0f, partialT)
                    OpenGL.glVertex2i(tileX, endY)
                    OpenGL.glTexCoord2f(texture2d.s, partialT)
                    OpenGL.glVertex2i(startX, endY)
                    OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
                    OpenGL.glVertex2i(startX, startY)
                    startX += spriteWidth
                    tileX += spriteWidth
                }

                if (tileX < endX) {
                    val partialS = texture2d.s * (endX - tileX) / texture2d.singleWidth
                    OpenGL.glTexCoord2f(0.0f, texture2d.t)
                    OpenGL.glVertex2i(tileX, startY)
                    OpenGL.glTexCoord2f(0.0f, partialT)
                    OpenGL.glVertex2i(tileX, endY)
                    OpenGL.glTexCoord2f(partialS, partialT)
                    OpenGL.glVertex2i(endX, endY)
                    OpenGL.glTexCoord2f(partialS, texture2d.t)
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
        if (blockedUnderModernToolkit("legacy SpriteGL.draw")) return
        texture2d.setLinearTextureFilter(false)
        toolkit.setupSpriteRendering()
        toolkit.setBlendMode(blendMode)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        var drawX = x + translateX
        var drawY = y + translateY
        if (channelMaskTexture == null) {
            toolkit.setActiveTexture(texture2d)
            toolkit.loadTextureCombineMode(combineMode)
            toolkit.legacyDrawTrace()
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
            toolkit.legacyDrawTrace()
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

    override fun drawWithSurface(x: Int, y: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        if (blockedUnderModernToolkit("legacy SpriteGL.drawWithSurface")) return
        val spriteSurface = surface as SpriteSurfaceGL
        val surfaceTexture = spriteSurface.surface
        texture2d.setLinearTextureFilter(false)
        toolkit.setupSpriteRendering()
        toolkit.setActiveTexture(texture2d)
        toolkit.loadTextureCombineMode(1)
        toolkit.activateTexture(1)
        toolkit.setActiveTexture(surfaceTexture)
        toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
        toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        toolkit.setBlendMode(1)
        val drawX = x + translateX
        val drawY = y + translateY
        val endX = texture2d.singleWidth + drawX
        val endY = texture2d.singleLength + drawY
        val sPerPixel = surfaceTexture.s / surfaceTexture.singleWidth
        val tPerPixel = surfaceTexture.t / surfaceTexture.singleLength
        val s0 = (drawX - surfaceOffsetX) * sPerPixel
        val s1 = (endX - surfaceOffsetX) * sPerPixel
        val t0 = surfaceTexture.t - (drawY - surfaceOffsetY) * tPerPixel
        val t1 = surfaceTexture.t - (endY - surfaceOffsetY) * tPerPixel
        toolkit.legacyDrawTrace()
        OpenGL.glBegin(7)
        OpenGL.glColor3f(1.0f, 1.0f, 1.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0f, texture2d.t)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, s0, t0)
        OpenGL.glVertex2i(drawX, drawY)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0f, 0.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, s0, t1)
        OpenGL.glVertex2i(drawX, texture2d.singleLength + drawY)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, texture2d.s, 0.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, s1, t1)
        OpenGL.glVertex2i(texture2d.singleWidth + drawX, texture2d.singleLength + drawY)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, texture2d.s, texture2d.t)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, s1, t0)
        OpenGL.glVertex2i(texture2d.singleWidth + drawX, drawY)
        OpenGL.glEnd()
        toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        toolkit.loadTextureCombineMode(0)
        toolkit.setActiveTexture(null)
        toolkit.activateTexture(0)
    }

    override fun drawColoredCircle(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        if (blockedUnderModernToolkit("legacy SpriteGL.drawColoredCircle")) return
        texture2d.setLinearTextureFilter(true)
        toolkit.setupSpriteRendering()
        toolkit.setBlendMode(blendMode)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        if (stretched) {
            val scaleX = width.toFloat() / getSpriteWidth()
            val scaleY = height.toFloat() / getSpriteHeight()
            val x0 = x + translateX * scaleX
            val y0 = y + translateY * scaleY
            val x1 = x0 + texture2d.singleWidth * scaleX
            val y1 = y0 + texture2d.singleLength * scaleY
            if (channelMaskTexture == null) {
                toolkit.setActiveTexture(texture2d)
                toolkit.loadTextureCombineMode(combineMode)
                toolkit.legacyDrawTrace()
                OpenGL.glBegin(GL_QUADS)
                OpenGL.glTexCoord2f(0.0f, texture2d.t)
                OpenGL.glVertex2f(x0, y0)
                OpenGL.glTexCoord2f(0.0f, 0.0f)
                OpenGL.glVertex2f(x0, y1)
                OpenGL.glTexCoord2f(texture2d.s, 0.0f)
                OpenGL.glVertex2f(x1, y1)
                OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
                OpenGL.glVertex2f(x1, y0)
                OpenGL.glEnd()
            } else {
                setupChannelMaskRendering(combineMode)
                channelMaskTexture!!.setLinearTextureFilter(true)
                toolkit.legacyDrawTrace()
                OpenGL.glBegin(GL_QUADS)
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, texture2d.t)
                OpenGL.glTexCoord2f(0.0f, texture2d.t)
                OpenGL.glVertex2f(x0, y0)
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 0.0f)
                OpenGL.glTexCoord2f(0.0f, 0.0f)
                OpenGL.glVertex2f(x0, y1)
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, 0.0f)
                OpenGL.glTexCoord2f(texture2d.s, 0.0f)
                OpenGL.glVertex2f(x1, y1)
                OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, texture2d.t)
                OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
                OpenGL.glVertex2f(x1, y0)
                OpenGL.glEnd()
                resetChannelMaskState()
            }
        } else if (channelMaskTexture == null) {
            toolkit.setActiveTexture(texture2d)
            toolkit.loadTextureCombineMode(combineMode)
            toolkit.legacyDrawTrace()
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glTexCoord2f(0.0f, texture2d.t)
            OpenGL.glVertex2i(x, y)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(x, y + height)
            OpenGL.glTexCoord2f(texture2d.s, 0.0f)
            OpenGL.glVertex2i(width + x, y + height)
            OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
            OpenGL.glVertex2i(width + x, y)
            OpenGL.glEnd()
        } else {
            setupChannelMaskRendering(combineMode)
            channelMaskTexture!!.setLinearTextureFilter(true)
            toolkit.legacyDrawTrace()
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, texture2d.t)
            OpenGL.glTexCoord2f(0.0f, texture2d.t)
            OpenGL.glVertex2i(x, y)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 0.0f)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(x, y + height)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, 0.0f)
            OpenGL.glTexCoord2f(texture2d.s, 0.0f)
            OpenGL.glVertex2i(width + x, y + height)
            OpenGL.glMultiTexCoord2f(GL_TEXTURE1, texture2d.s, texture2d.t)
            OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
            OpenGL.glVertex2i(width + x, y)
            OpenGL.glEnd()
            resetChannelMaskState()
        }
    }

    override fun drawAffine(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, combineMode: Int, colorArgb: Int, blendMode: Int) {
        if (blockedUnderModernToolkit("legacy SpriteGL.drawAffine")) return
        var x1 = topLeftX
        var y1 = topLeftY
        var x2 = topRightX
        var y2 = topRightY
        var x3 = bottomLeftX
        var y3 = bottomLeftY
        var x4: Float
        var y4: Float
        if (stretched) {
            x4 = getSpriteWidth().toFloat()
            y4 = getSpriteHeight().toFloat()
            val uX = (x2 - x1) / x4
            val uY = (y2 - y1) / x4
            val vX = (x3 - x1) / y4
            val vY = (y3 - y1) / y4
            val padTopX = vX * translateY
            val padTopY = vY * translateY
            val padLeftX = uX * translateX
            val padLeftY = uY * translateX
            val trimRightX = -uX * trimRight
            val trimRightY = -uY * trimRight
            val trimBottomX = -vX * trimBottom
            val trimBottomY = -vY * trimBottom
            x1 = x1 + padLeftX + padTopX
            y1 = y1 + padLeftY + padTopY
            x2 = x2 + trimRightX + padTopX
            y2 = y2 + trimRightY + padTopY
            x3 = x3 + padLeftX + trimBottomX
            y3 = y3 + padLeftY + trimBottomY
        }

        x4 = x3 + (x2 - x1)
        y4 = y2 + (y3 - y1)
        texture2d.setLinearTextureFilter(true)
        toolkit.setupSpriteRendering()
        toolkit.setActiveTexture(texture2d)
        toolkit.setBlendMode(blendMode)
        toolkit.loadTextureCombineMode(combineMode)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        toolkit.legacyDrawTrace()
        OpenGL.glBegin(7)
        OpenGL.glTexCoord2f(0.0f, texture2d.t)
        OpenGL.glVertex2f(x1, y1)
        OpenGL.glTexCoord2f(0.0f, 0.0f)
        OpenGL.glVertex2f(x3, y3)
        OpenGL.glTexCoord2f(texture2d.s, 0.0f)
        OpenGL.glVertex2f(x4, y4)
        OpenGL.glTexCoord2f(texture2d.s, texture2d.t)
        OpenGL.glVertex2f(x2, y2)
        OpenGL.glEnd()
    }

    override fun getSpriteWidth(): Int {
        return texture2d.singleWidth + translateX + trimRight
    }

    override fun drawAffineMasked(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, combineMode: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        if (blockedUnderModernToolkit("legacy SpriteGL.drawAffineMasked")) return
        val surfaceTexture = (surface as SpriteSurfaceGL).surface
        var x1 = topLeftX
        var y1 = topLeftY
        var x2 = topRightX
        var y2 = topRightY
        var x3 = bottomLeftX
        var y3 = bottomLeftY
        var x4: Float
        var y4: Float
        var uX: Float
        var uY: Float
        if (stretched) {
            x4 = getSpriteWidth().toFloat()
            y4 = getSpriteHeight().toFloat()
            uX = (x2 - x1) / x4
            uY = (y2 - y1) / x4
            val vX = (x3 - x1) / y4
            val vY = (y3 - y1) / y4
            val padTopX = vX * translateY
            val padTopY = vY * translateY
            val padLeftX = uX * translateX
            val padLeftY = uY * translateX
            val trimRightX = -uX * trimRight
            val trimRightY = -uY * trimRight
            val trimBottomX = -vX * trimBottom
            val trimBottomY = -vY * trimBottom
            x1 = x1 + padLeftX + padTopX
            y1 = y1 + padLeftY + padTopY
            x2 = x2 + trimRightX + padTopX
            y2 = y2 + trimRightY + padTopY
            x3 = x3 + padLeftX + trimBottomX
            y3 = y3 + padLeftY + trimBottomY
        }

        x4 = x3 + (x2 - x1)
        y4 = y2 + (y3 - y1)
        texture2d.setLinearTextureFilter((combineMode and 0x1) != 0)
        toolkit.setupSpriteRendering()
        toolkit.setActiveTexture(texture2d)
        toolkit.loadTextureCombineMode(1)
        toolkit.activateTexture(1)
        toolkit.setActiveTexture(surfaceTexture)
        toolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
        toolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        toolkit.setBlendMode(1)
        uX = surfaceTexture.s / surfaceTexture.singleWidth
        uY = surfaceTexture.t / surfaceTexture.singleLength
        toolkit.legacyDrawTrace()
        OpenGL.glBegin(7)
        OpenGL.glColor3f(1.0f, 1.0f, 1.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0f, texture2d.t)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, uX * (x1 - surfaceOffsetX), surfaceTexture.t - uY * (y1 - surfaceOffsetY))
        OpenGL.glVertex2f(x1, y1)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, 0.0f, 0.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, uX * (x3 - surfaceOffsetX), surfaceTexture.t - uY * (y3 - surfaceOffsetY))
        OpenGL.glVertex2f(x3, y3)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, texture2d.s, 0.0f)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, uX * (x4 - surfaceOffsetX), surfaceTexture.t - uY * (y4 - surfaceOffsetY))
        OpenGL.glVertex2f(x4, y4)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE0, texture2d.s, texture2d.t)
        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, uX * (x2 - surfaceOffsetX), surfaceTexture.t - uY * (y2 - surfaceOffsetY))
        OpenGL.glVertex2f(x2, y2)
        OpenGL.glEnd()
        toolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        toolkit.loadTextureCombineMode(0)
        toolkit.setActiveTexture(null)
        toolkit.activateTexture(0)
    }

    override fun cacheSizeInto(sizeOut: IntArray) {
        sizeOut[0] = translateX
        sizeOut[1] = translateY
        sizeOut[2] = trimRight
        sizeOut[3] = trimBottom
    }

    override fun getFramebuffer(): FrameBuffer {
        return texture2d.createFrameBuffer()
    }

    fun setupChannelMaskRendering(combineMode: Int) {
        toolkit.activateTexture(1)
        toolkit.setActiveTexture(texture2d)
        toolkit.loadTextureCombineMode(toolkit.getCombineModeEffectType(combineMode), GL_REPLACE)
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
