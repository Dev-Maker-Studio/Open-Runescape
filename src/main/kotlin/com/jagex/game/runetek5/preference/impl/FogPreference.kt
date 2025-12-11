package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 * Preference controlling atmospheric fog rendering.
 * Fog adds depth to distant scenery.
 *
 * @author Steven - 17/09/2023
 */
class FogPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        // Disable fog if ground blending is off
        if (value != 0 && gamePreferences.goundBlending!!.getValue() != 1) {
            value = 0
        }

        // Disable fog for toolkit value 2 (software renderer)
        if (value != 0 && gamePreferences.defaultToolkit!!.getValue() == 2) {
            value = 0
        }

        // Ensure value is within valid range (0 or 1)
        if (value < 0 || value > 1) {
            value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int = 1

    override fun canModify(value: Int): Int {
        return when {
            // Cannot modify with software renderer
            value != 0 && gamePreferences.defaultToolkit!!.getValue() == 2 -> 3

            // Incompatible with ground blending off
            value != 0 && gamePreferences.goundBlending!!.getValue() != 1 -> 2

            // Can modify
            else -> 1
        }
    }

    override fun getValue(): Int = value

    /**
     * Returns whether fog setting is visible (always true)
     */
    fun visible(): Boolean = true

    override fun setValue(value: Int) {
        this.value = value
    }
}
