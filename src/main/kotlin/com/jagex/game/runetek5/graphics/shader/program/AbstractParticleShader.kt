package com.jagex.game.runetek5.graphics.shader.program

import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

abstract class AbstractParticleShader protected constructor(
    @JvmField protected val activeToolkit: AbstractNativeToolkit
) {

    @JvmField
    var diffuseColor: Int = 0

    @JvmField
    var diffuseSampler: NativeTextureSampler? = null

    @JvmField
    val textureCoordinateMatrix: Matrix44 = Matrix44()

    abstract fun method1787(var1: Int)

    abstract fun method1788(var1: Matrix44)

    abstract fun method1790()
}
