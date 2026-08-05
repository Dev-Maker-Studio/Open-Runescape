package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import org.joml.Matrix4f
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Modern OpenGL 3.3 batched sprite renderer with multi-texture batching.
 *
 * Replaces the legacy fixed-function glBegin/glEnd calls in SpriteGL with
 * batched textured quads via VBO. Uses ONLY GLWrapper (zero jaggl imports).
 *
 * Multi-texture batching strategy:
 * Up to 8 textures are bound simultaneously to texture units 0-7.
 * Each vertex carries a texture slot index that the fragment shader uses
 * to select which sampler to read from. This reduces draw calls by up to 8x
 * compared to flushing on every texture change.
 *
 * A flush only occurs when:
 * - All 8 texture slots are occupied AND a new texture is needed
 * - The blend mode changes between quads
 * - The vertex buffer is full (MAX_QUADS reached)
 * - end() or flush() is called explicitly (e.g., before scissor changes)
 *
 * Vertex format (24 bytes per vertex):
 * - Position:  vec2 (8 bytes) at offset 0    -- screen-space coordinates
 * - TexCoord:  vec2 (8 bytes) at offset 8    -- texture UVs
 * - Color:     vec4 ubyte (4 bytes) at offset 16 -- RGBA vertex color
 * - TexIndex:  float (4 bytes) at offset 20  -- texture slot index (-1 = untextured)
 *
 * Usage pattern:
 * ```
 * renderer.begin(screenWidth, screenHeight)
 * renderer.drawSprite(textureId, x, y, width, height, u0, v0, u1, v1, colorArgb, blendMode)
 * renderer.drawSprite(...)  // more sprites - different textures batch together
 * renderer.end()            // flushes all batched quads in minimal draw calls
 * ```
 */
class ModernSpriteRenderer {

    companion object {
        /** Gates the per-flush glGet* diagnostics (driver sync points). */
        @JvmField var captureFlushState = false

        /** Bytes per vertex: 2 floats pos + 2 floats UV + 4 bytes color + 1 float texIndex = 24 bytes */
        const val BYTES_PER_VERTEX = 24

        /** Vertices per quad (4 corners) */
        private const val VERTICES_PER_QUAD = 4

        /** Indices per quad (2 triangles = 6 indices) */
        private const val INDICES_PER_QUAD = 6

        /** Maximum quads per batch before flush */
        private const val MAX_QUADS = 4096

        /** Maximum vertices in the batch */
        private const val MAX_VERTICES = MAX_QUADS * VERTICES_PER_QUAD

        /** Maximum indices in the batch */
        private const val MAX_INDICES = MAX_QUADS * INDICES_PER_QUAD

        /** Maximum number of textures bound simultaneously per batch.
         *
         *  Capped at 4 deliberately: on this client's GL setup, sampling a sprite
         *  quad whose texture landed in slot 5+ of a batch returned nothing (the
         *  minimenu entry text was invisible — box sprites filled slots 0-4 and the
         *  font atlas got slot 5). Slots 0-4 were confirmed working; capping at 4
         *  keeps every texture in a known-good low slot. When a 5th distinct texture
         *  is needed the batch auto-flushes (resolveTextureSlot) and the new texture
         *  starts again at slot 0 — the same flush-to-a-low-slot mechanism that fixed
         *  the menu, applied globally. Cost is a few extra UI draw calls per frame. */
        private const val MAX_TEXTURE_SLOTS = 4

        /** {0,1,...,MAX_TEXTURE_SLOTS-1} — sampler-unit indices written to uTextures[]. */
        private val SAMPLER_UNIT_INDICES = IntArray(MAX_TEXTURE_SLOTS) { it }

        /** Vertex attribute location for texture index (matches shader layout) */
        private const val ATTRIB_TEX_INDEX = 4
    }

    // GPU resources
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    // CPU staging buffer
    private var vertexData: ByteBuffer? = null

