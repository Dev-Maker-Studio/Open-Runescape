package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 * Preference controlling bloom lighting effects.
 * Bloom creates a glowing effect around bright areas.
 *
 * @author Steven - 17/09/2023
 */
class BloomPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        // Disable bloom if toolkit doesn't support it or game state doesn't allow it
        if (gamePreferences.activeToolkit!!.verifySettings() &&
            !GameState.isGamestate135(gamePreferences.activeToolkit!!.getValue())) {
            value = 0
        }

        // Ensure value is within valid range (0 or 1)
        if (value < 0 || value > 1) {
            value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int = 0

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getValue(): Int = value

    /**
     * Returns whether bloom is visible/available in current game state
     */
    fun visible(): Boolean = GameState.isGamestate135(gamePreferences.activeToolkit!!.getValue())

    override fun canModify(value: Int): Int {
        return if (!GameState.isGamestate135(gamePreferences.activeToolkit!!.getValue())) {
            3 // Cannot modify
        } else {
            1 // Can modify
        }
    }
}
