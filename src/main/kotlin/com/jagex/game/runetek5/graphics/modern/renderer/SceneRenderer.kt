package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.graphics.modern.material.MaterialAdapter
import com.jagex.game.runetek5.graphics.modern.material.MaterialUniforms
import com.jagex.game.runetek5.graphics.modern.state.BlendMode
import com.jagex.game.runetek5.graphics.modern.state.CullFace
import com.jagex.game.runetek5.graphics.modern.state.DepthFunc
import com.jagex.game.runetek5.graphics.modern.state.RenderState
import com.jagex.game.runetek5.graphics.modern.texture.TextureManager
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import org.joml.Matrix4f
import org.joml.Vector3f
import java.nio.ByteBuffer

/**
 * Unified scene renderer that coordinates all modern rendering components.
 *
 * This coordinator manages the entire rendering pipeline, including:
 * - Render state management
 * - Shader and material binding
 * - Multi-pass rendering (opaque, transparent, overlay)
 * - Draw call batching and sorting
 * - Camera and lighting setup
 *
 * Usage:
 * ```kotlin
 * val renderer = SceneRenderer(materialProvider)
 * renderer.initialize()
 *
 * // Each frame:
 * renderer.beginFrame(projection, view)
 * renderer.setLighting(sunDir, sunColor, ambient)
 * renderer.setFog(fogParams)
 *
 * // Draw geometry
 * renderer.beginOpaque()
 * renderer.drawModel(model, transform)
 * renderer.drawGround(ground)
 * renderer.endOpaque()
 *
 * renderer.beginTransparent()
 * renderer.drawModel(transparentModel, transform)
 * renderer.endTransparent()
 *
 * renderer.endFrame()
 * ```
 */
