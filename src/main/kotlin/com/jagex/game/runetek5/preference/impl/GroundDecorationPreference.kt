package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Steven - 17/09/2023
 *
 */
class GroundDecorationPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    override fun getValue(): Int {
        return super.value
    }

    fun ensureValid() {
        if (super.gamePreferences.getGame() != Game.RUNESCAPE) {
            super.value = 1
        }
        if (super.value != 0 && super.value != 1) {
            super.value = getDefaultValue()
        }
    }

    fun playingRunescape(): Boolean {
        return super.gamePreferences.getGame() == Game.RUNESCAPE
    }

    override fun getDefaultValue(): Int {
        return 1
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun canModify(value: Int): Int {
        return if (super.gamePreferences.getGame() == Game.RUNESCAPE) 1 else 3
    }
}
