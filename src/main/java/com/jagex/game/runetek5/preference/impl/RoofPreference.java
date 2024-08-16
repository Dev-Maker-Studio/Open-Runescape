package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Brainpower - 07/02/2023
 * Options:
 * 0 = Reset
 * 1 = Always
 * 2 = Selectively
 */
public class RoofPreference extends Preference {

    public RoofPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public RoofPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
        if (super.gamePreferences.defaultToolkit.getValue() == 2 && super.value == 2) {
            super.value = 1;
        }

        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue();
        }

    }

    @Override
    public int canModify(int value) {
        return 1;
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
        return 2;
    }
}
