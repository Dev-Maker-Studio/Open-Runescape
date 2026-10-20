package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class ParticlePreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences) {
        ParticleSystemManager.setDetail(value)
    }

    constructor(gamePreferences: GamePreferences) : super(gamePreferences) {
        ParticleSystemManager.setDetail(value)
    }

    fun ensureValid() {
        if (gamePreferences.getProcessorSpecs().getMaxMemory() < 245) {
            value = 0
        }

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
            3
        } else {
            1
        }
    }

    override fun setValue(value: Int) {
        this.value = value
        ParticleSystemManager.setDetail(this.value)
    }

    fun supported(): Boolean = gamePreferences.getProcessorSpecs().getMaxMemory() >= 245
}
