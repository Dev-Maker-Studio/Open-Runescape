package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.ShaderAttribute
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException
import com.jagex.game.runetek5.graphics.shader.ShaderMapping
import com.jagex.game.runetek5.graphics.texture.water.NativeWaterTexture
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.math.Vector4
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode

class NativeEvtWaterMapper(nativeToolkit: AbstractNativeToolkit, waterTexture: NativeWaterTexture) : NativeWaterShader(nativeToolkit) {

    @JvmField var startIndex = 0
    @JvmField var effectParam1 = 0
    @JvmField var startVertex = 0
    @JvmField var vertexCount = 0
    @JvmField var primitiveCount = 0
    @JvmField val waterViewingPlaneMatrix = Matrix44()
    @JvmField val textureCoordinateMatrix = Matrix44()
    @JvmField val eye = Vector3()
    @JvmField val heightFogPlaneVector = Vector4()
    @JvmField val heightFogColorVector = Vector3()
    @JvmField val distanceFogPlaneVector = Vector4()
    @JvmField val distanceFogColorVector = Vector3()
    @JvmField var normalSamplerAttribute: ShaderAttribute? = null
    @JvmField var envMapSamplerAttribute: ShaderAttribute? = null
    @JvmField var waterViewingPlaneMatrixAttribute: ShaderAttribute? = null
    @JvmField var textureCoordinateMatrixAttribute: ShaderAttribute? = null
    @JvmField var eyePosAndTimeAttribute: ShaderAttribute? = null
    @JvmField var sunDirectionAndExponentAttribute: ShaderAttribute? = null
    @JvmField var sunColorAndWaveExponentAttribute: ShaderAttribute? = null
    @JvmField var waveIntensityAndBreakWaterDepthAndOffsetAttribute: ShaderAttribute? = null
    @JvmField var heightFogPlaneAttribute: ShaderAttribute? = null
    @JvmField var heightFogColorAttribute: ShaderAttribute? = null
    @JvmField var distanceFogPlaneAttribute: ShaderAttribute? = null
    @JvmField var distanceFogColorAttribute: ShaderAttribute? = null
    @JvmField var textureScale = 0f
    @JvmField val sunDirectionAndExponentVector = Vector4()
    @JvmField val sunColorAndWaveExponentVector = Vector4()
    @JvmField val waveIntensityAndBreakWaterDepthAndOffsetVector = Vector4()
    @JvmField val nativeWaterTexture: NativeWaterTexture
    @JvmField val renderSea: Boolean

    init {
        Matrix44()
        nativeWaterTexture = waterTexture
        renderSea = true
        if (nativeToolkit.supportsArbVertexShader() && nativeToolkit.supportsArbFragmentShader()) {
            loadShaderWithAttributes("EnvMappedWater")
        }
    }

    override fun buildProgram(): Boolean {
        if (!nativeWaterTexture.loadSampledTexture()) {
            return false
        }
        normalSamplerAttribute = activeShader!!.getAttribute("NormalSampler")
        envMapSamplerAttribute = activeShader!!.getAttribute("EnvMapSampler")
        waterViewingPlaneMatrixAttribute = activeShader!!.getAttribute("WVPMatrix")
        textureCoordinateMatrixAttribute = activeShader!!.getAttribute("TexCoordMatrix")
        eyePosAndTimeAttribute = activeShader!!.getAttribute("EyePosAndTime")
        sunDirectionAndExponentAttribute = activeShader!!.getAttribute("SunDirectionAndExponent")
        sunColorAndWaveExponentAttribute = activeShader!!.getAttribute("SunColourAndWaveExponent")
        waveIntensityAndBreakWaterDepthAndOffsetAttribute = activeShader!!.getAttribute("WaveIntensityAndBreakWaterDepthAndOffset")
        heightFogPlaneAttribute = activeShader!!.getAttribute("HeightFogPlane")
        heightFogColorAttribute = activeShader!!.getAttribute("HeightFogColour")
        distanceFogPlaneAttribute = activeShader!!.getAttribute("DistanceFogPlane")
        distanceFogColorAttribute = activeShader!!.getAttribute("DistanceFogColour")
        val mappedWater = activeShader!!.getShaderMapping(if (renderSea) "EnvMappedSea" else "EnvMappedWater")
        activeShader!!.loadProgram(mappedWater)
        return true
    }