    // Cached uniform locations
    private var uProjectionLocation = -1
    private var uTextureLocations = IntArray(MAX_TEXTURE_SLOTS) { -1 }
    /** Base location of the sampler ARRAY (uTextures[0]) — used to set every element
     *  in one glUniform1iv call, robust to drivers that return -1 for element
     *  locations like uTextures[5]. */
    private var uTexturesBaseLocation = -1

    // Orthographic projection matrix (reused)
    private val projectionMatrix = Matrix4f()

    // Multi-texture slot management
    private val textureSlots = IntArray(MAX_TEXTURE_SLOTS) // GL texture handles per slot
    private var textureSlotCount = 0  // Number of slots currently occupied

    // Batch state
    private var currentBlendMode = -1
    private var quadCount = 0
    private var isBatching = false
    private var batchScreenWidth = 0
    private var batchScreenHeight = 0

    // Initialization state
    private var isInitialized = false

    // Statistics
    private var drawCallsThisFrame = 0
    private var quadsThisFrame = 0

    // Presentation diagnostics: GL state captured at the moment of the last flush.
    // Answers "which framebuffer/viewport/scissor did the quads actually go to".
    @JvmField var lastFlushFbo = -1
    @JvmField var lastFlushViewport = IntArray(4)
    @JvmField var lastFlushScissorEnabled = false
    @JvmField var lastFlushScissorBox = IntArray(4)

    /**
     * Initializes the renderer with shader and GPU resources.
     * Must be called once before any rendering (on GL thread).
     */
    fun initialize() {
        if (isInitialized) return

        // Create shader from resources
        shader = ShaderProgram.createFromResources("/shaders/sprite.vert", "/shaders/sprite.frag")

        // Create GPU resources
        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STATIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        // Setup VAO with multi-texture sprite format (24 bytes per vertex)
        vao!!.bind()
        vbo!!.bind()

        // Position: vec2 at location 0, offset 0
        vao!!.setAttribute(0, 2, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 0)
        // TexCoord: vec2 at location 2, offset 8
        vao!!.setAttribute(2, 2, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 8)
        // Color: ubyte4 at location 3, offset 16
        vao!!.setAttribute(3, 4, GLWrapper.Constants.UNSIGNED_BYTE, true, BYTES_PER_VERTEX, 16)
        // TexIndex: float at location 4, offset 20
        vao!!.setAttribute(ATTRIB_TEX_INDEX, 1, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 20)

        ibo!!.bind()
        vao!!.unbind()

        // Allocate CPU staging buffer
        vertexData = MemoryUtil.memAlloc(MAX_VERTICES * BYTES_PER_VERTEX)

        // Pre-build the static index buffer.
        // Every quad uses the same index pattern: (0,1,2, 0,2,3) offset by quad index * 4.
        val staticIndices = ShortArray(MAX_INDICES)
        for (q in 0 until MAX_QUADS) {
            val base = q * 4
            val idx = q * 6
            staticIndices[idx + 0] = (base + 0).toShort()
            staticIndices[idx + 1] = (base + 1).toShort()
            staticIndices[idx + 2] = (base + 2).toShort()
            staticIndices[idx + 3] = (base + 0).toShort()
            staticIndices[idx + 4] = (base + 2).toShort()
            staticIndices[idx + 5] = (base + 3).toShort()
        }
        ibo!!.upload(staticIndices)

        // Cache uniform locations
        cacheUniformLocations()

        isInitialized = true
    }

    private fun cacheUniformLocations() {
        val s = shader ?: return
        s.bind()
        uProjectionLocation = s.getUniformLocation("uProjection")
        for (i in 0 until MAX_TEXTURE_SLOTS) {
            uTextureLocations[i] = s.getUniformLocation("uTextures[$i]")
        }
        // Base location for the whole sampler array — set every element with one
        // glUniform1iv from uTextures[0] (robust to drivers that report element
        // locations like uTextures[3] as -1).
        uTexturesBaseLocation = uTextureLocations[0].takeIf { it >= 0 }
            ?: s.getUniformLocation("uTextures")
        s.unbind()
    }

