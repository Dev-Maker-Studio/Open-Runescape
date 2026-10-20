package com.jagex.game.runetek5.graphics.modern.batch

import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f

class BatchRenderer(
    private val shader: ShaderProgram,
    maxBatches: Int = 64
) {
    private val batches = Array(maxBatches) { DrawBatch() }
    private val activeBatches = mutableMapOf<Long, DrawBatch>()
    private var nextBatchIndex = 0

    private var currentTextureId = -1
    private var currentMaterialId = 0
    private var currentBatch: DrawBatch? = null

    private val projectionMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val modelViewMatrix = Matrix4f()
    private val modelViewProjectionMatrix = Matrix4f()
    private val normalMatrix = Matrix3f()

    private val sunDirection = Vector3f(0.0f, 1.0f, 0.0f)
    private val sunColor = Vector3f(1.0f, 1.0f, 1.0f)
    private var ambientIntensity = 0.3f
    private var contrastIntensity = 0.7f

    private var fogEnabled = false
    private var fogStart = 100.0f
    private var fogEnd = 500.0f
    private val fogColor = floatArrayOf(0.7f, 0.8f, 0.9f, 1.0f)

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

    fun begin(projection: Matrix4f, view: Matrix4f) {
        projectionMatrix.set(projection)
        viewMatrix.set(view)

        currentTextureId = -1
        currentMaterialId = 0
        currentBatch = null
        nextBatchIndex = 0
        activeBatches.clear()

        batches.forEach { it.clear() }

        drawCallCount = 0
        triangleCount = 0
        stateChangeCount = 0

        shader.bind()
        setLightingUniforms()
        setFogUniforms()

        shader.setUniform(textureLocation, 0)
        shader.setUniform(alphaThresholdLocation, 0.0f)
    }

    fun setTexture(textureId: Int) {
        if (textureId != currentTextureId) {
            currentTextureId = textureId
            currentBatch = null
        }
    }

    fun setMaterial(materialId: Int) {
        if (materialId != currentMaterialId) {
            currentMaterialId = materialId
            currentBatch = null
        }
    }

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

    fun setFog(enabled: Boolean, start: Float = 100f, end: Float = 500f, r: Float = 0.7f, g: Float = 0.8f, b: Float = 0.9f) {
        fogEnabled = enabled
        fogStart = start
        fogEnd = end
        fogColor[0] = r
        fogColor[1] = g
        fogColor[2] = b
    }

    private fun getOrCreateBatch(): DrawBatch {
        currentBatch?.let { return it }

        val key = (currentTextureId.toLong() shl 32) or currentMaterialId.toLong()

        val batch = activeBatches.getOrPut(key) {
            if (nextBatchIndex >= batches.size) {
                flush()
            }
            val newBatch = batches[nextBatchIndex++]
            newBatch.configure(currentTextureId, currentMaterialId)
            newBatch
        }

        currentBatch = batch
        return batch
    }

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Int, g: Int, b: Int, a: Int
    ): Int {
        return getOrCreateBatch().addVertex(x, y, z, nx, ny, nz, u, v, r, g, b, a)
    }

    fun addTriangle(a: Int, b: Int, c: Int) {
        getOrCreateBatch().addTriangle(a, b, c)
    }

    fun addQuad(bl: Int, br: Int, tr: Int, tl: Int) {
        getOrCreateBatch().addQuad(bl, br, tr, tl)
    }

    fun flush() {
        val sortedBatches = activeBatches.values
            .filter { !it.isEmpty() }
            .sortedBy { it.textureId }

        var lastTextureId = -1

        for (batch in sortedBatches) {
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

            batch.upload()
            batch.render()

            drawCallCount++
            triangleCount += batch.getIndexCount() / 3
        }

        activeBatches.clear()
        nextBatchIndex = 0
        currentBatch = null
        batches.forEach { it.clear() }
    }

    fun setModelTransform(modelMatrix: Matrix4f) {
        viewMatrix.mul(modelMatrix, modelViewMatrix)
        projectionMatrix.mul(modelViewMatrix, modelViewProjectionMatrix)

        modelViewMatrix.normal(normalMatrix)

        shader.setUniformMatrix4(mvpLocation, modelViewProjectionMatrix)
        shader.setUniformMatrix4(mvLocation, modelViewMatrix)
        shader.setUniform("uNormalMatrix", normalMatrix)
    }

    fun end() {
        flush()
        shader.unbind()
    }

    fun getStats(): RenderStats {
        return RenderStats(drawCallCount, triangleCount, stateChangeCount)
    }

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
