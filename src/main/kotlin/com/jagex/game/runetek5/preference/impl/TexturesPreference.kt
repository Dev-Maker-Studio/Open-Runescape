package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 * Preference controlling whether textures are enabled in the game.
 *
 * @author Steven - 17/09/2023
 */
class TexturesPreference : Preference {

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    fun ensureValid() {
        // Force textures on for non-RuneScape games
        if (gamePreferences.getGame() != Game.RUNESCAPE) {
            value = 1
        }

        // Ensure value is 0 or 1
        if (value != 0 && value != 1) {
            value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int = 1

    /**
     * Returns true if playing RuneScape (where texture preference can be changed)
     */
    fun playingRunescape(): Boolean = gamePreferences.getGame() == Game.RUNESCAPE

    override fun canModify(value: Int): Int {
        return if (gamePreferences.getGame() == Game.RUNESCAPE) {
            // Return 2 if textures enabled and ground blending is off, otherwise 1
            if (value != 0 && gamePreferences.goundBlending.getValue() != 1) 2 else 1
        } else {
            3 // Cannot modify for non-RuneScape games
        }
    }

    override fun getValue(): Int = value

    override fun setValue(value: Int) {
        this.value = value
    }
}
