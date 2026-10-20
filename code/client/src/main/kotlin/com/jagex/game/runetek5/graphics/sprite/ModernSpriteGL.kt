package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.texture.OpenGl2DImageTexture
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.SpriteSurfaceGL

class ModernSpriteGL : SpriteGL {

    constructor(toolkit: ModernGLToolkit, width: Int, height: Int, pixels: IntArray, stride: Int, srcWidth: Int)
        : super(toolkit, width, height, pixels, stride, srcWidth)

    constructor(toolkit: ModernGLToolkit, x: Int, y: Int, width: Int, height: Int)
        : super(toolkit, x, y, width, height)

    constructor(toolkit: ModernGLToolkit, width: Int, height: Int, hasAlpha: Boolean)
        : super(toolkit, width, height, hasAlpha)

    private val modernToolkit: ModernGLToolkit
        get() = toolkit as ModernGLToolkit

    private fun useModernPath(): Boolean {
        val modern = modernToolkit
        val spriteRenderer = modern.getSpriteRenderer() ?: return false
        return spriteRenderer.isReady() && modern.isModernRenderingActive()
    }

    private fun shaderColor(combineMode: Int, colorArgb: Int): Int =
        if (combineMode == 1) -1 else colorArgb

    private val SPRITE_DIAG = System.getProperty("render.spritediag") != null
    private var spriteDiagBudget = 600

    private fun spriteDiag(method: String, x: Int, y: Int, w: Int, h: Int, combineMode: Int, colorArgb: Int) {
        if (!SPRITE_DIAG || spriteDiagBudget <= 0) return
        if (y < 250 || y > 340 || x < 250 || x > 520) return
        spriteDiagBudget--
        System.err.println("[SpriteDiag] $method x=$x y=$y w=$w h=$h tiled=${texture2d.tiled} stretched=$stretched s=${texture2d.s} t=${texture2d.t} single=${texture2d.singleWidth}x${texture2d.singleLength} sprite=${getSpriteWidth()}x${getSpriteHeight()} translate=$translateX,$translateY trim=$trimRight,$trimBottom combine=$combineMode color=${Integer.toHexString(colorArgb)} linear=${texture2d.linearTextureFilter} forceNearest=${texture2d.forceNearestFilter} texClass=${texture2d.javaClass.simpleName}")
    }

    private fun useFilter(linear: Boolean) {
        val effective = linear && !texture2d.forceNearestFilter
        if (texture2d.linearTextureFilter == effective) return
        modernToolkit.getSpriteRenderer()?.flush("filter")
        texture2d.setLinearTextureFilter(linear)
    }

    override fun draw(x: Int, y: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        spriteDiag("draw", x, y, getSpriteWidth(), getSpriteHeight(), combineMode, colorArgb)
        useFilter(false)
        if (channelMaskTexture != null) {
            modernToolkit.countLegacySkip("SpriteGL.channelMask(approx)")
        }

        if (!useModernPath()) {
            modernToolkit.countLegacySkip("SpriteGL.draw(skipped)")
            return
        }

        val spriteRenderer = modernToolkit.getSpriteRenderer()!!
        modernToolkit.ensureSpriteBatchForFont()

        val drawX = x + translateX
        val drawY = y + translateY
        val textureHandle = texture2d.activeHandle
        if (textureHandle <= 0) return

        spriteRenderer.drawSprite(
            textureId = textureHandle,
            x0 = drawX.toFloat(),
            y0 = drawY.toFloat(),
            x1 = (drawX + texture2d.singleWidth).toFloat(),
            y1 = (drawY + texture2d.singleLength).toFloat(),
            u0 = 0.0f,
            v0 = texture2d.t,
            u1 = texture2d.s,
            v1 = 0.0f,
            colorArgb = shaderColor(combineMode, colorArgb),
            blendMode = blendMode
        )
    }

    fun drawStretchedMask(x: Int, y: Int, width: Int, height: Int, colorArgb: Int) {
        useFilter(false)
        if (!useModernPath()) {
            modernToolkit.countLegacySkip("SpriteGL.drawStretchedMask(skipped)")
            return
        }
        val spriteRenderer = modernToolkit.getSpriteRenderer()!!
        modernToolkit.ensureSpriteBatchForFont()
        val textureHandle = texture2d.activeHandle
        if (textureHandle <= 0) return
        spriteRenderer.drawSprite(
            textureId = textureHandle,
            x0 = x.toFloat(),
            y0 = y.toFloat(),
            x1 = (x + width).toFloat(),
            y1 = (y + height).toFloat(),
            u0 = 0.0f,
            v0 = texture2d.t,
            u1 = texture2d.s,
            v1 = 0.0f,
            colorArgb = colorArgb,
            blendMode = 1
        )
    }

