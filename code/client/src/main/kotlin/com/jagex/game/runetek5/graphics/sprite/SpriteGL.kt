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
            if (hasAlpha) GLFormat.RGBA else GLFormat.RGB,
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
        channelMaskTexture = OpenGl2DImageTexture.createFromScreenCapture(toolkit, 0, 0, texture2d.singleWidth, texture2d.singleLength)
        channelMaskIndex = 3
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
        if (texture2d.tiled && !stretched) {
            val t = texture2d.t * height / texture2d.singleLength
            val s = texture2d.s * width / texture2d.singleWidth
            toolkit.legacyDrawTrace()
        } else {
            val spriteWidth = getSpriteWidth()
            val spriteHeight = getSpriteHeight()
            var tileEndY = texture2d.singleLength + y
            toolkit.legacyDrawTrace()

            var startY: Int
            var startX: Int
            startY = y
            while (tileEndY <= endY) {
                var tileEndX = texture2d.singleWidth + x
                startX = x
                while (tileEndX <= endX) {
                    tileEndX += spriteWidth
                    startX += spriteWidth
                }

                if (startX < endX) {
                    val partialS = texture2d.s * (endX - startX) / texture2d.singleWidth
                }

                startY += spriteHeight
                tileEndY += spriteHeight
            }

            if (startY < endY) {
                val partialT = texture2d.t * (texture2d.singleLength - (endY - startY)) / texture2d.singleLength
                startX = texture2d.singleWidth + x

                var tileX = x
                while (startX <= endX) {
                    startX += spriteWidth
                    tileX += spriteWidth
                }

                if (tileX < endX) {
                    val partialS = texture2d.s * (endX - tileX) / texture2d.singleWidth
                }
            }

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
        var drawX = x + translateX
        var drawY = y + translateY
        if (channelMaskTexture == null) {
            toolkit.setActiveTexture(texture2d)
            toolkit.loadTextureCombineMode(combineMode)
            toolkit.legacyDrawTrace()
        } else {
            setupChannelMaskRendering(combineMode)
            channelMaskTexture!!.setLinearTextureFilter(false)
            toolkit.legacyDrawTrace()
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
            } else {
                setupChannelMaskRendering(combineMode)
                channelMaskTexture!!.setLinearTextureFilter(true)
                toolkit.legacyDrawTrace()
                resetChannelMaskState()
            }
        } else if (channelMaskTexture == null) {
            toolkit.setActiveTexture(texture2d)
            toolkit.loadTextureCombineMode(combineMode)
            toolkit.legacyDrawTrace()
        } else {
            setupChannelMaskRendering(combineMode)
            channelMaskTexture!!.setLinearTextureFilter(true)
            toolkit.legacyDrawTrace()
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
        toolkit.legacyDrawTrace()
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
