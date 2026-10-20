package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class SoundLevelPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
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
        return 127
    }
}
