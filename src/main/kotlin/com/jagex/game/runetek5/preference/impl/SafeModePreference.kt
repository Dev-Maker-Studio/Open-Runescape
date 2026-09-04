package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Steven - 17/09/2023
 *
 */
class SafeModePreference : Preference {

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    override fun getValue(): Int {
        return super.value
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun canModify(value: Int): Int {
        return 1
    }

    fun ensureValid() {
        if (super.value != 1 && super.value != 0) {
            super.value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int {
        return 0
    }
}
