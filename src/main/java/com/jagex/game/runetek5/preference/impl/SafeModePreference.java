package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class SafeModePreference extends Preference {

    public SafeModePreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public SafeModePreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int canModify(int value) {
        return 1;
    }

    public void ensureValid() {
        if (super.value != 1 && super.value != 0) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int getDefaultValue() {
        return 0;
    }
}
