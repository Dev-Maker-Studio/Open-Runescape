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
        activeToolkit.method14003()
        activeToolkit.loadWorldViewProjectionMatrix(worldViewProjectionMatrix)
        activeToolkit.setMatrixTransform(
            AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX,
            AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX,
            worldViewProjectionMatrix
        )
        activeToolkit.setTextureSlot(0)
        activeToolkit.setTextureSampler(anInterface6443)
        activeToolkit.method13967(0, TextureStageState.aClass67680)
        activeToolkit.method13969(0, TextureStageState.aClass67680)
        activeToolkit.method13967(1, TextureStageState.aClass67677)
        activeToolkit.method13969(1, TextureStageState.aClass67677)
        activeToolkit.method13973().copyFrom(aClass384440)
        activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST)
        activeToolkit.setStreamSource(0, anInterface4445!!)
        activeToolkit.setVertexDeclaration(aClass70447!!)
        activeToolkit.drawPrimitive(PrimitiveDrawMode.TRIANGLE_FAN, anInt446, 2)
    }

    override fun setColours(type: Int, value: Int) {
        activeToolkit.setColourType(type)
        activeToolkit.setColourValue(value)
    }

    override fun loadMasked() {
        activeToolkit.setTextureSlot(1)
        activeToolkit.setTextureSampler(anInterface6441)
        activeToolkit.method13973().copyFrom(aClass384444)
        activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST)
        activeToolkit.method13966(TextureCombineMode.REPLACE, TextureCombineMode.MODULATE)
        activeToolkit.method13967(0, TextureStageState.aClass67679)
        load()
        activeToolkit.setTextureSlot(1)
        activeToolkit.setTextureOnCurrentSlot()
    }
}
