package com.jagex.game.runetek5.graphics.texture

class TextureStageState(
    @JvmField val state: Int
) {
    companion object {
        @JvmField
        val PREVIOUS: TextureStageState = TextureStageState(3)

        @JvmField
        val TEXTURE: TextureStageState = TextureStageState(2)

        @JvmField
        val PRIMARY_COLOR: TextureStageState = TextureStageState(1)

        @JvmField
        val CONSTANT: TextureStageState = TextureStageState(0)
    }
}
