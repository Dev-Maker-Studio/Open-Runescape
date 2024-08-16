package com.jagex.game.runetek5.preference;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public abstract class Preference {

	public int value;
    protected final GamePreferences gamePreferences;

    protected Preference(int value, GamePreferences gamePreferences) {
        this.value = value;
        this.gamePreferences = gamePreferences;
    }

    protected Preference(GamePreferences preferences) {
        this.gamePreferences = preferences;
        this.value = getDefaultValue();
    }

    public abstract int getDefaultValue();

    public void setValidatedValue(int requestedValue) {
        if (canModify(requestedValue) != 3) {
            setValue(requestedValue);
        }
    }

    public abstract void setValue(int value);

    public abstract int canModify(int value);

}
