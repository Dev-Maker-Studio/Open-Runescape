package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class GraphicsPreference extends Preference {

    public GraphicsPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public GraphicsPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public void ensureValid() {
        if (super.value < 0 || super.value > 4) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int getDefaultValue() {
        return 0;
    }

    @Override
    public int canModify(int value) {
        return 1;
    }

    public int getValue() {
        return super.value;
    }
}
