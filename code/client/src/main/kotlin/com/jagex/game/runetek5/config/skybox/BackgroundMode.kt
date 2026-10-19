package com.jagex.game.runetek5.config.skybox

import com.jagex.game.runetek5.config.Identifiable

enum class BackgroundMode(
    @JvmField val value: Int
) : Identifiable {
    SINGLE_COLOR(0),
    COLOR_TRANSITION(1);

    override fun getValue(): Int {
        return value
    }
}
