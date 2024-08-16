package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class SoundLevelPreference extends Preference {

    public SoundLevelPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public SoundLevelPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
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
        return 127;
    }
}
