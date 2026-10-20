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

class ModernSpriteRenderer {

    companion object {
        @JvmField var captureFlushState = false

        const val BYTES_PER_VERTEX = 40

        private const val VERTICES_PER_QUAD = 4

        private const val INDICES_PER_QUAD = 6

        private const val ATTRIB_CLIP = 5

        private const val NO_CLIP = 1.0e8f

        private const val MAX_QUADS = 4096

        private const val MAX_VERTICES = MAX_QUADS * VERTICES_PER_QUAD

        private const val MAX_INDICES = MAX_QUADS * INDICES_PER_QUAD

        private const val MAX_TEXTURE_SLOTS = 8

        private val SAMPLER_UNIT_INDICES = IntArray(MAX_TEXTURE_SLOTS) { it }

        private const val ATTRIB_TEX_INDEX = 4
    }

    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    private var vertexData: ByteBuffer? = null

    private var uProjectionLocation = -1
    private var uTextureLocations = IntArray(MAX_TEXTURE_SLOTS) { -1 }
    private var uTexturesBaseLocation = -1

    private val projectionMatrix = Matrix4f()

    private val textureSlots = IntArray(MAX_TEXTURE_SLOTS)
    private var textureSlotCount = 0

    private var currentBlendMode = -1
    private var quadCount = 0
    private var isBatching = false
    private var batchScreenWidth = 0
    private var batchScreenHeight = 0

    private var isInitialized = false

    private var drawCallsThisFrame = 0
    private var quadsThisFrame = 0

    @JvmField var lastFlushFbo = -1
    @JvmField var lastFlushViewport = IntArray(4)
    @JvmField var lastFlushScissorEnabled = false
    @JvmField var lastFlushScissorBox = IntArray(4)

    fun initialize() {
        if (isInitialized) return

        shader = ShaderProgram.createFromResources("/shaders/sprite.vert", "/shaders/sprite.frag")

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STATIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        vao!!.bind()
        vbo!!.bind()

        vao!!.setAttribute(0, 2, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 0)
        vao!!.setAttribute(2, 2, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 8)
        vao!!.setAttribute(3, 4, GLWrapper.Constants.UNSIGNED_BYTE, true, BYTES_PER_VERTEX, 16)
        vao!!.setAttribute(ATTRIB_TEX_INDEX, 1, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 20)
        vao!!.setAttribute(ATTRIB_CLIP, 4, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 24)

        ibo!!.bind()
        vao!!.unbind()

        vertexData = MemoryUtil.memAlloc(MAX_VERTICES * BYTES_PER_VERTEX)

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
        uTexturesBaseLocation = uTextureLocations[0].takeIf { it >= 0 }
            ?: s.getUniformLocation("uTextures")
        s.unbind()
    }

    fun begin(screenWidth: Int, screenHeight: Int) {
        check(isInitialized) { "ModernSpriteRenderer not initialized. Call initialize() first." }
        check(!isBatching) { "begin() called while already batching. Call end() first." }

        isBatching = true
        quadCount = 0
        textureSlotCount = 0

        projectionMatrix.identity().ortho(
            0f, screenWidth.toFloat(),
            screenHeight.toFloat(), 0f,
            -1f, 1f
        )

        shader!!.bind()
        shader!!.setUniformMatrix4(uProjectionLocation, projectionMatrix)

        if (uTexturesBaseLocation >= 0) {
            GLWrapper.uniform1iv(uTexturesBaseLocation, SAMPLER_UNIT_INDICES)
        } else {
            for (i in 0 until MAX_TEXTURE_SLOTS) {
                if (uTextureLocations[i] >= 0) shader!!.setUniform(uTextureLocations[i], i)
            }
        }

        batchScreenWidth = screenWidth
        batchScreenHeight = screenHeight

        GLWrapper.disable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthMask(false)
        GLWrapper.enable(GLWrapper.Constants.BLEND)
        GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
        GLWrapper.disable(GLWrapper.Constants.CULL_FACE)

        currentBlendMode = -1

        vertexData!!.clear()
    }

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

        if (blendMode != currentBlendMode) {
            flush("blend")
            applyBlendMode(blendMode)
        }

        if (quadCount >= MAX_QUADS) {
            flush("quadsFull")
        }

        val texSlotIndex = resolveTextureSlot(textureId)

