package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping
import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.math.Vector4
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

abstract class Shader protected constructor(toolkit: AbstractNativeToolkit, shaderloader: ShaderLoader) {

    @JvmField protected var occupyingSlot: Int = -1

    @JvmField val shaderName: String

    @JvmField val primaryShaderVariableCount: Int

    @JvmField val primaryShaderBuilder: ProgramBuilder

    @JvmField val secondaryShaderVariableCount: Int

    @JvmField val secondaryShaderBuilder: ProgramBuilder

    @JvmField val shaderMapping: Array<ShaderMapping>

    init {
        this.shaderName = shaderloader.name!!
        this.primaryShaderVariableCount = shaderloader.primaryShaderVariables!!.size
        this.primaryShaderBuilder = ProgramBuilder(this.primaryShaderVariableCount, anInterface111012)
        for (index in 0 until this.primaryShaderVariableCount) {
            this.primaryShaderBuilder.addAttribute(index, shaderloader.primaryShaderVariables!![index].primaryShaderAttributeName!!, decode(shaderloader.primaryShaderVariables!![index]))
        }
        this.secondaryShaderVariableCount = shaderloader.secondaryShaderVariables!!.size
        this.secondaryShaderBuilder = ProgramBuilder(this.secondaryShaderVariableCount, anInterface111012)
        for (index in 0 until this.secondaryShaderVariableCount) {
            this.secondaryShaderBuilder.addAttribute(index, shaderloader.secondaryShaderVariables!![index].primaryShaderAttributeName!!, decode(shaderloader.secondaryShaderVariables!![index]))
        }
        this.shaderMapping = Array(shaderloader.mappings!!.size) { index ->
            loadMapping(toolkit, shaderloader.mappings!![index])
        }
    }

    abstract fun setActiveMapping()

    abstract fun reset()

    abstract fun isActiveMapping(): Boolean

    fun getMappingCount(): Int {
        return this.shaderMapping.size
    }

    @Throws(MappedShaderCreationException::class)
    fun getShaderMapping(requestedMapping: String): ShaderMapping {
        val mapped = this.shaderMapping
        for (index in mapped.indices) {
            val mapping = mapped[index]
            val shaderName = mapping.getShaderName()
            if (shaderName != null && shaderName == requestedMapping) {
                if (!mapping.createShaders()) {
                    throw MappedShaderCreationException(requestedMapping)
                }
                return mapping
            }
        }
        throw MappedShaderCreationException(requestedMapping)
    }

    fun getMapping(slot: Int): ShaderMapping {
        return this.shaderMapping[slot]
    }

    fun getMappingSlot(mapped: ShaderMapping): Int {
        for (slot in this.shaderMapping.indices) {
            if (this.shaderMapping[slot] === mapped) {
                return slot
            }
        }
        return -1
    }

    fun createSingleMapping(): ShaderMapping? {
        val mapped = this.shaderMapping
        for (slot in mapped.indices) {
            val mapping = mapped[slot]
            if (mapping.createShaders()) {
                return mapping
            }
        }
        return null
    }

    abstract fun loadProgram(mapping: ShaderMapping): Boolean

    fun getActiveMapping(): ShaderMapping? {
        return if (this.occupyingSlot >= 0) this.shaderMapping[this.occupyingSlot] else null
    }

    fun unform2f(attribute: ShaderAttribute, f1: Float, f2: Float) {
        this.shaderMapping[this.occupyingSlot].unform2f(attribute, f1, f2)
    }

    fun uniform1f(attribute: ShaderAttribute, variable: Float) {
        this.shaderMapping[this.occupyingSlot].uniform1f(attribute, variable)
    }

    fun uniform3f(attribute: ShaderAttribute, variableA: Float, variableB: Float, variableC: Float) {
        this.shaderMapping[this.occupyingSlot].uniform3f(attribute, variableA, variableB, variableC)
    }

    //active_shader.load_rgba_color_attribute(eye_pos_and_time, aClass385_10089.x, aClass385_10089.y, aClass385_10089.z, (float) (i_2 * toolkit.anInt8820 % 40000) / 40000.0F);
    fun loadRgbaColorAttribute(shaderAttribute: ShaderAttribute, x: Float, y: Float, z: Float, time: Float) {
        this.shaderMapping[this.occupyingSlot].addPreloadedPositionAttribute(shaderAttribute, x, y, z, time)
    }

