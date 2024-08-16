package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class ToolkitPreference extends Preference {

    public boolean isDefault;
    public boolean verifySettings = true;

    public ToolkitPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public ToolkitPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public void ensureValid() {
        if (super.value < 0 || super.value > 5) {
            super.value = getDefaultValue();
        }
    }

    public boolean visible() {
        return true;
    }

    @Override
    public int getDefaultValue() {
        this.isDefault = true;
        return 2;
    }

    @Override
    public void setValue(int value) {
        this.isDefault = false;
        this.value = value;
    }

    @Override
    public int canModify(int value) {
        return value == 3 && !AbstractToolkit.getNativeLibraryLoader().isJagdxLoaded() ? 3 : 2;
    }

    public int getValue() {
        return super.value;
    }

    public boolean verifySettings() {
        return this.verifySettings;
    }

    public void setVerifySettings(boolean verifySettings) {
        this.verifySettings = verifySettings;
    }

}
