package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.framebuffer.Framebuffer
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL33.*

class ShadowPass {

    companion object {
        const val DEFAULT_SHADOW_MAP_SIZE = 2048

        const val SHADOW_TEXTURE_UNIT = 1

        private const val ORTHO_SIZE = 3000f

        private const val NEAR_PLANE = 1f
        private const val FAR_PLANE = 8000f

        private const val LIGHT_HEIGHT_OFFSET = 4000f
    }

    private var shadowFbo: Framebuffer? = null
    private var shadowMapSize: Int = DEFAULT_SHADOW_MAP_SIZE

    private var shadowShader: ShaderProgram? = null

    private var uLightViewProjectionLocation = -1
    private var uModelMatrixLocation = -1
    private var uAlphaTestEnabledLocation = -1
    private var uTextureLocation = -1
    private var uAlphaThresholdLocation = -1

    private val lightViewProjectionMatrix = Matrix4f()

    private val lightViewMatrix = Matrix4f()
    private val lightProjectionMatrix = Matrix4f()

    private val lightPosition = Vector3f()
    private val lightTarget = Vector3f()
    private val lightUp = Vector3f()

    private var isInitialized = false
    private var isEnabled = true

    private var shadowBias = 0.002f
    private var shadowIntensity = 0.4f

    private val savedViewport = IntArray(4)

    fun initialize(mapSize: Int = DEFAULT_SHADOW_MAP_SIZE) {
        if (isInitialized) return

        shadowMapSize = mapSize

        shadowFbo = Framebuffer(shadowMapSize, shadowMapSize)
        shadowFbo!!.addDepthTexture()

        if (!shadowFbo!!.create()) {
            println("[ShadowPass] Failed to create shadow FBO")
            return
        }

        GLWrapper.bindFramebuffer(GLWrapper.Constants.FRAMEBUFFER, shadowFbo!!.getId())
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
        GLWrapper.bindFramebuffer(GLWrapper.Constants.FRAMEBUFFER, 0)

        try {
            shadowShader = ShaderProgram.createFromResources("/shaders/shadow.vert", "/shaders/shadow.frag")
        } catch (e: Exception) {
            println("[ShadowPass] Failed to compile shadow shader: ${e.message}")
            shadowFbo?.destroy()
            shadowFbo = null
            return
        }

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

    fun updateLightMatrix(sunDirection: Vector3f, sceneCenter: Vector3f) {
        if (!isInitialized) return

        lightPosition.set(sceneCenter).add(
            sunDirection.x * LIGHT_HEIGHT_OFFSET,
            sunDirection.y * LIGHT_HEIGHT_OFFSET,
            sunDirection.z * LIGHT_HEIGHT_OFFSET
        )
        lightTarget.set(sceneCenter)

        if (Math.abs(sunDirection.y.toDouble()) > 0.99) {
            lightUp.set(0f, 0f, 1f)
        } else {
            lightUp.set(0f, 1f, 0f)
        }

        lightViewMatrix.identity().lookAt(lightPosition, lightTarget, lightUp)

        lightProjectionMatrix.identity().ortho(
            -ORTHO_SIZE, ORTHO_SIZE,
            -ORTHO_SIZE, ORTHO_SIZE,
            NEAR_PLANE, FAR_PLANE
        )

        lightProjectionMatrix.mul(lightViewMatrix, lightViewProjectionMatrix)
    }

    fun beginPass() {
        if (!isInitialized || !isEnabled) return

        glGetIntegerv(GL_VIEWPORT, savedViewport)

        shadowFbo!!.bind()

        GLWrapper.clear(GLWrapper.Constants.DEPTH_BUFFER_BIT)

        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthFunc(GLWrapper.Constants.LESS)
        GLWrapper.depthMask(true)

        GLWrapper.colorMask(false, false, false, false)

        GLWrapper.disable(GLWrapper.Constants.BLEND)

        GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
        GLWrapper.cullFace(GLWrapper.Constants.FRONT)

        val s = shadowShader!!
        s.bind()
        s.setUniformMatrix4(uLightViewProjectionLocation, lightViewProjectionMatrix)

        s.setUniform(uAlphaTestEnabledLocation, 0)
        s.setUniform(uAlphaThresholdLocation, 0.5f)
    }

    fun setModelMatrix(modelMatrix: Matrix4f) {
        shadowShader?.setUniformMatrix4(uModelMatrixLocation, modelMatrix)
    }

    fun enableAlphaTest(textureUnit: Int = 0, threshold: Float = 0.5f) {
        val s = shadowShader ?: return
        s.setUniform(uAlphaTestEnabledLocation, 1)
        s.setUniform(uTextureLocation, textureUnit)
        s.setUniform(uAlphaThresholdLocation, threshold)
    }

    fun disableAlphaTest() {
        shadowShader?.setUniform(uAlphaTestEnabledLocation, 0)
    }

    fun endPass() {
        if (!isInitialized || !isEnabled) return

        shadowShader?.unbind()

        shadowFbo!!.unbind()

        GLWrapper.viewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3])

        GLWrapper.colorMask(true, true, true, true)

        GLWrapper.cullFace(GLWrapper.Constants.BACK)
        GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
    }

    fun bindShadowMap(textureUnit: Int = SHADOW_TEXTURE_UNIT) {
        if (!isInitialized || !isEnabled) return
        val depthTex = shadowFbo?.getDepthTexture() ?: return
        if (depthTex == 0) return

        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + textureUnit)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, depthTex)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
    }

    fun unbindShadowMap(textureUnit: Int = SHADOW_TEXTURE_UNIT) {
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + textureUnit)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, 0)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
    }

    fun getLightViewProjectionMatrix(): Matrix4f = lightViewProjectionMatrix

    fun getShadowMapTexture(): Int = shadowFbo?.getDepthTexture() ?: 0

    fun getShadowMapSize(): Int = shadowMapSize

    fun getShadowBias(): Float = shadowBias

    fun getShadowIntensity(): Float = shadowIntensity

    fun setShadowBias(bias: Float) {
        shadowBias = bias
    }

    fun setShadowIntensity(intensity: Float) {
        shadowIntensity = intensity.coerceIn(0f, 1f)
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled

    fun isReady(): Boolean = isInitialized && isEnabled

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

        GLWrapper.bindFramebuffer(GLWrapper.Constants.FRAMEBUFFER, shadowFbo!!.getId())
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
        GLWrapper.bindFramebuffer(GLWrapper.Constants.FRAMEBUFFER, 0)

        println("[ShadowPass] Resized to ${shadowMapSize}x${shadowMapSize}")
    }

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

    fun destroy() {
        shadowShader?.destroy()
        shadowShader = null

        shadowFbo?.destroy()
        shadowFbo = null

        isInitialized = false
    }
}