    override fun drawScaledColored(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        spriteDiag("scaled", x, y, width, height, combineMode, colorArgb)
        useFilter(false)
        if (!useModernPath()) {
            modernToolkit.countLegacySkip("SpriteGL.drawScaledColored(skipped)")
            return
        }

        val spriteRenderer = modernToolkit.getSpriteRenderer()!!
        modernToolkit.ensureSpriteBatchForFont()

        val textureHandle = texture2d.activeHandle
        if (textureHandle <= 0) return

        val endX = x + width
        val endY = y + height

        if (texture2d.tiled && !stretched) {
            val t = texture2d.t * height / texture2d.singleLength
            val s = texture2d.s * width / texture2d.singleWidth

            spriteRenderer.drawSprite(
                textureId = textureHandle,
                x0 = x.toFloat(), y0 = y.toFloat(),
                x1 = endX.toFloat(), y1 = endY.toFloat(),
                u0 = 0.0f, v0 = t,
                u1 = s, v1 = 0.0f,
                colorArgb = shaderColor(combineMode, colorArgb),
                blendMode = blendMode
            )
        } else {
            val spriteWidth = getSpriteWidth()
            val spriteHeight = getSpriteHeight()

            var startY = y
            var tileEndY = texture2d.singleLength + y

            while (tileEndY <= endY) {
                var startX = x
                var tileEndX = texture2d.singleWidth + x

                while (tileEndX <= endX) {
                    spriteRenderer.drawSprite(
                        textureId = textureHandle,
                        x0 = (startX + translateX).toFloat(), y0 = (startY + translateY).toFloat(),
                        x1 = (tileEndX + translateX).toFloat(), y1 = (tileEndY + translateY).toFloat(),
                        u0 = 0.0f, v0 = texture2d.t,
                        u1 = texture2d.s, v1 = 0.0f,
                        colorArgb = shaderColor(combineMode, colorArgb),
                        blendMode = blendMode
                    )
                    tileEndX += spriteWidth
                    startX += spriteWidth
                }

                if (startX < endX) {
                    val partialS = texture2d.s * (endX - startX) / texture2d.singleWidth
                    spriteRenderer.drawSprite(
                        textureId = textureHandle,
                        x0 = (startX + translateX).toFloat(), y0 = (startY + translateY).toFloat(),
                        x1 = (endX + translateX).toFloat(), y1 = (tileEndY + translateY).toFloat(),
                        u0 = 0.0f, v0 = texture2d.t,
                        u1 = partialS, v1 = 0.0f,
                        colorArgb = shaderColor(combineMode, colorArgb),
                        blendMode = blendMode
                    )
                }

                startY += spriteHeight
                tileEndY += spriteHeight
            }

            if (startY < endY) {
                val partialT = texture2d.t * (texture2d.singleLength - (endY - startY)) / texture2d.singleLength
                var startX = x
                var tileEndX = texture2d.singleWidth + x

                while (tileEndX <= endX) {
                    spriteRenderer.drawSprite(
                        textureId = textureHandle,
                        x0 = (startX + translateX).toFloat(), y0 = (startY + translateY).toFloat(),
                        x1 = (tileEndX + translateX).toFloat(), y1 = (endY + translateY).toFloat(),
                        u0 = 0.0f, v0 = texture2d.t,
                        u1 = texture2d.s, v1 = partialT,
                        colorArgb = shaderColor(combineMode, colorArgb),
                        blendMode = blendMode
                    )
                    tileEndX += spriteWidth
                    startX += spriteWidth
                }

                if (startX < endX) {
                    val partialS = texture2d.s * (endX - startX) / texture2d.singleWidth
                    spriteRenderer.drawSprite(
                        textureId = textureHandle,
                        x0 = (startX + translateX).toFloat(), y0 = (startY + translateY).toFloat(),
                        x1 = (endX + translateX).toFloat(), y1 = (endY + translateY).toFloat(),
                        u0 = 0.0f, v0 = texture2d.t,
                        u1 = partialS, v1 = partialT,
                        colorArgb = shaderColor(combineMode, colorArgb),
                        blendMode = blendMode
                    )
                }
            }
        }
    }

