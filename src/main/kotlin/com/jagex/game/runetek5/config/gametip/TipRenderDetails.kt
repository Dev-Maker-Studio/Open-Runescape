package com.jagex.game.runetek5.config.gametip

enum class TipRenderDetails(
    @JvmField val transformType: Int
) {
    BACKGROUND(1),
    COLOR_LOADING_BAR(2),
    SPRITE_LOADING_BAR(2),
    MULTI_SPRITE_LOADING_BAR(2),
    OUTLINED(1),
    SPRITE(1),
    ROTATED_SPRITE(1),
    ANCHORED_TEXT(2),
    TIPS_SPRITE(1),
    ANIMATED_LOADING_BAR(2)
}
