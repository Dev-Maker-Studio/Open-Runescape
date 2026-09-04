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

/**
 * Modern OpenGL renderer for ModelGL instances.
 *
 * This class provides a modern rendering path that can be used alongside
 * or instead of the legacy fixed-function pipeline in OpenGlToolkit.
 *
 * The renderer uses:
 * - VAOs for vertex attribute state
 * - VBOs for vertex data (DYNAMIC for per-frame updates)
 * - EBOs for index data
 * - Shader programs for programmable pipeline
 * - Uniform caching for performance
 *
 * Vertex format (36 bytes per vertex):
 * - Position: vec3 (12 bytes) at offset 0
 * - Normal: vec3 (12 bytes) at offset 12
 * - TexCoord: vec2 (8 bytes) at offset 24
 * - Color: RGBA (4 bytes) at offset 32
 */
class ModernModelRenderer {

    companion object {
        const val BYTES_PER_VERTEX = 36
        private const val DEFAULT_VERTEX_CAPACITY = 32768
        private const val DEFAULT_INDEX_CAPACITY = 65536
    }

    // GPU resources
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    // CPU staging buffers
    private var vertexData: ByteBuffer? = null
    private var indexData: java.nio.ShortBuffer? = null

    // Matrices (reused to avoid allocation)
    private val modelMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val projectionMatrix = Matrix4f()
    private val modelViewMatrix = Matrix4f()
    private val modelViewProjectionMatrix = Matrix4f()
    private val normalMatrix = Matrix3f()

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

    // Uniform locations (cached)
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

    // State: whether to use pre-lit colors (skip shader lighting)
    private var preLitColors = false

    // State tracking
    private var isInitialized = false
    private var currentTextureId = -1
    private var isShaderBound = false

    // Statistics
    private var drawCallsThisFrame = 0
    private var trianglesThisFrame = 0

    /**
     * Initializes the renderer with shader and GPU resources.
     * Must be called before rendering.
     */
    fun initialize() {
        if (isInitialized) return

        // Create shader from resources
        shader = ShaderProgram.createFromResources("/shaders/model.vert", "/shaders/model.frag")

        // Create GPU resources
        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.DYNAMIC)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        // Setup VAO with vertex format
        vao!!.bind()
        vbo!!.bind()
        VertexFormats.setupModelFormat(vao!!)
        ibo!!.bind()
        vao!!.unbind()

        // Allocate CPU staging buffers
        vertexData = MemoryUtil.memAlloc(DEFAULT_VERTEX_CAPACITY * BYTES_PER_VERTEX)
        indexData = MemoryUtil.memAllocShort(DEFAULT_INDEX_CAPACITY)

        // Cache uniform locations
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
     * Sets the model matrix for the next draw call.
     */
    fun setModelMatrix(matrix: Matrix4f) {
        modelMatrix.set(matrix)
    }

    /**
     * Configures lighting parameters.
     */
    fun setLighting(direction: Vector3f, color: Vector3f, ambientIntensity: Float, contrastIntensity: Float) {
        sunDirection.set(direction).normalize()
        sunColor.set(color)
        ambient = ambientIntensity
        contrast = contrastIntensity
    }

    /**
     * Configures fog parameters.
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
     * Sets whether colors passed to the renderer are pre-lit.
     * When true, the shader will skip lighting calculations and use vertex colors directly.
     */
    fun setPreLitColors(preLit: Boolean) {
        this.preLitColors = preLit
    }

    /**
     * Ensures the shader is bound and ready for drawing.
     * Called automatically by draw methods when needed.
     */
    fun ensureReady() {
        if (!isInitialized) return
        if (!isShaderBound) {
            shader!!.bind()
            setLightingUniforms()
            setFogUniforms()
            shader!!.setUniform(uTextureLocation, 0)
            shader!!.setUniform(uTextureEnabledLocation, 0) // Textures disabled by default
            shader!!.setUniform(uAlphaThresholdLocation, 0f)
            isShaderBound = true
        }
    }

