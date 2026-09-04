package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.texture.OpenGl2DImageTexture
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.SpriteSurfaceGL

/**
 * Modern GL routing layer for sprite rendering.
 *
 * This class extends SpriteGL to intercept the draw methods that would normally
 * call jaggl.OpenGL directly (glBegin, glVertex, glTexCoord, glEnd, etc.) and
 * routes them through ModernSpriteRenderer when running under ModernGLToolkit.
 *
 * When the modern renderer is active, all draw calls are converted to batched
 * textured quads via the ModernSpriteRenderer. When the modern renderer is not
 * ready (e.g., during early startup), calls fall through to the legacy SpriteGL
 * implementation via super.
 *
 * This class lives in graphics/sprite/ (not graphics/modern/) because it extends
 * SpriteGL which imports jaggl. The anti-hack rule prohibits jaggl imports in
 * graphics/modern/ files. This file acts as the bridge between the legacy sprite
 * system and the modern renderer.
 *
 * SpriteGL methods that are overridden and their legacy GL usage:
 * - draw(x, y, combineMode, color, blendMode) -- glBegin/glEnd, glTexCoord, glVertex, glColor4ub
 * - drawScaledColored(x, y, w, h, combine, color, blend) -- tiled sprite drawing with glBegin/glEnd
 * - drawColoredCircle(x, y, w, h, combine, color, blend) -- scaled sprite with glBegin/glEnd
 * - drawAffine(6 floats, combine, color, blend) -- rotated quad with glBegin/glEnd
 * - drawWithSurface(x, y, surface, ox, oy) -- multi-texture surface overlay (GL_TEXTURE1)
 * - drawAffineMasked(6 floats, flags, surface, ox, oy) -- rotated multi-texture surface
 */
class ModernSpriteGL : SpriteGL {

    constructor(toolkit: ModernGLToolkit, width: Int, height: Int, pixels: IntArray, stride: Int, srcWidth: Int)
        : super(toolkit, width, height, pixels, stride, srcWidth)

    constructor(toolkit: ModernGLToolkit, x: Int, y: Int, width: Int, height: Int)
        : super(toolkit, x, y, width, height)

    constructor(toolkit: ModernGLToolkit, width: Int, height: Int, hasAlpha: Boolean)
        : super(toolkit, width, height, hasAlpha)

    /**
     * Returns the ModernGLToolkit instance cast from the parent's toolkit field.
     * Safe because ModernSpriteGL is only constructed from ModernGLToolkit.
     */
    private val modernToolkit: ModernGLToolkit
        get() = toolkit as ModernGLToolkit

    /**
     * Returns true if the modern sprite renderer is available and we should use it
     * instead of falling through to the legacy SpriteGL draw methods.
     */
    private fun useModernPath(): Boolean {
        val modern = modernToolkit
        val spriteRenderer = modern.getSpriteRenderer() ?: return false
        return spriteRenderer.isReady() && modern.isModernRenderingActive()
    }

    /**
     * Maps the legacy (combineMode, colorArgb) pair to the vertex color for the
     * modern sprite shader (finalColor = texture * vertexColor).
     *
     * Legacy fixed-function reference — SpriteGL.draw sets glColor4ub(colorArgb) and
     * then loadTextureCombineMode(combineMode) (SpriteGL.kt:216-221), where mode 1 is
     * GL_REPLACE/GL_REPLACE (OpenGlToolkit.kt:1935-1937): the primary color is
     * IGNORED and the texture passes through unchanged. The default sprite wrappers
     * (NativeSprite.draw(x,y) at NativeSprite.kt:39-41, drawTiled at :49-51,
     * drawRotatedCenter at :53-55) all pass combineMode=1 with colorArgb=0 — feeding
     * that 0 into the multiply shader rendered every default sprite draw BLACK with
     * alpha 0. REPLACE maps to opaque white; mode 0 (GL_MODULATE/GL_MODULATE) is the
     * shader's native behavior. Modes 2/3/4 are unused by sprite draws and treated
     * as MODULATE.
     */
    private fun shaderColor(combineMode: Int, colorArgb: Int): Int =
        if (combineMode == 1) -1 else colorArgb

