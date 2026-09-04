package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class GameTipPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getDefaultValue(): Int {
        return 0
    }

    override fun canModify(value: Int): Int {
        return 1
    }

    fun ensureValid() {
    }

    override fun getValue(): Int {
        return this.value
    }
}
