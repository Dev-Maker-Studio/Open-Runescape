package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Steven - 17/09/2023
 *
 */
class AntiAliasingPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    override fun canModify(value: Int): Int {
        return if (!GameState.isGamestate135(super.gamePreferences.activeToolkit!!.getValue())) 3 else 1
    }

    fun ensureValid() {
        if (super.gamePreferences.activeToolkit!!.verifySettings() && !GameState.isGamestate135(super.gamePreferences.activeToolkit!!.getValue())) {
            super.value = 0
        }

        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int {
        return 0
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getValue(): Int {
        return super.value
    }

    fun visible(): Boolean {
        return GameState.isGamestate135(super.gamePreferences.activeToolkit!!.getValue())
    }
}
