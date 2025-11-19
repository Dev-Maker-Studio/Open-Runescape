package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Steven - 17/09/2023
 *
 */
class IdleAnimationsPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    override fun getDefaultValue(): Int {
        return 1
    }

    override fun getValue(): Int {
        return super.value
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    fun ensureValid() {
        if (super.gamePreferences.getGame() == Game.STELLAR_DAWN) {
            super.value = 2
        }

        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue()
        }
    }

    override fun canModify(value: Int): Int {
        return 1
    }
}
