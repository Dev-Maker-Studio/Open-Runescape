package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.math.MatrixRotations
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

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
        s.setUniform(s.getUniformLocation("uAmbient"), 1f, 1f, 1f)
        s.unbind()
        isInitialized = true
    }

    fun setFrame(projectionFloats: FloatArray, fogEnabled: Boolean, fogStart: Float, fogEnd: Float, fogR: Float, fogG: Float, fogB: Float) {
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
