package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 * Preference controlling particle system detail level.
 * Determines how many particles are rendered (0=off, 1=low, 2=high).
 *
 * @author Steven - 17/09/2023
 */
class ParticlePreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences) {
        ParticleSystemManager.setDetail(value)
    }

    constructor(gamePreferences: GamePreferences) : super(gamePreferences) {
        ParticleSystemManager.setDetail(value)
    }

    fun ensureValid() {
        // Force particles off if memory is too low
        if (gamePreferences.getProcessorSpecs().getMaxMemory() < 245) {
            value = 0
        }

        // Ensure value is in valid range (0-2)
        if (value < 0 || value > 2) {
            value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int {
        return if (gamePreferences.getProcessorSpecs().getMaxMemory() < 245) 0 else 2
    }

    override fun getValue(): Int = value

    override fun canModify(value: Int): Int {
        return if (gamePreferences.getProcessorSpecs().getMaxMemory() < 245) {
            3 // Cannot modify - insufficient memory
        } else {
            1 // Can modify
        }
    }

    override fun setValue(value: Int) {
        this.value = value
        ParticleSystemManager.setDetail(this.value)
    }

    /**
     * Returns whether particles are supported (sufficient memory available)
     */
    fun supported(): Boolean = gamePreferences.getProcessorSpecs().getMaxMemory() >= 245
}
