package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class CpuMaxMemoryPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
    }

    override fun getDefaultValue(): Int {
        return if (super.gamePreferences.getProcessorSpecs().getProcessorCount() > 1) 4 else 2
    }

    override fun canModify(value: Int): Int {
        return 1
    }

    override fun getValue(): Int {
        return super.value
    }

    override fun setValue(value: Int) {
        this.value = value
    }
}