    override fun loadTextureMatrix(textureMatrix: Matrix44) {
        val scaledMatrix = Matrix44()
        scaledMatrix.setDiagonal(textureScale, textureScale, 1.0f, 1.0f)
        scaledMatrix.setProduct(textureMatrix, scaledMatrix)
        super.loadTextureMatrix(scaledMatrix)
    }

    override fun loadLights(dummy: Boolean) {
        val intensityMultiplier = 1.0f + (1.0f - Math.abs(toolkit.sunPosition[1])) * 2.0f
        val baseIntensity = toolkit.sunColorIntensity
        toolkit.sunColorIntensity *= intensityMultiplier
        super.loadLights(dummy)
        toolkit.sunColorIntensity = baseIntensity
    }

    fun updateWaterParameters() {
        textureScale = 2.4414062E-4f
        val breakWaterDepth = 256
        val breakWaterOffset = 0.3f
        val waveIntensityHigh = 0.4f
        val waveIntensityLow = 0.1f
        val waveExponent = 5
        textureCoordinateMatrix.setDiagonal(textureScale, textureScale, textureScale, textureScale)
        sunDirectionAndExponentVector.set(-toolkit.sunPosition[0], -toolkit.sunPosition[1], -toolkit.sunPosition[2], 32.0f)
        sunColorAndWaveExponentVector.set(toolkit.baseLightColorRed, toolkit.baseLightColorGreen, toolkit.baseLightColorBlue, waveExponent.toFloat())
        waveIntensityAndBreakWaterDepthAndOffsetVector.set(waveIntensityLow, waveIntensityHigh, breakWaterDepth.toFloat(), breakWaterOffset)
    }

    fun renderWater() {
        updateWaterParameters()
        activeShader!!.loadProgram(activeShader!!.createSingleMapping()!!)
        activeShader!!.setActiveMapping()
        activeShader!!.loadTextureSampler(normalSamplerAttribute!!, 0, nativeWaterTexture.normalSampler!!)
        activeShader!!.loadTextureSampler(envMapSamplerAttribute!!, 1, toolkit.getEnvironmentMapSampler()!!)
        activeShader!!.loadMatrix(waterViewingPlaneMatrixAttribute!!, waterViewingPlaneMatrix)
        activeShader!!.loadTextureMatrix(textureCoordinateMatrixAttribute!!, textureCoordinateMatrix)
        val speed = 1 shl (effectParam1 and 0x3)
        activeShader!!.loadRgbaColorAttribute(eyePosAndTimeAttribute!!, eye.x, eye.y, eye.z, (speed * toolkit.animatingCycle % 40000).toFloat() / 40000.0f)
        activeShader!!.insertAttribute(sunDirectionAndExponentAttribute!!, sunDirectionAndExponentVector)
        activeShader!!.insertAttribute(sunColorAndWaveExponentAttribute!!, sunColorAndWaveExponentVector)
        activeShader!!.insertAttribute(waveIntensityAndBreakWaterDepthAndOffsetAttribute!!, waveIntensityAndBreakWaterDepthAndOffsetVector)
        activeShader!!.insertAttribute(heightFogPlaneAttribute!!, heightFogPlaneVector)
        activeShader!!.uniform3f(heightFogColorAttribute!!, heightFogColorVector)
        activeShader!!.insertAttribute(distanceFogPlaneAttribute!!, distanceFogPlaneVector)
        activeShader!!.uniform3f(distanceFogColorAttribute!!, distanceFogColorVector)
        toolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, startVertex, vertexCount, startIndex, primitiveCount)
    }
}
