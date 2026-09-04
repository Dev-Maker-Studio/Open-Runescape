package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.OpenGl2DImageTexture
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

/**
 * Modern OpenGL 3.3 font renderer.
 *
 * Replaces the legacy FontRendererGL which uses display lists (glNewList/glEndList)
 * and immediate mode rendering (glBegin/glEnd, glVertex2i, glTexCoord2f, glColor4ub,
 * glTranslatef, glLoadIdentity) with batched textured quads rendered through the
 * existing ModernSpriteRenderer.
 *
 * The font atlas texture is created the same way as the legacy path: a 16x16 grid
 * containing all 256 character glyphs packed into a single texture. Instead of
 * caching each character in a display list, we pre-compute per-glyph UV coordinates
 * and screen-space offsets at construction time, then emit textured quads through
 * the sprite batch at draw time.
 *
 * This class uses ONLY GLWrapper (via ModernSpriteRenderer) -- zero jaggl imports.
 *
 * Font rendering flow:
 * 1. AbstractFontRenderer.renderLine() iterates characters and calls drawChar()
 * 2. drawChar() ensures the sprite batch is active via the toolkit
 * 3. Each character is drawn as a textured quad using pre-computed glyph data
 * 4. The sprite renderer batches all quads and flushes in minimal draw calls
 */
