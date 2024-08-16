package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class TexturesPreference extends Preference {

    public TexturesPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public TexturesPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public void ensureValid() {
        if (super.gamePreferences.getGame() != Game.RUNESCAPE) {
            super.value = 1;
        }

        if (super.value != 0 && super.value != 1) {
            super.value = getDefaultValue();
        }

    }

    @Override
    public int getDefaultValue() {
        return 1;
    }

    public boolean playingRunescape() {
        return super.gamePreferences.getGame() == Game.RUNESCAPE;
    }

    @Override
    public int canModify(int value) {
        return super.gamePreferences.getGame() == Game.RUNESCAPE ? (value != 0 && super.gamePreferences.goundBlending.getValue() != 1 ? 2 : 1) : 3;
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }
}
