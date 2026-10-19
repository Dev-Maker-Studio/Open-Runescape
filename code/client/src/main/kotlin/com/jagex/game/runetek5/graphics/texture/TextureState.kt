package com.jagex.game.runetek5.graphics.texture

class TextureState(
    @JvmField val type: Int
) {
    companion object {
        @JvmField
        val INITIALIZE: TextureState = TextureState(0)

        @JvmField
        val PARTIAL_COPY_LAST: TextureState = TextureState(1)

        @JvmField
        internal val UNUSED_T2: TextureState = TextureState(2)

        @JvmField
        internal val UNUSED_T3: TextureState = TextureState(3)

        @JvmField
        internal val UNUSED_T5: TextureState = TextureState(5)

        @JvmField
        internal val UNUSED_T4: TextureState = TextureState(4)
    }
}
