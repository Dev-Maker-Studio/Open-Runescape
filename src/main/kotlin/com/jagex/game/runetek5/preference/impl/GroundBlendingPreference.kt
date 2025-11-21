package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 * Preference for ground blending (terrain smoothing).
 *
 * @author Steven - 17/09/2023
 */
class GroundBlendingPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    override fun canModify(value: Int): Int {
        if (gamePreferences.getGame() == Game.RUNESCAPE) {
            if (value == 0) {
                if (gamePreferences.fog.getValue() == 1) {
                    return 2
                }
                if (gamePreferences.textures.getValue() == 1) {
                    return 2
                }
                if (gamePreferences.water.getValue() > 0) {
                    return 2
                }
            }
            return 1
        }
        return 3
    }

    override fun getDefaultValue(): Int {
        return 1
    }

    fun playingRunescape(): Boolean {
        return gamePreferences.getGame() == Game.RUNESCAPE
    }

    override fun getValue(): Int {
        return value
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    fun ensureValid() {
        if (gamePreferences.getGame() != Game.RUNESCAPE) {
            value = 1
        }

        if (value != 0 && value != 1) {
            value = getDefaultValue()
        }
    }
}
