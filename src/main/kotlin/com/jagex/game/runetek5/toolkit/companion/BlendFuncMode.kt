package com.jagex.game.runetek5.toolkit.companion

enum class BlendFuncMode(
    @JvmField val mode: Int
) {
    OPAQUE_BLENDING(0), // Needs refactor
    ALPHA_BLENDING(1),
    OPAQUE(2),
    COLOR_BLENDING(3) // Needs refactor
}
