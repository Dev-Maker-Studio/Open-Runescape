package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.Shader
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException
import com.jagex.game.runetek5.graphics.shader.program.AbstractParticleShader
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode

/**
 * High-definition particle shader implementation for native toolkits.
 */
class NativeParticleShaderHd(toolkit: AbstractNativeToolkit) : AbstractParticleShader(toolkit) {

    @JvmField var particleShader: Shader? = null
    @JvmField var wvpMatrixAttribute: ShaderAttribute? = null
    @JvmField var diffuseSamplerAttribute: ShaderAttribute? = null
    @JvmField var textureCoordinateMatrixAttribute: ShaderAttribute? = null
    @JvmField var diffuseColorAttribute: ShaderAttribute? = null
    @JvmField val wvpMatrix = Matrix44()

    init {
        loadAttributes()
    }

    /**
     * Loads the shader attributes for the particle shader.
     *
     * @return true if attributes were loaded successfully
     * @throws ShaderBuilderException if shader loading fails
     */
    fun loadAttributes(): Boolean {
        particleShader = activeToolkit.loadShader("Particle")
        wvpMatrixAttribute = particleShader!!.getAttribute("WVPMatrix")
        diffuseSamplerAttribute = particleShader!!.getAttribute("DiffuseSampler")
        textureCoordinateMatrixAttribute = particleShader!!.getAttribute("TexCoordMatrix")
        diffuseColorAttribute = particleShader!!.getAttribute("DiffuseColour")
        particleShader!!.loadProgram(particleShader!!.createSingleMapping())
        return true
    }

    override fun method1788(matrix441: Matrix44) {
        wvpMatrix.copyFrom(matrix441)
        wvpMatrix.multiplyBy(activeToolkit.aClass3848729)
    }

    override fun method1787(i_1: Int) {
        loadProgram()
        activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, 0, i_1 * 4, 0, i_1 * 2)
    }

    override fun method1790() {
        loadProgram()
        activeToolkit.method14150()
    }

    /**
     * Loads the shader program and sets all shader parameters.
     */
    fun loadProgram() {
        particleShader!!.loadProgram(particleShader!!.createSingleMapping())
        particleShader!!.setActiveMapping()
        particleShader!!.loadTextureSampler(diffuseSamplerAttribute, 0, diffuseSampler)
        particleShader!!.loadMatrix(wvpMatrixAttribute, wvpMatrix)
        particleShader!!.loadTextureMatrix(textureCoordinateMatrixAttribute, textureCoordinateMatrix)
        particleShader!!.loadRgbaColorAttribute(diffuseColorAttribute, diffuseColor)
    }
}
