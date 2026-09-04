package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.math.MatrixRotations
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Model billboards (the glow sprites anchored to model faces — lamps, fires) on the
 * modern path: the twin of ModelGL.drawBillboards.
 *
 * Legacy draws each one as a camera-facing textured quad in EYE space: the anchor
 * face's centroid is transformed by model x view, pulled toward the camera by the
 * billboard's distance along the eye ray, offset by the surface's (offsetX, offsetY),
 * scaled to (scaleX*sizeX>>7, scaleY*sizeY>>7) half-extents, rotated in the screen
 * plane and drawn as GL_QUADS with the anchor face's colour, the material's texture,
 * the shape as blend mode (0 none, 1 alpha, 2 additive) and depth writes off when
 * the model has alpha transitions. The quad corners and UVs are the toolkit's
 * screen quad: (-1,-1) uv(0,0), (-1,1) uv(0,1), (1,1) uv(1,1), (1,-1) uv(1,0).
 *
 * Positions are already in eye space, so the shader (the particle shader) gets the
 * projection alone as its view-projection, built exactly the way the particle
 * renderer composes it (row-vector legacy convention, Y negated for GL).
 */
class ModernBillboardRenderer {
    private var shader: ShaderProgram? = null
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var vertexData: ByteBuffer? = null
    private var isInitialized = false

    private var uViewProjectionLocation = -1
    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uFogEnabledLocation = -1
    private var uFogStartLocation = -1
    private var uFogEndLocation = -1
    private var uFogColorLocation = -1

    private val vp = FloatArray(16)

    /** Draws this frame (diag). */
    @JvmField var drawsThisFrame = 0

