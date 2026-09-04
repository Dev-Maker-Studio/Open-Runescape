package com.jagex.game.runetek5.config.inputsubscriber

import com.jagex.game.runetek5.config.Identifiable

enum class NameType(
    @JvmField val type: Int = 2,
    private val value: Int = -1,
    @JvmField val str: String = ""
) : Identifiable {
    PLAYER(1, 0),
    EMAIL(6, 1),
    IGNORE(7, 2),
    CLAN_CHANNEL(3, 3),
    CLAN(4, 4),
    NOTES(5, 5),
    NONE(0, 6),
    UNKNOWN();

    override fun getValue(): Int = value

    override fun toString(): String = str
}
