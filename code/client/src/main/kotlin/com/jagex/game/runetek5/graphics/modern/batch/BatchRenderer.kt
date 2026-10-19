package com.jagex.game.runetek5.graphics.modern.batch

import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * Batch renderer that manages multiple draw batches for efficient rendering.
 *
 * Groups geometry by texture/material to minimize state changes and draw calls.
 * Automatically flushes batches when state changes or when explicitly requested.
 *
 * Usage:
 * ```kotlin
 * renderer.begin(projectionMatrix, viewMatrix)
 * renderer.setTexture(textureId)
 * renderer.addModel(model, modelMatrix)
 * // ... add more geometry
 * renderer.end() // flushes all batches
 * ```
 */
class BatchRenderer(
    private val shader: ShaderProgram,
    maxBatches: Int = 64
) {
    // Batch pool
    private val batches = Array(maxBatches) { DrawBatch() }
    private val activeBatches = mutableMapOf<Long, DrawBatch>()
    private var nextBatchIndex = 0

    // Current state
    private var currentTextureId = -1
    private var currentMaterialId = 0
    private var currentBatch: DrawBatch? = null

    // Matrices (reused to avoid allocation)
    private val projectionMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val modelViewMatrix = Matrix4f()
    private val modelViewProjectionMatrix = Matrix4f()
    private val normalMatrix = Matrix3f()

    // Lighting state
    private val sunDirection = Vector3f(0.0f, 1.0f, 0.0f)
    private val sunColor = Vector3f(1.0f, 1.0f, 1.0f)
    private var ambientIntensity = 0.3f
    private var contrastIntensity = 0.7f

    // Fog state
    private var fogEnabled = false
    private var fogStart = 100.0f
    private var fogEnd = 500.0f
    private val fogColor = floatArrayOf(0.7f, 0.8f, 0.9f, 1.0f)

    // Uniform locations (cached for performance)
    private var mvpLocation = -1
    private var mvLocation = -1
    private var normalMatrixLocation = -1
    private var sunDirLocation = -1
    private var sunColorLocation = -1
    private var ambientLocation = -1
    private var contrastLocation = -1
    private var fogStartLocation = -1
    private var fogEndLocation = -1
    private var fogEnabledLocation = -1
    private var fogColorLocation = -1
    private var textureLocation = -1
    private var textureEnabledLocation = -1
    private var alphaThresholdLocation = -1

    // Statistics
    private var drawCallCount = 0
    private var triangleCount = 0
    private var stateChangeCount = 0

    init {
        cacheUniformLocations()
    }

    private fun cacheUniformLocations() {
        shader.bind()
        mvpLocation = shader.getUniformLocation("uModelViewProjection")
        mvLocation = shader.getUniformLocation("uModelView")
        normalMatrixLocation = shader.getUniformLocation("uNormalMatrix")
        sunDirLocation = shader.getUniformLocation("uSunDirection")
        sunColorLocation = shader.getUniformLocation("uSunColor")
        ambientLocation = shader.getUniformLocation("uAmbient")
        contrastLocation = shader.getUniformLocation("uContrast")
        fogStartLocation = shader.getUniformLocation("uFogStart")
        fogEndLocation = shader.getUniformLocation("uFogEnd")
        fogEnabledLocation = shader.getUniformLocation("uFogEnabled")
        fogColorLocation = shader.getUniformLocation("uFogColor")
        textureLocation = shader.getUniformLocation("uTexture")
        textureEnabledLocation = shader.getUniformLocation("uTextureEnabled")
        alphaThresholdLocation = shader.getUniformLocation("uAlphaThreshold")
        shader.unbind()
    }

    /**
     * Begins a new render frame.
     */
    fun begin(projection: Matrix4f, view: Matrix4f) {
        projectionMatrix.set(projection)
        viewMatrix.set(view)

        // Reset state
        currentTextureId = -1
        currentMaterialId = 0
        currentBatch = null
        nextBatchIndex = 0
        activeBatches.clear()

        // Clear all batches
        batches.forEach { it.clear() }

        // Reset statistics
        drawCallCount = 0
        triangleCount = 0
        stateChangeCount = 0

        // Bind shader and set common uniforms
        shader.bind()
        setLightingUniforms()
        setFogUniforms()

        // Set texture sampler to unit 0
        shader.setUniform(textureLocation, 0)
        shader.setUniform(alphaThresholdLocation, 0.0f)
    }

    /**
     * Sets the current texture for subsequent geometry.
     */
    fun setTexture(textureId: Int) {
        if (textureId != currentTextureId) {
            currentTextureId = textureId
            currentBatch = null // Force batch lookup on next add
        }
    }

    /**
     * Sets the current material for subsequent geometry.
     */
    fun setMaterial(materialId: Int) {
        if (materialId != currentMaterialId) {
            currentMaterialId = materialId
            currentBatch = null
        }
    }

    /**
     * Sets lighting parameters.
     */
    fun setLighting(
        direction: Vector3f,
        color: Vector3f = Vector3f(1f, 1f, 1f),
        ambient: Float = 0.3f,
        contrast: Float = 0.7f
    ) {
        sunDirection.set(direction).normalize()
        sunColor.set(color)
        ambientIntensity = ambient
        contrastIntensity = contrast
    }

    /**
     * Configures fog settings.
     */
    fun setFog(enabled: Boolean, start: Float = 100f, end: Float = 500f, r: Float = 0.7f, g: Float = 0.8f, b: Float = 0.9f) {
        fogEnabled = enabled
        fogStart = start
        fogEnd = end
        fogColor[0] = r
        fogColor[1] = g
        fogColor[2] = b
    }

    /**
     * Gets or creates a batch for the current texture/material state.
     */
    private fun getOrCreateBatch(): DrawBatch {
        currentBatch?.let { return it }

        // Create batch key from texture and material
        val key = (currentTextureId.toLong() shl 32) or currentMaterialId.toLong()

        val batch = activeBatches.getOrPut(key) {
            if (nextBatchIndex >= batches.size) {
                // Flush existing batches to make room
                flush()
            }
            val newBatch = batches[nextBatchIndex++]
            newBatch.configure(currentTextureId, currentMaterialId)
            newBatch
        }

        currentBatch = batch
        return batch
    }

    /**
     * Adds a vertex to the current batch.
     *
     * @return The vertex index for use in triangle indices
     */
    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Int, g: Int, b: Int, a: Int
    ): Int {
        return getOrCreateBatch().addVertex(x, y, z, nx, ny, nz, u, v, r, g, b, a)
    }

    /**
     * Adds a triangle to the current batch.
     */
    fun addTriangle(a: Int, b: Int, c: Int) {
        getOrCreateBatch().addTriangle(a, b, c)
    }

    /**
     * Adds a quad to the current batch.
     */
    fun addQuad(bl: Int, br: Int, tr: Int, tl: Int) {
        getOrCreateBatch().addQuad(bl, br, tr, tl)
    }

    /**
     * Flushes all pending batches.
     */
    fun flush() {
        // Sort batches by texture to minimize state changes
        val sortedBatches = activeBatches.values
            .filter { !it.isEmpty() }
            .sortedBy { it.textureId }

        var lastTextureId = -1

        for (batch in sortedBatches) {
            // Bind texture if changed
            if (batch.textureId != lastTextureId) {
                lastTextureId = batch.textureId
                if (batch.textureId > 0) {
                    GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                    GLWrapper.bindTexture(TEXTURE_2D, batch.textureId)
                    shader.setUniform(textureEnabledLocation, 1)
                } else {
                    shader.setUniform(textureEnabledLocation, 0)
                }
                stateChangeCount++
            }

            // Upload and render batch
            batch.upload()
            batch.render()

            // Update statistics
            drawCallCount++
            triangleCount += batch.getIndexCount() / 3
        }

        // Clear for next frame
        activeBatches.clear()
        nextBatchIndex = 0
        currentBatch = null
        batches.forEach { it.clear() }
    }

    /**
     * Renders a model with a specific transform.
     *
     * @param modelMatrix Transform for the model
     */
    fun setModelTransform(modelMatrix: Matrix4f) {
        // Calculate model-view and model-view-projection matrices
        viewMatrix.mul(modelMatrix, modelViewMatrix)
        projectionMatrix.mul(modelViewMatrix, modelViewProjectionMatrix)

        // Extract normal matrix (inverse transpose of model-view)
        modelViewMatrix.normal(normalMatrix)

        // Set uniforms
        shader.setUniformMatrix4(mvpLocation, modelViewProjectionMatrix)
        shader.setUniformMatrix4(mvLocation, modelViewMatrix)
        shader.setUniform("uNormalMatrix", normalMatrix)
    }

    /**
     * Ends the render frame and flushes all batches.
     */
    fun end() {
        flush()
        shader.unbind()
    }

    /**
     * Gets rendering statistics.
     */
    fun getStats(): RenderStats {
        return RenderStats(drawCallCount, triangleCount, stateChangeCount)
    }

    /**
     * Releases GPU resources.
     */
    fun destroy() {
        batches.forEach { it.destroy() }
    }

    private fun setLightingUniforms() {
        shader.setUniform(sunDirLocation, sunDirection.x, sunDirection.y, sunDirection.z)
        shader.setUniform("uSunColor", sunColor)
        shader.setUniform(ambientLocation, ambientIntensity)
        shader.setUniform(contrastLocation, contrastIntensity)
    }

    private fun setFogUniforms() {
        shader.setUniform(fogEnabledLocation, if (fogEnabled) 1 else 0)
        shader.setUniform(fogStartLocation, fogStart)
        shader.setUniform(fogEndLocation, fogEnd)
        shader.setUniform(fogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])
    }

    data class RenderStats(
        val drawCalls: Int,
        val triangles: Int,
        val stateChanges: Int
    )
}
