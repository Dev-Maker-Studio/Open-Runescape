package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class CpuMaxMemoryPreference extends Preference {

    public CpuMaxMemoryPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public CpuMaxMemoryPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {

    }

    @Override
    public int getDefaultValue() {
        return super.gamePreferences.getProcessorSpecs().getProcessorCount() > 1 ? 4 : 2;
    }

    @Override
    public int canModify(int i_1) {
        return 1;
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }
}
