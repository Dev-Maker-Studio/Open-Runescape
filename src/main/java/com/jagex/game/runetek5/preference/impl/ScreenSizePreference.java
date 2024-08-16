package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class ScreenSizePreference extends Preference {

    public ScreenSizePreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public ScreenSizePreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
        if (super.value < 1 || super.value > 3) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public int getDefaultValue() {
        return super.gamePreferences.getProcessorSpecs().isARM() ? 3 : 2;
    }

    @Override
    public int canModify(int value) {
        return 1;
    }
}
