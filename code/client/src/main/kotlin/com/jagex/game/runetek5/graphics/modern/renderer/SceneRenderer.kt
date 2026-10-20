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

class SceneRenderer(
    private val materialProvider: MaterialProvider? = null
) {
    private lateinit var renderState: RenderState
    private lateinit var textureManager: TextureManager
    private lateinit var materialAdapter: MaterialAdapter
    private lateinit var modelRenderer: ModernModelRenderer
    private lateinit var groundRenderer: ModernGroundRenderer

    private val projection = Matrix4f()
    private val view = Matrix4f()
    private val viewProjection = Matrix4f()
    private val shadowMatrix = Matrix4f()

    private val sunDirection = Vector3f(0.5f, 1f, 0.3f).normalize()
    private val sunColor = Vector3f(1f, 1f, 1f)
    private var ambientStrength = 0.3f
    private var lightingContrast = 0.7f

    private var fogEnabled = false
    private var fogStart = 0f
    private var fogEnd = 1000f
    private val fogColor = Vector3f(0.7f, 0.8f, 0.9f)

    private var isInitialized = false
    private var currentPass = RenderPass.NONE
    private var frameStats = FrameStats()

    private val opaqueQueue = mutableListOf<DrawCommand>()
    private val transparentQueue = mutableListOf<DrawCommand>()

    private var shadowMapTexture = 0
    private var shadowMapEnabled = false

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

    fun beginFrame(projection: Matrix4f, view: Matrix4f) {
        if (!isInitialized) return

        this.projection.set(projection)
        this.view.set(view)
        this.viewProjection.set(projection).mul(view)

        frameStats = FrameStats()

        opaqueQueue.clear()
        transparentQueue.clear()

        renderState.reset()
        renderState.setDepthTest(true)
        renderState.setDepthWrite(true)
        renderState.setCullFace(CullFace.BACK)

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

    fun endFrame() {
        if (!isInitialized) return

        modelRenderer.endFrame()
        groundRenderer.endFrame()

        frameStats.modelStats = modelRenderer.getFrameStats()
        frameStats.groundStats = groundRenderer.getFrameStats()

        currentPass = RenderPass.NONE
    }

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

    fun beginOpaque() {
        currentPass = RenderPass.OPAQUE
        renderState.setOpaqueState()
    }

    fun endOpaque() {
        currentPass = RenderPass.NONE
    }

    fun beginTransparent() {
        currentPass = RenderPass.TRANSPARENT
        renderState.setTransparentState()
    }

    fun endTransparent() {
        currentPass = RenderPass.NONE
    }

    fun drawModel(
        vertices: ByteBuffer,
        indices: java.nio.ShortBuffer,
        indexCount: Int,
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
            } else {
                modelRenderer.bindTexture(0)
            }
        } else {
            modelRenderer.bindTexture(0)
        }

        modelRenderer.drawIndexed(vertices, indices, indexCount)

        frameStats.modelDrawCalls++
        frameStats.triangles += indexCount / 3
    }

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

    fun enableShadowMap(shadowMatrix: Matrix4f, shadowTexture: Int) {
        this.shadowMatrix.set(shadowMatrix)
        this.shadowMapTexture = shadowTexture
        this.shadowMapEnabled = true
        groundRenderer.setShadowMapping(true, shadowTexture, shadowMatrix)
    }

    fun disableShadowMap() {
        shadowMapEnabled = false
    }

    fun registerTexture(materialId: Int, glTextureId: Int, width: Int, height: Int) {
        materialAdapter.registerExternalTexture(materialId, glTextureId, width, height)
    }

    fun getTextureManager(): TextureManager = textureManager

    fun getRenderState(): RenderState = renderState

    fun getFrameStats(): FrameStats = frameStats

    fun isInitialized(): Boolean = isInitialized

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

enum class RenderPass {
    NONE,
    SHADOW,
    OPAQUE,
    TRANSPARENT,
    OVERLAY
}

data class DrawCommand(
    val vertices: ByteBuffer,
    val indices: java.nio.ShortBuffer,
    val indexCount: Int,
    val modelMatrix: Matrix4f,
    val materialId: Int,
    val depth: Float
)

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
