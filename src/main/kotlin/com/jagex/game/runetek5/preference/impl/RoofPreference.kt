package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Brainpower - 07/02/2023
 * Options:
 * 0 = Reset
 * 1 = Always
 * 2 = Selectively
 */
class RoofPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        if (super.gamePreferences.defaultToolkit.getValue() == 2 && super.value == 2) {
            super.value = 1
        }

        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue()
        }
    }

    override fun canModify(value: Int): Int {
        return 1
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getValue(): Int {
        return super.value
    }

    override fun getDefaultValue(): Int {
        return 2
    }
}
