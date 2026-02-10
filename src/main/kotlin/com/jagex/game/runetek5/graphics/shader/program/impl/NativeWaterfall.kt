package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.ShaderAttribute
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException
import com.jagex.game.runetek5.graphics.shader.ShaderMapping
import com.jagex.game.runetek5.graphics.texture.water.NativeWaterTexture
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Vector4
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode

class NativeWaterfall(
    toolkit: AbstractNativeToolkit,
    val waterTexture: NativeWaterTexture
) : NativeWaterShader(toolkit) {

    @JvmField var startIndex: Int = 0

    @JvmField var startVertex: Int = 0

    @JvmField var vertexCount: Int = 0

    @JvmField var primitiveCount: Int = 0
    @JvmField val viewProjectionMatrix = Matrix44()
    @JvmField val waterfallModelMatrix = Matrix44()
    @JvmField var worldMatrix: ShaderAttribute? = null
    @JvmField var wvpMatrix: ShaderAttribute? = null
    @JvmField var uGenerationPlane: ShaderAttribute? = null
    @JvmField var vGenerationPlane: ShaderAttribute? = null
    @JvmField var time: ShaderAttribute? = null
    @JvmField var billowSampler3d: ShaderAttribute? = null
    @JvmField var waterfall2d: ShaderMapping? = null
    @JvmField var waterfall3d: ShaderMapping? = null
    @JvmField val uGenerationCoefficients = FloatArray(4)

    @JvmField val vGenerationCoefficients = FloatArray(4)

    @JvmField var flowTime: Float = 0f

    init {
        if (waterTexture.readyToSample() && toolkit.supportsArbVertexShader()) {
            loadShaderWithAttributes("Waterfall")
        }
    }

    override fun buildProgram(): Boolean {
        worldMatrix = activeShader!!.getAttribute("WorldMatrix")
        wvpMatrix = activeShader!!.getAttribute("WVPMatrix")
        uGenerationPlane = activeShader!!.getAttribute("UGenerationPlane")
        vGenerationPlane = activeShader!!.getAttribute("VGenerationPlane")
        time = activeShader!!.getAttribute("Time")
        billowSampler3d = activeShader!!.getAttribute("billowSampler3D")
        if (waterTexture.hasDepth) {
            waterfall3d = activeShader!!.getShaderMapping("Waterfall3D")
            activeShader!!.loadProgram(waterfall3d!!)
        } else {
            waterfall2d = activeShader!!.getShaderMapping("Waterfall2D")
            activeShader!!.loadProgram(waterfall2d!!)
        }
        return true
    }

    fun renderWaterfall() {
        if (waterTexture.hasDepth) {
            activeShader!!.loadProgram(waterfall3d!!)
        } else {
            activeShader!!.loadProgram(waterfall2d!!)
        }
        activeShader!!.setActiveMapping()
        if (waterTexture.hasDepth) {
            activeShader!!.loadTextureSampler(billowSampler3d!!, 0, waterTexture.billowSampler!!)
        }
        activeShader!!.loadMatrix(worldMatrix!!, waterfallModelMatrix)
        activeShader!!.loadMatrix(wvpMatrix!!, viewProjectionMatrix)
        activeShader!!.insertAttribute(uGenerationPlane!!, Vector4(uGenerationCoefficients[0], uGenerationCoefficients[1], uGenerationCoefficients[2], uGenerationCoefficients[3]))
        activeShader!!.insertAttribute(vGenerationPlane!!, Vector4(vGenerationCoefficients[0], vGenerationCoefficients[1], vGenerationCoefficients[2], vGenerationCoefficients[3]))
        activeShader!!.insertAttribute(time!!, Vector4(flowTime, 0.0f, 0.0f, 0.0f))
        toolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, startVertex, vertexCount, startIndex, primitiveCount)
    }

    fun updateFlowParameters(flags: Int) {
        val flowSpeedU = ((flags and 0x3) + 1) * -5.0E-4f
        val flowSpeedV = ((flags shr 3 and 0x3) + 1) * 5.0E-4f
        val textureScale = if ((flags and 0x40) == 0) 4.8828125E-4f else 9.765625E-4f
        val isVertical = (flags and 0x80) != 0
        if (isVertical) {
            uGenerationCoefficients[0] = textureScale
            uGenerationCoefficients[1] = 0.0f
            uGenerationCoefficients[2] = 0.0f
        } else {
            uGenerationCoefficients[0] = 0.0f
            uGenerationCoefficients[1] = 0.0f
            uGenerationCoefficients[2] = textureScale
        }
        uGenerationCoefficients[3] = 0.0f
        vGenerationCoefficients[0] = 0.0f
        vGenerationCoefficients[1] = textureScale
        vGenerationCoefficients[2] = 0.0f
        vGenerationCoefficients[3] = toolkit.animatingCycle * flowSpeedU % 1.0f
        if (!waterTexture.hasDepth) {
            val textureIndex = (flowSpeedV * toolkit.animatingCycle * 16.0f).toInt()
            toolkit.setTextureSampler(waterTexture.textureSamplers!![textureIndex % 16])
        } else {
            flowTime = (toolkit.animatingCycle.toDouble() * flowSpeedV % 1.0).toFloat()
        }
    }
}