    fun uniform3f(attribute: ShaderAttribute, vector: Vector3) {
        this.shaderMapping[this.occupyingSlot].uniform3f(attribute, vector.x, vector.y, vector.z)
    }

    fun loadTextureMatrix(class282Sub21Sub11: ShaderAttribute, matrix442: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform2fv(class282Sub21Sub11, matrix442)
    }

    fun loadMatrix(class282Sub21Sub11: ShaderAttribute, matrix442: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform4fv(class282Sub21Sub11, matrix442)
    }

    fun load3f(i_1: Int, f_2: Float, f_3: Float, f_4: Float) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute3f(i_1, f_2, f_3, f_4)
    }

    fun loadTextureSampler(attribute: ShaderAttribute, i_2: Int, sampler: TextureSampler) {
        this.shaderMapping[this.occupyingSlot].loadTextureSampler(attribute, i_2, sampler)
    }

    fun loadVector3f(i_1: Int, vector32: Vector3) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute3f(i_1, vector32.x, vector32.y, vector32.z)
    }

    fun method1673(i_1: Int, floats_2: FloatArray, i_3: Int) {
        this.shaderMapping[this.occupyingSlot].addPositionAttributeDynamic(i_1, floats_2, i_3)
    }

    fun method1674(i_1: Int, matrix442: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize12(i_1, matrix442)
    }

    fun setTextureCoordinateMatrix(i_1: Int, matrix442: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize8(i_1, matrix442)
    }

    fun setWorldViewProjectionMatrix(i_1: Int, matrix442: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize16(i_1, matrix442)
    }

    fun setDiffuseSampler(i_1: Int, slot: Int, interface303: TextureSampler) {
        this.shaderMapping[this.occupyingSlot].initializeTextureSampler(i_1, slot, interface303)
    }

    fun getSecondaryShaderVariableCount(): Int {
        return this.secondaryShaderVariableCount
    }

    fun loadAttribute(attributeName: String): ShaderAttribute {
        return this.secondaryShaderBuilder.loadAttribute(attributeName) as ShaderAttribute
    }

    fun getPrimaryShaderVariableCount(): Int {
        return this.primaryShaderVariableCount
    }

    open fun dispose() {
    }

    fun method1688(class282Sub21Sub11: ShaderAttribute, floats_2: FloatArray) {
        this.shaderMapping[this.occupyingSlot].uniform4fv(class282Sub21Sub11, floats_2, floats_2.size)
    }

    fun insertAttribute(attribute: ShaderAttribute, vector4: Vector4) {
        this.shaderMapping[this.occupyingSlot].addPreloadedPositionAttribute(attribute, vector4.x, vector4.y, vector4.z, vector4.w)
    }

    @Throws(ShaderAttributeLoadingException::class)
    fun getAttribute(attributeName: String): ShaderAttribute {
        val shaderAttribute = this.primaryShaderBuilder.loadAttribute(attributeName) as ShaderAttribute?
        if (shaderAttribute == null) {
            throw ShaderAttributeLoadingException(attributeName)
        }
        return shaderAttribute
    }

    fun loadRgbaColorAttribute(shaderAttribute: ShaderAttribute, argb: Int) {
        val r = (argb shr 16 and 0xff) / 255.0F
        val g = (argb shr 8 and 0xff) / 255.0F
        val b = (argb and 0xff) / 255.0F
        val a = (argb shr 24 and 0xff) / 255.0F
        loadRgbaColorAttribute(shaderAttribute, r, g, b, a)
    }

    fun getSecondaryAttribute(index: Int): ShaderAttribute {
        return this.secondaryShaderBuilder.getAttribute(index) as ShaderAttribute
    }

    fun getOccupyingSlot(): Int {
        return this.occupyingSlot
    }

    fun getShaderName(): String {
        return this.shaderName
    }

    fun getShaderAttribute(index: Int): ShaderAttribute {
        return this.primaryShaderBuilder.getAttribute(index) as ShaderAttribute
    }

    abstract fun loadMapping(toolkit: AbstractNativeToolkit, mapping: NativeShaderMapping): ShaderMapping

    fun load4f(i_1: Int, f_2: Float, f_3: Float, f_4: Float, f_5: Float) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute4f(i_1, f_2, f_3, f_4, f_5)
    }

    abstract fun decode(decoder: ShaderVariableDecoder): ShaderAttribute

    companion object {
        @JvmField val anInterface111012: IProgramAttributeHandler = ProgramAttributeHandler()
    }
}