class SceneRenderer(
    private val materialProvider: MaterialProvider? = null
) {
    // Core components
    private lateinit var renderState: RenderState
    private lateinit var textureManager: TextureManager
    private lateinit var materialAdapter: MaterialAdapter
    private lateinit var modelRenderer: ModernModelRenderer
    private lateinit var groundRenderer: ModernGroundRenderer

    // Matrices
    private val projection = Matrix4f()
    private val view = Matrix4f()
    private val viewProjection = Matrix4f()
    private val shadowMatrix = Matrix4f()

    // Lighting
    private val sunDirection = Vector3f(0.5f, 1f, 0.3f).normalize()
    private val sunColor = Vector3f(1f, 1f, 1f)
    private var ambientStrength = 0.3f
    private var lightingContrast = 0.7f

    // Fog
    private var fogEnabled = false
    private var fogStart = 0f
    private var fogEnd = 1000f
    private val fogColor = Vector3f(0.7f, 0.8f, 0.9f)

    // Rendering state
    private var isInitialized = false
    private var currentPass = RenderPass.NONE
    private var frameStats = FrameStats()

    // Deferred rendering queues
    private val opaqueQueue = mutableListOf<DrawCommand>()
    private val transparentQueue = mutableListOf<DrawCommand>()

    // Shadow mapping (placeholder for now)
    private var shadowMapTexture = 0
    private var shadowMapEnabled = false

    /**
     * Initializes all rendering components.
     */
    fun initialize() {
        if (isInitialized) return

        renderState = RenderState()
        textureManager = TextureManager()
        materialAdapter = MaterialAdapter(materialProvider, textureManager, renderState)
        modelRenderer = ModernModelRenderer()
        groundRenderer = ModernGroundRenderer()

        modelRenderer.initialize()
        groundRenderer.initialize()

        isInitialized = true
        println("[SceneRenderer] Initialized")
    }

    /**
     * Begins a new frame with the given camera matrices.
     */
    fun beginFrame(projection: Matrix4f, view: Matrix4f) {
        if (!isInitialized) return

        this.projection.set(projection)
        this.view.set(view)
        this.viewProjection.set(projection).mul(view)

        // Reset frame stats
        frameStats = FrameStats()

        // Clear render queues
        opaqueQueue.clear()
        transparentQueue.clear()

        // Reset render state
        renderState.reset()
        renderState.setDepthTest(true)
        renderState.setDepthWrite(true)
        renderState.setCullFace(CullFace.BACK)

        // Set up renderers
        modelRenderer.beginFrame()
        modelRenderer.setProjection(projection)
        modelRenderer.setView(view)
        modelRenderer.setLighting(sunDirection, sunColor, ambientStrength, lightingContrast)
        modelRenderer.setFog(fogEnabled, fogStart, fogEnd, fogColor.x, fogColor.y, fogColor.z)

        groundRenderer.beginFrame()
        groundRenderer.setProjection(projection)
        groundRenderer.setView(view)
        groundRenderer.setLighting(sunDirection, sunColor, ambientStrength, lightingContrast)
        groundRenderer.setFog(fogEnabled, fogStart, fogEnd, fogColor.x, fogColor.y, fogColor.z)
        if (shadowMapEnabled) {
            groundRenderer.setShadowMapping(true, shadowMapTexture, shadowMatrix)
        }

        currentPass = RenderPass.NONE
    }

    /**
     * Ends the current frame and flushes all pending draw calls.
     */
    fun endFrame() {
        if (!isInitialized) return

        modelRenderer.endFrame()
        groundRenderer.endFrame()

        // Accumulate stats
        frameStats.modelStats = modelRenderer.getFrameStats()
        frameStats.groundStats = groundRenderer.getFrameStats()

        currentPass = RenderPass.NONE
    }

    // ===== Lighting Setup =====

    /**
     * Sets the sun/directional light parameters.
     */
    fun setLighting(direction: Vector3f, color: Vector3f, ambient: Float, contrast: Float = 0.7f) {
        sunDirection.set(direction).normalize()
        sunColor.set(color)
        ambientStrength = ambient
        lightingContrast = contrast

        if (isInitialized) {
            modelRenderer.setLighting(sunDirection, sunColor, ambientStrength, lightingContrast)
            groundRenderer.setLighting(sunDirection, sunColor, ambientStrength, lightingContrast)
        }
    }

    /**
     * Sets fog parameters.
     */
    fun setFog(enabled: Boolean, start: Float, end: Float, r: Float, g: Float, b: Float) {
        fogEnabled = enabled
        fogStart = start
        fogEnd = end
        fogColor.set(r, g, b)

        if (isInitialized) {
            modelRenderer.setFog(enabled, start, end, r, g, b)
            groundRenderer.setFog(enabled, start, end, r, g, b)
        }
    }

    // ===== Opaque Pass =====

    /**
     * Begins the opaque rendering pass.
     */
    fun beginOpaque() {
        currentPass = RenderPass.OPAQUE
        renderState.setOpaqueState()
    }

    /**
     * Ends the opaque rendering pass.
     */
    fun endOpaque() {
        currentPass = RenderPass.NONE
    }

    // ===== Transparent Pass =====

    /**
     * Begins the transparent rendering pass.
     */
    fun beginTransparent() {
        currentPass = RenderPass.TRANSPARENT
        renderState.setTransparentState()
    }

    /**
     * Ends the transparent rendering pass.
     */
    fun endTransparent() {
        currentPass = RenderPass.NONE
    }

    // ===== Model Drawing =====

    /**
     * Draws a model with the given transform.
     *
     * @param vertices Interleaved vertex data (position, normal, texcoord, color)
     * @param indices Index buffer for triangles
     * @param indexCount Number of indices to draw
     * @param modelMatrix Model transformation matrix
     * @param materialId Material/texture ID for the model
     */
    fun drawModel(
        vertices: ByteBuffer,
        indices: java.nio.ShortBuffer,
        indexCount: Int,
        modelMatrix: Matrix4f,
        materialId: Int = -1
    ) {
        if (!isInitialized) return

        // Bind material and get uniforms
        val uniforms = if (materialId >= 0) {
            materialAdapter.bindMaterial(materialId)
        } else {
            MaterialUniforms.DEFAULT
        }

        // Skip if material says not to render
        if (!uniforms.shouldRender) return

        // Set model matrix
        modelRenderer.setModelMatrix(modelMatrix)

        // Bind texture if available
        if (uniforms.hasTexture) {
            val texHandle = textureManager.getTextureByName("material_$materialId")
            if (texHandle > 0) {
                modelRenderer.bindTexture(textureManager.getGLId(texHandle))
            } else {
                modelRenderer.bindTexture(0)
            }
        } else {
            modelRenderer.bindTexture(0)
        }

        // Draw the model
        modelRenderer.drawIndexed(vertices, indices, indexCount)

        frameStats.modelDrawCalls++
        frameStats.triangles += indexCount / 3
    }

    /**
     * Draws a model using separate arrays (for legacy compatibility).
     */
    fun drawModelArrays(
        positions: FloatArray,
        normals: FloatArray?,
        texCoords: FloatArray?,
        colors: ByteArray?,
        indices: ShortArray,
        modelMatrix: Matrix4f,
        materialId: Int = -1
    ) {
        if (!isInitialized) return

        val uniforms = if (materialId >= 0) {
            materialAdapter.bindMaterial(materialId)
        } else {
            MaterialUniforms.DEFAULT
        }

        if (!uniforms.shouldRender) return

        modelRenderer.setModelMatrix(modelMatrix)

        if (uniforms.hasTexture) {
            val texHandle = textureManager.getTextureByName("material_$materialId")
            if (texHandle > 0) {
                modelRenderer.bindTexture(textureManager.getGLId(texHandle))
            }
        } else {
            modelRenderer.bindTexture(0)
        }

        val vertexCount = positions.size / 3
        modelRenderer.drawWithSeparateArrays(positions, normals, texCoords, colors, indices, vertexCount, indices.size)

        frameStats.modelDrawCalls++
        frameStats.triangles += indices.size / 3
    }

    // ===== Ground Drawing =====

    /**
     * Draws ground geometry.
     *
     * @param vertices Interleaved vertex data (position, normal, color)
     * @param indices Index buffer for triangles
     * @param indexCount Number of indices to draw
     */
    fun drawGround(
        vertices: ByteBuffer,
        indices: java.nio.ShortBuffer,
        indexCount: Int
    ) {
        if (!isInitialized) return

        groundRenderer.drawIndexed(vertices, indices, indexCount)

        frameStats.groundDrawCalls++
        frameStats.triangles += indexCount / 3
    }

    /**
     * Draws ground geometry using separate arrays.
     */
    fun drawGroundArrays(
        positions: FloatArray,
        normals: FloatArray,
        colors: ByteArray,
        indices: ShortArray
    ) {
        if (!isInitialized) return

        val vertexCount = positions.size / 3
        groundRenderer.drawTile(positions, normals, colors, indices, vertexCount, indices.size)

        frameStats.groundDrawCalls++
        frameStats.triangles += indices.size / 3
    }

    // ===== Shadow Mapping =====

    /**
     * Enables shadow mapping with the given shadow matrix and texture.
     */
    fun enableShadowMap(shadowMatrix: Matrix4f, shadowTexture: Int) {
        this.shadowMatrix.set(shadowMatrix)
        this.shadowMapTexture = shadowTexture
        this.shadowMapEnabled = true
        groundRenderer.setShadowMapping(true, shadowTexture, shadowMatrix)
    }

    /**
     * Disables shadow mapping.
     */
    fun disableShadowMap() {
        shadowMapEnabled = false
    }

    // ===== Texture Management =====

    /**
     * Registers an external OpenGL texture for use with materials.
     */
    fun registerTexture(materialId: Int, glTextureId: Int, width: Int, height: Int) {
        materialAdapter.registerExternalTexture(materialId, glTextureId, width, height)
    }

    /**
     * Gets the texture manager for direct texture operations.
     */
    fun getTextureManager(): TextureManager = textureManager

    // ===== State Access =====

    /**
     * Gets the render state manager for advanced state manipulation.
     */
    fun getRenderState(): RenderState = renderState

    /**
     * Gets the current frame statistics.
     */
    fun getFrameStats(): FrameStats = frameStats

    /**
     * Checks if the renderer is initialized.
     */
    fun isInitialized(): Boolean = isInitialized

    // ===== Cleanup =====

    /**
     * Releases all resources.
     */
    fun destroy() {
        if (!isInitialized) return

        materialAdapter.clearCache()
        textureManager.deleteAll()
        modelRenderer.destroy()
        groundRenderer.destroy()

        isInitialized = false
        println("[SceneRenderer] Destroyed")
    }
}

/**
 * Render pass types.
 */
enum class RenderPass {
    NONE,
    SHADOW,
    OPAQUE,
    TRANSPARENT,
    OVERLAY
}

/**
 * Draw command for deferred rendering.
 */
data class DrawCommand(
    val vertices: ByteBuffer,
    val indices: java.nio.ShortBuffer,
    val indexCount: Int,
    val modelMatrix: Matrix4f,
    val materialId: Int,
    val depth: Float  // For sorting
)

/**
 * Per-frame rendering statistics.
 */
data class FrameStats(
    var modelDrawCalls: Int = 0,
    var groundDrawCalls: Int = 0,
    var triangles: Int = 0,
    var stateChanges: Int = 0,
    var textureBinds: Int = 0,
    var modelStats: ModernModelRenderer.FrameStats? = null,
    var groundStats: ModernGroundRenderer.FrameStats? = null
) {
    val totalDrawCalls: Int get() = modelDrawCalls + groundDrawCalls

    override fun toString(): String {
        return "FrameStats(draws=$totalDrawCalls, tris=$triangles, states=$stateChanges, texBinds=$textureBinds)"
    }
}