    /**
     * Begins a sprite rendering batch.
     *
     * Sets up orthographic projection for screen-space rendering.
     * The coordinate system matches the legacy renderer:
     * - Origin at top-left
     * - X increases rightward
     * - Y increases downward
     *
     * @param screenWidth  Display width in pixels
     * @param screenHeight Display height in pixels
     */
    fun begin(screenWidth: Int, screenHeight: Int) {
        check(isInitialized) { "ModernSpriteRenderer not initialized. Call initialize() first." }
        check(!isBatching) { "begin() called while already batching. Call end() first." }

        isBatching = true
        quadCount = 0
        textureSlotCount = 0
        // NOTE: drawCallsThisFrame/quadsThisFrame are NOT reset here — begin() runs
        // many times per presented frame (every batch reopen), which made the stats
        // report only the LAST batch. They are reset once per presented frame via
        // resetFrameStats() from ModernGLToolkit.render().

        // Build orthographic projection matrix.
        // Maps (0,0)-(width,height) to (-1,-1)-(1,1) clip space.
        // Y is flipped so that y=0 is top of screen (matching screen coords).
        // This matches the legacy orthoMatrix + invertMatrix(negate Y row) behavior.
        projectionMatrix.identity().ortho(
            0f, screenWidth.toFloat(),   // left, right
            screenHeight.toFloat(), 0f,  // bottom, top (flipped: top=0, bottom=height)
            -1f, 1f                      // near, far
        )

        // Bind shader and set per-frame uniforms
        shader!!.bind()
        shader!!.setUniformMatrix4(uProjectionLocation, projectionMatrix)

        // Bind texture samplers to their respective texture units. Set the WHOLE
        // array from the base location (glUniform1iv) so element locations that the
        // driver reports as -1 (e.g. uTextures[5]) still get bound — the per-element
        // path silently skipped those, which is why slot-5+ text sampled unit 0 and
        // was invisible.
        if (uTexturesBaseLocation >= 0) {
            GLWrapper.uniform1iv(uTexturesBaseLocation, SAMPLER_UNIT_INDICES)
        } else {
            for (i in 0 until MAX_TEXTURE_SLOTS) {
                if (uTextureLocations[i] >= 0) shader!!.setUniform(uTextureLocations[i], i)
            }
        }

        // Remember the target dimensions: flush() re-asserts the full-window viewport,
        // because legacy 3D setup (setupMatrixRendering → setModelViewMatrixMode) can
        // shrink the viewport to the drawable sub-area while quads are still queued.
        batchScreenWidth = screenWidth
        batchScreenHeight = screenHeight

        // Set up GL state for 2D sprite rendering
        GLWrapper.disable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthMask(false)
        GLWrapper.enable(GLWrapper.Constants.BLEND)
        GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
        GLWrapper.disable(GLWrapper.Constants.CULL_FACE)

        // Reset batch state
        currentBlendMode = -1

        // Clear vertex staging buffer
        vertexData!!.clear()
    }

    /**
     * Draws a textured sprite quad.
     *
     * @param textureId  OpenGL texture handle (0 or negative for untextured)
     * @param x0         Left X position (screen pixels)
     * @param y0         Top Y position (screen pixels)
     * @param x1         Right X position (screen pixels)
     * @param y1         Bottom Y position (screen pixels)
     * @param u0         Left texture U coordinate
     * @param v0         Top texture V coordinate
     * @param u1         Right texture U coordinate
     * @param v1         Bottom texture V coordinate
     * @param colorArgb  Color in ARGB format (legacy RuneScape byte order)
     * @param blendMode  Blend mode (1=alpha, 2=additive, 128=multiply)
     */
    fun drawSprite(
        textureId: Int,
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        u0: Float, v0: Float,
        u1: Float, v1: Float,
        colorArgb: Int,
        blendMode: Int
    ) {
        if (!isBatching) return

        // Flush on blend mode change (blend mode affects all quads in the batch)
        if (blendMode != currentBlendMode) {
            flush()
            applyBlendMode(blendMode)
        }

        // Resolve the texture slot index for this quad
        val texSlotIndex = resolveTextureSlot(textureId)

        // Check if batch is full
        if (quadCount >= MAX_QUADS) {
            flush()
        }

        // Extract RGBA components from ARGB int (matching legacy glColor4ub byte order),
        // with the alpha byte corrected for the legacy blend-mode semantics.
        val effColor = effectiveColor(colorArgb, blendMode)
        val r = ((effColor ushr 16) and 0xFF).toByte()
        val g = ((effColor ushr 8) and 0xFF).toByte()
        val b = (effColor and 0xFF).toByte()
        val a = ((effColor ushr 24) and 0xFF).toByte()

        val texIdxFloat = texSlotIndex.toFloat()
        val vb = vertexData!!

        // Vertex 0: top-left
        vb.putFloat(x0).putFloat(y0).putFloat(u0).putFloat(v0)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)