    /**
     * Begins a render frame.
     * Must be called before any draw calls.
     */
    fun beginFrame() {
        check(isInitialized) { "Renderer not initialized. Call initialize() first." }

        drawCallsThisFrame = 0
        trianglesThisFrame = 0
        currentTextureId = -1

        // Bind shader and set per-frame uniforms
        shader!!.bind()
        setLightingUniforms()
        setFogUniforms()
        shader!!.setUniform(uTextureLocation, 0) // Texture unit 0
        shader!!.setUniform(uTextureEnabledLocation, 0) // Textures disabled by default
        shader!!.setUniform(uAlphaThresholdLocation, 0f)
        isShaderBound = true
    }

    /**
     * Ends a render frame.
     */
    fun endFrame() {
        shader?.unbind()
        isShaderBound = false
    }

    /**
     * Binds a texture for subsequent draw calls.
     *
     * @param textureId The OpenGL texture ID, or 0/negative for no texture
     */
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

    /**
     * Draws a model batch with interleaved vertex data.
     *
     * This method uploads the provided vertex and index data to the GPU
     * and issues a draw call.
     *
     * @param vertices ByteBuffer with interleaved vertex data (36 bytes per vertex)
     * @param indices ShortBuffer with triangle indices
     * @param indexCount Number of indices to draw
     */
    fun drawIndexed(vertices: ByteBuffer, indices: java.nio.ShortBuffer, indexCount: Int) {
        if (indexCount == 0) return

        // Ensure shader is bound (in case beginFrame wasn't called)
        ensureReady()

        // Calculate matrices
        viewMatrix.mul(modelMatrix, modelViewMatrix)
        projectionMatrix.mul(modelViewMatrix, modelViewProjectionMatrix)
        modelViewMatrix.normal(normalMatrix)

        // Set transform uniforms
        shader!!.setUniformMatrix4(uMVPLocation, modelViewProjectionMatrix)
        shader!!.setUniformMatrix4(uMVLocation, modelViewMatrix)
        shader!!.setUniform("uNormalMatrix", normalMatrix)

        // Set pre-lit colors mode (must be set per-draw since it can change)
        shader!!.setUniform(uPreLitColorsLocation, if (preLitColors) 1 else 0)

        // Upload vertex data
        vbo!!.upload(vertices)

        // Upload index data
        ibo!!.upload(indices)

        // Draw
        vao!!.bind()
        ibo!!.bind()
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    /**
     * Draws a model using separate attribute arrays.
     *
     * This method converts separate attribute arrays into the interleaved format
     * expected by the shader, then uploads and draws.
     *
     * @param positions Float array of vertex positions [x,y,z, x,y,z, ...]
     * @param normals Float array of normals [nx,ny,nz, nx,ny,nz, ...] (can be null)
     * @param texCoords Float array of texture coordinates [u,v, u,v, ...] (can be null)
     * @param colors Byte array of colors [r,g,b,a, r,g,b,a, ...] (can be null)
     * @param indices Short array of triangle indices
     * @param vertexCount Number of vertices
     * @param indexCount Number of indices
     */
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

        // Ensure staging buffer capacity
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
            if (normals != null) {
                vb.putFloat(normals[i * 3])
                vb.putFloat(normals[i * 3 + 1])
                vb.putFloat(normals[i * 3 + 2])
            } else {
                vb.putFloat(0f)
                vb.putFloat(1f)
                vb.putFloat(0f)
            }

            // TexCoord (8 bytes)
            if (texCoords != null) {
                vb.putFloat(texCoords[i * 2])
                vb.putFloat(texCoords[i * 2 + 1])
            } else {
                vb.putFloat(0f)
                vb.putFloat(0f)
            }

            // Color (4 bytes)
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

        // Copy indices
        ib.put(indices, 0, indexCount)

        // Flip buffers
        vb.flip()
        ib.flip()

        // Draw
        drawIndexed(vb, ib, indexCount)
    }

    /**
     * Gets rendering statistics for the current frame.
     */
    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, trianglesThisFrame)
    }

    /**
     * Gets the uniform location for the texture enabled flag.
     * Used by FrameBatcher to set this uniform when rendering batched geometry.
     */
    fun getTextureEnabledUniformLocation(): Int = uTextureEnabledLocation

    /**
     * Gets the shader program used by this renderer.
     * Used by FrameBatcher to set uniforms when rendering batched geometry.
     */
    fun getShader(): ShaderProgram? = shader

    /**
     * Releases all GPU and CPU resources.
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
