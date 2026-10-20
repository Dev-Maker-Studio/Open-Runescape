package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference
import com.jagex.game.runetek5.scene.BuildArea

class BuildAreaPreference : Preference {

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    fun ensureValid() {
        if (super.value < BuildArea.ORIGINAL_SIZE.id || super.value > BuildArea.LARGE_SIZE.id) {
            super.value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int {
        return BuildArea.ORIGINAL_SIZE.id
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
