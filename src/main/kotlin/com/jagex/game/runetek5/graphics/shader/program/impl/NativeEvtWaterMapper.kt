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

class NativeEvtWaterMapper(class505Sub21: AbstractNativeToolkit, class492: NativeWaterTexture) : NativeWaterShader(class505Sub21) {

    @JvmField var anInt10094 = 0
    @JvmField var effectParam1 = 0
    @JvmField var anInt10110 = 0
    @JvmField var anInt10111 = 0
    @JvmField var anInt10112 = 0
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
    @JvmField var aFloat10114 = 0f
    @JvmField val sunDirectionAndExponentVector = Vector4()
    @JvmField val sunColorAndWaveExponentVector = Vector4()
    @JvmField val waveIntensityAndBreakWaterDepthAndOffsetVector = Vector4()
    @JvmField val nativeWaterTexture: NativeWaterTexture
    @JvmField val renderSea: Boolean

    init {
        Matrix44()
        nativeWaterTexture = class492
        renderSea = true
        if (class505Sub21.supportsArbVertexShader() && class505Sub21.supportsArbFragmentShader()) {
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

    override fun loadTextureMatrix(matrix441: Matrix44) {
        val matrix442 = Matrix44()
        matrix442.method6525(aFloat10114, aFloat10114, 1.0f, 1.0f)
        matrix442.calculatePerspective(matrix441, matrix442)
        super.loadTextureMatrix(matrix442)
    }

    override fun loadLights(dummy: Boolean) {
        val intensityMultiplier = 1.0f + (1.0f - Math.abs(toolkit.sunPosition[1])) * 2.0f
        val baseIntensity = toolkit.sunColorIntensity
        toolkit.sunColorIntensity *= intensityMultiplier
        super.loadLights(dummy)
        toolkit.sunColorIntensity = baseIntensity
    }

    fun method15517() {
        aFloat10114 = 2.4414062E-4f
        val z = 256
        val w = 0.3f
        val y = 0.4f
        val x = 0.1f
        val waveExponent = 5
        textureCoordinateMatrix.method6525(aFloat10114, aFloat10114, aFloat10114, aFloat10114)
        sunDirectionAndExponentVector.set(-toolkit.sunPosition[0], -toolkit.sunPosition[1], -toolkit.sunPosition[2], 32.0f)
        sunColorAndWaveExponentVector.set(toolkit.baseLightColorRed, toolkit.baseLightColorGreen, toolkit.baseLightColorBlue, waveExponent.toFloat())
        waveIntensityAndBreakWaterDepthAndOffsetVector.set(x, y, z.toFloat(), w)
    }

    fun method15520() {
        method15517()
        activeShader!!.loadProgram(activeShader!!.createSingleMapping()!!)
        activeShader!!.setActiveMapping()
        activeShader!!.loadTextureSampler(normalSamplerAttribute!!, 0, nativeWaterTexture.normalSampler!!)
        activeShader!!.loadTextureSampler(envMapSamplerAttribute!!, 1, toolkit.getEnvironmentMapSampler())
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
        toolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, anInt10110, anInt10111, anInt10094, anInt10112)
    }
}
