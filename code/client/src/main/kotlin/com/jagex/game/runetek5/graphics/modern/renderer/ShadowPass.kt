package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.framebuffer.Framebuffer
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL33.*

/**
 * Shadow mapping pass for the modern renderer.
 *
 * Renders the scene depth from the sun's perspective into a depth texture,
 * which is then sampled by ground and model fragment shaders to apply shadows.
 *
 * Architecture:
 * 1. Before the main scene pass, ShadowPass binds its depth-only FBO
 * 2. The scene is rendered using the shadow shader (depth output only)
 * 3. The resulting depth texture is bound to a texture unit for the main pass
 * 4. Ground/model fragment shaders sample the depth texture with PCF for soft shadows
 *
 * The light-space matrix is an orthographic projection from the sun's perspective,
 * centered on the camera's position to keep the shadow map relevant to the visible scene.
 */
class ShadowPass {

    companion object {
        // Default shadow map resolution (configurable)
        const val DEFAULT_SHADOW_MAP_SIZE = 2048

        // Shadow texture unit -- ground shader samples from texture unit 1
        const val SHADOW_TEXTURE_UNIT = 1

        // Orthographic projection bounds for the light frustum.
        // These define how large an area the shadow map covers in world units.
        // Larger values cover more terrain but reduce shadow detail.
        private const val ORTHO_SIZE = 3000f

        // Near and far planes for the light's orthographic projection.
        // Must encompass the full depth range of shadow-casting geometry.
        private const val NEAR_PLANE = 1f
        private const val FAR_PLANE = 8000f

        // Height offset for the light camera position above the scene center.
        // Ensures the light "looks down" from sufficiently far away.
        private const val LIGHT_HEIGHT_OFFSET = 4000f
    }

    // Shadow FBO -- depth-only (no color attachment)
    private var shadowFbo: Framebuffer? = null
    private var shadowMapSize: Int = DEFAULT_SHADOW_MAP_SIZE

    // Shadow depth shader
    private var shadowShader: ShaderProgram? = null

    // Cached uniform locations
    private var uLightViewProjectionLocation = -1
    private var uModelMatrixLocation = -1
    private var uAlphaTestEnabledLocation = -1
    private var uTextureLocation = -1
    private var uAlphaThresholdLocation = -1

    // Light-space matrix: transforms world positions to light clip space
    // This is what gets passed to the ground/model shaders as uShadowMatrix
    private val lightViewProjectionMatrix = Matrix4f()

    // Component matrices (reused each frame to avoid allocation)
    private val lightViewMatrix = Matrix4f()
    private val lightProjectionMatrix = Matrix4f()

    // Temporary vectors (avoid per-frame allocation)
    private val lightPosition = Vector3f()
    private val lightTarget = Vector3f()
    private val lightUp = Vector3f()

    // State
    private var isInitialized = false
    private var isEnabled = true

    // Configuration
    private var shadowBias = 0.002f
    private var shadowIntensity = 0.4f  // 0.0 = pitch black shadows, 1.0 = no shadow effect

    // Saved viewport for restoration after shadow pass
    private val savedViewport = IntArray(4)

    /**
     * Initializes the shadow pass: creates the FBO and compiles the shadow shader.
     *
     * @param mapSize Shadow map resolution (width and height in pixels)
     */
    fun initialize(mapSize: Int = DEFAULT_SHADOW_MAP_SIZE) {
        if (isInitialized) return

        shadowMapSize = mapSize

        // Create depth-only FBO
        shadowFbo = Framebuffer(shadowMapSize, shadowMapSize)
        shadowFbo!!.addDepthTexture()

        if (!shadowFbo!!.create()) {
            println("[ShadowPass] Failed to create shadow FBO")
            return
        }

        // After FBO creation, configure it as depth-only (no color writes)
        GLWrapper.bindFramebuffer(GLWrapper.Constants.FRAMEBUFFER, shadowFbo!!.getId())
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
        GLWrapper.bindFramebuffer(GLWrapper.Constants.FRAMEBUFFER, 0)

        // Compile shadow shader
        try {
            shadowShader = ShaderProgram.createFromResources("/shaders/shadow.vert", "/shaders/shadow.frag")
        } catch (e: Exception) {
            println("[ShadowPass] Failed to compile shadow shader: ${e.message}")
            shadowFbo?.destroy()
            shadowFbo = null
            return
        }

        // Cache uniform locations
        val s = shadowShader!!
        s.bind()
        uLightViewProjectionLocation = s.getUniformLocation("uLightViewProjection")
        uModelMatrixLocation = s.getUniformLocation("uModelMatrix")
        uAlphaTestEnabledLocation = s.getUniformLocation("uAlphaTestEnabled")
        uTextureLocation = s.getUniformLocation("uTexture")
        uAlphaThresholdLocation = s.getUniformLocation("uAlphaThreshold")
        s.unbind()

        isInitialized = true
        println("[ShadowPass] Initialized (${shadowMapSize}x${shadowMapSize})")
    }

