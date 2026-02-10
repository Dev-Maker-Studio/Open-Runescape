package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.program.AbstractParticleShader
import com.jagex.game.runetek5.graphics.texture.TextureStageState
import com.jagex.game.runetek5.graphics.texture.TextureState
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode

class NativeParticleShaderLd(nativeToolkit: AbstractNativeToolkit) : AbstractParticleShader(nativeToolkit) {

    override fun setViewProjectionMatrix(matrix441: Matrix44) {
        super.activeToolkit.setMatrixTransform(matrix441, super.activeToolkit.particleMatrix, super.activeToolkit.projectionMatrix)
    }

    override fun drawPointSprite() {
        super.activeToolkit.setColourValue(super.diffuseColor)
        super.activeToolkit.setTextureRgbStageState(1, TextureStageState.CONSTANT)
        super.activeToolkit.setTextureAlphaStageState(1, TextureStageState.CONSTANT)
        loadTextureState()
        super.activeToolkit.drawQuadTriangleFan()
    }

    override fun drawParticles(i_1: Int) {
        super.activeToolkit.setTextureRgbStageState(1, TextureStageState.PRIMARY_COLOR)
        super.activeToolkit.setTextureAlphaStageState(1, TextureStageState.PRIMARY_COLOR)
        loadTextureState()
        super.activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, 0, i_1 * 4, 0, i_1 * 2)
    }

    fun loadTextureState() {
        super.activeToolkit.setTextureSlot(0)
        super.activeToolkit.setTextureSampler(super.diffuseSampler)
        super.activeToolkit.setTextureRgbStageState(0, TextureStageState.TEXTURE)
        super.activeToolkit.setTextureAlphaStageState(0, TextureStageState.TEXTURE)
        super.activeToolkit.setTextureCombineModes(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE)
        super.activeToolkit.getCurrentTextureMatrix().copyFrom(super.textureCoordinateMatrix)
        super.activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST)
    }
}