        // Vertex 1: bottom-left
        vb.putFloat(x0).putFloat(y1).putFloat(u0).putFloat(v1)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)

        // Vertex 2: bottom-right
        vb.putFloat(x1).putFloat(y1).putFloat(u1).putFloat(v1)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)

        // Vertex 3: top-right
        vb.putFloat(x1).putFloat(y0).putFloat(u1).putFloat(v0)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)

        quadCount++
    }

    /**
     * Draws a textured quad with arbitrary vertex positions (for rotated sprites).
     *
     * @param textureId  OpenGL texture handle
     * @param x0,y0      Vertex 0 position (typically top-left before rotation)
     * @param x1,y1      Vertex 1 position (typically bottom-left before rotation)
     * @param x2,y2      Vertex 2 position (typically bottom-right before rotation)
     * @param x3,y3      Vertex 3 position (typically top-right before rotation)
     * @param u0,v0      Vertex 0 texture coordinates
     * @param u1,v1      Vertex 1 texture coordinates
     * @param u2,v2      Vertex 2 texture coordinates
     * @param u3,v3      Vertex 3 texture coordinates
     * @param colorArgb  Color in ARGB format
     * @param blendMode  Blend mode
     */
    fun drawQuad(
        textureId: Int,
        x0: Float, y0: Float, u0: Float, v0: Float,
        x1: Float, y1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, u3: Float, v3: Float,
        colorArgb: Int,
        blendMode: Int
    ) {
        if (!isBatching) return

        // Flush on blend mode change
        if (blendMode != currentBlendMode) {
            flush()
            applyBlendMode(blendMode)
        }

        // Resolve the texture slot index for this quad
        val texSlotIndex = resolveTextureSlot(textureId)

        // Check if batch is full
        if (quadCount >= MAX_QUADS) {
            flush()
        }

        // Extract RGBA components (alpha corrected for legacy blend-mode semantics)
        val effColor = effectiveColor(colorArgb, blendMode)
        val r = ((effColor ushr 16) and 0xFF).toByte()
        val g = ((effColor ushr 8) and 0xFF).toByte()
        val b = (effColor and 0xFF).toByte()
        val a = ((effColor ushr 24) and 0xFF).toByte()

        val texIdxFloat = texSlotIndex.toFloat()
        val vb = vertexData!!

        // Vertex 0
        vb.putFloat(x0).putFloat(y0).putFloat(u0).putFloat(v0)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)

        // Vertex 1
        vb.putFloat(x1).putFloat(y1).putFloat(u1).putFloat(v1)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)

        // Vertex 2
        vb.putFloat(x2).putFloat(y2).putFloat(u2).putFloat(v2)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)

        // Vertex 3
        vb.putFloat(x3).putFloat(y3).putFloat(u3).putFloat(v3)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)

        quadCount++
    }

    /**
     * Draws an untextured filled rectangle.
     *
     * @param x      Left X position (screen pixels)
     * @param y      Top Y position (screen pixels)
     * @param width  Rectangle width
     * @param height Rectangle height
     * @param colorArgb Color in ARGB format
     * @param blendMode Blend mode
     */
    fun drawFilledRect(
        x: Int, y: Int,
        width: Int, height: Int,
        colorArgb: Int,
        blendMode: Int
    ) {
        // Match legacy 0.35 pixel offset for sub-pixel alignment
        val x0 = x + 0.35f
        val y0 = y + 0.35f
        val x1 = x0 + width
        val y1 = y0 + height

        drawSprite(
            textureId = 0,
            x0 = x0, y0 = y0,
            x1 = x1, y1 = y1,
            u0 = 0f, v0 = 0f,
            u1 = 0f, v1 = 0f,
            colorArgb = colorArgb,
            blendMode = blendMode
        )
    }

    /**
     * Draws an untextured rectangle outline.
     *
     * @param x      Left X position
     * @param y      Top Y position
     * @param width  Rectangle width
     * @param height Rectangle height
     * @param colorArgb Color in ARGB format
     * @param blendMode Blend mode
     */
    fun drawRectOutline(
        x: Int, y: Int,
        width: Int, height: Int,
        colorArgb: Int,
        blendMode: Int
    ) {
        // Match legacy 0.35 pixel offset
        val x0 = x + 0.35f
        val y0 = y + 0.35f
        val x1 = x0 + width - 1f
        val y1 = y0 + height - 1f

        // Top edge
        drawSprite(0, x0, y0, x1, y0 + 1f, 0f, 0f, 0f, 0f, colorArgb, blendMode)
        // Bottom edge
        drawSprite(0, x0, y1, x1, y1 + 1f, 0f, 0f, 0f, 0f, colorArgb, blendMode)
        // Left edge
        drawSprite(0, x0, y0, x0 + 1f, y1, 0f, 0f, 0f, 0f, colorArgb, blendMode)
        // Right edge
        drawSprite(0, x1 - 1f, y0, x1, y1, 0f, 0f, 0f, 0f, colorArgb, blendMode)
    }

    /**
     * Draws an untextured horizontal line as a 1-pixel-tall quad.
     * Matches the legacy OpenGL glBegin(GL_LINES) horizontal line behavior.
     *
     * @param x      Left X position (screen pixels)
     * @param y      Top Y position (screen pixels)
     * @param width  Line width in pixels
     * @param colorArgb Color in ARGB format
     * @param blendMode Blend mode
     */
    fun drawHorizontalLine(
        x: Int, y: Int,
        width: Int,
        colorArgb: Int,
        blendMode: Int
    ) {
        val x0 = x + 0.35f
        val y0 = y + 0.35f
        val x1 = x0 + width
        val y1 = y0 + 1f

        drawSprite(
            textureId = 0,
            x0 = x0, y0 = y0,
            x1 = x1, y1 = y1,
            u0 = 0f, v0 = 0f,
            u1 = 0f, v1 = 0f,
            colorArgb = colorArgb,
            blendMode = blendMode
        )
    }

    /**
     * Draws an untextured vertical line as a 1-pixel-wide quad.
     * Matches the legacy OpenGL glBegin(GL_LINES) vertical line behavior.
     *
     * @param x      X position (screen pixels)
     * @param y      Top Y position (screen pixels)
     * @param height Line height in pixels
     * @param colorArgb Color in ARGB format
     * @param blendMode Blend mode
     */
    fun drawVerticalLine(
        x: Int, y: Int,
        height: Int,
        colorArgb: Int,
        blendMode: Int
    ) {
        val x0 = x + 0.35f
        val y0 = y + 0.35f
        val x1 = x0 + 1f
        val y1 = y0 + height

        drawSprite(
            textureId = 0,
            x0 = x0, y0 = y0,
            x1 = x1, y1 = y1,
            u0 = 0f, v0 = 0f,
            u1 = 0f, v1 = 0f,
            colorArgb = colorArgb,
            blendMode = blendMode
        )
    }

    /**
     * Draws an untextured arbitrary line as a 1-pixel-wide quad.
     * The line is drawn from (startX, startY) to (endX, endY) with a unit
     * extension in the direction of the line, matching legacy GL_LINES behavior.
     *
     * The line is rendered as a thin quad with perpendicular offset of 0.5 pixels
     * on each side of the line's direction vector.
     *
     * @param startX Start X position (screen pixels)
     * @param startY Start Y position (screen pixels)
     * @param endX   End X position (screen pixels)
     * @param endY   End Y position (screen pixels)
     * @param colorArgb Color in ARGB format
     * @param blendMode Blend mode
     */
    fun drawLine(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        colorArgb: Int,
        blendMode: Int
    ) {
        if (!isBatching) return

        var deltaX = (endX - startX).toFloat()
        var deltaY = (endY - startY).toFloat()
        if (deltaX == 0f && deltaY == 0f) {
            deltaX = 1f
        } else {
            val invLen = (1.0 / kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
            deltaX *= invLen
            deltaY *= invLen
        }

        // Line endpoints with 0.35 pixel offset
        val sx = startX + 0.35f
        val sy = startY + 0.35f
        val ex = endX + deltaX + 0.35f
        val ey = endY + deltaY + 0.35f

        // Perpendicular vector (0.5 pixels wide on each side = 1 pixel total)
        val perpX = -deltaY * 0.5f
        val perpY = deltaX * 0.5f

        // Build quad around the line
        val x0 = sx + perpX
        val y0 = sy + perpY
        val x1 = sx - perpX
        val y1 = sy - perpY
        val x2 = ex - perpX
        val y2 = ey - perpY
        val x3 = ex + perpX
        val y3 = ey + perpY

        drawQuad(
            textureId = 0,
            x0 = x0, y0 = y0, u0 = 0f, v0 = 0f,
            x1 = x1, y1 = y1, u1 = 0f, v1 = 0f,
            x2 = x2, y2 = y2, u2 = 0f, v2 = 0f,
            x3 = x3, y3 = y3, u3 = 0f, v3 = 0f,
            colorArgb = colorArgb,
            blendMode = blendMode
        )
    }

    /**
     * Draws a textured line segment as a 1-pixel-wide quad.
     * Used for textured minimap lines and similar effects.
     *
     * @param startX     Start X position (screen pixels)
     * @param startY     Start Y position (screen pixels)
     * @param endX       End X position (screen pixels)
     * @param endY       End Y position (screen pixels)
     * @param textureId  GL texture handle
     * @param u0         Start U texture coordinate
     * @param v0         Start V texture coordinate
     * @param u1         End U texture coordinate
     * @param v1         End V texture coordinate
     * @param colorArgb  Color in ARGB format
     * @param blendMode  Blend mode
     */
    fun drawTexturedLine(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        textureId: Int,
        u0: Float, v0: Float,
        u1: Float, v1: Float,
        colorArgb: Int,
        blendMode: Int
    ) {
        if (!isBatching) return

        var deltaX = endX - startX
        var deltaY = endY - startY
        if (deltaX == 0f && deltaY == 0f) {
            deltaX = 1f
        } else {
            val invLen = (1.0 / kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
            deltaX *= invLen
            deltaY *= invLen
        }

        // Perpendicular vector (0.5 pixels wide on each side)
        val perpX = -deltaY * 0.5f
        val perpY = deltaX * 0.5f

        // Build quad around the line
        val x0 = startX + perpX
        val y0 = startY + perpY
        val x1 = startX - perpX
        val y1 = startY - perpY
        val x2 = endX - perpX
        val y2 = endY - perpY
        val x3 = endX + perpX
        val y3 = endY + perpY

        drawQuad(
            textureId = textureId,
            x0 = x0, y0 = y0, u0 = u0, v0 = v0,
            x1 = x1, y1 = y1, u1 = u0, v1 = v0,
            x2 = x2, y2 = y2, u2 = u1, v2 = v1,
            x3 = x3, y3 = y3, u3 = u1, v3 = v1,
            colorArgb = colorArgb,
            blendMode = blendMode
        )
    }

    /**
     * Ends the sprite batch. Flushes any remaining queued quads
     * and restores GL state.
     */
    fun end() {
        if (!isBatching) return

        flush()
        isBatching = false

        // Unbind shader
        shader?.unbind()

        // Unbind VAO
        vao?.unbind()
    }

    /**
     * Maps a legacy (colorArgb, blendMode) pair to the effective vertex color.
     *
     * Legacy blend semantics (OpenGlToolkit.setBlendMode, OpenGlToolkit.kt:2013-2065):
     *   mode 1   = alpha blend + GL_ALPHA_TEST on
     *   mode 2   = additive (ONE,ONE) + alpha test OFF
     *   mode 128 = multiply (DST_COLOR,ONE) + alpha test on
     *   other(0) = GL_BLEND DISABLED + alpha test OFF → draws OPAQUE regardless of
     *              the color's alpha byte. Interface TYPE_RECTANGLE opaque fills pass
     *              component.color = 0xRRGGBB with alpha byte 0 under mode 0
     *              (InterfaceComponentRenderer.kt:275) — legacy still draws them.
     *
     * The sprite shader discards fragments with alpha < 0.004 (mirroring the legacy
     * alpha test), which silently killed every alpha-0 quad in the non-alpha-tested
     * modes (0 and 2). For those modes the alpha byte is irrelevant to legacy output
     * (blend off, or ONE,ONE ignores src alpha), so force it opaque to bypass the
     * discard; alpha-tested modes (1, 128) keep the real alpha.
     */
    private fun effectiveColor(colorArgb: Int, blendMode: Int): Int {
        return if (blendMode == 1 || blendMode == 128) colorArgb
        else colorArgb or 0xFF000000.toInt()
    }

    /**
     * Resolves the texture slot index for a given GL texture handle.
     *
     * If the texture is already in a slot, returns its index.
     * If there is room for a new texture, assigns it to the next slot and returns the index.
     * If all slots are full, flushes the batch to free all slots, then assigns slot 0.
     *
     * For untextured quads (textureId <= 0), returns -1.
     *
     * @param textureId The GL texture handle
     * @return The texture slot index (0-7) or -1 for untextured
     */
    private fun resolveTextureSlot(textureId: Int): Int {
        if (textureId <= 0) return -1

        // Check if this texture is already in a slot
        for (i in 0 until textureSlotCount) {
            if (textureSlots[i] == textureId) return i
        }

        // Need a new slot. If all slots are full, flush to free them.
        if (textureSlotCount >= MAX_TEXTURE_SLOTS) {
            flush()
        }

        // Assign the texture to the next available slot
        val slotIndex = textureSlotCount
        textureSlots[slotIndex] = textureId
        textureSlotCount++

        // Bind the texture to its texture unit
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + slotIndex)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureId)

        return slotIndex
    }

    /**
     * Flushes the current batch of quads to the GPU.
     * Public so that ModernGLToolkit can flush before scissor/clear state changes
     * that would affect the meaning of already-queued draw commands.
     */
    fun flush() {
        if (quadCount == 0) {
            // Even if no quads, reset texture slots so new textures can be assigned
            textureSlotCount = 0
            return
        }

        val vb = vertexData!!
        vb.flip()

        // The ortho projection maps (0,0)-(w,h) assuming a FULL-WINDOW viewport.
        // Re-assert it at flush time: legacy code between begin() and flush() may
        // have set the 3D drawable sub-area viewport (the caller restores the
        // legacy-consistent viewport after end() via updateViewportAndCenter()).
        // Legacy parity: 2D drawing runs with the full-window viewport
        // (OpenGlToolkit.updateViewportAndCenter, depthClippingMode != 2 branch).
        if (batchScreenWidth > 0 && batchScreenHeight > 0) {
            GLWrapper.viewport(0, 0, batchScreenWidth, batchScreenHeight)
        }

        // [ModernDiag2] render-target capture — glGet* calls are driver sync points
        // and this runs PER FLUSH (dozens/frame). Off by default; flip on only while
        // actively debugging a render-target question.
        if (captureFlushState) {
            lastFlushFbo = GL33.glGetInteger(GL33.GL_FRAMEBUFFER_BINDING)
            GL33.glGetIntegerv(GL33.GL_VIEWPORT, lastFlushViewport)
            lastFlushScissorEnabled = GL33.glIsEnabled(GL33.GL_SCISSOR_TEST)
            GL33.glGetIntegerv(GL33.GL_SCISSOR_BOX, lastFlushScissorBox)
        }

        // Upload vertex data
        vbo!!.upload(vb)

        // Re-assert the sampler→unit mapping and texture→unit bindings for each active
        // slot right before the draw. Cheap safety net (<=MAX_TEXTURE_SLOTS binds) that
        // guards against a sampler-array element that never bound to its unit and
        // against texture-unit bindings clobbered by intervening GL between a slot's
        // first resolveTextureSlot() and this flush.
        shader!!.bind()
        if (uTexturesBaseLocation >= 0) {
            GLWrapper.uniform1iv(uTexturesBaseLocation, SAMPLER_UNIT_INDICES)
        }
        for (i in 0 until textureSlotCount) {
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + i)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureSlots[i])
        }

        // Draw the batch.
        // Bind VAO first, then explicitly re-bind IBO. The VAO should restore
        // the IBO binding from initialize(), but legacy jaggl GL calls between
        // initialize() and flush() can corrupt the default VAO's element buffer
        // binding, and on some drivers the VAO restore doesn't stick after that.
        vao!!.bind()
        ibo!!.bind()
        val indexCount = quadCount * INDICES_PER_QUAD
        GLWrapper.drawElements(
            GLWrapper.Constants.TRIANGLES,
            indexCount,
            GLWrapper.Constants.UNSIGNED_SHORT,
            0
        )

        drawCallsThisFrame++
        quadsThisFrame += quadCount

        // Reset for next batch
        quadCount = 0
        textureSlotCount = 0
        vb.clear()
    }

    /**
     * Applies the blend mode, mapping from legacy RuneScape blend mode IDs
     * to modern GL blend functions.
     */
    private fun applyBlendMode(blendMode: Int) {
        if (blendMode == currentBlendMode) return
        currentBlendMode = blendMode

        when (blendMode) {
            1 -> {
                // Alpha blend (most common)
                GLWrapper.enable(GLWrapper.Constants.BLEND)
                GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
            }
            2 -> {
                // Additive blend
                GLWrapper.enable(GLWrapper.Constants.BLEND)
                GLWrapper.blendFunc(GLWrapper.Constants.ONE, GLWrapper.Constants.ONE)
            }
            128 -> {
                // Multiply blend
                GLWrapper.enable(GLWrapper.Constants.BLEND)
                GLWrapper.blendFunc(GLWrapper.Constants.DST_COLOR, GLWrapper.Constants.ONE)
            }
            else -> {
                // No blend
                GLWrapper.disable(GLWrapper.Constants.BLEND)
            }
        }
    }

    /**
     * Gets frame statistics.
     */
    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, quadsThisFrame, textureSlotCount)
    }

    /**
     * Resets the per-presented-frame stats. Called once per presented frame
     * (ModernGLToolkit.render), after the stats have been read.
     */
    fun resetFrameStats() {
        drawCallsThisFrame = 0
        quadsThisFrame = 0
    }

    /**
     * Returns true if the renderer is currently between begin() and end().
     */
    fun isBatching(): Boolean = isBatching

    /**
     * Returns true if the renderer has been initialized.
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Releases all GPU and CPU resources.
     */
    fun destroy() {
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()

        vertexData?.let { MemoryUtil.memFree(it) }

        shader = null
        vao = null
        vbo = null
        ibo = null
        vertexData = null

        isInitialized = false
    }

    data class FrameStats(
        val drawCalls: Int,
        val quads: Int,
        val textureSlots: Int
    )
}
