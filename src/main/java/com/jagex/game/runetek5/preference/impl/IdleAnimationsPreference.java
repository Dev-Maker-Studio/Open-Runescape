package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class IdleAnimationsPreference extends Preference {

    public IdleAnimationsPreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    public IdleAnimationsPreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    @Override
    public int getDefaultValue() {
        return 1;
    }

    public int getValue() {
        return super.value;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    public void ensureValid() {
        if (super.gamePreferences.getGame() == Game.STELLAR_DAWN) {
            super.value = 2;
        }

        if (super.value < 0 || super.value > 2) {
            super.value = getDefaultValue();
        }
    }

    @Override
    public int canModify(int value) {
        return 1;
    }
}
