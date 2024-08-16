package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;
import com.jagex.game.runetek5.scene.BuildArea;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class BuildAreaPreference extends Preference {

    public BuildAreaPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public BuildAreaPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public void ensureValid() {
        if (super.value < BuildArea.ORIGINAL_SIZE.id || super.value > BuildArea.LARGE_SIZE.id) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int getDefaultValue() {
        return BuildArea.ORIGINAL_SIZE.id;
    }

    @Override
    public int canModify(int i_1) {
        return 1;
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }
}