    // ===== Basic textured sprite drawing =====

    /**
     * Draws a textured sprite at (x, y) with optional color tinting and blend mode.
     *
     * Legacy: calls toolkit.setupSpriteRendering(), toolkit.setActiveTexture(),
     * toolkit.loadTextureCombineMode(), toolkit.setBlendMode(), then
     * OpenGL.glColor4ub, OpenGL.glBegin(GL_QUADS), glTexCoord2f/glVertex2i x4, glEnd.
     *
     * Modern: draws a textured quad through ModernSpriteRenderer batch.
     *
     * Note: channelMaskTexture multi-texture path (GL_DOT3_RGBA) is not supported
     * in the modern path. Normal-mapped sprites fall through to legacy rendering.
     */
    /**
     * Legacy sets the texture's min/mag filter per draw call (nearest for draw /
     * scaled / surface, linear for circle / affine). The modern batch defers the
     * actual draw, so a filter change flushes quads already queued with the old
     * filter first. Without this, a scaled sprite kept whatever filter the last
     * affine draw left on it and linear sampling bled the padding texels into the
     * stretched middle of interface buttons.
     */
    private val SPRITE_DIAG = System.getProperty("render.spritediag") != null
    private var spriteDiagBudget = 600

    private fun spriteDiag(method: String, x: Int, y: Int, w: Int, h: Int, combineMode: Int, colorArgb: Int) {
        if (!SPRITE_DIAG || spriteDiagBudget <= 0) return
        if (y < 250 || y > 340 || x < 250 || x > 520) return
        spriteDiagBudget--
        System.err.println("[SpriteDiag] $method x=$x y=$y w=$w h=$h tiled=${texture2d.tiled} stretched=$stretched s=${texture2d.s} t=${texture2d.t} single=${texture2d.singleWidth}x${texture2d.singleLength} sprite=${getSpriteWidth()}x${getSpriteHeight()} translate=$translateX,$translateY trim=$trimRight,$trimBottom combine=$combineMode color=${Integer.toHexString(colorArgb)} linear=${texture2d.linearTextureFilter} forceNearest=${texture2d.forceNearestFilter} texClass=${texture2d.javaClass.simpleName}")
    }

    private fun useFilter(linear: Boolean) {
        // Padded (power-of-two) sprite textures force nearest filtering; their base
        // constructor turned linear on before that flag existed, so the effective state
        // is what legacy's per-draw call converges to: nearest for them, [linear] otherwise.
        val effective = linear && !texture2d.forceNearestFilter
        if (texture2d.linearTextureFilter == effective) return
        modernToolkit.getSpriteRenderer()?.flush("filter")
        texture2d.setLinearTextureFilter(linear)
    }

