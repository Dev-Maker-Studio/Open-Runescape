package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader
import com.jagex.game.runetek5.graphics.texture.TextureStageState
import com.jagex.game.runetek5.graphics.texture.TextureState
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode

/**
 * Native sprite shader implementation for low-detail mode.
 * Handles rendering of 2D sprites using the native rendering pipeline.
 */
class NativeSpriteShaderLd(nativeToolkit: AbstractNativeToolkit) : AbstractSpriteShader(nativeToolkit) {

    override fun loadNormal() {
        load()
    }

    /**
     * Loads and configures the sprite shader for rendering
     */
    fun load() {
        activeToolkit.setupUiRenderState()
        activeToolkit.loadWorldViewProjectionMatrix(worldViewProjectionMatrix)
        activeToolkit.setMatrixTransform(
            AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX,
            AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX,
            worldViewProjectionMatrix
        )
        activeToolkit.setTextureSlot(0)
        activeToolkit.setTextureSampler(spriteSampler)
        activeToolkit.setTextureRgbStageState(0, TextureStageState.TEXTURE)
        activeToolkit.setTextureAlphaStageState(0, TextureStageState.TEXTURE)
        activeToolkit.setTextureRgbStageState(1, TextureStageState.CONSTANT)
        activeToolkit.setTextureAlphaStageState(1, TextureStageState.CONSTANT)
        activeToolkit.getCurrentTextureMatrix().copyFrom(spriteTexCoordMatrix)
        activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST)
        activeToolkit.setStreamSource(0, vertexStream!!)
        activeToolkit.setVertexDeclaration(vertexDeclaration!!)
        activeToolkit.drawPrimitive(PrimitiveDrawMode.TRIANGLE_FAN, startVertex, 2)
    }

    override fun setColours(type: Int, value: Int) {
        activeToolkit.setColourType(type)
        activeToolkit.setColourValue(value)
    }

    override fun loadMasked() {
        activeToolkit.setTextureSlot(1)
        activeToolkit.setTextureSampler(maskSampler)
        activeToolkit.getCurrentTextureMatrix().copyFrom(maskTexCoordMatrix)
        activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST)
        activeToolkit.setTextureCombineModes(TextureCombineMode.REPLACE, TextureCombineMode.MODULATE)
        activeToolkit.setTextureRgbStageState(0, TextureStageState.PREVIOUS)
        load()
        activeToolkit.setTextureSlot(1)
        activeToolkit.setTextureOnCurrentSlot()
    }
}
