package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class AntiAliasingPreference extends Preference {

    public AntiAliasingPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public AntiAliasingPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    @Override
    public int canModify(int value) {
        return !GameState.isGamestate135(super.gamePreferences.activeToolkit.getValue()) ? 3 : 1;
    }

    public void ensureValid() {
        if (super.gamePreferences.activeToolkit.verifySettings() && !GameState.isGamestate135(super.gamePreferences.activeToolkit.getValue())) {
            super.value = 0;
        }

        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int getDefaultValue() {
        return 0;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return super.value;
    }

    public boolean visible() {
        return GameState.isGamestate135(super.gamePreferences.activeToolkit.getValue());
    }
}
