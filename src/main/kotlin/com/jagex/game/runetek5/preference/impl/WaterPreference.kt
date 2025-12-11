package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Steven - 17/09/2023
 *
 */
class WaterPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
    }

    override fun getDefaultValue(): Int {
        return 1
    }

    override fun getValue(): Int {
        return super.value
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun canModify(value: Int): Int {
        return if (value != 0 && super.gamePreferences.goundBlending!!.getValue() != 1) 2 else 1
    }

    fun display(): Boolean {
        return true
    }
}
