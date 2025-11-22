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
        activeToolkit.method13967(0, TextureStageState.aClass67680)
        activeToolkit.method13969(0, TextureStageState.aClass67680)
        activeToolkit.method13967(1, TextureStageState.aClass67678)
        activeToolkit.method13969(1, TextureStageState.aClass67678)
        activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE)
        renderPrimitive()
    }

    /**
     * Renders the primitive with current texture settings.
     */
    fun renderPrimitive() {
        activeToolkit.setTextureSampler(textureSampler)
        activeToolkit.method13973().copyFrom(textureCoordinateMatrix)
        activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST)
        activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, anInt467, anInt468, anInt469, anInt470)
    }

    override fun loadUnderwaterGround() {
        loadStandartPointlight(0)
    }

    override fun loadPointlightEnvironmentMapping(lightCount: Int) {
        activeToolkit.setTextureSlot(0)
        activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.REPLACE)
        activeToolkit.method13967(0, TextureStageState.aClass67680)
        activeToolkit.method13969(0, TextureStageState.aClass67678)
        activeToolkit.method13967(1, TextureStageState.aClass67678)
        activeToolkit.method13969(1, TextureStageState.aClass67678)
        renderPrimitive()
    }

    override fun loadSpecularPointlight(lightCount: Int) {
        activeToolkit.setTextureSlot(0)
        activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.REPLACE)
        activeToolkit.method13967(0, TextureStageState.aClass67680)
        activeToolkit.method13969(0, TextureStageState.aClass67678)
        activeToolkit.method13967(1, TextureStageState.aClass67678)
        activeToolkit.method13969(1, TextureStageState.aClass67678)
        renderPrimitive()
    }

    override fun loadStandartPointlight(lightCount: Int) {
        activeToolkit.setTextureSlot(0)
        activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE)
        activeToolkit.method13967(0, TextureStageState.aClass67680)
        activeToolkit.method13969(0, TextureStageState.aClass67680)
        activeToolkit.method13967(1, TextureStageState.aClass67678)
        activeToolkit.method13969(1, TextureStageState.aClass67678)
        renderPrimitive()
    }
}
