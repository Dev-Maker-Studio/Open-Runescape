package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class WaterPreference extends Preference {

    public WaterPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public WaterPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
    }

    @Override
    public int getDefaultValue() {
        return 1;
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
        return value != 0 && super.gamePreferences.goundBlending.getValue() != 1 ? 2 : 1;
    }

    public boolean display() {
        return true;
    }
}
