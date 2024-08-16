package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

public class GameTipPreference extends Preference {

    public GameTipPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public GameTipPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
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

    public void ensureValid() {
    }

    public int getValue() {
        return this.value;
    }
}
