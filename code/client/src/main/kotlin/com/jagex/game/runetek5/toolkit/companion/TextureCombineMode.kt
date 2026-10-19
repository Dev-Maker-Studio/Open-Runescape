package com.jagex.game.runetek5.toolkit.companion

enum class TextureCombineMode(
    @JvmField val mode: Int
) {
    ADD(0),
    SUBTRACT(1),
    REPLACE(2),
    INTERPOLATE(3),
    MODULATE(4)
}