    /**
     * Computes the light-space view-projection matrix for this frame.
     *
     * The light camera uses an orthographic projection centered on the provided
     * scene center (typically the camera position), looking along the sun direction.
     *
     * @param sunDirection Normalized direction TO the sun (not FROM the sun)
     * @param sceneCenter Center of the shadow-receiving area (usually camera world position)
     */
    fun updateLightMatrix(sunDirection: Vector3f, sceneCenter: Vector3f) {
        if (!isInitialized) return

        // The light "looks" from a position offset along the sun direction toward the scene center.
        // For a directional light, the absolute position does not matter (orthographic projection),
        // but placing it above the scene center ensures correct depth ordering.
        lightPosition.set(sceneCenter).add(
            sunDirection.x * LIGHT_HEIGHT_OFFSET,
            sunDirection.y * LIGHT_HEIGHT_OFFSET,
            sunDirection.z * LIGHT_HEIGHT_OFFSET
        )
        lightTarget.set(sceneCenter)

        // Choose an up vector that is not parallel to the sun direction.
        // If the sun is nearly vertical, use the Z axis as up instead.
        if (Math.abs(sunDirection.y.toDouble()) > 0.99) {
            lightUp.set(0f, 0f, 1f)
        } else {
            lightUp.set(0f, 1f, 0f)
        }

        // Build the light view matrix (world -> light view space)
        lightViewMatrix.identity().lookAt(lightPosition, lightTarget, lightUp)

        // Build the orthographic projection (light view space -> light clip space).
        // The bounds are symmetric around the light's look-at point.
        lightProjectionMatrix.identity().ortho(
            -ORTHO_SIZE, ORTHO_SIZE,   // left, right
            -ORTHO_SIZE, ORTHO_SIZE,   // bottom, top
            NEAR_PLANE, FAR_PLANE      // near, far
        )

        // Combined light-space matrix: world position -> light clip space
        lightProjectionMatrix.mul(lightViewMatrix, lightViewProjectionMatrix)
    }

    /**
     * Begins the shadow rendering pass.
     *
     * Binds the shadow FBO, sets the viewport to shadow map size, clears depth,
     * and binds the shadow shader. After this call, render shadow-casting geometry
     * using [renderGeometry] or by drawing with the shadow shader directly.
     *
     * Call [endPass] when finished to restore the previous FBO and viewport.
     */
    fun beginPass() {
        if (!isInitialized || !isEnabled) return

        // Save current viewport for restoration
        glGetIntegerv(GL_VIEWPORT, savedViewport)

        // Bind shadow FBO and set viewport to shadow map dimensions
        shadowFbo!!.bind()

        // Clear the depth buffer
        GLWrapper.clear(GLWrapper.Constants.DEPTH_BUFFER_BIT)

        // Enable depth testing with standard less-than comparison
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthFunc(GLWrapper.Constants.LESS)
        GLWrapper.depthMask(true)

        // Disable color writing (depth-only pass)
        GLWrapper.colorMask(false, false, false, false)

        // Disable blending (not needed for depth-only pass)
        GLWrapper.disable(GLWrapper.Constants.BLEND)

        // Optional: cull front faces to reduce shadow acne on back faces.
        // This is a common technique where rendering the back faces into the
        // shadow map avoids self-shadowing artifacts on front-facing surfaces.
        GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
        GLWrapper.cullFace(GLWrapper.Constants.FRONT)

        // Bind shadow shader and set the light VP matrix
        val s = shadowShader!!
        s.bind()
        s.setUniformMatrix4(uLightViewProjectionLocation, lightViewProjectionMatrix)

        // Default: no alpha testing (opaque geometry)
        s.setUniform(uAlphaTestEnabledLocation, 0)
        s.setUniform(uAlphaThresholdLocation, 0.5f)
    }

    /**
     * Sets the model matrix for the next shadow geometry draw call.
     * For ground tiles, pass an identity matrix.
     *
     * @param modelMatrix The model-to-world transform
     */
    fun setModelMatrix(modelMatrix: Matrix4f) {
        shadowShader?.setUniformMatrix4(uModelMatrixLocation, modelMatrix)
    }

    /**
     * Enables alpha testing for the current geometry (e.g., foliage).
     * The bound texture will be sampled and fragments below the threshold discarded.
     */
    fun enableAlphaTest(textureUnit: Int = 0, threshold: Float = 0.5f) {
        val s = shadowShader ?: return
        s.setUniform(uAlphaTestEnabledLocation, 1)
        s.setUniform(uTextureLocation, textureUnit)
        s.setUniform(uAlphaThresholdLocation, threshold)
    }

