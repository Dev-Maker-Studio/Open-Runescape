package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.Shader
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException
import com.jagex.game.runetek5.graphics.shader.ShaderMapping
import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader
import com.jagex.game.runetek5.math.Vector4
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode

class NativeSpriteShaderHd(nativeToolkit: AbstractNativeToolkit) : AbstractSpriteShader(nativeToolkit) {

    @JvmField var activeUniformLocations: IntArray? = null

    @JvmField var spriteShader: Shader? = null

    @JvmField val shaderMappings: Array<ShaderMapping?> = arrayOfNulls(3)

    @JvmField val uniformLocations: Array<IntArray> = Array(3) { IntArray(7) }

    @JvmField val mulColour = Vector4(1.0f, 1.0f, 1.0f, 1.0f)

    @JvmField val addColour = Vector4(0.0f, 0.0f, 0.0f, 0.0f)

    init {
        initialize()
    }

    override fun loadNormal() {
        spriteShader!!.loadProgram(shaderMappings[0])
        activeUniformLocations = uniformLocations[spriteShader!!.getOccupyingSlot()]
        load()
    }

    fun initialize(): Boolean {
        spriteShader = activeToolkit.loadShader("Sprite")
        val WVPMatrix = spriteShader!!.getAttribute("WVPMatrix")
        val SpriteSampler = spriteShader!!.getAttribute("SpriteSampler")
        val MaskSampler = spriteShader!!.getAttribute("MaskSampler")
        val MulColour = spriteShader!!.getAttribute("MulColour")
        val AddColour = spriteShader!!.getAttribute("AddColour")
        val SpriteTexCoordMatrix = spriteShader!!.getAttribute("SpriteTexCoordMatrix")
        val MaskTexCoordMatrix = spriteShader!!.getAttribute("MaskTexCoordMatrix")
        shaderMappings[0] = spriteShader!!.getShaderMapping("Normal")
        shaderMappings[1] = spriteShader!!.getShaderMapping("Masked")
        shaderMappings[2] = spriteShader!!.getShaderMapping("AlphaTex")
        for (type in 0 until 3) {
            val slot = spriteShader!!.getMappingSlot(shaderMappings[type])
            uniformLocations[type][0] = WVPMatrix.getUniformLocation(slot)
            uniformLocations[type][1] = SpriteSampler.getUniformLocation(slot)
            uniformLocations[type][2] = MaskSampler.getUniformLocation(slot)
            uniformLocations[type][5] = MulColour.getUniformLocation(slot)
            uniformLocations[type][6] = AddColour.getUniformLocation(slot)
            uniformLocations[type][3] = SpriteTexCoordMatrix.getUniformLocation(slot)
            uniformLocations[type][4] = MaskTexCoordMatrix.getUniformLocation(slot)
        }
        spriteShader!!.loadProgram(shaderMappings[0])
        return true
    }

    override fun setColours(type: Int, value: Int) {
        when (type) {
            0, 2 -> {
                mulColour.unpack(value)
                addColour.unpack(0)
            }
            1 -> {
                mulColour.unpack(-1)
                addColour.unpack(0)
            }
            3 -> {
                mulColour.unpack(0xffffff or (value and 0xffffff.inv()))
                addColour.unpack(value and 0xffffff)
            }
            4 -> {
                mulColour.unpack(-1)
                addColour.unpack(value)
                addColour.negate()
            }
        }
    }

    fun load() {
        spriteShader!!.setActiveMapping()
        spriteShader!!.setDiffuseSampler(activeUniformLocations!![1], 0, anInterface6443)
        activeToolkit.loadWorldViewProjectionMatrix(worldViewProjectionMatrix)
        spriteShader!!.setWorldViewProjectionMatrix(activeUniformLocations!![0], worldViewProjectionMatrix)
        spriteShader!!.setTextureCoordinateMatrix(activeUniformLocations!![3], aClass384440)
        spriteShader!!.load4f(activeUniformLocations!![5], mulColour.x, mulColour.y, mulColour.z, mulColour.w)
        spriteShader!!.load4f(activeUniformLocations!![6], addColour.x, addColour.y, addColour.z, addColour.w)
        activeToolkit.setStreamSource(0, anInterface4445)
        activeToolkit.setVertexDeclaration(aClass70447)
        activeToolkit.drawPrimitive(PrimitiveDrawMode.TRIANGLE_FAN, anInt446, 2)
    }

    override fun loadMasked() {
        spriteShader!!.loadProgram(shaderMappings[1])
        activeUniformLocations = uniformLocations[spriteShader!!.getOccupyingSlot()]
        spriteShader!!.setDiffuseSampler(activeUniformLocations!![2], 1, anInterface6441)
        spriteShader!!.setTextureCoordinateMatrix(activeUniformLocations!![4], aClass384444)
        load()
    }
}
