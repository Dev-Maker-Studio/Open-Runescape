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
    val aClass4910147: NativeWaterTexture
) : NativeWaterShader(toolkit) {

    @JvmField var anInt10142: Int = 0

    @JvmField var anInt10152: Int = 0

    @JvmField var anInt10153: Int = 0

    @JvmField var anInt10155: Int = 0
    @JvmField val aClass38410138 = Matrix44()
    @JvmField val aClass38410146 = Matrix44()
    @JvmField var worldMatrix: ShaderAttribute? = null
    @JvmField var wvpMatrix: ShaderAttribute? = null
    @JvmField var uGenerationPlane: ShaderAttribute? = null
    @JvmField var vGenerationPlane: ShaderAttribute? = null
    @JvmField var time: ShaderAttribute? = null
    @JvmField var billowSampler3d: ShaderAttribute? = null
    @JvmField var waterfall2d: ShaderMapping? = null
    @JvmField var waterfall3d: ShaderMapping? = null
    @JvmField val aFloatArray10148 = FloatArray(4)

    @JvmField val aFloatArray10150 = FloatArray(4)

    @JvmField var aFloat10149: Float = 0f

    init {
        if (aClass4910147.readyToSample() && toolkit.supportsArbVertexShader()) {
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
        if (aClass4910147.hasDepth) {
            waterfall3d = activeShader!!.getShaderMapping("Waterfall3D")
            activeShader!!.loadProgram(waterfall3d)
        } else {
            waterfall2d = activeShader!!.getShaderMapping("Waterfall2D")
            activeShader!!.loadProgram(waterfall2d)
        }
        return true
    }

    fun method15549() {
        if (aClass4910147.hasDepth) {
            activeShader!!.loadProgram(waterfall3d)
        } else {
            activeShader!!.loadProgram(waterfall2d)
        }
        activeShader!!.setActiveMapping()
        if (aClass4910147.hasDepth) {
            activeShader!!.loadTextureSampler(billowSampler3d, 0, aClass4910147.billowSampler)
        }
        activeShader!!.loadMatrix(worldMatrix, aClass38410146)
        activeShader!!.loadMatrix(wvpMatrix, aClass38410138)
        activeShader!!.insertAttribute(uGenerationPlane, Vector4(aFloatArray10148[0], aFloatArray10148[1], aFloatArray10148[2], aFloatArray10148[3]))
        activeShader!!.insertAttribute(vGenerationPlane, Vector4(aFloatArray10150[0], aFloatArray10150[1], aFloatArray10150[2], aFloatArray10150[3]))
        activeShader!!.insertAttribute(time, Vector4(aFloat10149, 0.0f, 0.0f, 0.0f))
        toolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, anInt10152, anInt10153, anInt10142, anInt10155)
    }

    fun method15550(flags: Int) {
        val flowSpeedU = ((flags and 0x3) + 1) * -5.0E-4f
        val flowSpeedV = ((flags shr 3 and 0x3) + 1) * 5.0E-4f
        val textureScale = if ((flags and 0x40) == 0) 4.8828125E-4f else 9.765625E-4f
        val isVertical = (flags and 0x80) != 0
        if (isVertical) {
            aFloatArray10148[0] = textureScale
            aFloatArray10148[1] = 0.0f
            aFloatArray10148[2] = 0.0f
        } else {
            aFloatArray10148[0] = 0.0f
            aFloatArray10148[1] = 0.0f
            aFloatArray10148[2] = textureScale
        }
        aFloatArray10148[3] = 0.0f
        aFloatArray10150[0] = 0.0f
        aFloatArray10150[1] = textureScale
        aFloatArray10150[2] = 0.0f
        aFloatArray10150[3] = toolkit.animatingCycle * flowSpeedU % 1.0f
        if (!aClass4910147.hasDepth) {
            val textureIndex = (flowSpeedV * toolkit.animatingCycle * 16.0f).toInt()
            toolkit.setTextureSampler(aClass4910147.textureSamplers[textureIndex % 16])
        } else {
            aFloat10149 = (toolkit.animatingCycle.toDouble() * flowSpeedV % 1.0).toFloat()
        }
    }
}
