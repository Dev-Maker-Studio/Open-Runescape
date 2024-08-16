package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class FogPreference extends Preference {

    public FogPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public FogPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
        if (super.value != 0 && super.gamePreferences.goundBlending.getValue() != 1) {
            super.value = 0;
        }

        if (super.value != 0 && super.gamePreferences.defaultToolkit.getValue() == 2) {
            super.value = 0;
        }

        if (super.value < 0 || super.value > 1) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int getDefaultValue() {
        return 1;
    }

    @Override
    public int canModify(int value) {
        return value != 0 && super.gamePreferences.defaultToolkit.getValue() == 2 ? 3 : (value != 0 && super.gamePreferences.goundBlending.getValue() != 1 ? 2 : 1);
    }

    public int getValue() {
        return super.value;
    }

    public boolean visible() {
        return true;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }
}
