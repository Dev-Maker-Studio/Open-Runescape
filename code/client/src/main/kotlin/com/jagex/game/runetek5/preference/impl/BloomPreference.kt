package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class BloomPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        if (gamePreferences.activeToolkit!!.verifySettings() &&
            !GameState.isGamestate135(gamePreferences.activeToolkit!!.getValue())) {
            value = 0
        }

        if (value < 0 || value > 1) {
            value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int = 0

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getValue(): Int = value

    fun visible(): Boolean = GameState.isGamestate135(gamePreferences.activeToolkit!!.getValue())

    override fun canModify(value: Int): Int {
        return if (!GameState.isGamestate135(gamePreferences.activeToolkit!!.getValue())) {
            3
        } else {
            1
        }
    }
}
