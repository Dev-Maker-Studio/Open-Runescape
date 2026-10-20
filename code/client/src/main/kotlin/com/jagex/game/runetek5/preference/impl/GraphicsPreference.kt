package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class GraphicsPreference : Preference {

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    fun ensureValid() {
        if (super.value < 0 || super.value > 4) {
            super.value = getDefaultValue()
        }
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getDefaultValue(): Int {
        return 0
    }

    override fun canModify(value: Int): Int {
        return 1
    }

    override fun getValue(): Int {
        return super.value
    }
}
