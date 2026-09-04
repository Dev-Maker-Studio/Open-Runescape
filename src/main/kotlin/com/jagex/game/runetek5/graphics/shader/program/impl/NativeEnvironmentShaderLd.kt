package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader
import com.jagex.game.runetek5.graphics.texture.TextureStageState
import com.jagex.game.runetek5.graphics.texture.TextureState
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode

/**
 * Native environment shader for low detail rendering.
 */
class NativeEnvironmentShaderLd(
    toolkit: AbstractNativeToolkit
) : AbstractEnvironmentShader(toolkit) {

    override fun loadWorldViewProjection(matrix: Matrix44) {
        activeToolkit.setMatrixTransform(matrix, activeToolkit.particleMatrix, activeToolkit.projectionMatrix)
    }

    override fun loadUnlitProgram(enabled: Boolean) {
        activeToolkit.setTextureSlot(0)
        activeToolkit.setTextureRgbStageState(0, TextureStageState.TEXTURE)
        activeToolkit.setTextureAlphaStageState(0, TextureStageState.TEXTURE)
        activeToolkit.setTextureRgbStageState(1, TextureStageState.PRIMARY_COLOR)
        activeToolkit.setTextureAlphaStageState(1, TextureStageState.PRIMARY_COLOR)
        activeToolkit.setTextureCombineModes(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE)
        renderPrimitive()
    }

    /**
     * Renders the primitive with current texture settings.
     */
    fun renderPrimitive() {
        activeToolkit.setTextureSampler(textureSampler)
        activeToolkit.getCurrentTextureMatrix().copyFrom(textureCoordinateMatrix)
        activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST)
        activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, startVertex, vertexCount, startIndex, primitiveCount)
    }

    override fun loadUnderwaterGround() {
        loadStandartPointlight(0)
    }

    override fun loadPointlightEnvironmentMapping(lightCount: Int) {
        activeToolkit.setTextureSlot(0)
        activeToolkit.setTextureCombineModes(TextureCombineMode.MODULATE, TextureCombineMode.REPLACE)
        activeToolkit.setTextureRgbStageState(0, TextureStageState.TEXTURE)
        activeToolkit.setTextureAlphaStageState(0, TextureStageState.PRIMARY_COLOR)
        activeToolkit.setTextureRgbStageState(1, TextureStageState.PRIMARY_COLOR)
        activeToolkit.setTextureAlphaStageState(1, TextureStageState.PRIMARY_COLOR)
        renderPrimitive()
    }

    override fun loadSpecularPointlight(lightCount: Int) {
        activeToolkit.setTextureSlot(0)
        activeToolkit.setTextureCombineModes(TextureCombineMode.MODULATE, TextureCombineMode.REPLACE)
        activeToolkit.setTextureRgbStageState(0, TextureStageState.TEXTURE)
        activeToolkit.setTextureAlphaStageState(0, TextureStageState.PRIMARY_COLOR)
        activeToolkit.setTextureRgbStageState(1, TextureStageState.PRIMARY_COLOR)
        activeToolkit.setTextureAlphaStageState(1, TextureStageState.PRIMARY_COLOR)
        renderPrimitive()
    }

    override fun loadStandartPointlight(lightCount: Int) {
        activeToolkit.setTextureSlot(0)
        activeToolkit.setTextureCombineModes(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE)
        activeToolkit.setTextureRgbStageState(0, TextureStageState.TEXTURE)
        activeToolkit.setTextureAlphaStageState(0, TextureStageState.TEXTURE)
        activeToolkit.setTextureRgbStageState(1, TextureStageState.PRIMARY_COLOR)
        activeToolkit.setTextureAlphaStageState(1, TextureStageState.PRIMARY_COLOR)
        renderPrimitive()
    }
}
