package com.jagex.game.runetek5.config.inputsubscriber

import com.jagex.game.runetek5.config.Identifiable

enum class NameType(
    @JvmField val type: Int = 2,
    private val value: Int = -1,
    @JvmField val str: String = ""
) : Identifiable {
    aClass4134971(1, 0),
    aClass4134965(6, 1),
    aClass4134968(7, 2),
    aClass4134967(3, 3),
    aClass4134970(4, 4),
    aClass4134969(5, 5),
    aClass4134964(0, 6),
    aClass4134966();

    override fun getValue(): Int = value

    override fun toString(): String = str
}
