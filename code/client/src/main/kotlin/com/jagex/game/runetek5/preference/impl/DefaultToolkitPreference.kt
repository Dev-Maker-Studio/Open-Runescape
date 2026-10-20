package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class DefaultToolkitPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    override fun setValue(value: Int) {
        this.value = value
    }

    fun ensureValid() {
        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int {
        return 0
    }

    fun visible(): Boolean {
        return true
    }

    override fun canModify(value: Int): Int {
        return if (value == 2) 2 else 1
    }

    override fun getValue(): Int {
        return super.value
    }
}