    override fun drawColoredCircle(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        spriteDiag("circle", x, y, width, height, combineMode, colorArgb)
        useFilter(true)
        if (channelMaskTexture != null) {
            modernToolkit.countLegacySkip("SpriteGL.channelMask(approx)")
        }

        if (!useModernPath()) {
            modernToolkit.countLegacySkip("SpriteGL.drawColoredCircle(skipped)")
            return
        }

        val spriteRenderer = modernToolkit.getSpriteRenderer()!!
        modernToolkit.ensureSpriteBatchForFont()

        val textureHandle = texture2d.activeHandle
        if (textureHandle <= 0) return

        if (stretched) {
            val scaleX = width.toFloat() / getSpriteWidth()
            val scaleY = height.toFloat() / getSpriteHeight()
            val drawX = x + translateX * scaleX
            val drawY = y + translateY * scaleY
            val endX = drawX + texture2d.singleWidth * scaleX
            val endY = drawY + texture2d.singleLength * scaleY

            spriteRenderer.drawSprite(
                textureId = textureHandle,
                x0 = drawX, y0 = drawY,
                x1 = endX, y1 = endY,
                u0 = 0.0f, v0 = texture2d.t,
                u1 = texture2d.s, v1 = 0.0f,
                colorArgb = shaderColor(combineMode, colorArgb),
                blendMode = blendMode
            )
        } else {
            spriteRenderer.drawSprite(
                textureId = textureHandle,
                x0 = x.toFloat(), y0 = y.toFloat(),
                x1 = (x + width).toFloat(), y1 = (y + height).toFloat(),
                u0 = 0.0f, v0 = texture2d.t,
                u1 = texture2d.s, v1 = 0.0f,
                colorArgb = shaderColor(combineMode, colorArgb),
                blendMode = blendMode
            )
        }
    }

    override fun drawAffine(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, combineMode: Int, colorArgb: Int, blendMode: Int) {
        spriteDiag("affine", topLeftX.toInt(), topLeftY.toInt(), (topRightX - topLeftX).toInt(), (bottomLeftY - topLeftY).toInt(), combineMode, colorArgb)
        useFilter(true)
        if (!useModernPath()) {
            modernToolkit.countLegacySkip("SpriteGL.drawAffine(skipped)")
            return
        }

        val spriteRenderer = modernToolkit.getSpriteRenderer()!!
        modernToolkit.ensureSpriteBatchForFont()

        val textureHandle = texture2d.activeHandle
        if (textureHandle <= 0) return

        var x1 = topLeftX
        var y1 = topLeftY
        var x2 = topRightX
        var y2 = topRightY
        var x3 = bottomLeftX
        var y3 = bottomLeftY

        if (stretched) {
            val spriteWidth = getSpriteWidth().toFloat()
            val spriteHeight = getSpriteHeight().toFloat()
            val scaleXx = (x2 - x1) / spriteWidth
            val scaleXy = (y2 - y1) / spriteWidth
            val scaleYx = (x3 - x1) / spriteHeight
            val scaleYy = (y3 - y1) / spriteHeight
            val transYx = scaleYx * translateY
            val transYy = scaleYy * translateY
            val transXx = scaleXx * translateX
            val transXy = scaleXy * translateX
            val trimRightX = -scaleXx * trimRight
            val trimRightY = -scaleXy * trimRight
            val trimBottomX = -scaleYx * trimBottom
            val trimBottomY = -scaleYy * trimBottom
            x1 = x1 + transXx + transYx
            y1 = y1 + transXy + transYy
            x2 = x2 + trimRightX + transYx
            y2 = y2 + trimRightY + transYy
            x3 = x3 + transXx + trimBottomX
            y3 = y3 + transXy + trimBottomY
        }

        val x4 = x3 + (x2 - x1)
        val y4 = y2 + (y3 - y1)

        spriteRenderer.drawQuad(
            textureId = textureHandle,
            x0 = x1, y0 = y1, u0 = 0.0f, v0 = texture2d.t,
            x1 = x3, y1 = y3, u1 = 0.0f, v1 = 0.0f,
            x2 = x4, y2 = y4, u2 = texture2d.s, v2 = 0.0f,
            x3 = x2, y3 = y2, u3 = texture2d.s, v3 = texture2d.t,
            colorArgb = shaderColor(combineMode, colorArgb),
            blendMode = blendMode
        )
    }

    override fun captureScreenRegion(offsetX: Int, offsetY: Int, width: Int, height: Int, x: Int, y: Int) {
        modernToolkit.flushAllPendingDraws()
        super.captureScreenRegion(offsetX, offsetY, width, height, x, y)
    }

