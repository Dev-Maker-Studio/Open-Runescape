package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class TexturesPreference : Preference {

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    fun ensureValid() {
        if (gamePreferences.getGame() != Game.RUNESCAPE) {
            value = 1
        }

        if (value != 0 && value != 1) {
            value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int = 1

    fun playingRunescape(): Boolean = gamePreferences.getGame() == Game.RUNESCAPE

    override fun canModify(value: Int): Int {
        return if (gamePreferences.getGame() == Game.RUNESCAPE) {
            if (value != 0 && gamePreferences.goundBlending!!.getValue() != 1) 2 else 1
        } else {
            3
        }
    }

    override fun getValue(): Int = value

    override fun setValue(value: Int) {
        this.value = value
    }
}
