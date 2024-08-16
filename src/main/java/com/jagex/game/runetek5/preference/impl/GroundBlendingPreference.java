package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class GroundBlendingPreference extends Preference {

    public GroundBlendingPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public GroundBlendingPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    @Override
    public int canModify(int value) {
        if (super.gamePreferences.getGame() == Game.RUNESCAPE) {
            if (value == 0) {
                if (super.gamePreferences.fog.getValue() == 1) {
                    return 2;
                }
                if (super.gamePreferences.textures.getValue() == 1) {
                    return 2;
                }
                if (super.gamePreferences.water.getValue() > 0) {
                    return 2;
                }
            }
            return 1;
        } 
        return 3;
    }

    @Override
    public int getDefaultValue() {
        return 1;
    }

    public boolean playingRunescape() {
        return super.gamePreferences.getGame() == Game.RUNESCAPE;
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    public void ensureValid() {
        if (super.gamePreferences.getGame() != Game.RUNESCAPE) {
            super.value = 1;
        }

        if (super.value != 0 && super.value != 1) {
            super.value = getDefaultValue();
        }
    }
}
