package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class ScreenSizePreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        if (super.value < 1 || super.value > 3) {
            super.value = getDefaultValue()
        }
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getValue(): Int {
        return super.value
    }

    override fun getDefaultValue(): Int {
        return if (super.gamePreferences.getProcessorSpecs().isARM()) 3 else 2
    }

    override fun canModify(value: Int): Int {
        return 1
    }
}
