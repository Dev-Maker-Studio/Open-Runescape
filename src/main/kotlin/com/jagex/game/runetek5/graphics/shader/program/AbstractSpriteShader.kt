package com.jagex.game.runetek5.graphics.shader.program

import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration

abstract class AbstractSpriteShader(
    @JvmField protected val activeToolkit: AbstractNativeToolkit
) {
    @JvmField var maskSampler: NativeTextureSampler? = null
    @JvmField var spriteSampler: NativeTextureSampler? = null
    @JvmField var vertexStream: NativeStream? = null
    @JvmField var startVertex: Int = 0
    @JvmField var vertexDeclaration: VertexDeclaration? = null
    @JvmField val worldViewProjectionMatrix: Matrix44 = Matrix44()
    @JvmField val spriteTexCoordMatrix: Matrix44 = Matrix44()
    @JvmField val maskTexCoordMatrix: Matrix44 = Matrix44()

    abstract fun loadNormal()

    abstract fun setColours(type: Int, value: Int)

    abstract fun loadMasked()
}
