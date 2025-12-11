package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Steven - 17/09/2023
 *
 */
class SkyboxPreference : Preference {

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    fun ensureValid() {
        if (super.value < 0 || super.value > 1) {
            super.value = getDefaultValue()
        }
    }

    fun display(): Boolean {
        return GameState.isGamestate135(super.gamePreferences.activeToolkit!!.getValue())
    }

    override fun canModify(i_1: Int): Int {
        return if (!GameState.isGamestate135(super.gamePreferences.activeToolkit!!.getValue())) 3 else 1
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getDefaultValue(): Int {
        return 1
    }

    override fun getValue(): Int {
        return super.value
    }
}
