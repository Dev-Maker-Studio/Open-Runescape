package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class FogPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        if (value != 0 && gamePreferences.goundBlending!!.getValue() != 1) {
            value = 0
        }

        if (value != 0 && gamePreferences.defaultToolkit!!.getValue() == 2) {
            value = 0
        }

        if (value < 0 || value > 1) {
            value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int = 1

    override fun canModify(value: Int): Int {
        return when {
            value != 0 && gamePreferences.defaultToolkit!!.getValue() == 2 -> 3

            value != 0 && gamePreferences.goundBlending!!.getValue() != 1 -> 2

            else -> 1
        }
    }

    override fun getValue(): Int = value

    fun visible(): Boolean = true

    override fun setValue(value: Int) {
        this.value = value
    }
}