    override fun drawWithSurface(x: Int, y: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        spriteDiag("surface", x, y, getSpriteWidth(), getSpriteHeight(), 0, 0)
        useFilter(false)
        val maskTexture = (surface as? SpriteSurfaceGL)?.surface
        if (maskTexture == null) {
            modernToolkit.countLegacySkip("SpriteGL.drawWithSurface(skipped)")
            return
        }
        val baseHandle = texture2d.activeHandle
        val maskHandle = maskTexture.activeHandle
        if (baseHandle <= 0 || maskHandle <= 0) {
            modernToolkit.countLegacySkip("SpriteGL.drawWithSurface(skipped)")
            return
        }

        val drawX = (x + translateX).toFloat()
        val drawY = (y + translateY).toFloat()
        val endX = drawX + texture2d.singleWidth
        val endY = drawY + texture2d.singleLength
        val sScale = maskTexture.s / maskTexture.singleWidth
        val tScale = maskTexture.t / maskTexture.singleLength
        val mu0 = (drawX - surfaceOffsetX) * sScale
        val mu1 = (endX - surfaceOffsetX) * sScale
        val mv0 = maskTexture.t - (drawY - surfaceOffsetY) * tScale
        val mv1 = maskTexture.t - (endY - surfaceOffsetY) * tScale

        if (!modernToolkit.drawSurfaceMaskedQuad(
                baseHandle, maskHandle,
                drawX, drawY, 0f, texture2d.t, mu0, mv0,
                drawX, endY, 0f, 0f, mu0, mv1,
                endX, endY, texture2d.s, 0f, mu1, mv1,
                endX, drawY, texture2d.s, texture2d.t, mu1, mv0
            )
        ) {
            modernToolkit.countLegacySkip("SpriteGL.drawWithSurface(skipped)")
        }
    }

    override fun drawAffineMasked(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, flags: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        val maskTexture = (surface as? SpriteSurfaceGL)?.surface
        if (maskTexture == null) {
            modernToolkit.countLegacySkip("SpriteGL.drawAffineMasked(skipped)")
            return
        }
        val baseHandle = texture2d.activeHandle
        val maskHandle = maskTexture.activeHandle
        if (baseHandle <= 0 || maskHandle <= 0) {
            modernToolkit.countLegacySkip("SpriteGL.drawAffineMasked(skipped)")
            return
        }

        texture2d.setLinearTextureFilter((flags and 0x1) != 0)

        var x1 = topLeftX
        var y1 = topLeftY
        var x2 = topRightX
        var y2 = topRightY
        var x3 = bottomLeftX
        var y3 = bottomLeftY
        if (stretched) {
            val spriteWidth = getSpriteWidth().toFloat()
            val spriteHeight = getSpriteHeight().toFloat()
            val scaleXx = (x2 - x1) / spriteWidth
            val scaleXy = (y2 - y1) / spriteWidth
            val scaleYx = (x3 - x1) / spriteHeight
            val scaleYy = (y3 - y1) / spriteHeight
            val transYx = scaleYx * translateY
            val transYy = scaleYy * translateY
            val transXx = scaleXx * translateX
            val transXy = scaleXy * translateX
            val trimRightX = -scaleXx * trimRight
            val trimRightY = -scaleXy * trimRight
            val trimBottomX = -scaleYx * trimBottom
            val trimBottomY = -scaleYy * trimBottom
            x1 = x1 + transXx + transYx
            y1 = y1 + transXy + transYy
            x2 = x2 + trimRightX + transYx
            y2 = y2 + trimRightY + transYy
            x3 = x3 + transXx + trimBottomX
            y3 = y3 + transXy + trimBottomY
        }
        val x4 = x3 + (x2 - x1)
        val y4 = y2 + (y3 - y1)

        val sScale = maskTexture.s / maskTexture.singleWidth
        val tScale = maskTexture.t / maskTexture.singleLength
        fun mu(px: Float) = sScale * (px - surfaceOffsetX)
        fun mv(py: Float) = maskTexture.t - tScale * (py - surfaceOffsetY)

        if (!modernToolkit.drawSurfaceMaskedQuad(
                baseHandle, maskHandle,
                x1, y1, 0f, texture2d.t, mu(x1), mv(y1),
                x3, y3, 0f, 0f, mu(x3), mv(y3),
                x4, y4, texture2d.s, 0f, mu(x4), mv(y4),
                x2, y2, texture2d.s, texture2d.t, mu(x2), mv(y2),
                texture2d.linearTextureFilter
            )
        ) {
            modernToolkit.countLegacySkip("SpriteGL.drawAffineMasked(skipped)")
        }
    }
}
