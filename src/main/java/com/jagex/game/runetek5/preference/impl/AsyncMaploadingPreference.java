package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class AsyncMaploadingPreference extends Preference {

    public AsyncMaploadingPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public AsyncMaploadingPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
        int value = super.gamePreferences.toolkit.getValue();
        if (value != 3 && value != 5) {
            value = 0;
        }

        if (super.gamePreferences.getProcessorSpecs().getProcessorCount() < 2) {
            value = 0;
        }

        if (value != 0 && value != 1) {
            value = getDefaultValue();
        }
    }

    @Override
    public int getDefaultValue() {
        return 0;
    }

    @Override
    public int canModify(int value) {
        if (value == 0) {
            return 1;
        } 
        if (super.gamePreferences.getProcessorSpecs().getProcessorCount() < 2) {
            return 3;
        } 
        int toolkit = super.gamePreferences.toolkit.getValue();
        return toolkit != 3 && toolkit != 5 ? 3 : 1;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return Loader.MULTI_THREADED_MAPLOADING ? 1 : this.value;
    }
}
