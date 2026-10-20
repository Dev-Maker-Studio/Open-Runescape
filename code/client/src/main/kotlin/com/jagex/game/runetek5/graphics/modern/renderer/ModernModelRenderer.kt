package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.batch.BatchRenderer
import com.jagex.game.runetek5.graphics.modern.batch.DrawBatch
import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.core.VertexFormats
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_SHORT
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class ModernModelRenderer {

    companion object {
        const val BYTES_PER_VERTEX = 36
        private const val DEFAULT_VERTEX_CAPACITY = 32768
        private const val DEFAULT_INDEX_CAPACITY = 65536
    }

    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    private var vertexData: ByteBuffer? = null
    private var indexData: java.nio.ShortBuffer? = null

    private val modelMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val projectionMatrix = Matrix4f()
    private val modelViewMatrix = Matrix4f()
    private val modelViewProjectionMatrix = Matrix4f()
    private val normalMatrix = Matrix3f()

    private val sunDirection = Vector3f(0f, 1f, 0f)
    private val sunColor = Vector3f(1f, 1f, 1f)
    private var ambient = 0.3f
    private var contrast = 0.7f

    private var fogEnabled = false
    private var fogStart = 100f
    private var fogEnd = 500f
    private val fogColor = floatArrayOf(0.7f, 0.8f, 0.9f, 1f)

    private var uMVPLocation = -1
    private var uMVLocation = -1
    private var uNormalMatrixLocation = -1
    private var uSunDirectionLocation = -1
    private var uSunColorLocation = -1
    private var uAmbientLocation = -1
    private var uContrastLocation = -1
    private var uFogStartLocation = -1
    private var uFogEndLocation = -1
    private var uFogEnabledLocation = -1
    private var uFogColorLocation = -1
    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uAlphaThresholdLocation = -1
    private var uPreLitColorsLocation = -1

    private var preLitColors = false

    private var isInitialized = false
    private var currentTextureId = -1
    private var isShaderBound = false

    private var drawCallsThisFrame = 0
    private var trianglesThisFrame = 0

    fun initialize() {
        if (isInitialized) return

        shader = ShaderProgram.createFromResources("/shaders/model.vert", "/shaders/model.frag")

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.DYNAMIC)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        vao!!.bind()
        vbo!!.bind()
        VertexFormats.setupModelFormat(vao!!)
        ibo!!.bind()
        vao!!.unbind()

        vertexData = MemoryUtil.memAlloc(DEFAULT_VERTEX_CAPACITY * BYTES_PER_VERTEX)
        indexData = MemoryUtil.memAllocShort(DEFAULT_INDEX_CAPACITY)

        cacheUniformLocations()

        isInitialized = true
    }

    private fun cacheUniformLocations() {
        val s = shader ?: return
        s.bind()
        uMVPLocation = s.getUniformLocation("uModelViewProjection")
        uMVLocation = s.getUniformLocation("uModelView")
        uNormalMatrixLocation = s.getUniformLocation("uNormalMatrix")
        uSunDirectionLocation = s.getUniformLocation("uSunDirection")
        uSunColorLocation = s.getUniformLocation("uSunColor")
        uAmbientLocation = s.getUniformLocation("uAmbient")
        uContrastLocation = s.getUniformLocation("uContrast")
        uFogStartLocation = s.getUniformLocation("uFogStart")
        uFogEndLocation = s.getUniformLocation("uFogEnd")
        uFogEnabledLocation = s.getUniformLocation("uFogEnabled")
        uFogColorLocation = s.getUniformLocation("uFogColor")
        uTextureLocation = s.getUniformLocation("uTexture")
        uTextureEnabledLocation = s.getUniformLocation("uTextureEnabled")
        uAlphaThresholdLocation = s.getUniformLocation("uAlphaThreshold")
        uPreLitColorsLocation = s.getUniformLocation("uPreLitColors")
        s.unbind()
    }

    fun setProjection(matrix: Matrix4f) {
        projectionMatrix.set(matrix)
    }

    fun setView(matrix: Matrix4f) {
        viewMatrix.set(matrix)
    }

    fun setModelMatrix(matrix: Matrix4f) {
        modelMatrix.set(matrix)
    }

    fun setLighting(direction: Vector3f, color: Vector3f, ambientIntensity: Float, contrastIntensity: Float) {
        sunDirection.set(direction).normalize()
        sunColor.set(color)
        ambient = ambientIntensity
        contrast = contrastIntensity
    }

    fun setFog(enabled: Boolean, start: Float, end: Float, r: Float, g: Float, b: Float) {
        fogEnabled = enabled
        fogStart = start
        fogEnd = end
        fogColor[0] = r
        fogColor[1] = g
        fogColor[2] = b
    }

    fun setPreLitColors(preLit: Boolean) {
        this.preLitColors = preLit
    }

    fun ensureReady() {
        if (!isInitialized) return
        if (!isShaderBound) {
            shader!!.bind()
            setLightingUniforms()
            setFogUniforms()
            shader!!.setUniform(uTextureLocation, 0)
            shader!!.setUniform(uTextureEnabledLocation, 0)
            shader!!.setUniform(uAlphaThresholdLocation, 0f)
            isShaderBound = true
        }
    }

    fun beginFrame() {
        check(isInitialized) { "Renderer not initialized. Call initialize() first." }

        drawCallsThisFrame = 0
        trianglesThisFrame = 0
        currentTextureId = -1

        shader!!.bind()
        setLightingUniforms()
        setFogUniforms()
        shader!!.setUniform(uTextureLocation, 0)
        shader!!.setUniform(uTextureEnabledLocation, 0)
        shader!!.setUniform(uAlphaThresholdLocation, 0f)
        isShaderBound = true
    }

    fun endFrame() {
        shader?.unbind()
        isShaderBound = false
    }

    fun bindTexture(textureId: Int) {
        if (textureId != currentTextureId) {
            currentTextureId = textureId
            if (textureId > 0) {
                GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                GLWrapper.bindTexture(TEXTURE_2D, textureId)
                shader!!.setUniform(uTextureEnabledLocation, 1)
            } else {
                shader!!.setUniform(uTextureEnabledLocation, 0)
            }
        }
    }

    fun drawIndexed(vertices: ByteBuffer, indices: java.nio.ShortBuffer, indexCount: Int) {
        if (indexCount == 0) return

        ensureReady()

        viewMatrix.mul(modelMatrix, modelViewMatrix)
        projectionMatrix.mul(modelViewMatrix, modelViewProjectionMatrix)
        modelViewMatrix.normal(normalMatrix)

        shader!!.setUniformMatrix4(uMVPLocation, modelViewProjectionMatrix)
        shader!!.setUniformMatrix4(uMVLocation, modelViewMatrix)
        shader!!.setUniform("uNormalMatrix", normalMatrix)

        shader!!.setUniform(uPreLitColorsLocation, if (preLitColors) 1 else 0)

        vbo!!.upload(vertices)

        ibo!!.upload(indices)

        vao!!.bind()
        ibo!!.bind()
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    fun drawWithSeparateArrays(
        positions: FloatArray,
        normals: FloatArray?,
        texCoords: FloatArray?,
        colors: ByteArray?,
        indices: ShortArray,
        vertexCount: Int,
        indexCount: Int
    ) {
        if (indexCount == 0 || vertexCount == 0) return

        ensureVertexCapacity(vertexCount)
        ensureIndexCapacity(indexCount)

        val vb = vertexData!!
        val ib = indexData!!

        vb.clear()
        ib.clear()

        for (i in 0 until vertexCount) {
            vb.putFloat(positions[i * 3])
            vb.putFloat(positions[i * 3 + 1])
            vb.putFloat(positions[i * 3 + 2])

            if (normals != null) {
                vb.putFloat(normals[i * 3])
                vb.putFloat(normals[i * 3 + 1])
                vb.putFloat(normals[i * 3 + 2])
            } else {
                vb.putFloat(0f)
                vb.putFloat(1f)
                vb.putFloat(0f)
            }

            if (texCoords != null) {
                vb.putFloat(texCoords[i * 2])
                vb.putFloat(texCoords[i * 2 + 1])
            } else {
                vb.putFloat(0f)
                vb.putFloat(0f)
            }

            if (colors != null) {
                vb.put(colors[i * 4])
                vb.put(colors[i * 4 + 1])
                vb.put(colors[i * 4 + 2])
                vb.put(colors[i * 4 + 3])
            } else {
                vb.put(255.toByte())
                vb.put(255.toByte())
                vb.put(255.toByte())
                vb.put(255.toByte())
            }
        }

        ib.put(indices, 0, indexCount)

        vb.flip()
        ib.flip()

        drawIndexed(vb, ib, indexCount)
    }

    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, trianglesThisFrame)
    }

    fun getTextureEnabledUniformLocation(): Int = uTextureEnabledLocation

    fun getShader(): ShaderProgram? = shader

    fun destroy() {
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()

        vertexData?.let { MemoryUtil.memFree(it) }
        indexData?.let { MemoryUtil.memFree(it) }

        shader = null
        vao = null
        vbo = null
        ibo = null
        vertexData = null
        indexData = null

        isInitialized = false
    }

    private fun setLightingUniforms() {
        val s = shader ?: return
        s.setUniform(uSunDirectionLocation, sunDirection.x, sunDirection.y, sunDirection.z)
        s.setUniform("uSunColor", sunColor)
        s.setUniform(uAmbientLocation, ambient)
        s.setUniform(uContrastLocation, contrast)
        s.setUniform(uPreLitColorsLocation, if (preLitColors) 1 else 0)
    }

    private fun setFogUniforms() {
        val s = shader ?: return
        s.setUniform(uFogEnabledLocation, if (fogEnabled) 1 else 0)
        s.setUniform(uFogStartLocation, fogStart)
        s.setUniform(uFogEndLocation, fogEnd)
        s.setUniform(uFogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])
    }

    private fun ensureVertexCapacity(vertexCount: Int) {
        val requiredBytes = vertexCount * BYTES_PER_VERTEX
        val vb = vertexData
        if (vb == null || requiredBytes > vb.capacity()) {
            vb?.let { MemoryUtil.memFree(it) }
            val newCapacity = (requiredBytes * 1.5).toInt().coerceAtLeast(DEFAULT_VERTEX_CAPACITY * BYTES_PER_VERTEX)
            vertexData = MemoryUtil.memAlloc(newCapacity)
        }
    }

    private fun ensureIndexCapacity(indexCount: Int) {
        val ib = indexData
        if (ib == null || indexCount > ib.capacity()) {
            ib?.let { MemoryUtil.memFree(it) }
            val newCapacity = (indexCount * 1.5).toInt().coerceAtLeast(DEFAULT_INDEX_CAPACITY)
            indexData = MemoryUtil.memAllocShort(newCapacity)
        }
    }

    data class FrameStats(
        val drawCalls: Int,
        val triangles: Int
    )
}