        val effColor = effectiveColor(colorArgb, blendMode)
        val r = ((effColor ushr 16) and 0xFF).toByte()
        val g = ((effColor ushr 8) and 0xFF).toByte()
        val b = (effColor and 0xFF).toByte()
        val a = ((effColor ushr 24) and 0xFF).toByte()

        val texIdxFloat = texSlotIndex.toFloat()
        val vb = vertexData!!
        val cx0 = clipX0; val cy0 = clipY0; val cx1 = clipX1; val cy1 = clipY1

        vb.putFloat(x0).putFloat(y0).putFloat(u0).putFloat(v0)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)
        vb.putFloat(cx0).putFloat(cy0).putFloat(cx1).putFloat(cy1)

        vb.putFloat(x0).putFloat(y1).putFloat(u0).putFloat(v1)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)
        vb.putFloat(cx0).putFloat(cy0).putFloat(cx1).putFloat(cy1)

        vb.putFloat(x1).putFloat(y1).putFloat(u1).putFloat(v1)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)
        vb.putFloat(cx0).putFloat(cy0).putFloat(cx1).putFloat(cy1)

        vb.putFloat(x1).putFloat(y0).putFloat(u1).putFloat(v0)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)
        vb.putFloat(cx0).putFloat(cy0).putFloat(cx1).putFloat(cy1)

        quadCount++
    }

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

        if (blendMode != currentBlendMode) {
            flush("blend")
            applyBlendMode(blendMode)
        }

        if (quadCount >= MAX_QUADS) {
            flush("quadsFull")
        }

        val texSlotIndex = resolveTextureSlot(textureId)

        val effColor = effectiveColor(colorArgb, blendMode)
        val r = ((effColor ushr 16) and 0xFF).toByte()
        val g = ((effColor ushr 8) and 0xFF).toByte()
        val b = (effColor and 0xFF).toByte()
        val a = ((effColor ushr 24) and 0xFF).toByte()

        val texIdxFloat = texSlotIndex.toFloat()
        val vb = vertexData!!
        val cx0 = clipX0; val cy0 = clipY0; val cx1 = clipX1; val cy1 = clipY1

        vb.putFloat(x0).putFloat(y0).putFloat(u0).putFloat(v0)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)
        vb.putFloat(cx0).putFloat(cy0).putFloat(cx1).putFloat(cy1)

        vb.putFloat(x1).putFloat(y1).putFloat(u1).putFloat(v1)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)
        vb.putFloat(cx0).putFloat(cy0).putFloat(cx1).putFloat(cy1)

        vb.putFloat(x2).putFloat(y2).putFloat(u2).putFloat(v2)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)
        vb.putFloat(cx0).putFloat(cy0).putFloat(cx1).putFloat(cy1)

        vb.putFloat(x3).putFloat(y3).putFloat(u3).putFloat(v3)
        vb.put(r).put(g).put(b).put(a).putFloat(texIdxFloat)
        vb.putFloat(cx0).putFloat(cy0).putFloat(cx1).putFloat(cy1)

        quadCount++
    }

    fun drawFilledRect(
        x: Int, y: Int,
        width: Int, height: Int,
        colorArgb: Int,
        blendMode: Int
    ) {
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

    fun drawRectOutline(
        x: Int, y: Int,
        width: Int, height: Int,
        colorArgb: Int,
        blendMode: Int
    ) {
        val x0 = x + 0.35f
        val y0 = y + 0.35f
        val x1 = x0 + width - 1f
        val y1 = y0 + height - 1f

        drawSprite(0, x0, y0, x1, y0 + 1f, 0f, 0f, 0f, 0f, colorArgb, blendMode)
        drawSprite(0, x0, y1, x1, y1 + 1f, 0f, 0f, 0f, 0f, colorArgb, blendMode)
        drawSprite(0, x0, y0, x0 + 1f, y1, 0f, 0f, 0f, 0f, colorArgb, blendMode)
        drawSprite(0, x1 - 1f, y0, x1, y1, 0f, 0f, 0f, 0f, colorArgb, blendMode)
    }

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

        val sx = startX + 0.35f
        val sy = startY + 0.35f
        val ex = endX + deltaX + 0.35f
        val ey = endY + deltaY + 0.35f

        val perpX = -deltaY * 0.5f
        val perpY = deltaX * 0.5f

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

        val perpX = -deltaY * 0.5f
        val perpY = deltaX * 0.5f

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

    fun end() {
        if (!isBatching) return

        flush("end")
        isBatching = false

        shader?.unbind()

        vao?.unbind()
    }

    private fun effectiveColor(colorArgb: Int, blendMode: Int): Int {
        return if (blendMode == 1 || blendMode == 128) colorArgb
        else colorArgb or 0xFF000000.toInt()
    }

    private fun resolveTextureSlot(textureId: Int): Int {
        if (textureId <= 0) return -1

        for (i in 0 until textureSlotCount) {
            if (textureSlots[i] == textureId) return i
        }

        if (textureSlotCount >= MAX_TEXTURE_SLOTS) {
            flush("slotsFull")
        }

        val slotIndex = textureSlotCount
        textureSlots[slotIndex] = textureId
        textureSlotCount++

        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + slotIndex)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureId)

        return slotIndex
    }

    private var clipX0 = 0f
    private var clipY0 = 0f
    private var clipX1 = NO_CLIP
    private var clipY1 = NO_CLIP

    fun setClipRect(x: Int, y: Int, width: Int, height: Int) {
        clipX0 = x.toFloat(); clipY0 = y.toFloat()
        clipX1 = (x + width).toFloat(); clipY1 = (y + height).toFloat()
    }

    fun clearClipRect() {
        clipX0 = 0f; clipY0 = 0f; clipX1 = NO_CLIP; clipY1 = NO_CLIP
    }

    @JvmField var scissorRestore: (() -> Unit)? = null

    @JvmField val flushCauses = HashMap<String, Int>()
    @JvmField var flushQuads = 0L

    fun flush(cause: String) {
        if (quadCount != 0) {
            flushCauses[cause] = (flushCauses[cause] ?: 0) + 1
            flushQuads += quadCount
        }
        flush()
    }

    fun flush() {
        if (quadCount == 0) {
            textureSlotCount = 0
            return
        }

        val vb = vertexData!!
        vb.flip()

        if (batchScreenWidth > 0 && batchScreenHeight > 0) {
            GLWrapper.viewport(0, 0, batchScreenWidth, batchScreenHeight)
        }

        if (captureFlushState) {
            lastFlushFbo = GL33.glGetInteger(GL33.GL_FRAMEBUFFER_BINDING)
            GL33.glGetIntegerv(GL33.GL_VIEWPORT, lastFlushViewport)
            lastFlushScissorEnabled = GL33.glIsEnabled(GL33.GL_SCISSOR_TEST)
            GL33.glGetIntegerv(GL33.GL_SCISSOR_BOX, lastFlushScissorBox)
        }

        vbo!!.upload(vb)

        shader!!.bind()
        if (uTexturesBaseLocation >= 0) {
            GLWrapper.uniform1iv(uTexturesBaseLocation, SAMPLER_UNIT_INDICES)
        }
        for (i in 0 until textureSlotCount) {
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + i)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureSlots[i])
        }

        vao!!.bind()
        ibo!!.bind()
        val indexCount = quadCount * INDICES_PER_QUAD
        GLWrapper.disable(GLWrapper.Constants.SCISSOR_TEST)
        GLWrapper.drawElements(
            GLWrapper.Constants.TRIANGLES,
            indexCount,
            GLWrapper.Constants.UNSIGNED_SHORT,
            0
        )
        scissorRestore?.invoke()

        drawCallsThisFrame++
        quadsThisFrame += quadCount

        vao!!.unbind()
        GLWrapper.useProgram(0)

        quadCount = 0
        textureSlotCount = 0
        vb.clear()
    }

    private fun applyBlendMode(blendMode: Int) {
        if (blendMode == currentBlendMode) return
        currentBlendMode = blendMode

        when (blendMode) {
            1 -> {
                GLWrapper.enable(GLWrapper.Constants.BLEND)
                GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
            }
            2 -> {
                GLWrapper.enable(GLWrapper.Constants.BLEND)
                GLWrapper.blendFunc(GLWrapper.Constants.ONE, GLWrapper.Constants.ONE)
            }
            128 -> {
                GLWrapper.enable(GLWrapper.Constants.BLEND)
                GLWrapper.blendFunc(GLWrapper.Constants.DST_COLOR, GLWrapper.Constants.ONE)
            }
            else -> {
                GLWrapper.disable(GLWrapper.Constants.BLEND)
            }
        }
    }

    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, quadsThisFrame, textureSlotCount)
    }

    fun resetFrameStats() {
        drawCallsThisFrame = 0
        quadsThisFrame = 0
    }

    fun isBatching(): Boolean = isBatching

    fun isReady(): Boolean = isInitialized

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
