package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.companion.Binding

abstract class ShaderMapping : Binding {

    @JvmField var vertexShaderName: String? = null
    @JvmField var fragmentShaderName: String? = null
    @JvmField var shaderName: String? = null

    abstract fun uniform4fv(var1: ShaderAttribute, var2: Matrix44)

    fun getShaderName(): String? {
        return this.shaderName
    }

    abstract fun addPositionAttribute3f(variableProgramAndIndex: Int, x: Float, y: Float, z: Float)

    abstract fun uniform1f(var1: ShaderAttribute, var2: Float)

    abstract fun addPositionAttribute4f(variableProgramAndIndex: Int, x: Float, y: Float, z: Float, w: Float)

    abstract fun loadTextureSampler(var1: ShaderAttribute, var2: Int, var3: TextureSampler)

    abstract fun addPreloadedPositionAttribute(attribute: ShaderAttribute, x: Float, y: Float, z: Float, w: Float)

    abstract fun createShaders(): Boolean

    abstract fun uniform3fSavedSize16(slotAndCopyDest: Int, matrix: Matrix44)

    abstract fun uniform4fv(var1: ShaderAttribute, var2: FloatArray, var3: Int)

    abstract fun addPositionAttributeDynamic(var1: Int, var2: FloatArray, var3: Int)

    abstract fun uniform3fSavedSize12(slotAndCopyDest: Int, matrix: Matrix44)

    abstract fun uniform3fSavedSize8(slotAndCopyDest: Int, matrix: Matrix44)

    abstract fun initializeTextureSampler(var1: Int, slot: Int, var3: TextureSampler)

    abstract fun unform2f(var1: ShaderAttribute, var2: Float, var3: Float)

    abstract fun uniform2fv(var1: ShaderAttribute, var2: Matrix44)

    abstract fun uniform3f(var1: ShaderAttribute, var2: Float, var3: Float, var4: Float)
}
