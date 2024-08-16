package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

public class MaxScreenSizePreference extends Preference {

    public MaxScreenSizePreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public MaxScreenSizePreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int canModify(int value) {
        return 1;
    }

    public boolean visible() {
        return true;
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int getDefaultValue() {
        return super.gamePreferences.activeToolkit.verifySettings() && nonHardwareMode(super.gamePreferences.activeToolkit.getValue()) ? 1 : 0;
    }

    /**
     * Safe mode / Software mode
     */
	public static boolean nonHardwareMode(int mode) {
	    return mode == 0 || mode == 2;
	}

}
