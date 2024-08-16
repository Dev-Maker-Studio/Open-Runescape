package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

public class CharacterShadowPreference extends Preference {

    public CharacterShadowPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public CharacterShadowPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
        if (super.value != 1 && super.value != 0) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int getDefaultValue() {
        return 1;
    }

    @Override
    public int canModify(int value) {
        return 1;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    public boolean visible() {
        return true;
    }

    public int getValue() {
        return super.value;
    }

}
