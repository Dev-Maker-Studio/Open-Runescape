package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class GroundDecorationPreference extends Preference {

    public GroundDecorationPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public GroundDecorationPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public int getValue() {
        return super.value;
    }

    public void ensureValid() {
        if (super.gamePreferences.getGame() != Game.RUNESCAPE) {
            super.value = 1;
        }
        if (super.value != 0 && super.value != 1) {
            super.value = getDefaultValue();
        }
    }

    public boolean playingRunescape() {
        return super.gamePreferences.getGame() == Game.RUNESCAPE;
    }

    @Override
    public int getDefaultValue() {
        return 1;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int canModify(int value) {
        return super.gamePreferences.getGame() == Game.RUNESCAPE ? 1 : 3;
    }
}
