package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Steven - 17/09/2023
 *
 */
class SceneryShadowPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun enabled(): Boolean {
        return super.gamePreferences.textures!!.getValue() != 0
    }

    override fun canModify(value: Int): Int {
        return if (super.gamePreferences.textures!!.getValue() == 0) 3 else 1
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getValue(): Int {
        return super.value
    }

    override fun getDefaultValue(): Int {
        return 2
    }

    fun ensureValid() {
        if (super.gamePreferences.textures!!.getValue() == 0) {
            super.value = 0
        }
        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue()
        }
    }
}