    fun initialize() {
        if (isInitialized) return
        shader = ShaderProgram.createFromResources("/shaders/particle.vert", "/shaders/particle.frag")
        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STATIC, IndexBuffer.IndexType.UNSIGNED_SHORT)
        vao!!.bind()
        vbo!!.bind()
        vao!!.setAttribute(VertexArray.Companion.Attributes.POSITION, 3, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 0)
        vao!!.setAttribute(VertexArray.Companion.Attributes.TEXCOORD, 2, GLWrapper.Constants.FLOAT, false, BYTES_PER_VERTEX, 12)
        vao!!.setAttribute(VertexArray.Companion.Attributes.COLOR, 4, GLWrapper.Constants.UNSIGNED_BYTE, true, BYTES_PER_VERTEX, 20)
        val indexData = MemoryUtil.memAllocShort(6)
        // Two triangles over the quad in the legacy corner order.
        indexData.put(0).put(1).put(2).put(2).put(3).put(0)
        indexData.flip()
        ibo!!.upload(indexData)
        MemoryUtil.memFree(indexData)
        vao!!.unbind()
        vertexData = MemoryUtil.memAlloc(4 * BYTES_PER_VERTEX)
        val s = shader!!
        s.bind()
        uViewProjectionLocation = s.getUniformLocation("uViewProjection")
        uTextureLocation = s.getUniformLocation("uTexture")
        uTextureEnabledLocation = s.getUniformLocation("uTextureEnabled")
        uFogEnabledLocation = s.getUniformLocation("uFogEnabled")
        uFogStartLocation = s.getUniformLocation("uFogStart")
        uFogEndLocation = s.getUniformLocation("uFogEnd")
        uFogColorLocation = s.getUniformLocation("uFogColor")
        s.unbind()
        isInitialized = true
    }

    /** Per-frame projection (legacy row-vector projection matrix elements) and fog. */
    fun setFrame(projectionFloats: FloatArray, fogEnabled: Boolean, fogStart: Float, fogEnd: Float, fogR: Float, fogG: Float, fogB: Float) {
        // View is the identity (positions are eye space): vp = projection, indexed the
        // way ModernParticleRenderer builds its (V·P)^T, then clip-Y negated for GL.
        for (i in 0 until 16) vp[i] = projectionFloats[i]
        vp[1] = -vp[1]; vp[5] = -vp[5]; vp[9] = -vp[9]; vp[13] = -vp[13]
        this.fogEnabled = fogEnabled
        this.fogStart = fogStart; this.fogEnd = fogEnd
        this.fogR = fogR; this.fogG = fogG; this.fogB = fogB
        drawsThisFrame = 0
    }

    private var fogEnabled = false
    private var fogStart = 0f
    private var fogEnd = 0f
    private var fogR = 0f
    private var fogG = 0f
    private var fogB = 0f

    /**
     * Draws one billboard immediately (other translucent passes may run between
     * calls, so every call binds its own state).
     *
     * @param ex eye-space centre (after the distance pull and surface offsets)
     * @param halfW half extents in world units, [rotation] in RS angle units (0..16383)
     * @param argb anchor face colour with alpha in the top byte
     * @param blendMode legacy setBlendMode argument (the billboard shape)
     */
    fun drawOne(
        ex: Float, ey: Float, ez: Float,
        halfW: Int, halfH: Int, rotation: Int,
        argb: Int, textureHandle: Int, blendMode: Int, depthWrite: Boolean
    ) {
        if (!isInitialized) return
        val s = shader ?: return
        val cos: Float
        val sin: Float
        if (rotation != 0) {
            cos = MatrixRotations.COS[rotation and 0x3fff]
            sin = MatrixRotations.SIN[rotation and 0x3fff]
        } else {
            cos = 1f; sin = 0f
        }
        val vb = vertexData!!
        vb.clear()
        val r = ((argb shr 16) and 0xff).toByte()
        val g = ((argb shr 8) and 0xff).toByte()
        val b = (argb and 0xff).toByte()
        val a = ((argb ushr 24) and 0xff).toByte()
        // Matrix44.setRotatedTranslation as a row-vector transform of the unit quad:
        // x' = cx*cos*w - cy*sin*h ; y' = cx*sin*w + cy*cos*h (see OpenGlToolkit's
        // screen quad for the corner/uv pairs).
        fun corner(cx: Float, cy: Float, u: Float, v: Float) {
            val x = cx * cos * halfW - cy * sin * halfH
            val y = cx * sin * halfW + cy * cos * halfH
            vb.putFloat(ex + x).putFloat(ey + y).putFloat(ez)
            vb.putFloat(u).putFloat(v)
            vb.put(r).put(g).put(b).put(a)
        }
        corner(-1f, -1f, 0f, 0f)
        corner(-1f, 1f, 0f, 1f)
        corner(1f, 1f, 1f, 1f)
        corner(1f, -1f, 1f, 0f)
        vb.flip()

        s.bind()
        GLWrapper.uniformMatrix4fv(uViewProjectionLocation, false, vp)
        s.setUniform(uTextureLocation, 0)
        s.setUniform(uFogEnabledLocation, if (fogEnabled) 1 else 0)
        if (fogEnabled) {
            s.setUniform(uFogStartLocation, fogStart)
            s.setUniform(uFogEndLocation, fogEnd)
            s.setUniform(uFogColorLocation, fogR, fogG, fogB, 1.0f)
        }
        if (textureHandle > 0) {
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureHandle)
            s.setUniform(uTextureEnabledLocation, 1)
        } else {
            s.setUniform(uTextureEnabledLocation, 0)
        }
        // Legacy setBlendMode: 1 alpha, 2 additive, 128 multiply, else no blending.
        when (blendMode) {
            1 -> { GLWrapper.enable(GLWrapper.Constants.BLEND); GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA) }
            2 -> { GLWrapper.enable(GLWrapper.Constants.BLEND); GLWrapper.blendFunc(GLWrapper.Constants.ONE, GLWrapper.Constants.ONE) }
            128 -> { GLWrapper.enable(GLWrapper.Constants.BLEND); GLWrapper.blendFunc(GLWrapper.Constants.DST_COLOR, GLWrapper.Constants.ONE) }
            else -> GLWrapper.disable(GLWrapper.Constants.BLEND)
        }
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthMask(depthWrite)
        GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
        vao!!.bind()
        vbo!!.upload(vb)
        ibo!!.bind()
        GLWrapper.drawElements(GLWrapper.Constants.TRIANGLES, 6, GLWrapper.Constants.UNSIGNED_SHORT, 0)
        vao!!.unbind()
        s.unbind()
        GLWrapper.depthMask(true)
        GLWrapper.enable(GLWrapper.Constants.BLEND)
        GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
        drawsThisFrame++
    }

    fun destroy() {
        vertexData?.let { MemoryUtil.memFree(it) }
        vertexData = null
        vao?.destroy(); vbo?.destroy(); ibo?.destroy(); shader?.destroy()
        vao = null; vbo = null; ibo = null; shader = null
        isInitialized = false
    }

    companion object {
        private const val BYTES_PER_VERTEX = 24
    }
}
