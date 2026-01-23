package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.core.VertexFormats
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_SHORT
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Modern OpenGL renderer for ground/terrain.
 *
 * Handles terrain rendering with:
 * - Material-based batching
 * - Per-vertex lighting (baked into vertex colors)
 * - Shadow mapping support
 * - Fog
 *
 * Vertex format (28 bytes per vertex):
 * - Position: vec3 (12 bytes) at offset 0
 * - Normal: vec3 (12 bytes) at offset 12
 * - Color: RGBA (4 bytes) at offset 24
 */
class ModernGroundRenderer {

    companion object {
        const val BYTES_PER_VERTEX = 28
        private const val DEFAULT_VERTEX_CAPACITY = 65536
        private const val DEFAULT_INDEX_CAPACITY = 131072
    }

    // GPU resources
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    // CPU staging buffer
    private var vertexData: ByteBuffer? = null
    private var indexData: java.nio.ShortBuffer? = null

    // Matrices
    private val viewMatrix = Matrix4f()
    private val projectionMatrix = Matrix4f()
    private val modelViewMatrix = Matrix4f()
    private val modelViewProjectionMatrix = Matrix4f()
    private val normalMatrix = Matrix3f()

    // Shadow matrix for shadow mapping
    private val shadowMatrix = Matrix4f()

    // Lighting
    private val sunDirection = Vector3f(0f, 1f, 0f)
    private val sunColor = Vector3f(1f, 1f, 1f)
    private var ambient = 0.3f
    private var contrast = 0.7f

    // Fog
    private var fogEnabled = false
    private var fogStart = 100f
    private var fogEnd = 500f
    private val fogColor = floatArrayOf(0.7f, 0.8f, 0.9f, 1f)

    // Shadow mapping
    private var shadowEnabled = false
    private var shadowMapTextureId = 0
    private var shadowBias = 0.005f
    private var shadowIntensity = 0.5f

    // Uniform locations
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
    private var uShadowMatrixLocation = -1
    private var uShadowEnabledLocation = -1
    private var uShadowMapLocation = -1
    private var uShadowBiasLocation = -1
    private var uShadowIntensityLocation = -1

    private var isInitialized = false

    // Statistics
    private var drawCallsThisFrame = 0
    private var trianglesThisFrame = 0

