package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.config.Identifiable

enum class MovementType(
    @JvmField val id: Byte
) : Identifiable {
    STATIONARY(-1),
    HALF_WALK(0),
    WALKING(1),
    RUNNING(2);

    override fun getValue(): Int {
        return id.toInt()
    }

    fun getValueIncr(): Int {
        return id + 1
    }
}
