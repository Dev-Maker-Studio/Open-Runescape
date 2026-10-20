package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class MaxScreenSizePreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue()
        }
    }

    override fun canModify(value: Int): Int {
        return 1
    }

    fun visible(): Boolean {
        return true
    }

    override fun getValue(): Int {
        return super.value
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getDefaultValue(): Int {
        return 0
    }
}
