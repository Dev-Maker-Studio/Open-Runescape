package com.jagex.game.runetek5.preference

abstract class Preference {
    @JvmField var value: Int
    @JvmField protected val gamePreferences: GamePreferences

    protected constructor(value: Int, gamePreferences: GamePreferences) {
        this.value = value
        this.gamePreferences = gamePreferences
    }

    protected constructor(preferences: GamePreferences) {
        this.gamePreferences = preferences
        this.value = getDefaultValue()
    }

    abstract fun getDefaultValue(): Int

    fun setValidatedValue(requestedValue: Int) {
        if (canModify(requestedValue) != 3) {
            setValue(requestedValue)
        }
    }

    abstract fun setValue(value: Int)

    abstract fun canModify(value: Int): Int

    open fun getValue(): Int = value
}
