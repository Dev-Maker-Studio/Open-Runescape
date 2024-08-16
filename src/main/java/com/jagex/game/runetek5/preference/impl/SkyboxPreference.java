package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class SkyboxPreference extends Preference {

    public SkyboxPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public SkyboxPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public void ensureValid() {
        if (super.value < 0 || super.value > 1) {
            super.value = getDefaultValue();
        }
    }

    public boolean display() {
        return GameState.isGamestate135(super.gamePreferences.activeToolkit.getValue());
    }

    @Override
    public int canModify(int i_1) {
        return !GameState.isGamestate135(super.gamePreferences.activeToolkit.getValue()) ? 3 : 1;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int getDefaultValue() {
        return 1;
    }

    public int getValue() {
        return super.value;
    }
}
