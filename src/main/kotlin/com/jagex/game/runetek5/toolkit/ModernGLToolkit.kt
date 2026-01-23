package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.graphics.modern.renderer.ModernModelRenderer
import com.jagex.game.runetek5.graphics.modern.util.MeshConverter
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.scene.Scene
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL33.*
import java.awt.Canvas

/**
 * Modern OpenGL 3.3 Core toolkit for rendering.
 *
 * This toolkit extends OpenGlToolkit to provide a modern shader-based
 * rendering pipeline for 3D scene content, while reusing the existing
 * 2D/UI rendering from the parent class.
 *
 * Key features:
 * - Shader-based rendering (no fixed-function pipeline)
 * - VAO/VBO for vertex management
 * - Proper lighting and fog through uniforms
 * - Compatible with the existing scene architecture
 */
class ModernGLToolkit(
    canvas: Canvas,
    materials: MaterialProvider,
    multiSamples: Int
) : OpenGlToolkit(canvas, materials, multiSamples) {

    // Modern renderer for 3D models
    private var modernRenderer: ModernModelRenderer? = null
    private var isModernRendererInitialized = false

    // Matrices
    private val projectionMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val modelMat = Matrix4f()

    // Camera state from toolkit
    private var camX = 0f
    private var camY = 0f
    private var camZ = 0f
    private var camYaw = 0f
    private var camPitch = 0f

    // Fog settings
    private var modernFogEnabled = true
    private var modernFogStart = 1000f
    private var modernFogEnd = 5000f
    private var modernFogR = 0.7f
    private var modernFogG = 0.8f
    private var modernFogB = 0.9f

    // Debug/stats
    private var frameCounter = 0

    companion object {
        /**
         * Creates a modern GL toolkit with the specified settings.
         */
        @JvmStatic
        fun createToolkit(
            canvas: Canvas?,
            materials: MaterialProvider?,
            multiSamples: Int
        ): ModernGLToolkit {
            requireNotNull(canvas) { "Canvas cannot be null" }
            requireNotNull(materials) { "MaterialProvider cannot be null" }
            return ModernGLToolkit(canvas, materials, multiSamples)
        }
    }

    override fun initialize() {
        super.initialize()
        // Modern renderer initialization will happen on first render
        // to ensure we're on the GL thread
    }

    private fun initializeModernRenderer() {
        if (isModernRendererInitialized) return

        try {
            modernRenderer = ModernModelRenderer()
            modernRenderer!!.initialize()
            isModernRendererInitialized = true
            println("[ModernGLToolkit] Modern renderer initialized successfully")
        } catch (e: Exception) {
            println("[ModernGLToolkit] Failed to initialize modern renderer: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun render() {
        // Initialize modern renderer on first render (GL thread)
        if (!isModernRendererInitialized) {
            initializeModernRenderer()
        }

        // Use modern renderer if available
        if (isModernRendererInitialized && modernRenderer != null) {
            try {
                renderWithModernPipeline()
            } catch (e: Exception) {
                println("[ModernGLToolkit] Modern render failed, falling back: ${e.message}")
                super.render()
            }
        } else {
            // Fallback to legacy rendering
            super.render()
        }
    }

    private fun renderWithModernPipeline() {
        val mr = modernRenderer ?: return

        // Get display dimensions
        val display = delegatingDisplay ?: return
        val width = display.getWidth()
        val height = display.getHeight()
        if (width <= 0 || height <= 0) return

        // Setup projection
        // Note: Flip Y-axis to match legacy renderer coordinate system
        val fov = Math.toRadians(60.0).toFloat()
        val aspect = width.toFloat() / height.toFloat()
        projectionMatrix.identity().perspective(fov, aspect, 10f, 10000f)
        projectionMatrix.scale(1f, -1f, 1f)  // Flip Y to match legacy

        // Get camera from scene/client state
        updateCameraFromClient()

        // Setup view matrix
        viewMatrix.identity()
        viewMatrix.rotateX(-camPitch)
        viewMatrix.rotateY(-camYaw)
        viewMatrix.translate(-camX, -camY, -camZ)

        // Configure renderer
        mr.setProjection(projectionMatrix)
        mr.setView(viewMatrix)

        // Set lighting from toolkit state
        val lightDir = Vector3f(sunDirection[0], sunDirection[1], sunDirection[2])
        if (lightDir.lengthSquared() < 0.001f) {
            lightDir.set(0.5f, 1f, 0.3f)
        }
        lightDir.normalize()
        mr.setLighting(lightDir, Vector3f(sunColorRed, sunColorGreen, sunColorBlue), 0.4f, 0.6f)

        // Set fog
        if (fogDensity > 0) {
            mr.setFog(true, fogMinimumStartPosition, fogMinimumStartPosition + 3000f,
                modernFogR, modernFogG, modernFogB)
        } else {
            mr.setFog(false, 0f, 0f, 0f, 0f, 0f)
        }

        // Since Y is flipped, change winding order for correct culling
        glFrontFace(GL_CW)

        // Begin frame
        mr.beginFrame()

        // Render scene content
        renderSceneWithModernPipeline(mr)

        // End frame
        mr.endFrame()

        // Restore winding order for 2D/UI rendering
        glFrontFace(GL_CCW)

        // Still need to call the delegating display for 2D/UI
        try {
            delegatingDisplay?.draw()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        materialProvider?.cycle()

        frameCounter++
        if (frameCounter % 300 == 0) {
            val stats = mr.getFrameStats()
            println("[ModernGLToolkit] Frame $frameCounter: ${stats.drawCalls} draws, ${stats.triangles} tris")
        }
    }

    private fun updateCameraFromClient() {
        try {
            // Try to get camera position from scene
            val mapRegion = Resource.mapRegion
            val scene = mapRegion?.getScene()

            if (scene != null) {
                // Scene stores camera position
                camX = scene.cameraPositionX.toFloat()
                camY = scene.cameraPositionY.toFloat()
                camZ = scene.cameraPositionZ.toFloat()
                // Camera rotation is handled by the rendering matrices in Scene
                // For now, use default orientation - full integration requires
                // extracting rotation from the scene's matrix state
            }
        } catch (e: Exception) {
            // Use default camera if we can't get scene camera
        }
    }

    private fun renderSceneWithModernPipeline(renderer: ModernModelRenderer) {
        // Get scene
        val mapRegion = Resource.mapRegion ?: return
        val scene = mapRegion.getScene() ?: return

        // Render ground meshes from localHeightmap
        renderGroundMeshes(renderer, scene)

        // TODO: Render scene objects (models, etc.)
        // This would iterate through scene entities and render their meshes
        // Full integration requires hooking into the existing draw callbacks
    }

    private fun renderGroundMeshes(renderer: ModernModelRenderer, scene: Scene) {
        // Get the ground heightmaps from scene
        val heightmaps = scene.localHeightmap
        if (heightmaps.isEmpty()) return

        // For each level, render ground
        for (level in heightmaps.indices) {
            val ground = heightmaps[level] ?: continue

            // The ground mesh would need to be converted and rendered
            // Ground class stores tile geometry - full integration requires
            // converting GroundGL vertex data to modern format
            // This is a placeholder for the actual ground rendering
        }
    }

    override fun createModel(rsmesh1: Mesh, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Model {
        // Create a ModelGL that can also be used with modern pipeline
        // For now, use the parent implementation
        // In the future, we could create a ModernModel that works with both pipelines
        return super.createModel(rsmesh1, i_2, i_3, i_4, i_5)
    }

    /**
     * Renders a mesh using the modern pipeline.
     * This can be called directly for testing or from the rendering system.
     */
    fun renderMeshModern(mesh: Mesh, x: Float, y: Float, z: Float, rotationY: Float = 0f) {
        val mr = modernRenderer ?: return

        // Convert mesh to modern format
        val converted = MeshConverter.convert(mesh) ?: return

        try {
            // Set model matrix
            modelMat.identity()
            modelMat.translate(x, y, z)
            modelMat.rotateY(rotationY)
            mr.setModelMatrix(modelMat)

            // Draw
            mr.drawIndexed(converted.vertexData, converted.indexData, converted.indexCount)
        } finally {
            converted.free()
        }
    }

    override fun shutdown() {
        modernRenderer?.destroy()
        modernRenderer = null
        isModernRendererInitialized = false
        super.shutdown()
    }

    /**
     * Checks if the modern renderer is active and working.
     */
    fun isModernRenderingActive(): Boolean {
        return isModernRendererInitialized && modernRenderer != null
    }

    /**
     * Gets statistics from the modern renderer.
     */
    fun getModernRendererStats(): ModernModelRenderer.FrameStats? {
        return modernRenderer?.getFrameStats()
    }
}
