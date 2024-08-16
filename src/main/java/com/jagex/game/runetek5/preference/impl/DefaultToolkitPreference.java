package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class DefaultToolkitPreference extends Preference {

    public DefaultToolkitPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public DefaultToolkitPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }
    
    public void ensureValid() {
        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int getDefaultValue() {
        return 0;
    }

    public boolean visible() {
        return true;
    }

    @Override
    public int canModify(int value) {
        return value == 2 ? 2 : 1;
    }

    public int getValue() {
        return super.value;
    }
}