class ModernFontRenderer(
    override val toolkit: ModernGLToolkit,
    fontMetrics: FontMetrics,
    characterSprites: Array<SpriteType>,
    isPlain: Boolean
) : AbstractFontRenderer(toolkit, fontMetrics) {

    /**
     * Pre-computed glyph rendering data for each of the 256 characters.
     * Stored at init time to avoid per-character computation at draw time.
     */
    private data class GlyphData(
        /** Screen-space X offset from the draw position (minX of original sprite) */
        val offsetX: Int,
        /** Screen-space Y offset from the draw position (minY of original sprite) */
        val offsetY: Int,
        /** Glyph width in pixels */
        val width: Int,
        /** Glyph height in pixels */
        val height: Int,
        /** Left UV coordinate in the atlas */
        val u0: Float,
        /** Top UV coordinate in the atlas */
        val v0: Float,
        /** Right UV coordinate in the atlas */
        val u1: Float,
        /** Bottom UV coordinate in the atlas */
        val v1: Float
    )

    /** GL texture handle for the glyph atlas */
    private val atlasTextureHandle: Int

    /** True for alpha-mask (plain) fonts; false for pre-colored (palette) glyph fonts */
    private val plainFont: Boolean = isPlain

    /** Pre-computed data for all 256 glyphs */
    private val glyphs: Array<GlyphData>

    init {
        // Build the glyph atlas texture using the legacy texture creation path.
        // This creates an OpenGl2DImageTexture that we only need the GL handle from.
        //
        // The atlas is a 16x16 grid of character cells. Each cell is maxDimension x maxDimension
        // pixels, where maxDimension is the largest width or height across all 256 characters.

        var maxDimension = 0
        for (charIndex in 0 until 256) {
            val charSprite = characterSprites[charIndex]
            if (charSprite.height > maxDimension) maxDimension = charSprite.height
            if (charSprite.width > maxDimension) maxDimension = charSprite.width
        }

        val atlasSize = maxDimension * 16

        // Build the atlas texture.
        // The legacy code has two paths based on isPlain:
        //   - isPlain=true: single-channel alpha stored in ByteArray (RGBA format with alpha-only data)
        //   - isPlain=false: ARGB pixels stored in IntArray
        //
        // Both paths produce an OpenGl2DImageTexture. We use the legacy creation to ensure
        // the texture is identical to what FontRendererGL would create.

        val texture: OpenGl2DImageTexture
        if (isPlain) {
            // WHITE-RGB + alpha atlas (NOT the legacy alpha-only surface). Legacy draws
            // plain glyphs with combine REPLACE(rgb from GL_PREVIOUS = primary color) +
            // MODULATE(alpha) — FontRendererGL.drawChar: loadTextureCombineMode(GL_REPLACE,
            // GL_MODULATE) + applyGlyphCombinerSource (OpenGlToolkit.kt:1846-1851) — i.e.
            // glyph rgb = TEXT COLOR, alpha = atlas mask. The modern sprite shader computes
            // tex * vertexColor; an alpha-only texture samples rgb=0 and rendered every
            // glyph BLACK. With rgb=white in the atlas, tex*color = (color.rgb, mask*color.a)
            // — exact legacy output.
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
                            pixels[dstIdx++] = if (spritePixels[srcIdx++] == 0.toByte()) 0 else -1 // 0xFFFFFFFF
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

        // Pre-compute UV coordinates and offsets for each glyph.
        //
        // The legacy FontRendererGL computes these same values and stores them as
        // display list geometry. We store them as plain data instead.
        //
        // UV calculation uses texture.s/singleWidth and texture.t/singleLength to handle
        // the case where the texture is padded to power-of-two dimensions.
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

            // UV coordinates in the atlas.
            // Note: The legacy code uses (texture.t - v) for V coordinates because the
            // legacy GL has Y=0 at bottom. In the modern sprite renderer, the ortho
            // projection already flips Y so the V coordinates go from top to bottom.
            // We compute UVs to match the modern sprite renderer's convention.
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

    /**
     * Renders a single character glyph at the given screen position.
     *
     * This is the core method called by AbstractFontRenderer.renderLine() for each
     * visible character. The character is drawn as a textured quad through the
     * ModernSpriteRenderer batch.
     *
     * @param charIndex  Character index (0-255) into the glyph atlas
     * @param translateX Screen X position for the character
     * @param translateY Screen Y position for the character
     * @param colorArgb  Text color in ARGB format
     * @param isShadow   True if this is the shadow pass (drawn with offset)
     */
    override fun drawChar(charIndex: Int, translateX: Int, translateY: Int, colorArgb: Int, isShadow: Boolean) {
        if (charIndex < 0 || charIndex >= 256) return
        val glyph = glyphs[charIndex]
        if (glyph.width <= 0 || glyph.height <= 0) return

        val spriteRenderer = toolkit.getSpriteRenderer() ?: return
        if (!spriteRenderer.isReady()) return

        // Ensure the sprite batch is started
        toolkit.ensureSpriteBatchForFont()
        if (!spriteRenderer.isBatching()) {
            // Batch refused to open (offscreen render target / pipeline unavailable).
            // No legacy fallback exists — drop the glyph and count the gap.
            toolkit.countLegacySkip("ModernFontRenderer.drawChar(skipped)")
            return
        }

        // Compute screen-space quad corners.
        // The legacy code uses glTranslatef(translateX, translateY, 0) then renders
        // the quad at (minX, minY) to (minX+width, minY+height).
        // We combine the translation with the glyph offset directly.
        val x0 = (translateX + glyph.offsetX).toFloat()
        val y0 = (translateY + glyph.offsetY).toFloat()
        val x1 = x0 + glyph.width
        val y1 = y0 + glyph.height

        // Legacy combine parity (FontRendererGL.drawChar):
        // - plain fonts (isAlphaOnlyTexture) OR the shadow pass: REPLACE(rgb=primary
        //   color)/MODULATE(alpha) — with the white-rgb atlas, tex*color reproduces
        //   this exactly, so pass the text/shadow color through.
        // - non-plain fonts, non-shadow: GL_REPLACE/GL_REPLACE — the pre-colored glyph
        //   is drawn as-is and the primary color is IGNORED → force opaque white.
        //   (Shadow pass on a colored atlas is approximated as tex*shadowColor.)
        // Legacy takes glyph alpha from the ATLAS ONLY — the primary color's alpha
        // byte is ignored by the FFP combiner (callers pass colors with alpha 0x00 or
        // 0x01, e.g. MiniMenu's mangled `or -0xffffff.inv()`, and legacy still drew
        // them opaque). Force alpha 255 so tex.a alone controls coverage.
        val effectiveColor = if (plainFont || isShadow) (colorArgb or 0xFF000000.toInt()) else -1

        // Draw the glyph as a textured quad.
        // UV layout: the quad vertices are (top-left, bottom-left, bottom-right, top-right)
        // matching the sprite renderer's drawSprite convention.
        // v0 is the top of the glyph in texture space, v1 is the bottom.
        spriteRenderer.drawSprite(
            textureId = atlasTextureHandle,
            x0 = x0, y0 = y0,
            x1 = x1, y1 = y1,
            u0 = glyph.u0, v0 = glyph.v0,
            u1 = glyph.u1, v1 = glyph.v1,
            colorArgb = effectiveColor,
            blendMode = 1  // Alpha blend for text
        )
    }

    /**
     * Renders a character with a component surface texture overlay.
     *
     * The legacy implementation uses multi-texture with GL_TEXTURE_GEN for surface
     * mapping. For the modern path, we render just the character glyph without the
     * surface overlay. Full surface-mapped text support will be added in WP-12
     * (UI Component Integration) when dedicated shader variants are available.
     *
     * @param charIndex     Character index (0-255)
     * @param translateX    Screen X position
     * @param translateY    Screen Y position
     * @param colorArgb     Text color in ARGB format
     * @param isShadow      True if shadow pass
     * @param surface       The component surface texture (currently unused in modern path)
     * @param surfaceOffsetX Surface texture X offset
     * @param surfaceOffsetY Surface texture Y offset
     */
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
        // Render as a standard glyph without surface overlay.
        // Multi-texture surface mapping requires a dedicated shader variant (WP-12).
        drawChar(charIndex, translateX, translateY, colorArgb, isShadow)
    }
}