    /**
     * Disables alpha testing (for opaque geometry).
     */
    fun disableAlphaTest() {
        shadowShader?.setUniform(uAlphaTestEnabledLocation, 0)
    }

    /**
     * Ends the shadow rendering pass.
     *
     * Unbinds the shadow FBO, restores the previous viewport, and re-enables
     * color writing. After this call, the shadow depth texture is ready for
     * sampling in the main scene pass.
     */
    fun endPass() {
        if (!isInitialized || !isEnabled) return

        // Unbind shadow shader
        shadowShader?.unbind()

        // Unbind FBO (return to default framebuffer)
        shadowFbo!!.unbind()

        // Restore viewport
        GLWrapper.viewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3])

        // Re-enable color writing
        GLWrapper.colorMask(true, true, true, true)

        // Restore default culling (back face culling or disabled, per scene needs)
        GLWrapper.cullFace(GLWrapper.Constants.BACK)
        GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
    }

    /**
     * Binds the shadow map depth texture to the specified texture unit
     * for sampling in the main scene pass.
     *
     * @param textureUnit The texture unit to bind to (default: SHADOW_TEXTURE_UNIT = 1)
     */
    fun bindShadowMap(textureUnit: Int = SHADOW_TEXTURE_UNIT) {
        if (!isInitialized || !isEnabled) return
        val depthTex = shadowFbo?.getDepthTexture() ?: return
        if (depthTex == 0) return

        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + textureUnit)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, depthTex)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
    }

    /**
     * Unbinds the shadow map texture.
     */
    fun unbindShadowMap(textureUnit: Int = SHADOW_TEXTURE_UNIT) {
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + textureUnit)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, 0)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
    }

    /**
     * Gets the light-space view-projection matrix for use as uShadowMatrix
     * in the ground/model shaders.
     */
    fun getLightViewProjectionMatrix(): Matrix4f = lightViewProjectionMatrix

    /**
     * Gets the shadow map depth texture ID.
     */
    fun getShadowMapTexture(): Int = shadowFbo?.getDepthTexture() ?: 0

    /**
     * Gets the shadow map resolution.
     */
    fun getShadowMapSize(): Int = shadowMapSize

    /**
     * Gets the shadow bias value.
     */
    fun getShadowBias(): Float = shadowBias

    /**
     * Gets the shadow intensity value.
     * 0.0 = pitch black shadows, 1.0 = no shadow effect.
     */
    fun getShadowIntensity(): Float = shadowIntensity

    /**
     * Sets the shadow bias to prevent shadow acne.
     * Typical values: 0.001 - 0.005.
     */
    fun setShadowBias(bias: Float) {
        shadowBias = bias
    }

    /**
     * Sets the shadow intensity (darkness of shadows).
     * 0.0 = pitch black shadows, 1.0 = no shadow effect.
     * Typical value: 0.3 - 0.5.
     */
    fun setShadowIntensity(intensity: Float) {
        shadowIntensity = intensity.coerceIn(0f, 1f)
    }

    /**
     * Enables or disables the shadow pass.
     * When disabled, no shadow rendering occurs and the shadow map is not bound.
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Returns whether the shadow pass is enabled.
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Returns whether the shadow pass is initialized and ready.
     */
    fun isReady(): Boolean = isInitialized && isEnabled

    /**
     * Resizes the shadow map. Destroys and recreates the FBO.
     */
    fun resize(newSize: Int) {
        if (newSize == shadowMapSize) return
        if (!isInitialized) {
            shadowMapSize = newSize
            return
        }

        shadowFbo?.destroy()
        shadowMapSize = newSize
        shadowFbo = Framebuffer(shadowMapSize, shadowMapSize)
        shadowFbo!!.addDepthTexture()

        if (!shadowFbo!!.create()) {
            println("[ShadowPass] Failed to recreate shadow FBO at ${shadowMapSize}x${shadowMapSize}")
            return
        }

        // Reconfigure as depth-only
        GLWrapper.bindFramebuffer(GLWrapper.Constants.FRAMEBUFFER, shadowFbo!!.getId())
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
        GLWrapper.bindFramebuffer(GLWrapper.Constants.FRAMEBUFFER, 0)

        println("[ShadowPass] Resized to ${shadowMapSize}x${shadowMapSize}")
    }

    /**
     * Returns debug information about the shadow pass state.
     */
    fun getDebugStatus(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "enabled" to isEnabled,
            "shadowMapSize" to shadowMapSize,
            "shadowBias" to shadowBias,
            "shadowIntensity" to shadowIntensity,
            "depthTextureId" to (shadowFbo?.getDepthTexture() ?: 0),
            "fboValid" to (shadowFbo?.isValid() ?: false)
        )
    }

    /**
     * Releases all GPU resources.
     */
    fun destroy() {
        shadowShader?.destroy()
        shadowShader = null

        shadowFbo?.destroy()
        shadowFbo = null

        isInitialized = false
    }
}
