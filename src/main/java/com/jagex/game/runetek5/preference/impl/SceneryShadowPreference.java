package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class SceneryShadowPreference extends Preference {

    public SceneryShadowPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public SceneryShadowPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public boolean enabled() {
        return super.gamePreferences.textures.getValue() != 0;
    }

    @Override
	public int canModify(int value) {
        return super.gamePreferences.textures.getValue() == 0 ? 3 : 1;
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

    public void ensureValid() {
        if (super.gamePreferences.textures.getValue() == 0) {
            super.value = 0;
        }
        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue();
        }
    }
}
