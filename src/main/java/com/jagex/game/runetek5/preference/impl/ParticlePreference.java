package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class ParticlePreference extends Preference {

    public ParticlePreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
        ParticleSystemManager.setDetail(value);
    }

    public ParticlePreference(GamePreferences gamePreferences) {
        super(gamePreferences);
        ParticleSystemManager.setDetail(super.value);
    }

    public void ensureValid() {
        if (super.gamePreferences.getProcessorSpecs().getMaxMemory() < 245) {
            super.value = 0;
        }
        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int getDefaultValue() {
        return super.gamePreferences.getProcessorSpecs().getMaxMemory() < 245 ? 0 : 2;
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public int canModify(int value) {
        return super.gamePreferences.getProcessorSpecs().getMaxMemory() < 245 ? 3 : 1;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
        ParticleSystemManager.setDetail(this.value);
    }

    public boolean supported() {
        return super.gamePreferences.getProcessorSpecs().getMaxMemory() >= 245;
    }
}