    override fun draw(x: Int, y: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        spriteDiag("draw", x, y, getSpriteWidth(), getSpriteHeight(), combineMode, colorArgb)
        useFilter(false)
        // Normal-map (GL_DOT3_RGBA channel mask) sprites need a dedicated shader
        // variant; until then draw the base sprite without the mask (approximation,
        // counted as a parity gap — NO legacy fallback under the modern toolkit).
        if (channelMaskTexture != null) {
            modernToolkit.countLegacySkip("SpriteGL.channelMask(approx)")
        }

        if (!useModernPath()) {
            modernToolkit.countLegacySkip("SpriteGL.draw(skipped)")
            return
        }

        val spriteRenderer = modernToolkit.getSpriteRenderer()!!
        modernToolkit.ensureSpriteBatchForFont() // Ensures sprite batch is active

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

    // ===== Tiled sprite drawing =====

    /**
     * Draws a tiled/repeated sprite filling the given rectangle.
     *
     * Legacy: Uses glBegin/glEnd with multiple quads for tiling when the sprite
     * needs to be repeated. Single quad for simple cases.
     *
     * Modern: Emits multiple quads through the sprite batch for tiling, or a
     * single quad for simple cases.
     */
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
            // Simple tiled case -- scale UVs to cover the area
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
            // Tiled repeating -- emit multiple quads
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

                // Partial column at right edge
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

            // Partial row at bottom
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

                // Bottom-right corner partial
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

    // ===== Scaled sprite drawing =====

    /**
     * Draws a sprite scaled to fit a rectangle, with optional color tinting.
     *
     * Legacy: calls glBegin(GL_QUADS) with scaled vertex positions.
     * Modern: draws a single textured quad through the sprite batch.
     */
    override fun drawColoredCircle(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        spriteDiag("circle", x, y, width, height, combineMode, colorArgb)
        useFilter(true)
        // Channel-mask variant approximated without the mask (counted parity gap)
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
            // Stretched sprites need per-component scaling
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

    // ===== Rotated sprite drawing =====

    /**
     * Draws a rotated/transformed sprite as an arbitrary quad.
     *
     * Legacy: calls glBegin(7) [GL_QUADS] with transformed float vertex positions.
     * Modern: uses drawQuad to emit an arbitrary quad.
     *
     * The 6 float parameters define 3 corners of a parallelogram:
     * (topLeftX, topLeftY), (topRightX, topRightY), (bottomLeftX, bottomLeftY)
     * The 4th corner (bottom-right) is computed as: bottom-left + (top-right - top-left)
     */
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

        // Apply stretch/translate transforms matching legacy drawAffine
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

        // 4th corner = bottom-left + (top-right - top-left)
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

    // ===== Multi-texture surface overlay methods =====
    // These use GL_TEXTURE1 with complex combine modes (GL_REPLACE, GL_MODULATE,
    // GL_DOT3_RGBA). They require dedicated shader variants that are not yet available.
    // For now, they fall through to the legacy SpriteGL implementation.
    //
    // drawWithSurface: surface overlay for axis-aligned sprites
    // drawAffineMasked: surface overlay for rotated/transformed sprites
    //
    // These are used by InterfaceComponentRenderer for drawing sprites with
    // ComponentSurface backgrounds. The visual difference is that the surface
    // texture overlay will be missing, but the sprite itself renders correctly.
    // Full support requires WP-23 (Material Effects Migration) shader variants.

    /**
     * Screen-capture update: legacy copies framebuffer pixels into this sprite's
     * texture (SpriteGL.captureScreenRegion → OpenGl2DTexture.copyTextureSubImage,
     * OpenGl2DTexture.kt:162-167, glCopyTexSubImage2D). The modern batch defers
     * draws, so the pending quads must reach the framebuffer BEFORE the copy —
     * otherwise the captured region is stale/black (this fed the black minimap).
     * The copy itself is a resource operation and remains the legacy call.
     */
    override fun captureScreenRegion(offsetX: Int, offsetY: Int, width: Int, height: Int, x: Int, y: Int) {
        modernToolkit.flushAllPendingDraws()
        super.captureScreenRegion(offsetX, offsetY, width, height, x, y)
    }

    /**
     * Real surface-mask composite (was an unmasked approximation). Corner and UV
     * math is byte-for-byte the legacy fixed-function version:
     * SpriteGL.drawWithSurface, SpriteGL.kt:271-313 — mask UVs are screen-space
     * relative to (surfaceOffsetX, surfaceOffsetY) scaled by surface.s/singleWidth and
     * surface.t/singleLength, with the mask V axis flipped (surface.t - v).
     */
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

    /**
     * Real affine surface-mask composite. Legacy reference:
     * SpriteGL.drawAffineMasked, SpriteGL.kt:451-519 — the stretch/translate corner
     * transform matches drawAffine; mask UVs per corner are sScale*(cornerX - surfaceOffsetX)
     * and surface.t - tScale*(cornerY - surfaceOffsetY); linear filtering per (flags and 1).
     */
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

        // Legacy filtering toggle (SpriteGL.kt:489)
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
        // 4th corner (legacy SpriteGL.kt:487-488)
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
                x2, y2, texture2d.s, texture2d.t, mu(x2), mv(y2)
            )
        ) {
            modernToolkit.countLegacySkip("SpriteGL.drawAffineMasked(skipped)")
        }
    }
}