    /**
     * Initializes the renderer.
     */
    fun initialize() {
        if (isInitialized) return

        shader = ShaderProgram.createFromResources("/shaders/ground.vert", "/shaders/ground.frag")

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.DYNAMIC)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        vao!!.bind()
        vbo!!.bind()
        VertexFormats.setupGroundFormat(vao!!)
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
        uShadowMatrixLocation = s.getUniformLocation("uShadowMatrix")
        uShadowEnabledLocation = s.getUniformLocation("uShadowEnabled")
        uShadowMapLocation = s.getUniformLocation("uShadowMap")
        uShadowBiasLocation = s.getUniformLocation("uShadowBias")
        uShadowIntensityLocation = s.getUniformLocation("uShadowIntensity")
        s.unbind()
    }

    /**
     * Sets the projection matrix.
     */
    fun setProjection(matrix: Matrix4f) {
        projectionMatrix.set(matrix)
    }

    /**
     * Sets the view matrix.
     */
    fun setView(matrix: Matrix4f) {
        viewMatrix.set(matrix)
    }

    /**
     * Configures lighting.
     */
    fun setLighting(direction: Vector3f, color: Vector3f, ambientIntensity: Float, contrastIntensity: Float) {
        sunDirection.set(direction).normalize()
        sunColor.set(color)
        ambient = ambientIntensity
        contrast = contrastIntensity
    }

    /**
     * Configures fog.
     */
    fun setFog(enabled: Boolean, start: Float, end: Float, r: Float, g: Float, b: Float) {
        fogEnabled = enabled
        fogStart = start
        fogEnd = end
        fogColor[0] = r
        fogColor[1] = g
        fogColor[2] = b
    }

    /**
     * Configures shadow mapping.
     */
    fun setShadowMapping(enabled: Boolean, shadowMap: Int, matrix: Matrix4f, bias: Float = 0.005f, intensity: Float = 0.5f) {
        shadowEnabled = enabled
        shadowMapTextureId = shadowMap
        shadowMatrix.set(matrix)
        shadowBias = bias
        shadowIntensity = intensity
    }

    /**
     * Begins a render frame.
     */
    fun beginFrame() {
        check(isInitialized) { "Renderer not initialized" }

        drawCallsThisFrame = 0
        trianglesThisFrame = 0

        shader!!.bind()
        setLightingUniforms()
        setFogUniforms()
        setShadowUniforms()
    }

    /**
     * Ends a render frame.
     */
    fun endFrame() {
        shader?.unbind()
    }

    /**
     * Draws terrain with the provided vertex and index data.
     *
     * @param vertices ByteBuffer with interleaved vertex data (28 bytes per vertex)
     * @param indices ShortBuffer with indices
     * @param indexCount Number of indices
     */
    fun drawIndexed(vertices: ByteBuffer, indices: java.nio.ShortBuffer, indexCount: Int) {
        if (indexCount == 0) return

        // Use identity model matrix for terrain (world space)
        projectionMatrix.mul(viewMatrix, modelViewProjectionMatrix)
        viewMatrix.normal(normalMatrix)

        shader!!.setUniformMatrix4(uMVPLocation, modelViewProjectionMatrix)
        shader!!.setUniformMatrix4(uMVLocation, viewMatrix)
        shader!!.setUniform("uNormalMatrix", normalMatrix)

        vbo!!.upload(vertices)
        ibo!!.upload(indices)

        vao!!.bind()
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    /**
     * Draws a terrain tile with separate attribute arrays.
     *
     * @param positions Vertex positions [x,y,z, ...]
     * @param normals Vertex normals [nx,ny,nz, ...]
     * @param colors Vertex colors [r,g,b,a, ...]
     * @param indices Triangle indices
     * @param vertexCount Number of vertices
     * @param indexCount Number of indices
     */
    fun drawTile(
        positions: FloatArray,
        normals: FloatArray,
        colors: ByteArray,
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

        // Interleave vertex data
        for (i in 0 until vertexCount) {
            // Position (12 bytes)
            vb.putFloat(positions[i * 3])
            vb.putFloat(positions[i * 3 + 1])
            vb.putFloat(positions[i * 3 + 2])

            // Normal (12 bytes)
            vb.putFloat(normals[i * 3])
            vb.putFloat(normals[i * 3 + 1])
            vb.putFloat(normals[i * 3 + 2])

            // Color (4 bytes)
            vb.put(colors[i * 4])
            vb.put(colors[i * 4 + 1])
            vb.put(colors[i * 4 + 2])
            vb.put(colors[i * 4 + 3])
        }

        ib.put(indices, 0, indexCount)

        vb.flip()
        ib.flip()

        drawIndexed(vb, ib, indexCount)
    }

    /**
     * Gets frame statistics.
     */
    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, trianglesThisFrame)
    }

    /**
     * Releases resources.
     */
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
    }

    private fun setFogUniforms() {
        val s = shader ?: return
        s.setUniform(uFogEnabledLocation, if (fogEnabled) 1 else 0)
        s.setUniform(uFogStartLocation, fogStart)
        s.setUniform(uFogEndLocation, fogEnd)
        s.setUniform(uFogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])
    }

    private fun setShadowUniforms() {
        val s = shader ?: return
        s.setUniform(uShadowEnabledLocation, if (shadowEnabled) 1 else 0)
        if (shadowEnabled) {
            s.setUniform("uShadowMatrix", shadowMatrix)
            s.setUniform(uShadowMapLocation, 1) // Texture unit 1
            s.setUniform(uShadowBiasLocation, shadowBias)
            s.setUniform(uShadowIntensityLocation, shadowIntensity)

            // Bind shadow map to texture unit 1
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + 1)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, shadowMapTextureId)
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        }
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
