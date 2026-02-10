package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.companion.Binding

abstract class ShaderMapping : Binding {

    @JvmField var vertexShaderName: String? = null
    @JvmField var fragmentShaderName: String? = null
    @JvmField var shaderName: String? = null

    abstract fun uniform4fv(attribute: ShaderAttribute, matrix: Matrix44)

    fun getShaderName(): String? {
        return this.shaderName
    }

    abstract fun addPositionAttribute3f(variableProgramAndIndex: Int, x: Float, y: Float, z: Float)

    abstract fun uniform1f(attribute: ShaderAttribute, value: Float)

    abstract fun addPositionAttribute4f(variableProgramAndIndex: Int, x: Float, y: Float, z: Float, w: Float)

    abstract fun loadTextureSampler(attribute: ShaderAttribute, slot: Int, sampler: TextureSampler)

    abstract fun addPreloadedPositionAttribute(attribute: ShaderAttribute, x: Float, y: Float, z: Float, w: Float)

    abstract fun createShaders(): Boolean

    abstract fun uniform3fSavedSize16(slotAndCopyDest: Int, matrix: Matrix44)

    abstract fun uniform4fv(attribute: ShaderAttribute, values: FloatArray, count: Int)

    abstract fun addPositionAttributeDynamic(location: Int, values: FloatArray, count: Int)

    abstract fun uniform3fSavedSize12(slotAndCopyDest: Int, matrix: Matrix44)

    abstract fun uniform3fSavedSize8(slotAndCopyDest: Int, matrix: Matrix44)

    abstract fun initializeTextureSampler(location: Int, slot: Int, sampler: TextureSampler)

    abstract fun unform2f(attribute: ShaderAttribute, x: Float, y: Float)

    abstract fun uniform2fv(attribute: ShaderAttribute, matrix: Matrix44)

    abstract fun uniform3f(attribute: ShaderAttribute, x: Float, y: Float, z: Float)
}
