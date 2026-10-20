package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class MonoStereoPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    override fun getValue(): Int {
        return super.value
    }

    fun ensureValid() {
        if (super.value != 1 && super.value != 0) {
            super.value = getDefaultValue()
        }
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getDefaultValue(): Int {
        return 1
    }

    override fun canModify(value: Int): Int {
        return 1
    }
}
