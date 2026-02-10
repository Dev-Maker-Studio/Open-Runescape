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
        this.primaryShaderBuilder = ProgramBuilder(this.primaryShaderVariableCount, attributeHandler)
        for (index in 0 until this.primaryShaderVariableCount) {
            this.primaryShaderBuilder.addAttribute(index, shaderloader.primaryShaderVariables!![index].primaryShaderAttributeName!!, decode(shaderloader.primaryShaderVariables!![index]))
        }
        this.secondaryShaderVariableCount = shaderloader.secondaryShaderVariables!!.size
        this.secondaryShaderBuilder = ProgramBuilder(this.secondaryShaderVariableCount, attributeHandler)
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

    fun loadTextureMatrix(attribute: ShaderAttribute, matrix: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform2fv(attribute, matrix)
    }

    fun loadMatrix(attribute: ShaderAttribute, matrix: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform4fv(attribute, matrix)
    }

    fun load3f(location: Int, x: Float, y: Float, z: Float) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute3f(location, x, y, z)
    }

    fun loadTextureSampler(attribute: ShaderAttribute, slot: Int, sampler: TextureSampler) {
        this.shaderMapping[this.occupyingSlot].loadTextureSampler(attribute, slot, sampler)
    }

    fun loadVector3f(location: Int, vector: Vector3) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute3f(location, vector.x, vector.y, vector.z)
    }

    fun uploadDynamicUniform4fv(location: Int, values: FloatArray, count: Int) {
        this.shaderMapping[this.occupyingSlot].addPositionAttributeDynamic(location, values, count)
    }

    fun setWorldMatrix(location: Int, matrix: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize12(location, matrix)
    }

    fun setTextureCoordinateMatrix(location: Int, matrix: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize8(location, matrix)
    }

    fun setWorldViewProjectionMatrix(location: Int, matrix: Matrix44) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize16(location, matrix)
    }

    fun setDiffuseSampler(location: Int, slot: Int, sampler: TextureSampler) {
        this.shaderMapping[this.occupyingSlot].initializeTextureSampler(location, slot, sampler)
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

    fun uploadUniform4fv(attribute: ShaderAttribute, values: FloatArray) {
        this.shaderMapping[this.occupyingSlot].uniform4fv(attribute, values, values.size)
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

    fun load4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute4f(location, x, y, z, w)
    }

    abstract fun decode(decoder: ShaderVariableDecoder): ShaderAttribute

    companion object {
        @JvmField val attributeHandler: IProgramAttributeHandler = ProgramAttributeHandler()
    }
}
