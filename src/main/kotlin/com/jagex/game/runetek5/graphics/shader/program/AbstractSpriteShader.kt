package com.jagex.game.runetek5.graphics.shader.program

import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration

abstract class AbstractSpriteShader(
    @JvmField protected val activeToolkit: AbstractNativeToolkit
) {
    @JvmField var anInterface6441: NativeTextureSampler? = null
    @JvmField var anInterface6443: NativeTextureSampler? = null
    @JvmField var anInterface4445: NativeStream? = null
    @JvmField var anInt446: Int = 0
    @JvmField var aClass70447: VertexDeclaration? = null
    @JvmField val worldViewProjectionMatrix: Matrix44 = Matrix44()
    @JvmField val aClass384440: Matrix44 = Matrix44()
    @JvmField val aClass384444: Matrix44 = Matrix44()

    abstract fun loadNormal()

    abstract fun setColours(type: Int, value: Int)

    abstract fun loadMasked()
}
